/**
 * Shared, framework-agnostic type definitions for the EdgeEdu curriculum data.
 *
 * The on-disk JSON (under /data) uses a loose schema that varies slightly
 * between files. These types describe the *normalized* shape that the rest of
 * the app consumes, after the server loader has cleaned things up.
 */

export type Language = "English" | "Hindi" | "Marathi";

export const LANGUAGES: Language[] = ["English", "Hindi", "Marathi"];

/** A single retrievable unit of curriculum content. */
export interface CurriculumChunk {
  /** e.g. "1.3" — unique within a document. */
  chunkId: string;
  heading: string;
  /** Mixed-script keywords, including English transliterations. */
  keywords: string[];
  text: string;

  // ---- augmented during normalization ----
  /** Id of the document this chunk belongs to. */
  docId: string;
  /** Chapter number this chunk belongs to (parsed from chunkId / metadata). */
  chapterId: number;
}

/** Lightweight metadata describing one curriculum document (one subject file). */
export interface DocumentMeta {
  /** Stable slug used in URLs and as a lookup key. */
  id: string;
  fileName: string;
  standard: string;
  subject: string;
  language: Language;
  board: string;
  chapterTitle: string;
  chapterId: number;
  chunkCount: number;
}

/** A document together with all of its content chunks. */
export interface CurriculumDocument extends DocumentMeta {
  chunks: CurriculumChunk[];
}

/** A search hit: a chunk plus its score and the spans that matched. */
export interface SearchResult {
  chunk: CurriculumChunk;
  doc: DocumentMeta;
  score: number;
  /** Lowercased query terms that contributed to the score. */
  matchedTerms: string[];
}

/** The flat payload the client downloads once to build its search index. */
export interface SearchIndexPayload {
  generatedAt: string;
  docs: DocumentMeta[];
  chunks: CurriculumChunk[];
}
