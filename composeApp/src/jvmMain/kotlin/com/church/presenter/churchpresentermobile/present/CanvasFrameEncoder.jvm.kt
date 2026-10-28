package com.church.presenter.churchpresentermobile.present

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import com.church.presenter.churchpresentermobile.util.Logger
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "CanvasFrameEncoder"

/** Compose draws with Skia here, so the frame is scaled and encoded with it directly. */
actual fun encodeCanvasFrame(bitmap: ImageBitmap, maxEdge: Int): ByteArray? = runCatching {
    val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
    val scale = (maxEdge.toFloat() / max(image.width, image.height)).coerceAtMost(1f)
    val width = (image.width * scale).roundToInt().coerceAtLeast(1)
    val height = (image.height * scale).roundToInt().coerceAtLeast(1)
    val surface = Surface.makeRasterN32Premul(width, height)
    surface.canvas.drawImageRect(image, Rect.makeWH(width.toFloat(), height.toFloat()))
    surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)?.bytes
}.getOrElse {
    Logger.e(TAG, "encode — failed: ${it.message}", it)
    null
}
