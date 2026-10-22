package com.church.presenter.churchpresentermobile.present

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.church.presenter.churchpresentermobile.util.Logger
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "CanvasFrameEncoder"

/** PNG ignores the quality argument; it is required all the same. */
private const val PNG_QUALITY = 100

actual fun encodeCanvasFrame(bitmap: ImageBitmap, maxEdge: Int): ByteArray? = runCatching {
    val source = bitmap.asAndroidBitmap()
    val scale = maxEdge.toFloat() / max(source.width, source.height)
    val scaled = if (scale >= 1f) source else Bitmap.createScaledBitmap(
        source,
        (source.width * scale).roundToInt().coerceAtLeast(1),
        (source.height * scale).roundToInt().coerceAtLeast(1),
        true,
    )
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
    if (scaled !== source) scaled.recycle()
    out.toByteArray()
}.getOrElse {
    Logger.e(TAG, "encode — failed: ${it.message}", it)
    null
}
