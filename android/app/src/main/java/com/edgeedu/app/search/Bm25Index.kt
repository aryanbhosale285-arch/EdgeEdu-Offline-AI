package com.edgeedu.app.search

import com.edgeedu.app.data.IndexedChunk
import kotlin.math.ln

/** Unicode-aware tokenizer; handles Latin and Devanagari alike. */
object Tokenizer {
    private val word = Regex("""[\p{L}\p{N}]+""")
    fun tokenize(text: String): List<String> =
        word.findAll(text.lowercase()).map { it.value }.toList()

    private val devanagari = Regex("""[ऀ-ॿ]""")

    /** "Devanagari" covers Hindi and Marathi (same script). */
    fun isDevanagari(text: String): Boolean {
        val letters = text.filterNot { it.isWhitespace() }
        if (letters.isEmpty()) return false
        return letters.count { devanagari.matches(it.toString()) } / letters.length.toDouble() > 0.3
    }
}

data class SearchHit(
    val chunk: IndexedChunk,
    val score: Double,
    /** IDF-weighted fraction of query terms present in the chunk (0..1). */
    val coverage: Double,
)

data class SearchFilters(
    val language: String? = null,
    val standard: Int? = null,
    val subject: String? = null,
)

/**
 * BM25 over chunk text with boosted heading and keyword fields — a direct
 * port of the Phase-2 web ranker so retrieval behaves identically on device.
 */
class Bm25Index(chunks: List<IndexedChunk>) {

    private class Doc(val chunk: IndexedChunk, val termFreq: Map<String, Double>, val length: Double)

    private val docs: List<Doc>
    private val docFreq = HashMap<String, Int>()
    private val avgLength: Double

    init {
        var totalLength = 0.0
        docs = chunks.map { indexed ->
            val termFreq = HashMap<String, Double>()
            fun add(tokens: List<String>, weight: Double) {
                for (token in tokens) termFreq.merge(token, weight, Double::plus)
            }
            add(Tokenizer.tokenize(indexed.chunk.text), 1.0)
            add(Tokenizer.tokenize(indexed.chunk.heading), HEADING_BOOST)
            add(Tokenizer.tokenize(indexed.chunk.keywords.joinToString(" ")), KEYWORD_BOOST)

            val length = termFreq.values.sum()
            totalLength += length
            for (term in termFreq.keys) docFreq.merge(term, 1, Int::plus)
            Doc(indexed, termFreq, length)
        }
        avgLength = if (docs.isEmpty()) 1.0 else totalLength / docs.size
    }

    private fun idf(term: String): Double {
        val df = docFreq[term] ?: 0
        return ln(1 + (docs.size - df + 0.5) / (df + 0.5))
    }

    fun search(query: String, filters: SearchFilters = SearchFilters(), k: Int = 10): List<SearchHit> {
        val terms = Tokenizer.tokenize(query)
        if (terms.isEmpty()) return emptyList()
        val totalIdf = terms.sumOf { idf(it) }

        val hits = mutableListOf<SearchHit>()
        for (doc in docs) {
            val c = doc.chunk
            if (filters.language != null && c.language != filters.language) continue
            if (filters.standard != null && c.standard != filters.standard) continue
            if (filters.subject != null && c.subject != filters.subject) continue

            var score = 0.0
            var matchedIdf = 0.0
            for (term in terms) {
                val freq = doc.termFreq[term] ?: continue
                val idf = idf(term)
                matchedIdf += idf
                score += idf * freq * (K1 + 1) /
                    (freq + K1 * (1 - B + B * doc.length / avgLength))
            }
            if (score > 0) {
                hits += SearchHit(c, score, if (totalIdf > 0) matchedIdf / totalIdf else 0.0)
            }
        }
        return hits.sortedByDescending { it.score }.take(k)
    }

    companion object {
        private const val K1 = 1.5
        private const val B = 0.75
        private const val HEADING_BOOST = 2.0
        private const val KEYWORD_BOOST = 3.0
    }
}
