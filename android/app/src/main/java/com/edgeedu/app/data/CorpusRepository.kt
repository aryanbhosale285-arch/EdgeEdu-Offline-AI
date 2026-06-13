package com.edgeedu.app.data

import com.edgeedu.app.content.ContentProvisioner
import com.edgeedu.app.content.ContentScope
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.json.Json

class IntegrityException(message: String) : Exception(message)

/**
 * Loads the downloaded curriculum for one [ContentScope] from the provisioned
 * content directory, verifying every file's SHA-256 against the signed manifest
 * before use. Content is no longer bundled-and-static (PRD v3.1 §12): it is
 * downloaded per class+medium on login and deleted on logout, so a repository
 * is built fresh for the logged-in scope and discarded on logout — there is no
 * process-wide singleton anymore.
 *
 * The same per-file check runs whether the bytes came from the bundled-asset
 * origin (this build) or a future HTTP host; the manifest kept on disk retains
 * its Ed25519 signature for the (deferred) full signature verification.
 */
class CorpusRepository private constructor(
    val chunks: List<IndexedChunk>,
    val contentVersion: Int,
    val fileCount: Int,
    val verifiedSolutionChunks: Int,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }

        /**
         * Reads the [scope] slice from [contentDir] (as written by
         * [ContentProvisioner]). Files outside the scope are ignored; a scoped
         * file that is missing or fails its hash raises [IntegrityException].
         */
        fun load(contentDir: File, scope: ContentScope): CorpusRepository {
            val manifestFile = File(contentDir, ContentProvisioner.MANIFEST)
            if (!manifestFile.isFile) throw IntegrityException("content not downloaded")
            val manifest: Manifest = json.decodeFromString(manifestFile.readBytes().decodeToString())

            val chunks = mutableListOf<IndexedChunk>()
            var verifiedSolutions = 0
            var scopedFiles = 0

            for ((rel, info) in manifest.files.entries.sortedBy { it.key }) {
                if (!scope.matches(rel)) continue
                val file = File(contentDir, rel)
                if (!file.isFile) throw IntegrityException("missing content file $rel")
                val bytes = file.readBytes()
                if (sha256(bytes) != info.sha256) throw IntegrityException("hash mismatch for $rel")
                scopedFiles++

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
            if (scopedFiles == 0) throw IntegrityException("no content for ${scope.label}")

            return CorpusRepository(
                chunks = chunks,
                contentVersion = manifest.content_version,
                fileCount = scopedFiles,
                verifiedSolutionChunks = verifiedSolutions,
            )
        }
    }
}
