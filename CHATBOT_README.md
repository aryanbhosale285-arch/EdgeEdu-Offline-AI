# EdgeEdu Offline AI Chatbot

An offline-first **RAG** (retrieval-augmented generation) assistant over the
Maharashtra State Board curriculum (Standards 9 & 10) in **English, Hindi, and
Marathi**. It retrieves the most relevant curriculum chunks for a question and
generates a grounded, cited answer with a local transformer.

> This Python chatbot lives alongside the Next.js web prototype in the same repo
> and reuses the same `data/` curriculum. (The web app's README is
> [README.md](README.md).)

## How it works

```
question
  -> detect language + tokenize (Devanagari-safe)
  -> hybrid retrieval:  BM25 (lexical, field-boosted)
                      + dense (sentence-transformers + FAISS)   [optional]
                      fused with Reciprocal Rank Fusion -> top-k chunks
  -> grounded prompt (context + citations + "answer in the user's language")
  -> local LLM (default google/gemma-2-2b-it) -> answer + [uid] citations
```

Dense retrieval is **optional**: if the embedding model or FAISS isn't available,
the system degrades to BM25-only and still works fully offline.

## Install

```bash
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

> **Default model is gated.** `google/gemma-2-2b-it` requires accepting the
> license on Hugging Face and authenticating (`huggingface-cli login` or set
> `HF_TOKEN`). To try the pipeline without any large download, run in mock mode
> (next section) or point `model.name` at an open model in `config.yaml`.

## Usage

```bash
# 1. Build the corpus (data/**/*.json -> data/curriculum.jsonl)
python -m chatbot.main build-corpus

# 2. (optional) Build + persist the retrieval index
python -m chatbot.main build-index

# 3a. Chat with the real LLM (needs model weights)
python -m chatbot.main chat

# 3b. Chat fully offline with no weights (extractive mock answers)
EDGEEDU_MODEL__ENABLED=false EDGEEDU_EMBEDDING__ENABLED=false \
  python -m chatbot.main chat

# 4. Or run the HTTP API
python -m chatbot.main serve         # POST /chat, GET /health
```

Example request:

```bash
curl -s localhost:8000/chat -H 'content-type: application/json' \
  -d '{"message":"What are thematic maps?","session_id":"demo"}'
```

## Configuration

All settings live in [config.yaml](config.yaml) and can be overridden by
environment variables (`EDGEEDU_` prefix, `__` for nesting):

| Key | Purpose |
| --- | --- |
| `model.enabled` / `model.name` | use real LLM vs mock; which model |
| `embedding.enabled` / `embedding.name` | dense retrieval on/off; which embedder |
| `retrieval.top_k`, `candidate_k`, `rrf_k`, `*_boost`, `multi_hop` | retrieval tuning |
| `history.max_turns`, `max_context_tokens`, `persist` | conversation memory |

## Layout

```
chatbot/
  main.py            Typer CLI (build-corpus | build-index | chat | serve)
  api.py             FastAPI app
  data_pipeline.py   data/**/*.json -> curriculum.jsonl (schema normalizer)
  content_index.py   hybrid BM25 + FAISS retrieval (RRF), persisted
  language_model.py  transformers wrapper + MockLanguageModel
  retrieval_qa.py    RAG orchestration + grounded prompt building
  chat_history.py    token-budgeted context window + JSONL persistence
  utils/
    config.py        pydantic-settings + YAML
    text_processing.py  Devanagari-safe tokenize / normalize / lang-detect
tests/               pytest suite (runs offline, BM25 + mock LM)
```

## Tests

```bash
pytest
```

The suite runs entirely offline (BM25 + mock LM, no downloads) and includes a
regression test for the Devanagari tokenization fix.

## Notes & limitations

- **Versions modernized** from the original spec (`torch>=2.2`,
  `transformers>=4.44`) so Gemma-2 can load; FAISS via `faiss-cpu`.
- **Hindi vs Marathi** detection is heuristic (shared Devanagari script); pass
  `--language` or set `default_language` when you know it.
- Real LLM generation can't be exercised in a weightless/offline CI; the mock
  path and prompt-construction tests cover the plumbing.

## License

Apache-2.0.
