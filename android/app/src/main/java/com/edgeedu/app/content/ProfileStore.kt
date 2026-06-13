package com.edgeedu.app.content

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Persists the single local profile (PRD §12.1, §12.3): it survives app
 * restarts and reboots, and is cleared only on explicit logout. Kept in its
 * own DB, separate from bookmarks/notes, because their logout lifecycles
 * differ — the profile is wiped on logout, user data is not (§12.4).
 */
class ProfileStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "edgeedu_profile.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE profile (
                id INTEGER PRIMARY KEY CHECK (id = 0),
                name TEXT NOT NULL,
                standard INTEGER NOT NULL,
                medium TEXT NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun current(): Profile? =
        readableDatabase.query(
            "profile", arrayOf("name", "standard", "medium"), "id = 0", null, null, null, null
        ).use { c ->
            if (c.moveToFirst()) Profile(c.getString(0), c.getInt(1), c.getString(2)) else null
        }

    fun save(profile: Profile) {
        writableDatabase.insertWithOnConflict(
            "profile", null,
            ContentValues().apply {
                put("id", 0)
                put("name", profile.name)
                put("standard", profile.standard)
                put("medium", profile.medium)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun clear() {
        writableDatabase.delete("profile", null, null)
    }
}
