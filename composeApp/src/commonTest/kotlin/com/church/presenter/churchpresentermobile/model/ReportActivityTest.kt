package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.model.ReportFixtures.day
import com.church.presenter.churchpresentermobile.model.ReportFixtures.firstQuarter
import com.church.presenter.churchpresentermobile.model.ReportFixtures.log
import com.church.presenter.churchpresentermobile.model.ReportFixtures.zone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The activity chart's buckets. */
class ReportActivityTest {

    @Test
    fun `a quarter charts by week, two years by month, longer by year`() {
        assertEquals(ActivityGranularity.WEEKLY, ReportActivity.granularityFor(1))
        assertEquals(ActivityGranularity.WEEKLY, ReportActivity.granularityFor(90))
        assertEquals(ActivityGranularity.MONTHLY, ReportActivity.granularityFor(91))
        assertEquals(ActivityGranularity.MONTHLY, ReportActivity.granularityFor(730))
        assertEquals(ActivityGranularity.YEARLY, ReportActivity.granularityFor(731))
    }

    @Test
    fun `weekly buckets start on the first day of the range and step seven days`() {
        val points = ReportActivity.points(log, firstQuarter, zone)

        assertEquals(13, points.size, "Jan 1 to Mar 31 is 90 days: twelve full weeks and a stub")
        assertEquals(listOf("Jan 1", "Jan 8", "Jan 15", "Jan 22"), points.take(4).map { it.label })
    }

    @Test
    fun `each play lands in the bucket its day falls in`() {
        val points = ReportActivity.points(log, firstQuarter, zone)
        val byLabel = points.associateBy { it.label }

        // Jan 4: Amazing Grace and Come Thou Fount. Jan 11: John 3:16.
        assertEquals(2, byLabel.getValue("Jan 1").songCount)
        assertEquals(0, byLabel.getValue("Jan 1").verseCount)
        assertEquals(1, byLabel.getValue("Jan 8").verseCount)
        assertEquals(log.songs.size, points.sumOf { it.songCount }, "every song play is in exactly one bucket")
        assertEquals(log.verses.size, points.sumOf { it.verseCount })
    }

    @Test
    fun `monthly buckets start on the first of the month`() {
        val range = ReportRange(day(2025, 11, 15), day(2026, 3, 31))

        val points = ReportActivity.points(log, range, zone)

        assertEquals(listOf("Nov 25", "Dec 25", "Jan 26", "Feb 26", "Mar 26"), points.map { it.label })
        assertEquals(3, points[2].songCount, "January: Jan 4 ×2 and Jan 18")
        assertEquals(2, points[3].verseCount, "February: two verses on Feb 22")
    }

    @Test
    fun `yearly buckets are labelled by year`() {
        val range = ReportRange(day(2024, 6, 1), day(2026, 12, 31))

        val points = ReportActivity.points(log, range, zone)

        assertEquals(listOf("2024", "2025", "2026"), points.map { it.label })
        assertEquals(log.songs.size, points.last().songCount)
    }

    @Test
    fun `a range that runs backwards charts nothing`() {
        assertTrue(ReportActivity.points(log, ReportRange(day(2026, 2, 1), day(2026, 1, 1)), zone).isEmpty())
    }

    @Test
    fun `a point's total is songs plus verses`() {
        assertEquals(5, ActivityPoint("x", songCount = 3, verseCount = 2).total)
    }
}
