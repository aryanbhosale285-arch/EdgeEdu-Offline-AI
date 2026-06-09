"""Hybrid retrieval over the curriculum corpus.

Combines a lexical BM25 retriever (with keyword/heading field boosting, mirroring
the web ranker) and an optional dense retriever (sentence-transformers + FAISS),
fused with Reciprocal Rank Fusion. The dense half is optional: if the embedding
model or FAISS isn't available, the index transparently degrades to BM25-only so
the system still works fully offline.
"""
from __future__ import annotations

import pickle
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional

from rank_bm25 import BM25Okapi

from chatbot.data_pipeline import Chunk, load_corpus
from chatbot.utils.config import EmbeddingConfig, RetrievalConfig
from chatbot.utils.text_processing import tokenize

_META_FILE = "index_meta.pkl"
_FAISS_FILE = "dense.faiss"


@dataclass
class RetrievedChunk:
    chunk: Chunk
    score: float
    lexical_rank: Optional[int] = None
    dense_rank: Optional[int] = None


class Embedder:
    """Lazy wrapper around a sentence-transformers model. Optional dependency."""

    def __init__(self, config: EmbeddingConfig):
        self.config = config
        self._model = None

    @property
    def available(self) -> bool:
        if not self.config.enabled:
            return False
        try:
            self._ensure_loaded()
            return self._model is not None
        except Exception:
            return False

    def _ensure_loaded(self):
        if self._model is None:
            from sentence_transformers import SentenceTransformer  # lazy

            self._model = SentenceTransformer(self.config.name)

    def encode(self, texts: List[str], *, is_query: bool):
        import numpy as np

        self._ensure_loaded()
        prefix = (
            self.config.query_prefix if is_query else self.config.passage_prefix
        )
        prefixed = [f"{prefix}{t}" for t in texts]
        vecs = self._model.encode(
            prefixed,
            batch_size=self.config.batch_size,
            normalize_embeddings=True,
            show_progress_bar=False,
            convert_to_numpy=True,
        )
        return vecs.astype("float32", copy=False)


def _index_tokens(text: str) -> List[str]:
    return tokenize(text, remove_stopwords=True)


def _boosted_tokens(chunk: Chunk, retrieval: RetrievalConfig) -> List[str]:
    """Token list for BM25 with field boosting via repetition."""
    kw_boost = max(1, int(round(retrieval.keyword_boost)))
    head_boost = max(1, int(round(retrieval.heading_boost)))
    tokens: List[str] = []
    for kw in chunk.keywords:
        tokens.extend(_index_tokens(kw) * kw_boost)
    tokens.extend(_index_tokens(chunk.heading) * head_boost)
    tokens.extend(_index_tokens(chunk.text))
    return tokens


class ContentIndex:
    """Build / load / query the hybrid retrieval index."""

    def __init__(
        self,
        chunks: List[Chunk],
        retrieval: RetrievalConfig,
        embedder: Optional[Embedder] = None,
    ):
        self.chunks = chunks
        self.retrieval = retrieval
        self.embedder = embedder
        self._bm25 = BM25Okapi([_boosted_tokens(c, retrieval) for c in chunks])
        self._faiss = None  # built or loaded lazily

    # ---- construction ----

    @classmethod
    def build(
        cls,
        corpus_file: Path,
        retrieval: RetrievalConfig,
        embedder: Optional[Embedder] = None,
    ) -> "ContentIndex":
        chunks = load_corpus(corpus_file)
        index = cls(chunks, retrieval, embedder)
        if embedder is not None and embedder.available:
            index._build_dense()
        return index

    def _passage_text(self, chunk: Chunk) -> str:
        parts = [chunk.heading, chunk.text]
        if chunk.keywords:
            parts.append(" ".join(chunk.keywords))
        return " ".join(p for p in parts if p)

    def _build_dense(self):
        import faiss
        import numpy as np

        vecs = self.embedder.encode(
            [self._passage_text(c) for c in self.chunks], is_query=False
        )
        dim = vecs.shape[1]
        faiss_index = faiss.IndexFlatIP(dim)  # vectors are L2-normalized -> cosine
        faiss_index.add(vecs)
        self._faiss = faiss_index

    # ---- persistence ----

    def save(self, index_dir: Path):
        index_dir.mkdir(parents=True, exist_ok=True)
        meta = {
            "chunks": self.chunks,
            "embedding_model": (
                self.embedder.config.name if self.embedder else None
            ),
            "has_dense": self._faiss is not None,
        }
        with open(index_dir / _META_FILE, "wb") as fh:
            pickle.dump(meta, fh)
        if self._faiss is not None:
            import faiss

            faiss.write_index(self._faiss, str(index_dir / _FAISS_FILE))

    @classmethod
    def load(
        cls,
        index_dir: Path,
        retrieval: RetrievalConfig,
        embedder: Optional[Embedder] = None,
    ) -> "ContentIndex":
        with open(index_dir / _META_FILE, "rb") as fh:
            meta = pickle.load(fh)
        index = cls(meta["chunks"], retrieval, embedder)
        faiss_path = index_dir / _FAISS_FILE
        if meta.get("has_dense") and faiss_path.exists() and embedder is not None:
            import faiss

            index._faiss = faiss.read_index(str(faiss_path))
        return index

    # ---- query ----

    def _lexical_ranking(self, query: str, k: int) -> List[int]:
        scores = self._bm25.get_scores(tokenize(query, remove_stopwords=True))
        ranked = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)
        return [i for i in ranked if scores[i] > 0][:k]

    def _dense_ranking(self, query: str, k: int) -> List[int]:
        if self._faiss is None or self.embedder is None:
            return []
        try:
            qvec = self.embedder.encode([query], is_query=True)
            _, ids = self._faiss.search(qvec, k)
            return [int(i) for i in ids[0] if i != -1]
        except Exception:
            return []

    def search(self, query: str, k: Optional[int] = None) -> List[RetrievedChunk]:
        """Return the top-``k`` chunks using RRF over lexical + dense rankings."""
        if not query.strip():
            return []
        k = k or self.retrieval.top_k
        cand = self.retrieval.candidate_k
        rrf_k = self.retrieval.rrf_k

        lexical = self._lexical_ranking(query, cand)
        dense = self._dense_ranking(query, cand)

        fused: Dict[int, float] = {}
        lex_rank: Dict[int, int] = {}
        den_rank: Dict[int, int] = {}
        for rank, idx in enumerate(lexical):
            fused[idx] = fused.get(idx, 0.0) + 1.0 / (rrf_k + rank)
            lex_rank[idx] = rank
        for rank, idx in enumerate(dense):
            fused[idx] = fused.get(idx, 0.0) + 1.0 / (rrf_k + rank)
            den_rank[idx] = rank

        ordered = sorted(fused.items(), key=lambda kv: kv[1], reverse=True)[:k]
        return [
            RetrievedChunk(
                chunk=self.chunks[idx],
                score=score,
                lexical_rank=lex_rank.get(idx),
                dense_rank=den_rank.get(idx),
            )
            for idx, score in ordered
        ]

    @property
    def has_dense(self) -> bool:
        return self._faiss is not None

    def __len__(self) -> int:
        return len(self.chunks)
