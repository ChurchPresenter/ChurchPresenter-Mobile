package com.church.presenter.churchpresentermobile.present

// The per-platform pieces of [VideoFiles]. Declared here rather than beside the
// shared implementation in mobileMain: iosMain is not a child of that source set,
// so an expect there has nowhere on iOS to be completed.

/** The app's private folder for videos, created if missing; null when there is none. */
internal expect fun videoStorageDirectory(): String?

/** Up to [length] bytes of [path] from [offset], without reading what comes before. */
internal expect fun readFileRange(path: String, offset: Long, length: Int): ByteArray?

/** Duration and size of the video at [path], writing a JPEG still to [thumbPath]. */
internal expect suspend fun probeVideoFile(path: String, thumbPath: String): VideoProbe?
