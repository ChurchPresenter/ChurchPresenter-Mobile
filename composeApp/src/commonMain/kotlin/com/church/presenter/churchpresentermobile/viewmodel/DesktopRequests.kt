package com.church.presenter.churchpresentermobile.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The two things a content screen asks the desktop to do. */
enum class DesktopRequest {
    /** Go Live — or, while live, clear the display. */
    PROJECT,

    /** Add to schedule. */
    SCHEDULE,
}

/** How long the desktop may take before the operator is told it is waiting. */
internal const val SLOW_REQUEST_MS = 1_000L

/**
 * At most one request of each kind on its way to the desktop.
 *
 * A request can wait up to two minutes for an answer — the desktop may be
 * showing its operator an approval prompt — and for all of that time the button
 * used to look untouched. So people pressed it again: a second Go Live sent a
 * clear, a second Add queued the song twice. While a request is out, [pending]
 * names it, so its button can show it is busy, and another press of the same
 * button is ignored.
 *
 * Most answers come back at once, and a "waiting" message that flashes up just
 * before "Song is live" is noise; [launch]'s `onSlow` runs only when the answer
 * has not arrived within [slowAfterMs].
 */
class DesktopRequests(private val slowAfterMs: Long = SLOW_REQUEST_MS) {

    private val _pending = MutableStateFlow<Set<DesktopRequest>>(emptySet())

    /** The requests currently waiting on the desktop. */
    val pending: StateFlow<Set<DesktopRequest>> = _pending.asStateFlow()

    /** True while a request of [kind] is waiting on the desktop. */
    fun isPending(kind: DesktopRequest): Boolean = kind in _pending.value

    /**
     * Runs [request] in [scope] unless one of [kind] is already out, in which
     * case nothing happens and this returns false.
     *
     * @param onSlow Called once if [request] is still running after the slow
     *   threshold — the moment to tell the operator it was sent and is waiting.
     */
    fun launch(
        scope: CoroutineScope,
        kind: DesktopRequest,
        onSlow: () -> Unit = {},
        request: suspend () -> Unit,
    ): Boolean {
        // Checked and claimed in one step, so two presses in the same frame
        // cannot both get through.
        while (true) {
            val current = _pending.value
            if (kind in current) return false
            if (_pending.compareAndSet(current, current + kind)) break
        }

        scope.launch {
            val slow = launch {
                delay(slowAfterMs)
                onSlow()
            }
            try {
                request()
            } finally {
                slow.cancel()
                _pending.update { it - kind }
            }
        }
        return true
    }
}
