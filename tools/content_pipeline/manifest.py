"""Signed content manifest: SHA-256 per file, Ed25519 signature, monotonic version.

The manifest is the device's root of trust for downloaded content:
- every curriculum file is hashed; a file that doesn't match its hash is rejected;
- the manifest body is signed with Ed25519; the public key ships inside the APK
  (here: committed to the repo), the private key never leaves the build machine;
- ``content_version`` is monotonic — a device that has version N installed
  refuses any manifest with version < N (downgrade attack).
"""
from __future__ import annotations

import datetime
import hashlib
import json
from pathlib import Path

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import (
    Ed25519PrivateKey,
    Ed25519PublicKey,
)

MANIFEST_NAME = "manifest.json"
PRIVATE_KEY_NAME = "content_signing_private.pem"
PUBLIC_KEY_NAME = "content_signing_public.pem"


class IntegrityError(ValueError):
    pass


def generate_keypair(key_dir: Path) -> tuple[Path, Path]:
    key_dir.mkdir(parents=True, exist_ok=True)
    private = Ed25519PrivateKey.generate()
    private_path = key_dir / PRIVATE_KEY_NAME
    public_path = key_dir / PUBLIC_KEY_NAME
    private_path.write_bytes(
        private.private_bytes(
            serialization.Encoding.PEM,
            serialization.PrivateFormat.PKCS8,
            serialization.NoEncryption(),
        )
    )
    public_path.write_bytes(
        private.public_key().public_bytes(
            serialization.Encoding.PEM,
            serialization.PublicFormat.SubjectPublicKeyInfo,
        )
    )
    return private_path, public_path


def _load_private(path: Path) -> Ed25519PrivateKey:
    return serialization.load_pem_private_key(path.read_bytes(), password=None)


def _load_public(path: Path) -> Ed25519PublicKey:
    return serialization.load_pem_public_key(path.read_bytes())


def _canonical(body: dict) -> bytes:
    return json.dumps(body, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode(
        "utf-8"
    )


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _content_files(root: Path, data_dir: str) -> list[Path]:
    base = root / data_dir
    return sorted(
        p for p in base.rglob("*.json") if p.name != MANIFEST_NAME
    )


def build_manifest(
    root: Path,
    private_key_path: Path,
    data_dir: str = "data",
    content_version: int | None = None,
    generated: str | None = None,
) -> dict:
    manifest_path = root / data_dir / MANIFEST_NAME
    previous = 0
    if manifest_path.exists():
        previous = json.loads(manifest_path.read_text(encoding="utf-8")).get(
            "content_version", 0
        )

    version = content_version if content_version is not None else previous + 1
    if version <= previous:
        raise IntegrityError(
            f"content_version {version} must exceed previous manifest version {previous}"
        )

    files = {}
    for path in _content_files(root, data_dir):
        rel = path.relative_to(root).as_posix()
        files[rel] = {"sha256": _sha256(path), "bytes": path.stat().st_size}
    if not files:
        raise IntegrityError(f"no content files found under {data_dir!r}")

    body = {
        "manifest_schema": 1,
        "content_version": version,
        "generated": generated or datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "algorithm": "sha256",
        "files": files,
    }
    signature = _load_private(private_key_path).sign(_canonical(body)).hex()
    manifest = {**body, "signature": signature}
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return manifest


def verify_manifest(
    root: Path,
    public_key_path: Path,
    data_dir: str = "data",
    installed_version: int | None = None,
) -> dict:
    """Verify signature, per-file hashes, and monotonic version. Returns the body."""
    manifest_path = root / data_dir / MANIFEST_NAME
    if not manifest_path.exists():
        raise IntegrityError(f"missing {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

    body = dict(manifest)
    signature = body.pop("signature", None)
    if not signature:
        raise IntegrityError("manifest has no signature")
    try:
        _load_public(public_key_path).verify(bytes.fromhex(signature), _canonical(body))
    except Exception as exc:
        raise IntegrityError(f"manifest signature invalid: {exc}") from exc

    if installed_version is not None and body["content_version"] < installed_version:
        raise IntegrityError(
            f"downgrade rejected: manifest version {body['content_version']} "
            f"< installed version {installed_version}"
        )

    for rel, info in body["files"].items():
        path = root / Path(rel)
        if not path.exists():
            raise IntegrityError(f"listed file missing: {rel}")
        digest = _sha256(path)
        if digest != info["sha256"]:
            raise IntegrityError(f"hash mismatch for {rel}")

    listed = set(body["files"])
    on_disk = {p.relative_to(root).as_posix() for p in _content_files(root, data_dir)}
    unlisted = on_disk - listed
    if unlisted:
        raise IntegrityError(f"files on disk not in manifest: {sorted(unlisted)}")

    return body
