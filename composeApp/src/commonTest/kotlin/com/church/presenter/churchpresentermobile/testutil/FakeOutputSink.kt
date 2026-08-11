package com.church.presenter.churchpresentermobile.testutil

import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.present.OutputSink
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Records every envelope broadcast to it, so [com.church.presenter.churchpresentermobile.present.SinkRegistry]
 * fan-out and [com.church.presenter.churchpresentermobile.present.StandaloneEngine]
 * emission can be asserted without a real display or socket.
 *
 * Set [failOnRender] to verify that one broken sink does not stop the others.
 */
class FakeOutputSink(
    override val id: String = "fake",
    displayName: String = "Fake sink",
    var failOnRender: Boolean = false,
) : OutputSink {
    val rendered = mutableListOf<SlideEnvelope>()
    var attachCount = 0
        private set
    var detachCount = 0
        private set

    private val _status = MutableStateFlow(SinkStatus(id = id, displayName = displayName))
    override val status: StateFlow<SinkStatus> = _status.asStateFlow()

    val last: SlideEnvelope get() = rendered.last()

    override suspend fun attach() {
        attachCount++
        _status.value = _status.value.copy(state = SinkState.ATTACHED, clientCount = 1)
    }

    override suspend fun detach() {
        detachCount++
        _status.value = _status.value.copy(state = SinkState.DETACHED, clientCount = 0)
    }

    override fun render(envelope: SlideEnvelope) {
        if (failOnRender) throw IllegalStateException("sink $id is broken")
        rendered += envelope
    }
}
