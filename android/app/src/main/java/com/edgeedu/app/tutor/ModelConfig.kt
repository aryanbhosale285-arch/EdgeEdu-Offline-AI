package com.edgeedu.app.tutor

import android.content.Context
import com.edgeedu.app.BuildConfig
import java.io.File

/**
 * The explanation model as a swappable config value (PRD §13.4). The default is
 * Qwen2.5-3B-Instruct (Q4_K_M GGUF, §13.2), but the file name comes from
 * [BuildConfig.MODEL_FILE], so a different GGUF can be dropped in at build time
 * (`-PmodelFile=…`) without touching the inference code.
 *
 * The model is not bundled in the APK (it's ~2 GB); it's expected under the
 * app's private models dir, where a downloader/side-load can place it. When
 * absent, the app falls back to the extractive mock so it still runs.
 */
object ModelConfig {
    const val DEFAULT_MODEL_NAME = "Qwen2.5-3B-Instruct"

    /** GGUF file name for the active model (config-driven). */
    val modelFileName: String get() = BuildConfig.MODEL_FILE

    /** Absolute path where the GGUF is expected on device. */
    fun modelPath(context: Context): String =
        File(File(context.filesDir, "models"), modelFileName).absolutePath

    /**
     * Builds the active engine: the llama.cpp engine when its native lib is
     * built (`-PenableLlama`) and the model file is present, otherwise the
     * extractive [MockLlmEngine] so the app always works (§13.5).
     */
    fun createEngine(context: Context): LlmEngine {
        val grammar = runCatching {
            context.assets.open("grammars/calc.gbnf").use { it.readBytes().decodeToString() }
        }.getOrDefault("")
        val llama = LlamaCppEngine(modelPath(context), grammar)
        return if (llama.isAvailable) llama else MockLlmEngine()
    }
}
