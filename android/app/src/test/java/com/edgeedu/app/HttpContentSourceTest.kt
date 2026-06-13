package com.edgeedu.app

import com.edgeedu.app.content.ContentProvisioner
import com.edgeedu.app.content.ContentScope
import com.edgeedu.app.content.HttpContentSource
import com.edgeedu.app.data.CorpusRepository
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.URLDecoder
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase-2 download path: verifies [HttpContentSource] fetches the manifest and
 * scoped files over real HTTP, and that the provisioner's SHA-256 check still
 * guards the result. Uses a raw-socket mini HTTP server (the Android unit-test
 * classpath hides com.sun.net.httpserver) — hermetic, no emulator.
 */
class HttpContentSourceTest {

    private val tmp = Files.createTempDirectory("edgeedu-http").toFile()
    private lateinit var server: ServerSocket
    @Volatile private var running = true
    private fun baseUrl() = "http://localhost:${server.localPort}"

    private fun sha256(b: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun doc(lang: String) =
        ("""{"metadata":{"file_name":"f","standard":10,"subject":"Geography",""" +
            """"language":"$lang","board":"MH","schema_version":"3.0","content_version":3,""" +
            """"last_updated":"2026-06-13"},"content_chunks":[{"chunk_id":"1.1",""" +
            """"heading":"H","text":"Body."}],"generation_status":"complete"}""").toByteArray()

    @Before
    fun startServer() {
        // A filename with a space exercises path encoding end-to-end.
        val files = mapOf(
            "data/10th/10th_Geography_English.json" to doc("English"),
            "data/10th/10th_Science and Technology Part 1_English.json" to doc("English"),
        )
        val entries = files.entries.joinToString(",") { (path, bytes) ->
            "\"$path\":{\"sha256\":\"${sha256(bytes)}\",\"bytes\":${bytes.size}}"
        }
        val manifest = ("""{"manifest_schema":1,"content_version":3,""" +
            """"generated":"2026-06-13T00:00:00Z","algorithm":"sha256",""" +
            """"files":{$entries},"signature":"test-sig"}""").toByteArray()
        val routes = files + ("data/${ContentProvisioner.MANIFEST}" to manifest)

        server = ServerSocket(0)
        thread(isDaemon = true) {
            while (running) {
                val socket = try { server.accept() } catch (e: Exception) { break }
                socket.use { s ->
                    val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                    val requestLine = reader.readLine() ?: return@use
                    // "GET /<path> HTTP/1.1"
                    val rawPath = requestLine.split(" ").getOrNull(1)?.removePrefix("/") ?: return@use
                    val path = URLDecoder.decode(rawPath, "UTF-8")
                    val body = routes[path]
                    val out = s.getOutputStream()
                    if (body == null) {
                        out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    } else {
                        out.write(
                            ("HTTP/1.1 200 OK\r\nContent-Length: ${body.size}\r\n" +
                                "Connection: close\r\n\r\n").toByteArray()
                        )
                        out.write(body)
                    }
                    out.flush()
                }
            }
        }
    }

    @After
    fun stop() {
        running = false
        server.close()
        tmp.deleteRecursively()
    }

    @Test
    fun downloadsAndVerifiesOverHttp() {
        val provisioner = ContentProvisioner(File(tmp, "content"), File(tmp, "content_version"))
        val scope = ContentScope(10, "English")
        var lastTotal = -1
        val result = provisioner.provision(HttpContentSource(baseUrl()), scope) { _, total ->
            lastTotal = total
        }

        assertEquals(2, result.fileCount)
        assertEquals(2, lastTotal)
        val repo = CorpusRepository.load(File(tmp, "content"), scope)
        assertEquals(2, repo.fileCount)
        assertTrue(repo.chunks.all { it.subject == "Geography" })
    }

    @Test(expected = java.io.IOException::class)
    fun surfacesHttpErrorsForMissingFiles() {
        HttpContentSource(baseUrl()).open("data/10th/does_not_exist.json")
    }
}
