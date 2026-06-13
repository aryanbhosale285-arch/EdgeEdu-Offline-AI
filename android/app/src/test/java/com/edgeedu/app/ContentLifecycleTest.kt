package com.edgeedu.app

import com.edgeedu.app.content.ContentProvisioner
import com.edgeedu.app.content.ContentScope
import com.edgeedu.app.content.ContentSource
import com.edgeedu.app.content.DowngradeException
import com.edgeedu.app.data.CorpusRepository
import com.edgeedu.app.data.IntegrityException
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the PRD v3.1 §12 content lifecycle end-to-end off-device: download
 * (scoped + checksum-verified), load, scope isolation, downgrade rejection, and
 * logout deletion. Pure java.io, so no Robolectric/emulator needed.
 */
class ContentLifecycleTest {

    private val tmp = Files.createTempDirectory("edgeedu").toFile()
    private val contentDir = File(tmp, "content")
    private val versionFile = File(tmp, "content_version")

    @After fun cleanup() { tmp.deleteRecursively() }

    private fun sha256(b: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun doc(subject: String, std: Int, lang: String, chunkId: String) =
        ("""{"metadata":{"file_name":"f","standard":$std,"subject":"$subject",""" +
            """"language":"$lang","board":"MH","schema_version":"3.0",""" +
            """"content_version":2,"last_updated":"2026-06-13"},""" +
            """"content_chunks":[{"chunk_id":"$chunkId","heading":"H","text":"Body text."}],""" +
            """"generation_status":"complete"}""").toByteArray()

    /** A ContentSource backed by an in-memory file map + a built manifest. */
    private class MapSource(
        private val files: Map<String, ByteArray>,
        version: Int,
        hashOverride: Map<String, String> = emptyMap(),
        sha: (ByteArray) -> String,
    ) : ContentSource {
        private val manifestBytes: ByteArray
        init {
            val entries = files.entries.joinToString(",") { (path, bytes) ->
                val h = hashOverride[path] ?: sha(bytes)
                "\"$path\":{\"sha256\":\"$h\",\"bytes\":${bytes.size}}"
            }
            manifestBytes = ("""{"manifest_schema":1,"content_version":$version,""" +
                """"generated":"2026-06-13T00:00:00Z","algorithm":"sha256",""" +
                """"files":{$entries},"signature":"test-sig"}""").toByteArray()
        }
        override fun manifest() = manifestBytes
        override fun open(path: String) = files.getValue(path)
    }

    private fun corpus(version: Int = 2, hashOverride: Map<String, String> = emptyMap()) = MapSource(
        files = mapOf(
            "data/10th/10th_Geography_English.json" to doc("Geography", 10, "English", "1.1"),
            "data/10th/10th_Geography_Hindi.json" to doc("Geography", 10, "Hindi", "1.1"),
            "data/9th/9th_Math 1_English.json" to doc("Math 1", 9, "English", "2.1"),
        ),
        version = version,
        hashOverride = hashOverride,
        sha = ::sha256,
    )

    @Test
    fun downloadsOnlyTheScopedSliceAndLoadsIt() {
        val p = ContentProvisioner(contentDir, versionFile)
        val scope = ContentScope(10, "English")
        val result = p.provision(corpus(), scope)

        assertEquals(1, result.fileCount) // only the 10th English Geography file
        assertTrue(File(contentDir, "data/10th/10th_Geography_English.json").isFile)
        assertFalse(File(contentDir, "data/10th/10th_Geography_Hindi.json").exists())
        assertFalse(File(contentDir, "data/9th/9th_Math 1_English.json").exists())

        val repo = CorpusRepository.load(contentDir, scope)
        assertEquals(1, repo.fileCount)
        assertEquals(1, repo.chunks.size)
        assertEquals("Geography", repo.chunks.single().subject)
        assertEquals(2, repo.contentVersion)
    }

    @Test(expected = IntegrityException::class)
    fun rejectsTamperedContentOnDownload() {
        val p = ContentProvisioner(contentDir, versionFile)
        // Manifest declares a hash that won't match the bytes -> tamper detected.
        val bad = mapOf("data/10th/10th_Geography_English.json" to "deadbeef".repeat(8))
        p.provision(corpus(hashOverride = bad), ContentScope(10, "English"))
    }

    @Test(expected = DowngradeException::class)
    fun rejectsDowngradeAcrossRedownload() {
        val p = ContentProvisioner(contentDir, versionFile)
        val scope = ContentScope(10, "English")
        p.provision(corpus(version = 2), scope)
        p.provision(corpus(version = 1), scope) // older -> rejected
    }

    @Test
    fun logoutDeletesContentButKeepsDowngradeFloor() {
        val p = ContentProvisioner(contentDir, versionFile)
        val scope = ContentScope(10, "English")
        p.provision(corpus(version = 2), scope)

        p.clear()
        assertFalse(p.isProvisioned())
        assertFalse(contentDir.exists())
        assertTrue(versionFile.isFile) // high-water survives logout

        // A stale v1 is still rejected after logout (§12.7).
        try {
            p.provision(corpus(version = 1), scope)
            throw AssertionError("expected downgrade rejection after logout")
        } catch (_: DowngradeException) { /* expected */ }
    }

    @Test
    fun scopeMatchesByClassFolderAndMediumSuffix() {
        val s = ContentScope(10, "English")
        assertTrue(s.matches("data/10th/10th_Geography_English.json"))
        assertTrue(s.matches("data/10th/10th_Science and Technology Part 1_English.json"))
        assertFalse(s.matches("data/10th/10th_Geography_Hindi.json"))
        assertFalse(s.matches("data/9th/9th_Math 1_English.json"))
    }
}
