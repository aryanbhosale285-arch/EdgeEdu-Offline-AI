import copy

from conftest import make_document
from content_pipeline.schema import validate_document


def test_valid_document_passes():
    assert validate_document(make_document()) == []


def test_solution_steps_validate():
    doc = make_document()
    doc["content_chunks"][0]["latex"] = "2x + 3 = 7"
    doc["content_chunks"][0]["solution_steps"] = [
        {"text": "Subtract 3.", "latex": "2x = 4", "verified": True}
    ]
    assert validate_document(doc) == []


def test_violations_are_reported():
    base = make_document()

    bad_language = copy.deepcopy(base)
    bad_language["metadata"]["language"] = "Tamil"
    assert any("language" in e for e in validate_document(bad_language))

    bad_difficulty = copy.deepcopy(base)
    bad_difficulty["content_chunks"][0]["difficulty"] = 9
    assert any("difficulty" in e for e in validate_document(bad_difficulty))

    duplicate = copy.deepcopy(base)
    duplicate["content_chunks"].append(copy.deepcopy(duplicate["content_chunks"][0]))
    assert any("duplicate chunk_id" in e for e in validate_document(duplicate))

    stray_key = copy.deepcopy(base)
    stray_key["content_chunks_chapter_2"] = []
    assert any("unexpected top-level keys" in e for e in validate_document(stray_key))

    unverified_step = copy.deepcopy(base)
    unverified_step["content_chunks"][0]["solution_steps"] = [{"text": "x", "latex": "x"}]
    assert any("verified" in e for e in validate_document(unverified_step))

    string_standard = copy.deepcopy(base)
    string_standard["metadata"]["standard"] = "9"
    assert any("standard" in e for e in validate_document(string_standard))
