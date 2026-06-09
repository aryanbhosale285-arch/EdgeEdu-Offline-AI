from chatbot.content_index import ContentIndex, RetrievedChunk
from chatbot.language_model import MockLanguageModel
from chatbot.retrieval_qa import RetrievalQA, build_messages
from chatbot.utils.config import RetrievalConfig


def _qa(chunks):
    index = ContentIndex(chunks, RetrievalConfig(), embedder=None)
    return RetrievalQA(index, MockLanguageModel(), RetrievalConfig())


def test_build_messages_structure(sample_chunks):
    retrieved = [RetrievedChunk(chunk=sample_chunks[0], score=1.0)]
    msgs = build_messages("what are thematic maps?", retrieved, "English")
    assert msgs[0]["role"] == "system"
    assert "English" in msgs[0]["content"]
    assert msgs[-1]["role"] == "user"
    # The retrieved chunk's text must be present in the grounded context.
    assert "Thematic maps" in msgs[-1]["content"]
    assert sample_chunks[0].uid in msgs[-1]["content"]


def test_build_messages_no_context():
    msgs = build_messages("unknown topic", [], "Hindi")
    assert "No matching curriculum excerpts" in msgs[-1]["content"]


def test_answer_end_to_end_with_mock(sample_chunks):
    qa = _qa(sample_chunks)
    ans = qa.answer("what are thematic maps?", language="English")
    assert ans.citations
    assert ans.citations[0].uid == "9-geo-en#1.1"
    # Mock composes an extractive answer from the top chunk.
    assert "Thematic maps" in ans.text
    assert "sources:" in ans.text


def test_answer_detects_language(sample_chunks):
    qa = _qa(sample_chunks)
    ans = qa.answer("वितरण के मानचित्र क्या हैं?")
    assert ans.language in {"Hindi", "Marathi"}
