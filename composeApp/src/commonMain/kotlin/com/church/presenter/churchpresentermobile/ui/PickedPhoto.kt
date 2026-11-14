package com.church.presenter.churchpresentermobile.ui

/**
 * A single picked photo: a suggested file name, and a way to read its bytes.
 *
 * The bytes are read on demand, not when the picker answers. A multi-select of
 * camera photos read up front held every original in memory at once, and a few
 * dozen of them exhausted the heap (OutOfMemoryError in `readBytes`). Callers
 * read one photo, finish with it, and only then read the next.
 *
 * @param fileName Suggested file name, e.g. `IMG_0042.jpg`.
 * @param load Reads the photo's bytes; null when it can no longer be read.
 */
class PickedPhoto(
    val fileName: String,
    private val load: suspend () -> ByteArray?,
) {
    /** A photo already in memory — for platforms and callers that have the bytes. */
    constructor(bytes: ByteArray, fileName: String) : this(fileName, { bytes })

    /**
     * Reads the photo's bytes, or null when they cannot be read. Safe to call
     * from any thread; platform loaders move their own I/O off the caller's.
     */
    suspend fun readBytes(): ByteArray? = load()
}
