package com.edgeedu.app.notes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.edgeedu.app.data.Chunk
import com.edgeedu.app.data.ChunkSource
import com.edgeedu.app.data.IndexedChunk

/**
 * On-device store for imported notes (PRD §8). Like bookmarks/notes, imported
 * files are user data and are KEPT across logout (§12.4), so this is a separate
 * DB from the wiped-on-logout content. Each file's chunks are attached to the
 * subject + class they were imported under, keeping retrieval focused (§8.3).
 * Everything stays on-device — nothing is uploaded.
 */
class NotesStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "edgeedu_notes.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE imported_files (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                subject TEXT NOT NULL,
                standard INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE imported_chunks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_id INTEGER NOT NULL,
                idx INTEGER NOT NULL,
                heading TEXT NOT NULL,
                body TEXT NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX idx_chunks_file ON imported_chunks(file_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    /** Stores a file and its chunks under [subject]+[standard]; returns its id. */
    fun addImport(name: String, subject: String, standard: Int, chunks: List<String>): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val fileId = db.insert(
                "imported_files", null,
                ContentValues().apply {
                    put("name", name)
                    put("subject", subject)
                    put("standard", standard)
                    put("created_at", System.currentTimeMillis())
                },
            )
            chunks.forEachIndexed { i, body ->
                db.insert(
                    "imported_chunks", null,
                    ContentValues().apply {
                        put("file_id", fileId)
                        put("idx", i)
                        put("heading", NoteChunker.heading(body, name))
                        put("body", body)
                    },
                )
            }
            db.setTransactionSuccessful()
            return fileId
        } finally {
            db.endTransaction()
        }
    }

    /**
     * The imported-note chunks for [subject]+[standard], as [IndexedChunk]s
     * tagged [ChunkSource.Notes] and labelled with the active [medium] so the
     * tutor's language filter keeps them in this session.
     */
    fun chunksFor(subject: String, standard: Int, medium: String): List<IndexedChunk> {
        val sql =
            """SELECT f.id, f.name, c.idx, c.heading, c.body
               FROM imported_chunks c JOIN imported_files f ON c.file_id = f.id
               WHERE f.subject = ? AND f.standard = ?
               ORDER BY f.id, c.idx"""
        return readableDatabase.rawQuery(sql, arrayOf(subject, standard.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val fileId = c.getLong(0)
                    val fileName = c.getString(1)
                    val idx = c.getInt(2)
                    add(
                        IndexedChunk(
                            chunk = Chunk(
                                chunk_id = "N$fileId.$idx",
                                heading = c.getString(3),
                                text = c.getString(4),
                            ),
                            file = fileName,
                            standard = standard,
                            subject = subject,
                            language = medium,
                            source = ChunkSource.Notes,
                        )
                    )
                }
            }
        }
    }

    /** Files imported under [subject]+[standard], with chunk counts, for the UI. */
    fun importedFiles(subject: String, standard: Int): List<ImportedFile> {
        val sql =
            """SELECT f.id, f.name, f.subject, f.standard, COUNT(c.id)
               FROM imported_files f LEFT JOIN imported_chunks c ON c.file_id = f.id
               WHERE f.subject = ? AND f.standard = ?
               GROUP BY f.id ORDER BY f.created_at DESC"""
        return readableDatabase.rawQuery(sql, arrayOf(subject, standard.toString())).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        ImportedFile(
                            id = c.getLong(0),
                            name = c.getString(1),
                            subject = c.getString(2),
                            standard = c.getInt(3),
                            chunkCount = c.getInt(4),
                        )
                    )
                }
            }
        }
    }

    fun deleteFile(fileId: Long) {
        writableDatabase.run {
            delete("imported_chunks", "file_id = ?", arrayOf(fileId.toString()))
            delete("imported_files", "id = ?", arrayOf(fileId.toString()))
        }
    }
}
