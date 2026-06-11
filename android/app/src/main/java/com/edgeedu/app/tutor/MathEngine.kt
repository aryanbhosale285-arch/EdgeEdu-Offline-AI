package com.edgeedu.app.tutor

import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression
import org.mariuszgromada.math.mxparser.License
import kotlin.math.abs

/**
 * Verified computation via mXparser. The LLM never calculates: it emits a
 * structured call, this engine computes, and the result is verified by
 * substitution before it reaches the student.
 */
object MathEngine {

    init {
        // mXparser >= 5 requires explicit license confirmation.
        License.iConfirmNonCommercialUse("EdgeEdu portfolio build")
    }

    sealed class CalcResult {
        /** [display] is plain text, [latex] is renderable. Verified = checked by substitution. */
        data class Success(val display: String, val latex: String, val verified: Boolean) : CalcResult()
        data class Failure(val reason: String) : CalcResult()
    }

    /** Students write "2x + 3"; mXparser needs "2*x + 3". */
    private fun normalize(expr: String): String =
        expr.replace(Regex("""(\d)\s*([a-zA-Z(])"""), "$1*$2")
            .replace(Regex("""(\))\s*([a-zA-Z(\d])"""), "$1*$2")

    private fun round(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else "%.6g".format(value).trimEnd('0').trimEnd('.')

    /** Evaluate a plain numeric expression, e.g. "15*3 + 7". */
    fun evaluate(rawExpr: String): CalcResult {
        val expr = normalize(rawExpr)
        val e = Expression(expr)
        if (!e.checkSyntax()) return CalcResult.Failure("syntax: ${e.errorMessage.trim()}")
        val value = e.calculate()
        if (value.isNaN()) return CalcResult.Failure("could not evaluate '$expr'")
        return CalcResult.Success(
            display = "$expr = ${round(value)}",
            latex = "$expr = ${round(value)}",
            verified = true,
        )
    }

    /**
     * Solve a linear equation "lhs = rhs" in one variable.
     *
     * Works algebraically for any linear form: with f(t) = lhs - rhs, linearity
     * means f(t) = f(0) + (f(1) - f(0)) t, so the root is -f(0) / (f(1) - f(0)).
     * Linearity itself is checked via f(2), and the root is verified by
     * substitution — a non-linear or unsolvable input returns Failure rather
     * than a wrong answer.
     */
    fun solveLinear(rawEquation: String, variable: String = "x"): CalcResult {
        val equation = normalize(rawEquation)
        val sides = equation.split("=")
        if (sides.size != 2) return CalcResult.Failure("expected one '=' in '$equation'")
        val f = "(${sides[0]}) - (${sides[1]})"

        fun at(value: Double): Double {
            val arg = Argument("$variable = $value")
            val e = Expression(f, arg)
            return if (e.checkSyntax()) e.calculate() else Double.NaN
        }

        val f0 = at(0.0)
        val f1 = at(1.0)
        val f2 = at(2.0)
        if (f0.isNaN() || f1.isNaN()) return CalcResult.Failure("could not parse '$equation'")
        val slope = f1 - f0
        if (abs(f2 - (2 * f1 - f0)) > 1e-9) {
            return CalcResult.Failure("'$equation' is not linear in $variable")
        }
        if (abs(slope) < 1e-12) return CalcResult.Failure("$variable cancels out in '$equation'")

        val root = -f0 / slope
        val residual = at(root)
        val verified = abs(residual) < 1e-9
        if (!verified) return CalcResult.Failure("solution failed verification")
        return CalcResult.Success(
            display = "$variable = ${round(root)}",
            latex = "$variable = ${round(root)}",
            verified = true,
        )
    }
}
