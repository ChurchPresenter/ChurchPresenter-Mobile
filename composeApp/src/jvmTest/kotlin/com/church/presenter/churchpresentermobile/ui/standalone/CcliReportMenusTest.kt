package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.library.PlayLogRepository
import com.church.presenter.churchpresentermobile.model.ReportFixtures
import com.church.presenter.churchpresentermobile.model.ReportFixtures.at
import com.church.presenter.churchpresentermobile.model.ReportFixtures.day
import com.church.presenter.churchpresentermobile.model.ReportPreset
import com.church.presenter.churchpresentermobile.model.SongCredit
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.CcliReportViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * The CCLI report's menus and date pickers, used as an operator uses them: open
 * one, pick, and the report narrows. On the JVM because the date picker's buttons
 * are found by their words, which do not resolve in the wasm test runtime.
 */
@OptIn(ExperimentalTestApi::class)
class CcliReportMenusTest {

    private val today = at(day(2026, 3, 25))

    private class Fixture(now: Long, extra: List<SongCredit>) {
        var clock = now
        val repository = PlayLogRepository(InMemoryFileStorage()) { clock }
        val viewModel: CcliReportViewModel

        init {
            ReportFixtures.log.songs.forEach {
                clock = it.at
                repository.recordSong(it.credit)
            }
            ReportFixtures.log.verses.forEach {
                clock = it.at
                repository.recordVerse(it.credit)
            }
            clock = now
            extra.forEach(repository::recordSong)
            viewModel = CcliReportViewModel(repository, now = { clock }, zone = ReportFixtures.zone)
        }

        val filters get() = viewModel.filters.value
    }

    private fun fixture(extra: List<SongCredit> = emptyList()) = Fixture(today, extra)

    private fun ComposeUiTest.showReport(f: Fixture) = showScreen {
        Box(modifier = Modifier.width(360.dp)) {
            CcliReportScreen(f.repository, providedViewModel = f.viewModel)
        }
    }

    private fun ComposeUiTest.press(words: String) =
        onNodeWithText(words).performSemanticsAction(SemanticsActions.OnClick)

    // ── Year ─────────────────────────────────────────────────────────────

    @Test
    fun pickingAYearMakesItTheRange() = runComposeUiTest {
        val f = fixture()
        showReport(f)

        click(ReportTags.YEAR)
        awaitThat { exists(ReportTags.year(2026)) }
        click(ReportTags.year(2026))

        awaitThat { f.filters.preset == ReportPreset.YEAR }
        assertEquals(day(2026, 1, 1), f.filters.range.from)
        assertEquals(day(2026, 12, 31), f.filters.range.to)
    }

    // ── Songbook and translation ─────────────────────────────────────────

    @Test
    fun pickingASongbookNarrowsTheReportAndAllWidensItAgain() = runComposeUiTest {
        val f = fixture()
        showReport(f)

        click(ReportTags.FILTER)
        awaitThat { exists(ReportTags.filterOption("Worship")) }
        click(ReportTags.filterOption("Worship"))
        awaitThat { f.filters.songbook == "Worship" }

        awaitThat { !exists(ReportTags.filterOption("Worship")) }
        click(ReportTags.FILTER)
        awaitThat { exists(ReportTags.filterOption(null)) }
        click(ReportTags.filterOption(null))

        awaitThat { f.filters.songbook == null }
    }

    @Test
    fun aSongWithNoBookIsOfferedByName() = runComposeUiTest {
        val f = fixture(extra = listOf(SongCredit(id = "s9", number = "9", title = "Untitled", songbook = "")))
        showReport(f)

        click(ReportTags.FILTER)

        awaitThat { isShowing("No songbook") }
    }

    @Test
    fun theBibleViewFiltersByTranslation() = runComposeUiTest {
        val f = fixture()
        showReport(f)
        click(ReportTags.tab(1))
        awaitThat { exists(ReportTags.FILTER) }

        click(ReportTags.FILTER)
        awaitThat { exists(ReportTags.filterOption("NIV")) }
        click(ReportTags.filterOption("NIV"))

        awaitThat { f.filters.bible == "NIV" }
    }

    // ── Dates ────────────────────────────────────────────────────────────

    @Test
    fun confirmingTheFromDateKeepsThatDay() = runComposeUiTest {
        // A date that shifted a day here would be the picker's UTC millis read
        // in the wrong zone, which only shows east of Greenwich.
        val f = fixture()
        val from = f.filters.range.from
        showReport(f)

        click(ReportTags.FROM)
        awaitThat { isShowing("Cancel") }
        press("OK")

        awaitThat { f.filters.preset == null }
        assertEquals(from, f.filters.range.from)
    }

    @Test
    fun confirmingTheToDateKeepsThatDay() = runComposeUiTest {
        val f = fixture()
        val to = f.filters.range.to
        showReport(f)

        click(ReportTags.TO)
        awaitThat { isShowing("Cancel") }
        press("OK")

        awaitThat { f.filters.preset == null }
        assertEquals(to, f.filters.range.to)
    }

    @Test
    fun cancellingTheDatePickerLeavesTheRangeAlone() = runComposeUiTest {
        val f = fixture()
        val preset = f.filters.preset
        showReport(f)

        click(ReportTags.FROM)
        awaitThat { isShowing("Cancel") }
        press("Cancel")

        awaitThat { !isShowing("Cancel") }
        assertEquals(preset, f.filters.preset)
        assertNotEquals(null, preset)
        assertNull(f.filters.songbook)
    }
}
