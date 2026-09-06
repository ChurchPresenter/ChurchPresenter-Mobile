package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the Songs tab says when there is nothing to list.
 *
 * Three different problems with three different fixes: clear the filter, get
 * songs onto this phone, or go and look at the desktop. Sending an operator to
 * the wrong one mid-service costs them the time they do not have.
 *
 * Each state therefore carries its own tag, and each test asserts the other two
 * are *absent* — with a single shared tag every one of these passed while the
 * screen showed the wrong words, which is how they were written before.
 */
@OptIn(ExperimentalTestApi::class)
class SongsListEmptyStatesTest {

    @Test
    fun aFilterThatMatchesNothingSaysSo() = runComposeUiTest {
        showSongs(songs = emptyList(), hasActiveFilter = true)

        assertTrue(exists(UiTags.SONGS_EMPTY_NO_MATCH))
    }

    @Test
    fun anEmptySearchResultIsNotConfusedWithAnEmptyCatalogue() = runComposeUiTest {
        // "No songs match" sends the operator to clear the filter; "no songs"
        // sends them to check the desktop.
        showSongs(songs = emptyList(), hasActiveFilter = true)

        assertFalse(exists(UiTags.SONGS_EMPTY_NO_SONGS))
    }

    @Test
    fun anEmptyCatalogueSaysSo() = runComposeUiTest {
        showSongs(songs = emptyList(), hasActiveFilter = false)

        assertTrue(exists(UiTags.SONGS_EMPTY_NO_SONGS))
    }

    @Test
    fun anEmptyCatalogueIsNotReportedAsAFailedSearch() = runComposeUiTest {
        showSongs(songs = emptyList(), hasActiveFilter = false)

        assertFalse(exists(UiTags.SONGS_EMPTY_NO_MATCH))
    }

    // ── A phone that has not been given any songs yet ────────────────────

    @Test
    fun anEmptyLocalLibraryOffersAWayToFillIt() = runComposeUiTest {
        // Not an error — the tab says how to fix it, so it is a different
        // state again from a desktop that returned nothing.
        showSongs(songs = emptyList(), showsLocalLibrary = true)

        assertTrue(exists(UiTags.SONGS_EMPTY_LOCAL_LIBRARY))
    }

    @Test
    fun anEmptyLocalLibraryIsNotReportedAsAnEmptyCatalogue() = runComposeUiTest {
        showSongs(songs = emptyList(), showsLocalLibrary = true)

        assertFalse(exists(UiTags.SONGS_EMPTY_NO_SONGS))
    }

    @Test
    fun anEmptyLocalLibraryWithAFilterOnJustSaysNoMatch() = runComposeUiTest {
        // Offering "copy songs from a computer" when the library is full and
        // the filter is narrow would be the wrong advice.
        showSongs(songs = emptyList(), showsLocalLibrary = true, hasActiveFilter = true)

        assertTrue(exists(UiTags.SONGS_EMPTY_NO_MATCH))
        assertFalse(exists(UiTags.SONGS_EMPTY_LOCAL_LIBRARY))
    }

    // ── While the first fetch is still in flight ─────────────────────────

    @Test
    fun nothingIsSaidWhileTheListIsStillLoading() = runComposeUiTest {
        // "No songs" during the first fetch is wrong and reads as a failure.
        showSongs(songs = emptyList(), isLoading = true)

        assertFalse(exists(UiTags.SONGS_EMPTY_NO_SONGS))
        assertFalse(exists(UiTags.SONGS_EMPTY_NO_MATCH))
    }

    @Test
    fun everyEmptyStateIsGoneOnceSongsArrive() = runComposeUiTest {
        showSongs()

        assertFalse(exists(UiTags.SONGS_EMPTY_NO_MATCH))
        assertFalse(exists(UiTags.SONGS_EMPTY_NO_SONGS))
        assertFalse(exists(UiTags.SONGS_EMPTY_LOCAL_LIBRARY))
    }
}
