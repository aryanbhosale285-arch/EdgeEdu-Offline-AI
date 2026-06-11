package com.edgeedu.app.data

import android.content.Context
import java.security.MessageDigest
import kotlinx.serialization.json.Json

class IntegrityException(message: String) : Exception(message)

/**
 * Loads the bundled curriculum from assets, verifying every file's SHA-256
 * against the signed manifest before use. The APK signature protects the
 * bundle as a whole; this check additionally pins each content file to the
 * build pipeline's manifest, and is the same code path a future downloader
 * must pass (plus Ed25519 manifest-signature verification, bundled key in
 * assets/keys/).
 */
class CorpusRepository private constructor(
    val chunks: List<IndexedChunk>,
    val contentVersion: Int,
    val fileCount: Int,
    val verifiedSolutionChunks: Int,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        @Volatile
        private var instance: CorpusRepository? = null

        fun get(context: Context): CorpusRepository =
            instance ?: synchronized(this) {
                instance ?: load(context.applicationContext).also { instance = it }
            }

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }

        private fun load(context: Context): CorpusRepository {
            val assets = context.assets
            val manifest: Manifest = json.decodeFromString(
                assets.open("data/manifest.json").readBytes().decodeToString()
            )

            val chunks = mutableListOf<IndexedChunk>()
            var verifiedSolutions = 0

            for ((rel, info) in manifest.files.entries.sortedBy { it.key }) {
                // Manifest paths are repo-relative ("data/10th/x.json"); the
                // same tree is bundled under assets/.
                val bytes = assets.open(rel).readBytes()
                val digest = sha256(bytes)
                if (digest != info.sha256) {
                    throw IntegrityException("hash mismatch for $rel")
                }
                val doc: CurriculumDoc = json.decodeFromString(bytes.decodeToString())
                for (chunk in doc.content_chunks) {
                    if (!chunk.solution_steps.isNullOrEmpty()) verifiedSolutions++
                    chunks += IndexedChunk(
                        chunk = chunk,
                        file = rel,
                        standard = doc.metadata.standard,
                        subject = doc.metadata.subject,
                        language = doc.metadata.language,
                    )
                }
            }
            return CorpusRepository(
                chunks = chunks,
                contentVersion = manifest.content_version,
                fileCount = manifest.files.size,
                verifiedSolutionChunks = verifiedSolutions,
            )
        }
    }
}
