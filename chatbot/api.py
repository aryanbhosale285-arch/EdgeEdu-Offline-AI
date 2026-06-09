"""FastAPI surface for the chatbot.

Exposes a minimal HTTP API so the same RAG engine can serve a web/mobile client:

    GET  /health
    POST /chat   {"message": "...", "session_id": "...", "language": "..."}
"""
from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import List, Optional

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from chatbot.chat_history import ConversationHistory
from chatbot.main import create_qa
from chatbot.utils.config import get_settings

app = FastAPI(title="EdgeEdu Offline AI Chatbot", version="0.1.0")

# Allow the Next.js frontend to call the chatbot directly (fallback if proxy
# isn't used). In production you'd lock this down to specific origins.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class ChatRequest(BaseModel):
    message: str
    session_id: str = "default"
    language: Optional[str] = None


class CitationModel(BaseModel):
    uid: str
    subject: str
    chapter_title: str
    heading: str
    language: str


class ChatResponse(BaseModel):
    answer: str
    language: str
    citations: List[CitationModel]


@lru_cache(maxsize=1)
def _qa():
    # Built once on first request (loading the model/index can be slow).
    return create_qa(get_settings())


@app.get("/health")
def health():
    qa = _qa()
    return {
        "status": "ok",
        "chunks": len(qa.index),
        "dense_retrieval": qa.index.has_dense,
    }


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    settings = get_settings()
    qa = _qa()
    history = ConversationHistory(
        settings.history, req.session_id, Path(settings.paths.history_dir)
    )
    result = qa.answer(req.message, history.context_messages(), req.language)
    history.add_user(req.message, [c.uid for c in result.citations])
    history.add_assistant(result.text)
    return ChatResponse(
        answer=result.text,
        language=result.language,
        citations=[CitationModel(**vars(c)) for c in result.citations],
    )
