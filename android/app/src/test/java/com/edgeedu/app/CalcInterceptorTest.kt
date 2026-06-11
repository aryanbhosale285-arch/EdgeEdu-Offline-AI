package com.edgeedu.app

import com.edgeedu.app.tutor.CalcInterceptor
import com.edgeedu.app.tutor.MathEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcInterceptorTest {

    @Test
    fun replacesCalcBlockWithVerifiedResult() {
        val out = CalcInterceptor.process(
            "Subtract 3 from both sides. <calc>solve: 2*x + 3 = 7</calc> Check by substitution."
        )
        assertTrue(out.text.contains("x = 2 ✓"))
        assertTrue(!out.text.contains("<calc>"))
        assertEquals(1, out.results.size)
        assertTrue(out.results[0] is MathEngine.CalcResult.Success)
    }

    @Test
    fun handlesEvalAndMultipleBlocks() {
        val out = CalcInterceptor.process(
            "First <calc>eval: 14 + 11</calc>, then <calc>eval: 99 * 3</calc>."
        )
        assertTrue(out.text.contains("= 25 ✓"))
        assertTrue(out.text.contains("= 297 ✓"))
        assertEquals(2, out.results.size)
    }

    @Test
    fun surfacesFailureInsteadOfWrongMaths() {
        val out = CalcInterceptor.process("<calc>solve: x^2 = 4</calc>")
        assertTrue(out.text.contains("computation failed"))
        assertTrue(out.results[0] is MathEngine.CalcResult.Failure)
    }

    @Test
    fun leavesProseUntouched() {
        val text = "Plain explanation with no tool calls, even mentioning 2 + 2."
        assertEquals(text, CalcInterceptor.process(text).text)
    }
}
