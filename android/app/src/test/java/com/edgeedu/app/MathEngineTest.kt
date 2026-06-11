package com.edgeedu.app

import com.edgeedu.app.tutor.MathEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEngineTest {

    private fun success(result: MathEngine.CalcResult): MathEngine.CalcResult.Success {
        assertTrue("expected Success, got $result", result is MathEngine.CalcResult.Success)
        return result as MathEngine.CalcResult.Success
    }

    @Test
    fun evaluatesArithmetic() {
        val result = success(MathEngine.evaluate("15*3 + 7"))
        assertTrue(result.display.endsWith("= 52"))
        assertTrue(result.verified)
    }

    @Test
    fun solvesLinearEquation() {
        val result = success(MathEngine.solveLinear("2*x + 3 = 7"))
        assertEquals("x = 2", result.display)
    }

    @Test
    fun handlesImplicitMultiplication() {
        val result = success(MathEngine.solveLinear("2x + 3 = 7"))
        assertEquals("x = 2", result.display)
    }

    @Test
    fun solvesNegativeAndFractionalRoots() {
        assertEquals("x = -2", success(MathEngine.solveLinear("3x + 10 = x + 6")).display)
        assertEquals("x = 2.5", success(MathEngine.solveLinear("2x = 5")).display)
    }

    @Test
    fun rejectsNonLinearEquation() {
        val result = MathEngine.solveLinear("x^2 = 4")
        assertTrue(result is MathEngine.CalcResult.Failure)
    }

    @Test
    fun rejectsDegenerateEquation() {
        val result = MathEngine.solveLinear("x = x + 1")
        assertTrue(result is MathEngine.CalcResult.Failure)
    }

    @Test
    fun rejectsGarbage() {
        assertTrue(MathEngine.evaluate("banana ++ 7") is MathEngine.CalcResult.Failure)
        assertTrue(MathEngine.solveLinear("no equals sign here") is MathEngine.CalcResult.Failure)
    }
}
