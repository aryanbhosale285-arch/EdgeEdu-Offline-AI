import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))


def make_document(**overrides):
    """A minimal valid v3.0 document for tests."""
    doc = {
        "metadata": {
            "file_name": "Test_File",
            "standard": 10,
            "subject": "Mathematics Part-I",
            "language": "English",
            "board": "Maharashtra State Board",
            "schema_version": "3.0",
            "content_version": 1,
            "last_updated": "2026-06-11",
        },
        "content_chunks": [
            {
                "chunk_id": "1.1",
                "heading": "Linear equations",
                "keywords": ["linear"],
                "text": "An equation of degree one.",
                "difficulty": 2,
                "importance": 5,
                "linked_concepts": [],
                "prerequisites": [],
            }
        ],
        "generation_status": "complete",
    }
    doc.update(overrides)
    return doc
