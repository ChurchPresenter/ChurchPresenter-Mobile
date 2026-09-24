package com.church.presenter.churchpresentermobile.calendar.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarSyncStateTest {

    private val token = "tokentokentokentoken_-"
    private val key = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8"
    private val qr =
        "churchpresenter://calendar-enroll?relay=https://sync.example.org&instance=inst-1&token=$token&key=$key"

    @Test
    fun theEnrollmentQrIsRead() {
        val state = CalendarSyncState.fromQr(qr)!!
        assertEquals("https://sync.example.org", state.relayUrl)
        assertEquals("inst-1", state.instanceId)
        assertEquals(token, state.deviceToken)
        assertEquals(key, state.instanceKey)
        assertEquals(0L, state.cursor)
        assertTrue(state.isEnrolled)
    }

    @Test
    fun aQrWithAnyPartWrongIsRefused() {
        assertNull(CalendarSyncState.fromQr("https://example.org/not-an-enrollment"))
        assertNull(CalendarSyncState.fromQr(qr.replace("https://sync.example.org", "http://sync.example.org")))
        assertNull(CalendarSyncState.fromQr(qr.replace("instance=inst-1", "instance=bad id")))
        assertNull(CalendarSyncState.fromQr(qr.replace("token=$token", "token=short")))
        assertNull(CalendarSyncState.fromQr(qr.replace("&key=$key", "")))
        assertNull(CalendarSyncState.fromQr("churchpresenter://calendar-enroll?"))
    }

    @Test
    fun anUnknownQueryKeyIsIgnored() {
        val state = CalendarSyncState.fromQr("$qr&evil=1&=orphan")
        assertEquals("inst-1", state!!.instanceId)
    }

    @Test
    fun stateRoundTripsThroughJson() {
        val state = CalendarSyncState.fromQr(qr)!!
            .copy(cursor = 42, lastSyncAt = "2026-09-20T10:00:00Z", registeredPushToken = "fcm")
        assertEquals(state, CalendarSyncState.fromJson(state.toJson()))
    }

    @Test
    fun unreadableStoredStateMeansNotEnrolled() {
        val state = CalendarSyncState.fromJson("{ nope")
        assertEquals(CalendarSyncState(), state)
        assertFalse(state.isEnrolled)
        assertFalse(CalendarSyncState.fromJson("").isEnrolled)
    }

    @Test
    fun enrolledNeedsEveryPart() {
        val full = CalendarSyncState.fromQr(qr)!!
        assertFalse(full.copy(relayUrl = "").isEnrolled)
        assertFalse(full.copy(instanceId = "").isEnrolled)
        assertFalse(full.copy(deviceToken = "").isEnrolled)
        assertFalse(full.copy(instanceKey = "").isEnrolled)
    }

    @Test
    fun theQrsDeviceIdIsKeptWhenItIsOneAndDroppedWhenItIsNot() {
        assertEquals("phone-1", CalendarSyncState.fromQr("$qr&device=phone-1")!!.deviceId)
        val odd = CalendarSyncState.fromQr("$qr&device=not an id")!!
        assertEquals("", odd.deviceId, "an odd device id is not a reason to refuse the whole enrollment")
        assertTrue(odd.isEnrolled)
    }
}
