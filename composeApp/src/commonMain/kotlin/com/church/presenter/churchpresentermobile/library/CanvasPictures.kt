package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.StoredPicture
import com.church.presenter.churchpresentermobile.present.downscaleImage
import com.church.presenter.churchpresentermobile.util.Logger
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64

private const val TAG = "CanvasPictures"

/** What a canvas picture's file name starts with, so the store can tell its own files from the rest. */
internal const val CANVAS_PICTURE_PREFIX = "canvas-picture-"

/**
 * The pictures placed on the canvas, kept on this device beside [CANVAS_FILE].
 *
 * The canvas file names a picture by key and this store holds its bytes: a photo
 * is far too large to rewrite into the canvas's JSON on every stroke. Each is
 * shrunk to [downscaleImage]'s size on the way in — the board and the screens it
 * goes to are no larger — and kept as Base64 text, which is what [FileStore] holds.
 *
 * @param newKey A fresh, unique key for each picture put in.
 * @param prepare Shrinks a picked picture before it is kept.
 * @param measure A picture's width and height in pixels, or null for bytes that are
 *   not a picture this device can draw.
 */
class CanvasPictures(
    private val storage: FileStore = createFileStore(),
    private val newKey: () -> String,
    private val prepare: (ByteArray) -> ByteArray = { downscaleImage(it) },
    private val measure: (ByteArray) -> Pair<Int, Int>? = ::decodedSize,
) {
    private val cache = HashMap<String, ByteArray>()

    /**
     * Keeps [bytes] and returns it as placed on the canvas, or null when it is not a
     * picture that can be drawn or could not be written.
     */
    fun put(bytes: ByteArray): StoredPicture? {
        val prepared = prepare(bytes)
        val (width, height) = measure(prepared) ?: return null
        val key = CANVAS_PICTURE_PREFIX + newKey()
        val written = runCatching { storage.write(key, Base64.encode(prepared)) }
            .onFailure { Logger.e(TAG, "could not keep a picture: ${it.message}") }
            .isSuccess
        if (!written) return null
        cache[key] = prepared
        return StoredPicture(key, width, height)
    }

    /** The picture kept as [key], or null when there is none. */
    fun get(key: String): ByteArray? = cache[key] ?: storage.read(key)
        ?.let { text -> runCatching { Base64.decode(text) }.getOrNull() }
        ?.also { cache[key] = it }

    /**
     * Deletes every picture not in [keep] — the ones whose elements were deleted in
     * an earlier session. Only at open: within a session, Undo can still bring one back.
     */
    fun prune(keep: Set<String>) {
        storage.list()
            .filter { it.startsWith(CANVAS_PICTURE_PREFIX) && it !in keep }
            .forEach { name ->
                runCatching { storage.delete(name) }
                cache.remove(name)
            }
    }
}

/** [bytes]' size as a decoded picture, or null when they will not decode. */
private fun decodedSize(bytes: ByteArray): Pair<Int, Int>? =
    runCatching { bytes.decodeToImageBitmap() }.getOrNull()?.let { it.width to it.height }
