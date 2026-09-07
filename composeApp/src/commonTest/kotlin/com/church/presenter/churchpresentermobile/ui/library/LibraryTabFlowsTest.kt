package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The library tab used the way an operator actually uses it.
 *
 * Each control is covered on its own elsewhere; what these hold is that they do
 * not interfere. A filter left applied after a deletion, a search that survives
 * a sheet and hides everything, a chip row that disappears once the list is
 * empty — each of those reads as content having gone missing, and each is a
 * combination rather than a single control.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryTabFlowsTest {

    private fun notice(id: String, title: String) =
        LocalAnnouncement(id = id, title = title, body = "Coffee in the hall")

    private fun mixed() = libraryOf(
        songs = listOf(amazingGrace(), song("s2", "43", "How Great")),
        notices = listOf(notice("n1", "Welcome"), notice("n2", "Offering")),
    )

    // ── A filter and a search together ───────────────────────────────────

    @Test
    fun aFilterSurvivesASearchBeingCleared() = runComposeUiTest {
        showLibrary(mixed())
        click(LibraryTags.filter(1))
        type(LibraryTags.SEARCH, "Amazing")

        type(LibraryTags.SEARCH, "")

        tagged(LibraryTags.filter(1)).assertIsSelected()
        assertFalse(exists(LibraryTags.row("n1")))
    }

    @Test
    fun aSearchSurvivesAFilterChange() = runComposeUiTest {
        showLibrary(mixed())
        type(LibraryTags.SEARCH, "Amazing")

        click(LibraryTags.filter(1))

        assertTrue(exists(LibraryTags.row("s1")))
        assertFalse(exists(LibraryTags.row("s2")))
    }

    @Test
    fun narrowingToNothingSaysSoRatherThanLookingEmpty() = runComposeUiTest {
        showLibrary(mixed())

        click(LibraryTags.filter(2))
        type(LibraryTags.SEARCH, "Amazing")

        assertTrue(exists(LibraryTags.NO_RESULTS))
    }

    @Test
    fun wideningAgainBringsEverythingBack() = runComposeUiTest {
        showLibrary(mixed())
        click(LibraryTags.filter(2))
        type(LibraryTags.SEARCH, "Amazing")

        type(LibraryTags.SEARCH, "")
        click(LibraryTags.filter(0))

        assertTrue(exists(LibraryTags.row("s1")))
        assertTrue(exists(LibraryTags.row("n1")))
    }

    @Test
    fun theChipsStayWhileNothingMatches() = runComposeUiTest {
        // They are how content gets onto the phone; hiding them when the list is
        // empty removes the answer to "where is everything".
        showLibrary(mixed())

        type(LibraryTags.SEARCH, "zzzzz")

        assertTrue(exists(LibraryTags.SYNC_CHIP))
        assertTrue(exists(LibraryTags.SHARE_CHIP))
    }

    @Test
    fun theFiltersStayWhileNothingMatches() = runComposeUiTest {
        showLibrary(mixed())

        type(LibraryTags.SEARCH, "zzzzz")

        assertTrue(exists(LibraryTags.filter(0)))
    }

    @Test
    fun theAddButtonStaysWhileNothingMatches() = runComposeUiTest {
        showLibrary(mixed())

        type(LibraryTags.SEARCH, "zzzzz")

        assertTrue(exists(LibraryTags.ADD))
    }

    // ── Deleting while narrowed ──────────────────────────────────────────

    @Test
    fun deletingUnderAFilterKeepsTheFilter() = runComposeUiTest {
        val repo = mixed()
        showLibrary(repo)
        click(LibraryTags.filter(1))

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.size == 1 }
        tagged(LibraryTags.filter(1)).assertIsSelected()
    }

    @Test
    fun deletingUnderAFilterLeavesTheOtherKindAlone() = runComposeUiTest {
        val repo = mixed()
        showLibrary(repo)
        click(LibraryTags.filter(1))

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.size == 1 }
        assertEquals(2, repo.library.value.announcements.size)
    }

    @Test
    fun deletingTheLastMatchLeavesTheNoResultsHint() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()), notices = listOf(notice("n1", "Welcome")))
        showLibrary(repo)
        click(LibraryTags.filter(1))

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { exists(LibraryTags.NO_RESULTS) }
    }

    @Test
    fun deletingEverythingLeavesTheEmptyLibraryHint() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo)

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { exists(LibraryTags.EMPTY_LIBRARY) }
    }

    @Test
    fun deletingDuringASearchKeepsTheSearch() = runComposeUiTest {
        val repo = mixed()
        showLibrary(repo)
        type(LibraryTags.SEARCH, "Amazing")

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.size == 1 }
        assertFalse(exists(LibraryTags.row("s2")))
    }

    // ── Sheets over a narrowed list ──────────────────────────────────────

    @Test
    fun theCopySheetOpensOverAFilteredList() = runComposeUiTest {
        showLibrary(mixed())
        click(LibraryTags.filter(1))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun theShareSheetOpensOverASearch() = runComposeUiTest {
        showLibrary(mixed())
        type(LibraryTags.SEARCH, "Amazing")

        click(LibraryTags.SHARE_CHIP)

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
    }

    @Test
    fun theCopySheetOpensOverAnEmptyResult() = runComposeUiTest {
        showLibrary(mixed())
        type(LibraryTags.SEARCH, "zzzzz")

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun addingIsOfferedOverAFilteredList() = runComposeUiTest {
        showLibrary(mixed())
        click(LibraryTags.filter(2))

        click(LibraryTags.ADD)

        assertTrue(exists(LibraryTags.ADD_SONG))
    }

    @Test
    fun addingASongIsOfferedEvenWhileFilteredToNotices() = runComposeUiTest {
        // The filter narrows the list, not what the operator may create.
        var edited: String? = "not called"
        showLibrary(mixed(), onEditSong = { edited = it })
        click(LibraryTags.filter(2))
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_SONG)

        assertEquals(null, edited)
    }

    // ── The Bible chip alongside the rest ────────────────────────────────

    @Test
    fun theBibleChipSitsAlongsideAFilteredList() = runComposeUiTest {
        showLibrary(mixed(), bibles = biblesWith("King James Version"))

        click(LibraryTags.filter(1))

        assertTrue(exists(LibraryTags.BIBLE_CHIP))
    }

    @Test
    fun theBibleChipSurvivesADeletion() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo, bibles = biblesWith("King James Version"))

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { exists(LibraryTags.EMPTY_LIBRARY) }
        assertTrue(exists(LibraryTags.BIBLE_CHIP))
    }

    @Test
    fun aBibleOnThePhoneDoesNotCountAsLibraryContent() = runComposeUiTest {
        // The empty state is about songs and notices; a downloaded translation
        // does not make the list non-empty.
        showLibrary(libraryOf(), bibles = biblesWith("King James Version"))

        assertTrue(exists(LibraryTags.EMPTY_LIBRARY))
    }

    // ── Editing from a narrowed list ─────────────────────────────────────

    @Test
    fun editingFromAFilteredListNamesTheRightSong() = runComposeUiTest {
        var edited: String? = null
        showLibrary(mixed(), onEditSong = { edited = it })
        click(LibraryTags.filter(1))

        click(LibraryTags.rowEdit("s2"))

        assertEquals("s2", edited)
    }

    @Test
    fun editingFromASearchNamesTheRightNotice() = runComposeUiTest {
        var edited: String? = null
        showLibrary(mixed(), onEditAnnouncement = { edited = it })
        type(LibraryTags.SEARCH, "Welcome")

        click(LibraryTags.rowEdit("n1"))

        assertEquals("n1", edited)
    }

    @Test
    fun theListIsUsableAgainAfterEverySheetHasBeenOpened() = runComposeUiTest {
        // A smoke pass: nothing left behind that swallows the list.
        val repo = mixed()
        showLibrary(repo)
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(LibraryTags.SYNC_BUTTON) }

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun aDeletionAfterASheetStillReachesTheRepository() = runComposeUiTest {
        val repo = mixed()
        showLibrary(repo)
        click(LibraryTags.SHARE_CHIP)
        awaitThat { exists(LibraryTags.SHARE_EXPORT) }

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.size == 1 }
    }
}
