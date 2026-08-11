package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "SinkRegistry"

/**
 * Holds the [OutputSink]s the app knows about and fans envelopes out to all of
 * them.
 *
 * Registration is separate from attachment: a sink is registered when the
 * platform can support it at all, and attached when the user turns it on. That
 * keeps the outputs sheet able to show "External display — no screen connected"
 * rather than hiding the option entirely.
 *
 * Adding a new sink (Cast, a confidence monitor, a stream overlay) is one
 * [register] call — nothing else in the app changes.
 */
class SinkRegistry {
    private val sinks = mutableMapOf<String, OutputSink>()

    private val _statuses = MutableStateFlow<List<SinkStatus>>(emptyList())

    /** Current status of every registered sink, in registration order. */
    val statuses: StateFlow<List<SinkStatus>> = _statuses.asStateFlow()

    /** True while at least one sink is live. Drives the "casting" chip on the controller. */
    val hasAttachedSink: Boolean
        get() = _statuses.value.any { it.isAttached }

    /** Registers [sink], replacing any previous sink with the same [OutputSink.id]. */
    fun register(sink: OutputSink) {
        sinks[sink.id] = sink
        refreshStatuses()
        Logger.d(TAG, "register — id=${sink.id} total=${sinks.size}")
    }

    /** Removes [id] from the registry. The caller is responsible for detaching it first. */
    fun unregister(id: String) {
        sinks.remove(id)
        refreshStatuses()
    }

    /** Returns the registered sink with [id], or null. */
    fun sink(id: String): OutputSink? = sinks[id]

    /** All registered sinks, in registration order. */
    fun all(): List<OutputSink> = sinks.values.toList()

    /**
     * Pushes [envelope] to every registered sink.
     *
     * A throwing sink must not take the others down with it — a dead Chromecast
     * session should never stop the HDMI screen from updating — so each call is
     * guarded and failures are logged, not propagated.
     */
    fun broadcast(envelope: SlideEnvelope) {
        sinks.values.forEach { sink ->
            runCatching { sink.render(envelope) }
                .onFailure { Logger.e(TAG, "render failed for sink=${sink.id}: ${it.message}") }
        }
    }

    /**
     * Re-reads every sink's current status into [statuses].
     *
     * Called on registration and by the owner whenever a sink's state changes.
     * Sinks expose their own [OutputSink.status] flows for anyone who wants to
     * observe a single sink continuously; this aggregate is a snapshot for the
     * outputs list.
     */
    fun refreshStatuses() {
        _statuses.value = sinks.values.map { it.status.value }
    }
}
