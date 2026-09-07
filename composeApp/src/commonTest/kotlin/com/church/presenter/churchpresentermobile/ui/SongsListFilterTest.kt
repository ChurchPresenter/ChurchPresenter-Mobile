package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Songs tab's list — what an operator scans while a service is starting.
 *
 * Everything here is state the screen is handed, so the failures it guards are
 * about *which* of several states it shows: a list that keeps a stale error
 * banner, an empty result that reads as "no songs" when a filter is on, or a
 * card whose tap reports the wrong song. Every one of those puts the wrong
 * words on the screen behind the operator.
 */
/**
 * Searching and filtering the Songs tab.
 *
 * The screen holds neither the query nor the filter — both are reported to the
 * caller — so what it must get right is reporting them faithfully, and telling
 * a failed fetch apart from an empty result.
 */
@OptIn(ExperimentalTestApi::class)
class SongsListFilterTest {
    @Test
    fun noErrorBannerWhenThereIsNoError() = runComposeUiTest {
        showSongs()

        assertFalse(exists(UiTags.SONGS_ERROR))
    }

    @Test
    fun anErrorIsShownAsTheDesktopWordedIt() = runComposeUiTest {
        // The message names the address or the reason; a generic one leaves the
        // operator with nothing to act on.
        showSongs(error = "Could not reach 192.168.1.5")

        assertTrue(exists(UiTags.SONGS_ERROR))
        assertTrue(isShowing("Could not reach 192.168.1.5"))
    }

    @Test
    fun anErrorOffersARetry() = runComposeUiTest {
        var retried = false
        showSongs(error = "Could not reach the desktop", onRefresh = { retried = true })

        click(UiTags.SONGS_RETRY)

        assertTrue(retried)
    }

    @Test
    fun theSongsStayListedBehindAnError() = runComposeUiTest {
        // A blip must not blank a list the operator is mid-service with.
        showSongs(error = "Could not reach the desktop")

        assertTrue(exists(card("42")))
    }

    // ── The songbook filter ──────────────────────────────────────────────

    @Test
    fun theFilterOpensOnAllBooks() = runComposeUiTest {
        showSongs()

        assertTrue(exists(UiTags.SONGS_BOOK_FILTER))
        assertFalse(exists(UiTags.bookOption("Hymns")), "the menu should be closed")
    }

    @Test
    fun tappingTheFilterOffersEveryBook() = runComposeUiTest {
        showSongs()

        click(UiTags.SONGS_BOOK_FILTER)

        assertTrue(exists(UiTags.bookOption(null)), "no 'all books' entry")
        assertTrue(exists(UiTags.bookOption("Hymns")))
        assertTrue(exists(UiTags.bookOption("Chorus Book")))
    }

    @Test
    fun pickingABookReportsIt() = runComposeUiTest {
        var picked: String? = null
        var reported = false
        showSongs(onBookSelected = { picked = it; reported = true })

        click(UiTags.SONGS_BOOK_FILTER)
        click(UiTags.bookOption("Chorus Book"))

        assertTrue(reported)
        assertEquals("Chorus Book", picked)
    }

    @Test
    fun pickingAllBooksClearsTheFilter() = runComposeUiTest {
        // Null is how the caller is told the filter is off; "All books" as a
        // string would be looked up as a songbook of that name.
        var picked: String? = "Hymns"
        var reported = false
        showSongs(selectedBook = "Hymns", onBookSelected = { picked = it; reported = true })

        click(UiTags.SONGS_BOOK_FILTER)
        click(UiTags.bookOption(null))

        assertTrue(reported)
        assertNull(picked)
    }

    @Test
    fun theChosenBookIsWhatTheFilterSays() = runComposeUiTest {
        showSongs(selectedBook = "Chorus Book")

        assertTrue(isShowing("CHORUS BOOK"))
    }
}
