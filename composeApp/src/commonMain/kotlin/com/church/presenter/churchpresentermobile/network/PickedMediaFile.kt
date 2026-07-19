package com.church.presenter.churchpresentermobile.network

import io.ktor.utils.io.ByteWriteChannel

/**
 * A picked media file that streams its bytes on demand rather than holding the whole
 * file in memory — essential for large videos (Android's heap can't fit a 100 MB+ array).
 *
 * @property fileName   original file name (used for the server-side name + extension)
 * @property sizeBytes  total size in bytes (0 if unknown) — used for the Content-Length + progress
 * @property streamTo   writes the file to [channel] in chunks, reporting cumulative bytes sent
 */
class PickedMediaFile(
    val fileName: String,
    val sizeBytes: Long,
    val streamTo: suspend (channel: ByteWriteChannel, onProgress: (Long) -> Unit) -> Unit,
)
