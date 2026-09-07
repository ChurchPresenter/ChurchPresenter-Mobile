package com.church.presenter.churchpresentermobile.ui.library

import com.church.presenter.churchpresentermobile.model.LibrarySyncState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * "Synced 3 days ago", worked out without waiting three days.
 *
 * The chip reads a stored blob and a wall clock, neither of which a UI test can
 * move, so the thresholds live here as plain values in and a bucket out. They
 * are the part that can be wrong: an off-by-one at the hour boundary reads as
 * "59 minutes ago" for an hour, and a clock that has gone backwards — a device
 * correcting its time, or a state copied off another phone — must not report a
 * negative age.
 */
class SyncAgeTest {

    private val minute = 60_000L
    private val hour = 3_600_000L
    private val day = 86_400_000L

    private fun syncedAt(epochMs: Long) = LibrarySyncState(lastSyncEpochMs = epochMs)

    private fun bucketAt(state: LibrarySyncState, nowMs: Long) = syncAgeFor(state, nowMs).bucket

    // ── Never ────────────────────────────────────────────────────────────

    @Test
    fun aPhoneThatHasNeverSyncedSaysSo() {
        assertEquals(SyncAge.Bucket.NEVER, bucketAt(LibrarySyncState.NEVER, day))
    }

    @Test
    fun aStateWithNoTimestampHasNeverSynced() {
        assertEquals(SyncAge.Bucket.NEVER, bucketAt(LibrarySyncState(songCount = 42), day))
    }

    @Test
    fun neverCarriesNoCount() {
        assertEquals(0L, syncAgeFor(LibrarySyncState.NEVER, day).count)
    }

    // ── Just now ─────────────────────────────────────────────────────────

    @Test
    fun aSyncThisSecondIsJustNow() {
        assertEquals(SyncAge.Bucket.JUST_NOW, bucketAt(syncedAt(day), day))
    }

    @Test
    fun aSyncFiftyNineSecondsAgoIsStillJustNow() {
        assertEquals(SyncAge.Bucket.JUST_NOW, bucketAt(syncedAt(day), day + 59_000L))
    }

    @Test
    fun justNowCarriesNoCount() {
        // "Synced 0 minutes ago" is worse than "just now".
        assertEquals(0L, syncAgeFor(syncedAt(day), day + 30_000L).count)
    }

    @Test
    fun aSyncExactlyAMinuteAgoIsMinutes() {
        assertEquals(SyncAge.Bucket.MINUTES, bucketAt(syncedAt(day), day + minute))
    }

    // ── Minutes ──────────────────────────────────────────────────────────

    @Test
    fun aSyncTenMinutesAgoCountsTenMinutes() {
        assertEquals(10L, syncAgeFor(syncedAt(day), day + 10 * minute).count)
    }

    @Test
    fun minutesRoundDownRatherThanUp() {
        // Ninety seconds is one minute ago, not two.
        assertEquals(1L, syncAgeFor(syncedAt(day), day + 90_000L).count)
    }

    @Test
    fun aSyncFiftyNineMinutesAgoIsStillMinutes() {
        assertEquals(SyncAge.Bucket.MINUTES, bucketAt(syncedAt(day), day + 59 * minute))
    }

    @Test
    fun aSyncFiftyNineMinutesAgoCountsFiftyNine() {
        assertEquals(59L, syncAgeFor(syncedAt(day), day + 59 * minute).count)
    }

    // ── Hours ────────────────────────────────────────────────────────────

    @Test
    fun aSyncExactlyAnHourAgoIsHours() {
        assertEquals(SyncAge.Bucket.HOURS, bucketAt(syncedAt(day), day + hour))
    }

    @Test
    fun aSyncAnHourAgoCountsOneHour() {
        assertEquals(1L, syncAgeFor(syncedAt(day), day + hour).count)
    }

    @Test
    fun anHourAndAHalfIsStillOneHour() {
        assertEquals(1L, syncAgeFor(syncedAt(day), day + hour + 30 * minute).count)
    }

    @Test
    fun aSyncFiveHoursAgoCountsFive() {
        assertEquals(5L, syncAgeFor(syncedAt(day), day + 5 * hour).count)
    }

