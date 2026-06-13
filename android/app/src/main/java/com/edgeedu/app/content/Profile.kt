package com.edgeedu.app.content

/**
 * The local profile (PRD §12.1): a display name plus the class+medium that
 * decides which content is downloaded. This is NOT an account — no password,
 * no auth server, nothing leaves the device. Its only job is to record which
 * curriculum slice the student is studying.
 */
data class Profile(
    val name: String,
    val standard: Int,   // 9 or 10
    val medium: String,  // "English" | "Hindi" | "Marathi"
) {
    val scope: ContentScope get() = ContentScope(standard, medium)
}

/**
 * The class+medium slice of the corpus a login works within. Files are named
 * by the build pipeline's convention (e.g. "data/10th/10th_Geography_English.json"),
 * so a scope selects exactly its class folder and language suffix — this is
 * what lets first-login download only that class+medium (§12.2).
 */
data class ContentScope(val standard: Int, val medium: String) {
    val label: String get() = "Class $standard · $medium"

    /** True if [path] (a manifest-relative content path) belongs to this scope. */
    fun matches(path: String): Boolean {
        val file = path.substringAfterLast('/')
        return path.contains("/${standard}th/") && file.endsWith("_$medium.json")
    }

    companion object {
        val MEDIUMS = listOf("English", "Hindi", "Marathi")
        val STANDARDS = listOf(9, 10)
    }
}
