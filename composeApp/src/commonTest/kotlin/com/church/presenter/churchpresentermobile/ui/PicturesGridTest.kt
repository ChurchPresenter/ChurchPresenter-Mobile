package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.WsMessageType
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The pictures grid — the folder the desktop has open, mirrored on the phone.
 *
 * Tapping a tile puts that picture on the screen in the room, so the tests that
 * matter are about *which* tile: that the tap carries the image the operator
 * touched, that the highlight follows it, and that a folder the desktop never
 * opened produces an empty screen rather than a stale one.
 */
@OptIn(ExperimentalTestApi::class)
class PicturesGridTest {

    // ── The grid ─────────────────────────────────────────────────────────

    @Test
    fun everyImageInTheFolderGetsATile() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertTrue(exists(UiTags.pictureCell(1)))
        assertTrue(exists(UiTags.pictureCell(2)))
    }

    @Test
    fun noTileIsInventedForAnImageTheDesktopDidNotSend() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertFalse(exists(UiTags.pictureCell(3)))
    }

    @Test
    fun theFolderIsNamed() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.PICTURES_FOLDER) }
    }

    @Test
    fun theFolderNameIsTheOneTheDesktopSent() = runComposeUiTest {
        val vm = FakePictureDesktop(folder = picturesFolder(name = "Advent 2025")).viewModel()
        showPictures(vm)

        awaitThat { isShowing("ADVENT 2025") }
    }

    @Test
    fun aFolderWithNoNameStillGetsAHeader() = runComposeUiTest {
        val vm = FakePictureDesktop(folder = picturesFolder(name = null)).viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.PICTURES_FOLDER) }
    }

    @Test
    fun theFolderHeaderCountsTheImages() = runComposeUiTest {
        val vm = FakePictureDesktop(
            folder = picturesFolder(images = List(5) { picture(it) }),
        ).viewModel()
        showPictures(vm)

        awaitThat { isShowing("5") }
    }

    @Test
    fun theCountIsTheDesktopsTotalNotJustWhatWasSent() = runComposeUiTest {
        // The desktop pages long folders: 12 tiles of a folder of 240 must not
        // read as a folder of 12.
        val vm = FakePictureDesktop(
            folder = picturesFolder(images = List(3) { picture(it) }, total = 240),
        ).viewModel()
        showPictures(vm)

        awaitThat { isShowing("240") }
    }

    @Test
    fun aTileIsShownForAnImageWithNoFileName() = runComposeUiTest {
        // Index is all the desktop guarantees; a nameless image is still an image.
        val vm = FakePictureDesktop(
            folder = picturesFolder(images = listOf(picture(0, fileName = null))),
        ).viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
    }

    // ── Nothing to show ──────────────────────────────────────────────────

    @Test
    fun aDesktopWithNoFolderOpenSaysSo() = runComposeUiTest {
        val vm = FakePictureDesktop(folder = null).viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.PICTURES_EMPTY) }
    }

    @Test
    fun anEmptyFolderSaysSo() = runComposeUiTest {
        val vm = FakePictureDesktop(folder = picturesFolder(images = emptyList())).viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.PICTURES_EMPTY) }
    }

    @Test
    fun anEmptyFolderShowsNoHeader() = runComposeUiTest {
        val vm = FakePictureDesktop(folder = picturesFolder(images = emptyList())).viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.PICTURES_EMPTY) }
        assertFalse(exists(UiTags.PICTURES_FOLDER))
    }

    @Test
    fun aFolderWithImagesShowsNoEmptyState() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertFalse(exists(UiTags.PICTURES_EMPTY))
    }

    // ── When the desktop refuses ─────────────────────────────────────────

    @Test
    fun aRefusedFolderRequestIsReported() = runComposeUiTest {
        val vm = FakePictureDesktop(folderStatus = HttpStatusCode.InternalServerError).viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.PICTURES_ERROR) }
    }

    @Test
    fun aRefusedFolderRequestOffersARetry() = runComposeUiTest {
        val vm = FakePictureDesktop(folderStatus = HttpStatusCode.InternalServerError).viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.PICTURES_RETRY) }
    }

    @Test
    fun retryingAsksTheDesktopAgain() = runComposeUiTest {
        val desktop = FakePictureDesktop(folderStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.PICTURES_RETRY) }
        val before = desktop.folderRequests.size

        click(UiTags.PICTURES_RETRY)

        awaitThat { desktop.folderRequests.size > before }
    }

    @Test
    fun aWorkingFolderShowsNoErrorBanner() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertFalse(exists(UiTags.PICTURES_ERROR))
    }

    @Test
    fun aWorkingFolderOffersNoRetry() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertFalse(exists(UiTags.PICTURES_RETRY))
    }

    @Test
    fun aRecoveredRetryClearsTheBanner() = runComposeUiTest {
        val desktop = FakePictureDesktop(folderStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.PICTURES_ERROR) }

        desktop.folderStatus = HttpStatusCode.OK
        click(UiTags.PICTURES_RETRY)

        awaitThat { !exists(UiTags.PICTURES_ERROR) }
    }

    @Test
    fun aRecoveredRetryFillsTheGrid() = runComposeUiTest {
        val desktop = FakePictureDesktop(folderStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.PICTURES_ERROR) }

        desktop.folderStatus = HttpStatusCode.OK
        click(UiTags.PICTURES_RETRY)

        awaitThat { exists(UiTags.pictureCell(0)) }
    }

    // ── Tapping a tile ───────────────────────────────────────────────────

    @Test
    fun tappingATileSelectsThatImage() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(1)) }

        click(UiTags.pictureCell(1))

        awaitThat { vm.selectedImage.value?.index == 1 }
    }

    @Test
    fun tappingTheThirdTileSelectsTheThirdImage() = runComposeUiTest {
        // Three tiles on screen and the callback carrying the first one's index
        // is a real bug that "the grid rendered" never sees.
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(2)) }

        click(UiTags.pictureCell(2))

        awaitThat { vm.selectedImage.value?.fileName == "photo-2.jpg" }
    }

    @Test
    fun nothingIsSelectedBeforeATap() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertNull(vm.selectedImage.value)
    }

    @Test
    fun noTileIsHighlightedBeforeATap() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
        tagged(UiTags.pictureCell(0)).assertIsNotSelected()
    }

    @Test
    fun theTappedTileIsHighlighted() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(1)) }

        click(UiTags.pictureCell(1))

        awaitThat { vm.selectedImage.value != null }
        tagged(UiTags.pictureCell(1)).assertIsSelected()
    }

    @Test
    fun onlyTheTappedTileIsHighlighted() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(1)) }

        click(UiTags.pictureCell(1))

        awaitThat { vm.selectedImage.value != null }
        tagged(UiTags.pictureCell(0)).assertIsNotSelected()
        tagged(UiTags.pictureCell(2)).assertIsNotSelected()
    }

    @Test
    fun theHighlightMovesToTheNextTapped() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }
        click(UiTags.pictureCell(0))
        awaitThat { vm.selectedImage.value?.index == 0 }

        click(UiTags.pictureCell(2))

        awaitThat { vm.selectedImage.value?.index == 2 }
        tagged(UiTags.pictureCell(0)).assertIsNotSelected()
        tagged(UiTags.pictureCell(2)).assertIsSelected()
    }

    @Test
    fun tappingATileSendsItToTheDesktop() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }

        click(UiTags.pictureCell(0))

        awaitThat { desktop.actions.contains(WsMessageType.SELECT_PICTURE) }
    }

    @Test
    fun theSelectionNamesTheFolderItCameFrom() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }

        click(UiTags.pictureCell(0))

        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_PICTURE).isNotEmpty() }
        assertTrue(
            desktop.payloadsOf(WsMessageType.SELECT_PICTURE).first().contains("sunday-morning"),
            "the folder id should reach the desktop",
        )
    }

    @Test
    fun tappingATileStartsProjecting() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }

        click(UiTags.pictureCell(0))

        awaitThat { vm.isProjecting.value }
    }

    @Test
    fun nothingIsProjectingWhenTheScreenOpens() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertFalse(vm.isProjecting.value)
    }

    @Test
    fun aFolderWithoutAnIdProjectsNothing() = runComposeUiTest {
        // Without a folder id the desktop cannot resolve the file, so the tap
        // must not claim to be projecting.
        val vm = FakePictureDesktop(folder = picturesFolder(id = null)).viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }

        click(UiTags.pictureCell(0))

        awaitThat { vm.selectedImage.value != null }
        assertFalse(vm.isProjecting.value)
    }

    @Test
    fun aFolderWithoutAnIdSendsNothingToTheDesktop() = runComposeUiTest {
        val desktop = FakePictureDesktop(folder = picturesFolder(id = null))
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }

        click(UiTags.pictureCell(0))

        awaitThat { vm.selectedImage.value != null }
        assertFalse(desktop.actions.contains(WsMessageType.SELECT_PICTURE))
    }

    @Test
    fun reTappingTheSameTileProjectsItAgain() = runComposeUiTest {
        // Re-tapping is how an operator recovers a display someone cleared on
        // the desktop; it must not be a dead zone.
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }
        click(UiTags.pictureCell(0))
        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_PICTURE).size == 1 }

        click(UiTags.pictureCell(0))

        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_PICTURE).size == 2 }
    }

    // ── Reloading ────────────────────────────────────────────────────────

    @Test
    fun theFolderIsRequestedOnOpen() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)

        awaitThat { desktop.folderRequests.isNotEmpty() }
    }

    @Test
    fun savingSettingsReloadsTheFolder() = runComposeUiTest {
        // The address may have just changed; what is on screen came from the
        // old one.
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, settingsSaveToken = 1)

        awaitThat { desktop.folderRequests.size >= 2 }
    }

    @Test
    fun openingWithoutASettingsSaveRequestsTheFolderOnce() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, settingsSaveToken = 0)

        awaitThat { desktop.folderRequests.isNotEmpty() }
        assertEquals(1, desktop.folderRequests.size)
    }

    // ── Arriving from the schedule ───────────────────────────────────────

    @Test
    fun arrivingFromTheScheduleOpensTheNamedFolder() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, pendingNavFolderId = "advent")

        awaitThat { desktop.folderRequests.any { it.endsWith("/advent") } }
    }

    @Test
    fun arrivingFromTheScheduleIsReportedHandled() = runComposeUiTest {
        var handled = 0
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, pendingNavFolderId = "advent", onPendingNavHandled = { handled++ })

        awaitThat { handled == 1 }
    }

    @Test
    fun openingNormallyReportsNoPendingNavigation() = runComposeUiTest {
        var handled = 0
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertEquals(0, handled)
    }

    @Test
    fun arrivingFromTheScheduleSelectsTheNamedImage() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, pendingNavFolderId = "advent", pendingNavImageIndex = 2)

        awaitThat { vm.selectedImage.value?.index == 2 }
    }

    @Test
    fun arrivingFromTheScheduleProjectsTheNamedImage() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, pendingNavFolderId = "advent", pendingNavImageIndex = 1)

        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_PICTURE).isNotEmpty() }
    }

    @Test
    fun aScheduleArrivalIsActedOnOnlyOnce() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, pendingNavFolderId = "advent", pendingNavImageIndex = 1)

        awaitThat { vm.selectedImage.value != null }
        awaitThat { vm.pendingScrollIndex.value == null }
    }

    @Test
    fun anImageIndexTheFolderDoesNotHaveSelectsNothing() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, pendingNavFolderId = "advent", pendingNavImageIndex = 99)

        awaitThat { vm.pendingScrollIndex.value == null }
        assertNull(vm.selectedImage.value)
    }
}
