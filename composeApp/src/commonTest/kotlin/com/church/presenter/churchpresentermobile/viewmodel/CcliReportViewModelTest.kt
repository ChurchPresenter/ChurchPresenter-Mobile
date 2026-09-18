package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.CSV_MIME_TYPE
import com.church.presenter.churchpresentermobile.library.PlayLogRepository
import com.church.presenter.churchpresentermobile.library.XLSX_MIME_TYPE
import com.church.presenter.churchpresentermobile.model.ReportFixtures
import com.church.presenter.churchpresentermobile.model.ReportFixtures.at
import com.church.presenter.churchpresentermobile.model.ReportFixtures.day
import com.church.presenter.churchpresentermobile.model.ReportPreset
import com.church.presenter.churchpresentermobile.model.ReportRange
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The report screen's state: ranges, filters, and what a play does to it. */
class CcliReportViewModelTest {

    /** "Today" for every test: Wednesday, March 25, 2026, mid-morning UTC. */
    private val today = at(day(2026, 3, 25))

    private class Fixture(now: Long, seeded: Boolean = true) {
        val storage = InMemoryFileStorage()
        var clock = now
        val repository = PlayLogRepository(storage) { clock }
        val viewModel: CcliReportViewModel

        init {
            if (seeded) {
                ReportFixtures.log.songs.forEach { clock = it.at; repository.recordSong(it.credit) }
                ReportFixtures.log.verses.forEach { clock = it.at; repository.recordVerse(it.credit) }
                clock = now
            }
            viewModel = CcliReportViewModel(repository, now = { clock }, zone = ReportFixtures.zone)
        }
    }

    // ── Ranges ───────────────────────────────────────────────────────────

