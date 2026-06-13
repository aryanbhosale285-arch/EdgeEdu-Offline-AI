package com.edgeedu.app

import com.edgeedu.app.data.Chunk
import com.edgeedu.app.data.ChunkSource
import com.edgeedu.app.data.IndexedChunk
import com.edgeedu.app.notes.ImportException
import com.edgeedu.app.notes.NoteChunker
import com.edgeedu.app.notes.NoteImport
import com.edgeedu.app.search.Bm25Index
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesImportTest {

    // ---- validation (untrusted input, PRD §8.3 / §15 #16-17) ----

    @Test fun acceptsSupportedFiles() {
        NoteImport.validate("chemistry.txt", 1000)
        NoteImport.validate("notes.md", 1000)
        NoteImport.validate("notes.json", 1000)
        NoteImport.validate("chapter.pdf", 1000) // PDF now allowed (extracted separately)
    }

    @Test(expected = ImportException::class)
    fun rejectsUnsupportedExtension() {
        NoteImport.validate("photo.png", 1000)
    }

    @Test fun extractsReadableStringsFromJson() {
        val json = """{"heading":"Cells","text":"The basic unit of life","difficulty":2}""".toByteArray()
        val text = NoteImport.extractText("notes.json", json)
        assertTrue(text.contains("Cells"))
        assertTrue(text.contains("The basic unit of life"))
        assertFalse(text.contains("difficulty")) // keys/numbers dropped, only string values
    }

    @Test(expected = ImportException::class)
    fun rejectsOversizeFile() {
        NoteImport.validate("big.txt", NoteImport.MAX_BYTES + 1)
    }

    @Test(expected = ImportException::class)
    fun rejectsBinaryMasqueradingAsText() {
        NoteImport.extractText("evil.txt", byteArrayOf(72, 105, 0, 1, 2))
    }

    @Test fun extractsPlainText() {
        assertEquals("Hello notes", NoteImport.extractText("a.txt", "Hello notes".toByteArray()))
    }

    // ---- chunking (PRD §8.1) ----

    @Test fun shortTextIsOneChunk() {
        assertEquals(1, NoteChunker.chunk("a short note about photosynthesis").size)
    }

    @Test fun longTextSplitsWithOverlap() {
        val text = (1..300).joinToString(" ") { "word$it" }
        val chunks = NoteChunker.chunk(text)
        assertTrue("expected multiple chunks", chunks.size > 1)
        // Overlap: the last OVERLAP_WORDS of chunk 0 reappear at the start of chunk 1.
        val tail = chunks[0].split(" ").takeLast(NoteChunker.OVERLAP_WORDS)
        val head = chunks[1].split(" ").take(NoteChunker.OVERLAP_WORDS)
        assertEquals(tail, head)
    }

    @Test fun chunkCountIsCapped() {
        val text = (1..100_000).joinToString(" ") { "w$it" }
        assertTrue(NoteChunker.chunk(text).size <= NoteChunker.MAX_CHUNKS)
    }

    // ---- dual-corpus retrieval with source tagging (PRD §8.1, §8.3) ----

    private fun textbook(id: String, heading: String, text: String) = IndexedChunk(
        chunk = Chunk(chunk_id = id, heading = heading, text = text),
        file = "data/10th/10th_Science_English.json",
        standard = 10, subject = "Science", language = "English",
        source = ChunkSource.Textbook,
    )

    private fun note(id: String, text: String) = IndexedChunk(
        chunk = Chunk(chunk_id = id, heading = "note", text = text),
        file = "my_notes.txt",
        standard = 10, subject = "Science", language = "English",
        source = ChunkSource.Notes,
    )

    @Test fun retrievalSpansTextbookAndNotesAndKeepsSource() {
        val index = Bm25Index(
            listOf(
                textbook("1.1", "Cells", "A cell is the basic unit of life in biology."),
                note("N1.0", "My teacher said mitochondria is the powerhouse of the cell."),
            )
        )
        val mitoHit = index.search("mitochondria powerhouse", k = 5).first()
        assertEquals(ChunkSource.Notes, mitoHit.chunk.source)

        val cellHit = index.search("basic unit of life", k = 5).first()
        assertEquals(ChunkSource.Textbook, cellHit.chunk.source)
    }
}
