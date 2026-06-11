"""CLI for the Phase-1 content pipeline.

    python -m content_pipeline keygen
    python -m content_pipeline upgrade
    python -m content_pipeline solutions
    python -m content_pipeline validate
    python -m content_pipeline manifest-build [--content-version N]
    python -m content_pipeline manifest-verify [--installed-version N]
    python -m content_pipeline all          # upgrade + solutions + validate + manifest-build
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from . import manifest as mf
from . import solutions as sol
from .schema import validate_document
from .upgrade import load_json, upgrade_file

ROOT = Path(__file__).resolve().parents[2]


def _data_files(root: Path) -> list[Path]:
    return sorted(
        p for p in (root / "data").rglob("*.json") if p.name != mf.MANIFEST_NAME
    )


def cmd_keygen(root: Path) -> int:
    private_path, public_path = mf.generate_keypair(root / "keys")
    print(f"wrote {private_path} (KEEP PRIVATE, gitignored)")
    print(f"wrote {public_path}")
    return 0


def cmd_upgrade(root: Path) -> int:
    for path in _data_files(root):
        doc = upgrade_file(path)
        print(f"upgraded {path.relative_to(root)} ({len(doc['content_chunks'])} chunks)")
    return 0


def cmd_solutions(root: Path) -> int:
    applied = sol.apply_all_seeds(root)
    total = sum(applied.values())
    for name, count in applied.items():
        print(f"{name}: {count} verified solutions attached")
    print(f"total: {total} chunks now carry SymPy-verified solution_steps")
    return 0


def cmd_validate(root: Path) -> int:
    errors: list[str] = []
    for path in _data_files(root):
        errors += validate_document(load_json(path), name=str(path.relative_to(root)))
    for error in errors:
        print(f"ERROR: {error}", file=sys.stderr)
    print(f"validated {len(_data_files(root))} files: " + ("FAIL" if errors else "OK"))
    return 1 if errors else 0


def cmd_manifest_build(root: Path, content_version: int | None) -> int:
    manifest = mf.build_manifest(
        root, root / "keys" / mf.PRIVATE_KEY_NAME, content_version=content_version
    )
    print(
        f"manifest v{manifest['content_version']}: {len(manifest['files'])} files signed"
    )
    return 0


def cmd_manifest_verify(root: Path, installed_version: int | None) -> int:
    body = mf.verify_manifest(
        root, root / "keys" / mf.PUBLIC_KEY_NAME, installed_version=installed_version
    )
    print(
        f"manifest v{body['content_version']} OK: signature valid, "
        f"{len(body['files'])} file hashes match"
    )
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="content_pipeline")
    parser.add_argument("--root", type=Path, default=ROOT)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("keygen")
    sub.add_parser("upgrade")
    sub.add_parser("solutions")
    sub.add_parser("validate")
    build = sub.add_parser("manifest-build")
    build.add_argument("--content-version", type=int, default=None)
    verify = sub.add_parser("manifest-verify")
    verify.add_argument("--installed-version", type=int, default=None)
    sub.add_parser("all")

    args = parser.parse_args(argv)
    root = args.root

    if args.command == "keygen":
        return cmd_keygen(root)
    if args.command == "upgrade":
        return cmd_upgrade(root)
    if args.command == "solutions":
        return cmd_solutions(root)
    if args.command == "validate":
        return cmd_validate(root)
    if args.command == "manifest-build":
        return cmd_manifest_build(root, args.content_version)
    if args.command == "manifest-verify":
        return cmd_manifest_verify(root, args.installed_version)
    if args.command == "all":
        for step in (cmd_upgrade, cmd_solutions, cmd_validate):
            code = step(root)
            if code:
                return code
        return cmd_manifest_build(root, None)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
