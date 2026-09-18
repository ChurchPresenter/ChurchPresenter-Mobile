package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.model.ReportFixtures.amazingGrace
import com.church.presenter.churchpresentermobile.model.ReportFixtures.at
import com.church.presenter.churchpresentermobile.model.ReportFixtures.day
import com.church.presenter.churchpresentermobile.model.ReportFixtures.firstQuarter
import com.church.presenter.churchpresentermobile.model.ReportFixtures.john316
import com.church.presenter.churchpresentermobile.model.ReportFixtures.log
import com.church.presenter.churchpresentermobile.model.ReportFixtures.zone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The report's rows: grouped, counted, sorted and filtered the way the desktop's are. */
class CcliReportTest {

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun `songs are one row each, most played first`() {
        val rows = CcliReport.songs(log, firstQuarter, zone)

        assertEquals(listOf("Amazing Grace", "Come Thou Fount", "Blessed Be"), rows.map { it.credit.title })
        assertEquals(listOf(4, 2, 1), rows.map { it.count })
    }

    @Test
    fun `a row knows when the song was first and last used`() {
        val grace = CcliReport.songs(log, firstQuarter, zone).first()

        assertEquals(at(day(2026, 1, 4)), grace.firstUsed)
        assertEquals(at(day(2026, 3, 22)), grace.lastUsed)
    }

    @Test
    fun `equal counts sort by title`() {
        val range = ReportRange(day(2026, 1, 4), day(2026, 1, 4))

        val rows = CcliReport.songs(log, range, zone)

        assertEquals(listOf("Amazing Grace", "Come Thou Fount"), rows.map { it.credit.title })
    }

    @Test
    fun `the range is inclusive of both days, whatever the time of day`() {
        // A play at 10:30 on the last day of the range is in the range.
        val lastDayOnly = ReportRange(day(2026, 3, 22), day(2026, 3, 22))
        val dayBefore = ReportRange(day(2026, 3, 21), day(2026, 3, 21))

        assertEquals(1, CcliReport.songs(log, lastDayOnly, zone).size)
        assertTrue(CcliReport.songs(log, dayBefore, zone).isEmpty())
    }

    @Test
    fun `a songbook filter keeps only that book`() {
        val rows = CcliReport.songs(log, firstQuarter, zone, songbook = "Worship")

        assertEquals(listOf("Blessed Be"), rows.map { it.credit.title })
    }

    @Test
    fun `a retitled song is still one row, under its newest title`() {
        val renamed = PlayLog(
            songs = listOf(
                SongPlay(amazingGrace, at(day(2026, 1, 4))),
                SongPlay(amazingGrace.copy(title = "Amazing Grace (My Chains Are Gone)"), at(day(2026, 2, 1))),
            ),
        )

        val rows = CcliReport.songs(renamed, firstQuarter, zone)

        assertEquals(1, rows.size)
        assertEquals("Amazing Grace (My Chains Are Gone)", rows.single().credit.title)
        assertEquals(2, rows.single().count)
    }

    // ── Verses ───────────────────────────────────────────────────────────

    @Test
    fun `verses are one row each, most shown first, then in Bible order`() {
        val rows = CcliReport.verses(log, firstQuarter, zone)

        assertEquals(listOf("John 3:16", "John 3:17", "Psalm 23:1", "Romans 8:28"), rows.map { it.credit.reference })
        assertEquals(listOf(2, 1, 1, 1), rows.map { it.count })
    }

    @Test
    fun `a translation filter keeps only that translation`() {
        val rows = CcliReport.verses(log, firstQuarter, zone, bible = "NIV")

        assertEquals(listOf("Romans 8:28"), rows.map { it.credit.reference })
    }

    @Test
    fun `books are ranked by how many verse plays they had`() {
        val books = CcliReport.topBooks(CcliReport.verses(log, firstQuarter, zone))

        assertEquals(listOf("John" to 3, "Psalm" to 1, "Romans" to 1), books.map { it.bookName to it.count })
    }

    // ── Filters' choices ─────────────────────────────────────────────────

    @Test
    fun `the songbooks and translations on offer are every one ever played, sorted`() {
        assertEquals(listOf("Hymnal", "Worship"), CcliReport.songbooks(log))
        assertEquals(listOf("KJV", "NIV"), CcliReport.bibles(log))
    }

    @Test
    fun `the years on offer are those with a play in them, newest first`() {
        val twoYears = log.copy(verses = log.verses + VersePlay(john316, at(day(2024, 12, 25))))

        assertEquals(listOf(2026, 2024), CcliReport.years(twoYears, zone))
    }

    // ── Ranges ───────────────────────────────────────────────────────────

    private val today = at(day(2026, 3, 25))

    @Test
    fun `the quick ranges end today and start whole months back`() {
        assertEquals(
            ReportRange(day(2025, 12, 25), day(2026, 3, 25)),
            CcliReport.rangeFor(ReportPreset.LAST_3_MONTHS, log, today, zone),
        )
        assertEquals(day(2025, 9, 25), CcliReport.rangeFor(ReportPreset.LAST_6_MONTHS, log, today, zone).from)
        assertEquals(day(2025, 3, 25), CcliReport.rangeFor(ReportPreset.LAST_12_MONTHS, log, today, zone).from)
    }

    @Test
    fun `all time starts at the first play on record`() {
        assertEquals(
            ReportRange(day(2026, 1, 4), day(2026, 3, 25)),
            CcliReport.rangeFor(ReportPreset.ALL_TIME, log, today, zone),
        )
    }

    @Test
    fun `all time on an empty log is just today`() {
        // So the pickers never open on a date nothing was ever recorded before.
        assertEquals(
            ReportRange(day(2026, 3, 25), day(2026, 3, 25)),
            CcliReport.rangeFor(ReportPreset.ALL_TIME, PlayLog.EMPTY, today, zone),
        )
    }

    @Test
    fun `year is the whole calendar year`() {
        assertEquals(ReportRange(day(2026, 1, 1), day(2026, 12, 31)), CcliReport.yearRange(2026))
        assertEquals(CcliReport.yearRange(2026), CcliReport.rangeFor(ReportPreset.YEAR, log, today, zone))
    }

    @Test
    fun `a range is valid only when it runs forwards`() {
        assertTrue(ReportRange(day(2026, 1, 1), day(2026, 1, 1)).isValid)
        assertTrue(!ReportRange(day(2026, 1, 2), day(2026, 1, 1)).isValid)
    }
}