    @Test
    fun `the report opens on the last three months`() = runVmTest {
        val f = Fixture(today)
        try {
            assertEquals(ReportPreset.LAST_3_MONTHS, f.viewModel.filters.value.preset)
            assertEquals(ReportRange(day(2025, 12, 25), day(2026, 3, 25)), f.viewModel.filters.value.range)
            assertEquals(3, f.viewModel.report.value.songs.size)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a preset moves the range and the rows follow`() = runVmTest {
        val f = Fixture(today)
        try {
            f.viewModel.setPreset(ReportPreset.ALL_TIME)
            advanceUntilIdle()

            assertEquals(day(2026, 1, 4), f.viewModel.filters.value.range.from)
            assertEquals(7, f.viewModel.report.value.songPlays)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a year is its whole calendar year`() = runVmTest {
        val f = Fixture(today)
        try {
            f.viewModel.setYear(2025)
            advanceUntilIdle()

            assertEquals(ReportPreset.YEAR, f.viewModel.filters.value.preset)
            assertEquals(ReportRange(day(2025, 1, 1), day(2025, 12, 31)), f.viewModel.filters.value.range)
            assertTrue(f.viewModel.report.value.songs.isEmpty(), "nothing was played in 2025")
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `editing a date by hand leaves the presets`() = runVmTest {
        val f = Fixture(today)
        try {
            f.viewModel.setFrom(day(2026, 2, 1))
            advanceUntilIdle()

            assertNull(f.viewModel.filters.value.preset)
            assertEquals(ReportRange(day(2026, 2, 1), day(2026, 3, 25)), f.viewModel.filters.value.range)
            assertEquals(
                listOf("Amazing Grace", "Blessed Be", "Come Thou Fount"),
                f.viewModel.report.value.songs.map { it.credit.title },
            )
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a start after the end drags the end with it, and vice versa`() = runVmTest {
        // The pickers can never turn the range inside out.
        val f = Fixture(today)
        try {
            f.viewModel.setFrom(day(2026, 6, 1))
            assertEquals(ReportRange(day(2026, 6, 1), day(2026, 6, 1)), f.viewModel.filters.value.range)

            f.viewModel.setTo(day(2026, 1, 1))
            assertEquals(ReportRange(day(2026, 1, 1), day(2026, 1, 1)), f.viewModel.filters.value.range)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Views and filters ────────────────────────────────────────────────

    @Test
    fun `the songbook filter narrows the songs and nothing else`() = runVmTest {
        val f = Fixture(today)
        try {
            f.viewModel.setPreset(ReportPreset.ALL_TIME)
            f.viewModel.setSongbook("Worship")
            advanceUntilIdle()

            val report = f.viewModel.report.value
            assertEquals(listOf("Blessed Be"), report.songs.map { it.credit.title })
            assertEquals(4, report.verses.size, "verses are not filtered by songbook")
            assertEquals(listOf("Hymnal", "Worship"), report.songbooks, "the menu still offers every book")
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the translation filter narrows the verses and the books`() = runVmTest {
        val f = Fixture(today)
        try {
            f.viewModel.setPreset(ReportPreset.ALL_TIME)
            f.viewModel.setBible("NIV")
            advanceUntilIdle()

            val report = f.viewModel.report.value
            assertEquals(listOf("Romans 8:28"), report.verses.map { it.credit.reference })
            assertEquals(listOf("Romans"), report.books.map { it.bookName })
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the open view is remembered`() = runVmTest {
        val f = Fixture(today)
        try {
            f.viewModel.setTab(ReportTab.ACTIVITY)
            advanceUntilIdle()

            assertEquals(ReportTab.ACTIVITY, f.viewModel.report.value.filters.tab)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the busiest period is the bucket with the most plays`() = runVmTest {
        val f = Fixture(today)
        try {
            f.viewModel.setPreset(ReportPreset.ALL_TIME)
            advanceUntilIdle()

            // Feb 22 had two verses; Jan 4 had two songs. Jan 4 – 10 is the first bucket and wins the tie.
            assertEquals(2, f.viewModel.report.value.busiest?.total)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Plays arriving and leaving ───────────────────────────────────────

    @Test
    fun `a play recorded while the report is open shows up in it`() = runVmTest {
        val f = Fixture(today)
        try {
            f.repository.recordSong(ReportFixtures.blessedBe)
            advanceUntilIdle()

            assertEquals(2, f.viewModel.report.value.songs.first { it.credit.title == "Blessed Be" }.count)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `an empty log is reported as such, not as an empty range`() = runVmTest {
        val f = Fixture(today, seeded = false)
        try {
            assertTrue(f.viewModel.report.value.isLogEmpty)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `clearing empties the repository`() = runVmTest {
        val f = Fixture(today)
        try {
            f.viewModel.clearStatistics()
            advanceUntilIdle()

            assertTrue(f.repository.log.value.isEmpty)
            assertTrue(f.viewModel.report.value.isLogEmpty)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Exports ──────────────────────────────────────────────────────────

    @Test
    fun `the CSV export is named for the range and honours the songbook filter`() = runVmTest {
        val f = Fixture(today)
        try {
            f.viewModel.setPreset(ReportPreset.ALL_TIME)
            f.viewModel.setSongbook("Hymnal")
            advanceUntilIdle()

            val file = f.viewModel.export(ReportExportFormat.CSV)

            assertEquals("CCLI-report-2026-01-04-to-2026-03-25.csv", file.fileName)
            assertEquals(CSV_MIME_TYPE, file.mimeType)
            val lines = file.bytes.decodeToString().trimEnd().lines()
            assertEquals(3, lines.size, "header plus the two Hymnal songs")
            assertTrue(lines.none { "Blessed Be" in it })
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the workbook export is an xlsx`() = runVmTest {
        val f = Fixture(today)
        try {
            val file = f.viewModel.export(ReportExportFormat.XLSX)

            assertEquals("CCLI-report-2025-12-25-to-2026-03-25.xlsx", file.fileName)
            assertEquals(XLSX_MIME_TYPE, file.mimeType)
            assertEquals("PK", file.bytes.copyOfRange(0, 2).decodeToString())
        } finally {
            tearDown(f.viewModel)
        }
    }
}
