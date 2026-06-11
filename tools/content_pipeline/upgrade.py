"""Upgrade legacy (v2-era) curriculum JSON to the v3.0 schema.

Handles two legacy quirks:
- fragmented files where chapters live in ``content_chunks_chapter_N`` /
  ``metadata_chapter_N`` top-level keys (merged back into one chunk list);
- per-file ``metadata.chapter_id`` / ``chapter_title`` leftovers from the
  original chunking run (dropped — chunk_ids already encode the chapter).
"""
from __future__ import annotations

import datetime
import json
import re
from pathlib import Path

from .schema import SCHEMA_VERSION

_FRAGMENT_CHUNKS = re.compile(r"^content_chunks_chapter_(\d+)$")
_FRAGMENT_META = re.compile(r"^metadata_chapter_\d+$")

DEFAULT_DIFFICULTY = 3
DEFAULT_IMPORTANCE = 3


class UpgradeError(ValueError):
    pass


def _devanagari_ratio(text: str) -> float:
    visible = [ch for ch in text if not ch.isspace()]
    if not visible:
        return 0.0
    return sum("ऀ" <= ch <= "ॿ" for ch in visible) / len(visible)


def _script_matches(language: str, text: str) -> bool:
    ratio = _devanagari_ratio(text)
    return ratio < 0.15 if language == "English" else ratio > 0.15


def _dedupe_chunks(chunks: list[dict], language: str, name: str) -> list[dict]:
    """Resolve duplicate chunk_ids by keeping the chunk written in the
    document's declared language (a known extraction defect left stray
    Marathi chunks inside an English file). Ambiguous duplicates abort."""
    by_id: dict[str, list[dict]] = {}
    for chunk in chunks:
        by_id.setdefault(chunk["chunk_id"], []).append(chunk)
    out = []
    for cid, group in by_id.items():
        if len(group) > 1:
            group = [c for c in group if _script_matches(language, c["text"])]
            if len(group) != 1:
                raise UpgradeError(
                    f"{name}: duplicate chunk_id {cid!r} cannot be resolved by language script"
                )
        out.append(group[0])
    return out


def _upgrade_chunk(chunk: dict) -> dict:
    out = {
        "chunk_id": chunk["chunk_id"],
        "heading": chunk["heading"],
        "keywords": chunk.get("keywords", []),
        "text": chunk["text"],
        "difficulty": chunk.get("difficulty", DEFAULT_DIFFICULTY),
        "importance": chunk.get("importance", DEFAULT_IMPORTANCE),
        "linked_concepts": chunk.get("linked_concepts", []),
        "prerequisites": chunk.get("prerequisites", []),
    }
    for key in ("latex", "solution_steps"):
        if key in chunk:
            out[key] = chunk[key]
    return out


def upgrade_document(doc: dict, name: str = "<doc>", today: str | None = None) -> dict:
    today = today or datetime.date.today().isoformat()

    chunks = list(doc.get("content_chunks", []))
    fragments = sorted(
        (int(m.group(1)), key)
        for key in doc
        if (m := _FRAGMENT_CHUNKS.match(key))
    )
    for _, key in fragments:
        chunks.extend(doc[key])

    stray = [
        key for key in doc
        if key not in ("metadata", "content_chunks", "generation_status")
        and not _FRAGMENT_CHUNKS.match(key) and not _FRAGMENT_META.match(key)
    ]
    if stray:
        raise UpgradeError(f"{name}: unrecognised top-level keys {stray}")

    chunks = _dedupe_chunks(chunks, doc["metadata"].get("language", ""), name)

    meta = dict(doc["metadata"])
    meta.pop("chapter_id", None)
    meta.pop("chapter_title", None)
    meta["standard"] = int(meta["standard"])
    meta["schema_version"] = SCHEMA_VERSION
    meta["content_version"] = meta.get("content_version", 1)
    meta["last_updated"] = today

    return {
        "metadata": meta,
        "content_chunks": [_upgrade_chunk(c) for c in chunks],
        "generation_status": doc.get("generation_status", "complete"),
    }


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def dump_json(path: Path, doc: dict) -> None:
    path.write_text(
        json.dumps(doc, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


def upgrade_file(path: Path, today: str | None = None) -> dict:
    doc = upgrade_document(load_json(path), name=path.name, today=today)
    dump_json(path, doc)
    return doc
