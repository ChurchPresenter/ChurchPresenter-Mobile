package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.type
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * "Add to Sunday Morning": the picker the run of show is filled from.
 *
 * Five tabs over one search field — which doubles as a reference field, so `John 3:16` typed while
 * looking at songs adds the passage. Each tab is driven here by the data it lists, since its label
 * comes from `strings.xml` and renders empty in this runtime.
 */
@OptIn(ExperimentalTestApi::class)
class AddToServiceContentTest {

    private val added = mutableListOf<Triple<PlanRow, Int?, RowTiming>>()

    private fun ComposeUiTest.show(sources: PickerSources = CalendarFixtures.sources()) = showScreen {
        AddToServiceContent(
            serviceName = CalendarFixtures.SERVICE_NAME,
            serviceStart = "10:00",
            sources = sources,
            newRowId = { "row-${added.size}" },
            onAdd = { row, seconds, timing -> added += Triple(row, seconds, timing) },
            onDismiss = {},
        )
    }

    private fun ComposeUiTest.search(text: String) = type(CalendarTags.PICKER_SEARCH, text)

    private fun ComposeUiTest.tab(tab: PickerTab) = click(CalendarTags.tab(tab))

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun theSongsTabOpensOnTheWholeLibrary() = runComposeUiTest {
        show()

        assertTrue(isShowing(CalendarFixtures.FIRST_SONG))
        assertTrue(isShowing(CalendarFixtures.SECOND_SONG))
    }

    @Test
    fun aSongIsListedByNumberAndTitle() = runComposeUiTest {
        show()

        assertTrue(isShowing("42 - ${CalendarFixtures.FIRST_SONG}"))
    }

    @Test
    fun searchingNarrowsTheList() = runComposeUiTest {
        show()

        search("great")
        waitForIdle()

        assertTrue(isShowing(CalendarFixtures.SECOND_SONG))
    }

    @Test
    fun tappingASongAddsIt() = runComposeUiTest {
        show()

        click(CalendarTags.song("42 - ${CalendarFixtures.FIRST_SONG}"))
        waitForIdle()
        click(CalendarTags.PICKER_ADD)
        waitForIdle()

        val row = assertIs<PlanRow.Song>(added.single().first)
        assertEquals("42 - ${CalendarFixtures.FIRST_SONG}", row.title)
    }

    @Test
    fun anEmptyLibrarySaysSoRatherThanShowingAnEmptyList() = runComposeUiTest {
        show(CalendarFixtures.sources(songs = emptyList()))

        // Nothing from the library is listed; the sheet still draws its search field and tabs.
        assertTrue(onAllNodes(hasSetTextAction(), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun aSearchThatMatchesNothingLeavesTheListEmpty() = runComposeUiTest {
        show()

        search("zzzz")
        waitForIdle()

        assertTrue(!isShowing(CalendarFixtures.FIRST_SONG))
    }

    // ── A reference typed into the search field ──────────────────────────

    @Test
    fun aTypedReferenceIsOfferedAsAPassage() = runComposeUiTest {
        show()

        search("John 3:16")
        waitForIdle()

        assertTrue(isShowing("John 3:16"), "the reference card, above the song list")
    }

    @Test
    fun theReferenceCardAddsThePassage() = runComposeUiTest {
        show()
        search("John 3:16-17")
        waitForIdle()

        click(CalendarTags.PICKER_ADD)
        waitForIdle()

        val row = assertIs<PlanRow.Bible>(added.single().first)
        assertEquals("John 3:16-17", row.title)
        assertEquals(43, row.bookId, "the book number the desktop resolves against")
    }

    @Test
    fun textThatIsNotAReferenceOffersNoCard() = runComposeUiTest {
        show()

        search("Amazing")
        waitForIdle()

        assertTrue(isShowing(CalendarFixtures.FIRST_SONG), "it is a song search, not a passage")
    }

    // ── The other tabs ───────────────────────────────────────────────────

    @Test
    fun theBibleTabListsTheBooks() = runComposeUiTest {
        show()

        tab(PickerTab.BIBLE)
        waitForIdle()

        assertTrue(isShowing("John"))
        assertTrue(isShowing("Psalms"))
    }

    @Test
    fun pickingABookOpensItsChapters() = runComposeUiTest {
        show()
        tab(PickerTab.BIBLE)
        waitForIdle()

        click(CalendarTags.book(43))
        waitForIdle()

        assertTrue(isShowing("21"), "John has 21 chapters")
    }

    @Test
    fun thePresetsTabListsWhatTheDesktopSent() = runComposeUiTest {
        show()

        tab(PickerTab.PRESETS)
        waitForIdle()

        assertTrue(isShowing("Welcome loop"))
        assertTrue(isShowing("Offering slide"))
    }

    @Test
    fun tappingAPresetAddsItByIdAlone() = runComposeUiTest {
        show()
        tab(PickerTab.PRESETS)
        waitForIdle()

        click(CalendarTags.preset("p1"))
        waitForIdle()
        click(CalendarTags.PICKER_ADD)
        waitForIdle()

        val row = assertIs<PlanRow.Preset>(added.single().first)
        assertEquals("p1", row.presetId, "what it contains stays on the desktop")
    }

    @Test
    fun aPhoneWithNoPresetsYetSaysSo() = runComposeUiTest {
        show(CalendarFixtures.sources(presets = emptyList()))

        tab(PickerTab.PRESETS)
        waitForIdle()

        assertTrue(!isShowing("Welcome loop"))
    }

    @Test
    fun theSectionTabOffersTheUsualNames() = runComposeUiTest {
        show()

        tab(PickerTab.SECTION)
        waitForIdle()

        // The suggestions are resource strings, so what is assertable is that the tab drew its
        // swatches and its own controls rather than the song list.
        assertTrue(!isShowing(CalendarFixtures.FIRST_SONG))
    }

    @Test
    fun aTypedSectionNameIsWhatGetsAdded() = runComposeUiTest {
        show()
        tab(PickerTab.SECTION)
        waitForIdle()

        search("Communion")
        waitForIdle()
        click(CalendarTags.PICKER_ADD)
        waitForIdle()

        assertEquals("Communion", added.single().first.title)
    }

    @Test
    fun theMinistryTabTakesWhatHappensAndWho() = runComposeUiTest {
        show()

        tab(PickerTab.MINISTRY)
        waitForIdle()

        type(CalendarTags.MINISTRY_WHAT, "Notices")
        type(CalendarTags.MINISTRY_WHO, "Anna")
        type(CalendarTags.MINISTRY_DURATION, "5:00")
        waitForIdle()
        click(CalendarTags.PICKER_ADD)
        waitForIdle()

        val row = assertIs<PlanRow.Ministry>(added.single().first)
        assertEquals("Notices", row.title)
        assertEquals("Anna", row.detail)
        assertEquals(300, added.single().second)
    }

    @Test
    fun switchingTabsClearsTheSearchSoOneTabsQueryDoesNotFilterTheNext() = runComposeUiTest {
        show()
        search("great")
        waitForIdle()

        tab(PickerTab.PRESETS)
        waitForIdle()

        assertTrue(isShowing("Welcome loop"), "not filtered by the song search")
    }
}
