package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide holder for the envelope currently being projected.
 *
 * This exists because the two most important renderers live *outside* the
 * Compose tree of `App()`: Android's `Presentation` window and iOS's external
 * `UIWindowScene` are created by platform code, not by a Composable, so they
 * cannot be handed a ViewModel (and the project rule forbids passing one
 * anyway). They collect [current] instead.
 *
 * A [StateFlow] rather than a `SharedFlow` on purpose: a display that attaches
 * mid-service must immediately see the slide already on screen, which a
 * replay-less event stream could not give it.
 */
object SlideBus {
    private val _current = MutableStateFlow(SlideEnvelope.INITIAL)

    /** The envelope every attached renderer should currently be showing. */
    val current: StateFlow<SlideEnvelope> = _current.asStateFlow()

    /** Publishes [envelope], ignoring anything not newer than what is already shown. */
    fun publish(envelope: SlideEnvelope) {
        if (envelope.rev < _current.value.rev) return
        _current.value = envelope
    }

    /** Test hook — resets the singleton so tests don't leak state into each other. */
    fun resetForTest() {
        _current.value = SlideEnvelope.INITIAL
    }
}
