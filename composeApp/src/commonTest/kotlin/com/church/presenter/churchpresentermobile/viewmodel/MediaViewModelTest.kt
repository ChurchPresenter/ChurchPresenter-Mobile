package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the media screen: the title it guesses, the kind it reports, and the
 * payload it puts on the wire.
 *
 * The desktop infers a media item from `mediaUrl` and decides from `mediaType`
 * whether to stream a URL or open a local file, so both have to be right.
 */
class MediaViewModelTest {

    private fun vm(): Pair<MediaViewModel, FakeWsSender> {
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        return MediaViewModel(settings, ServerEventService(settings), ws) to ws
    }

    // ── mediaTitleFrom (pure) ────────────────────────────────────────────

    @Test
    fun aTitleIsTheFileNameWithoutItsExtension() {
        assertEquals("sermon clip", mediaTitleFrom("https://example.org/sermon_clip.mp4"))
        assertEquals("week one", mediaTitleFrom("https://example.org/media/week-one.mov"))
    }

    @Test
    fun aQueryStringOrFragmentIsNotPartOfTheTitle() {
        assertEquals("clip", mediaTitleFrom("https://example.org/clip.mp4?token=abc"))
        assertEquals("clip", mediaTitleFrom("https://example.org/clip.mp4#t=30"))
    }

    @Test
    fun `a url with no file names itself after the host, minus the TLD`() {
        // Documents current behaviour rather than ideal: with no path segment the
        // host becomes the "file name", and substringBeforeLast('.') then treats
        // the TLD as an extension. "example.org/" reads as "example", and
        // "www.example.org" as "www.example". Never blank, which is what the
        // caller relies on, but not the domain either.
        assertEquals("example", mediaTitleFrom("https://example.org/"))
        assertEquals("www.example", mediaTitleFrom("https://www.example.org"))
    }

    @Test
    fun aTitleIsNeverBlank() {
        // The label is shown as-is in the media row, so an empty one leaves a
        // nameless entry the operator cannot identify.
        for (url in listOf("https://example.org/", "https://www.example.org", "https://example.org/a.mp4")) {
            assertTrue(mediaTitleFrom(url).isNotBlank(), url)
        }
    }

    // ── mediaKindFrom (pure) ─────────────────────────────────────────────

    @Test
    fun eachExtensionMapsToTheKindTheSubtitleShows() {
        assertEquals("Video", mediaKindFrom("https://example.org/a.mp4"))
        assertEquals("Video", mediaKindFrom("https://example.org/a.mkv"))
        assertEquals("Audio", mediaKindFrom("https://example.org/a.mp3"))
        assertEquals("Audio", mediaKindFrom("https://example.org/a.opus"))
        assertEquals("Media", mediaKindFrom("https://example.org/a.pdf"))
        assertEquals("Media", mediaKindFrom("https://example.org/stream"))
    }

    @Test
    fun theKindIsReadCaseInsensitivelyAndIgnoresAQueryString() {
        assertEquals("Video", mediaKindFrom("https://example.org/A.MP4"))
        assertEquals("Audio", mediaKindFrom("https://example.org/a.mp3?x=1"))
    }

    // ── Defaults ─────────────────────────────────────────────────────────

