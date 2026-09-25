package com.church.presenter.churchpresentermobile.ui.calendar

import com.church.presenter.churchpresentermobile.calendar.sync.SyncStatus
import com.church.presenter.churchpresentermobile.ui.library.SyncAge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * What the calendar headers' sync line says: how long ago the last round was, and how long until
 * the next — the rounding is where it goes wrong, so it is checked here rather than on screen.
 */
class SyncStatusLineTest {

    private val now = Instant.parse("2026-09-20T10:00:00Z")
    private fun synced(ago: Duration) = SyncStatus.Synced((now - ago).toString(), 0, 0)

    @Test
    fun aPhoneThatIsNotEnrolledSaysNothing() {
        assertNull(syncLineFor(SyncStatus.NotEnrolled, nextAt = null, now = now))
    }

    @Test
    fun beforeTheFirstRoundFinishesThereIsNothingToSay() {
        // The engine opens as Synced("") for an enrolled phone; that is not a time.
        assertNull(syncLineFor(SyncStatus.Synced("", 0, 0), nextAt = null, now = now))
    }

    @Test
    fun aRoundInProgressSaysSoWithoutACountdown() {
        assertEquals(SyncLine(SyncLine.Kind.SYNCING, null, null), syncLineFor(SyncStatus.Syncing, null, now))
    }

    @Test
    fun aFreshRoundReadsAsJustNowWithTheFullIntervalToGo() {
        val line = syncLineFor(synced(10.seconds), nextAt = now + 5.minutes, now = now)!!
        assertEquals(SyncLine.Kind.SYNCED, line.kind)
        assertEquals(SyncAge.Bucket.JUST_NOW, line.age?.bucket)
        assertEquals(5L, line.nextMinutes)
    }

    @Test
    fun theCountdownRoundsUpSoItNeverSaysZeroTooEarly() {
        val line = syncLineFor(synced(2.minutes), nextAt = now + 2.minutes + 30.seconds, now = now)!!
        assertEquals(SyncAge(SyncAge.Bucket.MINUTES, 2L), line.age)
        assertEquals(3L, line.nextMinutes)
    }

    @Test
    fun underAMinuteToGoReadsAsSoon() {
        assertEquals(0L, syncLineFor(synced(4.minutes), now + 20.seconds, now)!!.nextMinutes)
        // Overdue — the round is due but has not started yet.
        assertEquals(0L, syncLineFor(synced(6.minutes), now - 10.seconds, now)!!.nextMinutes)
    }

    @Test
    fun anOldRoundReadsInHours() {
        val line = syncLineFor(synced(3.hours), nextAt = null, now = now)!!
        assertEquals(SyncAge(SyncAge.Bucket.HOURS, 3L), line.age)
        assertNull(line.nextMinutes)
    }

    @Test
    fun aRoundStampedAheadOfThisClockReadsAsJustNow() {
        val line = syncLineFor(synced((-2).minutes), nextAt = null, now = now)!!
        assertEquals(SyncAge.Bucket.JUST_NOW, line.age?.bucket)
    }

    @Test
    fun aFailedRoundSaysWhenItTriesAgain() {
        val line = syncLineFor(SyncStatus.Failed("offline"), nextAt = now + 4.minutes, now = now)
        assertEquals(SyncLine(SyncLine.Kind.FAILED, null, 4L), line)
    }

    @Test
    fun aRevokedPhoneSaysSyncHasStopped() {
        assertEquals(SyncLine(SyncLine.Kind.STOPPED, null, null), syncLineFor(SyncStatus.Unauthorized, null, now))
    }
}
