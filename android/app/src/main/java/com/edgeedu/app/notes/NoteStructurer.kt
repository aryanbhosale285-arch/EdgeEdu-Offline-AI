package com.edgeedu.app.notes

import com.edgeedu.app.data.Chunk
import com.edgeedu.app.data.CurriculumDoc
import com.edgeedu.app.data.DocMeta
import com.edgeedu.app.search.Tokenizer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A note chunk after structuring: a display heading, the body, and keywords. */
data class StructuredChunk(
    val heading: String,
    val body: String,
    val keywords: List<String>,
)

/**
 * The on-device "structuring AI" for user-created subjects (no network): it
 * splits imported notes into retrieval-sized chunks and derives a heading and
 * keywords for each, then emits the same JSON document shape the curriculum
 * uses. It is heuristic (not the LLM) — chunking and indexing are mechanical;
 * the LLM's job is only to explain a retrieved chunk at answer time.
 */
object NoteStructurer {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    private val STOPWORDS = setOf(
        "the", "and", "for", "are", "but", "not", "you", "all", "can", "her", "was", "one",
        "our", "out", "his", "has", "had", "how", "its", "may", "who", "this", "that", "with",
        "from", "they", "have", "were", "will", "your", "what", "when", "which", "their", "there",
        "these", "those", "into", "than", "then", "them", "also", "such", "been", "more", "some",
    )

    fun structure(text: String): List<StructuredChunk> =
        NoteChunker.chunk(text).map { body ->
            StructuredChunk(heading = headingOf(body), body = body, keywords = keywordsOf(body))
        }

    /** First sentence, trimmed to a few words — a readable label for the chunk. */
    private fun headingOf(body: String): String {
        val firstSentence = body.split(Regex("""(?<=[.!?।])\s+""")).firstOrNull().orEmpty()
        val words = (firstSentence.ifBlank { body }).split(Regex("""\s+""")).filter { it.isNotBlank() }
        val head = words.take(8).joinToString(" ").trim().trimEnd('.', ',', ';', ':')
        return head.ifBlank { "Note" }
    }

    /** Top frequent, non-trivial terms in the chunk. */
    private fun keywordsOf(body: String): List<String> =
        Tokenizer.tokenize(body)
            .filter { it.length > 3 && it !in STOPWORDS && !it.all(Char::isDigit) }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(5).map { it.key }

    /**
     * Builds a curriculum-shaped JSON document for a custom subject, so the
     * structured notes exist as an offline, re-usable `.json` file too.
     */
    fun buildJson(
        subject: String,
        standard: Int,
        medium: String,
        chunks: List<StructuredChunk>,
    ): String {
        val doc = CurriculumDoc(
            metadata = DocMeta(
                file_name = "${standard}th_${subject}_$medium.json",
                standard = standard,
                subject = subject,
                language = medium,
                board = "User notes",
                schema_version = "3.0-notes",
                content_version = 1,
                last_updated = java.time.LocalDate.now().toString(),
            ),
            content_chunks = chunks.mapIndexed { i, c ->
                Chunk(
                    chunk_id = "${i + 1}",
                    heading = c.heading,
                    keywords = c.keywords,
                    text = c.body,
                )
            },
            generation_status = "user-notes",
        )
        return json.encodeToString(doc)
    }
}
