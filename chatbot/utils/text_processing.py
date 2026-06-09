"""Text utilities: tokenization, normalization, and language detection.

The tokenizer is deliberately dependency-light and **Devanagari-safe**: vowel
signs (मात्रा) belong to the Unicode *Mark* category (\\p{M}), not *Letter*, so a
naive ``\\w+`` / ``[^\\p{L}\\p{N}]+`` split would break words like "वितरण" mid-
character. This mirrors (and ports) the fix made in the web prototype's
``app/lib/search-utils.ts``.
"""
from __future__ import annotations

from typing import List, Optional

import regex  # third-party `regex` module: supports \p{...} Unicode classes

# Split on any run of characters that are NOT letters, numbers, or combining
# marks. Keeping \p{M} preserves Devanagari matras attached to their consonant.
_TOKEN_SPLIT = regex.compile(r"[^\p{L}\p{N}\p{M}]+", regex.UNICODE)

# Devanagari block (covers Hindi & Marathi).
_DEVANAGARI = regex.compile(r"[\p{Block=Devanagari}]")

# A few high-signal Marathi-only function words / markers to nudge hi-vs-mr
# disambiguation. This is a heuristic, not a classifier (see detect_language).
_MARATHI_HINTS = (
    "आणि",  # "and"
    "आहे",  # "is"
    "मध्ये",  # "in"
    "नाही",  # "no/not"
    "त्या",
    "च्या",
    "ला",
)


def tokenize(text: Optional[str], min_len: int = 2) -> List[str]:
    """Lowercase and split text into Unicode-aware tokens.

    Tokens shorter than ``min_len`` characters are dropped as low-signal.
    """
    if not text:
        return []
    return [t for t in _TOKEN_SPLIT.split(text.lower()) if len(t) >= min_len]


def normalize(text: Optional[str]) -> str:
    """Collapse whitespace and trim. Safe on ``None``."""
    if not text:
        return ""
    return regex.sub(r"\s+", " ", text).strip()


def detect_language(text: Optional[str]) -> str:
    """Best-effort language detection.

    Returns one of ``"English"``, ``"Hindi"``, ``"Marathi"``. Hindi and Marathi
    share the Devanagari script, so the split between them is heuristic: we look
    for a few Marathi-specific markers and otherwise default to Hindi. Callers
    that know the user's preferred language should prefer that over this guess.
    """
    if not text:
        return "English"
    devanagari = _DEVANAGARI.findall(text)
    if len(devanagari) < max(2, len(text) * 0.15):
        return "English"
    lowered = text
    if any(hint in lowered for hint in _MARATHI_HINTS):
        return "Marathi"
    return "Hindi"


def lemmatize(tokens: List[str], language: str = "English") -> List[str]:
    """Optional lemmatization via spaCy if installed; otherwise a no-op.

    Kept behind a lazy import so spaCy remains an optional extra — the chatbot
    runs fine without it.
    """
    try:  # pragma: no cover - exercised only when spaCy is installed
        import spacy  # type: ignore

        model = {"English": "en_core_web_sm"}.get(language)
        if not model:
            return tokens
        nlp = _get_spacy(model)
        if nlp is None:
            return tokens
        doc = nlp(" ".join(tokens))
        return [tok.lemma_.lower() for tok in doc if tok.lemma_.strip()]
    except Exception:
        return tokens


_SPACY_CACHE: dict = {}


def _get_spacy(model: str):  # pragma: no cover - optional path
    if model in _SPACY_CACHE:
        return _SPACY_CACHE[model]
    try:
        import spacy  # type: ignore

        nlp = spacy.load(model, disable=["parser", "ner"])
    except Exception:
        nlp = None
    _SPACY_CACHE[model] = nlp
    return nlp
