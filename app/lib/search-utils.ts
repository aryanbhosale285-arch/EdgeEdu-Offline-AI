/**
 * In-memory search over curriculum chunks.
 *
 * Builds an inverted index mapping tokens -> chunk postings, with per-field
 * weighting (keywords > heading > body). Supports cross-lingual matching
 * (because each chunk's `keywords` already bundle native + transliterated +
 * English terms) and light fuzzy matching for typo tolerance.
 *
 * This is intentionally dependency-free so it can later be ported to the
 * offline Android app.
 */
import type {
  CurriculumChunk,
  DocumentMeta,
  SearchIndexPayload,
  SearchResult,
} from "@/app/lib/types";

const FIELD_WEIGHTS = { keyword: 3, heading: 2, text: 1 } as const;

/**
 * Split text into lowercased tokens. Keeps Unicode letters/digits (so
 * Devanagari is preserved) and drops punctuation. Tokens shorter than 2 chars
 * are ignored as they carry little signal.
 */
export function tokenize(input: string): string[] {
  if (!input) return [];
  return input
    .toLowerCase()
    // Keep letters, numbers AND combining marks (\p{M}) — Devanagari vowel
    // signs (मात्रा) are Marks, not Letters, so excluding them would split
    // words like "वितरण" mid-character.
    .split(/[^\p{L}\p{N}\p{M}]+/u)
    .filter((t) => t.length >= 2);
}

interface Posting {
  /** Index into the engine's `chunks` array. */
  i: number;
  /** Accumulated weight of this term within that chunk. */
  w: number;
}

/** Bounded Levenshtein: returns true if edit distance <= max. */
function withinEditDistance(a: string, b: string, max: number): boolean {
  if (Math.abs(a.length - b.length) > max) return false;
  if (a === b) return true;
  const prev = new Array(b.length + 1);
  const curr = new Array(b.length + 1);
  for (let j = 0; j <= b.length; j++) prev[j] = j;
  for (let i = 1; i <= a.length; i++) {
    curr[0] = i;
    let rowMin = curr[0];
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      curr[j] = Math.min(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost);
      rowMin = Math.min(rowMin, curr[j]);
    }
    if (rowMin > max) return false; // whole row already exceeds budget
    for (let j = 0; j <= b.length; j++) prev[j] = curr[j];
  }
  return prev[b.length] <= max;
}

export class SearchEngine {
  private chunks: CurriculumChunk[];
  private docsById: Map<string, DocumentMeta>;
  private index: Map<string, Posting[]> = new Map();
  /** Document frequency per term, for idf. */
  private df: Map<string, number> = new Map();
  /** All distinct tokens, for fuzzy candidate lookup. */
  private vocabulary: string[] = [];

  constructor(payload: SearchIndexPayload) {
    this.chunks = payload.chunks;
    this.docsById = new Map(payload.docs.map((d) => [d.id, d]));
    this.build();
  }

  private addToken(token: string, chunkIdx: number, weight: number) {
    let postings = this.index.get(token);
    if (!postings) {
      postings = [];
      this.index.set(token, postings);
    }
    const last = postings[postings.length - 1];
    if (last && last.i === chunkIdx) {
      last.w += weight;
    } else {
      postings.push({ i: chunkIdx, w: weight });
      this.df.set(token, (this.df.get(token) ?? 0) + 1);
    }
  }

  private build() {
    this.chunks.forEach((chunk, i) => {
      for (const kw of chunk.keywords) {
        for (const t of tokenize(kw)) this.addToken(t, i, FIELD_WEIGHTS.keyword);
      }
      for (const t of tokenize(chunk.heading)) {
        this.addToken(t, i, FIELD_WEIGHTS.heading);
      }
      for (const t of tokenize(chunk.text)) {
        this.addToken(t, i, FIELD_WEIGHTS.text);
      }
    });
    this.vocabulary = Array.from(this.index.keys());
  }

  private idf(term: string): number {
    const n = this.chunks.length;
    const df = this.df.get(term) ?? 0;
    if (df === 0) return 0;
    return Math.log(1 + n / df);
  }

