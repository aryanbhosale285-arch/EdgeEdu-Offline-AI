import { getCorpus } from "./data";
import type { IndexedChunk, SearchHit } from "./types";

/** Unicode-aware tokenizer; handles Latin and Devanagari alike. */
export function tokenize(text: string): string[] {
  return text.normalize("NFC").toLowerCase().match(/[\p{L}\p{N}]+/gu) ?? [];
}

const DEVANAGARI = /[ऀ-ॿ]/;

/** "Devanagari" covers Hindi and Marathi (same script, heuristically inseparable). */
export function detectScript(text: string): "Devanagari" | "Latin" {
  const letters = text.replace(/\s/g, "");
  if (!letters) return "Latin";
  const count = [...letters].filter((ch) => DEVANAGARI.test(ch)).length;
  return count / letters.length > 0.3 ? "Devanagari" : "Latin";
}

const K1 = 1.5;
const B = 0.75;
const HEADING_BOOST = 2;
const KEYWORD_BOOST = 3;

interface IndexedDoc {
  chunk: IndexedChunk;
  termFreq: Map<string, number>;
  length: number;
}

interface Bm25Index {
  docs: IndexedDoc[];
  docFreq: Map<string, number>;
  avgLength: number;
}

function buildIndex(chunks: IndexedChunk[]): Bm25Index {
  const docs: IndexedDoc[] = [];
  const docFreq = new Map<string, number>();
  let totalLength = 0;

  for (const chunk of chunks) {
    const termFreq = new Map<string, number>();
    const add = (tokens: string[], weight: number) => {
      for (const token of tokens) {
        termFreq.set(token, (termFreq.get(token) ?? 0) + weight);
      }
    };
    add(tokenize(chunk.text), 1);
    add(tokenize(chunk.heading), HEADING_BOOST);
    add(tokenize(chunk.keywords.join(" ")), KEYWORD_BOOST);

    let length = 0;
    for (const freq of termFreq.values()) length += freq;
    for (const term of termFreq.keys()) {
      docFreq.set(term, (docFreq.get(term) ?? 0) + 1);
    }
    docs.push({ chunk, termFreq, length });
    totalLength += length;
  }
  return { docs, docFreq, avgLength: totalLength / Math.max(1, docs.length) };
}

declare global {
  // eslint-disable-next-line no-var
  var __edgeeduIndex: Bm25Index | undefined;
}

function getIndex(): Bm25Index {
  if (!globalThis.__edgeeduIndex) globalThis.__edgeeduIndex = buildIndex(getCorpus().chunks);
  return globalThis.__edgeeduIndex;
}

export interface SearchFilters {
  language?: string;
  standard?: number;
  subject?: string;
}

export function search(query: string, filters: SearchFilters = {}, k = 10): SearchHit[] {
  const index = getIndex();
  const queryTerms = tokenize(query);
  if (queryTerms.length === 0) return [];

  const n = index.docs.length;
  const hits: SearchHit[] = [];

  const idfOf = (term: string) => {
    const df = index.docFreq.get(term) ?? 0;
    return Math.log(1 + (n - df + 0.5) / (df + 0.5));
  };
  // Unknown terms (df = 0) get the maximum idf, so asking about content the
  // corpus has never seen drives coverage down instead of being ignored.
  const totalIdf = queryTerms.reduce((sum, term) => sum + idfOf(term), 0);

  for (const doc of index.docs) {
    const { chunk } = doc;
    if (filters.language && chunk.language !== filters.language) continue;
    if (filters.standard && chunk.standard !== filters.standard) continue;
    if (filters.subject && chunk.subject !== filters.subject) continue;

    let score = 0;
    let matchedIdf = 0;
    for (const term of queryTerms) {
      const freq = doc.termFreq.get(term);
      if (!freq) continue;
      const idf = idfOf(term);
      matchedIdf += idf;
      score +=
        (idf * freq * (K1 + 1)) /
        (freq + K1 * (1 - B + (B * doc.length) / index.avgLength));
    }
    if (score > 0) {
      hits.push({ chunk, score, coverage: totalIdf > 0 ? matchedIdf / totalIdf : 0 });
    }
  }

  hits.sort((a, b) => b.score - a.score);
  return hits.slice(0, k);
}
