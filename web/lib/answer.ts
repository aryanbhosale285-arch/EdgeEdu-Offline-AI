import { detectScript, search, type SearchFilters } from "./search";
import type { ChatAnswer, IndexedChunk } from "./types";

/**
 * Phase-2 retrieve->explain composer.
 *
 * Extractive and fully grounded: the reply is assembled from retrieved,
 * checksum-verified curriculum chunks (plus their SymPy-verified solution
 * steps) and always cites its sources. When retrieval is empty or weak it
 * declines instead of guessing — the same contract the Phase-3 on-device
 * LLM must honour, with generation swapped in for extraction.
 */

/** Decline unless the top chunk contains at least this IDF-weighted share of the query. */
const MIN_COVERAGE = 0.45;
const SENTENCE_SPLIT = /(?<=[.!?।])\s+/u;

function excerpt(chunk: IndexedChunk, maxSentences = 3): string {
  const sentences = chunk.text.split(SENTENCE_SPLIT);
  const head = sentences.slice(0, maxSentences).join(" ");
  return sentences.length > maxSentences ? `${head} …` : head;
}

const DECLINE: Record<string, string> = {
  Latin:
    "I could not find this in the Maharashtra Board Class 9–10 textbooks I know. " +
    "Try rephrasing, or pick a chapter from Browse — I only answer from the curriculum.",
  Devanagari:
    "यह प्रश्न मेरे पाठ्यपुस्तक (महाराष्ट्र बोर्ड, कक्षा ९–१०) में नहीं मिला। " +
    "कृपया प्रश्न बदलकर पूछें — मैं केवल पाठ्यक्रम से उत्तर देता हूँ।",
};

export function answer(message: string, filters: SearchFilters = {}): ChatAnswer {
  const script = detectScript(message);

  // Without an explicit language filter, keep scripts consistent so a
  // Devanagari question is not answered with English text and vice versa.
  const effective: SearchFilters = { ...filters };
  const hits = search(message, effective, 8).filter((hit) =>
    filters.language
      ? true
      : script === "Devanagari"
        ? hit.chunk.language !== "English"
        : hit.chunk.language === "English"
  );

  if (hits.length === 0 || hits[0].coverage < MIN_COVERAGE) {
    return { answer: DECLINE[script], grounded: false, sources: [] };
  }

  const best = hits[0].chunk;
  const parts = [`${best.heading} (Std ${best.standard}, ${best.subject})`, excerpt(best)];

  const related = hits.slice(1, 3).map((h) => h.chunk);
  if (related.length > 0) {
    parts.push(`Related: ${related.map((c) => `${c.heading} (${c.chunk_id})`).join(" · ")}`);
  }

  return {
    answer: parts.join("\n\n"),
    grounded: true,
    latex: best.latex,
    steps: best.solution_steps,
    sources: hits.slice(0, 3).map(({ chunk }) => ({
      file: chunk.file,
      chunk_id: chunk.chunk_id,
      heading: chunk.heading,
      subject: chunk.subject,
      standard: chunk.standard,
      language: chunk.language,
    })),
  };
}
