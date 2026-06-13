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
 * Identifies the active subject — either one of the built-in curriculum
 * [Subject]s, or a user-created [Custom] subject (its content comes entirely
 * from the student's imported notes, structured on-device).
 */
sealed interface SubjectRef {
    val label: String
    val isMath: Boolean

    /** Stable key for storage and active-highlight comparison. */
    val key: String

    data class Builtin(val subject: Subject) : SubjectRef {
        override val label get() = subject.label
        override val isMath get() = subject.isMath
        override val key get() = "builtin:${subject.name}"
    }

    data class Custom(val id: Long, override val label: String) : SubjectRef {
        override val isMath get() = false
        override val key get() = "custom:$id"
    }
}

/**
 * A focused subject session: only this subject's chunks are indexed and
 * retrievable, and the tutor's math tooling is enabled only for Mathematics.
 * Switching subjects replaces the whole session, freeing the old index.
 */
class SubjectSession(
    val subject: SubjectRef,
    val chunks: List<IndexedChunk>,
    val index: Bm25Index,
    val tutor: Tutor,
)
