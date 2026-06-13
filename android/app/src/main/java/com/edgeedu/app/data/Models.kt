package com.edgeedu.app.data

import kotlinx.serialization.Serializable

@Serializable
data class SolutionStep(
    val text: String,
    val latex: String,
    val verified: Boolean,
)

@Serializable
data class Chunk(
    val chunk_id: String,
    val heading: String,
    val keywords: List<String> = emptyList(),
    val text: String,
    val difficulty: Int = 3,
    val importance: Int = 3,
    val linked_concepts: List<String> = emptyList(),
    val prerequisites: List<String> = emptyList(),
    val latex: String? = null,
    val solution_steps: List<SolutionStep>? = null,
)

@Serializable
data class DocMeta(
    val file_name: String,
    val standard: Int,
    val subject: String,
    val language: String,
    val board: String,
    val schema_version: String,
    val content_version: Int,
    val last_updated: String,
)

@Serializable
data class CurriculumDoc(
    val metadata: DocMeta,
    val content_chunks: List<Chunk>,
    val generation_status: String,
)

@Serializable
data class ManifestFileInfo(val sha256: String, val bytes: Long)

@Serializable
data class Manifest(
    val manifest_schema: Int,
    val content_version: Int,
    val generated: String,
    val algorithm: String,
    val files: Map<String, ManifestFileInfo>,
    val signature: String,
)

/** Where a retrievable chunk came from — the curriculum, or the student's
 * own imported notes (PRD §8.3, source labelling). */
enum class ChunkSource { Textbook, Notes }

/** A chunk flattened with its document context, as held in the search index. */
data class IndexedChunk(
    val chunk: Chunk,
    val file: String,
    val standard: Int,
    val subject: String,
    val language: String,
    val source: ChunkSource = ChunkSource.Textbook,
)
