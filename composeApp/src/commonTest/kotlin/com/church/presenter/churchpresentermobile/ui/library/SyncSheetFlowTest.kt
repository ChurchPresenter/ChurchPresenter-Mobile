package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.UiTags
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The copy sheet as a whole: an address, then a choice of what to take.
 *
 * One sheet with two halves rather than two sheets, because both need the same
 * computer address — and getting that address wrong is the single most common
 * reason a copy does nothing at all. So the address block belongs above both
 * halves, stays put while the operator switches between them, and what is typed
 * into it survives the switch.
 */
@OptIn(ExperimentalTestApi::class)
class SyncSheetFlowTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    // ── The address block ────────────────────────────────────────────────

    @Test
    fun theSheetOpensOnTheAddress() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(UiTags.ADDRESS_HOST) }
    }

    @Test
    fun theSheetOffersAPortAlongsideTheHost() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(UiTags.ADDRESS_PORT) }
    }

    @Test
    fun theAddressIsRememberedAsItIsTyped() = runComposeUiTest {
        val settings = settings()
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settings)
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(UiTags.ADDRESS_HOST) }

        type(UiTags.ADDRESS_HOST, "192.168.1.50")

        awaitThat { settings.host == "192.168.1.50" }
    }

    @Test
    fun thePortIsRememberedAsItIsTyped() = runComposeUiTest {
        val settings = settings()
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settings)
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(UiTags.ADDRESS_PORT) }

        type(UiTags.ADDRESS_PORT, "9000")

        awaitThat { settings.port == 9000 }
    }

    @Test
    fun anAddressAlreadySetIsShownRatherThanBlank() = runComposeUiTest {
        val settings = settings()
        settings.host = "192.168.1.50"
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settings)

        click(LibraryTags.SYNC_CHIP)

        awaitThat { isShowing("192.168.1.50") }
    }

    @Test
    fun aNonsenseHostIsRefusedRatherThanStored() = runComposeUiTest {
        // Saving "my computer" as a host makes every later copy fail with a
        // network error that says nothing about the cause.
        val settings = settings()
        settings.host = "192.168.1.50"
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settings)
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(UiTags.ADDRESS_HOST) }

        type(UiTags.ADDRESS_HOST, "not a host")

        awaitThat { settings.host == "192.168.1.50" }
    }

    @Test
    fun aNonsensePortIsRefusedRatherThanStored() = runComposeUiTest {
        val settings = settings()
        settings.port = 8765
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settings)
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(UiTags.ADDRESS_PORT) }

        type(UiTags.ADDRESS_PORT, "99999")

        awaitThat { settings.port == 8765 }
    }

    @Test
    fun theAddressBlockSitsAboveBothHalves() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(UiTags.ADDRESS_HOST) }

        click(LibraryTags.syncSection(1))

        awaitThat { exists(UiTags.ADDRESS_HOST) }
    }

    @Test
    fun anAddressTypedOnOneHalfSurvivesTheSwitch() = runComposeUiTest {
        val settings = settings()
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settings)
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(UiTags.ADDRESS_HOST) }
        type(UiTags.ADDRESS_HOST, "192.168.1.50")

        click(LibraryTags.syncSection(1))

        awaitThat { isShowing("192.168.1.50") }
    }

    // ── The two halves ───────────────────────────────────────────────────

    @Test
    fun theSheetOpensOnTheSongsHalf() = runComposeUiTest {
        // Songs are what most people are here for.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun theSongsHalfIsMarkedAsTheOneOnShow() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.syncSection(0)) }
        tagged(LibraryTags.syncSection(0)).assertIsSelected()
    }

    @Test
    fun theBibleHalfCanBeReachedFromTheSongsHalf() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(LibraryTags.SYNC_BUTTON) }

        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun switchingToTheBibleHalfTakesTheSongsHalfAway() = runComposeUiTest {
        // Two "copy" buttons on one sheet is an invitation to press the wrong one.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(LibraryTags.SYNC_BUTTON) }

        click(LibraryTags.syncSection(1))

        awaitThat { !exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun theSongsHalfCanBeReachedBack() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.SYNC_CHIP)
        click(LibraryTags.syncSection(1))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }

        click(LibraryTags.syncSection(0))

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun theBibleHalfIsMarkedWhenItIsOnShow() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.SYNC_CHIP)

        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
        tagged(LibraryTags.syncSection(1)).assertIsSelected()
    }

    @Test
    fun switchingHalvesTwiceEndsWhereItStarted() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.SYNC_CHIP)

        click(LibraryTags.syncSection(1))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
        click(LibraryTags.syncSection(0))

        awaitThat { exists(LibraryTags.syncScope(0)) }
    }

    @Test
    fun theScopeChoiceIsOnTheSongsHalf() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.syncScope(0)) }
    }

    @Test
    fun theBibleHalfHasNoSongbookScope() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.SYNC_CHIP)

        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
        assertFalse(exists(LibraryTags.syncScope(0)))
    }

    // ── Getting to the sheet, and out of it ──────────────────────────────

    @Test
    fun theCopyChipOpensTheSheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun theEmptyLibraryHintIsShownWithNothingToList() = runComposeUiTest {
        // It is the only place that says how content gets onto the phone.
        showLibrary(libraryOf())

        assertTrue(exists(LibraryTags.EMPTY_LIBRARY))
    }

    @Test
    fun theBibleMenuOffersACopy() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())), bibles = biblesWith("King James Version"))

        click(LibraryTags.BIBLE_CHIP)

        awaitThat { exists(LibraryTags.BIBLE_MENU_COPY) }
    }

    @Test
    fun theBibleMenusCopyOpensTheBibleHalf() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())), bibles = biblesWith("King James Version"))
        click(LibraryTags.BIBLE_CHIP)
        awaitThat { exists(LibraryTags.BIBLE_MENU_COPY) }

        click(LibraryTags.BIBLE_MENU_COPY)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun theBibleMenusCopyClosesTheMenu() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())), bibles = biblesWith("King James Version"))
        click(LibraryTags.BIBLE_CHIP)
        awaitThat { exists(LibraryTags.BIBLE_MENU_COPY) }

        click(LibraryTags.BIBLE_MENU_COPY)

        awaitThat { !exists(LibraryTags.BIBLE_MENU_COPY) }
    }

    @Test
    fun theSheetKeepsTheListBehindIt() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun theShareChipOpensTheOtherSheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SHARE_CHIP)

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
    }

    @Test
    fun theShareSheetIsNotTheCopySheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SHARE_CHIP)

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
        assertFalse(exists(LibraryTags.SYNC_BUTTON))
    }

    @Test
    fun theCopySheetIsNotTheShareSheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
        assertFalse(exists(LibraryTags.SHARE_EXPORT))
    }

    // ── Adding by hand instead ───────────────────────────────────────────

    @Test
    fun theAddButtonOffersBothKinds() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        click(LibraryTags.ADD)

        assertTrue(exists(LibraryTags.ADD_SONG))
        assertTrue(exists(LibraryTags.ADD_NOTICE))
    }

    @Test
    fun choosingASongOpensAnEmptyEditor() = runComposeUiTest {
        var edited: String? = "not called"
        showLibrary(libraryOf(songs = listOf(amazingGrace())), onEditSong = { edited = it })
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_SONG)

        assertTrue(edited == null)
    }

    @Test
    fun choosingANoticeOpensAnEmptyEditor() = runComposeUiTest {
        var edited: String? = "not called"
        showLibrary(libraryOf(songs = listOf(amazingGrace())), onEditAnnouncement = { edited = it })
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_NOTICE)

        assertTrue(edited == null)
    }

    @Test
    fun choosingASongClosesTheChoice() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_SONG)

        awaitThat { !exists(LibraryTags.ADD_SONG) }
    }

    @Test
    fun openingTheChoiceOpensNoEditorByItself() = runComposeUiTest {
        // Asking which kind is not a decision; nothing should have happened yet.
        var songs = 0
        var notices = 0
        showLibrary(
            libraryOf(songs = listOf(amazingGrace())),
            onEditSong = { songs++ },
            onEditAnnouncement = { notices++ },
        )

        click(LibraryTags.ADD)

        awaitThat { exists(LibraryTags.ADD_SONG) }
        assertTrue(songs == 0 && notices == 0)
    }

    @Test
    fun theChoiceCanBeOpenedAgainAfterChoosing() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.ADD)
        click(LibraryTags.ADD_NOTICE)
        awaitThat { !exists(LibraryTags.ADD_SONG) }

        click(LibraryTags.ADD)

        awaitThat { exists(LibraryTags.ADD_SONG) }
    }

    @Test
    fun theListIsStillThereAfterChoosing() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.ADD)

        click(LibraryTags.ADD_NOTICE)

        awaitThat { exists(LibraryTags.row("s1")) }
    }
}
