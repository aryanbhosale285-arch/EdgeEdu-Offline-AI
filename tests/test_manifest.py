import json

import pytest

from content_pipeline.manifest import (
    PRIVATE_KEY_NAME,
    PUBLIC_KEY_NAME,
    IntegrityError,
    build_manifest,
    generate_keypair,
    verify_manifest,
)


@pytest.fixture
def signed_repo(tmp_path):
    (tmp_path / "data").mkdir()
    (tmp_path / "data" / "a.json").write_text('{"x": 1}', encoding="utf-8")
    (tmp_path / "data" / "b.json").write_text('{"y": 2}', encoding="utf-8")
    generate_keypair(tmp_path / "keys")
    build_manifest(tmp_path, tmp_path / "keys" / PRIVATE_KEY_NAME)
    return tmp_path


def _public(repo):
    return repo / "keys" / PUBLIC_KEY_NAME


def test_roundtrip(signed_repo):
    body = verify_manifest(signed_repo, _public(signed_repo))
    assert body["content_version"] == 1
    assert set(body["files"]) == {"data/a.json", "data/b.json"}


def test_tampered_file_detected(signed_repo):
    (signed_repo / "data" / "a.json").write_text('{"x": 999}', encoding="utf-8")
    with pytest.raises(IntegrityError, match="hash mismatch"):
        verify_manifest(signed_repo, _public(signed_repo))


def test_tampered_manifest_detected(signed_repo):
    manifest_path = signed_repo / "data" / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    manifest["content_version"] = 99  # bump version without re-signing
    manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
    with pytest.raises(IntegrityError, match="signature invalid"):
        verify_manifest(signed_repo, _public(signed_repo))


def test_unlisted_file_detected(signed_repo):
    (signed_repo / "data" / "smuggled.json").write_text("{}", encoding="utf-8")
    with pytest.raises(IntegrityError, match="not in manifest"):
        verify_manifest(signed_repo, _public(signed_repo))


def test_downgrade_rejected(signed_repo):
    build_manifest(signed_repo, signed_repo / "keys" / PRIVATE_KEY_NAME)  # -> v2
    verify_manifest(signed_repo, _public(signed_repo), installed_version=2)
    with pytest.raises(IntegrityError, match="downgrade"):
        verify_manifest(signed_repo, _public(signed_repo), installed_version=3)


def test_version_must_be_monotonic_at_build(signed_repo):
    with pytest.raises(IntegrityError, match="must exceed"):
        build_manifest(
            signed_repo, signed_repo / "keys" / PRIVATE_KEY_NAME, content_version=1
        )


def test_wrong_key_rejected(signed_repo, tmp_path_factory):
    other = tmp_path_factory.mktemp("otherkeys")
    generate_keypair(other)
    with pytest.raises(IntegrityError, match="signature invalid"):
        verify_manifest(signed_repo, other / PUBLIC_KEY_NAME)
