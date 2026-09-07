package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Editing and deleting from the Library tab.
 *
 * This is the one screen where content can be lost: the library is the only
 * copy, so a delete that skips its confirmation destroys work a church typed in
 * by hand.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryRowActionsTest {

    @Test
    fun eachRowOffersEditAndDelete() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"))))

        assertTrue(exists(LibraryTags.rowEdit("s1")))
        assertTrue(exists(LibraryTags.rowDelete("s1")))
    }

    @Test
    fun editingASongReportsWhichOne() = runComposeUiTest {
        var edited: String? = null
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"))), onEditSong = { edited = it })

        tagged(LibraryTags.rowEdit("s1")).performClick()

        assertEquals("s1", edited)
    }

    @Test
    fun editingReportsTheRowThatWasTapped() = runComposeUiTest {
        // With two songs listed, the callback has to carry the one beside the
        // button rather than the first in the list.
        var edited: String? = null
        showLibrary(
            libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"), song("s2", "7", "Be Thou My Vision"))),
            onEditSong = { edited = it },
        )

        tagged(LibraryTags.rowEdit("s2")).performClick()

        assertEquals("s2", edited)
    }

    @Test
    fun editingANoticeReportsItSeparately() = runComposeUiTest {
        // Songs and notices open different editors; crossing them would open a
        // song editor on a notice's id and show a blank song.
        var editedNotice: String? = null
        var editedSong: String? = null
        showLibrary(
            libraryOf(
                songs = listOf(song("s1", "42", "Amazing Grace")),
                notices = listOf(LocalAnnouncement(id = "a1", title = "Bring a dish")),
            ),
            onEditSong = { editedSong = it },
            onEditAnnouncement = { editedNotice = it },
        )

        tagged(LibraryTags.rowEdit("a1")).performClick()

        assertEquals("a1", editedNotice)
        assertNull(editedSong)
    }

    @Test
    fun deletingAsksBeforeItRemovesAnything() = runComposeUiTest {
        // The library is the only copy. One stray tap must not be enough.
        val repository = libraryOf(songs = listOf(song("s1", "42", "Amazing Grace")))
        showLibrary(repository)

        tagged(LibraryTags.rowDelete("s1")).performClick()

        assertNotNull(repository.song("s1"), "the song went without a confirmation")
        assertTrue(exists(LibraryTags.DELETE_CONFIRM), "no confirmation appeared")
    }

    @Test
    fun confirmingTheDeleteRemovesTheSong() = runComposeUiTest {
        val repository = libraryOf(songs = listOf(song("s1", "42", "Amazing Grace")))
        showLibrary(repository)
        tagged(LibraryTags.rowDelete("s1")).performClick()

        tagged(LibraryTags.DELETE_CONFIRM).performClick()

        assertNull(repository.song("s1"))
        assertFalse(exists(LibraryTags.row("s1")))
    }

    @Test
    fun dismissingTheConfirmationKeepsTheSong() = runComposeUiTest {
        // The way out of a mistap, and the reason the dialog exists at all.
        val repository = libraryOf(songs = listOf(song("s1", "42", "Amazing Grace")))
        showLibrary(repository)
        tagged(LibraryTags.rowDelete("s1")).performClick()

        tagged(LibraryTags.DELETE_DISMISS).performClick()

        assertNotNull(repository.song("s1"))
        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun deletingOneSongLeavesTheOthers() = runComposeUiTest {
        val repository = libraryOf(
            songs = listOf(song("s1", "42", "Amazing Grace"), song("s2", "7", "Be Thou My Vision")),
        )
        showLibrary(repository)

        tagged(LibraryTags.rowDelete("s1")).performClick()
        tagged(LibraryTags.DELETE_CONFIRM).performClick()

        assertNull(repository.song("s1"))
        assertNotNull(repository.song("s2"))
        assertTrue(exists(LibraryTags.row("s2")))
    }

    @Test
    fun deletingANoticeRemovesTheNoticeAndNotASong() = runComposeUiTest {
        val repository = libraryOf(
            songs = listOf(song("s1", "42", "Amazing Grace")),
            notices = listOf(LocalAnnouncement(id = "a1", title = "Bring a dish")),
        )
        showLibrary(repository)

        tagged(LibraryTags.rowDelete("a1")).performClick()
        tagged(LibraryTags.DELETE_CONFIRM).performClick()

        assertNull(repository.announcement("a1"))
        assertNotNull(repository.song("s1"))
    }

    @Test
    fun aRowDoesNotProjectWhenTapped() = runComposeUiTest {
        // Browsing your own library used to put the song straight on the
        // audience screen. Going live belongs to the Songs tab.
        val sender = FakeWsSender()
        showLibrary(libraryOf(songs = listOf(song("s1", "42", "Amazing Grace"))), sender = sender)

        tagged(LibraryTags.row("s1")).performClick()

        assertTrue(sender.calls.isEmpty(), "tapping a row sent ${sender.calls}")
    }
}
