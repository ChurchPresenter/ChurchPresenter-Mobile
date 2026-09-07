package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The dictionary entry sheet — everything known about one Strong's number.
 *
 * The desktop fills in as much of an entry as its data set has, so most of this
 * screen is optional: a blank transliteration, no KJV usage, an occurrence list
 * still on its way. Each of those must be *absent* rather than an empty label
 * with nothing under it, which is why the negative cases are asserted as
 * carefully as the positive ones.
 *
 * The body is composed directly rather than through its `ModalBottomSheet`: the
 * sheet contributes a scrim and a spring animation and nothing this screen says.
 */
@OptIn(ExperimentalTestApi::class)
class DictionaryEntrySheetTest {

    // ── The word itself ──────────────────────────────────────────────────

    @Test
    fun theWordIsShown() = runComposeUiTest {
        showEntryDetail()

        assertTrue(isShowing("bara"))
    }

    @Test
    fun theStrongsNumberIsShown() = runComposeUiTest {
        showEntryDetail()

        assertTrue(isShowing("H1254"))
    }

    @Test
    fun theTransliterationIsShown() = runComposeUiTest {
        showEntryDetail()

        assertTrue(exists(UiTags.DICT_SHEET_TRANSLITERATION))
    }

    @Test
    fun theTransliterationIsTheOneTheDesktopSent() = runComposeUiTest {
        showEntryDetail(entry = strongs("H430", word = "elohim", transliteration = "el-o-heem"))

        assertTrue(isShowing("el-o-heem"))
    }

    @Test
    fun anEntryWithNoTransliterationShowsNone() = runComposeUiTest {
        showEntryDetail(entry = strongs("H430", word = "elohim"))

        assertFalse(exists(UiTags.DICT_SHEET_TRANSLITERATION))
    }

    @Test
    fun aBlankTransliterationIsNotAnEmptyLine() = runComposeUiTest {
        showEntryDetail(entry = strongs("H430", word = "elohim", transliteration = "   "))

        assertFalse(exists(UiTags.DICT_SHEET_TRANSLITERATION))
    }

    @Test
    fun thePronunciationIsShown() = runComposeUiTest {
        showEntryDetail()

        assertTrue(exists(UiTags.DICT_SHEET_PRONUNCIATION))
    }

    @Test
    fun thePronunciationIsTheOneTheDesktopSent() = runComposeUiTest {
        showEntryDetail(entry = strongs("H430", word = "elohim", pronunciation = "el-o-heem'"))

        assertTrue(isShowing("el-o-heem'"))
    }

    @Test
    fun anEntryWithNoPronunciationShowsNone() = runComposeUiTest {
        showEntryDetail(entry = strongs("H430", word = "elohim"))

        assertFalse(exists(UiTags.DICT_SHEET_PRONUNCIATION))
    }

    @Test
    fun theLanguageIsNamed() = runComposeUiTest {
        showEntryDetail()

        assertTrue(exists(UiTags.DICT_SHEET_LANGUAGE))
    }

    // ── Occurrences ──────────────────────────────────────────────────────

    @Test
    fun theOccurrenceCountIsShown() = runComposeUiTest {
        showEntryDetail()

        assertTrue(exists(UiTags.DICT_SHEET_OCCURRENCES))
    }

    // The count itself is not asserted here: the sheet writes it through a
    // formatted `stringResource` ("2,606 occurrences"), which renders empty in
    // the wasmJs runtime. The number is covered where it is plain text — on the
    // list row, and in OccurrenceCountTest.

    @Test
    fun theSheetShowsTheEntryItWasGiven() = runComposeUiTest {
        // Opening the second row and reading the first entry's word is a real
        // failure that every "does it render" assertion here would miss.
        showEntryDetail(entry = agape)

        assertTrue(isShowing("agape"))
        assertFalse(isShowing("bara"))
    }

    @Test
    fun anEntryWithNoOccurrencesStillShowsTheCard() = runComposeUiTest {
        // Zero is a fact about the interlinear index, not a missing field: the
        // card stays so the sheet does not silently drop a section.
        showEntryDetail(entry = strongs("H9999", word = "hapax"))

        assertTrue(exists(UiTags.DICT_SHEET_OCCURRENCES))
    }

