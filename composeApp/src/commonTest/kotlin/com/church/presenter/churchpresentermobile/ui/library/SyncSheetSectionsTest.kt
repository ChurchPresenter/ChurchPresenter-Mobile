package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.ui.UiTags
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * One copy sheet with two halves.
 *
 * Two sections rather than two sheets, because both need the same address: an
 * operator who has just fixed it for songs should not have to fix it again for
 * Bibles. That is the thing worth holding — the address stays put across the
 * switch, and each half brings its own controls without leaving the other's
 * behind.
 */
@OptIn(ExperimentalTestApi::class)
class SyncSheetSectionsTest {

    /** Opens the library and gets into the copy sheet. */
    private fun androidx.compose.ui.test.ComposeUiTest.openSyncSheet() {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))
        click(LibraryTags.SYNC_CHIP)
        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    // ── Which half opens ─────────────────────────────────────────────────

    @Test
    fun bothHalvesAreOffered() = runComposeUiTest {
        openSyncSheet()

        assertTrue(exists(LibraryTags.syncSection(0)))
        assertTrue(exists(LibraryTags.syncSection(1)))
    }

    @Test
    fun theSongsHalfOpensFirstFromTheSyncChip() = runComposeUiTest {
        openSyncSheet()

        tagged(LibraryTags.syncSection(0)).assertIsSelected()
    }

    @Test
    fun theBibleHalfIsNotSelectedFirst() = runComposeUiTest {
        openSyncSheet()

        tagged(LibraryTags.syncSection(1)).assertIsNotSelected()
    }

    @Test
    fun theSongsHalfBringsItsOwnControls() = runComposeUiTest {
        openSyncSheet()

        assertTrue(exists(LibraryTags.syncScope(0)))
    }

    @Test
    fun theSongsHalfDoesNotBringTheBibleControls() = runComposeUiTest {
        openSyncSheet()

        assertFalse(exists(LibraryTags.BIBLE_SYNC_FIND))
    }

    @Test
    fun theBibleHalfCarriesItsOwnControls() = runComposeUiTest {
        openSyncSheet()

        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
        tagged(LibraryTags.syncSection(1)).assertIsSelected()
    }

    // ── Switching between them ───────────────────────────────────────────

    @Test
    fun switchingToTheBibleHalfShowsItsControls() = runComposeUiTest {
        openSyncSheet()

        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun switchingToTheBibleHalfPutsTheSongControlsAway() = runComposeUiTest {
        openSyncSheet()

        click(LibraryTags.syncSection(1))

        awaitThat { !exists(LibraryTags.syncScope(0)) }
    }

    @Test
    fun switchingMarksTheNewHalfSelected() = runComposeUiTest {
        openSyncSheet()

        click(LibraryTags.syncSection(1))

        tagged(LibraryTags.syncSection(1)).assertIsSelected()
        tagged(LibraryTags.syncSection(0)).assertIsNotSelected()
    }

    @Test
    fun switchingBackBringsTheSongControlsReturn() = runComposeUiTest {
        openSyncSheet()
        click(LibraryTags.syncSection(1))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }

        click(LibraryTags.syncSection(0))

        awaitThat { exists(LibraryTags.syncScope(0)) }
    }

    @Test
    fun switchingBackPutsTheBibleControlsAway() = runComposeUiTest {
        openSyncSheet()
        click(LibraryTags.syncSection(1))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }

        click(LibraryTags.syncSection(0))

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    // ── The address both halves share ────────────────────────────────────

    @Test
    fun theAddressIsOfferedAboveTheChoice() = runComposeUiTest {
        openSyncSheet()

        assertTrue(exists(UiTags.ADDRESS_HOST))
        assertTrue(exists(UiTags.ADDRESS_PORT))
    }

    @Test
    fun theAddressStaysOnTheBibleHalf() = runComposeUiTest {
        // Fixing it once has to be enough for both.
        openSyncSheet()

        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
        assertTrue(exists(UiTags.ADDRESS_HOST))
    }

    @Test
    fun theAddressKeepsWhatWasTypedAcrossASwitch() = runComposeUiTest {
        openSyncSheet()
        type(UiTags.ADDRESS_HOST, "10.0.0.9")

        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
        assertTrue(isShowing("10.0.0.9"))
    }

    @Test
    fun theApiKeyIsOfferedOnlyWhenItIsNeeded() = runComposeUiTest {
        // Most churches run without one; the field appears when a key is saved
        // or when a copy comes back 401.
        openSyncSheet()

        assertFalse(exists(UiTags.ADDRESS_API_KEY))
        assertTrue(exists(UiTags.ADDRESS_REVEAL_KEY))
    }

    @Test
    fun theApiKeyFieldCanBeAskedFor() = runComposeUiTest {
        openSyncSheet()

        click(UiTags.ADDRESS_REVEAL_KEY)

        awaitThat { exists(UiTags.ADDRESS_API_KEY) }
    }

    @Test
    fun theSheetSurvivesSwitchingBackAndForth() = runComposeUiTest {
        openSyncSheet()

        click(LibraryTags.syncSection(1))
        click(LibraryTags.syncSection(0))
        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun theCopyButtonBelongsToTheSongsHalfOnly() = runComposeUiTest {
        openSyncSheet()

        click(LibraryTags.syncSection(1))

        awaitThat { !exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun theBibleHalfHasItsOwnButton() = runComposeUiTest {
        openSyncSheet()

        click(LibraryTags.syncSection(1))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun theSongsHalfStillOffersItsScopeAfterASwitch() = runComposeUiTest {
        openSyncSheet()
        click(LibraryTags.syncSection(1))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }

        click(LibraryTags.syncSection(0))

        awaitThat { exists(LibraryTags.syncScope(1)) }
    }

    @Test
    fun theSongsHalfComesBackWhole() = runComposeUiTest {
        // Switching to Bibles and back must leave the songs half usable rather
        // than half-built. The book picker is not part of this: with no computer
        // answering, the list it asks for never arrives.
        openSyncSheet()

        click(LibraryTags.syncSection(1))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
        click(LibraryTags.syncSection(0))

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
        assertTrue(exists(LibraryTags.syncScope(0)))
    }
}
