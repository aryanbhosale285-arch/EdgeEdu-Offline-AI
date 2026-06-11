"""The shipped repo state must itself pass the pipeline's guarantees."""
import json
from pathlib import Path

from content_pipeline.manifest import MANIFEST_NAME, PUBLIC_KEY_NAME, verify_manifest
from content_pipeline.schema import validate_document
from content_pipeline.solutions import verify_check

ROOT = Path(__file__).resolve().parents[1]
DATA_FILES = sorted(
    p for p in (ROOT / "data").rglob("*.json") if p.name != MANIFEST_NAME
)


def test_all_data_files_are_valid_v3():
    assert len(DATA_FILES) == 33
    errors = []
    for path in DATA_FILES:
        doc = json.loads(path.read_text(encoding="utf-8"))
        errors += validate_document(doc, name=path.name)
    assert errors == []


def test_every_shipped_solution_step_is_verified():
    chunks_with_solutions = 0
    for path in DATA_FILES:
        doc = json.loads(path.read_text(encoding="utf-8"))
        for chunk in doc["content_chunks"]:
            for step in chunk.get("solution_steps", []):
                assert step["verified"] is True, f"{path.name} {chunk['chunk_id']}"
            if chunk.get("solution_steps"):
                chunks_with_solutions += 1
                assert "latex" in chunk
    assert chunks_with_solutions >= 13


def test_all_seed_checks_still_pass_in_sympy():
    for seed_path in sorted((ROOT / "content" / "solutions").glob("*.json")):
        seed = json.loads(seed_path.read_text(encoding="utf-8"))
        for sol in seed["solutions"]:
            verify_check(sol["check"])


def test_shipped_manifest_verifies():
    body = verify_manifest(ROOT, ROOT / "keys" / PUBLIC_KEY_NAME, installed_version=1)
    assert len(body["files"]) == 33
