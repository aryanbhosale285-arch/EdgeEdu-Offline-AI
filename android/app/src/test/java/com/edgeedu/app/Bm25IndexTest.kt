package com.edgeedu.app

import com.edgeedu.app.data.Chunk
import com.edgeedu.app.data.IndexedChunk
import com.edgeedu.app.search.Bm25Index
import com.edgeedu.app.search.SearchFilters
import com.edgeedu.app.search.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Bm25IndexTest {

    private fun chunk(id: String, heading: String, text: String, language: String) = IndexedChunk(
        chunk = Chunk(chunk_id = id, heading = heading, text = text, keywords = emptyList()),
        file = "test.json",
        standard = 10,
        subject = "Math",
        language = language,
    )

    private val index = Bm25Index(
        listOf(
            chunk("1.1", "Quadratic equations", "A quadratic equation has degree two.", "English"),
            chunk("1.2", "Linear equations", "A linear equation has degree one.", "English"),
            chunk("2.1", "द्विघात समीकरण", "द्विघात समीकरण की घात दो होती है।", "Hindi"),
        )
    )

    @Test
    fun ranksTheRightChunkFirst() {
        val hits = index.search("quadratic equation")
        assertEquals("1.1", hits.first().chunk.chunk.chunk_id)
        assertTrue(hits.first().coverage > 0.9)
    }

    @Test
    fun devanagariQueriesMatchDevanagariContent() {
        val hits = index.search("द्विघात समीकरण")
        assertEquals("2.1", hits.first().chunk.chunk.chunk_id)
    }

    @Test
    fun languageFilterApplies() {
        val hits = index.search("equation", SearchFilters(language = "Hindi"))
        assertTrue(hits.isEmpty())
    }

    @Test
    fun unknownTermsLowerCoverage() {
        val hits = index.search("quadratic pasta carbonara recipe")
        assertTrue(hits.first().coverage < 0.45)
    }

    @Test
    fun tokenizerHandlesBothScripts() {
        assertEquals(listOf("solve", "2x", "3", "7"), Tokenizer.tokenize("Solve 2x + 3 = 7"))
        assertTrue(Tokenizer.isDevanagari("द्विघात समीकरण के मूल"))
        assertTrue(!Tokenizer.isDevanagari("quadratic roots"))
    }
}
