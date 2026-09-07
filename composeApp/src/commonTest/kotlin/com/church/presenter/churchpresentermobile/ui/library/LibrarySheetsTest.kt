package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The library's chips, its add button, and the sheets they open.
 *
 * Three of these lead somewhere the operator cannot get back from by accident —
 * a sync sheet, a share sheet, an editor — so what matters is that each opens
 * the one it promises, and that opening one does not quietly do the work as
 * well. Adding is the sharpest case: the button offers a choice, and picking
 * "song" must open a song editor rather than a notice.
 */
@OptIn(ExperimentalTestApi::class)
class LibrarySheetsTest {

    private fun notice(id: String, title: String, body: String = "Coffee in the hall") =
        LocalAnnouncement(id = id, title = title, body = body)

    // ── The chips above the list ─────────────────────────────────────────

    @Test
    fun theSyncChipIsOffered() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun theShareChipIsOffered() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.SHARE_CHIP))
    }

    @Test
    fun noBibleChipIsOfferedUntilOneIsDownloaded() = runComposeUiTest {
        // An empty picker is not a feature, and the Bible tab already says how
        // to get a translation onto the phone.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertFalse(exists(LibraryTags.BIBLE_CHIP))
    }

    @Test
    fun noSyncSheetIsOpenToBeginWith() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertFalse(exists(LibraryTags.SYNC_BUTTON))
    }

    @Test
    fun theSyncChipOpensTheCopySheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun theCopySheetOpensOnTheSongsHalf() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.syncScope(0)) }
    }

    @Test
    fun theCopySheetCarriesTheComputersAddress() = runComposeUiTest {
        // In standalone there is nowhere else it can be reached, and without it
        // every copy silently targets the default host.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
        assertTrue(exists(com.church.presenter.churchpresentermobile.ui.UiTags.ADDRESS_HOST))
    }

    @Test
    fun theBibleHalfIsStillReachableWithoutTheChip() = runComposeUiTest {
        // With no translation downloaded the chip is absent, so the copy sheet's
        // own section control is the way in.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(LibraryTags.SYNC_BUTTON) }

        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun theShareChipOpensTheShareSheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SHARE_CHIP)

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
    }

    @Test
    fun theShareSheetOffersBothDirections() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SHARE_CHIP)

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
        assertTrue(exists(LibraryTags.SHARE_IMPORT))
    }

    @Test
    fun theShareChipDoesNotOpenTheCopySheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SHARE_CHIP)

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
        assertFalse(exists(LibraryTags.SYNC_BUTTON))
    }

    @Test
    fun theSyncChipDoesNotOpenTheShareSheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
        assertFalse(exists(LibraryTags.SHARE_EXPORT))
    }

    @Test
    fun openingTheCopySheetChangesNothingOnItsOwn() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo)

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
        assertEquals(1, repo.library.value.songs.size)
    }

    @Test
    fun theCopySheetIsReachableFromAnEmptyLibrary() = runComposeUiTest {
        // The empty state's whole job is getting content onto the phone.
        showLibrary(libraryOf())

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    // ── The empty state's own buttons ────────────────────────────────────

    @Test
    fun theEmptyStateOffersToCopyFromAComputer() = runComposeUiTest {
        showLibrary(libraryOf())

        assertTrue(exists(LibraryTags.EMPTY_LIBRARY))
    }

    @Test
    fun theEmptyStateStillOffersTheFilters() = runComposeUiTest {
        showLibrary(libraryOf())

        assertTrue(exists(LibraryTags.filter(0)))
    }

    // ── Adding something new ─────────────────────────────────────────────

    @Test
    fun theAddButtonIsOffered() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.ADD))
    }

    @Test
    fun noAddChoiceIsOpenToBeginWith() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertFalse(exists(LibraryTags.ADD_SONG))
    }

    @Test
    fun addingAsksWhichKind() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.ADD)

        assertTrue(exists(LibraryTags.ADD_SONG))
        assertTrue(exists(LibraryTags.ADD_NOTICE))
    }

    @Test
    fun choosingASongOpensASongEditor() = runComposeUiTest {
        var editedSong: String? = "not called"
        var editedNotice: String? = "not called"
        showLibrary(
            libraryOf(songs = listOf(amazingGrace())),
            onEditSong = { editedSong = it },
            onEditAnnouncement = { editedNotice = it },
        )
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_SONG)

        assertNull(editedSong)
        assertEquals("not called", editedNotice)
    }

    @Test
    fun choosingANoticeOpensANoticeEditor() = runComposeUiTest {
        var editedSong: String? = "not called"
        var editedNotice: String? = "not called"
        showLibrary(
            libraryOf(songs = listOf(amazingGrace())),
            onEditSong = { editedSong = it },
            onEditAnnouncement = { editedNotice = it },
        )
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_NOTICE)

        assertNull(editedNotice)
        assertEquals("not called", editedSong)
    }

    @Test
    fun choosingASongClosesTheChoice() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_SONG)

        assertFalse(exists(LibraryTags.ADD_SONG))
    }

    @Test
    fun choosingANoticeClosesTheChoice() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_NOTICE)

        assertFalse(exists(LibraryTags.ADD_NOTICE))
    }

    @Test
    fun anAddedItemIsWrittenByTheEditorNotTheChoice() = runComposeUiTest {
        // The dialog only routes; nothing lands in the library until the editor
        // saves.
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo)
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_SONG)

        assertEquals(1, repo.library.value.songs.size)
    }

    @Test
    fun addingIsOfferedOnAnEmptyLibrary() = runComposeUiTest {
        showLibrary(libraryOf())

        click(LibraryTags.ADD)

        assertTrue(exists(LibraryTags.ADD_SONG))
    }

    // ── Editing what is already there ────────────────────────────────────

    @Test
    fun aSongRowOffersAnEditButton() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.rowEdit("s1")))
    }

    @Test
    fun editingASongNamesThatSong() = runComposeUiTest {
        var edited: String? = null
        showLibrary(libraryOf(songs = listOf(amazingGrace())), onEditSong = { edited = it })

        click(LibraryTags.rowEdit("s1"))

        assertEquals("s1", edited)
    }

    @Test
    fun editingTheSecondSongNamesTheSecondSong() = runComposeUiTest {
        // Two rows on screen and the callback carrying the first one's id is a
        // real bug that "the button was there" never sees.
        var edited: String? = null
        showLibrary(
            libraryOf(songs = listOf(amazingGrace(), song("s2", "43", "How Great"))),
            onEditSong = { edited = it },
        )

        click(LibraryTags.rowEdit("s2"))

        assertEquals("s2", edited)
    }

    @Test
    fun aNoticeRowOffersAnEditButton() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        assertTrue(exists(LibraryTags.rowEdit("n1")))
    }

    @Test
    fun editingANoticeNamesThatNotice() = runComposeUiTest {
        var edited: String? = null
        showLibrary(
            libraryOf(notices = listOf(notice("n1", "Welcome"))),
            onEditAnnouncement = { edited = it },
        )

        click(LibraryTags.rowEdit("n1"))

        assertEquals("n1", edited)
    }

    @Test
    fun editingANoticeDoesNotOpenASongEditor() = runComposeUiTest {
        var editedSong: String? = "not called"
        showLibrary(
            libraryOf(notices = listOf(notice("n1", "Welcome"))),
            onEditSong = { editedSong = it },
        )

        click(LibraryTags.rowEdit("n1"))

        assertEquals("not called", editedSong)
    }

    @Test
    fun editingChangesNothingByItself() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace()))
        showLibrary(repo)

        click(LibraryTags.rowEdit("s1"))

        assertEquals("Amazing Grace", repo.library.value.songs.first().title)
    }
}
