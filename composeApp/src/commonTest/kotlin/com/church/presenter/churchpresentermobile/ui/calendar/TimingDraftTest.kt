package com.church.presenter.churchpresentermobile.ui.calendar

import com.church.presenter.churchpresentermobile.model.RowEnd
import com.church.presenter.churchpresentermobile.model.RowTiming
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The timing panel's state: minutes relative to the service start while it is being edited, and
 * a wall-clock time once it is saved.
 *
 * The offset is what the operator reasons about ("five minutes before we start"), and the clock
 * time is what the desktop's automation fires on — so the two have to survive the round trip.
 */
class TimingDraftTest {

    private val serviceStart = "10:00"

    @Test
    fun `a cued row has no start time`() {
        val timing = TimingDraft().toTiming(serviceStart)

        assertEquals("", timing.startAt)
        assertFalse(timing.startsOnItsOwn())
    }

    @Test
    fun `an offset becomes a wall-clock time`() {
        val timing = TimingDraft(startOffsetMinutes = 15).toTiming(serviceStart)

        assertEquals("10:15", timing.startAt)
    }

    @Test
    fun `a negative offset starts before the service does`() {
        val timing = TimingDraft(startOffsetMinutes = -10).toTiming(serviceStart)

        assertEquals("09:50", timing.startAt, "the countdown before a 10:00 service")
    }

    @Test
    fun `an offset past the hour carries into the next one`() {
        assertEquals("11:30", TimingDraft(startOffsetMinutes = 90).toTiming(serviceStart).startAt)
    }

    @Test
    fun `a service with no readable start time cannot pin anything to the clock`() {
        val timing = TimingDraft(startOffsetMinutes = 15).toTiming("not a time")

        assertEquals("", timing.startAt, "better cued than pinned to a time nobody can read")
    }

    @Test
    fun `a pinned row does not also follow the previous one`() {
        // Both would be a contradiction: it either waits for the row before it or for the clock.
        val timing = TimingDraft(startOffsetMinutes = 5, followsPrevious = true).toTiming(serviceStart)

        assertEquals("10:05", timing.startAt)
        assertFalse(timing.followsPrevious)
    }

    @Test
    fun `an unpinned row may follow the previous one`() {
        val timing = TimingDraft(followsPrevious = true).toTiming(serviceStart)

        assertTrue(timing.followsPrevious)
        assertEquals("", timing.startAt)
    }

    @Test
    fun `repeats and what happens at the end are carried through`() {
        val timing = TimingDraft(repeats = 0, atEnd = RowEnd.NEXT).toTiming(serviceStart)

        assertEquals(0, timing.repeats, "0 is a loop")
        assertEquals(RowEnd.NEXT, timing.atEnd)
    }

    @Test
    fun `reading a saved row back gives the offset the operator typed`() {
        val saved = RowTiming(startAt = "10:20", repeats = 2, atEnd = RowEnd.BLANK)

        val draft = TimingDraft.of(saved, plannedSeconds = 240, serviceStart = serviceStart)

        assertEquals(20, draft.startOffsetMinutes)
        assertEquals(240, draft.runSeconds)
        assertEquals(2, draft.repeats)
        assertEquals(RowEnd.BLANK, draft.atEnd)
    }

    @Test
    fun `a row before the service start reads back as a negative offset`() {
        val draft = TimingDraft.of(RowTiming(startAt = "09:45"), null, serviceStart)

        assertEquals(-15, draft.startOffsetMinutes)
    }

    @Test
    fun `a cued row reads back with no offset at all`() {
        val draft = TimingDraft.of(RowTiming(followsPrevious = true), null, serviceStart)

        assertNull(draft.startOffsetMinutes)
        assertTrue(draft.followsPrevious)
    }

    @Test
    fun `a saved row survives the round trip`() {
        val saved = RowTiming(startAt = "10:35", repeats = 3, atEnd = RowEnd.NEXT)

        val again = TimingDraft.of(saved, 120, serviceStart).toTiming(serviceStart)

        assertEquals(saved, again)
    }

    @Test
    fun `an unreadable service start leaves the offset unknown rather than wrong`() {
        val draft = TimingDraft.of(RowTiming(startAt = "10:20"), null, serviceStart = "")

        assertNull(draft.startOffsetMinutes)
    }

    @Test
    fun `the item's own length is null, not zero`() {
        assertNull(TimingDraft().runSeconds)
        assertEquals(0, TimingDraft(runSeconds = 0).runSeconds, "0 is a length somebody typed")
    }
}
