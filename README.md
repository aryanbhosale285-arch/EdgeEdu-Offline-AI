# EdgeEdu — Offline AI Tutor

An offline-first, AI-powered tutor for Class 9 & 10 students (Maharashtra State
Board) in **English, Hindi, and Marathi**. A small language model will run fully
on-device; a real math engine — not the LLM — guarantees arithmetic correctness.

Built against **PRD v3.0** (June 2026). Current status: **Phase 1 complete**.

| Phase | Scope | Status |
| --- | --- | --- |
| 1 — Data & content | v3.0 schema, verified `solution_steps` + `latex`, signed bundles | ✅ done |
| 2 — Web prototype | indexing, search, retrieve → explain end-to-end | ✅ done ([web/](web/)) |
| 3 — Android MVP | Compose UI, llama.cpp via JNI, math engine + GBNF, KaTeX | ✅ done ([android/](android/)) |
| 4 — Hardening & test | integrity, JNI crash safety, performance | planned |
| 5 — Showcase / expand | APK packaging, more subjects/grades | planned |

## Why verified solutions?

A phone-sized LLM cannot be trusted with arithmetic. EdgeEdu's fix is
architectural: worked solutions are **pre-solved and machine-verified with
SymPy at build time**, stored in the curriculum, and the runtime LLM only
*rephrases* known-correct steps (or routes fresh computation to a math
engine via GBNF-constrained tool calls — Phase 3).

Every shipped `solution_steps` entry carries `"verified": true` only because a
machine-checkable spec for it actually passed SymPy during the build. A failing
check aborts the build; unverified maths never ships.

## Repository layout

```
data/                 33 curriculum files (9th/10th × subject × language), v3.0 schema
data/manifest.json    signed content manifest (SHA-256 per file, Ed25519 signature)
content/solutions/    seed files: worked steps + machine-checkable SymPy specs
tools/content_pipeline/
  schema.py           v3.0 document schema + validator
  upgrade.py          legacy → v3.0 migration (incl. fragment & duplicate repair)
  solutions.py        SymPy verification + attachment of solution steps
  manifest.py         keygen, manifest build/sign, verify (hashes, signature, downgrade)
keys/                 Ed25519 public key (private key is gitignored, never committed)
tests/                pytest suite, incl. integrity tests over the real shipped data
```

## Web prototype (Phase 2)

A Next.js app in [web/](web/) proving the retrieve→explain loop end-to-end:

- **Integrity-gated loading** — the server verifies the Ed25519 manifest
  signature and every file hash before serving any content.
- **Multilingual BM25 search** — Devanagari-safe tokenizer, heading/keyword
  field boosts, language & standard filters (`/api/search`).
- **Grounded chat** (`/api/chat`) — answers are assembled *only* from retrieved
  chunks and always cite sources; SymPy-verified solution steps render in KaTeX
  with a ✓ badge. When IDF-weighted query coverage is too low it declines
  instead of guessing — the same contract the Phase-3 on-device LLM must honour.
- **Browse** — every chunk by standard/subject/language with difficulty,
  importance, and verified-solution markers.

```bash
cd web
npm install
npm run dev     # http://localhost:3000
```

## Android app (Phase 3)

A native Kotlin/Compose app in [android/](android/) — fully offline, no
network permission in the manifest at all.

- **Verified maths on device** — the LLM emits structured
  `<calc>solve: …</calc>` / `<calc>eval: …</calc>` calls instead of
  calculating; a Kotlin interceptor routes them through mXparser and splices
  back engine-verified results (PRD §7.1 Layer 1). Solutions are verified by
  substitution before display; non-linear or unparseable input fails loudly
  rather than guessing.
- **Engine abstraction** — `MockLlmEngine` (extractive, like Phase 2) runs by
  default so the APK builds and works without model weights. `LlamaCppEngine`
  + JNI bridge + CMake build are in place behind the `-PenableLlama` flag:
  clone llama.cpp into `app/src/main/cpp/llama.cpp`, install the NDK, and
  build. Generation is constrained by `assets/grammars/calc.gbnf`, so tool
  calls are grammar-enforced.
- **Same retrieval as the web** — BM25 with field boosts and IDF-coverage
  decline, ported 1:1 to Kotlin; bundled content is SHA-256-verified against
  the signed manifest at startup.
- **KaTeX rendering** — bundled KaTeX in a sandboxed WebView via
  WebViewAssetLoader (no file access, no remote loads).

```bash
cd android
# local.properties must point at your Android SDK, then:
gradle assembleDebug          # mock engine, no NDK needed
gradle testDebugUnitTest      # math engine / interceptor / BM25 tests
```

## Content pipeline

```bash
pip install -r requirements.txt

# PYTHONPATH=tools (or run from tools/)
python -m content_pipeline keygen            # one-time: Ed25519 keypair
python -m content_pipeline upgrade           # legacy JSON -> v3.0 schema
python -m content_pipeline solutions         # verify seeds in SymPy, attach to chunks
python -m content_pipeline validate          # schema-check all 33 files
python -m content_pipeline manifest-build    # hash + sign, monotonic version bump
python -m content_pipeline manifest-verify --installed-version 1
python -m content_pipeline all               # upgrade + solutions + validate + build
```

Tests:

```bash
python -m pytest
```

## v3.0 chunk schema (excerpt)

```json
{
  "chunk_id": "1.3",
  "heading": "Solving Equations with Interchanged Coefficients",
  "keywords": ["Interchanged Coefficients"],
  "text": "…",
  "latex": "15x + 17y = 21, \\quad 17x + 15y = 11",
  "difficulty": 3,
  "importance": 4,
  "linked_concepts": [],
  "prerequisites": [],
  "solution_steps": [
    {"text": "Add the two equations: 32x + 32y = 32. Divide both sides by 32.",
     "latex": "x + y = 1", "verified": true}
  ]
}
```

## Integrity model

- **Hashing** — every content file is SHA-256-hashed into `data/manifest.json`;
  a file that does not match its hash is rejected, as is any unlisted file.
- **Signing** — the manifest body is signed with Ed25519. The public key ships
  with the app; the private key stays on the build machine (gitignored).
- **Downgrade protection** — `content_version` is monotonic. A device holding
  version N refuses any manifest with a lower version.
- **Build-time math verification** — see above; this is also the primary
  defence against shipping hallucinated worked solutions.

## Deliberately deferred (production work, out of scope for this build)

- On-device safety moderation of model input/output (users are minors).
- DPDP Act 2023 parental-consent flows; analytics are OFF and absent.
- Textbook copyright licensing before any public distribution.
- SQLCipher database encryption; Play Integrity attestation.

## Known Phase-1 limitations

- Verified `solution_steps` currently cover 13 representative Math chunks in
  the English files; Hindi/Marathi step text needs translation, and coverage
  should grow toward every solvable worked example.
- `difficulty` / `importance` default to 3 except where curated in seeds.
- Chapter titles are not stored (chunk ids encode the chapter number).

## License

Apache-2.0.
