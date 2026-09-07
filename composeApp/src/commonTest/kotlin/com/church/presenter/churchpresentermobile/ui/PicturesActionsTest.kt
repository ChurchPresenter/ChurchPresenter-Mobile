package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.WsMessageType
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The pictures screen's buttons — the FAB stack and the photo picker.
 *
 * Two of these act on the room: Cast changes what the congregation is looking
 * at, and Add to Schedule changes the running order. Both act on *the current
 * selection*, so the case that matters is the one where there isn't one: a Cast
 * with nothing selected must be a no-op rather than a blank screen mid-service.
 *
 * The upload button exists in two forms — a live picker and an inert one when
 * the desktop has uploads switched off — and the inert one must be unable to
 * present the OS picker at all, not merely be ignored afterwards.
 */
@OptIn(ExperimentalTestApi::class)
class PicturesActionsTest {

    private fun photo(name: String = "from-phone.jpg") =
        PickedPhoto(bytes = byteArrayOf(1, 2, 3), fileName = name)

    // ── The stack that is offered ────────────────────────────────────────

    @Test
    fun castIsOffered() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.FAB_CAST) }
    }

    @Test
    fun addToScheduleIsOffered() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.FAB_ADD_TO_SCHEDULE) }
    }

    @Test
    fun noMultiSelectIsOffered() = runComposeUiTest {
        // Pictures project one at a time; a Select button here would promise a
        // batch the screen cannot send.
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.FAB_CAST) }
        assertFalse(exists(UiTags.FAB_SELECT))
    }

    @Test
    fun noHoldIsOffered() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }

        click(UiTags.pictureCell(0))

        awaitThat { vm.isProjecting.value }
        assertFalse(exists(UiTags.FAB_HOLD))
    }

    @Test
    fun noClearDisplayIsOffered() = runComposeUiTest {
        // Cast doubles as the clear action on this screen.
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }

        click(UiTags.pictureCell(0))

        awaitThat { vm.isProjecting.value }
        assertFalse(exists(UiTags.FAB_CLEAR_DISPLAY))
    }

    @Test
    fun noCastBadgeIsShown() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)

        awaitThat { exists(UiTags.FAB_CAST) }
        assertFalse(exists(UiTags.FAB_CAST_BADGE))
    }

    // ── Cast ─────────────────────────────────────────────────────────────

    @Test
    fun castingWithNothingSelectedSendsNothing() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertTrue(desktop.actions.isEmpty(), "sent ${desktop.actions}")
    }

    @Test
    fun castingWithNothingSelectedDoesNotClaimToProject() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertFalse(vm.isProjecting.value)
    }

    @Test
    fun castingWhileProjectingClearsTheDisplay() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }
        click(UiTags.pictureCell(0))
        awaitThat { vm.isProjecting.value }
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { desktop.actions.contains(WsMessageType.CLEAR) }
    }

    @Test
    fun clearingStopsProjecting() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }
        click(UiTags.pictureCell(0))
        awaitThat { vm.isProjecting.value }
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { !vm.isProjecting.value }
    }

    @Test
    fun clearingKeepsTheSelectionSoItCanComeBack() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(1)) }
        click(UiTags.pictureCell(1))
        awaitThat { vm.isProjecting.value }
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { !vm.isProjecting.value }
        assertEquals(1, vm.selectedImage.value?.index)
    }

    @Test
    fun castingAfterClearingPutsTheSameImageBack() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(1)) }
        click(UiTags.pictureCell(1))
        awaitThat { vm.isProjecting.value }
        click(UiTags.FAB_CAST)
        awaitThat { !vm.isProjecting.value }
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { vm.isProjecting.value }
        assertEquals(1, vm.selectedImage.value?.index)
    }

    @Test
    fun castingAfterClearingTellsTheDesktopAgain() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }
        click(UiTags.pictureCell(0))
        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_PICTURE).size == 1 }
        click(UiTags.FAB_CAST)
        awaitThat { desktop.actions.contains(WsMessageType.CLEAR) }
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_PICTURE).size == 2 }
    }

    // ── Add to schedule ──────────────────────────────────────────────────

    @Test
    fun addingToTheScheduleReachesTheDesktop() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }
        click(UiTags.pictureCell(0))
        awaitThat { vm.selectedImage.value != null }
        awaitThat { exists(UiTags.FAB_ADD_TO_SCHEDULE) }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { desktop.actions.contains(WsMessageType.ADD_TO_SCHEDULE) }
    }

    @Test
    fun theScheduleEntryNamesTheImage() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(2)) }
        click(UiTags.pictureCell(2))
        awaitThat { vm.selectedImage.value != null }
        awaitThat { exists(UiTags.FAB_ADD_TO_SCHEDULE) }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { desktop.payloadsOf(WsMessageType.ADD_TO_SCHEDULE).isNotEmpty() }
        assertTrue(
            desktop.payloadsOf(WsMessageType.ADD_TO_SCHEDULE).first().contains("photo-2.jpg"),
            "the schedule entry should name the picture",
        )
    }

    @Test
    fun aNamelessImageIsScheduledByItsIndex() = runComposeUiTest {
        val desktop = FakePictureDesktop(
            folder = picturesFolder(images = listOf(picture(4, fileName = null))),
        )
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(4)) }
        click(UiTags.pictureCell(4))
        awaitThat { vm.selectedImage.value != null }
        awaitThat { exists(UiTags.FAB_ADD_TO_SCHEDULE) }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { desktop.payloadsOf(WsMessageType.ADD_TO_SCHEDULE).isNotEmpty() }
        assertTrue(
            desktop.payloadsOf(WsMessageType.ADD_TO_SCHEDULE).first().contains("Image 4"),
            "a nameless image still needs a label in the running order",
        )
    }

    @Test
    fun addingToTheScheduleWithNothingSelectedSendsNothing() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.FAB_ADD_TO_SCHEDULE) }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertFalse(desktop.actions.contains(WsMessageType.ADD_TO_SCHEDULE))
    }

    @Test
    fun aScheduledPictureRefreshesTheRunningOrder() = runComposeUiTest {
        var refreshes = 0
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, onScheduleRefresh = { refreshes++ })
        awaitThat { exists(UiTags.pictureCell(0)) }
        click(UiTags.pictureCell(0))
        awaitThat { vm.selectedImage.value != null }
        awaitThat { exists(UiTags.FAB_ADD_TO_SCHEDULE) }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { refreshes == 1 }
    }

    @Test
    fun nothingRefreshesTheRunningOrderOnOpen() = runComposeUiTest {
        var refreshes = 0
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, onScheduleRefresh = { refreshes++ })

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertEquals(0, refreshes)
    }

    @Test
    fun addingToTheScheduleDoesNotAlsoProject() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }
        click(UiTags.pictureCell(0))
        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_PICTURE).size == 1 }
        awaitThat { exists(UiTags.FAB_ADD_TO_SCHEDULE) }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { desktop.actions.contains(WsMessageType.ADD_TO_SCHEDULE) }
        assertEquals(1, desktop.payloadsOf(WsMessageType.SELECT_PICTURE).size)
    }

    @Test
    fun choosingAnotherPictureForgetsItWasScheduled() = runComposeUiTest {
        // The tick belongs to the picture that was added, not to the screen.
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm)
        awaitThat { exists(UiTags.pictureCell(0)) }
        click(UiTags.pictureCell(0))
        click(UiTags.FAB_ADD_TO_SCHEDULE)
        awaitThat { vm.scheduleAdded.value }

        click(UiTags.pictureCell(1))

        awaitThat { !vm.scheduleAdded.value }
    }

    // ── Adding photos from the phone ─────────────────────────────────────

    @Test
    fun aDesktopThatAcceptsUploadsOffersThePicker() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, canUploadFiles = true)

        awaitThat { exists(UiTags.PICTURES_PICK) }
    }

    @Test
    fun aDesktopThatAcceptsUploadsShowsNoBlockedButton() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, canUploadFiles = true)

        awaitThat { exists(UiTags.PICTURES_PICK) }
        assertFalse(exists(UiTags.PICTURES_PICK_BLOCKED))
    }

    @Test
    fun aDesktopWithUploadsOffShowsTheBlockedButton() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, canUploadFiles = false)

        awaitThat { exists(UiTags.PICTURES_PICK_BLOCKED) }
    }

    @Test
    fun aDesktopWithUploadsOffNeverComposesThePicker() = runComposeUiTest {
        // The point of the second button: with no picker composed, no timing
        // accident can present the OS photo sheet.
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, canUploadFiles = false)

        awaitThat { exists(UiTags.PICTURES_PICK_BLOCKED) }
        assertFalse(exists(UiTags.PICTURES_PICK))
    }

    @Test
    fun theBlockedButtonUploadsNothing() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = false)
        awaitThat { exists(UiTags.PICTURES_PICK_BLOCKED) }

        click(UiTags.PICTURES_PICK_BLOCKED)

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertTrue(desktop.uploads.isEmpty())
    }

    @Test
    fun theBlockedButtonStillLeavesTheGridUsable() = runComposeUiTest {
        val vm = FakePictureDesktop().viewModel()
        showPictures(vm, canUploadFiles = false)
        awaitThat { exists(UiTags.PICTURES_PICK_BLOCKED) }
        click(UiTags.PICTURES_PICK_BLOCKED)

        click(UiTags.pictureCell(1))

        awaitThat { vm.selectedImage.value?.index == 1 }
    }

    @Test
    fun anUploadInFlightReplacesThePickerWithAProgressSpinner() = runComposeUiTest {
        val desktop = FakePictureDesktop(uploadDelayMs = 2_000)
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo()))

        awaitThat { exists(UiTags.PICTURES_UPLOADING) }
    }

    @Test
    fun anUploadInFlightHidesThePicker() = runComposeUiTest {
        val desktop = FakePictureDesktop(uploadDelayMs = 2_000)
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo()))

        awaitThat { !exists(UiTags.PICTURES_PICK) }
    }

    @Test
    fun thePickerComesBackWhenTheUploadFinishes() = runComposeUiTest {
        val desktop = FakePictureDesktop(uploadDelayMs = 50)
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo()))

        awaitThat { !vm.isUploading.value }
        awaitThat { exists(UiTags.PICTURES_PICK) }
    }

    @Test
    fun anUploadedPhotoReachesTheDesktop() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo("sunset.jpg")))

        awaitThat { desktop.uploads.isNotEmpty() }
    }

    @Test
    fun anUploadedPhotoGoesStraightOnScreen() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo()))

        awaitThat { desktop.actions.contains(WsMessageType.SELECT_PICTURE) }
    }

    @Test
    fun anUploadedPhotoLeavesTheScreenProjecting() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo()))

        // `isUploading` is only true once the launched upload starts running, so
        // waiting for it to be false says nothing until the upload is under way.
        awaitThat { desktop.uploads.isNotEmpty() }
        awaitThat { !vm.isUploading.value }
        assertTrue(vm.isProjecting.value)
    }

    @Test
    fun aRefusedUploadIsReported() = runComposeUiTest {
        val desktop = FakePictureDesktop(uploadStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo()))

        awaitThat { exists(UiTags.PICTURES_ERROR) }
    }

    @Test
    fun aRefusedUploadStopsTheSpinner() = runComposeUiTest {
        val desktop = FakePictureDesktop(uploadStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo()))

        awaitThat { !exists(UiTags.PICTURES_UPLOADING) }
    }

    @Test
    fun aRefusedUploadProjectsNothing() = runComposeUiTest {
        val desktop = FakePictureDesktop(uploadStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo()))

        awaitThat { !vm.isUploading.value }
        assertFalse(vm.isProjecting.value)
    }

    @Test
    fun anEmptyPickUploadsNothing() = runComposeUiTest {
        // Cancelling the OS picker returns an empty list; nothing should happen.
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(emptyList())

        awaitThat { exists(UiTags.pictureCell(0)) }
        assertTrue(desktop.uploads.isEmpty())
        assertFalse(vm.isUploading.value)
    }

    @Test
    fun everyPickedPhotoIsUploaded() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PICTURES_PICK) }

        vm.uploadDevicePhotos(listOf(photo("a.jpg"), photo("b.jpg"), photo("c.jpg")))

        awaitThat { desktop.uploads.size == 3 }
    }

    @Test
    fun theGridRefreshesToShowWhatWasUploaded() = runComposeUiTest {
        val desktop = FakePictureDesktop()
        val vm = desktop.viewModel()
        showPictures(vm, canUploadFiles = true)
        awaitThat { desktop.folderRequests.size == 1 }

        vm.uploadDevicePhotos(listOf(photo()))

        awaitThat { desktop.folderRequests.size >= 2 }
    }
}
