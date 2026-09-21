package com.church.presenter.churchpresentermobile.calendar

import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowTiming
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RunClockTest {

    private val service = PlannedService(
        id = "svc",
        date = "2026-09-27",
        name = "Sunday",
        startTime = "10:00",
        rows = listOf(
            PlanRow.Section("s1", "Worship"),
            PlanRow.Song("r1", "Opening"),
            PlanRow.Song("r2", "Second"),
            PlanRow.Ministry("r3", "Sermon"),
            PlanRow.Song("r4", "Closing"),
        ),
        plannedSeconds = mapOf("r1" to 270, "r2" to 90, "r3" to 1800, "r4" to 240),
        timing = mapOf("r3" to RowTiming(startAt = "10:30"), "r4" to RowTiming(repeats = 2)),
    )

    @Test
    fun durationsFormatAndParse() {
        assertEquals("4:30", formatDuration(270))
        assertEquals("0:05", formatDuration(5))
        assertEquals("1:02:00", formatDuration(3720))
        assertEquals("0:00", formatDuration(-4))
        assertEquals(270, parseDuration("4:30"))
        assertEquals(240, parseDuration(" 4 "))
        assertEquals(45, parseDuration("0:45"))
        assertEquals(3720, parseDuration("1:02:00"))
        assertNull(parseDuration(""))
        assertNull(parseDuration("4:"))
        assertNull(parseDuration("4m"))
        assertNull(parseDuration("1:2:3:4"))
        assertNull(parseDuration("1500"))
        assertEquals("69 min", minutesText(69 * 60 + 20))
        assertEquals("70 min", minutesText(69 * 60 + 40))
    }

    @Test
    fun theClockAdvancesByEachRowAndIsPinnedByARowThatStartsOnItsOwn() {
        val clocked = clockedRows(service)
        assertEquals(listOf(null, 600, 604, 630, 660), clocked.map { it.startMinutes })
        assertNull(clocked[0].seconds)
        assertEquals(270, clocked[1].seconds)
        assertEquals(RowTiming(startAt = "10:30"), clocked[3].timing)
    }

    @Test
    fun secondsCarryAcrossRowsRatherThanRoundingEachOne() {
        val short = service.copy(
            rows = listOf(PlanRow.Song("a", "A"), PlanRow.Song("b", "B"), PlanRow.Song("c", "C")),
            plannedSeconds = mapOf("a" to 45, "b" to 45, "c" to 45),
            timing = emptyMap(),
        )
        assertEquals(listOf(600, 600, 601), clockedRows(short).map { it.startMinutes })
    }

    @Test
    fun aServiceWithoutAValidStartHasNoClock() {
        val unclocked = service.copy(startTime = "soon", timing = emptyMap())
        assertEquals(listOf(null, null, null, null, null), clockedRows(unclocked).map { it.startMinutes })
        assertNull(endMinutes(unclocked))
    }

    @Test
    fun totalsCountRepeatsAndSkipSections() {
        assertEquals(270 + 90 + 1800 + 240 * 2, totalSeconds(service))
        assertEquals(600 + (270 + 90 + 1800 + 480) / 60, endMinutes(service))
        assertEquals(1, autoStartCount(service))
    }
}