    @Test
    fun `the screen starts on the url source with nothing entered`() = runVmTestUnconfined {
        val (vm, _) = vm()
        try {
            assertEquals(MediaSource.URL, vm.source.value)
            assertEquals("", vm.url.value)
            assertNull(vm.uploaded.value)
            assertNull(vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── The payload ──────────────────────────────────────────────────────

    @Test
    fun `a typed url goes live as a url-typed item`() = runVmTestUnconfined {
        val (vm, ws) = vm()
        try {
            vm.setUrl("https://example.org/clip.mp4")

            vm.goLive()

            assertEquals(WsMessageType.PROJECT, ws.lastType)
            assertTrue(ws.lastPayload.contains("\"mediaUrl\":\"https://example.org/clip.mp4\""), ws.lastPayload)
            assertTrue(ws.lastPayload.contains("\"mediaType\":\"url\""), ws.lastPayload)
            assertTrue(ws.lastPayload.contains("\"mediaTitle\":\"clip\""), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a bare domain is sent as https`() = runVmTestUnconfined {
        // Typed on a phone keyboard, the scheme is the first thing to be left off.
        val (vm, ws) = vm()
        try {
            vm.setUrl("example.org/clip.mp4")

            vm.goLive()

            assertTrue(ws.lastPayload.contains("\"mediaUrl\":\"https://example.org/clip.mp4\""), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding to schedule sends the same item down the schedule path`() = runVmTestUnconfined {
        val (vm, ws) = vm()
        try {
            vm.setUrl("https://example.org/clip.mp4")

            vm.addToSchedule()

            assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
            assertTrue(ws.lastPayload.contains("https://example.org/clip.mp4"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `going live records what is live so the screen can say so`() = runVmTestUnconfined {
        val (vm, _) = vm()
        try {
            vm.setUrl("https://example.org/clip.mp4")

            vm.goLive()

            assertEquals("https://example.org/clip.mp4", vm.liveUrl.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Nothing to send ──────────────────────────────────────────────────

    @Test
    fun `an empty url is refused with a message rather than sent`() = runVmTestUnconfined {
        val (vm, ws) = vm()
        try {
            vm.goLive()

            assertTrue(ws.calls.isEmpty(), "nothing should reach the socket")
            assertEquals("Enter a media URL first", vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a whitespace-only url counts as empty`() = runVmTestUnconfined {
        val (vm, ws) = vm()
        try {
            vm.setUrl("   ")

            vm.goLive()

            assertTrue(ws.calls.isEmpty())
            assertEquals("Enter a media URL first", vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the upload source with no file names the right thing to fix`() = runVmTestUnconfined {
        // The message has to match the source the user is actually on, or it
        // sends them to the wrong control.
        val (vm, ws) = vm()
        try {
            vm.setSource(MediaSource.UPLOAD)

            vm.goLive()

            assertTrue(ws.calls.isEmpty())
            assertEquals("Upload a file first", vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a url typed while on the upload source is not sent`() = runVmTestUnconfined {
        // The composed source is the one that counts; a leftover URL must not
        // travel when the user has switched to Upload.
        val (vm, ws) = vm()
        try {
            vm.setUrl("https://example.org/clip.mp4")
            vm.setSource(MediaSource.UPLOAD)

            vm.goLive()

            assertTrue(ws.calls.isEmpty())
            assertEquals("Upload a file first", vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Transport controls ───────────────────────────────────────────────
    //
    // Each button forwards one message; the desktop echoes the new playback state
    // back over the socket, so none of them waits for a reply.

    @Test
    fun `each transport button sends its own message`() = runVmTestUnconfined {
        val (vm, ws) = vm()
        try {
            vm.playPause()
            assertEquals(WsMessageType.MEDIA_PLAY_PAUSE, ws.lastType)

            vm.stopPlayback()
            assertEquals(WsMessageType.MEDIA_STOP, ws.lastType)

            vm.seekForward()
            assertEquals(WsMessageType.MEDIA_SEEK_FORWARD, ws.lastType)

            vm.seekBackward()
            assertEquals(WsMessageType.MEDIA_SEEK_BACKWARD, ws.lastType)

            vm.muteToggle()
            assertEquals(WsMessageType.MEDIA_MUTE_TOGGLE, ws.lastType)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `scrubbing carries the position in milliseconds`() = runVmTestUnconfined {
        val (vm, ws) = vm()
        try {
            vm.seekTo(90_000L)

            assertEquals(WsMessageType.MEDIA_SEEK_TO, ws.lastType)
            assertEquals("90000", ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the volume slider carries its level`() = runVmTestUnconfined {
        val (vm, ws) = vm()
        try {
            vm.setVolume(0.25f)

            assertEquals(WsMessageType.MEDIA_SET_VOLUME, ws.lastType)
            assertEquals("0.25", ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `no transport control waits for an acknowledgement`() = runVmTestUnconfined {
        val (vm, ws) = vm()
        try {
            vm.playPause()
            vm.stopPlayback()
            vm.seekTo(0L)
            vm.setVolume(1f)
            vm.muteToggle()

            assertTrue(ws.calls.all { it.third }, "transport controls should be fire-and-forget")
        } finally {
            tearDown(vm)
        }
    }

    // ── Clearing and messages ────────────────────────────────────────────

    @Test
    fun `clearing the screen forgets what was live`() = runVmTestUnconfined {
        val (vm, ws) = vm()
        try {
            vm.setUrl("https://example.org/clip.mp4")
            vm.goLive()
            assertNotNull(vm.liveUrl.value)

            vm.clearScreen()

            assertEquals(WsMessageType.CLEAR, ws.lastType)
            assertNull(vm.liveUrl.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a message can be surfaced and consumed once`() = runVmTestUnconfined {
        // Used for file-picker errors, which are known without asking the desktop.
        val (vm, _) = vm()
        try {
            vm.showMessage("That file is too large")
            assertEquals("That file is too large", vm.message.value)

            vm.clearMessage()

            assertNull(vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `nothing is uploading before a file is picked`() = runVmTestUnconfined {
        val (vm, _) = vm()
        try {
            assertFalse(vm.uploading.value)
            assertEquals(0f, vm.uploadProgress.value)
            assertNull(vm.playback.value, "no desktop is pushing playback state here")
        } finally {
            tearDown(vm)
        }
    }
}
