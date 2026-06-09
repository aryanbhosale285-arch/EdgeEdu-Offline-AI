"""Conversation history with a token-budgeted context window.

Stores each turn (user / assistant) plus the chunk uids that were retrieved for
it, exposes a trimmed message list for prompting, and persists per-session
history to JSONL on disk for long-term memory.
"""
from __future__ import annotations

import json
import time
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import List, Optional

from chatbot.language_model import Message
from chatbot.utils.config import HistoryConfig


@dataclass
class Turn:
    role: str               # "user" | "assistant"
    content: str
    retrieved_uids: List[str] = field(default_factory=list)
    timestamp: float = field(default_factory=time.time)


def _estimate_tokens(text: str) -> int:
    """Cheap, model-agnostic token estimate (~4 chars/token)."""
    return max(1, len(text) // 4)


class ConversationHistory:
    def __init__(
        self,
        config: HistoryConfig,
        session_id: str = "default",
        history_dir: Optional[Path] = None,
    ):
        self.config = config
        self.session_id = session_id
        self.history_dir = history_dir
        self.turns: List[Turn] = []
        if config.persist and history_dir is not None:
            self._load()

    # ---- mutation ----

    def add_user(self, content: str, retrieved_uids: Optional[List[str]] = None):
        self._append(Turn("user", content, retrieved_uids or []))

    def add_assistant(self, content: str):
        self._append(Turn("assistant", content))

    def _append(self, turn: Turn):
        self.turns.append(turn)
        if self.config.persist and self.history_dir is not None:
            self._persist_turn(turn)

    # ---- context window ----

    def context_messages(self) -> List[Message]:
        """Most-recent turns that fit the token + turn-count budget."""
        selected: List[Turn] = []
        budget = self.config.max_context_tokens
        for turn in reversed(self.turns[-2 * self.config.max_turns:]):
            cost = _estimate_tokens(turn.content)
            if budget - cost < 0 and selected:
                break
            budget -= cost
            selected.append(turn)
            if len(selected) >= 2 * self.config.max_turns:
                break
        selected.reverse()
        return [{"role": t.role, "content": t.content} for t in selected]

    # ---- persistence ----

    def _session_file(self) -> Path:
        assert self.history_dir is not None
        self.history_dir.mkdir(parents=True, exist_ok=True)
        return self.history_dir / f"{self.session_id}.jsonl"

    def _persist_turn(self, turn: Turn):
        with open(self._session_file(), "a", encoding="utf-8") as fh:
            fh.write(json.dumps(asdict(turn), ensure_ascii=False) + "\n")

    def _load(self):
        path = self._session_file()
        if not path.exists():
            return
        with open(path, "r", encoding="utf-8") as fh:
            for line in fh:
                line = line.strip()
                if line:
                    self.turns.append(Turn(**json.loads(line)))

    def clear(self):
        self.turns = []
        if self.history_dir is not None:
            path = self._session_file()
            if path.exists():
                path.unlink()
