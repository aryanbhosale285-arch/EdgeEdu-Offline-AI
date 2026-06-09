"""EdgeEdu Offline AI Chatbot — command-line entry point.

Commands:
    build-corpus   data/**/*.json  ->  data/curriculum.jsonl
    build-index    build + persist the hybrid retrieval index
    chat           interactive RAG chat loop
    serve          run the FastAPI server
"""
from __future__ import annotations

from pathlib import Path
from typing import Optional

import typer

from chatbot.chat_history import ConversationHistory
from chatbot.content_index import ContentIndex, Embedder
from chatbot.data_pipeline import build_corpus
from chatbot.language_model import build_language_model
from chatbot.retrieval_qa import RetrievalQA
from chatbot.utils.config import Settings, get_settings

app = typer.Typer(
    add_completion=False,
    help="EdgeEdu offline RAG chatbot over the curriculum.",
)


# --------------------------------------------------------------------------- #
# Assembly helpers (shared by the CLI and the FastAPI app)
# --------------------------------------------------------------------------- #

def ensure_corpus(settings: Settings) -> Path:
    """Build the JSONL corpus on demand if it doesn't exist yet."""
    corpus = Path(settings.paths.corpus_file)
    if not corpus.exists():
        data_dir = Path(settings.paths.data_dir)
        n = build_corpus(data_dir, corpus)
        typer.echo(f"Built corpus: {n} chunks -> {corpus}")
    return corpus


def load_index(settings: Settings) -> ContentIndex:
    """Load a persisted index if present, else build one from the corpus."""
    corpus = ensure_corpus(settings)
    embedder = Embedder(settings.embedding) if settings.embedding.enabled else None
    index_dir = Path(settings.paths.index_dir)
    if (index_dir / "index_meta.pkl").exists():
        return ContentIndex.load(index_dir, settings.retrieval, embedder)
    return ContentIndex.build(corpus, settings.retrieval, embedder)


def create_qa(settings: Optional[Settings] = None) -> RetrievalQA:
    settings = settings or get_settings()
    index = load_index(settings)
    model = build_language_model(settings.model)
    return RetrievalQA(
        index, model, settings.retrieval, settings.default_language
    )


# --------------------------------------------------------------------------- #
# Commands
# --------------------------------------------------------------------------- #

@app.command("build-corpus")
def build_corpus_cmd():
    """Normalize data/**/*.json into data/curriculum.jsonl."""
    settings = get_settings()
    n = build_corpus(Path(settings.paths.data_dir), Path(settings.paths.corpus_file))
    typer.echo(f"Wrote {n} chunks to {settings.paths.corpus_file}")


@app.command("build-index")
def build_index_cmd():
    """Build and persist the hybrid retrieval index."""
    settings = get_settings()
    corpus = ensure_corpus(settings)
    embedder = Embedder(settings.embedding) if settings.embedding.enabled else None
    index = ContentIndex.build(corpus, settings.retrieval, embedder)
    index.save(Path(settings.paths.index_dir))
    mode = "hybrid (BM25 + dense)" if index.has_dense else "BM25-only"
    typer.echo(f"Indexed {len(index)} chunks [{mode}] -> {settings.paths.index_dir}")


@app.command()
def chat(
    session: str = typer.Option("default", help="Conversation/session id."),
    language: Optional[str] = typer.Option(
        None, help="Force answer language (English/Hindi/Marathi)."
    ),
):
    """Start an interactive chat loop. Type 'quit' or 'exit' to leave."""
    settings = get_settings()
    qa = create_qa(settings)
    history_dir = Path(settings.paths.history_dir)
    history = ConversationHistory(settings.history, session, history_dir)

    mode = "hybrid" if qa.index.has_dense else "BM25-only"
    llm = "mock" if not settings.model.enabled else settings.model.name
    typer.echo(
        f"EdgeEdu chatbot ready — retrieval: {mode}, model: {llm}. "
        "Ask a question (or 'exit').\n"
    )

    while True:
        try:
            query = typer.prompt("You").strip()
        except (EOFError, KeyboardInterrupt):
            typer.echo("\nGoodbye!")
            break
        if query.lower() in {"quit", "exit"}:
            typer.echo("Goodbye!")
            break
        if not query:
            continue

        result = qa.answer(query, history.context_messages(), language)
        history.add_user(query, [c.uid for c in result.citations])
        history.add_assistant(result.text)

        typer.echo(f"\nEdgeEdu: {result.text}")
        if result.citations:
            srcs = ", ".join(c.uid for c in result.citations)
            typer.echo(f"  sources: {srcs}\n")
        else:
            typer.echo("")


@app.command()
def serve(
    host: Optional[str] = typer.Option(None),
    port: Optional[int] = typer.Option(None),
):
    """Run the FastAPI server."""
    import uvicorn

    settings = get_settings()
    uvicorn.run(
        "chatbot.api:app",
        host=host or settings.server.host,
        port=port or settings.server.port,
    )


if __name__ == "__main__":
    app()
