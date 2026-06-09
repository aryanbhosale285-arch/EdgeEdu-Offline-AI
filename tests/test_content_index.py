from chatbot.content_index import ContentIndex
from chatbot.utils.config import RetrievalConfig


def _index(chunks):
    # No embedder -> BM25-only path (offline-friendly, no model download).
    return ContentIndex(chunks, RetrievalConfig(), embedder=None)


def test_english_query_retrieves_english_chunk(sample_chunks):
    index = _index(sample_chunks)
    hits = index.search("thematic maps", k=3)
    assert hits
    assert hits[0].chunk.uid == "9-geo-en#1.1"


def test_devanagari_query_retrieves_hindi_chunk(sample_chunks):
    index = _index(sample_chunks)
    hits = index.search("वितरण मानचित्र", k=3)
    assert hits
    assert hits[0].chunk.language == "Hindi"


def test_cross_lingual_via_keywords(sample_chunks):
    # "parallel lines" should surface the math chunk.
    index = _index(sample_chunks)
    hits = index.search("parallel lines", k=3)
    assert hits[0].chunk.subject == "Mathematics"


def test_empty_query_returns_nothing(sample_chunks):
    assert _index(sample_chunks).search("   ") == []


def test_persistence_roundtrip(tmp_path, sample_chunks):
    index = _index(sample_chunks)
    index.save(tmp_path)
    loaded = ContentIndex.load(tmp_path, RetrievalConfig(), embedder=None)
    assert len(loaded) == len(sample_chunks)
    assert loaded.search("parallel lines")[0].chunk.subject == "Mathematics"


def test_build_from_corpus_file(sample_corpus_file):
    index = ContentIndex.build(sample_corpus_file, RetrievalConfig(), embedder=None)
    assert len(index) == 3
    assert not index.has_dense  # no embedder -> BM25 only
