import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import type { CurriculumDoc, IndexedChunk } from "./types";

/** The repo root holds data/ and keys/; the web app lives in web/. */
const ROOT = path.resolve(process.cwd(), "..");
const DATA_DIR = path.join(ROOT, "data");
const PUBLIC_KEY = path.join(ROOT, "keys", "content_signing_public.pem");
const MANIFEST = path.join(DATA_DIR, "manifest.json");

export class IntegrityError extends Error {}

/** Canonical JSON identical to Python's json.dumps(sort_keys=True, separators=(",", ":")). */
function canonical(value: unknown): string {
  if (value === null || typeof value === "number" || typeof value === "boolean") {
    return JSON.stringify(value);
  }
  if (typeof value === "string") return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map(canonical).join(",")}]`;
  const entries = Object.entries(value as Record<string, unknown>).sort(([a], [b]) =>
    a < b ? -1 : a > b ? 1 : 0
  );
  return `{${entries.map(([k, v]) => `${JSON.stringify(k)}:${canonical(v)}`).join(",")}}`;
}

interface Manifest {
  manifest_schema: number;
  content_version: number;
  generated: string;
  algorithm: string;
  files: Record<string, { sha256: string; bytes: number }>;
  signature: string;
}

/** Verify the Ed25519 signature and every file hash; throw on any mismatch. */
function verifyManifest(): Manifest {
  const manifest: Manifest = JSON.parse(fs.readFileSync(MANIFEST, "utf-8"));
  const { signature, ...body } = manifest;
  if (!signature) throw new IntegrityError("manifest has no signature");

  const publicKey = crypto.createPublicKey(fs.readFileSync(PUBLIC_KEY));
  const ok = crypto.verify(
    null,
    Buffer.from(canonical(body), "utf-8"),
    publicKey,
    Buffer.from(signature, "hex")
  );
  if (!ok) throw new IntegrityError("manifest signature invalid");

  for (const [rel, info] of Object.entries(body.files)) {
    const file = path.join(ROOT, rel);
    if (!fs.existsSync(file)) throw new IntegrityError(`listed file missing: ${rel}`);
    const digest = crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
    if (digest !== info.sha256) throw new IntegrityError(`hash mismatch for ${rel}`);
  }
  return manifest;
}

export interface Corpus {
  chunks: IndexedChunk[];
  contentVersion: number;
  fileCount: number;
  verifiedSolutionChunks: number;
}

function loadCorpus(): Corpus {
  const manifest = verifyManifest();
  const chunks: IndexedChunk[] = [];
  let verifiedSolutionChunks = 0;

  for (const rel of Object.keys(manifest.files).sort()) {
    const doc: CurriculumDoc = JSON.parse(fs.readFileSync(path.join(ROOT, rel), "utf-8"));
    const { standard, subject, language } = doc.metadata;
    for (const chunk of doc.content_chunks) {
      if (chunk.solution_steps?.length) verifiedSolutionChunks += 1;
      chunks.push({ ...chunk, file: rel, standard, subject, language });
    }
  }
  return {
    chunks,
    contentVersion: manifest.content_version,
    fileCount: Object.keys(manifest.files).length,
    verifiedSolutionChunks,
  };
}

declare global {
  // eslint-disable-next-line no-var
  var __edgeeduCorpus: Corpus | undefined;
}

/** Load once per process; content is verified against the signed manifest before use. */
export function getCorpus(): Corpus {
  if (!globalThis.__edgeeduCorpus) globalThis.__edgeeduCorpus = loadCorpus();
  return globalThis.__edgeeduCorpus;
}