    // ── Definition ───────────────────────────────────────────────────────

    @Test
    fun theDefinitionIsShown() = runComposeUiTest {
        showEntryDetail()

        assertTrue(exists(UiTags.DICT_SHEET_DEFINITION))
    }

    @Test
    fun theDefinitionIsTheOneTheDesktopSent() = runComposeUiTest {
        showEntryDetail(entry = strongs("H430", word = "elohim", definition = "rulers, judges, divine ones"))

        assertTrue(isShowing("rulers, judges, divine ones"))
    }

    @Test
    fun anEntryWithNoDefinitionShowsNoDefinitionSection() = runComposeUiTest {
        showEntryDetail(entry = strongs("H430", word = "elohim"))

        assertFalse(exists(UiTags.DICT_SHEET_DEFINITION))
    }

    // ── KJV usage ────────────────────────────────────────────────────────

    @Test
    fun theKjvUsageIsShown() = runComposeUiTest {
        showEntryDetail()

        assertTrue(exists(UiTags.DICT_SHEET_KJV_USAGE))
    }

    @Test
    fun theKjvUsageIsTheOneTheDesktopSent() = runComposeUiTest {
        showEntryDetail(entry = strongs("H430", word = "elohim", kjvUsage = "God, gods, judge"))

        assertTrue(isShowing("God, gods, judge"))
    }

    @Test
    fun anEntryWithNoKjvUsageShowsNoUsageSection() = runComposeUiTest {
        showEntryDetail(entry = strongs("H430", word = "elohim"))

        assertFalse(exists(UiTags.DICT_SHEET_KJV_USAGE))
    }

    // ── Where the word appears ───────────────────────────────────────────

    @Test
    fun theOccurrenceListIsShownOnceItArrives() = runComposeUiTest {
        showEntryDetail(appearsIn = appearsIn("Genesis 1:1" to "In the beginning God created"))

        assertTrue(exists(UiTags.DICT_SHEET_APPEARS_IN))
    }

    @Test
    fun eachOccurrenceNamesItsReference() = runComposeUiTest {
        showEntryDetail(
            appearsIn = appearsIn(
                "Genesis 1:1" to "In the beginning God created",
                "Isaiah 40:26" to "Lift up your eyes on high",
            ),
        )

        assertTrue(exists(UiTags.dictAppearsVerse("Genesis 1:1")))
        assertTrue(exists(UiTags.dictAppearsVerse("Isaiah 40:26")))
    }

    @Test
    fun eachOccurrenceShowsItsVerse() = runComposeUiTest {
        showEntryDetail(appearsIn = appearsIn("Genesis 1:1" to "In the beginning God created"))

        assertTrue(isShowing("In the beginning God created"))
    }

    @Test
    fun anOccurrenceWithNoVerseTextStillShowsItsReference() = runComposeUiTest {
        // The desktop sends references without text when the Bible text itself
        // is unavailable; a reference alone is still worth showing.
        showEntryDetail(appearsIn = appearsIn("Genesis 1:1" to ""))

        assertTrue(exists(UiTags.dictAppearsVerse("Genesis 1:1")))
    }

    @Test
    fun theOccurrenceListWaitsBehindASpinner() = runComposeUiTest {
        showEntryDetail(appearsInLoading = true)

        assertTrue(exists(UiTags.DICT_SHEET_APPEARS_LOADING))
    }

    @Test
    fun noOccurrenceListIsShownWhileItLoads() = runComposeUiTest {
        showEntryDetail(appearsInLoading = true)

        assertFalse(exists(UiTags.DICT_SHEET_APPEARS_IN))
    }

    @Test
    fun theSpinnerGoesWhenTheOccurrencesArrive() = runComposeUiTest {
        showEntryDetail(
            appearsIn = appearsIn("Genesis 1:1" to "In the beginning"),
            appearsInLoading = false,
        )

        assertFalse(exists(UiTags.DICT_SHEET_APPEARS_LOADING))
    }

