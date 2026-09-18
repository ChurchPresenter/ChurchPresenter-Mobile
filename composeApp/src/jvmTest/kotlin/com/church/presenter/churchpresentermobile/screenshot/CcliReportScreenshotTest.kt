package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.library.PlayLogRepository
import com.church.presenter.churchpresentermobile.model.ReportFixtures
import com.church.presenter.churchpresentermobile.model.ReportPreset
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.standalone.CcliReportScreen
import com.church.presenter.churchpresentermobile.ui.standalone.ClearStatisticsDialog
import com.church.presenter.churchpresentermobile.viewmodel.CcliReportViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ReportTab
import kotlin.test.Test

/**
 * The CCLI report — standalone's record of what this device projected.
 *
 * Every populated state is drawn from [ReportFixtures.log] with "today" fixed
 * at March 25, 2026 and the zone fixed at UTC, so the dates in the rows and
 * the buckets in the chart are the same on every machine that records it.
 */
class CcliReportScreenshotTest {

    /** Wednesday, March 25, 2026, mid-morning UTC. */
    private val today = ReportFixtures.at(ReportFixtures.day(2026, 3, 25))

    /**
     * @param year Show this whole year instead of [preset] — the fixture has no plays
     *   outside 2026, so any other year is the "nothing in this range" state.
     */
    private fun report(
        tab: ReportTab = ReportTab.SONGS,
        seeded: Boolean = true,
        preset: ReportPreset = ReportPreset.ALL_TIME,
        year: Int? = null,
    ): @Composable () -> Unit {
        var clock = today
        val repository = PlayLogRepository(InMemoryFileStorage()) { clock }
        if (seeded) {
            ReportFixtures.log.songs.forEach { clock = it.at; repository.recordSong(it.credit) }
            ReportFixtures.log.verses.forEach { clock = it.at; repository.recordVerse(it.credit) }
            clock = today
        }
        val viewModel = CcliReportViewModel(repository, now = { clock }, zone = ReportFixtures.zone)
        viewModel.setPreset(preset)
        year?.let(viewModel::setYear)
        viewModel.setTab(tab)
        return { CcliReportScreen(repository = repository, providedViewModel = viewModel) }
    }

    // ── Phone ────────────────────────────────────────────────────────────

    @Test
    fun empty() = screenshot("ccli-report__empty", content = report(seeded = false))

    @Test
    fun songs() = screenshot("ccli-report__songs", content = report(ReportTab.SONGS))

    @Test
    fun bible() = screenshot("ccli-report__bible", content = report(ReportTab.BIBLE))

    @Test
    fun activity() = screenshot("ccli-report__activity", content = report(ReportTab.ACTIVITY))

    @Test
    fun emptyRange() = screenshot("ccli-report__empty-range", content = report(ReportTab.SONGS, year = 2025))

    @Test
    fun clearDialog() = screenshot("ccli-report__clear-dialog", dialog = true) {
        ClearStatisticsDialog(onConfirm = {}, onDismiss = {})
    }

    // ── Tablet ───────────────────────────────────────────────────────────

    @Test
    fun songsOnTablet() = screenshot(
        "ccli-report__songs-tablet",
        width = null,
        surface = Screenshots.TABLET_SURFACE,
        content = report(ReportTab.SONGS),
    )

    @Test
    fun bibleOnTablet() = screenshot(
        "ccli-report__bible-tablet",
        width = null,
        surface = Screenshots.TABLET_SURFACE,
        content = report(ReportTab.BIBLE),
    )

    @Test
    fun activityOnTablet() = screenshot(
        "ccli-report__activity-tablet",
        width = null,
        surface = Screenshots.TABLET_SURFACE,
        content = report(ReportTab.ACTIVITY),
    )
}
