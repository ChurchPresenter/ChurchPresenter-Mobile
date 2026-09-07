package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import com.church.presenter.churchpresentermobile.network.PicturesService
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import com.church.presenter.churchpresentermobile.ui.PickedPhoto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
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

    // ── The live paths ───────────────────────────────────────────────────
    //
    // Everything above runs in demo mode, which short-circuits before any
    // request. These drive the real ones through a mocked PicturesService.

    private fun liveVm(
        ws: FakeWsSender = FakeWsSender(),
        handler: MockRequestHandleScope.(path: String) -> HttpResponseData,
    ): PicturesViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return PicturesViewModel(settings, ws, isDemoMode = false) {
            PicturesService(it, ws, mockClient(handler))
        }
    }

    private val folderJson = """
        {"folder-id":"f1","folder-name":"Nature","image-total":2,
         "images":[{"index":0,"file-name":"a.jpg"},{"index":1,"file-name":"b.jpg"}]}
    """.trimIndent()

    @Test
    fun `loading fetches the folder and lowers the spinner`() = runVmTestUnconfined {
        val vm = liveVm { respond(folderJson) }
        try {
            val folder = vm.folder.first { it != null }

            assertEquals("Nature", folder?.displayName)
            assertEquals(2, folder?.allImages?.size)
            assertFalse(vm.isLoading.value)
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a desktop with no folder open is an empty state, not an error`() = runVmTestUnconfined {
        // The desktop answers 503 until the operator opens a picture folder.
        // Nothing has gone wrong and there is nothing the phone can retry into
        // existence, so a red banner offering "try again" would be a lie.
        val vm = liveVm { respond("No picture folder loaded", HttpStatusCode.ServiceUnavailable) }
        try {
            vm.isLoading.first { !it }

            assertNull(vm.error.value, "503 must not become an error banner")
            assertNull(vm.folder.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a real failure is reported`() = runVmTestUnconfined {
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
    fun `selecting a picture sends it over the socket`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws) { respond(folderJson) }
        try {
            val folder = vm.folder.first { it != null }!!

            vm.selectPicture(folder.allImages.first())
            vm.isProjecting.first { it }

            assertEquals(WsMessageType.SELECT_PICTURE, ws.lastType)
            assertTrue(ws.lastPayload.contains("f1"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the display sends clear`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws) { respond(folderJson) }
        try {
            vm.folder.first { it != null }

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
        val vm = liveVm(ws) { respond(folderJson) }
        try {
            val folder = vm.folder.first { it != null }!!
            vm.selectPicture(folder.allImages.first())

            vm.addToSchedule()
            vm.scheduleAdded.first { it }

            assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
            assertTrue(ws.lastPayload.contains("a.jpg"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `an image with no file name falls back to its index for the label`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws) { respond("""{"folder-id":"f1","images":[{"index":7}]}""") }
        try {
            val folder = vm.folder.first { it != null }!!
            vm.selectPicture(folder.allImages.first())

            vm.addToSchedule()
            vm.scheduleAdded.first { it }

            assertTrue(ws.lastPayload.contains("Image 7"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a refused add is reported and not confirmed`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("denied"))
        val vm = liveVm(ws) { respond(folderJson) }
        try {
            val folder = vm.folder.first { it != null }!!
            vm.selectPicture(folder.allImages.first())

            vm.addToSchedule()
            val error = vm.error.first { it != null }

            assertNotNull(error)
            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `navigating loads the named folder before asking to scroll`() = runVmTestUnconfined {
        // Order matters: the screen reads the folder when the scroll index appears,
        // so setting the index first projected an image from the previous folder.
        val vm = liveVm { respond(folderJson) }
        try {
            vm.folder.first { it != null }

            vm.navigateTo(folderId = "f1", imageIndex = 1)
            val index = vm.pendingScrollIndex.first { it != null }

            assertEquals(1, index)
            assertNotNull(vm.folder.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `saving settings rebuilds the service through the same factory`() = runVmTestUnconfined {
        var built = 0
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val vm = PicturesViewModel(settings, ws, isDemoMode = false) {
            built++
            PicturesService(it, ws, mockClient { respond(folderJson) })
        }
        try {
            vm.folder.first { it != null }
            assertEquals(1, built)

            vm.onSettingsSaved()
            vm.folder.first { it != null }

            assertEquals(2, built, "a new server needs a new client")
        } finally {
            tearDown(vm)
        }
    }

    // ── Uploading device photos ──────────────────────────────────────────
    //
    // The longest flow on this screen: upload each photo in turn, project the
    // last one that succeeded, then reload the folder so the grid shows them.
    // Order matters at the end — the folder has to arrive before the scroll
    // index, or the grid scrolls within the *previous* folder.

    private fun uploadVm(
        ws: FakeWsSender = FakeWsSender(),
        uploadStatus: HttpStatusCode = HttpStatusCode.OK,
        folderStatus: HttpStatusCode = HttpStatusCode.OK,
    ): PicturesViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        var uploaded = 0
        return PicturesViewModel(settings, ws, isDemoMode = false) {
            PicturesService(
                it,
                ws,
                mockClient { respond(uploadedFolderJson, folderStatus) },
                uploadClient = HttpClient(MockEngine {
                    uploaded += 1
                    respond(
                        """{"ok":true,"folder-id":"device_uploads",""" +
                            """"image-index":$uploaded,"file-name":"p$uploaded.jpg"}""",
                        uploadStatus,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }),
            )
        }
    }

    private val uploadedFolderJson = """
        {"folder-id":"device_uploads","folder-name":"Device Photos","image-total":2,
         "images":[{"index":1,"file-name":"p1.jpg"},{"index":2,"file-name":"p2.jpg"}]}
    """.trimIndent()

    private fun photo(name: String) = PickedPhoto(byteArrayOf(1, 2, 3), name)

    @Test
    fun `uploading nothing does not start an upload`() = runVmTestUnconfined {
        val vm = uploadVm()
        try {
            vm.uploadDevicePhotos(emptyList())

            assertFalse(vm.isUploading.value)
            assertEquals(0, vm.uploadPhotoTotal.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a finished upload leaves no spinner behind`() = runVmTestUnconfined {
        val vm = uploadVm()
        try {
            vm.uploadDevicePhotos(listOf(photo("a.jpg")))
            vm.pendingScrollIndex.first { it != null }

            assertFalse(vm.isUploading.value)
            assertNull(vm.uploadProgress.value)
            assertEquals(0, vm.uploadPhotoTotal.value)
            assertEquals(0, vm.uploadPhotoIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the last uploaded photo goes live`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = uploadVm(ws)
        try {
            vm.uploadDevicePhotos(listOf(photo("a.jpg"), photo("b.jpg")))
            vm.pendingScrollIndex.first { it != null }

            assertTrue(vm.isProjecting.value)
            assertEquals(WsMessageType.SELECT_PICTURE, ws.lastType)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the folder is reloaded before the grid is told to scroll`() = runVmTestUnconfined {
        // Reversing these two scrolls the grid inside the old folder, which is
        // the bug the inline fetch exists to prevent.
        val vm = uploadVm()
        try {
            vm.uploadDevicePhotos(listOf(photo("a.jpg")))
            val index = vm.pendingScrollIndex.first { it != null }

            assertEquals("Device Photos", vm.folder.value?.displayName)
            assertEquals(1, index)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `every photo in the batch is counted`() = runVmTestUnconfined {
        val vm = uploadVm()
        try {
            vm.uploadDevicePhotos(listOf(photo("a.jpg"), photo("b.jpg"), photo("c.jpg")))
            vm.pendingScrollIndex.first { it != null }

            // The last upload's index is what the grid scrolls to.
            assertEquals(3, vm.pendingScrollIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed upload is reported and does not project`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = uploadVm(ws, uploadStatus = HttpStatusCode.PayloadTooLarge)
        try {
            vm.uploadDevicePhotos(listOf(photo("big.jpg")))
            val error = vm.error.first { it != null }

            assertTrue(error!!.contains("big.jpg"), error)
            assertFalse(vm.isProjecting.value, "nothing uploaded, so nothing to project")
            assertFalse(vm.isUploading.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a photo that uploaded but could not be shown says so`() = runVmTestUnconfined {
        // The distinction matters: the photo is on the desktop, so retrying the
        // upload would duplicate it. Only the projection needs another go.
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("socket closed"))
        val vm = uploadVm(ws)
        try {
            vm.uploadDevicePhotos(listOf(photo("a.jpg")))
            val error = vm.error.first { it != null }

            assertTrue(error!!.contains("Uploaded but"), error)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a photo that uploaded but whose folder would not load says so`() = runVmTestUnconfined {
        val vm = uploadVm(folderStatus = HttpStatusCode.InternalServerError)
        try {
            vm.uploadDevicePhotos(listOf(photo("a.jpg")))
            val error = vm.error.first { it != null && it.contains("Uploaded but") }

            assertTrue(error!!.contains("folder"), error)
            assertFalse(vm.isUploading.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `demo mode uploads nothing at all`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = PicturesViewModel(AppSettings(InMemorySettingsStorage()), ws, isDemoMode = true)
        try {
            vm.uploadDevicePhotos(listOf(photo("a.jpg")))

            assertFalse(vm.isUploading.value)
            assertTrue(ws.calls.isEmpty())
        } finally {
            tearDown(vm)
        }
    }
}