    @Test
    fun aStillLoadingListThatAlreadyHasVersesShowsThem() = runComposeUiTest {
        // A refresh must not blank out what is already on screen.
        showEntryDetail(
            appearsIn = appearsIn("Genesis 1:1" to "In the beginning"),
            appearsInLoading = true,
        )

        assertTrue(exists(UiTags.dictAppearsVerse("Genesis 1:1")))
    }

    @Test
    fun anEntryThatAppearsNowhereShowsNoOccurrenceSection() = runComposeUiTest {
        showEntryDetail(appearsIn = appearsIn())

        assertFalse(exists(UiTags.DICT_SHEET_APPEARS_IN))
    }

    @Test
    fun aSheetWithNoOccurrenceDataYetShowsNoSection() = runComposeUiTest {
        showEntryDetail(appearsIn = null, appearsInLoading = false)

        assertFalse(exists(UiTags.DICT_SHEET_APPEARS_IN))
        assertFalse(exists(UiTags.DICT_SHEET_APPEARS_LOADING))
    }

    @Test
    fun aCappedOccurrenceListSaysHowManyThereAre() = runComposeUiTest {
        // 25 shown out of 54 is the normal case; without the count the operator
        // would read the list as complete.
        showEntryDetail(
            appearsIn = appearsIn("Genesis 1:1" to "In the beginning", total = 54),
        )

        assertTrue(exists(UiTags.DICT_SHEET_APPEARS_COUNT))
    }

    @Test
    fun aCompleteOccurrenceListShowsNoCount() = runComposeUiTest {
        showEntryDetail(
            appearsIn = appearsIn(
                "Genesis 1:1" to "In the beginning",
                "Genesis 1:21" to "And God created",
                total = 2,
            ),
        )

        assertFalse(exists(UiTags.DICT_SHEET_APPEARS_COUNT))
    }

    // ── The two actions ──────────────────────────────────────────────────

    @Test
    fun theSheetOffersGoLive() = runComposeUiTest {
        showEntryDetail()

        assertTrue(exists(UiTags.DICT_SHEET_PROJECT))
    }

    @Test
    fun theSheetOffersAddToSchedule() = runComposeUiTest {
        showEntryDetail()

        assertTrue(exists(UiTags.DICT_SHEET_ADD_TO_SCHEDULE))
    }

    @Test
    fun goingLiveReachesTheHandler() = runComposeUiTest {
        var projected = 0
        showEntryDetail(onProject = { projected++ })

        click(UiTags.DICT_SHEET_PROJECT)

        assertEquals(1, projected)
    }

    @Test
    fun addingToTheScheduleReachesTheHandler() = runComposeUiTest {
        var added = 0
        showEntryDetail(onAddToSchedule = { added++ })

        click(UiTags.DICT_SHEET_ADD_TO_SCHEDULE)

        assertEquals(1, added)
    }

    @Test
    fun goingLiveDoesNotAlsoAddToTheSchedule() = runComposeUiTest {
        var added = 0
        showEntryDetail(onProject = {}, onAddToSchedule = { added++ })

        click(UiTags.DICT_SHEET_PROJECT)

        assertEquals(0, added)
    }

    @Test
    fun addingToTheScheduleDoesNotAlsoGoLive() = runComposeUiTest {
        var projected = 0
        showEntryDetail(onProject = { projected++ }, onAddToSchedule = {})

        click(UiTags.DICT_SHEET_ADD_TO_SCHEDULE)

        assertEquals(0, projected)
    }

    @Test
    fun anAlreadyScheduledEntryStillOffersTheAction() = runComposeUiTest {
        // The tint changes once it is on the schedule; the button must not stop
        // working — the same entry can legitimately be added twice.
        var added = 0
        showEntryDetail(scheduleAdded = true, onAddToSchedule = { added++ })

        click(UiTags.DICT_SHEET_ADD_TO_SCHEDULE)

        assertEquals(1, added)
    }
}
