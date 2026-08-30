package com.church.presenter.churchpresentermobile.present

/**
 * The longest edge a photo is kept at, in pixels.
 *
 * Sized for the screens these photos land on: a 1080p projector or TV is 1920
 * across, and no output this app drives is larger. A modern phone camera hands
 * over something like 4000×3000 — six times the pixels, and around ten to twenty
 * times the bytes — every one of which is sent over the hall's Wi-Fi and then
 * thrown away by the browser as it scales the image down to fit.
 */
const val PHOTO_MAX_EDGE: Int = 1920

/**
 * Re-encodes [bytes] so its longest edge is at most [maxEdge], keeping the
 * aspect ratio. Returns the original bytes when the image is already small
 * enough, cannot be decoded, or the platform has no image pipeline.
 *
 * Returning the input unchanged rather than throwing is deliberate: a photo that
 * cannot be shrunk should still go on the screen. It is slower than it needs to
 * be, which is a far better failure than a blank slide mid-service.
 */
expect fun downscaleImage(bytes: ByteArray, maxEdge: Int = PHOTO_MAX_EDGE): ByteArray
