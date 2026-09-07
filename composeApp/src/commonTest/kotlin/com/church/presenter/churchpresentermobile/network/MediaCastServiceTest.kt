package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.MediaItemPayload
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.model.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests [MediaCastService]'s WebSocket surface: casting a URL, and the transport
 * controls behind the player bar.
 *
 * `uploadMedia` is not covered here — it streams through an internally-built
 * upload client with no request timeout and no injection seam, so a unit test
 * cannot reach it.
 */
class MediaCastServiceTest {

    private fun service(ws: FakeWsSender) =
        MediaCastService(AppSettings(InMemorySettingsStorage()), ws)

    private fun item(url: String = "https://example.org/clip.mp4", type: String = "url") =
        MediaItemPayload(id = "m1", mediaUrl = url, mediaTitle = "Clip", mediaType = type)

    // ── Casting ──────────────────────────────────────────────────────────

    @Test
    fun goLiveSendsProject() = runTest {
        val ws = FakeWsSender()

        service(ws).goLive(item()).getOrThrow()

        assertEquals(WsMessageType.PROJECT, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"mediaUrl\":\"https://example.org/clip.mp4\""), ws.lastPayload)
    }

    @Test
    fun addToScheduleSendsAddToSchedule() = runTest {
        val ws = FakeWsSender()

        service(ws).addToSchedule(item()).getOrThrow()

        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"mediaTitle\":\"Clip\""), ws.lastPayload)
    }

    @Test
    fun bothCastActionsWaitForTheOperatorsApproval() = runTest {
        // Projecting and adding both raise a dialog on the desktop, so neither can
        // be fire-and-forget — the result is what tells the phone it was allowed.
        val ws = FakeWsSender()

        service(ws).goLive(item()).getOrThrow()
        assertFalse(ws.calls.last().third)

        service(ws).addToSchedule(item()).getOrThrow()
        assertFalse(ws.calls.last().third)
    }

    /**
     * The bug this guards: kotlinx omits any field equal to its default, so without
     * `encodeDefaults` the payload carried no `mediaType`. The desktop then fell back
     * to "local" and tried to open the URL as a file path — nothing played.
     */
    @Test
    fun theDefaultMediaTypeStillReachesTheDesktop() = runTest {
        val ws = FakeWsSender()

        service(ws).goLive(MediaItemPayload(mediaUrl = "https://example.org/clip.mp4")).getOrThrow()

        assertTrue(ws.lastPayload.contains("\"mediaType\":\"url\""), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"type\":\"media\""), ws.lastPayload)
    }

    @Test
    fun anUploadedFileIsCastAsLocal() = runTest {
        val ws = FakeWsSender()

        service(ws).goLive(item(url = "/Users/av/Movies/clip.mp4", type = "local")).getOrThrow()

        assertTrue(ws.lastPayload.contains("\"mediaType\":\"local\""), ws.lastPayload)
    }

    @Test
    fun aRefusedCastIsReportedAsAFailure() = runTest {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("denied"))

        assertTrue(service(ws).goLive(item()).isFailure)
        assertTrue(service(ws).addToSchedule(item()).isFailure)
    }

    @Test
    fun clearScreenSendsClearAndDoesNotWait() = runTest {
        val ws = FakeWsSender()

        service(ws).clearScreen().getOrThrow()

        assertEquals(WsMessageType.CLEAR, ws.lastType)
        assertEquals("", ws.lastPayload)
        assertTrue(ws.calls.last().third)
    }

    // ── Transport controls ───────────────────────────────────────────────

    @Test
    fun eachTransportControlSendsItsOwnMessage() = runTest {
        val ws = FakeWsSender()
        val svc = service(ws)

        svc.playPause().getOrThrow()
        assertEquals(WsMessageType.MEDIA_PLAY_PAUSE, ws.lastType)

        svc.stop().getOrThrow()
        assertEquals(WsMessageType.MEDIA_STOP, ws.lastType)

        svc.seekForward().getOrThrow()
        assertEquals(WsMessageType.MEDIA_SEEK_FORWARD, ws.lastType)

        svc.seekBackward().getOrThrow()
        assertEquals(WsMessageType.MEDIA_SEEK_BACKWARD, ws.lastType)

        svc.muteToggle().getOrThrow()
        assertEquals(WsMessageType.MEDIA_MUTE_TOGGLE, ws.lastType)
    }

    @Test
    fun seekToCarriesThePositionInMilliseconds() = runTest {
        val ws = FakeWsSender()

        service(ws).seekTo(12_500L).getOrThrow()

        assertEquals(WsMessageType.MEDIA_SEEK_TO, ws.lastType)
        assertEquals("12500", ws.lastPayload)
    }

    @Test
    fun setVolumeCarriesTheLevel() = runTest {
        val ws = FakeWsSender()

        service(ws).setVolume(0.5f).getOrThrow()

        assertEquals(WsMessageType.MEDIA_SET_VOLUME, ws.lastType)
        assertEquals("0.5", ws.lastPayload)
    }

    @Test
    fun everyTransportControlIsFireAndForget() = runTest {
        // The desktop echoes the new playback state back over the socket, so the
        // button does not wait for an acknowledgement it would only discard.
        val ws = FakeWsSender()
        val svc = service(ws)

        svc.playPause().getOrThrow()
        svc.stop().getOrThrow()
        svc.seekForward().getOrThrow()
        svc.seekBackward().getOrThrow()
        svc.seekTo(0L).getOrThrow()
        svc.setVolume(1f).getOrThrow()
        svc.muteToggle().getOrThrow()

        assertEquals(7, ws.calls.size)
        assertTrue(ws.calls.all { it.third }, "every transport control should be fire-and-forget")
    }

    @Test
    fun aTransportControlReportsASocketFailure() = runTest {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("socket closed"))

        assertTrue(service(ws).playPause().isFailure)
        assertTrue(service(ws).seekTo(1L).isFailure)
    }

    // ── Uploading a media file ───────────────────────────────────────────
    //
    // The file streams rather than being buffered — a 100 MB video will not fit
    // in Android's heap — so the progress callback is the only signal the caller
    // has, and Content-Length has to come from the picked file's own size.

    private fun uploadService(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = """{"ok":true,"path":"/Users/av/Movies/clip.mp4","name":"clip.mp4","mediaType":"local"}""",
        capture: (String) -> Unit = {},
    ): MediaCastService = MediaCastService(
        AppSettings(InMemorySettingsStorage()),
        FakeWsSender(),
        uploadClient = HttpClient(MockEngine { request ->
            capture(request.url.toString())
            // MockEngine does not drain a WriteChannelContent on its own, so the
            // streaming body — and with it the progress callback — would never run.
            (request.body as? OutgoingContent.WriteChannelContent)?.writeTo(ByteChannel())
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }),
    )

    private fun picked(name: String = "clip.mp4", size: Long = 300L) =
        PickedMediaFile(name, size) { _, onProgress ->
            onProgress(100L)
            onProgress(200L)
            onProgress(300L)
        }

    @Test
    fun uploadingReturnsTheDesktopsOwnFilePath() = runTest {
        val response = uploadService().uploadMedia(picked()) { }.getOrThrow()

        assertTrue(response.ok)
        assertEquals("/Users/av/Movies/clip.mp4", response.path)
        assertEquals("local", response.mediaType)
    }

    @Test
    fun theFileNameTravelsAsAQueryParameter() = runTest {
        var url = ""

        uploadService(capture = { url = it }).uploadMedia(picked("sermon clip.mp4")) { }.getOrThrow()

        assertTrue(url.contains("name="), url)
        assertTrue(url.contains("clip"), url)
    }

    @Test
    fun aNameNeedingEncodingDoesNotBreakTheUrl() = runTest {
        // Picked file names carry spaces and ampersands routinely.
        var url = ""

        uploadService(capture = { url = it }).uploadMedia(picked("a&b c.mp4")) { }.getOrThrow()

        assertFalse(url.contains(" "), url)
    }

    @Test
    fun progressIsReportedAsAFractionOfTheWhole() = runTest {
        val fractions = mutableListOf<Float>()

        uploadService().uploadMedia(picked(size = 300L)) { fractions += it }.getOrThrow()

        assertEquals(listOf(1f / 3f, 2f / 3f, 1f), fractions)
    }

    @Test
    fun progressStaysWithinZeroToOneEvenIfMoreIsSentThanExpected() = runTest {
        // A file whose reported size is short of what it streams must not drive a
        // progress bar past full.
        val fractions = mutableListOf<Float>()
        val overrun = PickedMediaFile("clip.mp4", 100L) { _, onProgress -> onProgress(500L) }

        uploadService().uploadMedia(overrun) { fractions += it }.getOrThrow()

        assertEquals(listOf(1f), fractions)
    }

    @Test
    fun aFileOfUnknownSizeReportsNoProgressRatherThanDividingByZero() = runTest {
        val fractions = mutableListOf<Float>()
        val unknown = PickedMediaFile("clip.mp4", 0L) { _, onProgress -> onProgress(10L) }

        uploadService().uploadMedia(unknown) { fractions += it }.getOrThrow()

        assertTrue(fractions.isEmpty())
    }

    @Test
    fun aRejectedUploadIsAFailureCarryingTheServersReason() = runTest {
        val error = uploadService(status = HttpStatusCode.PayloadTooLarge, body = "Too large")
            .uploadMedia(picked()) { }
            .exceptionOrNull()

        assertIs<ApiException>(error)
        assertEquals(413, error.httpStatus)
        assertEquals("Too large", error.reason)
    }
}