    @Test
    fun aSyncTwentyThreeHoursAgoIsStillHours() {
        assertEquals(SyncAge.Bucket.HOURS, bucketAt(syncedAt(day), day + 23 * hour))
    }

    // ── Days ─────────────────────────────────────────────────────────────

    @Test
    fun aSyncExactlyADayAgoIsDays() {
        assertEquals(SyncAge.Bucket.DAYS, bucketAt(syncedAt(day), day + day))
    }

    @Test
    fun aSyncADayAgoCountsOneDay() {
        assertEquals(1L, syncAgeFor(syncedAt(day), day + day).count)
    }

    @Test
    fun aSyncLastSundayCountsSevenDays() {
        assertEquals(7L, syncAgeFor(syncedAt(day), day + 7 * day).count)
    }

    @Test
    fun aVeryOldSyncIsStillCountedInDays() {
        // Weeks and months would be a second vocabulary for no gain.
        assertEquals(SyncAge.Bucket.DAYS, bucketAt(syncedAt(day), day + 400 * day))
    }

    @Test
    fun aSyncADayAndAHalfAgoIsOneDay() {
        assertEquals(1L, syncAgeFor(syncedAt(day), day + day + 12 * hour).count)
    }

    // ── A clock that has gone backwards ──────────────────────────────────

    @Test
    fun aSyncRecordedInTheFutureReadsAsJustNow() {
        // A device correcting its time, or a state copied off another phone.
        assertEquals(SyncAge.Bucket.JUST_NOW, bucketAt(syncedAt(day + hour), day))
    }

    @Test
    fun aSyncRecordedInTheFutureCarriesNoNegativeCount() {
        assertEquals(0L, syncAgeFor(syncedAt(day + 400 * day), day).count)
    }

    // ── Reading the stored blob ──────────────────────────────────────────

    @Test
    fun aStoredStateIsReadBack() {
        val state = readSyncState("""{"lastSyncEpochMs":1700000000000,"songCount":42}""")

        assertEquals(1_700_000_000_000L, state.lastSyncEpochMs)
    }

    @Test
    fun aStoredStateKeepsItsCount() {
        val state = readSyncState("""{"lastSyncEpochMs":1700000000000,"songCount":42}""")

        assertEquals(42, state.songCount)
    }

    @Test
    fun aTruncatedBlobReadsAsNeverSynced() {
        // Killed mid-write. The tab has to open either way.
        assertEquals(LibrarySyncState.NEVER, readSyncState("{ truncated"))
    }

    @Test
    fun anEmptyBlobReadsAsNeverSynced() {
        assertEquals(LibrarySyncState.NEVER, readSyncState(""))
    }

    @Test
    fun aBlobOfSomethingElseEntirelyReadsAsNeverSynced() {
        assertEquals(LibrarySyncState.NEVER, readSyncState("""["not","a","state"]"""))
    }

    @Test
    fun aBlobFromALaterVersionKeepsWhatItRecognises() {
        // Unknown fields are ignored rather than making the whole thing unreadable.
        val state = readSyncState("""{"lastSyncEpochMs":1700000000000,"somethingNew":true}""")

        assertEquals(1_700_000_000_000L, state.lastSyncEpochMs)
    }

    @Test
    fun aPartialSyncIsRecordedAsPartial() {
        val state = readSyncState("""{"lastSyncEpochMs":1700000000000,"failedCount":3}""")

        assertEquals(true, state.wasPartial)
    }

    @Test
    fun aCleanSyncIsNotPartial() {
        val state = readSyncState("""{"lastSyncEpochMs":1700000000000,"failedCount":0}""")

        assertEquals(false, state.wasPartial)
    }

    @Test
    fun aReadStateFeedsTheBucketDirectly() {
        // The two halves are used together, and a state that reads back wrong
        // would report the wrong age with no other symptom.
        val state = readSyncState("""{"lastSyncEpochMs":${day}}""")

        assertEquals(SyncAge.Bucket.DAYS, bucketAt(state, day + 3 * day))
    }
}
