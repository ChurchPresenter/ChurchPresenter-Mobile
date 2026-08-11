package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import kotlinx.coroutines.flow.StateFlow

/** Lifecycle state of an [OutputSink]. */
enum class SinkState {
    /** Not connected and not trying to be. */
    DETACHED,

    /** Attach is in flight — starting a server, waiting for a display, connecting a session. */
    ATTACHING,

    /** Live: [OutputSink.render] reaches a real screen. */
    ATTACHED,

    /** Attach failed or the connection dropped unrecoverably. See [SinkStatus.detail]. */
    ERROR,
}

/**
 * A snapshot of one sink, shaped for direct display on the outputs sheet.
 *
 * @property detail Human-readable extra: the URL for the web sink, the display
 *   name for an external screen, or the error text when [state] is [SinkState.ERROR].
 * @property clientCount How many screens are currently attached — the web sink
 *   can serve several browsers at once; others are 0 or 1.
 */
data class SinkStatus(
    val id: String,
    val displayName: String,
    val state: SinkState = SinkState.DETACHED,
    val detail: String? = null,
    val clientCount: Int = 0,
) {
    val isAttached: Boolean get() = state == SinkState.ATTACHED
}

/**
 * One place a slide can be shown: an external display, a phone-hosted web page,
 * a Cast receiver.
 *
 * Implementations must be safe to [attach] and [detach] repeatedly, and
 * [render] must never block — it is called from the UI's action path and may
 * fire many times a second while an operator scrubs through a deck.
 */
interface OutputSink {
    /** Stable identifier, unique within a [SinkRegistry]. */
    val id: String

    /** Live status for the outputs UI. */
    val status: StateFlow<SinkStatus>

    /** Brings the sink online. Idempotent — attaching an attached sink is a no-op. */
    suspend fun attach()

    /** Takes the sink offline and releases its resources. Idempotent. */
    suspend fun detach()

    /**
     * Pushes an envelope to the sink's screen(s). Must return immediately;
     * queue or drop internally rather than suspending the caller.
     * A detached sink silently ignores the call.
     */
    fun render(envelope: SlideEnvelope)
}
