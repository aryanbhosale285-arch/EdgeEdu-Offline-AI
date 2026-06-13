package com.edgeedu.app.notes

/** Raised when an imported file is the wrong type, too big, or unparseable. */
class ImportException(message: String) : Exception(message)

/**
 * Validates and extracts text from an imported file (PRD §8.2). User content is
 * untrusted input (§8.3, §15 #16/#17): the type and size are checked *before*
 * parsing, and only plain-text formats are accepted in this build — text-PDF
 * (a parser dependency) and OCR/handwriting/maths-image are deliberately out of
 * scope here (§8.2, §20).
 */
object NoteImport {
    /** 2 MB cap: large enough for real notes, small enough to bound parsing. */
    const val MAX_BYTES = 2_000_000

    private val TEXT_EXTENSIONS = setOf("txt", "text", "md", "markdown")

    fun extension(fileName: String): String =
        fileName.substringAfterLast('.', "").lowercase()

    /** Verifies [fileName]/[byteCount] before any bytes are read into a parser. */
    fun validate(fileName: String, byteCount: Int) {
        val ext = extension(fileName)
        if (ext !in TEXT_EXTENSIONS) {
            throw ImportException(
                "Only text files (.txt, .md) are supported in this build — got “.$ext”."
            )
        }
        if (byteCount <= 0) throw ImportException("The file is empty.")
        if (byteCount > MAX_BYTES) {
            throw ImportException("File is too large (max ${MAX_BYTES / 1_000_000} MB).")
        }
    }

    /** Decodes validated text bytes; rejects binary masquerading as text. */
    fun extractText(fileName: String, bytes: ByteArray): String {
        validate(fileName, bytes.size)
        // A NUL byte means this isn't really text (e.g. a renamed binary).
        if (bytes.any { it == 0.toByte() }) {
            throw ImportException("This doesn't look like a text file.")
        }
        return bytes.decodeToString()
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
