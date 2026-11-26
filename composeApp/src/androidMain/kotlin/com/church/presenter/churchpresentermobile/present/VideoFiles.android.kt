package com.church.presenter.churchpresentermobile.present

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.church.presenter.churchpresentermobile.model.getAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

private const val DIRECTORY = "videos"

/** Where in the clip the list's still is taken from — past any fade-in from black. */
private const val THUMB_AT_US = 1_000_000L

/** The list shows thumbnails at 96dp; this is generous for any phone's density. */
private const val THUMB_WIDTH_PX = 480
private const val THUMB_QUALITY = 80
private const val QUARTER_TURN = 90
private const val THREE_QUARTER_TURN = 270

/** The app's private files directory — no permissions needed, cleared on uninstall. */
internal actual fun videoStorageDirectory(): String? =
    getAppContext()?.filesDir?.let { File(it, DIRECTORY).apply { mkdirs() } }?.absolutePath

internal actual fun readFileRange(path: String, offset: Long, length: Int): ByteArray? {
    val file = File(path)
    if (!file.exists()) return null
    RandomAccessFile(file, "r").use { raf ->
        val available = (raf.length() - offset).coerceAtLeast(0L)
        val count = minOf(length.toLong(), available).toInt()
        if (count == 0) return ByteArray(0)
        val bytes = ByteArray(count)
        raf.seek(offset)
        raf.readFully(bytes)
        return bytes
    }
}

internal actual suspend fun probeVideoFile(path: String, thumbPath: String): VideoProbe? =
    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val duration = retriever.int(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
            val rawWidth = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val rawHeight = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotation = retriever.int(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            // A phone held upright records landscape pixels plus a rotation; the
            // list should say what the room will see, which is portrait.
            val sideways = rotation == QUARTER_TURN || rotation == THREE_QUARTER_TURN
            retriever.getFrameAtTime(THUMB_AT_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.let { frame -> writeThumbnail(frame, thumbPath) }
            VideoProbe(
                durationMs = duration,
                width = if (sideways) rawHeight else rawWidth,
                height = if (sideways) rawWidth else rawHeight,
            )
        } finally {
            retriever.release()
        }
    }

private fun MediaMetadataRetriever.int(key: Int): Int = extractMetadata(key)?.toIntOrNull() ?: 0

private fun writeThumbnail(frame: Bitmap, thumbPath: String) {
    val scaled = if (frame.width > THUMB_WIDTH_PX) {
        val height = (frame.height.toLong() * THUMB_WIDTH_PX / frame.width).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(frame, THUMB_WIDTH_PX, height, true)
    } else {
        frame
    }
    FileOutputStream(thumbPath).use { scaled.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, it) }
    if (scaled !== frame) scaled.recycle()
    frame.recycle()
}
