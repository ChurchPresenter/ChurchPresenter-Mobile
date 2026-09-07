package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the Library tab lists, and what searching does to it.
 *
 * A search that hides a song the operator can no longer find costs work that
 * exists nowhere else — the library never leaves the device unless someone
 * exports it.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryListingTest {

    @Test
    fun everySongInTheLibraryIsListed() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"), song("s2", "7", "Be Thou My Vision"))))

        assertTrue(exists(LibraryTags.row("s1")))
        assertTrue(exists(LibraryTags.row("s2")))
    }

    @Test
    fun aSongIsListedUnderItsNumberAndTitle() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"))))

        assertTrue(isShowing("42 Amazing Grace"))
    }

    @Test
    fun aNoticeIsListedAlongsideTheSongs() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(song("s1", "42", "Amazing Grace")),
                notices = listOf(LocalAnnouncement(id = "a1", title = "Bring a dish")),
            )
        )

        assertTrue(exists(LibraryTags.row("s1")))
        assertTrue(exists(LibraryTags.row("a1")))
    }

    @Test
    fun anEmptyLibraryListsNothing() = runComposeUiTest {
        showLibrary(libraryOf())

        assertFalse(exists(LibraryTags.row("s1")))
    }

    @Test
    fun searchingNarrowsTheListToWhatMatches() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"), song("s2", "7", "Be Thou My Vision"))))

        type(LibraryTags.SEARCH, "Amazing")

        assertTrue(exists(LibraryTags.row("s1")))
        assertFalse(exists(LibraryTags.row("s2")))
    }

    @Test
    fun searchingByNumberFindsTheSong() = runComposeUiTest {
        // How an operator with a hymnal in front of them looks a song up.
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"), song("s2", "7", "Be Thou My Vision"))))

        type(LibraryTags.SEARCH, "42")

        assertTrue(exists(LibraryTags.row("s1")))
        assertFalse(exists(LibraryTags.row("s2")))
    }

    @Test
    fun searchIgnoresCase() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"))))

        type(LibraryTags.SEARCH, "amazing")

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun clearingTheSearchBringsEverythingBack() = runComposeUiTest {
        // The failure that costs work: a stale filter leaving a song the
        // operator then believes they never wrote.
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"), song("s2", "7", "Be Thou My Vision"))))
        type(LibraryTags.SEARCH, "Amazing")

        type(LibraryTags.SEARCH, "")

        assertTrue(exists(LibraryTags.row("s1")))
        assertTrue(exists(LibraryTags.row("s2")))
    }

    @Test
    fun aSearchThatMatchesNothingListsNothing() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"))))

        type(LibraryTags.SEARCH, "zzzzz")

        assertFalse(exists(LibraryTags.row("s1")))
    }

    @Test
    fun searchingLeavesNoticesOutWhenTheyDoNotMatch() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(song("s1", "42", "Amazing Grace")),
                notices = listOf(LocalAnnouncement(id = "a1", title = "Bring a dish")),
            )
        )

        type(LibraryTags.SEARCH, "Amazing")

        assertTrue(exists(LibraryTags.row("s1")))
        assertFalse(exists(LibraryTags.row("a1")))
    }
}
