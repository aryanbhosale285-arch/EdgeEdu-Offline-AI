package com.edgeedu.app.tutor

import com.edgeedu.app.data.IndexedChunk
import com.edgeedu.app.data.SolutionStep
import com.edgeedu.app.search.Bm25Index
import com.edgeedu.app.search.SearchFilters
import com.edgeedu.app.search.Tokenizer

data class TutorReply(
    val text: String,
    val grounded: Boolean,
    val latex: String?,
    val steps: List<SolutionStep>,
    val sources: List<IndexedChunk>,
    val engine: String,
)

/**
 * Retrieve → explain → intercept, the PRD §8 flow:
 * retrieval grounds the model, the model explains and emits <calc> calls,
 * the interceptor swaps in engine-verified results, and pre-verified
 * solution steps ride along from the retrieved chunk.
 */
class Tutor(private val index: Bm25Index, private val engine: LlmEngine) {

    companion object {
        /** Decline unless the top chunk covers this IDF share of the query. */
        private const val MIN_COVERAGE = 0.45
    }

    suspend fun ask(question: String, filters: SearchFilters = SearchFilters()): TutorReply {
        val devanagari = Tokenizer.isDevanagari(question)
        val hits = index.search(question, filters, k = 8).filter {
            when {
                filters.language != null -> true
                devanagari -> it.chunk.language != "English"
                else -> it.chunk.language == "English"
            }
        }

        // A question with inline maths is answerable by the engine even when
        // retrieval is weak; otherwise low coverage means: decline, don't guess.
        val hasInlineMath = question.contains('=') || Regex("""\d\s*[-+*/^]\s*\d""").containsMatchIn(question)
        if ((hits.isEmpty() || hits.first().coverage < MIN_COVERAGE) && !hasInlineMath) {
            return TutorReply(
                text = if (devanagari) {
                    "यह प्रश्न पाठ्यपुस्तक (महाराष्ट्र बोर्ड, कक्षा ९–१०) में नहीं मिला। कृपया प्रश्न बदलकर पूछें।"
                } else {
                    "I could not find this in the Maharashtra Board Class 9–10 textbooks. " +
                        "Try rephrasing — I only answer from the curriculum."
                },
                grounded = false, latex = null, steps = emptyList(), sources = emptyList(),
                engine = engine.name,
            )
        }

        val context = hits.take(3).map { it.chunk }
        val raw = engine.explain(question, context)
        val intercepted = CalcInterceptor.process(raw)

        val best = context.firstOrNull()
        return TutorReply(
            text = intercepted.text,
            grounded = best != null,
            latex = best?.chunk?.latex,
            steps = best?.chunk?.solution_steps.orEmpty(),
            sources = context,
            engine = engine.name,
        )
    }
}
