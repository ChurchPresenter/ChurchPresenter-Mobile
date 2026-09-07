package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests [PicturesService]: the `getPictures` decode and thumbnail resolution, and
 * the WebSocket actions — select, clear and add-to-schedule — which reach the
 * desktop as JSON payloads rather than HTTP requests.
 */
class PicturesServiceTest {

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK): PicturesService {
        val settings = AppSettings(InMemorySettingsStorage())
        return PicturesService(settings, ServerEventService(settings), mockClient { respond(body, status) })
    }

    @Test
    fun getPicturesParsesAndResolvesThumbnails() = runTest {
        val body = """
            {"folder-id":"f1","folder-name":"Nature","image-total":2,
             "images":[{"index":0,"file-name":"a.jpg","thumbnail-url":"/thumb/0"}]}
        """.trimIndent()
        val folder = service(body).getPictures().getOrThrow()
        assertEquals("Nature", folder.displayName)
        assertEquals(2, folder.totalImages)
        assertEquals("http://192.168.1.100:8765/thumb/0", folder.allImages[0].thumbnailUrl)
    }

    @Test
    fun getPicturesLeavesNullThumbnailNull() = runTest {
        val body = """{"folder-name":"F","images":[{"index":0,"file-name":"a.jpg"}]}"""
        assertNull(service(body).getPictures().getOrThrow().allImages[0].thumbnailUrl)
    }

    @Test
    fun getPicturesNonSuccessIsFailure() = runTest {
        assertTrue(service("no", HttpStatusCode.NotFound).getPictures().isFailure)
    }

    @Test
    fun getPicturesNonSuccessThrowsApiExceptionCarryingStatusAndReason() = runTest {
        // Must be an ApiException, not a plain one: recordNetworkError decides what
        // reaches crash reporting by type, and a bare exception is treated as a bug.
        val error = service("No picture folder loaded", HttpStatusCode.ServiceUnavailable)
            .getPictures().exceptionOrNull()
        assertIs<ApiException>(error)
        assertEquals(503, error.httpStatus)
        assertEquals("No picture folder loaded", error.reason)
    }

    @Test
    fun getPicturesAsksForTheNamedFolderWhenGivenOne() = runTest {
        // The schedule drawer navigates into a specific folder; without the id in
        // the path the desktop answers with whatever is currently open instead.
        var requested = ""
        val settings = AppSettings(InMemorySettingsStorage())
        val service = PicturesService(
            settings,
            FakeWsSender(),
            mockClient { path -> requested = path; respond("""{"folder-name":"F"}""") },
        )

        service.getPictures("f7").getOrThrow()

        assertTrue(requested.endsWith("/pictures/f7"), "requested $requested")
    }

    @Test
    fun getPicturesWithABlankFolderIdFallsBackToTheCurrentFolder() = runTest {
        var requested = ""
        val settings = AppSettings(InMemorySettingsStorage())
        val service = PicturesService(
            settings,
            FakeWsSender(),
            mockClient { path -> requested = path; respond("""{"folder-name":"F"}""") },
        )

        service.getPictures("   ").getOrThrow()

        assertTrue(requested.endsWith("/pictures"), "requested $requested")
    }

    // ── WebSocket actions ────────────────────────────────────────────────

    private fun wsService(ws: FakeWsSender): PicturesService {
        val settings = AppSettings(InMemorySettingsStorage())
        return PicturesService(settings, ws, mockClient { respond("{}") })
    }

    @Test
    fun selectPictureSendsSelectPictureOverTheSocket() = runTest {
        val ws = FakeWsSender()

        wsService(ws).selectPicture("f1", "a.jpg", indexFallback = 3).getOrThrow()

        assertEquals(WsMessageType.SELECT_PICTURE, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"folder-id\":\"f1\""), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"index\":3"), ws.lastPayload)
    }

    @Test
    fun selectPictureIsFireAndForget() = runTest {
        // Projecting must not block on an acknowledgement — the operator taps and
        // the picture appears; waiting for a round-trip made the grid feel stuck.
        val ws = FakeWsSender()

        wsService(ws).selectPicture("f1", "a.jpg").getOrThrow()

        assertTrue(ws.calls.last().third, "select should be fire-and-forget")
    }

    @Test
    fun selectPictureReportsASocketFailure() = runTest {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("socket closed"))

        assertTrue(wsService(ws).selectPicture("f1", "a.jpg").isFailure)
    }

    @Test
    fun clearDisplaySendsClear() = runTest {
        val ws = FakeWsSender()

        wsService(ws).clearDisplay().getOrThrow()

        assertEquals(WsMessageType.CLEAR, ws.lastType)
        assertEquals("", ws.lastPayload)
        assertTrue(ws.calls.last().third, "clear should be fire-and-forget")
    }

    @Test
    fun addToScheduleSendsTheItemTheDesktopReads() = runTest {
        val ws = FakeWsSender()

        wsService(ws).addToSchedule("f1", imageIndex = 4, displayText = "a.jpg").getOrThrow()

        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"folder-id\":\"f1\""), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"image-index\":4"), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"displayText\":\"a.jpg\""), ws.lastPayload)
    }

    @Test
    fun addToScheduleWaitsForApprovalRatherThanFiringAndForgetting() = runTest {
        // Adding needs the operator's Allow on the desktop, so the result has to
        // come back — unlike select, which is immediate.
        val ws = FakeWsSender()

        wsService(ws).addToSchedule("f1", 0, "a.jpg").getOrThrow()

        assertFalse(ws.calls.last().third)
    }

    @Test
    fun addToScheduleReportsARefusal() = runTest {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("denied"))

        assertTrue(wsService(ws).addToSchedule("f1", 0, "a.jpg").isFailure)
    }

    // ── Uploading a device photo ─────────────────────────────────────────
    //
    // The photo is sent as a base64 data-URI whose MIME type comes from the file
    // extension. A wrong type is not rejected — the desktop saves the bytes under
    // a name it cannot then display.

    private fun uploadService(
        capture: (String) -> Unit = {},
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"ok":true,"folder-id":"device_uploads","image-index":3,"file-name":"a.jpg"}""",
    ): PicturesService {
        val settings = AppSettings(InMemorySettingsStorage())
        return PicturesService(
            settings,
            FakeWsSender(),
            mockClient { respond("{}") },
            uploadClient = HttpClient(MockEngine { request ->
                capture((request.body as io.ktor.http.content.TextContent).text)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }),
        )
    }

    @Test
    fun uploadingReturnsWhereTheServerPutThePhoto() = runTest {
        val response = uploadService().uploadPhoto(byteArrayOf(1, 2, 3), "a.jpg").getOrThrow()

        assertTrue(response.ok)
        assertEquals("device_uploads", response.folderId)
        assertEquals(3, response.imageIndex)
    }

    @Test
    fun uploadingSendsTheNameAndABase64DataUri() = runTest {
        var body = ""

        uploadService(capture = { body = it }).uploadPhoto(byteArrayOf(1, 2, 3), "a.jpg").getOrThrow()

        assertTrue(body.contains("\"name\":\"a.jpg\""), body)
        assertTrue(body.contains("data:image/jpeg;base64,"), body)
    }

    @Test
    fun eachExtensionGetsItsOwnMimeType() = runTest {
        val expected = mapOf(
            "a.png" to "image/png",
            "a.gif" to "image/gif",
            "a.bmp" to "image/bmp",
            "a.webp" to "image/webp",
            "a.heic" to "image/heic",
            "a.heif" to "image/heif",
            "a.jpg" to "image/jpeg",
            "a.jpeg" to "image/jpeg",
        )
        for ((name, mime) in expected) {
            var body = ""
            uploadService(capture = { body = it }).uploadPhoto(byteArrayOf(1), name).getOrThrow()

            assertTrue(body.contains("data:$mime;base64,"), "$name → $body")
        }
    }

    @Test
    fun anUnknownOrMissingExtensionIsSentAsJpeg() = runTest {
        // The desktop needs *a* type; JPEG is the safe default for a camera roll.
        for (name in listOf("a.raw", "photo")) {
            var body = ""
            uploadService(capture = { body = it }).uploadPhoto(byteArrayOf(1), name).getOrThrow()

            assertTrue(body.contains("data:image/jpeg;base64,"), "$name → $body")
        }
    }

    @Test
    fun theExtensionIsReadCaseInsensitively() = runTest {
        var body = ""

        uploadService(capture = { body = it }).uploadPhoto(byteArrayOf(1), "A.PNG").getOrThrow()

        assertTrue(body.contains("data:image/png;base64,"), body)
    }

    @Test
    fun aNameWithSpacesOrQuotesIsEscapedRatherThanBreakingTheJson() = runTest {
        // The body is assembled by hand, so the name has to be encoded properly.
        var body = ""

        uploadService(capture = { body = it }).uploadPhoto(byteArrayOf(1), """my "best" shot.jpg""").getOrThrow()

        assertTrue(body.contains("\\\"best\\\""), body)
    }

    @Test
    fun aRejectedUploadIsAFailureCarryingTheServersReason() = runTest {
        val error = uploadService(status = HttpStatusCode.PayloadTooLarge, body = "File too large")
            .uploadPhoto(byteArrayOf(1), "a.jpg")
            .exceptionOrNull()

        assertIs<ApiException>(error)
        assertEquals(413, error.httpStatus)
        assertEquals("File too large", error.reason)
    }
}
