package com.edgeedu.app.ui.theme

import android.content.Context

/** The user's appearance choice. [System] follows the device dark-mode setting. */
enum class ThemeMode { System, Light, Dark }

/** Persists the appearance choice across restarts (small UI pref, plain prefs). */
object ThemePrefs {
    private const val FILE = "edgeedu_prefs"
    private const val KEY = "theme_mode"

    fun load(context: Context): ThemeMode =
        runCatching {
            ThemeMode.valueOf(
                context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                    .getString(KEY, ThemeMode.System.name)!!
            )
        }.getOrDefault(ThemeMode.System)

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, mode.name).apply()
    }
}
