package com.edgeedu.app

import com.edgeedu.app.tutor.ModelConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelConfigTest {

    @Test fun defaultModelIsQwen() {
        assertTrue(ModelConfig.DEFAULT_MODEL_NAME.contains("Qwen"))
    }

    @Test fun modelFileIsAConfiguredGguf() {
        // Comes from BuildConfig.MODEL_FILE (overridable with -PmodelFile).
        assertTrue(ModelConfig.modelFileName.endsWith(".gguf"))
    }
}