  /**
   * Find vocabulary tokens that fuzzily match a query token: exact, prefix, or
   * within edit distance 1 (only for tokens of length >= 4 to avoid noise).
   * Returns [token, weightFactor] pairs.
   */
  private expandTerm(term: string): Array<[string, number]> {
    const matches: Array<[string, number]> = [];
    if (this.index.has(term)) matches.push([term, 1]);

    const allowFuzzy = term.length >= 4;
    for (const vocab of this.vocabulary) {
      if (vocab === term) continue;
      if (vocab.startsWith(term) || term.startsWith(vocab)) {
        matches.push([vocab, 0.6]); // prefix / stem-ish match
      } else if (allowFuzzy && withinEditDistance(term, vocab, 1)) {
        matches.push([vocab, 0.5]); // typo tolerance
      }
    }
    return matches;
  }

  /**
   * Run a query and return the top-N ranked results.
   */
  search(query: string, limit = 20): SearchResult[] {
    const terms = Array.from(new Set(tokenize(query)));
    if (terms.length === 0) return [];

    // chunkIdx -> { score, matched terms }
    const scores = new Map<number, { score: number; matched: Set<string> }>();

    for (const term of terms) {
      const idf = this.idf(term) || 1; // unknown terms still allowed via fuzzy
      for (const [vocab, factor] of this.expandTerm(term)) {
        const postings = this.index.get(vocab);
        if (!postings) continue;
        const termIdf = this.idf(vocab) || idf;
        for (const { i, w } of postings) {
          const contribution = w * termIdf * factor;
          const entry = scores.get(i) ?? { score: 0, matched: new Set() };
          entry.score += contribution;
          entry.matched.add(term);
          scores.set(i, entry);
        }
      }
    }

    const results: SearchResult[] = [];
    for (const [i, { score, matched }] of scores) {
      const chunk = this.chunks[i];
      const doc = this.docsById.get(chunk.docId);
      if (!doc) continue;
      // Reward chunks that match more of the distinct query terms.
      const coverage = matched.size / terms.length;
      results.push({
        chunk,
        doc,
        score: score * (0.5 + 0.5 * coverage),
        matchedTerms: Array.from(matched),
      });
    }

    results.sort((a, b) => b.score - a.score);
    return results.slice(0, limit);
  }

  get size(): number {
    return this.chunks.length;
  }
}

/** Convenience factory. */
export function buildSearchEngine(payload: SearchIndexPayload): SearchEngine {
  return new SearchEngine(payload);
}

/**
 * Produce a short snippet from a chunk's text centered on the first matched
 * term, with the matched terms wrapped in <mark> for highlighting.
 */
export function buildSnippet(
  text: string,
  matchedTerms: string[],
  radius = 120
): string {
  if (!text) return "";
  const lower = text.toLowerCase();
  let pos = -1;
  for (const term of matchedTerms) {
    const p = lower.indexOf(term);
    if (p !== -1 && (pos === -1 || p < pos)) pos = p;
  }
  let snippet: string;
  if (pos === -1) {
    snippet = text.slice(0, radius * 2);
    if (text.length > radius * 2) snippet += "…";
  } else {
    const start = Math.max(0, pos - radius);
    const end = Math.min(text.length, pos + radius);
    snippet = (start > 0 ? "…" : "") + text.slice(start, end) + (end < text.length ? "…" : "");
  }
  return highlightTerms(snippet, matchedTerms);
}

/** Wrap occurrences of any matched term in <mark> tags (case-insensitive). */
export function highlightTerms(text: string, matchedTerms: string[]): string {
  if (!matchedTerms.length) return escapeHtml(text);
  const escaped = matchedTerms
    .filter(Boolean)
    .map((t) => t.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"))
    .sort((a, b) => b.length - a.length);
  if (!escaped.length) return escapeHtml(text);
  const re = new RegExp(`(${escaped.join("|")})`, "giu");
  return text
    .split(re)
    .map((part, idx) =>
      idx % 2 === 1 ? `<mark>${escapeHtml(part)}</mark>` : escapeHtml(part)
    )
    .join("");
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}
