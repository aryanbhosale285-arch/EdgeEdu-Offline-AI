"""Build the normalized curriculum corpus (``data/curriculum.jsonl``).

Reads every ``data/**/*.json`` file, cleans up the slightly inconsistent
on-disk schema, and emits one JSON object per line. Ports the normalization
logic from the web prototype's ``app/lib/server/curriculum-loader.ts`` —
including the quirk that some files (e.g. ``9th_Math 2_Marathi.json``) pack
multiple chapters under ``content_chunks``, ``content_chunks_chapter_2`` … keys.
"""
from __future__ import annotations

import json
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Dict, Iterable, List

from chatbot.utils.text_processing import normalize, tokenize


@dataclass
class Chunk:
    uid: str            # globally unique: "{doc_id}#{chunk_id}"
    doc_id: str
    chunk_id: str
    standard: str
    subject: str
    language: str
    board: str
    chapter_id: int
    chapter_title: str
    heading: str
    keywords: List[str]
    text: str
    tokens: List[str] = field(default_factory=list)


def _slugify(value: str) -> str:
    out = []
    for ch in value.lower():
        if ch.isalnum() and ch.isascii():
            out.append(ch)
        else:
            out.append("-")
    slug = "".join(out)
    while "--" in slug:
        slug = slug.replace("--", "-")
    return slug.strip("-")


def _normalize_language(raw: str | None) -> str:
    v = (raw or "").lower()
    if v.startswith("hin"):
        return "Hindi"
    if v.startswith("mar"):
        return "Marathi"
    return "English"


def _chapter_from_chunk_id(chunk_id: str, fallback: int) -> int:
    head = chunk_id.split(".")[0]
    try:
        return int(head)
    except (ValueError, TypeError):
        return fallback


def _collect_raw_chunks(doc: dict) -> List[dict]:
    """Gather every array whose key starts with ``content_chunks``."""
    chunks: List[dict] = []
    for key, value in doc.items():
        if key.startswith("content_chunks") and isinstance(value, list):
            chunks.extend(value)
    return chunks


def iter_documents(data_dir: Path) -> Iterable[Chunk]:
    """Yield normalized :class:`Chunk` objects from all JSON files in ``data_dir``."""
    used_ids: Dict[str, int] = {}
    files = sorted(p for p in data_dir.rglob("*.json") if p.name != "curriculum.jsonl")

    for path in files:
        try:
            doc = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            print(f"[data_pipeline] skipping invalid JSON: {path} ({exc})")
            continue

        meta = doc.get("metadata", {}) or {}
        standard = str(meta.get("standard", "")).strip() or "Unknown"
        subject = (meta.get("subject") or "Unknown subject").strip()
        language = _normalize_language(meta.get("language"))
        doc_chapter_id = int(meta.get("chapter_id", 1) or 1)

        base_id = _slugify(f"{standard}-{subject}-{language}") or _slugify(path.stem)
        doc_id = base_id
        if base_id in used_ids:
            used_ids[base_id] += 1
            doc_id = f"{base_id}-{used_ids[base_id]}"
        else:
            used_ids[base_id] = 1

        for raw in _collect_raw_chunks(doc):
            if not isinstance(raw, dict):
                continue
            text = normalize(raw.get("text"))
            heading = normalize(raw.get("heading"))
            if not text and not heading:
                continue
            chunk_id = str(raw.get("chunk_id", "")).strip() or "0.0"
            keywords = [k for k in (raw.get("keywords") or []) if k]
            # Index tokens over keywords + heading + text (keyword/heading
            # boosting is applied later by the BM25 retriever).
            token_source = " ".join([" ".join(keywords), heading, text])
            yield Chunk(
                uid=f"{doc_id}#{chunk_id}",
                doc_id=doc_id,
                chunk_id=chunk_id,
                standard=standard,
                subject=subject,
                language=language,
                board=(meta.get("board") or "Maharashtra State Board").strip(),
                chapter_id=_chapter_from_chunk_id(chunk_id, doc_chapter_id),
                chapter_title=normalize(meta.get("chapter_title") or subject),
                heading=heading,
                keywords=keywords,
                text=text,
                tokens=tokenize(token_source),
            )


def build_corpus(data_dir: Path, corpus_file: Path) -> int:
    """Write the JSONL corpus and return the number of chunks written."""
    corpus_file.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    with open(corpus_file, "w", encoding="utf-8") as fh:
        for chunk in iter_documents(data_dir):
            fh.write(json.dumps(asdict(chunk), ensure_ascii=False) + "\n")
            count += 1
    return count


def load_corpus(corpus_file: Path) -> List[Chunk]:
    """Read the JSONL corpus back into :class:`Chunk` objects."""
    chunks: List[Chunk] = []
    with open(corpus_file, "r", encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            chunks.append(Chunk(**json.loads(line)))
    return chunks
