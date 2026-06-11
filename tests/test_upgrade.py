import copy

import pytest

from content_pipeline.schema import validate_document
from content_pipeline.upgrade import UpgradeError, upgrade_document

LEGACY = {
    "metadata": {
        "file_name": "Legacy_File",
        "standard": "9",
        "subject": "Math Part-II",
        "language": "Marathi",
        "board": "Maharashtra State Board",
        "chapter_id": 0,
        "chapter_title": "Front Matter",
    },
    "content_chunks": [
        {"chunk_id": "1.1", "heading": "h", "keywords": ["k"], "text": "मूलभूत संकल्पना"}
    ],
    "metadata_chapter_2": {"chapter_id": 2, "chapter_title": "Ch 2"},
    "content_chunks_chapter_2": [
        {"chunk_id": "2.1", "heading": "h2", "keywords": [], "text": "समांतर रेषा"}
    ],
}


def test_upgrade_merges_fragments_and_normalizes():
    doc = upgrade_document(copy.deepcopy(LEGACY), today="2026-06-11")
    assert validate_document(doc) == []
    assert [c["chunk_id"] for c in doc["content_chunks"]] == ["1.1", "2.1"]
    assert doc["metadata"]["standard"] == 9
    assert doc["metadata"]["schema_version"] == "3.0"
    assert "chapter_id" not in doc["metadata"]
    chunk = doc["content_chunks"][0]
    assert chunk["difficulty"] == 3 and chunk["linked_concepts"] == []


def test_upgrade_is_idempotent():
    once = upgrade_document(copy.deepcopy(LEGACY), today="2026-06-11")
    twice = upgrade_document(copy.deepcopy(once), today="2026-06-11")
    assert once == twice


def test_upgrade_preserves_solutions_and_content_version():
    doc = upgrade_document(copy.deepcopy(LEGACY), today="2026-06-11")
    doc["metadata"]["content_version"] = 7
    doc["content_chunks"][0]["latex"] = "x = 2"
    doc["content_chunks"][0]["solution_steps"] = [
        {"text": "s", "latex": "x = 2", "verified": True}
    ]
    again = upgrade_document(copy.deepcopy(doc), today="2026-06-12")
    assert again["metadata"]["content_version"] == 7
    assert again["content_chunks"][0]["solution_steps"][0]["verified"] is True


def test_duplicate_resolved_by_language_script():
    legacy = copy.deepcopy(LEGACY)
    legacy["metadata"]["language"] = "English"
    legacy["content_chunks"] = [
        {"chunk_id": "7.1", "heading": "IDL", "keywords": [], "text": "The Earth rotates."},
        {"chunk_id": "7.1", "heading": "वाररेषा", "keywords": [], "text": "पृथ्वीच्या परिवलनामुळे"},
    ]
    del legacy["content_chunks_chapter_2"], legacy["metadata_chapter_2"]
    doc = upgrade_document(legacy, today="2026-06-11")
    assert len(doc["content_chunks"]) == 1
    assert doc["content_chunks"][0]["text"] == "The Earth rotates."


def test_unresolvable_duplicate_aborts():
    legacy = copy.deepcopy(LEGACY)
    legacy["metadata"]["language"] = "English"
    legacy["content_chunks"] = [
        {"chunk_id": "7.1", "heading": "a", "keywords": [], "text": "Both English."},
        {"chunk_id": "7.1", "heading": "b", "keywords": [], "text": "Also English."},
    ]
    del legacy["content_chunks_chapter_2"], legacy["metadata_chapter_2"]
    with pytest.raises(UpgradeError, match="duplicate chunk_id"):
        upgrade_document(legacy)


def test_unknown_top_level_key_aborts():
    legacy = copy.deepcopy(LEGACY)
    legacy["mystery_key"] = []
    with pytest.raises(UpgradeError, match="unrecognised"):
        upgrade_document(legacy)
