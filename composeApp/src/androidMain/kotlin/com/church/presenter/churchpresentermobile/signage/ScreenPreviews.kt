package com.church.presenter.churchpresentermobile.signage

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.PixelCopy
import android.view.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.churchpresenter.signage.core.MediaKind
import org.churchpresenter.signage.core.PlayerMessage
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

private const val JPEG_QUALITY = 70

/** How big a preview of a [width] × [height] screen is: no wider than [maxWidth], never enlarged. */
internal fun previewSize(width: Int, height: Int, maxWidth: Int): Pair<Int, Int> {
    val scaled = minOf(width, maxWidth).coerceAtLeast(1)
    return scaled to (height.toLong() * scaled / width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
}

/**
 * Pictures of what the TV is showing, for the desktop's Signage tab.
 *
 * A copy of the window has a hole where a video plays, because a video is drawn on
 * a surface of its own; a video's preview is therefore a frame of its file. A
 * stream has no file, so its preview is the hole.
 */
internal class ScreenPreviews(private val window: Window) {

    suspend fun take(maxWidth: Int, screen: SignageScreen): PlayerMessage.Preview? {
        val playing = screen as? SignageScreen.Playing
        val video = playing?.file?.takeIf { playing.item.kind == MediaKind.VIDEO }
        val bitmap = if (video != null) videoFrame(video, maxWidth) else windowCopy(maxWidth)
        return bitmap?.let(::encoded)
    }

    private suspend fun windowCopy(maxWidth: Int): Bitmap? {
        val view = window.decorView
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || view.width == 0) return null
        val (width, height) = previewSize(view.width, view.height, maxWidth)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return suspendCancellableCoroutine { done ->
            try {
                PixelCopy.request(
                    window,
                    bitmap,
                    { result -> done.resume(bitmap.takeIf { result == PixelCopy.SUCCESS }) },
                    Handler(Looper.getMainLooper()),
                )
            } catch (_: IllegalArgumentException) {
                // The window has no surface yet: nothing to show.
                done.resume(null)
            }
        }
    }

    private suspend fun videoFrame(file: File, maxWidth: Int): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.path)
            retriever.frameAtTime?.let { frame ->
                val (width, height) = previewSize(frame.width, frame.height, maxWidth)
                Bitmap.createScaledBitmap(frame, width, height, true)
            }
        } catch (_: IllegalArgumentException) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun encoded(bitmap: Bitmap): PlayerMessage.Preview {
        val jpeg = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, jpeg)
        return PlayerMessage.Preview(
            jpegBase64 = Base64.encodeToString(jpeg.toByteArray(), Base64.NO_WRAP),
            width = bitmap.width,
            height = bitmap.height,
            takenAtMs = System.currentTimeMillis(),
        )
    }
}
