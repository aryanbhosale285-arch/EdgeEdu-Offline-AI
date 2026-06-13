package com.edgeedu.app

import com.edgeedu.app.data.Chunk
import com.edgeedu.app.data.IndexedChunk
import com.edgeedu.app.search.Bm25Index
import com.edgeedu.app.session.Subject
import com.edgeedu.app.tutor.LlmEngine
import com.edgeedu.app.tutor.MockLlmEngine
import com.edgeedu.app.tutor.Tutor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectSessionTest {

    @Test
    fun mapsEveryShippedSubjectString() {
        // The exact subject values appearing across the 33 curriculum files.
        val expected = mapOf(
            "Math 1" to Subject.Mathematics,
            "Math 2" to Subject.Mathematics,
            "Mathematics 1" to Subject.Mathematics,
            "Mathematics 2" to Subject.Mathematics,
            "Science and Technology" to Subject.Science,
            "Science and Technology Part 1" to Subject.Science,
            "Science and Technology Part 2" to Subject.Science,
            "Geography" to Subject.Geography,
            "History & Political Science" to Subject.SocialStudies,
            "History and Political Science" to Subject.SocialStudies,
        )
        for ((raw, subject) in expected) {
            assertEquals(raw, subject, Subject.of(raw))
        }
        assertNull(Subject.of("Sanskrit"))
    }

    @Test
    fun onlyMathematicsIsAMathSession() {
        assertTrue(Subject.Mathematics.isMath)
        assertFalse(Subject.Science.isMath)
        assertFalse(Subject.Geography.isMath)
        assertFalse(Subject.SocialStudies.isMath)
    }

    private fun chunk(id: String, subject: String, heading: String, text: String) = IndexedChunk(
        chunk = Chunk(chunk_id = id, heading = heading, text = text),
        file = "data/test.json",
        standard = 10,
        subject = subject,
        language = "English",
    )

    /** Engine that always emits a calc call, regardless of session type. */
    private class CalcHappyEngine : LlmEngine {
        override val name = "test"
        override suspend fun explain(
            question: String,
            context: List<IndexedChunk>,
            mathSession: Boolean,
        ) = "Let me compute: <calc>eval: 2 + 2</calc>"
    }

    @Test
    fun mathSessionInterceptsCalcCalls() = runTest {
        val index = Bm25Index(listOf(chunk("1.1", "Math 1", "Addition", "Adding numbers together sums them.")))
        val tutor = Tutor(index, CalcHappyEngine(), mathSession = true)
        val reply = tutor.ask("addition of numbers 2 + 2")
        assertTrue(reply.text.contains("= 4 ✓"))
        assertFalse(reply.text.contains("<calc>"))
    }

    @Test
    fun nonMathSessionNeverRunsTheInterceptor() = runTest {
        val index = Bm25Index(listOf(chunk("1.1", "Geography", "Population", "Population growth between census years.")))
        val tutor = Tutor(index, CalcHappyEngine(), mathSession = false)
        val reply = tutor.ask("population growth census years")
        // The interceptor must not run outside maths sessions: the raw output
        // passes through untouched (a real engine is prompted/grammar-gated
        // not to emit calls at all).
        assertTrue(reply.text.contains("<calc>"))
        assertFalse(reply.text.contains("✓"))
    }

    @Test
    fun mockEngineEmitsCalcOnlyInMathSessions() = runTest {
        val engine = MockLlmEngine()
        val context = listOf(chunk("1.1", "Geography", "Census", "Population in 1991 - 1981 grew."))
        val mathOut = engine.explain("what is 1991 - 1981", context, mathSession = true)
        val geoOut = engine.explain("what is 1991 - 1981", context, mathSession = false)
        assertTrue(mathOut.contains("<calc>"))
        assertFalse(geoOut.contains("<calc>"))
    }

    @Test
    fun inlineMathBypassOnlyAppliesInMathSessions() = runTest {
        // Empty index -> zero retrieval coverage. A maths session still
        // answers via the engine; a non-maths session declines.
        val index = Bm25Index(emptyList())
        val mathReply = Tutor(index, CalcHappyEngine(), mathSession = true)
            .ask("solve 2*x + 3 = 7")
        val geoReply = Tutor(index, CalcHappyEngine(), mathSession = false)
            .ask("solve 2*x + 3 = 7")
        assertTrue(mathReply.text.contains("= 4 ✓"))
        assertFalse(geoReply.grounded)
        assertTrue(geoReply.text.contains("could not find"))
    }
}
