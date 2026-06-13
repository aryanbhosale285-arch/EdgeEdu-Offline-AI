plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.edgeedu.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.edgeedu.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.3.0"

        // The signed static host the login download fetches from (PRD §9, §12.2).
        // Empty -> fall back to the bundled-asset origin so the app still demos
        // fully offline. A real build/CI sets this to the deployed host, e.g.
        //   -PcontentBaseUrl=https://edgeedu.example.com/content
        val contentBaseUrl = (project.findProperty("contentBaseUrl") as String?).orEmpty()
        buildConfigField("String", "CONTENT_BASE_URL", "\"$contentBaseUrl\"")

        // The on-device GGUF model is a config value, not a hardcoded dependency
        // (PRD §13.4) — a better model can be dropped in without code changes.
        // Default: Qwen2.5-3B-Instruct, Q4_K_M (PRD §13.2). Override with
        //   -PmodelFile=<other>.gguf
        val modelFile = (project.findProperty("modelFile") as String?)
            ?: "qwen2.5-3b-instruct-q4_k_m.gguf"
        buildConfigField("String", "MODEL_FILE", "\"$modelFile\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // On-device LLM via llama.cpp. Off by default so the app builds without
    // the NDK; enable with -PenableLlama after cloning llama.cpp into
    // app/src/main/cpp/llama.cpp (see cpp/CMakeLists.txt).
    if (project.hasProperty("enableLlama")) {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
        defaultConfig.ndk.abiFilters += listOf("arm64-v8a")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Verified computation: the LLM explains, this engine calculates.
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:5.2.1")
    // Offline PDF text extraction for bring-your-own-notes (PRD §8.2).
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    // On-device OCR for photo imports (printed text) — bundled model, no network.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // WebViewAssetLoader for the offline KaTeX renderer.
    implementation("androidx.webkit:webkit:1.12.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
