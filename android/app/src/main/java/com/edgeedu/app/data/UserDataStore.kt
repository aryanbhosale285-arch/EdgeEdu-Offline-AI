package com.edgeedu.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** A bookmarked chunk, denormalized enough to display without the corpus. */
data class BookmarkEntry(
    val key: String,
    val file: String,
    val chunkId: String,
    val heading: String,
    val subject: String,
    val standard: Int,
    val language: String,
    val createdAt: Long,
)

/**
 * On-device persistence for bookmarks and notes (PRD §6.7). Plain SQLite —
 * the data is non-personal study metadata; SQLCipher encryption is
 * deliberately deferred production work (PRD §13.2).
 */
class UserDataStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "edgeedu_user.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE bookmarks (
                chunk_key TEXT PRIMARY KEY,
                file TEXT NOT NULL,
                chunk_id TEXT NOT NULL,
                heading TEXT NOT NULL,
                subject TEXT NOT NULL,
                standard INTEGER NOT NULL,
                language TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE notes (
                chunk_key TEXT PRIMARY KEY,
                text TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun bookmarks(): List<BookmarkEntry> =
        readableDatabase.query(
            "bookmarks", null, null, null, null, null, "created_at DESC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        BookmarkEntry(
                            key = cursor.getString(0),
                            file = cursor.getString(1),
                            chunkId = cursor.getString(2),
                            heading = cursor.getString(3),
                            subject = cursor.getString(4),
                            standard = cursor.getInt(5),
                            language = cursor.getString(6),
                            createdAt = cursor.getLong(7),
                        )
                    )
                }
            }
        }

    fun addBookmark(entry: BookmarkEntry) {
        writableDatabase.insertWithOnConflict(
            "bookmarks", null,
            ContentValues().apply {
                put("chunk_key", entry.key)
                put("file", entry.file)
                put("chunk_id", entry.chunkId)
                put("heading", entry.heading)
                put("subject", entry.subject)
                put("standard", entry.standard)
                put("language", entry.language)
                put("created_at", entry.createdAt)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun removeBookmark(key: String) {
        writableDatabase.delete("bookmarks", "chunk_key = ?", arrayOf(key))
    }

    fun notes(): Map<String, String> =
        readableDatabase.query(
            "notes", arrayOf("chunk_key", "text"), null, null, null, null, null
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1))
            }
        }

    /** Saves [text] for [key]; a blank text deletes the note. */
    fun putNote(key: String, text: String) {
        if (text.isBlank()) {
            writableDatabase.delete("notes", "chunk_key = ?", arrayOf(key))
        } else {
            writableDatabase.insertWithOnConflict(
                "notes", null,
                ContentValues().apply {
                    put("chunk_key", key)
                    put("text", text)
                    put("updated_at", System.currentTimeMillis())
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
    }

    companion object {
        fun keyOf(chunk: IndexedChunk) = "${chunk.file}::${chunk.chunk.chunk_id}"
    }
}
