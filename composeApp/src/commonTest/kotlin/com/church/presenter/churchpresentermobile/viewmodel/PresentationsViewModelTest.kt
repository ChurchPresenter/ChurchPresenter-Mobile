package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the presentations screen's selection and schedule state through demo
 * mode, so no request is made — the approach `SongsViewModelTest` established.
 */
class PresentationsViewModelTest {

    private fun demoVm(): PresentationsViewModel =
        PresentationsViewModel(AppSettings(InMemorySettingsStorage()), FakeWsSender(), isDemoMode = true)

    private fun firstPresentation() = DemoData.presentations.first()

    // ── Loading ──────────────────────────────────────────────────────────

    @Test
    fun `demo mode serves the canned presentations without a request`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()

            assertEquals(DemoData.presentations, vm.presentations.value)
            assertTrue(vm.presentations.value.isNotEmpty())
            assertFalse(vm.isLoading.value)
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

            assertNull(vm.selectedPresentation.value)
            assertNull(vm.selectedSlideIndex.value)
            assertFalse(vm.isProjecting.value)
            assertFalse(vm.scheduleAdded.value)
            assertNull(vm.toastEvent.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Selecting a slide ────────────────────────────────────────────────

    @Test
    fun `selecting a slide records the presentation and the slide index`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val presentation = firstPresentation()

            vm.selectPresentation(presentation, slideIndex = 3)
            advanceUntilIdle()

            assertEquals(presentation, vm.selectedPresentation.value)
            assertEquals(3, vm.selectedSlideIndex.value)
            assertTrue(vm.isProjecting.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `selecting a slide drops the previous add-to-schedule confirmation`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.selectPresentation(firstPresentation(), slideIndex = 0)
            vm.addToSchedule()
            advanceUntilIdle()
            assertTrue(vm.scheduleAdded.value)

            vm.selectPresentation(firstPresentation(), slideIndex = 1)

            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a presentation with no id is still selected locally, but sends nothing`() = runVmTest {
        // The early return skips the request; the UI must still show the tap.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val idless = Presentation(id = "", fileName = "Untitled")

            vm.selectPresentation(idless, slideIndex = 2)
            advanceUntilIdle()

            assertEquals(idless, vm.selectedPresentation.value)
            assertEquals(2, vm.selectedSlideIndex.value)
            assertNull(vm.toastEvent.value, "a skipped request is not a failure to report")
        } finally {
            tearDown(vm)
        }
    }

    // ── Clearing ─────────────────────────────────────────────────────────

    @Test
    fun `clearing the display stops projecting but keeps the selection`() = runVmTest {
        // Kept deliberately so Cast can re-project and Add-to-Schedule has a target.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val presentation = firstPresentation()
            vm.selectPresentation(presentation, slideIndex = 2)
            advanceUntilIdle()

            vm.clearDisplay()
            advanceUntilIdle()

            assertFalse(vm.isProjecting.value)
            assertEquals(presentation, vm.selectedPresentation.value)
            assertEquals(2, vm.selectedSlideIndex.value)
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
            vm.selectPresentation(firstPresentation(), slideIndex = 0)

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
    fun `navigating narrows the list to the presentation that was tapped`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val target = DemoData.presentations.last()

            vm.navigateTo(target.displayId)
            advanceUntilIdle()

            assertEquals(listOf(target), vm.presentations.value)
            assertEquals(target, vm.selectedPresentation.value)
            assertEquals(target.displayId, vm.pendingScrollToId.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `navigating to an unknown id falls back rather than emptying the screen`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()

            vm.navigateTo("no-such-presentation")
            advanceUntilIdle()

            assertEquals(1, vm.presentations.value.size)
            assertEquals(DemoData.presentations.first(), vm.selectedPresentation.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a handled scroll fires only once`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.navigateTo(firstPresentation().displayId)
            advanceUntilIdle()

            vm.onPendingScrollHandled()

            assertNull(vm.pendingScrollToId.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Errors and toasts ────────────────────────────────────────────────

    @Test
    fun `an error reported before any request reaches the UI`() = runVmTest {
        // Used for file-too-large, which is known without asking the server.
        val vm = demoVm()
        try {
            advanceUntilIdle()

            vm.reportError("That file is too large")

            assertEquals("That file is too large", vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a consumed toast is cleared so it shows once`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()

            vm.toastShown()

            assertNull(vm.toastEvent.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────

    @Test
    fun `saving settings resets the screen before reloading`() = runVmTest {
        // A new server means the old selection points at content that may not
        // exist there, so every piece of per-server state has to go.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.selectPresentation(firstPresentation(), slideIndex = 4)
            vm.addToSchedule()
            advanceUntilIdle()

            vm.onSettingsSaved()
            advanceUntilIdle()

            assertNull(vm.selectedPresentation.value)
            assertNull(vm.selectedSlideIndex.value)
            assertNull(vm.pendingScrollToId.value)
            assertFalse(vm.isProjecting.value)
            assertFalse(vm.scheduleAdded.value)
            assertFalse(vm.isUploading.value)
            assertNull(vm.toastEvent.value)
            // …and the list is loaded again from the (demo) source.
            assertEquals(DemoData.presentations, vm.presentations.value)
        } finally {
            tearDown(vm)
        }
    }
}
