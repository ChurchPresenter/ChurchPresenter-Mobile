package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the pictures grid's selection and schedule state, driven through demo
 * mode so no request is made — the same approach `SongsViewModelTest` takes.
 *
 * Assertions are on invariants rather than particular demo filenames, so
 * editing the canned folder does not break the suite.
 */
class PicturesViewModelTest {

    private fun demoVm(): PicturesViewModel =
        PicturesViewModel(AppSettings(InMemorySettingsStorage()), FakeWsSender(), isDemoMode = true)

    private fun firstImage() = DemoData.picturesFolder.allImages.first()

    // ── Loading ──────────────────────────────────────────────────────────

    @Test
    fun `demo mode serves a folder without a request`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()

            assertEquals(DemoData.picturesFolder, vm.folder.value)
            assertTrue(vm.folder.value!!.allImages.isNotEmpty())
            assertFalse(vm.isLoading.value, "canned content needs no spinner")
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `nothing is selected or projecting before anything is tapped`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()

            assertNull(vm.selectedImage.value)
            assertFalse(vm.isProjecting.value)
            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Selecting ────────────────────────────────────────────────────────

    @Test
    fun `selecting a picture marks it selected and projecting`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val image = firstImage()

            vm.selectPicture(image)
            advanceUntilIdle()

            assertEquals(image, vm.selectedImage.value)
            assertTrue(vm.isProjecting.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `selecting a new picture drops the previous add-to-schedule confirmation`() = runVmTest {
        // Otherwise the tick stays on next to a picture that was never added.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.selectPicture(firstImage())
            vm.addToSchedule()
            advanceUntilIdle()
            assertTrue(vm.scheduleAdded.value)

            vm.selectPicture(firstImage())

            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Clearing the display ─────────────────────────────────────────────

    @Test
    fun `clearing the display stops projecting but keeps the selection`() = runVmTest {
        // The selection is kept deliberately, so Cast can re-project the same
        // image and Add-to-Schedule still has a target.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val image = firstImage()
            vm.selectPicture(image)
            advanceUntilIdle()

            vm.clearDisplay()
            advanceUntilIdle()

            assertFalse(vm.isProjecting.value)
            assertEquals(image, vm.selectedImage.value)
            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Add to schedule ──────────────────────────────────────────────────

    @Test
    fun `adding to schedule confirms and asks the drawer to reload`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val before = vm.scheduleRefreshTrigger.value
            vm.selectPicture(firstImage())

            vm.addToSchedule()
            advanceUntilIdle()

            assertTrue(vm.scheduleAdded.value)
            assertEquals(before + 1, vm.scheduleRefreshTrigger.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding with nothing selected does nothing`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val before = vm.scheduleRefreshTrigger.value

            vm.addToSchedule()
            advanceUntilIdle()

            assertFalse(vm.scheduleAdded.value)
            assertEquals(before, vm.scheduleRefreshTrigger.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Navigating in from the schedule drawer ───────────────────────────

    @Test
    fun `navigating to an image loads the folder before asking to scroll`() = runVmTest {
        // The order matters: the screen's LaunchedEffect reads the folder when the
        // scroll index appears, so setting the index first projected the wrong image.
        val vm = demoVm()
        try {
            vm.navigateTo(folderId = "demo", imageIndex = 2)
            advanceUntilIdle()

            assertNotNull(vm.folder.value)
            assertEquals(2, vm.pendingScrollIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a handled scroll fires only once`() = runVmTest {
        val vm = demoVm()
        try {
            vm.navigateTo(folderId = "demo", imageIndex = 1)
            advanceUntilIdle()

            vm.onPendingScrollHandled()

            assertNull(vm.pendingScrollIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Uploading ────────────────────────────────────────────────────────

    @Test
    fun `demo mode uploads nothing`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()

            vm.uploadDevicePhotos(emptyList())
            advanceUntilIdle()

            assertFalse(vm.isUploading.value)
            assertNull(vm.uploadProgress.value)
            assertEquals(0, vm.uploadPhotoTotal.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Folder shape ─────────────────────────────────────────────────────

    @Test
    fun `the demo folder reports a consistent image count`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val folder = vm.folder.value!!

            assertEquals(folder.allImages.size, folder.totalImages)
            assertTrue(folder.displayName.isNotBlank())
        } finally {
            tearDown(vm)
        }
    }
}
