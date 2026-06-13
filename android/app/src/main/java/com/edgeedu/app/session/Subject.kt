package com.edgeedu.app.session

import com.edgeedu.app.data.IndexedChunk
import com.edgeedu.app.search.Bm25Index
import com.edgeedu.app.tutor.Tutor

/**
 * The PRD's Phase-1 subjects. Exactly one is active at a time: a session
 * loads only that subject's chunks, index, and tools (the math engine is a
 * maths-session tool only).
 */
enum class Subject(val label: String, val isMath: Boolean = false) {
    Mathematics("Mathematics", isMath = true),
    Science("Science"),
    Geography("Geography"),
    SocialStudies("Social Studies / History");

    companion object {
        /**
         * Maps a curriculum file's subject string (e.g. "Math 1",
         * "Mathematics 2", "Science and Technology Part 1",
         * "History & Political Science") to its session subject.
         * Unknown subjects return null and are not offered as sessions.
         */
        fun of(fileSubject: String): Subject? = when {
            fileSubject.contains("math", ignoreCase = true) -> Mathematics
            fileSubject.contains("geography", ignoreCase = true) -> Geography
            // Before "science": "History & Political Science" is not Science.
            fileSubject.contains("history", ignoreCase = true) -> SocialStudies
            fileSubject.contains("science", ignoreCase = true) -> Science
            else -> null
        }
    }
}

/**
 * A focused subject session: only this subject's chunks are indexed and
 * retrievable, and the tutor's math tooling is enabled only for Mathematics.
 * Switching subjects replaces the whole session, freeing the old index.
 */
class SubjectSession(
    val subject: Subject,
    val chunks: List<IndexedChunk>,
    val index: Bm25Index,
    val tutor: Tutor,
)
