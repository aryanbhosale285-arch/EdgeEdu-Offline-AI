"""v3.0 curriculum document schema and validation.

A document is one JSON file per (standard, subject, language):

    {
      "metadata": {
        "file_name": str, "standard": 9|10, "subject": str,
        "language": "English"|"Hindi"|"Marathi", "board": str,
        "schema_version": "3.0", "content_version": int >= 1,
        "last_updated": "YYYY-MM-DD"
      },
      "content_chunks": [{
        "chunk_id": str (unique), "heading": str, "keywords": [str],
        "text": str, "difficulty": 1..5, "importance": 1..5,
        "linked_concepts": [str], "prerequisites": [str],
        # optional, Math only:
        "latex": str,
        "solution_steps": [{"text": str, "latex": str, "verified": bool}]
      }],
      "generation_status": "complete"|"partial"
    }
"""
from __future__ import annotations

import re

SCHEMA_VERSION = "3.0"
LANGUAGES = {"English", "Hindi", "Marathi"}
STANDARDS = {9, 10}
GENERATION_STATUSES = {"complete", "partial"}

_DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")

_TOP_KEYS = {"metadata", "content_chunks", "generation_status"}
_META_REQUIRED = {
    "file_name": str,
    "subject": str,
    "language": str,
    "board": str,
    "schema_version": str,
    "content_version": int,
    "last_updated": str,
}
_CHUNK_REQUIRED = {
    "chunk_id": str,
    "heading": str,
    "text": str,
    "difficulty": int,
    "importance": int,
}
_CHUNK_OPTIONAL = {"keywords", "linked_concepts", "prerequisites", "latex", "solution_steps"}
_STEP_REQUIRED = {"text": str, "latex": str, "verified": bool}


def _check_str_list(value, label: str, errors: list[str]) -> None:
    if not isinstance(value, list) or not all(isinstance(v, str) for v in value):
        errors.append(f"{label} must be a list of strings")


def _validate_chunk(chunk: dict, idx: int, errors: list[str]) -> None:
    label = f"content_chunks[{idx}]"
    if not isinstance(chunk, dict):
        errors.append(f"{label} must be an object")
        return
    unknown = set(chunk) - set(_CHUNK_REQUIRED) - _CHUNK_OPTIONAL
    if unknown:
        errors.append(f"{label} has unknown keys: {sorted(unknown)}")
    for key, typ in _CHUNK_REQUIRED.items():
        if not isinstance(chunk.get(key), typ):
            errors.append(f"{label}.{key} missing or not {typ.__name__}")
    for key in ("difficulty", "importance"):
        value = chunk.get(key)
        if isinstance(value, int) and not 1 <= value <= 5:
            errors.append(f"{label}.{key} must be in 1..5, got {value}")
    for key in ("keywords", "linked_concepts", "prerequisites"):
        if key in chunk:
            _check_str_list(chunk[key], f"{label}.{key}", errors)
    if "latex" in chunk and not isinstance(chunk["latex"], str):
        errors.append(f"{label}.latex must be a string")
    if "solution_steps" in chunk:
        steps = chunk["solution_steps"]
        if not isinstance(steps, list):
            errors.append(f"{label}.solution_steps must be a list")
            return
        for j, step in enumerate(steps):
            if not isinstance(step, dict):
                errors.append(f"{label}.solution_steps[{j}] must be an object")
                continue
            for key, typ in _STEP_REQUIRED.items():
                if not isinstance(step.get(key), typ):
                    errors.append(
                        f"{label}.solution_steps[{j}].{key} missing or not {typ.__name__}"
                    )
            unknown = set(step) - set(_STEP_REQUIRED)
            if unknown:
                errors.append(f"{label}.solution_steps[{j}] has unknown keys: {sorted(unknown)}")


def validate_document(doc: dict, name: str = "<doc>") -> list[str]:
    """Return a list of human-readable schema violations (empty = valid)."""
    errors: list[str] = []
    if not isinstance(doc, dict):
        return [f"{name}: document must be an object"]

    unknown = set(doc) - _TOP_KEYS
    if unknown:
        errors.append(f"unexpected top-level keys: {sorted(unknown)}")
    missing = _TOP_KEYS - set(doc)
    if missing:
        errors.append(f"missing top-level keys: {sorted(missing)}")

    meta = doc.get("metadata")
    if isinstance(meta, dict):
        for key, typ in _META_REQUIRED.items():
            if not isinstance(meta.get(key), typ):
                errors.append(f"metadata.{key} missing or not {typ.__name__}")
        if meta.get("schema_version") != SCHEMA_VERSION:
            errors.append(f"metadata.schema_version must be {SCHEMA_VERSION!r}")
        if meta.get("standard") not in STANDARDS:
            errors.append(f"metadata.standard must be one of {sorted(STANDARDS)}")
        if meta.get("language") not in LANGUAGES:
            errors.append(f"metadata.language must be one of {sorted(LANGUAGES)}")
        if isinstance(meta.get("content_version"), int) and meta["content_version"] < 1:
            errors.append("metadata.content_version must be >= 1")
        if isinstance(meta.get("last_updated"), str) and not _DATE_RE.match(meta["last_updated"]):
            errors.append("metadata.last_updated must be YYYY-MM-DD")
    elif "metadata" in doc:
        errors.append("metadata must be an object")

    chunks = doc.get("content_chunks")
    if isinstance(chunks, list):
        seen: set[str] = set()
        for i, chunk in enumerate(chunks):
            _validate_chunk(chunk, i, errors)
            cid = chunk.get("chunk_id") if isinstance(chunk, dict) else None
            if isinstance(cid, str):
                if cid in seen:
                    errors.append(f"duplicate chunk_id {cid!r}")
                seen.add(cid)
    elif "content_chunks" in doc:
        errors.append("content_chunks must be a list")

    if "generation_status" in doc and doc["generation_status"] not in GENERATION_STATUSES:
        errors.append(f"generation_status must be one of {sorted(GENERATION_STATUSES)}")

    return [f"{name}: {e}" for e in errors]
