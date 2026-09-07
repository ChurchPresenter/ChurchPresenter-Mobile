package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.ui.test.hasText
import com.church.presenter.churchpresentermobile.model.DictionaryVerse
import com.church.presenter.churchpresentermobile.model.DictionaryVersesResponse
import com.church.presenter.churchpresentermobile.ui.DictionaryScreen
import com.church.presenter.churchpresentermobile.ui.EntryDetail
import com.church.presenter.churchpresentermobile.ui.FakeDesktop
import com.church.presenter.churchpresentermobile.ui.agape
import com.church.presenter.churchpresentermobile.ui.bara
import kotlin.test.Test

/**
 * The Strong's dictionary: the search list, and one entry opened.
 *
 * The entry sheet is the interesting half — it grows a "appears in" section
 * that can be loading, loaded or absent, and its definition text carries
 * cross-references (`compare H1262 and G26`) that are turned into links.
 */
class DictionaryScreenshotTest {

    @Test
    fun searchResults() = screenshot(
        "dictionary__results",
        until = { onAllNodes(hasText("bara", substring = true)).fetchSemanticsNodes().isNotEmpty() },
    ) {
        DictionaryScreen(viewModel = FakeDesktop().viewModel(), settingsSaveToken = 0)
    }

    @Test
    fun noEntries() = screenshot("dictionary__empty") {
        DictionaryScreen(viewModel = FakeDesktop(entries = emptyList()).viewModel(), settingsSaveToken = 0)
    }

    @Test
    fun hebrewEntry() = screenshot("dictionary-entry__hebrew") {
        EntryDetail(
            entry = bara,
            scheduleAdded = false,
            appearsIn = null,
            appearsInLoading = false,
            onProject = {},
            onAddToSchedule = {},
            onOpenNumber = {},
        )
    }

    @Test
    fun greekEntry() = screenshot("dictionary-entry__greek") {
        EntryDetail(
            entry = agape,
            scheduleAdded = false,
            appearsIn = null,
            appearsInLoading = false,
            onProject = {},
            onAddToSchedule = {},
            onOpenNumber = {},
        )
    }

    @Test
    fun entryAlreadyInSchedule() = screenshot("dictionary-entry__in-schedule") {
        EntryDetail(
            entry = bara,
            scheduleAdded = true,
            appearsIn = null,
            appearsInLoading = false,
            onProject = {},
            onAddToSchedule = {},
            onOpenNumber = {},
        )
    }

    @Test
    fun occurrencesLoading() = screenshot("dictionary-entry__occurrences-loading") {
        EntryDetail(
            entry = bara,
            scheduleAdded = false,
            appearsIn = null,
            appearsInLoading = true,
            onProject = {},
            onAddToSchedule = {},
            onOpenNumber = {},
        )
    }

    @Test
    fun occurrencesLoaded() = screenshot("dictionary-entry__occurrences") {
        EntryDetail(
            entry = bara,
            scheduleAdded = false,
            appearsIn = DictionaryVersesResponse(
                number = bara.number,
                total = 54,
                verses = listOf(
                    DictionaryVerse("Genesis", 1, 1, "Genesis 1:1", "In the beginning God created the heaven"),
                    DictionaryVerse("Genesis", 1, 21, "Genesis 1:21", "And God created great whales"),
                    DictionaryVerse("Isaiah", 40, 26, "Isaiah 40:26", "Who hath created these things?"),
                ),
            ),
            appearsInLoading = false,
            onProject = {},
            onAddToSchedule = {},
            onOpenNumber = {},
        )
    }
}
