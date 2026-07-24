package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for [ServerStatus.deriveWarnings] — the "Connected" vs "Limited functionality" logic. */
class ServerStatusTest {

    private fun status(
        bibles: List<String> = listOf("KJV"),
        songbooks: List<String> = listOf("Hymns"),
        permissions: DevicePermissions = DevicePermissions(),
        endpointAvailable: Boolean = true,
    ) = ServerStatus(
        bibles = bibles,
        songbooks = songbooks,
        permissions = permissions,
        endpointAvailable = endpointAvailable,
    )

    @Test
    fun fullyProvisionedServerHasNoWarnings() {
        assertTrue(status().deriveWarnings().isEmpty())
    }

    @Test
    fun endpointUnavailableSuppressesAllWarnings() {
        // A verified server whose /api/status endpoint is absent must not fabricate
        // content warnings from the empty fallback ServerStatus.
        val s = status(bibles = emptyList(), songbooks = emptyList(), endpointAvailable = false)
        assertTrue(s.deriveWarnings().isEmpty())
    }

    @Test
    fun noBiblesWarns() {
        val w = status(bibles = emptyList()).deriveWarnings()
        assertTrue(w.contains(StatusWarning.NoBibles))
        assertFalse(w.contains(StatusWarning.NoSongbooks))
    }

    @Test
    fun noSongbooksWarns() {
        val w = status(songbooks = emptyList()).deriveWarnings()
        assertTrue(w.contains(StatusWarning.NoSongbooks))
        assertFalse(w.contains(StatusWarning.NoBibles))
    }

    @Test
    fun blockedPermissionsWarn() {
        val s = status(
            permissions = DevicePermissions(
                canPresent = false,
                canAddToSchedule = false,
                canUploadFiles = false,
            ),
        )
        val w = s.deriveWarnings()
        assertTrue(w.contains(StatusWarning.PresentBlocked))
        assertTrue(w.contains(StatusWarning.ScheduleBlocked))
        assertTrue(w.contains(StatusWarning.UploadBlocked))
    }

    @Test
    fun grantedPermissionsDoNotWarn() {
        val w = status().deriveWarnings()
        assertFalse(w.contains(StatusWarning.PresentBlocked))
        assertFalse(w.contains(StatusWarning.ScheduleBlocked))
        assertFalse(w.contains(StatusWarning.UploadBlocked))
    }

    @Test
    fun apiKeyAbsenceIsNotAWarning() {
        // No API key configured is a preference, not a defect.
        assertFalse(status().deriveWarnings().contains(StatusWarning.NoApiKey))
    }
}
