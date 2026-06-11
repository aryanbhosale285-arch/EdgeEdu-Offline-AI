package com.edgeedu.app.tutor

/**
 * The Kotlin interceptor from PRD §7.1: scans model output for structured
 * <calc>…</calc> calls, routes them to [MathEngine], and splices the verified
 * result back in. With the real llama.cpp engine the calls are GBNF-enforced
 * (assets/grammars/calc.gbnf); the contract here is identical.
 */
object CalcInterceptor {

    private val callPattern = Regex("""<calc>\s*(eval|solve)\s*:\s*(.+?)\s*</calc>""")

    data class Interception(
        /** Model text with each <calc> block replaced by its verified result. */
        val text: String,
        val results: List<MathEngine.CalcResult>,
    )

    fun process(modelOutput: String): Interception {
        val results = mutableListOf<MathEngine.CalcResult>()
        val text = callPattern.replace(modelOutput) { match ->
            val (op, payload) = match.destructured
            val result = when (op) {
                "solve" -> MathEngine.solveLinear(payload)
                else -> MathEngine.evaluate(payload)
            }
            results += result
            when (result) {
                is MathEngine.CalcResult.Success -> "${result.display} ✓"
                is MathEngine.CalcResult.Failure -> "[computation failed: ${result.reason}]"
            }
        }
        return Interception(text, results)
    }
}
