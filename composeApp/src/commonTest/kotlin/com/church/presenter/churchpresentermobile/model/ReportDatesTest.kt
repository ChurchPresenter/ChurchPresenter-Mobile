package com.church.presenter.churchpresentermobile.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

/** Days and labels. Fixed-offset zones only: the JS test runtime has no tz database. */
class ReportDatesTest {

    private val utc = TimeZone.UTC
    private val east = TimeZone.of("+12:00")
    private val jan4 = LocalDate(2026, 1, 4)

    @Test
    fun `a day starts at midnight and ends a millisecond before the next`() {
        val start = ReportDates.startOf(jan4, utc)

        assertEquals(start + 86_400_000L - 1, ReportDates.endOf(jan4, utc))
        assertEquals(jan4, ReportDates.dayOf(start, utc))
        assertEquals(jan4, ReportDates.dayOf(ReportDates.endOf(jan4, utc), utc))
        assertEquals(LocalDate(2026, 1, 5), ReportDates.dayOf(ReportDates.endOf(jan4, utc) + 1, utc))
    }

    @Test
    fun `the day depends on the zone`() {
        // 23:00 UTC on Jan 4 is already Jan 5 twelve hours east — and the report
        // must count the play on the day the congregation saw it.
        val lateOnJan4 = ReportDates.startOf(jan4, utc) + 23 * 3_600_000L

        assertEquals(jan4, ReportDates.dayOf(lateOnJan4, utc))
        assertEquals(LocalDate(2026, 1, 5), ReportDates.dayOf(lateOnJan4, east))
    }

    @Test
    fun `labels read the way the design shows them`() {
        assertEquals("Jan", ReportDates.monthLabel(jan4))
        assertEquals("Jan 4", ReportDates.dayLabel(jan4))
        assertEquals("Jan 4, 2026", ReportDates.dateLabel(jan4))
        assertEquals("Jan 26", ReportDates.monthYearLabel(jan4))
        assertEquals("2026-01-04", ReportDates.isoDate(jan4))
    }

    @Test
    fun `every month abbreviates to three capitalised letters`() {
        val labels = (1..12).map { ReportDates.monthLabel(LocalDate(2026, it, 1)) }

        assertEquals(
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
            labels,
        )
    }
}
