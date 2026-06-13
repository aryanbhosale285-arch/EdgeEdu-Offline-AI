package com.edgeedu.app.content

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Reads content over HTTP from the signed static host (PRD §9, §12.2) for the
 * real first-login download. The bundled [AssetContentSource] stays as the
 * offline fallback when no host is configured. Either way the bytes are still
 * SHA-256 verified against the manifest by [ContentProvisioner], so a hostile
 * or corrupted host cannot inject content — the network is untrusted transport,
 * not a trusted source.
 */
class HttpContentSource(
    baseUrl: String,
    private val timeoutMs: Int = 15_000,
) : ContentSource {
    private val base = baseUrl.trimEnd('/')

    override fun manifest(): ByteArray = fetch("data/${ContentProvisioner.MANIFEST}")

    override fun open(path: String): ByteArray = fetch(path)

    private fun fetch(path: String): ByteArray {
        // Curriculum filenames contain spaces ("Mathematics 1"), so encode each
        // path segment while leaving the separators intact.
        val encoded = path.split('/').joinToString("/") { segment ->
            URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }
        val conn = (URL("$base/$encoded").openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            requestMethod = "GET"
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code for $path")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }
}
