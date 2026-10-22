package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "CanvasRepository"

/** The file the canvas is kept in, beside the library and the play log. */
internal const val CANVAS_FILE = "canvas.json"

/**
 * Everything about the canvas worth finding again after the app is closed: the
 * pages, the page that was open, and how each tool was set up.
 *
 * The undo history is not kept. It is a record of this session's edits, and
 * offering to undo yesterday's strokes after a restart would surprise more than
 * it would help.
 *
 * @property version Bumped only for a change an older file cannot be read through.
 *   Every field carries a default, so adding one never needs it.
 */
@Serializable
data class SavedCanvas(
    val version: Int = CANVAS_FILE_VERSION,
    val document: CanvasDocument = CanvasDocument(),
    val pageIndex: Int = 0,
    val options: CanvasToolOptions = CanvasToolOptions(),
)

/** The format [SavedCanvas] is written in. */
internal const val CANVAS_FILE_VERSION = 1

/**
 * The canvas on this device's disk.
 *
 * One JSON document, read when the canvas opens and rewritten after edits — the
 * same whole-document approach as the library ([LibraryRepository]), for the
 * same reasons. [FileStore] writes atomically, so a tablet that dies mid-save
 * keeps the last good canvas rather than half of a new one.
 */
class CanvasRepository(private val storage: FileStore = createFileStore()) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * The saved canvas, or null when there is none. A file that cannot be read is
     * treated as no canvas — and left on disk, untouched, rather than overwritten
     * by the next save, so nothing is lost while the cause is looked into.
     */
    fun load(): SavedCanvas? {
        val text = storage.read(CANVAS_FILE)
        if (text.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<SavedCanvas>(text) }
            .onFailure {
                Logger.e(TAG, "could not read the saved canvas: ${it.message}")
                unreadable = true
            }
            .getOrNull()
            ?.let(::sanitised)
    }

    /**
     * Writes [canvas]. Returns whether it was written; a failure is logged and
     * reported rather than thrown, so a full disk costs the save, not the service.
     */
    fun save(canvas: SavedCanvas): Boolean {
        if (unreadable) {
            // Keep the unreadable file aside instead of writing over it.
            runCatching { storage.read(CANVAS_FILE)?.let { storage.write(UNREADABLE_COPY, it) } }
            unreadable = false
        }
        return runCatching { storage.write(CANVAS_FILE, json.encodeToString(SavedCanvas.serializer(), canvas)) }
            .onFailure { Logger.e(TAG, "could not save the canvas: ${it.message}") }
            .isSuccess
    }

    private var unreadable = false

    /** [canvas] with anything that could not be shown put right: at least one page, and an open page that exists. */
    private fun sanitised(canvas: SavedCanvas): SavedCanvas {
        val document = canvas.document.takeIf { it.pages.isNotEmpty() }
            ?: canvas.document.copy(pages = CanvasDocument().pages)
        return canvas.copy(document = document, pageIndex = canvas.pageIndex.coerceIn(0, document.pages.lastIndex))
    }

    private companion object {
        const val UNREADABLE_COPY = "canvas.unreadable.json"
    }
}
