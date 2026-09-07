package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The library's filter, its headings, and the two ways it can be empty.
 *
 * "Nothing here yet" and "nothing matches what you typed" are different
 * problems with different fixes — copy some songs across, or clear the search —
 * and showing the wrong one sends the operator to the wrong place. The filter
 * has the same trap: All, Songs and Notices each hide something, and a heading
 * left behind for a section with nothing under it reads as content that has
 * gone missing.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryFiltersTest {

    private fun notice(id: String, title: String, body: String = "Coffee in the hall") =
        LocalAnnouncement(id = id, title = title, body = body)

    // ── An empty library ─────────────────────────────────────────────────

    @Test
    fun anEmptyLibraryExplainsHowToFillIt() = runComposeUiTest {
        showLibrary(libraryOf())

        assertTrue(exists(LibraryTags.EMPTY_LIBRARY))
    }

    @Test
    fun anEmptyLibraryIsNotShownAsAFruitlessSearch() = runComposeUiTest {
        showLibrary(libraryOf())

        assertFalse(exists(LibraryTags.NO_RESULTS))
    }

    @Test
    fun aLibraryWithSongsShowsNoEmptyHint() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertFalse(exists(LibraryTags.EMPTY_LIBRARY))
    }

    @Test
    fun aLibraryWithOnlyNoticesShowsNoEmptyHint() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        assertFalse(exists(LibraryTags.EMPTY_LIBRARY))
    }

    @Test
    fun anEmptyLibraryStillOffersTheSearchBox() = runComposeUiTest {
        showLibrary(libraryOf())

        assertTrue(exists(LibraryTags.SEARCH))
    }

    @Test
    fun anEmptyLibraryStillOffersTheChips() = runComposeUiTest {
        // They are how content gets onto the phone in the first place.
        showLibrary(libraryOf())

        assertTrue(exists(LibraryTags.SYNC_CHIP))
        assertTrue(exists(LibraryTags.SHARE_CHIP))
    }

    @Test
    fun anEmptyLibraryStillOffersTheAddButton() = runComposeUiTest {
        showLibrary(libraryOf())

        assertTrue(exists(LibraryTags.ADD))
    }

    // ── A search that matches nothing ────────────────────────────────────

    @Test
    fun aSearchThatMatchesNothingSaysSo() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        type(LibraryTags.SEARCH, "zzzzz")

        assertTrue(exists(LibraryTags.NO_RESULTS))
    }

    @Test
    fun aFruitlessSearchIsNotShownAsAnEmptyLibrary() = runComposeUiTest {
        // "Copy songs from your computer" would be the wrong advice — they are
        // already there.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        type(LibraryTags.SEARCH, "zzzzz")

        assertFalse(exists(LibraryTags.EMPTY_LIBRARY))
    }

    @Test
    fun clearingTheSearchBringsTheLibraryBack() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        type(LibraryTags.SEARCH, "zzzzz")

        type(LibraryTags.SEARCH, "")

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun aSearchThatMatchesKeepsTheRow() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        type(LibraryTags.SEARCH, "Amazing")

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun aSearchMatchesANoticeToo() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        type(LibraryTags.SEARCH, "Welcome")

        assertTrue(exists(LibraryTags.row("n1")))
    }

    @Test
    fun aSearchHidesWhatDoesNotMatch() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(amazingGrace()),
                notices = listOf(notice("n1", "Welcome")),
            )
        )

        type(LibraryTags.SEARCH, "Amazing")

        assertTrue(exists(LibraryTags.row("s1")))
        assertFalse(exists(LibraryTags.row("n1")))
    }

    // ── The three filters ────────────────────────────────────────────────

    @Test
    fun allThreeFiltersAreOffered() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.filter(0)))
        assertTrue(exists(LibraryTags.filter(1)))
        assertTrue(exists(LibraryTags.filter(2)))
    }

    @Test
    fun everythingIsShownToBeginWith() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        tagged(LibraryTags.filter(0)).assertIsSelected()
    }

    @Test
    fun bothKindsAreListedUnderAll() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(amazingGrace()),
                notices = listOf(notice("n1", "Welcome")),
            )
        )

        assertTrue(exists(LibraryTags.row("s1")))
        assertTrue(exists(LibraryTags.row("n1")))
    }

    @Test
    fun filteringToSongsHidesTheNotices() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(amazingGrace()),
                notices = listOf(notice("n1", "Welcome")),
            )
        )

        click(LibraryTags.filter(1))

        assertTrue(exists(LibraryTags.row("s1")))
        assertFalse(exists(LibraryTags.row("n1")))
    }

    @Test
    fun filteringToNoticesHidesTheSongs() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(amazingGrace()),
                notices = listOf(notice("n1", "Welcome")),
            )
        )

        click(LibraryTags.filter(2))

        assertTrue(exists(LibraryTags.row("n1")))
        assertFalse(exists(LibraryTags.row("s1")))
    }

    @Test
    fun theChosenFilterIsTheSelectedOne() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.filter(1))

        tagged(LibraryTags.filter(1)).assertIsSelected()
        tagged(LibraryTags.filter(0)).assertIsNotSelected()
    }

    @Test
    fun goingBackToAllShowsEverythingAgain() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(amazingGrace()),
                notices = listOf(notice("n1", "Welcome")),
            )
        )
        click(LibraryTags.filter(1))

        click(LibraryTags.filter(0))

        assertTrue(exists(LibraryTags.row("n1")))
    }

    @Test
    fun filteringToNoticesWithNoneSaysNothingMatches() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.filter(2))

        assertTrue(exists(LibraryTags.NO_RESULTS))
    }

    @Test
    fun filteringToSongsWithNoneSaysNothingMatches() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        click(LibraryTags.filter(1))

        assertTrue(exists(LibraryTags.NO_RESULTS))
    }

    @Test
    fun anEmptyFilterIsNotShownAsAnEmptyLibrary() = runComposeUiTest {
        // The songs are still there; only this filter is hiding them.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.filter(2))

        assertFalse(exists(LibraryTags.EMPTY_LIBRARY))
    }

    @Test
    fun theFilterAndTheSearchNarrowTogether() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(amazingGrace(), song("s2", "43", "How Great")),
                notices = listOf(notice("n1", "Amazing notice")),
            )
        )

        click(LibraryTags.filter(1))
        type(LibraryTags.SEARCH, "Amazing")

        assertTrue(exists(LibraryTags.row("s1")))
        assertFalse(exists(LibraryTags.row("s2")))
        assertFalse(exists(LibraryTags.row("n1")))
    }

    // ── Section headings ─────────────────────────────────────────────────

    @Test
    fun songsAreListedUnderTheirOwnHeading() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.SONGS_HEADING))
    }

    @Test
    fun noticesAreListedUnderTheirOwnHeading() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        assertTrue(exists(LibraryTags.NOTICES_HEADING))
    }

    @Test
    fun aLibraryWithNoNoticesShowsNoNoticesHeading() = runComposeUiTest {
        // A heading with nothing under it reads as content that has gone missing.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertFalse(exists(LibraryTags.NOTICES_HEADING))
    }

    @Test
    fun aLibraryWithNoSongsShowsNoSongsHeading() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        assertFalse(exists(LibraryTags.SONGS_HEADING))
    }

    @Test
    fun filteringToNoticesDropsTheSongsHeading() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(amazingGrace()),
                notices = listOf(notice("n1", "Welcome")),
            )
        )

        click(LibraryTags.filter(2))

        assertFalse(exists(LibraryTags.SONGS_HEADING))
        assertTrue(exists(LibraryTags.NOTICES_HEADING))
    }

    @Test
    fun bothHeadingsAreShownWhenBothKindsAreThere() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(amazingGrace()),
                notices = listOf(notice("n1", "Welcome")),
            )
        )

        assertTrue(exists(LibraryTags.SONGS_HEADING))
        assertTrue(exists(LibraryTags.NOTICES_HEADING))
    }

    @Test
    fun aSearchThatMatchesNothingDropsBothHeadings() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(amazingGrace()),
                notices = listOf(notice("n1", "Welcome")),
            )
        )

        type(LibraryTags.SEARCH, "zzzzz")

        assertFalse(exists(LibraryTags.SONGS_HEADING))
        assertFalse(exists(LibraryTags.NOTICES_HEADING))
    }
}
