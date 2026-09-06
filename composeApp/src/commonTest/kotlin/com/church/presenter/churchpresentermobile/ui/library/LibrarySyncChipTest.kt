package com.church.presenter.churchpresentermobile.ui.library

import com.church.presenter.churchpresentermobile.model.LibrarySyncState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the Library tab's sync chip says about the last copy from a desktop.
 *
 * It is the only place the operator is told whether the songs on the phone are
 * current, and it is read at a glance before a service. The thresholds are the
 * part that can be wrong — and a composable that reads the wall clock cannot be
 * checked without waiting out an hour, which is why the decision lives outside
 * it.
 */
class LibrarySyncChipTest {

    private fun syncedAt(epochMs: Long) = LibrarySyncState(lastSyncEpochMs = epochMs)

    private fun ageAfter(elapsedMs: Long) = syncAgeFor(syncedAt(SYNCED_AT), SYNCED_AT + elapsedMs)

    // ── Reading the stored state ─────────────────────────────────────────

    @Test
    fun `a stored sync is read back`() {
        val state = readSyncState("""{"lastSyncEpochMs":1700000000000,"songCount":42}""")

        assertEquals(1_700_000_000_000L, state.lastSyncEpochMs)
        assertEquals(42, state.songCount)
    }

    @Test
    fun `a phone that has never synced reads as never`() {
        assertEquals(LibrarySyncState.NEVER, readSyncState("{}"))
    }

    @Test
    fun `a blob this build cannot read does not stop the tab opening`() {
        // Written by a previous version, or truncated by a kill mid-write. The
        // chip being wrong is not worth a crash on the Library tab.
        for (stored in listOf("", "not json", """{"lastSyncEpochMs":"yesterday"}""", "[]")) {
            assertEquals(LibrarySyncState.NEVER, readSyncState(stored), stored)
        }
    }

    @Test
    fun `a field this build does not know about is ignored`() {
        // A newer app writes the blob; an older one still has to read the parts
        // it understands.
        val state = readSyncState("""{"lastSyncEpochMs":1700000000000,"somethingNew":true}""")

        assertEquals(1_700_000_000_000L, state.lastSyncEpochMs)
    }

    // ── Which bucket the age falls into ──────────────────────────────────

    @Test
    fun `a library that has never been synced says so`() {
        assertEquals(SyncAge.Bucket.NEVER, syncAgeFor(LibrarySyncState.NEVER, SYNCED_AT).bucket)
    }

    @Test
    fun `a sync moments ago reads as just now`() {
        assertEquals(SyncAge.Bucket.JUST_NOW, ageAfter(0L).bucket)
        assertEquals(SyncAge.Bucket.JUST_NOW, ageAfter(59_999L).bucket)
    }

    @Test
    fun `a minute old sync starts counting minutes`() {
        // The boundary: at exactly a minute it stops saying "just now".
        val age = ageAfter(60_000L)

        assertEquals(SyncAge.Bucket.MINUTES, age.bucket)
        assertEquals(1L, age.count)
    }

    @Test
    fun `minutes are counted down, not rounded up`() {
        // "5 minutes ago" for something 5m59s old is right; "6" would be ahead
        // of the clock.
        assertEquals(5L, ageAfter(5 * 60_000L + 59_000L).count)
    }

    @Test
    fun `an hour old sync starts counting hours`() {
        val age = ageAfter(3_600_000L)

        assertEquals(SyncAge.Bucket.HOURS, age.bucket)
        assertEquals(1L, age.count)
    }

    @Test
    fun `fifty-nine minutes is still minutes`() {
        assertEquals(SyncAge.Bucket.MINUTES, ageAfter(3_599_999L).bucket)
        assertEquals(59L, ageAfter(3_599_999L).count)
    }

    @Test
    fun `a day old sync starts counting days`() {
        val age = ageAfter(86_400_000L)

        assertEquals(SyncAge.Bucket.DAYS, age.bucket)
        assertEquals(1L, age.count)
    }

    @Test
    fun `twenty-three hours is still hours`() {
        assertEquals(SyncAge.Bucket.HOURS, ageAfter(86_399_999L).bucket)
        assertEquals(23L, ageAfter(86_399_999L).count)
    }

    @Test
    fun `a sync from last month is counted in days`() {
        assertEquals(30L, ageAfter(30 * 86_400_000L).count)
    }

    @Test
    fun `a sync in the future reads as just now rather than as a negative age`() {
        // Reachable when the device corrects its clock, or when the blob was
        // written on another phone whose clock ran ahead. "-3 hours ago" is the
        // one answer that would look broken.
        val age = syncAgeFor(syncedAt(SYNCED_AT + 3_600_000L), SYNCED_AT)

        assertEquals(SyncAge.Bucket.JUST_NOW, age.bucket)
    }

    @Test
    fun `the count is never negative`() {
        assertEquals(0L, syncAgeFor(syncedAt(SYNCED_AT + 86_400_000L), SYNCED_AT).count)
    }

    private companion object {
        /** An arbitrary fixed instant; only the difference matters. */
        const val SYNCED_AT = 1_700_000_000_000L
    }
}
