import json

import pytest

from conftest import make_document
from content_pipeline.solutions import VerificationError, apply_seed_file, verify_check


def test_solve_system_passes():
    verify_check(
        {
            "type": "solve_system",
            "equations": ["15*x + 17*y - 21", "17*x + 15*y - 11"],
            "answer": {"x": "-2", "y": "3"},
        }
    )


def test_solve_system_rejects_wrong_answer():
    with pytest.raises(VerificationError):
        verify_check(
            {
                "type": "solve_system",
                "equations": ["x + y - 25"],
                "answer": {"x": "10", "y": "10"},
            }
        )


def test_roots_passes_and_rejects():
    verify_check({"type": "roots", "poly": "x**2 - 4*x - 5", "roots": ["5", "-1"]})
    with pytest.raises(VerificationError):
        verify_check({"type": "roots", "poly": "x**2 - 4*x - 5", "roots": ["5", "2"]})
    with pytest.raises(VerificationError, match="roots"):
        verify_check({"type": "roots", "poly": "x**2 - 4*x - 5", "roots": ["5"]})


def test_roots_sum_product():
    verify_check(
        {"type": "roots_sum_product", "poly": "2*x**2 + 6*x - 5", "sum": "-3", "product": "-5/2"}
    )
    with pytest.raises(VerificationError):
        verify_check(
            {"type": "roots_sum_product", "poly": "2*x**2 + 6*x - 5", "sum": "3", "product": "-5/2"}
        )


def test_value_handles_decimals_exactly():
    verify_check({"type": "value", "expr": "2/5", "expected": "0.4"})
    with pytest.raises(VerificationError):
        verify_check({"type": "value", "expr": "2/5", "expected": "0.41"})


def test_identity():
    verify_check({"type": "identity", "lhs": "sin(t)**2 + cos(t)**2", "rhs": "1"})
    with pytest.raises(VerificationError):
        verify_check({"type": "identity", "lhs": "sin(t) + cos(t)", "rhs": "1"})


def test_unknown_check_type_rejected():
    with pytest.raises(VerificationError, match="unknown check type"):
        verify_check({"type": "trust_me"})


def test_apply_seed_file_attaches_verified_steps(tmp_path):
    data_file = tmp_path / "data.json"
    data_file.write_text(json.dumps(make_document()), encoding="utf-8")
    seed = {
        "target_file": "data.json",
        "solutions": [
            {
                "chunk_id": "1.1",
                "problem_latex": "2x + 3 = 7",
                "check": {
                    "type": "solve_system",
                    "equations": ["2*x + 3 - 7"],
                    "answer": {"x": "2"},
                },
                "steps": [{"text": "Subtract 3, divide by 2.", "latex": "x = 2"}],
            }
        ],
    }
    seed_file = tmp_path / "seed.json"
    seed_file.write_text(json.dumps(seed), encoding="utf-8")

    assert apply_seed_file(seed_file, tmp_path) == 1
    doc = json.loads(data_file.read_text(encoding="utf-8"))
    chunk = doc["content_chunks"][0]
    assert chunk["latex"] == "2x + 3 = 7"
    assert chunk["solution_steps"] == [
        {"text": "Subtract 3, divide by 2.", "latex": "x = 2", "verified": True}
    ]


def test_apply_seed_file_rejects_bad_maths(tmp_path):
    data_file = tmp_path / "data.json"
    data_file.write_text(json.dumps(make_document()), encoding="utf-8")
    seed = {
        "target_file": "data.json",
        "solutions": [
            {
                "chunk_id": "1.1",
                "check": {
                    "type": "solve_system",
                    "equations": ["2*x + 3 - 7"],
                    "answer": {"x": "3"},
                },
                "steps": [{"text": "wrong", "latex": "x = 3"}],
            }
        ],
    }
    seed_file = tmp_path / "seed.json"
    seed_file.write_text(json.dumps(seed), encoding="utf-8")

    with pytest.raises(VerificationError):
        apply_seed_file(seed_file, tmp_path)
    # the target file must be untouched after a failed verification
    assert "solution_steps" not in json.loads(data_file.read_text(encoding="utf-8"))[
        "content_chunks"
    ][0]
