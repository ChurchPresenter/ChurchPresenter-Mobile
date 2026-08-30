package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "ImageDownscale"

/** Matches the quality the picker already re-encodes at, so nothing degrades twice over. */
private const val JPEG_QUALITY = 0.85

@OptIn(ExperimentalForeignApi::class)
actual fun downscaleImage(bytes: ByteArray, maxEdge: Int): ByteArray = runCatching {
    if (bytes.isEmpty()) return bytes
    val data = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
    val image = UIImage(data = data) ?: return bytes

    val width: Double = image.size.useContents { this.width }
    val height: Double = image.size.useContents { this.height }
    val longest = max(width, height)
    if (longest <= 0.0 || longest <= maxEdge.toDouble()) return bytes

    val scale = maxEdge.toDouble() / longest
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1).toDouble()
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1).toDouble()

    // Scale 1.0: the target is already in pixels, and letting UIKit apply the
    // device's scale factor would hand back a 2x or 3x image — the opposite of
    // what this is for.
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val scaled = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    val jpeg = scaled?.let { UIImageJPEGRepresentation(it, JPEG_QUALITY) } ?: return bytes
    val result = jpeg.bytes?.readBytes(jpeg.length.toInt()) ?: return bytes
    Logger.d(TAG, "downscale — ${bytes.size} bytes -> ${result.size} bytes")
    if (result.size < bytes.size) result else bytes
}.getOrElse {
    Logger.e(TAG, "downscale — failed, sending the original: ${it.message}", it)
    bytes
}
