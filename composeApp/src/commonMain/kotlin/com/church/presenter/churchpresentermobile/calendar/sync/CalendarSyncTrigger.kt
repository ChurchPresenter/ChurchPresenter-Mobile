package com.church.presenter.churchpresentermobile.calendar.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * "Something changed at the relay" — raised by the platform push handler when a silent
 * `calendar_changed` message arrives, collected by whoever holds a [CalendarSyncEngine].
 */
object CalendarSyncTrigger {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = _requests.asSharedFlow()

    /** The data-message type the relay sends. */
    const val MESSAGE_TYPE = "calendar_changed"

    fun requested() {
        _requests.tryEmit(Unit)
    }

    /** Whether a push's data payload is the relay's nudge. */
    fun matches(data: Map<String, String>): Boolean = data["type"] == MESSAGE_TYPE
}
