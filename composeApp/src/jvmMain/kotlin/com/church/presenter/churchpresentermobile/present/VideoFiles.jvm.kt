package com.church.presenter.churchpresentermobile.present

// Nowhere to keep a video on this target: standalone does not run here, so the
// Media tab never offers to import one.

actual fun createVideoFiles(): VideoFiles? = null

internal actual fun videoStorageDirectory(): String? = null

internal actual fun readFileRange(path: String, offset: Long, length: Int): ByteArray? = null

internal actual suspend fun probeVideoFile(path: String, thumbPath: String): VideoProbe? = null
