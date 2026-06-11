"""SymPy verification of seeded worked solutions, and attachment to chunks.

Seed files live in ``content/solutions/*.json``:

    {
      "target_file": "data/10th/10th_Mathematics 1_English.json",
      "solutions": [{
        "chunk_id": "1.3",
        "problem_latex": "...",            # optional, becomes chunk.latex
        "difficulty": 3, "importance": 5,  # optional overrides
        "check": {...},                    # machine-checkable spec, see below
        "steps": [{"text": "...", "latex": "..."}]
      }]
    }

Every solution must pass its ``check`` with SymPy before it is written into
the curriculum; steps are then stored with ``verified: true``. A failing
check aborts the build — unverified maths never ships.

Check types:
- solve_system: {"equations": ["15*x + 17*y - 21", ...], "answer": {"x": "-2"}}
  (each expression must vanish at the answer, and the answer must agree with
  SymPy's own solution of the system)
- roots:        {"poly": "x**2 + 5*x - 6", "roots": ["-6", "1"]}
  (must be exactly the root set SymPy finds)
- roots_sum_product: {"poly": "2*x**2 + 6*x - 5", "sum": "-3", "product": "-5/2"}
- value:        {"expr": "5 + (100 - 1)*3", "expected": "302"}
- values:       {"items": [<value dicts>]}
- identity:     {"lhs": "sin(t)**2 + cos(t)**2", "rhs": "1"}
"""
from __future__ import annotations

import json
from pathlib import Path

import sympy
from sympy import simplify, sympify

from .upgrade import dump_json, load_json


class VerificationError(ValueError):
    pass


def _expr(s: str):
    return sympify(s, rational=True)


def _is_zero(expr) -> bool:
    return simplify(expr) == 0


def _check_solve_system(check: dict) -> None:
    equations = [_expr(e) for e in check["equations"]]
    answer = {sympy.Symbol(k): _expr(v) for k, v in check["answer"].items()}
    for raw, eq in zip(check["equations"], equations):
        if not _is_zero(eq.subs(answer)):
            raise VerificationError(f"answer does not satisfy {raw!r}")
    solved = sympy.solve(equations, list(answer), dict=True)
    if not any(
        all(_is_zero(sol.get(var, sympy.nan) - val) for var, val in answer.items())
        for sol in solved
    ):
        raise VerificationError(f"SymPy's solution differs from claimed answer {check['answer']}")


def _check_roots(check: dict) -> None:
    poly = _expr(check["poly"])
    claimed = [_expr(r) for r in check["roots"]]
    actual = sympy.solve(poly, list(poly.free_symbols)[0])
    if len(claimed) != len(actual):
        raise VerificationError(
            f"{check['poly']!r} has {len(actual)} roots, {len(claimed)} claimed"
        )
    remaining = list(actual)
    for root in claimed:
        for i, candidate in enumerate(remaining):
            if _is_zero(root - candidate):
                remaining.pop(i)
                break
        else:
            raise VerificationError(f"{root} is not a root of {check['poly']!r}")


def _check_roots_sum_product(check: dict) -> None:
    poly = _expr(check["poly"])
    symbol = list(poly.free_symbols)[0]
    roots = sympy.roots(poly, symbol)  # {root: multiplicity}
    total = sum(r * m for r, m in roots.items())
    product = sympy.prod([r**m for r, m in roots.items()])
    if not _is_zero(total - _expr(check["sum"])):
        raise VerificationError(f"sum of roots of {check['poly']!r} is {total}, not {check['sum']}")
    if not _is_zero(product - _expr(check["product"])):
        raise VerificationError(
            f"product of roots of {check['poly']!r} is {product}, not {check['product']}"
        )


def _check_value(check: dict) -> None:
    if not _is_zero(_expr(check["expr"]) - _expr(check["expected"])):
        raise VerificationError(f"{check['expr']!r} != {check['expected']!r}")


def _check_values(check: dict) -> None:
    for item in check["items"]:
        _check_value(item)


def _check_identity(check: dict) -> None:
    if not _is_zero(_expr(check["lhs"]) - _expr(check["rhs"])):
        raise VerificationError(f"{check['lhs']!r} is not identically {check['rhs']!r}")


_CHECKS = {
    "solve_system": _check_solve_system,
    "roots": _check_roots,
    "roots_sum_product": _check_roots_sum_product,
    "value": _check_value,
    "values": _check_values,
    "identity": _check_identity,
}


def verify_check(check: dict) -> None:
    """Raise VerificationError unless the check passes in SymPy."""
    kind = check.get("type")
    if kind not in _CHECKS:
        raise VerificationError(f"unknown check type {kind!r}")
    _CHECKS[kind](check)


def apply_seed_file(seed_path: Path, root: Path) -> int:
    """Verify every solution in a seed file and write it into the target data file."""
    seed = json.loads(seed_path.read_text(encoding="utf-8"))
    target = root / seed["target_file"]
    doc = load_json(target)
    by_id = {c["chunk_id"]: c for c in doc["content_chunks"]}

    for sol in seed["solutions"]:
        cid = sol["chunk_id"]
        if cid not in by_id:
            raise VerificationError(f"{seed_path.name}: chunk {cid!r} not in {seed['target_file']}")
        try:
            verify_check(sol["check"])
        except VerificationError as exc:
            raise VerificationError(f"{seed_path.name}: chunk {cid}: {exc}") from exc
        chunk = by_id[cid]
        if "problem_latex" in sol:
            chunk["latex"] = sol["problem_latex"]
        for key in ("difficulty", "importance"):
            if key in sol:
                chunk[key] = sol[key]
        chunk["solution_steps"] = [
            {"text": s["text"], "latex": s["latex"], "verified": True} for s in sol["steps"]
        ]

    dump_json(target, doc)
    return len(seed["solutions"])


def apply_all_seeds(root: Path, seeds_dir: Path | None = None) -> dict[str, int]:
    seeds_dir = seeds_dir or root / "content" / "solutions"
    applied: dict[str, int] = {}
    for seed_path in sorted(seeds_dir.glob("*.json")):
        applied[seed_path.name] = apply_seed_file(seed_path, root)
    return applied
