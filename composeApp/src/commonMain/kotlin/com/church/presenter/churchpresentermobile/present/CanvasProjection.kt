package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** What became of a frame sent to a [CanvasProjection]. */
enum class CanvasShowResult {
    /** On screen. */
    SHOWN,

    /** Not shown: something else has the screen, and this frame was a refresh, not a take-over. */
    NOT_SHOWING,

    /** Not shown: the operator at the desktop said no. */
    DECLINED,

    /** Not shown: nowhere to show it — the server is down or unreachable. */
    FAILED,

    /** Not shown: the desktop is too old to know drawings, and answered 404. */
    UNSUPPORTED,
}

/**
 * Somewhere a canvas page can be put on screen: this device's own outputs in
 * standalone ([CanvasProjector]), or the desktop's in remote ([RemoteCanvasProjector]).
 *
 * The canvas speaks to this one shape and does not know which it has. The rules
 * are the same for both: a take-over puts the page up in place of whatever is
 * showing; a refresh only replaces a page that is still up, so a stroke cannot take
 * the screen back once the operator has moved on.
 */
interface CanvasProjection {
    /**
     * Whether a take-over waits for someone to allow it — the desktop operator
     * approves a remote device going live. The canvas says it is waiting meanwhile.
     */
    val asksFirst: Boolean

    /** Whether the canvas is what the screen is showing — false the moment anything else takes it. */
    val showing: Flow<Boolean>

    /**
     * Shows [png], a picture of the page, framed by [paper] (`#RRGGBB`).
     *
     * @param takeOver Go Live: replace whatever is on screen (after approval, where
     *   [asksFirst]). Otherwise refresh the page only if it is still showing.
     */
    suspend fun show(png: ByteArray, paper: String, title: String, takeOver: Boolean): CanvasShowResult

    /** Takes the canvas off the screen if it is still what is showing, and withdraws a pending take-over. */
    suspend fun stop()
}

/**
 * Hands the canvas to whichever [CanvasProjection] the app's mode calls for, read
 * per call — the same way [ProjectionRouter] does for every other screen — so a
 * canvas opened in one mode follows the app into the other.
 */
class CanvasProjectionRouter(
    private val mode: StateFlow<AppMode>,
    private val standalone: CanvasProjection,
    private val remote: CanvasProjection,
) : CanvasProjection {

    private val current: CanvasProjection?
        get() = when (mode.value) {
            AppMode.STANDALONE -> standalone
            AppMode.REMOTE -> remote
            AppMode.CALENDAR -> null
        }

    override val asksFirst: Boolean get() = current?.asksFirst ?: false

    @OptIn(ExperimentalCoroutinesApi::class)
    override val showing: Flow<Boolean> = mode.flatMapLatest { m ->
        when (m) {
            AppMode.STANDALONE -> standalone.showing
            AppMode.REMOTE -> remote.showing
            AppMode.CALENDAR -> flowOf(false)
        }
    }

    override suspend fun show(png: ByteArray, paper: String, title: String, takeOver: Boolean): CanvasShowResult =
        current?.show(png, paper, title, takeOver) ?: CanvasShowResult.FAILED

    override suspend fun stop() {
        current?.stop()
    }
}
