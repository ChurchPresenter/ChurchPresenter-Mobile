package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.concurrent.Volatile

private const val TAG = "CanvasOutput"

/** What a canvas frame's id starts with, so it can never be mistaken for a picked photo's. */
const val CANVAS_FRAME_PREFIX: String = "canvas-"

/**
 * The canvas page as the outputs see it: the latest picture of it, served by the
 * phone's presentation server beside the operator's photos.
 *
 * Each frame gets a new address. An output fetches by URL, and one that had
 * cached the previous picture under the same address would go on showing it.
 * The frame before the latest is kept too: a screen may still be fetching it
 * when the next one arrives, and an answer of "not found" there is a blank
 * screen for a moment.
 *
 * @param baseUrl Where the server answers — the same address photos are served
 *   from, null while it is not running.
 */
class CanvasOutput(private val baseUrl: StateFlow<String?>) {

    @Volatile
    private var frames: List<Pair<String, ByteArray>> = emptyList()
    private var revision = 0

    /** Whether a frame put up now could be reached by the outputs. */
    val canServe: Boolean get() = baseUrl.value != null

    /**
     * Keeps [png] as the latest frame and returns the address it is served at,
     * or null while the server is down — a slide pointing nowhere is a black screen.
     */
    fun publish(png: ByteArray): String? {
        val base = baseUrl.value ?: return null
        val id = "$CANVAS_FRAME_PREFIX${++revision}"
        frames = (frames.takeLast(1) + (id to png))
        return "$base/$PHOTO_ROUTE/$id"
    }

    /** Forgets every frame — the canvas has come off the screen. */
    fun clear() {
        frames = emptyList()
    }

    /** Whether [url] is one of this output's frames. */
    fun owns(url: String?): Boolean =
        url != null && frames.any { (id, _) -> url.endsWith("/$PHOTO_ROUTE/$id") }

    /** The frames as something the server can serve. */
    val source: PhotoSource = PhotoSource { id ->
        frames.firstOrNull { it.first == id }?.let { ServedPhoto(it.second, PNG) }
    }

    private companion object {
        const val PNG = "image/png"
    }
}

/**
 * Puts a canvas page on this device's outputs, through the same [StandaloneEngine]
 * every other standalone screen projects with.
 *
 * The canvas follows its own edits while live: each new frame replaces the slide.
 * But only while the canvas is still what is on screen — once the operator has
 * put a song up from the Present tab, a stroke on the canvas must not take the
 * screen back. [show] with `takeOver` is the one exception: that is Go Live.
 */
class CanvasProjector(
    private val output: CanvasOutput,
    private val engine: StandaloneEngine,
) {
    /** Whether the canvas's last frame is what the outputs are showing. */
    val isShowing: Boolean
        get() = shows(engine.currentSlide.value)

    /** [isShowing] as it changes — false the moment something else takes the screen. */
    val showing: Flow<Boolean> = engine.currentSlide.map(::shows).distinctUntilChanged()

    private fun shows(slide: Slide): Boolean = slide.kind == SlideKind.CANVAS && output.owns(slide.mediaUrl)

    /**
     * Shows [png] as the canvas page. With [takeOver] it replaces whatever is on
     * screen and lifts blank and off-air; without, it only refreshes a canvas that
     * is already showing.
     *
     * @return Whether the frame went up. False when the server is down, or when
     *   something else has the screen and this was not a take-over.
     */
    fun show(png: ByteArray, paper: String, title: String, takeOver: Boolean): Boolean {
        if (!takeOver && !isShowing) return false
        val url = output.publish(png) ?: run {
            Logger.e(TAG, "show — the presentation server is not running")
            return false
        }
        engine.setDeck(SlideDeckBuilder.fromCanvas(url, paper, title))
        if (takeOver) engine.goLive()
        return true
    }

    /** Takes the canvas off the screen, if it is still what is showing. */
    fun stop() {
        if (isShowing) engine.clear()
        output.clear()
    }
}
