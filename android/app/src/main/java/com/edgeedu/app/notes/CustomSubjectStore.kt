package com.edgeedu.app.notes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.edgeedu.app.data.Chunk
import com.edgeedu.app.data.ChunkSource
import com.edgeedu.app.data.IndexedChunk

/** A user-created subject and how much content it has. */
data class CustomSubject(
    val id: Long,
    val name: String,
    val standard: Int,
    val chunkCount: Int,
)

/**
 * Stores user-created subjects and their structured note-chunks (the offline
 * "add your own subject" feature). Kept across logout like other user data
 * (PRD §12.4), keyed by class. The chunks are produced by [NoteStructurer].
 */
class CustomSubjectStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "edgeedu_custom.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE custom_subjects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                standard INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE custom_chunks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject_id INTEGER NOT NULL,
                idx INTEGER NOT NULL,
                heading TEXT NOT NULL,
                body TEXT NOT NULL,
                keywords TEXT NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX idx_custom_chunks ON custom_chunks(subject_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun create(name: String, standard: Int): Long =
        writableDatabase.insert(
            "custom_subjects", null,
            ContentValues().apply {
                put("name", name)
                put("standard", standard)
                put("created_at", System.currentTimeMillis())
            },
        )

    fun subjects(standard: Int): List<CustomSubject> {
        val sql =
            """SELECT s.id, s.name, s.standard, COUNT(c.id)
               FROM custom_subjects s LEFT JOIN custom_chunks c ON c.subject_id = s.id
               WHERE s.standard = ?
               GROUP BY s.id ORDER BY s.created_at DESC"""
        return readableDatabase.rawQuery(sql, arrayOf(standard.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(CustomSubject(c.getLong(0), c.getString(1), c.getInt(2), c.getInt(3)))
                }
            }
        }
    }

    /** Appends [chunks] to a subject, continuing the chunk index. */
    fun appendChunks(subjectId: Long, chunks: List<StructuredChunk>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var idx = nextIndex(db, subjectId)
            for (ch in chunks) {
                db.insert(
                    "custom_chunks", null,
                    ContentValues().apply {
                        put("subject_id", subjectId)
                        put("idx", idx++)
                        put("heading", ch.heading)
                        put("body", ch.body)
                        put("keywords", ch.keywords.joinToString("\n"))
                    },
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun nextIndex(db: SQLiteDatabase, subjectId: Long): Int =
        db.rawQuery(
            "SELECT COALESCE(MAX(idx), -1) + 1 FROM custom_chunks WHERE subject_id = ?",
            arrayOf(subjectId.toString()),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /** The subject's chunks as retrievable [IndexedChunk]s, tagged as notes. */
    fun chunksFor(subjectId: Long, medium: String): List<IndexedChunk> {
        val sql =
            """SELECT s.name, s.standard, c.idx, c.heading, c.body, c.keywords
               FROM custom_chunks c JOIN custom_subjects s ON c.subject_id = s.id
               WHERE c.subject_id = ? ORDER BY c.idx"""
        return readableDatabase.rawQuery(sql, arrayOf(subjectId.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val name = c.getString(0)
                    add(
                        IndexedChunk(
                            chunk = Chunk(
                                chunk_id = "C$subjectId.${c.getInt(2)}",
                                heading = c.getString(3),
                                text = c.getString(4),
                                keywords = c.getString(5).split("\n").filter { it.isNotBlank() },
                            ),
                            file = "custom:$subjectId",
                            standard = c.getInt(1),
                            subject = name,
                            language = medium,
                            source = ChunkSource.Notes,
                        )
                    )
                }
            }
        }
    }

    /** All structured chunks for a subject — used to (re)generate the JSON file. */
    fun structuredChunks(subjectId: Long): List<StructuredChunk> =
        readableDatabase.rawQuery(
            "SELECT heading, body, keywords FROM custom_chunks WHERE subject_id = ? ORDER BY idx",
            arrayOf(subjectId.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        StructuredChunk(
                            heading = c.getString(0),
                            body = c.getString(1),
                            keywords = c.getString(2).split("\n").filter { it.isNotBlank() },
                        )
                    )
                }
            }
        }

    fun delete(subjectId: Long) {
        writableDatabase.run {
            delete("custom_chunks", "subject_id = ?", arrayOf(subjectId.toString()))
            delete("custom_subjects", "id = ?", arrayOf(subjectId.toString()))
        }
    }
}
