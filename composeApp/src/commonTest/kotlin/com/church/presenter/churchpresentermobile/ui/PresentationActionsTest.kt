package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.WsMessageType
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The presentations screen's buttons — the FAB stack and the file picker.
 *
 * Cast on this screen resumes the deck that is already selected rather than
 * choosing one, so with nothing selected it must do nothing at all. The upload
 * button, as on the pictures screen, exists in a live form and an inert one:
 * when the desktop has uploads switched off, the picker is never composed, so
 * no timing accident can put the OS document sheet in front of the operator.
 */
@OptIn(ExperimentalTestApi::class)
class PresentationActionsTest {

    private fun file(name: String = "Sermon.pptx") =
        PickedFile(bytes = byteArrayOf(1, 2, 3), fileName = name)

    // ── The stack that is offered ────────────────────────────────────────

    @Test
    fun castIsOffered() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.FAB_CAST) }
    }

    @Test
    fun addToScheduleIsOffered() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.FAB_ADD_TO_SCHEDULE) }
    }

    @Test
    fun noMultiSelectIsOffered() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.FAB_CAST) }
        assertFalse(exists(UiTags.FAB_SELECT))
    }

    @Test
    fun noHoldIsOffered() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }

        click(UiTags.presentationSlide("sermon", 0))

        awaitThat { vm.isProjecting.value }
        assertFalse(exists(UiTags.FAB_HOLD))
    }

    @Test
    fun noClearDisplayIsOffered() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }

        click(UiTags.presentationSlide("sermon", 0))

        awaitThat { vm.isProjecting.value }
        assertFalse(exists(UiTags.FAB_CLEAR_DISPLAY))
    }

    // ── Cast ─────────────────────────────────────────────────────────────

    @Test
    fun castingWithNothingSelectedSendsNothing() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        assertTrue(desktop.actions.isEmpty(), "sent ${desktop.actions}")
    }

    @Test
    fun castingWithNothingSelectedDoesNotClaimToProject() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        assertFalse(vm.isProjecting.value)
    }

    @Test
    fun castingWhileProjectingClearsTheDisplay() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { vm.isProjecting.value }

        click(UiTags.FAB_CAST)

        awaitThat { desktop.actions.contains(WsMessageType.CLEAR) }
    }

    @Test
    fun clearingStopsProjecting() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { vm.isProjecting.value }

        click(UiTags.FAB_CAST)

        awaitThat { !vm.isProjecting.value }
    }

    @Test
    fun clearingKeepsTheSlideSoItCanComeBack() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 2)) }
        click(UiTags.presentationSlide("sermon", 2))
        awaitThat { vm.isProjecting.value }

        click(UiTags.FAB_CAST)

        awaitThat { !vm.isProjecting.value }
        assertEquals(2, vm.selectedSlideIndex.value)
    }

    @Test
    fun castingAfterClearingResumesTheSameSlide() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 2)) }
        click(UiTags.presentationSlide("sermon", 2))
        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_SLIDE).size == 1 }
        click(UiTags.FAB_CAST)
        awaitThat { !vm.isProjecting.value }

        click(UiTags.FAB_CAST)

        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_SLIDE).size == 2 }
        assertTrue(desktop.payloadsOf(WsMessageType.SELECT_SLIDE).last().contains("\"index\":2"))
    }

    @Test
    fun castingAfterClearingProjectsAgain() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { vm.isProjecting.value }
        click(UiTags.FAB_CAST)
        awaitThat { !vm.isProjecting.value }

        click(UiTags.FAB_CAST)

        awaitThat { vm.isProjecting.value }
    }

    // ── Add to schedule ──────────────────────────────────────────────────

    @Test
    fun addingToTheScheduleReachesTheDesktop() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { vm.selectedPresentation.value != null }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { desktop.actions.contains(WsMessageType.ADD_TO_SCHEDULE) }
    }

    @Test
    fun theScheduleEntryNamesTheDeck() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("notices", 0)) }
        click(UiTags.presentationSlide("notices", 0))
        awaitThat { vm.selectedPresentation.value != null }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { desktop.payloadsOf(WsMessageType.ADD_TO_SCHEDULE).isNotEmpty() }
        assertTrue(
            desktop.payloadsOf(WsMessageType.ADD_TO_SCHEDULE).first().contains("Notices.pdf"),
            "the running order should name the deck the operator added",
        )
    }

    @Test
    fun addingToTheScheduleWithNothingSelectedSendsNothing() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.FAB_ADD_TO_SCHEDULE) }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        assertFalse(desktop.actions.contains(WsMessageType.ADD_TO_SCHEDULE))
    }

    @Test
    fun aScheduledDeckRefreshesTheRunningOrder() = runComposeUiTest {
        var refreshes = 0
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, onScheduleRefresh = { refreshes++ })
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { vm.selectedPresentation.value != null }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { refreshes == 1 }
    }

    @Test
    fun nothingRefreshesTheRunningOrderOnOpen() = runComposeUiTest {
        var refreshes = 0
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, onScheduleRefresh = { refreshes++ })

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        assertEquals(0, refreshes)
    }

    @Test
    fun addingToTheScheduleDoesNotAlsoMoveTheDeck() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_SLIDE).size == 1 }

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { desktop.actions.contains(WsMessageType.ADD_TO_SCHEDULE) }
        assertEquals(1, desktop.payloadsOf(WsMessageType.SELECT_SLIDE).size)
    }

    @Test
    fun choosingAnotherSlideForgetsItWasScheduled() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        click(UiTags.FAB_ADD_TO_SCHEDULE)
        awaitThat { vm.scheduleAdded.value }

        click(UiTags.presentationSlide("sermon", 1))

        awaitThat { !vm.scheduleAdded.value }
    }

    @Test
    fun aRefusedScheduleAddIsReported() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        desktop.sender.failWith(IllegalStateException("no route to desktop"))
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { desktop.actions.contains(WsMessageType.ADD_TO_SCHEDULE) }
        assertFalse(vm.scheduleAdded.value)
    }

    // ── Uploading a deck ─────────────────────────────────────────────────

    @Test
    fun aDesktopThatAcceptsUploadsOffersThePicker() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, canUploadFiles = true)

        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }
    }

    @Test
    fun aDesktopThatAcceptsUploadsShowsNoBlockedButton() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, canUploadFiles = true)

        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }
        assertFalse(exists(UiTags.PRESENTATION_UPLOAD_BLOCKED))
    }

    @Test
    fun aDesktopWithUploadsOffShowsTheBlockedButton() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, canUploadFiles = false)

        awaitThat { exists(UiTags.PRESENTATION_UPLOAD_BLOCKED) }
    }

    @Test
    fun aDesktopWithUploadsOffNeverComposesThePicker() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, canUploadFiles = false)

        awaitThat { exists(UiTags.PRESENTATION_UPLOAD_BLOCKED) }
        assertFalse(exists(UiTags.PRESENTATION_UPLOAD))
    }

    @Test
    fun theBlockedButtonUploadsNothing() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = false)
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD_BLOCKED) }

        click(UiTags.PRESENTATION_UPLOAD_BLOCKED)

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        assertTrue(desktop.uploads.isEmpty())
    }

    @Test
    fun theBlockedButtonLeavesTheDeckUsable() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, canUploadFiles = false)
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD_BLOCKED) }
        click(UiTags.PRESENTATION_UPLOAD_BLOCKED)

        click(UiTags.presentationSlide("sermon", 1))

        awaitThat { vm.selectedSlideIndex.value == 1 }
    }

    @Test
    fun anUploadInFlightReplacesThePickerWithAProgressSpinner() = runComposeUiTest {
        val desktop = FakeDeckDesktop(uploadDelayMs = 2_000)
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }

        vm.uploadPresentationFile(file())

        awaitThat { exists(UiTags.PRESENTATION_UPLOADING) }
    }

    @Test
    fun anUploadInFlightHidesThePicker() = runComposeUiTest {
        val desktop = FakeDeckDesktop(uploadDelayMs = 2_000)
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }

        vm.uploadPresentationFile(file())

        awaitThat { !exists(UiTags.PRESENTATION_UPLOAD) }
    }

    @Test
    fun anUploadedFileReachesTheDesktop() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }

        vm.uploadPresentationFile(file("Advent.pdf"))

        awaitThat { desktop.uploads.isNotEmpty() }
    }

    @Test
    fun thePickerComesBackWhenTheUploadFinishes() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }

        vm.uploadPresentationFile(file())

        awaitThat { !vm.isUploading.value }
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }
    }

    @Test
    fun theListIsReloadedAfterAnUpload() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = true)
        awaitThat { desktop.listRequests.size == 1 }

        vm.uploadPresentationFile(file())

        awaitThat { desktop.listRequests.size >= 2 }
    }

    @Test
    fun theUploadedDeckIsOnScreenWhenTheUploadFinishes() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }

        vm.uploadPresentationFile(file())

        awaitThat { !vm.isUploading.value }
        assertTrue(exists(UiTags.presentationHeader("sermon")))
    }

    @Test
    fun aRefusedUploadStopsTheSpinner() = runComposeUiTest {
        val desktop = FakeDeckDesktop(uploadStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }

        vm.uploadPresentationFile(file())

        awaitThat { !exists(UiTags.PRESENTATION_UPLOADING) }
    }

    @Test
    fun aRefusedUploadIsReported() = runComposeUiTest {
        val desktop = FakeDeckDesktop(uploadStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.PRESENTATION_UPLOAD) }

        vm.uploadPresentationFile(file())

        awaitThat { vm.toastEvent.value != null || vm.error.value != null }
        assertNotNull(vm.toastEvent.value ?: vm.error.value)
    }

    @Test
    fun aRefusedUploadLeavesTheExistingDecksAlone() = runComposeUiTest {
        // Clearing the list is part of the success path; a failed upload must
        // not take the operator's decks away with it.
        val desktop = FakeDeckDesktop(uploadStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPresentations(vm, canUploadFiles = true)
        awaitThat { exists(UiTags.presentationHeader("sermon")) }

        vm.uploadPresentationFile(file())

        awaitThat { !vm.isUploading.value }
        assertTrue(exists(UiTags.presentationHeader("sermon")))
    }
}
