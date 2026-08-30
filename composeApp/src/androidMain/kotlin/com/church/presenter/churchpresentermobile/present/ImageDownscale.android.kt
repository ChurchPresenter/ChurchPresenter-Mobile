package com.church.presenter.churchpresentermobile.present

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.church.presenter.churchpresentermobile.util.Logger
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "ImageDownscale"

/** Quality for the re-encode. High enough that a projected photo shows no artefacts. */
private const val JPEG_QUALITY = 88

actual fun downscaleImage(bytes: ByteArray, maxEdge: Int): ByteArray = runCatching {
    // Read the header alone first: decoding a 12-megapixel photo to find out it is
    // too big would allocate the very memory this exists to avoid.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val longest = max(bounds.outWidth, bounds.outHeight)
    if (longest <= 0 || longest <= maxEdge) return bytes

    // inSampleSize halves in powers of two and is cheap; it gets us close, then
    // one exact scale finishes the job.
    val options = BitmapFactory.Options().apply {
        inSampleSize = generateSequence(1) { it * 2 }.first { longest / it <= maxEdge * 2 }
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return bytes
    val scale = maxEdge.toFloat() / max(decoded.width, decoded.height)
    val scaled = if (scale >= 1f) decoded else Bitmap.createScaledBitmap(
        decoded,
        (decoded.width * scale).roundToInt().coerceAtLeast(1),
        (decoded.height * scale).roundToInt().coerceAtLeast(1),
        true,
    )
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    if (scaled !== decoded) decoded.recycle()
    scaled.recycle()
    val result = out.toByteArray()
    Logger.d(TAG, "downscale — ${bytes.size} bytes -> ${result.size} bytes (${longest}px -> ${maxEdge}px)")
    // A tiny PNG can grow when re-encoded as JPEG; keep whichever is smaller.
    if (result.size < bytes.size) result else bytes
}.getOrElse {
    Logger.e(TAG, "downscale — failed, sending the original: ${it.message}", it)
    bytes
}
