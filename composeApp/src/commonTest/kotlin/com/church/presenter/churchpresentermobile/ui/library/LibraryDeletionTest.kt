package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Removing something from this device's library.
 *
 * Deleting is the one action here that cannot be undone — the song is not on a
 * computer somewhere unless it was copied from one — so it asks first, and the
 * answer has to reach the right item. A confirmation that deletes the row below
 * the one tapped is the failure worth catching, and a "no" that deletes anyway
 * is worse.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryDeletionTest {

    private fun notice(id: String, title: String, body: String = "Coffee in the hall") =
        LocalAnnouncement(id = id, title = title, body = body)

    private fun songs() = listOf(amazingGrace(), song("s2", "43", "How Great"))

    // ── Asking first ─────────────────────────────────────────────────────

    @Test
    fun aSongRowOffersDelete() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.rowDelete("s1")))
    }

    @Test
    fun aNoticeRowOffersDelete() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        assertTrue(exists(LibraryTags.rowDelete("n1")))
    }

    @Test
    fun noConfirmationIsOpenToBeginWith() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertFalse(exists(LibraryTags.DELETE_CONFIRM))
    }

    @Test
    fun deletingAsksBeforeDoingAnything() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.rowDelete("s1"))

        assertTrue(exists(LibraryTags.DELETE_CONFIRM))
    }

    @Test
    fun theQuestionOffersAWayOut() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.rowDelete("s1"))

        assertTrue(exists(LibraryTags.DELETE_DISMISS))
    }

    @Test
    fun askingRemovesNothingYet() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo)

        click(LibraryTags.rowDelete("s1"))

        assertEquals(1, repo.library.value.songs.size)
    }

    @Test
    fun theRowIsStillThereWhileTheQuestionIsOpen() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.rowDelete("s1"))

        assertTrue(exists(LibraryTags.row("s1")))
    }

    // ── Saying no ────────────────────────────────────────────────────────

    @Test
    fun backingOutKeepsTheSong() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_DISMISS)

        assertEquals(1, repo.library.value.songs.size)
    }

    @Test
    fun backingOutClosesTheQuestion() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_DISMISS)

        assertFalse(exists(LibraryTags.DELETE_CONFIRM))
    }

    @Test
    fun backingOutLeavesTheRowUsable() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_DISMISS)

        click(LibraryTags.rowDelete("s1"))

        assertTrue(exists(LibraryTags.DELETE_CONFIRM))
    }

    @Test
    fun backingOutKeepsANotice() = runComposeUiTest {
        val repo = libraryOf(notices = listOf(notice("n1", "Welcome")))
        showLibrary(repo)
        click(LibraryTags.rowDelete("n1"))

        click(LibraryTags.DELETE_DISMISS)

        assertEquals(1, repo.library.value.announcements.size)
    }

    // ── Saying yes ───────────────────────────────────────────────────────

    @Test
    fun confirmingRemovesTheSong() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.isEmpty() }
    }

    @Test
    fun confirmingTakesTheRowOffScreen() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { !exists(LibraryTags.row("s1")) }
    }

    @Test
    fun confirmingClosesTheQuestion() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { !exists(LibraryTags.DELETE_CONFIRM) }
    }

    @Test
    fun confirmingRemovesOnlyTheSongThatWasAskedAbout() = runComposeUiTest {
        // Two rows on screen and the confirmation carrying the wrong id is a
        // deletion the operator cannot undo.
        val repo = libraryOf(songs = songs())
        showLibrary(repo)
        click(LibraryTags.rowDelete("s2"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.size == 1 }
        assertEquals("s1", repo.library.value.songs.first().id)
    }

    @Test
    fun theOtherRowSurvives() = runComposeUiTest {
        val repo = libraryOf(songs = songs())
        showLibrary(repo)
        click(LibraryTags.rowDelete("s2"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { !exists(LibraryTags.row("s2")) }
        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun confirmingRemovesANotice() = runComposeUiTest {
        val repo = libraryOf(notices = listOf(notice("n1", "Welcome")))
        showLibrary(repo)
        click(LibraryTags.rowDelete("n1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.announcements.isEmpty() }
    }

    @Test
    fun deletingANoticeLeavesTheSongsAlone() = runComposeUiTest {
        // Songs and notices are deleted through different calls; the row has to
        // pick the right one.
        val repo = libraryOf(
            songs = listOf(amazingGrace()),
            notices = listOf(notice("n1", "Welcome")),
        )
        showLibrary(repo)
        click(LibraryTags.rowDelete("n1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.announcements.isEmpty() }
        assertEquals(1, repo.library.value.songs.size)
    }

    @Test
    fun deletingASongLeavesTheNoticesAlone() = runComposeUiTest {
        val repo = libraryOf(
            songs = listOf(amazingGrace()),
            notices = listOf(notice("n1", "Welcome")),
        )
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.isEmpty() }
        assertEquals(1, repo.library.value.announcements.size)
    }

    @Test
    fun deletingTheLastSongLeavesTheEmptyLibraryHint() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { exists(LibraryTags.EMPTY_LIBRARY) }
    }

    @Test
    fun deletingTheLastSongDropsItsHeading() = runComposeUiTest {
        val repo = libraryOf(
            songs = listOf(amazingGrace()),
            notices = listOf(notice("n1", "Welcome")),
        )
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { !exists(LibraryTags.SONGS_HEADING) }
        assertTrue(exists(LibraryTags.NOTICES_HEADING))
    }

    @Test
    fun aSecondDeletionAsksAgain() = runComposeUiTest {
        // The question is not a one-time thing; every deletion is asked about.
        val repo = libraryOf(songs = songs())
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)
        awaitThat { repo.library.value.songs.size == 1 }

        click(LibraryTags.rowDelete("s2"))

        assertTrue(exists(LibraryTags.DELETE_CONFIRM))
    }

    @Test
    fun bothSongsCanBeDeletedInTurn() = runComposeUiTest {
        val repo = libraryOf(songs = songs())
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)
        awaitThat { repo.library.value.songs.size == 1 }

        click(LibraryTags.rowDelete("s2"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.isEmpty() }
    }

    @Test
    fun deletingSurvivesAFilteredList() = runComposeUiTest {
        val repo = libraryOf(
            songs = listOf(amazingGrace()),
            notices = listOf(notice("n1", "Welcome")),
        )
        showLibrary(repo)
        click(LibraryTags.filter(1))

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.isEmpty() }
    }

    @Test
    fun deletingSurvivesASearch() = runComposeUiTest {
        val repo = libraryOf(songs = songs())
        showLibrary(repo)
        type(LibraryTags.SEARCH, "Amazing")

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.size == 1 }
        assertEquals("s2", repo.library.value.songs.first().id)
    }

    // ── Where a row came from ────────────────────────────────────────────

    @Test
    fun aSongCopiedFromAComputerIsStillDeletable() = runComposeUiTest {
        // It can be copied again; what it must not do is refuse silently.
        val repo = libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.DESKTOP)))
        showLibrary(repo)

        assertTrue(exists(LibraryTags.rowDelete("s1")))
    }

    @Test
    fun aCopiedSongCanBeRemoved() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.DESKTOP)))
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.isEmpty() }
    }

    @Test
    fun aSongWrittenOnThisPhoneIsDeletable() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.LOCAL)))
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.isEmpty() }
    }

    @Test
    fun anEditedCopyOfADesktopSongIsDeletable() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.LOCAL_OVERRIDE)))
        showLibrary(repo)
        click(LibraryTags.rowDelete("s1"))

        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.isEmpty() }
    }
}
