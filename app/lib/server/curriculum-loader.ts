/**
 * Server-only curriculum loader.
 *
 * Reads the raw JSON files under /data, normalizes their (somewhat
 * inconsistent) schema into the types in app/lib/types, and caches the result
 * in module scope so we only hit the filesystem once per server process.
 *
 * NOTE: this module uses `fs` and must never be imported from a Client
 * Component. It is consumed only by the Route Handlers under app/api.
 */
import "server-only";
import { promises as fs } from "fs";
import path from "path";
import type {
  CurriculumChunk,
  CurriculumDocument,
  DocumentMeta,
  Language,
} from "@/app/lib/types";

const DATA_DIR = path.join(process.cwd(), "data");

interface RawChunk {
  chunk_id?: string;
  heading?: string;
  keywords?: string[];
  text?: string;
}

interface RawMetadata {
  file_name?: string;
  standard?: string | number;
  subject?: string;
  language?: string;
  board?: string;
  chapter_id?: number;
  chapter_title?: string;
}

let cache: CurriculumDocument[] | null = null;

function slugify(input: string): string {
  return input
    .toLowerCase()
    .normalize("NFKD")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");
}

function normalizeLanguage(raw: string | undefined): Language {
  const v = (raw ?? "").toLowerCase();
  if (v.startsWith("hin")) return "Hindi";
  if (v.startsWith("mar")) return "Marathi";
  return "English";
}

/** Parse the chapter number from a chunk id like "2.4" -> 2. */
function chapterFromChunkId(chunkId: string, fallback: number): number {
  const n = parseInt(chunkId.split(".")[0], 10);
  return Number.isFinite(n) ? n : fallback;
}

/**
 * Some files (e.g. 9th_Math 2_Marathi) pack several chapters into one document
 * using keys like `content_chunks`, `content_chunks_chapter_2`, etc. Collect
 * every array whose key starts with `content_chunks`.
 */
function collectRawChunks(json: Record<string, unknown>): RawChunk[] {
  const out: RawChunk[] = [];
  for (const [key, value] of Object.entries(json)) {
    if (key.startsWith("content_chunks") && Array.isArray(value)) {
      out.push(...(value as RawChunk[]));
    }
  }
  return out;
}

async function findJsonFiles(dir: string): Promise<string[]> {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  const files: string[] = [];
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await findJsonFiles(full)));
    } else if (entry.isFile() && entry.name.toLowerCase().endsWith(".json")) {
      files.push(full);
    }
  }
  return files;
}

async function loadFromDisk(): Promise<CurriculumDocument[]> {
  const files = (await findJsonFiles(DATA_DIR)).sort();
  const usedIds = new Set<string>();
  const docs: CurriculumDocument[] = [];

  for (const file of files) {
    let json: Record<string, unknown>;
    try {
      json = JSON.parse(await fs.readFile(file, "utf-8"));
    } catch (err) {
      // Skip unparseable files rather than crashing the whole server.
      console.error(`[curriculum-loader] skipping invalid JSON: ${file}`, err);
      continue;
    }

    const meta = (json.metadata ?? {}) as RawMetadata;
    const standard = String(meta.standard ?? "").trim() || "Unknown";
    const subject = (meta.subject ?? "Unknown subject").trim();
    const language = normalizeLanguage(meta.language);
    const docChapterId = Number(meta.chapter_id ?? 1) || 1;

    // Build a stable, unique, URL-safe id.
    let id = slugify(`${standard}-${subject}-${language}`);
    if (!id) id = slugify(path.basename(file, ".json"));
    let unique = id;
    let n = 2;
    while (usedIds.has(unique)) unique = `${id}-${n++}`;
    usedIds.add(unique);
    id = unique;

    const chunks: CurriculumChunk[] = collectRawChunks(json)
      .filter((c) => c && (c.text || c.heading))
      .map((c) => {
        const chunkId = String(c.chunk_id ?? "").trim() || "0.0";
        return {
          chunkId,
          heading: (c.heading ?? "").trim(),
          keywords: Array.isArray(c.keywords) ? c.keywords.filter(Boolean) : [],
          text: (c.text ?? "").trim(),
          docId: id,
          chapterId: chapterFromChunkId(chunkId, docChapterId),
        };
      });

    docs.push({
      id,
      fileName: meta.file_name ?? path.basename(file, ".json"),
      standard,
      subject,
      language,
      board: meta.board ?? "Maharashtra State Board",
      chapterTitle: (meta.chapter_title ?? subject).trim(),
      chapterId: docChapterId,
      chunkCount: chunks.length,
      chunks,
    });
  }

  return docs;
}

/** Load (and memoize) every normalized curriculum document. */
export async function getAllDocuments(): Promise<CurriculumDocument[]> {
  if (!cache) cache = await loadFromDisk();
  return cache;
}

/** Document metadata only (no chunk bodies) — cheap manifest payload. */
export async function getManifest(): Promise<DocumentMeta[]> {
  const docs = await getAllDocuments();
  return docs.map(({ chunks, ...meta }) => meta);
}

export async function getDocumentById(
  id: string
): Promise<CurriculumDocument | null> {
  const docs = await getAllDocuments();
  return docs.find((d) => d.id === id) ?? null;
}

/** Every chunk across every document, flattened — used to build search index. */
export async function getAllChunks(): Promise<CurriculumChunk[]> {
  const docs = await getAllDocuments();
  return docs.flatMap((d) => d.chunks);
}
