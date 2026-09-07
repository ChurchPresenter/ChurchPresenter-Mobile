package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the book list says when it has nothing to list.
 *
 * "No match" means clear the search; "no books" means download a translation.
 * Confusing them sends an operator to re-download one they already have.
 */
@OptIn(ExperimentalTestApi::class)
class BibleBooksEmptyStatesTest {

    @Test
    fun aSearchThatMatchesNothingSaysSo() = runComposeUiTest {
        showBooks(books = emptyList(), searchQuery = "zzz")

        assertTrue(exists(UiTags.BIBLE_NO_MATCH))
    }

    @Test
    fun anEmptySearchResultIsNotConfusedWithNoBible() = runComposeUiTest {
        // "No match" means clear the search; "no books" means download one.
        showBooks(books = emptyList(), searchQuery = "zzz")

        assertFalse(exists(UiTags.BIBLE_NO_BOOKS))
    }

    // ── Nothing downloaded ───────────────────────────────────────────────

    @Test
    fun noBibleAtAllSaysSo() = runComposeUiTest {
        showBooks(books = emptyList(), searchQuery = "")

        assertTrue(exists(UiTags.BIBLE_NO_BOOKS))
    }

    @Test
    fun nothingIsSaidWhileTheBooksAreStillLoading() = runComposeUiTest {
        // "No Bible" during the first fetch is wrong and reads as a failure.
        showBooks(books = emptyList(), isLoading = true)

        assertFalse(exists(UiTags.BIBLE_NO_BOOKS))
    }

    @Test
    fun aSearchWithNoMatchIsStillReportedWhileLoading() = runComposeUiTest {
        // The operator has typed; telling them nothing matches is accurate even
        // if a refresh happens to be in flight.
        showBooks(books = emptyList(), searchQuery = "zzz", isLoading = true)

        assertTrue(exists(UiTags.BIBLE_NO_MATCH))
    }

    @Test
    fun neitherEmptyStateIsShownOnceBooksArrive() = runComposeUiTest {
        showBooks()

        assertFalse(exists(UiTags.BIBLE_NO_BOOKS))
        assertFalse(exists(UiTags.BIBLE_NO_MATCH))
    }

    @Test
    fun theSearchFieldIsOfferedEvenWithNoBooks() = runComposeUiTest {
        // It is how the operator gets back from a search that matched nothing.
        showBooks(books = emptyList())

        assertTrue(exists(UiTags.BIBLE_SEARCH))
    }
}
