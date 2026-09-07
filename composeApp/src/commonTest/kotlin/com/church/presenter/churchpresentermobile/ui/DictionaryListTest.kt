package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.DictionaryFilter
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The dictionary index — the list of Strong's entries and the two filters above it.
 *
 * The screen has nothing of its own to fall back on: every entry shown came from
 * a request, so the states worth pinning are the ones where a request did not
 * come back with a list. They are three different problems — the desktop said
 * no, the desktop has not answered yet, and the desktop answered with nothing —
 * and each is tagged apart because their words are `stringResource`s.
 */
@OptIn(ExperimentalTestApi::class)
class DictionaryListTest {

    // ── What the operator sees first ─────────────────────────────────────

    @Test
    fun theEntriesTheDesktopSentAreListed() = runComposeUiTest {
        val desktop = FakeDesktop()
        val vm = desktop.viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H1254")) }
        assertTrue(exists(UiTags.dictEntry("G26")))
    }

    @Test
    fun anEntryShowsItsWord() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H1254")) }
        assertTrue(isShowing("bara"))
    }

    @Test
    fun anEntryShowsItsTransliteration() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H1254")) }
        assertTrue(isShowing("baw-raw"))
    }

    @Test
    fun anEntryShowsItsDefinition() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("G26")) }
        assertTrue(isShowing("love, affection, benevolence"))
    }

    @Test
    fun anEntryShowsItsNumber() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H1254")) }
        assertTrue(isShowing("H1254"))
    }

    @Test
    fun aTransliterationlessEntryStillLists() = runComposeUiTest {
        val vm = FakeDesktop(entries = listOf(strongs("H7225", word = "reshith"))).viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H7225")) }
        assertTrue(isShowing("reshith"))
    }

    // ── Occurrence counts ────────────────────────────────────────────────

    @Test
    fun anEntryShowsHowOftenItOccurs() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H1254")) }
        assertTrue(exists(UiTags.dictEntryUses("H1254")))
    }

    @Test
    fun theOccurrenceCountIsTheNumberTheDesktopSent() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("G26")) }
        assertTrue(isShowing("116"))
    }

    @Test
    fun aFourFigureCountIsGrouped() = runComposeUiTest {
        val vm = FakeDesktop(
            entries = listOf(strongs("H430", word = "elohim", occurrences = 2606)),
        ).viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H430")) }
        assertTrue(isShowing("2,606"))
    }

    @Test
    fun anEntryWithNoRecordedOccurrencesShowsNoCount() = runComposeUiTest {
        // Zero is not "0 uses" — the interlinear index simply has nothing for
        // this number, and a zero would read as a word that appears nowhere.
        val vm = FakeDesktop(entries = listOf(strongs("H9999", word = "hapax"))).viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H9999")) }
        assertFalse(exists(UiTags.dictEntryUses("H9999")))
    }

    // ── The three empty screens ──────────────────────────────────────────

    @Test
    fun aDesktopThatRefusesTheSearchExplainsItself() = runComposeUiTest {
        val vm = FakeDesktop(searchStatus = HttpStatusCode.InternalServerError).viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.DICT_ERROR) }
    }

    @Test
    fun aFailedSearchListsNoEntries() = runComposeUiTest {
        val vm = FakeDesktop(searchStatus = HttpStatusCode.InternalServerError).viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.DICT_ERROR) }
        assertFalse(exists(UiTags.dictEntry("H1254")))
    }

    @Test
    fun aFailedSearchIsNotShownAsAnEmptyDictionary() = runComposeUiTest {
        // "No entries" would send the operator looking for a search term to
        // change, when the actual problem is the connection.
        val vm = FakeDesktop(searchStatus = HttpStatusCode.InternalServerError).viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.DICT_ERROR) }
        assertFalse(exists(UiTags.DICT_EMPTY))
    }

    @Test
    fun aDesktopWithNothingMatchingSaysSo() = runComposeUiTest {
        val vm = FakeDesktop(entries = emptyList()).viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.DICT_EMPTY) }
    }

    @Test
    fun anEmptyDictionaryIsNotShownAsAFailure() = runComposeUiTest {
        val vm = FakeDesktop(entries = emptyList()).viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.DICT_EMPTY) }
        assertFalse(exists(UiTags.DICT_ERROR))
    }

    @Test
    fun aLoadedListShowsNoEmptyHint() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H1254")) }
        assertFalse(exists(UiTags.DICT_EMPTY))
    }

    @Test
    fun aLoadedListShowsNoSpinner() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H1254")) }
        assertFalse(exists(UiTags.DICT_LOADING))
    }

    // ── Searching ────────────────────────────────────────────────────────

    @Test
    fun theSearchFieldIsOffered() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        assertTrue(exists(UiTags.DICT_SEARCH))
    }

    @Test
    fun typingASearchKeepsTheWords() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        type(UiTags.DICT_SEARCH, "love")

        assertEquals("love", vm.searchQuery.value)
    }

    @Test
    fun theTypedSearchIsShownBack() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        type(UiTags.DICT_SEARCH, "create")

        assertTrue(isShowing("create"))
    }

    @Test
    fun aSearchReachesTheDesktop() = runComposeUiTest {
        val desktop = FakeDesktop()
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { desktop.searches.isNotEmpty() }

        type(UiTags.DICT_SEARCH, "grace")

        awaitThat { desktop.searches.any { it["q"] == "grace" } }
    }

    @Test
    fun theOpeningSearchAsksForEverything() = runComposeUiTest {
        val desktop = FakeDesktop()
        val vm = desktop.viewModel()
        showDictionary(vm)

        awaitThat { desktop.searches.isNotEmpty() }
        assertEquals("", desktop.searches.first()["q"])
    }

    @Test
    fun theOpeningSearchIsUnfiltered() = runComposeUiTest {
        val desktop = FakeDesktop()
        val vm = desktop.viewModel()
        showDictionary(vm)

        awaitThat { desktop.searches.isNotEmpty() }
        assertEquals(DictionaryFilter.ALL.apiValue, desktop.searches.first()["filter"])
    }

    @Test
    fun clearingTheSearchEmptiesTheQuery() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        type(UiTags.DICT_SEARCH, "love")
        type(UiTags.DICT_SEARCH, "")

        assertEquals("", vm.searchQuery.value)
    }

    // ── The language filter ──────────────────────────────────────────────

    @Test
    fun theLanguageFilterOffersThreeChoices() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        assertTrue(exists(UiTags.dictFilter(0)))
        assertTrue(exists(UiTags.dictFilter(1)))
        assertTrue(exists(UiTags.dictFilter(2)))
    }

    @Test
    fun everythingIsSelectedToBeginWith() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        tagged(UiTags.dictFilter(0)).assertIsSelected()
    }

    @Test
    fun neitherLanguageIsSelectedToBeginWith() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        tagged(UiTags.dictFilter(1)).assertIsNotSelected()
        tagged(UiTags.dictFilter(2)).assertIsNotSelected()
    }

    @Test
    fun pickingHebrewFiltersToHebrew() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        click(UiTags.dictFilter(1))

        assertEquals(DictionaryFilter.HEBREW, vm.filter.value)
    }

    @Test
    fun pickingGreekFiltersToGreek() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        click(UiTags.dictFilter(2))

        assertEquals(DictionaryFilter.GREEK, vm.filter.value)
    }

    @Test
    fun theChosenLanguageIsShownAsSelected() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        click(UiTags.dictFilter(1))

        awaitThat { vm.filter.value == DictionaryFilter.HEBREW }
        tagged(UiTags.dictFilter(1)).assertIsSelected()
    }

    @Test
    fun choosingALanguageDeselectsEverything() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        click(UiTags.dictFilter(2))

        awaitThat { vm.filter.value == DictionaryFilter.GREEK }
        tagged(UiTags.dictFilter(0)).assertIsNotSelected()
    }

    @Test
    fun theLanguageFilterReachesTheDesktop() = runComposeUiTest {
        val desktop = FakeDesktop()
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { desktop.searches.isNotEmpty() }

        click(UiTags.dictFilter(1))

        awaitThat { desktop.searches.any { it["filter"] == DictionaryFilter.HEBREW.apiValue } }
    }

    @Test
    fun goingBackToAllReachesTheDesktop() = runComposeUiTest {
        val desktop = FakeDesktop()
        val vm = desktop.viewModel()
        showDictionary(vm)
        click(UiTags.dictFilter(2))
        awaitThat { vm.filter.value == DictionaryFilter.GREEK }

        click(UiTags.dictFilter(0))

        awaitThat { vm.filter.value == DictionaryFilter.ALL }
    }

    @Test
    fun reTappingTheSelectedLanguageChangesNothing() = runComposeUiTest {
        // The ViewModel drops a repeat of the current filter rather than
        // re-running the search; the selection must survive that.
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        click(UiTags.dictFilter(0))

        assertEquals(DictionaryFilter.ALL, vm.filter.value)
    }

    // ── Opening an entry ─────────────────────────────────────────────────

    @Test
    fun tappingAnEntryOpensThatEntry() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        click(UiTags.dictEntry("H1254"))

        awaitThat { vm.selectedEntry.value?.number == "H1254" }
    }

    @Test
    fun tappingTheSecondEntryOpensTheSecondEntry() = runComposeUiTest {
        // Two rows on screen and the wrong one opening is exactly the failure a
        // "does it render" test never sees.
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("G26")) }

        click(UiTags.dictEntry("G26"))

        awaitThat { vm.selectedEntry.value?.number == "G26" }
    }

    @Test
    fun nothingIsOpenBeforeAnEntryIsTapped() = runComposeUiTest {
        val vm = FakeDesktop().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H1254")) }
        assertEquals(null, vm.selectedEntry.value)
    }

    @Test
    fun openingAnEntryAsksTheDesktopWhereItAppears() = runComposeUiTest {
        val desktop = FakeDesktop(verses = appearsIn("Genesis 1:1" to "In the beginning"))
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.dictEntry("H1254")) }

        click(UiTags.dictEntry("H1254"))

        awaitThat { desktop.verseRequests.isNotEmpty() }
    }

    // ── Settings changes ─────────────────────────────────────────────────

    @Test
    fun savingSettingsRunsTheSearchAgain() = runComposeUiTest {
        // The desktop's address may have just changed; the list on screen was
        // fetched from the old one.
        val desktop = FakeDesktop()
        val vm = desktop.viewModel()
        showDictionary(vm, settingsSaveToken = 1)

        awaitThat { desktop.searches.size >= 2 }
    }

    @Test
    fun openingWithoutASettingsSaveSearchesOnlyOnce() = runComposeUiTest {
        val desktop = FakeDesktop()
        val vm = desktop.viewModel()
        showDictionary(vm, settingsSaveToken = 0)

        awaitThat { desktop.searches.isNotEmpty() }
        assertEquals(1, desktop.searches.size)
    }
}
