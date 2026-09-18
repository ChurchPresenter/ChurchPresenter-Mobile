package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.library.PlayLogRepository
import com.church.presenter.churchpresentermobile.model.ReportFixtures
import com.church.presenter.churchpresentermobile.model.ReportFixtures.at
import com.church.presenter.churchpresentermobile.model.ReportFixtures.day
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.CcliReportViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The CCLI report screen: what it shows for a log, and what its buttons do to
 * the log. The clear flow is the one that matters — it deletes history, so the
 * thing to assert is the repository afterwards, not that a dialog closed.
 */
@OptIn(ExperimentalTestApi::class)
class CcliReportScreenTest {

    private val today = at(day(2026, 3, 25))

    private class Fixture(now: Long, seeded: Boolean) {
        var clock = now
        val repository = PlayLogRepository(InMemoryFileStorage()) { clock }
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

    private fun fixture(seeded: Boolean = true) = Fixture(today, seeded)

    /**
     * The test window is tablet-sized, so a bare screen lays out as two panes.
     * Phone is the layout most of these are about, so it is the default.
     */
    private fun ComposeUiTest.showReport(f: Fixture, wide: Boolean = false) = showScreen {
        Box(modifier = if (wide) Modifier else Modifier.width(360.dp)) {
            CcliReportScreen(f.repository, providedViewModel = f.viewModel)
        }
    }

    // ── Empty ────────────────────────────────────────────────────────────

    @Test
    fun nothingRecordedShowsTheEmptyStateAndNoActions() = runComposeUiTest {
        val f = fixture(seeded = false)

        showReport(f)

        assertTrue(exists(ReportTags.EMPTY))
        assertFalse(exists(ReportTags.CLEAR), "there is nothing to clear")
        assertFalse(exists(ReportTags.EXPORT_CSV))
    }

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun theSongsViewShowsTheTotalsAndTheRankedSongs() = runComposeUiTest {
        val f = fixture()

        showReport(f)

        // Last three months from Mar 25: everything but nothing before Dec 25, so all seven plays.
        onNodeWithTag(ReportTags.SONG_PLAYS).assertTextContains("7")
        onNodeWithTag(ReportTags.UNIQUE_SONGS).assertTextContains("3")
        assertTrue(exists(ReportTags.song(1)))
        assertTrue(isShowing("Amazing Grace"))
        assertTrue(isShowing("John Newton"))
        assertFalse(exists(ReportTags.EMPTY_RANGE))
    }

    @Test
    fun aRangeWithNothingInItSaysSo() = runComposeUiTest {
        val f = fixture()

        showReport(f)
        f.viewModel.setYear(2025)

        awaitThat { exists(ReportTags.EMPTY_RANGE) }
        assertFalse(exists(ReportTags.song(1)))
    }

    @Test
    fun theQuickRangesAreOfferedAndPressable() = runComposeUiTest {
        val f = fixture()

        showReport(f)
        click(ReportTags.preset(3))

        awaitThat { f.viewModel.filters.value.range.from == day(2026, 1, 4) }
    }

    // ── Bible ────────────────────────────────────────────────────────────

    @Test
    fun theBibleViewShowsVersesAndBooks() = runComposeUiTest {
        val f = fixture()

        showReport(f)
        click(ReportTags.tab(1))

        awaitThat { exists(ReportTags.VERSE_PLAYS) }
        onNodeWithTag(ReportTags.VERSE_PLAYS).assertTextContains("5")
        onNodeWithTag(ReportTags.UNIQUE_VERSES).assertTextContains("4")
        assertTrue(exists(ReportTags.book("John")))
        assertTrue(exists(ReportTags.verse(1)))
        assertTrue(isShowing("John 3:16"))
    }

    // ── Activity ─────────────────────────────────────────────────────────

    @Test
    fun theActivityViewShowsBothTotalsAndTheBusiestPeriod() = runComposeUiTest {
        val f = fixture()

        showReport(f)
        click(ReportTags.tab(2))

        awaitThat { exists(ReportTags.BUSIEST) }
        onNodeWithTag(ReportTags.SONG_PLAYS).assertTextContains("7")
        onNodeWithTag(ReportTags.VERSE_PLAYS).assertTextContains("5")
        assertFalse(exists(ReportTags.FILTER), "activity has no songbook or translation to filter by")
    }

    // ── Clearing ─────────────────────────────────────────────────────────

    @Test
    fun clearingAsksFirstAndCancellingKeepsEverything() = runComposeUiTest {
        val f = fixture()

        showReport(f)
        click(ReportTags.CLEAR)
        awaitThat { exists(ReportTags.CLEAR_CONFIRM) }
        click(ReportTags.CLEAR_CANCEL)

        awaitThat { !exists(ReportTags.CLEAR_CONFIRM) }
        assertEquals(7, f.repository.log.value.songs.size)
    }

    @Test
    fun confirmingTheClearEmptiesTheLogAndTheScreen() = runComposeUiTest {
        val f = fixture()

        showReport(f)
        click(ReportTags.CLEAR)
        awaitThat { exists(ReportTags.CLEAR_CONFIRM) }
        click(ReportTags.CLEAR_CONFIRM)

        awaitThat { exists(ReportTags.EMPTY) }
        assertTrue(f.repository.log.value.isEmpty)
    }

    // ── Tablet ───────────────────────────────────────────────────────────

    @Test
    fun onATabletTheSongsAreRankedBesideTheFullTable() = runComposeUiTest {
        val f = fixture()

        showReport(f, wide = true)

        assertTrue(exists(ReportTags.rankedSong(1)), "the rankings pane")
        assertTrue(exists(ReportTags.song(1)), "the table")
        assertFalse(exists(ReportTags.SONG_PLAYS), "the tiles belong to the phone layout and the activity view")
        assertTrue(isShowing("Amazing Grace"))
    }

    @Test
    fun onATabletTheBibleViewRanksBooksBesideTheVerseTable() = runComposeUiTest {
        val f = fixture()

        showReport(f, wide = true)
        click(ReportTags.tab(1))

        awaitThat { exists(ReportTags.book("John")) }
        assertTrue(exists(ReportTags.verse(1)))
        assertTrue(isShowing("John 3:16"))
    }

    @Test
    fun onATabletAnEmptyRangeSaysSoInTheTablePane() = runComposeUiTest {
        val f = fixture()

        showReport(f, wide = true)
        f.viewModel.setYear(2025)

        awaitThat { exists(ReportTags.EMPTY_RANGE) }
    }

    // ── Exports ──────────────────────────────────────────────────────────

    @Test
    fun bothExportsAreOffered() = runComposeUiTest {
        val f = fixture()

        showReport(f)

        assertTrue(exists(ReportTags.EXPORT_CSV))
        assertTrue(exists(ReportTags.EXPORT_XLS))
    }
}
