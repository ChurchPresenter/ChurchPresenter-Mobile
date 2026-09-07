package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests what the Library screen reads to answer "is this current?", and what the
 * progress bar reads while a sync is running.
 */
class LibrarySyncStateTest {

    @Test
    fun `never synced`() {
        assertFalse(LibrarySyncState.NEVER.hasEverSynced)
        assertFalse(LibrarySyncState.NEVER.wasPartial)
        assertEquals(0, LibrarySyncState.NEVER.songCount)
        assertEquals("", LibrarySyncState.NEVER.sourceHost)
    }

    @Test
    fun `any recorded timestamp counts as having synced`() {
        assertTrue(LibrarySyncState(lastSyncEpochMs = 1L).hasEverSynced)
    }

    @Test
    fun `a sync that fetched everything is not partial`() {
        val state = LibrarySyncState(lastSyncEpochMs = 1_700_000_000_000L, songCount = 240)

        assertTrue(state.hasEverSynced)
        assertFalse(state.wasPartial)
    }

    @Test
    fun `a single failure makes the sync partial`() {
        assertTrue(LibrarySyncState(lastSyncEpochMs = 1L, songCount = 239, failedCount = 1).wasPartial)
    }

    @Test
    fun `sync state round-trips through JSON`() {
        val state = LibrarySyncState(
            lastSyncEpochMs = 1_700_000_000_000L,
            sourceHost = "office-mac",
            songCount = 240,
            failedCount = 2,
            keptLocalCount = 5,
        )

        assertEquals(state, Json.decodeFromString<LibrarySyncState>(Json.encodeToString(state)))
    }

    @Test
    fun `a state written before keptLocalCount existed still loads`() {
        val stored = """{"lastSyncEpochMs":1700000000000,"sourceHost":"office-mac","songCount":240}"""

        val state = Json.decodeFromString<LibrarySyncState>(stored)

        assertEquals(0, state.keptLocalCount)
        assertEquals(0, state.failedCount)
        assertTrue(state.hasEverSynced)
    }

    @Test
    fun `idle progress is not running and not preparing`() {
        assertFalse(SyncProgress.IDLE.isRunning)
        assertFalse(SyncProgress.IDLE.isPreparing)
        assertEquals(0f, SyncProgress.IDLE.fraction)
    }

    @Test
    fun `fraction reports how far along the sync is`() {
        assertEquals(0f, SyncProgress(done = 0, total = 240, isRunning = true).fraction)
        assertEquals(0.5f, SyncProgress(done = 120, total = 240, isRunning = true).fraction)
        assertEquals(1f, SyncProgress(done = 240, total = 240, isRunning = true).fraction)
    }

    @Test
    fun `fraction stays inside zero to one whatever the counts say`() {
        // A desktop that reports fewer items than it sends must not overflow the bar.
        assertEquals(1f, SyncProgress(done = 300, total = 240, isRunning = true).fraction)
        assertEquals(0f, SyncProgress(done = -5, total = 240, isRunning = true).fraction)
    }

    @Test
    fun `an unknown total reads as zero rather than dividing by it`() {
        assertEquals(0f, SyncProgress(done = 3, total = 0, isRunning = true).fraction)
        assertEquals(0f, SyncProgress(done = 3, total = -1, isRunning = true).fraction)
    }

    @Test
    fun `running without a total yet is preparing, so the UI shows an indeterminate bar`() {
        assertTrue(SyncProgress(isRunning = true, total = 0).isPreparing)
    }

    @Test
    fun `once the total is known it is no longer preparing`() {
        assertFalse(SyncProgress(isRunning = true, total = 240).isPreparing)
    }

    @Test
    fun `a stopped sync is never preparing`() {
        assertFalse(SyncProgress(isRunning = false, total = 0).isPreparing)
    }

    @Test
    fun `outcomes carry their counts`() {
        val success = SyncOutcome.Success(songCount = 240, failedCount = 2, keptLocal = 5)
        assertEquals(SyncOutcome.Success(240, 2, 5), success)
        assertEquals("offline", SyncOutcome.Failed("offline").message)
        // Cancelling keeps whatever had already been written.
        assertEquals(80, SyncOutcome.Cancelled(80).songCount)
    }
}
