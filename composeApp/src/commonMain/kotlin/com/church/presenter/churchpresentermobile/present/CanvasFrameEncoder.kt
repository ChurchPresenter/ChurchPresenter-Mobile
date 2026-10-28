package com.church.presenter.churchpresentermobile.present

import androidx.compose.ui.graphics.ImageBitmap

/**
 * The longest edge a projected canvas frame is sent at, in pixels — the same 1080p
 * ceiling as a photo ([PHOTO_MAX_EDGE]). A tablet captures its board at two or three
 * times that, every pixel of which would cross the hall's Wi-Fi on every stroke.
 */
const val CANVAS_FRAME_MAX_EDGE: Int = PHOTO_MAX_EDGE

/**
 * [bitmap] scaled down to at most [maxEdge] on its longest side and encoded as a
 * PNG — lossless, so ink and type stay crisp on a projector where a JPEG would
 * smear them. Null where the platform has no image pipeline or encoding fails;
 * the caller then leaves the screen on the last frame that did encode.
 */
expect fun encodeCanvasFrame(bitmap: ImageBitmap, maxEdge: Int = CANVAS_FRAME_MAX_EDGE): ByteArray?
