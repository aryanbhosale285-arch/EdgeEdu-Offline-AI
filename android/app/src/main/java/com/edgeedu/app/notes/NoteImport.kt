package com.edgeedu.app.notes

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Raised when an imported file is the wrong type, too big, or unparseable. */
class ImportException(message: String) : Exception(message)

/**
 * Validates and extracts text from an imported file (PRD §8.2). User content is
 * untrusted input (§8.3, §15 #16/#17): the type and size are checked *before*
 * parsing. Supported here: plain text (.txt/.md), JSON, and text-based PDF.
 * OCR/handwriting/maths-image PDFs remain out of scope (§8.2, §20).
 */
object NoteImport {
    /** 50 MB cap: generous for PDFs and photos, still bounded for on-device parsing. */
    const val MAX_BYTES = 50_000_000

    private val TEXT_EXTENSIONS = setOf("txt", "text", "md", "markdown", "json")
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp")
    private val SUPPORTED = TEXT_EXTENSIONS + "pdf" + IMAGE_EXTENSIONS

    fun extension(fileName: String): String =
        fileName.substringAfterLast('.', "").lowercase()

    fun isPdf(fileName: String): Boolean = extension(fileName) == "pdf"
    fun isImage(fileName: String): Boolean = extension(fileName) in IMAGE_EXTENSIONS

    /** Type check only — usable before the file's size is known (e.g. images). */
    fun validateType(fileName: String) {
        val ext = extension(fileName)
        if (ext !in SUPPORTED) {
            throw ImportException(
                "Unsupported file “.$ext”. Import a .txt, .md, .json, .pdf or photo."
            )
        }
    }

    /** Verifies [fileName]/[byteCount] before any bytes are read into a parser. */
    fun validate(fileName: String, byteCount: Int) {
        validateType(fileName)
        if (byteCount <= 0) throw ImportException("The file is empty.")
        if (byteCount > MAX_BYTES) {
            throw ImportException("File is too large (max ${MAX_BYTES / 1_000_000} MB).")
        }
    }

    /**
     * Extracts text from a TEXT-format file (.txt/.md/.json). PDFs and images
     * are handled separately ([PdfTextExtractor] / [ImageTextExtractor]) because
     * they need Android resources.
     */
    fun extractText(fileName: String, bytes: ByteArray): String {
        validate(fileName, bytes.size)
        if (isPdf(fileName) || isImage(fileName)) {
            // Defensive: callers route these to their own extractors.
            throw ImportException("This file type is extracted separately.")
        }
        if (extension(fileName) == "json") return jsonToText(bytes)
        // A NUL byte means this isn't really text (e.g. a renamed binary).
        if (bytes.any { it == 0.toByte() }) {
            throw ImportException("This doesn't look like a text file.")
        }
        return bytes.decodeToString()
    }

    /**
     * Pulls the human-readable string values out of a JSON file (headings,
     * explanations, etc.) so it indexes cleanly as notes rather than as a wall
     * of braces. Falls back to the raw text if it isn't valid JSON.
     */
    fun jsonToText(bytes: ByteArray): String {
        val raw = bytes.decodeToString()
        return try {
            val sb = StringBuilder()
            fun walk(e: JsonElement) {
                when (e) {
                    is JsonObject -> e.values.forEach(::walk)
                    is JsonArray -> e.forEach(::walk)
                    is JsonPrimitive -> if (e.isString && e.content.isNotBlank()) {
                        sb.append(e.content).append('\n')
                    }
                }
            }
            walk(Json.parseToJsonElement(raw))
            sb.toString().ifBlank { raw }
        } catch (_: Exception) {
            raw
        }
    }
}

/** One imported file's metadata, as listed in the UI. */
data class ImportedFile(
    val id: Long,
    val name: String,
    val subject: String,
    val standard: Int,
    val chunkCount: Int,
)

/**
 * Splits extracted note text into overlapping retrieval chunks (PRD §8.1) — the
 * same retrieve-only-the-relevant-piece reason the curriculum is chunked, so a
 * long file fits a small model's context. Word-based with a small overlap so a
 * passage spanning a boundary is still found from either side.
 */
object NoteChunker {
    const val WORDS_PER_CHUNK = 120
    const val OVERLAP_WORDS = 20
    /** Cap chunks per file so a pathological import can't blow up the index (§15 #17). */
    const val MAX_CHUNKS = 400

    fun chunk(text: String): List<String> {
        val words = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        if (words.size <= WORDS_PER_CHUNK) return listOf(words.joinToString(" "))

        val step = WORDS_PER_CHUNK - OVERLAP_WORDS
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < words.size && chunks.size < MAX_CHUNKS) {
            val end = minOf(start + WORDS_PER_CHUNK, words.size)
            chunks += words.subList(start, end).joinToString(" ")
            if (end == words.size) break
            start += step
        }
        return chunks
    }

    /** A short display heading for a chunk: its first few words. */
    fun heading(chunkText: String, fileName: String): String {
        val snippet = chunkText.split(Regex("""\s+""")).take(8).joinToString(" ")
        return if (snippet.isBlank()) fileName else snippet
    }
}
