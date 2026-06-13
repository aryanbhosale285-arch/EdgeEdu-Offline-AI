package com.edgeedu.app.content

import android.content.res.AssetManager

/**
 * The origin a login-time download reads from. In this build the origin is the
 * APK's bundled assets — a local stand-in for the signed static host — so the
 * lifecycle (copy into per-profile storage, verify, delete on logout) is real
 * and demonstrable fully offline. Phase 2 swaps in an HTTP source against the
 * actual host; the [ContentProvisioner] above it is unchanged either way.
 */
interface ContentSource {
    /** Raw bytes of the signed manifest (data/manifest.json). */
    fun manifest(): ByteArray

    /** Raw bytes of one content file, named by its manifest-relative path. */
    fun open(path: String): ByteArray
}

/** Reads content from the APK's bundled assets (the offline origin stand-in). */
class AssetContentSource(private val assets: AssetManager) : ContentSource {
    override fun manifest(): ByteArray =
        assets.open("data/${ContentProvisioner.MANIFEST}").use { it.readBytes() }

    override fun open(path: String): ByteArray =
        assets.open(path).use { it.readBytes() }
}
