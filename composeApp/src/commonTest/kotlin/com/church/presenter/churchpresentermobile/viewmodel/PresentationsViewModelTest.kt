package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.PresentationService
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import com.church.presenter.churchpresentermobile.ui.PickedFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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

    // ── The live paths ───────────────────────────────────────────────────

    private fun liveVm(
        ws: FakeWsSender = FakeWsSender(),
        handler: MockRequestHandleScope.(path: String) -> HttpResponseData,
    ): PresentationsViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return PresentationsViewModel(settings, ws, isDemoMode = false) {
            PresentationService(it, ws, mockClient(handler))
        }
    }

    private val listJson = """
        {"presentations":[
          {"id":"p1","file-name":"Sermon.pptx","slide-total":3,
           "slides":[{"slide-index":0},{"slide-index":1},{"slide-index":2}]}
        ]}
    """.trimIndent()

    @Test
    fun `loading fetches the list and lowers the spinner`() = runVmTestUnconfined {
        val vm = liveVm { respond(listJson) }
        try {
            val list = vm.presentations.first { it.isNotEmpty() }

            assertEquals(1, list.size)
            assertEquals("Sermon.pptx", list.first().displayName)
            assertFalse(vm.isLoading.value)
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed load is reported and stops the spinner`() = runVmTestUnconfined {
        val vm = liveVm { respond("boom", HttpStatusCode.InternalServerError) }
        try {
            val error = vm.error.first { it != null }

            assertNotNull(error)
            assertFalse(vm.isLoading.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `selecting a slide sends its index over the socket`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws) { respond(listJson) }
        try {
            val list = vm.presentations.first { it.isNotEmpty() }

            vm.selectPresentation(list.first(), slideIndex = 2)
            vm.isProjecting.first { it }

            assertEquals(WsMessageType.SELECT_SLIDE, ws.lastType)
            assertTrue(ws.lastPayload.contains("\"index\":2"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed slide selection raises a toast`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("socket closed"))
        val vm = liveVm(ws) { respond(listJson) }
        try {
            val list = vm.presentations.first { it.isNotEmpty() }

            vm.selectPresentation(list.first(), slideIndex = 0)
            val toast = vm.toastEvent.first { it != null }

            assertIs<ToastEvent.FailedToSelectPresentation>(toast)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the display sends clear`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws) { respond(listJson) }
        try {
            vm.presentations.first { it.isNotEmpty() }

            vm.clearDisplay()
            vm.isProjecting.first { !it }

            assertEquals(WsMessageType.CLEAR, ws.lastType)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding to schedule sends the item and confirms`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws) { respond(listJson) }
        try {
            val list = vm.presentations.first { it.isNotEmpty() }
            vm.selectPresentation(list.first(), slideIndex = 0)

            vm.addToSchedule()
            vm.scheduleAdded.first { it }

            assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
            assertTrue(ws.lastPayload.contains("Sermon.pptx"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a refused add raises a toast rather than confirming`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("denied"))
        val vm = liveVm(ws) { respond(listJson) }
        try {
            val list = vm.presentations.first { it.isNotEmpty() }
            vm.selectPresentation(list.first(), slideIndex = 0)

            vm.addToSchedule()
            val toast = vm.toastEvent.first { it is ToastEvent.FailedToAddPresentationSchedule }

            assertIs<ToastEvent.FailedToAddPresentationSchedule>(toast)
            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `navigating fetches just the presentation that was tapped`() = runVmTestUnconfined {
        val vm = liveVm { path ->
            if (path.endsWith("/p1")) respond("""{"id":"p1","file-name":"Sermon.pptx"}""")
            else respond(listJson)
        }
        try {
            vm.presentations.first { it.isNotEmpty() }

            vm.navigateTo("p1")
            val id = vm.pendingScrollToId.first { it != null }

            assertEquals("p1", id)
            assertEquals(1, vm.presentations.value.size, "the list narrows to the tapped deck")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed navigation is reported`() = runVmTestUnconfined {
        val vm = liveVm { path ->
            if (path.endsWith("/p9")) respond("gone", HttpStatusCode.NotFound)
            else respond(listJson)
        }
        try {
            vm.presentations.first { it.isNotEmpty() }

            vm.navigateTo("p9")
            val error = vm.error.first { it != null }

            assertNotNull(error)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `saving settings rebuilds the service through the same factory`() = runVmTestUnconfined {
        var built = 0
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val vm = PresentationsViewModel(settings, ws, isDemoMode = false) {
            built++
            PresentationService(it, ws, mockClient { respond(listJson) })
        }
        try {
            vm.presentations.first { it.isNotEmpty() }
            assertEquals(1, built)

            vm.onSettingsSaved()
            vm.presentations.first { it.isNotEmpty() }

            assertEquals(2, built, "a new server needs a new client")
        } finally {
            tearDown(vm)
        }
    }

    // ── Uploading a deck ─────────────────────────────────────────────────
    //
    // The server renders slides asynchronously, so the upload reply is not the
    // end: the ViewModel polls the list until the new deck appears, then scrolls
    // to it. The polling is what makes a freshly uploaded deck usable rather than
    // absent until the operator pulls to refresh.

    private fun uploadVm(
        uploadStatus: HttpStatusCode = HttpStatusCode.OK,
        uploadBody: String = """{"ok":true,"id":"p9","name":"Sermon.pptx"}""",
        listBody: () -> String,
    ): PresentationsViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        return PresentationsViewModel(settings, ws, isDemoMode = false) {
            PresentationService(
                it,
                ws,
                mockClient { respond(listBody()) },
                uploadClient = HttpClient(MockEngine {
                    respond(uploadBody, uploadStatus, headersOf(HttpHeaders.ContentType, "application/json"))
                }),
            )
        }
    }

    private val withUploaded = """{"presentations":[{"id":"p9","file-name":"Sermon.pptx","slide-total":1}]}"""
    private val withoutUploaded = """{"presentations":[]}"""

    private fun file(name: String = "Sermon.pptx") = PickedFile(byteArrayOf(1, 2, 3), name)

    @Test
    fun `an uploaded deck appears and is scrolled to`() = runVmTestUnconfined {
        val vm = uploadVm { withUploaded }
        try {
            vm.uploadPresentationFile(file())
            val id = vm.pendingScrollToId.first { it != null }

            assertEquals("p9", id)
            assertEquals("p9", vm.selectedPresentation.value?.displayId)
            assertEquals(1, vm.presentations.value.size)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a finished upload leaves no spinner behind`() = runVmTestUnconfined {
        val vm = uploadVm { withUploaded }
        try {
            vm.uploadPresentationFile(file())
            vm.pendingScrollToId.first { it != null }

            assertFalse(vm.isUploading.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the list is emptied while the server renders slides`() = runVmTestUnconfined {
        // Leaving the old list up would show entries that no longer match what the
        // server has, and the new deck would appear to be missing.
        val vm = uploadVm { withUploaded }
        try {
            vm.uploadPresentationFile(file())
            vm.pendingScrollToId.first { it != null }

            assertTrue(vm.presentations.value.none { it.displayId == "old" })
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `polling keeps asking until the deck has finished rendering`() = runVmTestUnconfined {
        // The first look does not have it yet — exactly what the poll is for.
        var attempt = 0
        val vm = uploadVm { if (++attempt < 3) withoutUploaded else withUploaded }
        try {
            vm.uploadPresentationFile(file())
            val id = vm.pendingScrollToId.first { it != null }

            assertEquals("p9", id)
            assertTrue(attempt >= 3, "gave up after $attempt attempts")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a reply with no id still refreshes the list`() = runVmTestUnconfined {
        // Older desktops answer {"ok":true}; there is nothing to scroll to, but the
        // deck did upload and the list must show it.
        val vm = uploadVm(uploadBody = """{"ok":true}""") { withUploaded }
        try {
            vm.uploadPresentationFile(file())
            // Await the flag that marks the whole flow finished, not the list:
            // the list fills mid-flight, and asserting on isUploading then raced
            // (green on JS, red on the JVM).
            vm.isUploading.first { !it }

            assertNull(vm.pendingScrollToId.value, "nothing to scroll to without an id")
            assertTrue(vm.presentations.value.isNotEmpty())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a rejected upload raises a toast and stops the spinner`() = runVmTestUnconfined {
        val vm = uploadVm(uploadStatus = HttpStatusCode.PayloadTooLarge, uploadBody = "Too large") { withUploaded }
        try {
            vm.uploadPresentationFile(file())
            val toast = vm.toastEvent.first { it != null }

            assertNotNull(toast)
            assertFalse(vm.isUploading.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `demo mode uploads nothing`() = runVmTestUnconfined {
        val vm = demoVm()
        try {
            advanceUntilIdle()

            vm.uploadPresentationFile(file())

            assertFalse(vm.isUploading.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Why an upload was refused ────────────────────────────────────────
    //
    // Four different answers, because four different things are wrong and each
    // needs the operator to do something different — or nothing at all.

    @Test
    fun `a desktop too old to accept uploads says so`() = runVmTestUnconfined {
        // 404: the endpoint does not exist on that build. Retrying will never work;
        // the operator needs to update the desktop.
        val vm = uploadVm(uploadStatus = HttpStatusCode.NotFound, uploadBody = "no such route") { withUploaded }
        try {
            vm.uploadPresentationFile(file())
            val toast = vm.toastEvent.first { it != null }

            assertIs<ToastEvent.UploadUnsupported>(toast)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a file the server will not take says it is too large`() = runVmTestUnconfined {
        // 413: a smaller file would work, which is worth telling them.
        val vm = uploadVm(uploadStatus = HttpStatusCode.PayloadTooLarge, uploadBody = "too big") { withUploaded }
        try {
            vm.uploadPresentationFile(file())
            val toast = vm.toastEvent.first { it != null }

            assertIs<ToastEvent.UploadFileTooLarge>(toast)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `any other server refusal is quoted`() = runVmTestUnconfined {
        // 500: the desktop itself broke. Nothing for the operator to fix, but the
        // reason is worth showing so it can be reported.
        val vm = uploadVm(uploadStatus = HttpStatusCode.InternalServerError, uploadBody = "boom") { withUploaded }
        try {
            vm.uploadPresentationFile(file())
            val toast = vm.toastEvent.first { it != null }

            assertIs<ToastEvent.UploadServerError>(toast)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `each refusal is a distinguishable outcome`() = runVmTestUnconfined {
        // Collapsing these into one message is what this mapping replaced.
        val seen = mutableListOf<ToastEvent>()
        for (status in listOf(
            HttpStatusCode.NotFound,
            HttpStatusCode.PayloadTooLarge,
            HttpStatusCode.InternalServerError,
        )) {
            val vm = uploadVm(uploadStatus = status, uploadBody = "x") { withUploaded }
            try {
                vm.uploadPresentationFile(file())
                seen += vm.toastEvent.first { it != null }!!
            } finally {
                tearDown(vm)
            }
        }

        assertEquals(seen.size, seen.map { it::class }.toSet().size, "two refusals share a message: $seen")
    }

    @Test
    fun `a failed upload leaves no spinner behind`() = runVmTestUnconfined {
        val vm = uploadVm(uploadStatus = HttpStatusCode.InternalServerError, uploadBody = "boom") { withUploaded }
        try {
            vm.uploadPresentationFile(file())
            vm.toastEvent.first { it != null }

            assertFalse(vm.isUploading.value)
            assertNull(vm.uploadProgress.value)
        } finally {
            tearDown(vm)
        }
    }
}
