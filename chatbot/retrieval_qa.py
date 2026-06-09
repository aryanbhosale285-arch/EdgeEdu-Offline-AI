"""Retrieval-augmented question answering.

Ties retrieval + generation together: retrieve the top-k curriculum chunks for a
query, build a grounded chat prompt (with citations and a language instruction),
and generate an answer. Supports an optional multi-hop pass that re-queries using
the first answer to pull in supporting chunks.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional, Sequence

from chatbot.content_index import ContentIndex, RetrievedChunk
from chatbot.language_model import BaseLanguageModel, Message
from chatbot.utils.config import RetrievalConfig
from chatbot.utils.text_processing import detect_language

SYSTEM_PROMPT = (
    "You are EdgeEdu, an offline study assistant for Maharashtra State Board "
    "students. Answer the student's question using ONLY the curriculum excerpts "
    "provided as context. Be clear and concise, and explain like a patient "
    "teacher. Cite the excerpts you use by their [uid]. If the answer is not in "
    "the provided excerpts, say you don't have that in the curriculum yet — do "
    "not invent facts. Always reply in the same language as the student's "
    "question ({language})."
)


@dataclass
class Citation:
    uid: str
    subject: str
    chapter_title: str
    heading: str
    language: str


@dataclass
class Answer:
    text: str
    citations: List[Citation]
    retrieved: List[RetrievedChunk]
    language: str


def _format_context(chunks: Sequence[RetrievedChunk]) -> str:
    blocks = []
    for rc in chunks:
        c = rc.chunk
        blocks.append(f"[{c.uid}] {c.subject} · {c.heading}\n{c.text}")
    return "\n\n".join(blocks)


def build_messages(
    query: str,
    chunks: Sequence[RetrievedChunk],
    language: str,
    history: Optional[Sequence[Message]] = None,
) -> List[Message]:
    """Assemble the chat messages sent to the language model."""
    messages: List[Message] = [
        {"role": "system", "content": SYSTEM_PROMPT.format(language=language)}
    ]
    if history:
        messages.extend(history)
    if chunks:
        context = _format_context(chunks)
        user = (
            f"Context:\n{context}\n\n"
            f"Question: {query}\n"
            f"Answer in {language}, citing the relevant [uid]s."
        )
    else:
        user = (
            f"Question: {query}\n"
            "(No matching curriculum excerpts were found.) "
            f"Answer in {language}."
        )
    messages.append({"role": "user", "content": user})
    return messages


class RetrievalQA:
    def __init__(
        self,
        index: ContentIndex,
        model: BaseLanguageModel,
        retrieval: RetrievalConfig,
        default_language: str = "English",
    ):
        self.index = index
        self.model = model
        self.retrieval = retrieval
        self.default_language = default_language

    def _retrieve(self, query: str) -> List[RetrievedChunk]:
        chunks = self.index.search(query, self.retrieval.top_k)
        if self.retrieval.multi_hop and chunks:
            # Second hop: expand the query with the top headings to surface
            # supporting material, then merge (dedup by uid, keep best score).
            expansion = " ".join(c.chunk.heading for c in chunks[:2])
            extra = self.index.search(f"{query} {expansion}", self.retrieval.top_k)
            seen = {c.chunk.uid for c in chunks}
            for rc in extra:
                if rc.chunk.uid not in seen:
                    chunks.append(rc)
                    seen.add(rc.chunk.uid)
            chunks = chunks[: self.retrieval.top_k]
        return chunks

    def answer(
        self,
        query: str,
        history: Optional[Sequence[Message]] = None,
        language: Optional[str] = None,
    ) -> Answer:
        lang = language or detect_language(query) or self.default_language
        chunks = self._retrieve(query)
        messages = build_messages(query, chunks, lang, history)
        text = self.model.generate(messages, context=chunks)
        citations = [
            Citation(
                uid=rc.chunk.uid,
                subject=rc.chunk.subject,
                chapter_title=rc.chunk.chapter_title,
                heading=rc.chunk.heading,
                language=rc.chunk.language,
            )
            for rc in chunks
        ]
        return Answer(
            text=text, citations=citations, retrieved=chunks, language=lang
        )
