package com.church.presenter.churchpresentermobile.calendar.sync

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarSyncTriggerTest {

    @Test
    fun onlyTheRelaysOwnMessageTypeCounts() {
        assertTrue(CalendarSyncTrigger.matches(mapOf("type" to "calendar_changed")))
        assertFalse(CalendarSyncTrigger.matches(mapOf("type" to "song_changed")))
        assertFalse(CalendarSyncTrigger.matches(emptyMap()))
    }

    @Test
    fun aRequestReachesWhoeverIsListening() = runTest {
        val listener = async { CalendarSyncTrigger.requests.first() }
        yield()
        CalendarSyncTrigger.requested()
        assertEquals(Unit, listener.await())
    }
}
