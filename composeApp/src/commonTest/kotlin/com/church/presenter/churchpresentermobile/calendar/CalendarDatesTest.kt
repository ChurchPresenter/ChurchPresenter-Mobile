package com.church.presenter.churchpresentermobile.calendar

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarDatesTest {

    @Test
    fun theMonthGridIsSixWeeksStartingOnTheChosenDay() {
        val grid = monthGridDates(YearMonthRef(2026, 9))
        assertEquals(42, grid.size)
        assertEquals(LocalDate(2026, 8, 30), grid.first())
        assertEquals(DayOfWeek.SUNDAY, grid.first().dayOfWeek)
        assertTrue(grid.filterIndexed { index, _ -> index % 7 == 0 }.all { it.dayOfWeek == DayOfWeek.SUNDAY })
        assertEquals(LocalDate(2026, 10, 10), grid.last())
        val monday = monthGridDates(YearMonthRef(2026, 9), weekStart = DayOfWeek.MONDAY)
        assertEquals(LocalDate(2026, 8, 31), monday.first())
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), weekDays(DayOfWeek.MONDAY).take(2))
        assertEquals(DayOfWeek.SATURDAY, weekDays().last())
    }

    @Test
    fun monthsStepAndContain() {
        val september = YearMonthRef.of(LocalDate(2026, 9, 20))
        assertEquals(YearMonthRef(2026, 10), september.next())
        assertEquals(YearMonthRef(2026, 8), september.previous())
        assertEquals(YearMonthRef(2027, 1), YearMonthRef(2026, 12).next())
        assertTrue(september.contains(LocalDate(2026, 9, 1)))
        assertTrue(!september.contains(LocalDate(2025, 9, 1)))
        assertEquals("September 2026", monthTitle(september))
    }

    @Test
    fun dateTextsAreEnglishAndFixed() {
        val date = LocalDate(2026, 9, 20)
        assertEquals("Sunday, September 20, 2026", longDate(date))
        assertEquals("Sep 20, 2026", shortDate(date))
        assertEquals("Sunday, Sep 20", dayOverline(date))
        assertEquals("SUN", dayShortName(DayOfWeek.SUNDAY))
        assertEquals("March", monthName(Month.MARCH))
        assertEquals("2026-09-20", storedDate(date))
        assertEquals(date, parseStoredDate("2026-09-20"))
        assertNull(parseStoredDate("20/09/2026"))
    }

    @Test
    fun theClockIsShownInTheChosenFormat() {
        assertEquals("10:00 AM", clockText("10:00"))
        assertEquals("12:05 AM", clockText("00:05"))
        assertEquals("12:00 PM", clockText("12:00"))
        assertEquals("6:30 PM", clockText("18:30"))
        assertEquals("18:30", clockText("18:30", use24Hour = true))
        assertEquals("junk", clockText("junk"))
    }

    @Test
    fun aTimeFieldAcceptsTheFormsPeopleType() {
        assertEquals(LocalTime(18, 30), parseClockText("18:30"))
        assertEquals(LocalTime(18, 30), parseClockText("6:30 PM"))
        assertEquals(LocalTime(18, 30), parseClockText("6.30pm"))
        assertEquals(LocalTime(18, 0), parseClockText("6 pm"))
        assertEquals(LocalTime(18, 30), parseClockText("1830"))
        assertEquals(LocalTime(0, 15), parseClockText("12:15 am"))
        assertEquals(LocalTime(12, 15), parseClockText("12:15 pm"))
        assertEquals(LocalTime(9, 0), parseClockText("9"))
        assertNull(parseClockText(""))
        assertNull(parseClockText("25:00"))
        assertNull(parseClockText("9:75"))
        assertNull(parseClockText("noon"))
    }

    @Test
    fun minutesRoundTripAndWrapAtMidnight() {
        assertEquals(630, minutesOfDay("10:30"))
        assertNull(minutesOfDay("10:3"))
        assertEquals("10:30", timeFromMinutes(630))
        assertEquals("00:10", timeFromMinutes(24 * 60 + 10))
        assertEquals("23:50", timeFromMinutes(-10))
        assertEquals("09:05", storedTime(LocalTime(9, 5)))
    }
}
