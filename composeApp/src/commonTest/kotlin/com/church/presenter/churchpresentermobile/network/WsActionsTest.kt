package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AnnouncementItemPayload
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.MediaItemPayload
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.StrongsEntry
import com.church.presenter.churchpresentermobile.model.WebsiteItemPayload
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the WebSocket *action* methods across services: correct message type,
 * payload contents, fire-and-forget flag, and failure propagation. Uses a
 * [FakeWsSender] so no socket is involved; the (unused) HTTP client is mocked.
 */
class WsActionsTest {

    private val settings get() = AppSettings(InMemorySettingsStorage())
    private val unusedClient get() = mockClient { respond("", HttpStatusCode.OK) }

    // ── SongService ──────────────────────────────────────────────────────────

    @Test
    fun songSelectProjectScheduleAndSection() = runTest {
        val ws = FakeWsSender()
        val svc = SongService(settings, ws, unusedClient)
        val song = Song(id = 1, number = "42", title = "Amazing Grace", bookName = "Hymns")

        svc.selectSong(song).getOrThrow()
        assertEquals(WsMessageType.SELECT_SONG, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"songNumber\":42"))
        assertTrue(ws.lastPayload.contains("Amazing Grace"))

        svc.projectSong(song).getOrThrow()
        assertEquals(WsMessageType.PROJECT, ws.lastType)
        assertTrue(ws.lastPayload.contains("42 - Amazing Grace")) // displayText built from number+title

        svc.addSongToSchedule(song).getOrThrow()
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)

        svc.selectVerse("42", "Hymns", 2).getOrThrow()
        assertEquals(WsMessageType.SELECT_SONG_SECTION, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"section\":2"))

        svc.clearDisplay().getOrThrow()
        assertEquals(WsMessageType.CLEAR, ws.lastType)
    }

    @Test
    fun songActionPropagatesFailure() = runTest {
        val ws = FakeWsSender().apply { failWith(RuntimeException("ws down")) }
        val svc = SongService(settings, ws, unusedClient)
        assertTrue(svc.projectSong(Song(number = "1", title = "T")).isFailure)
    }

    // ── BibleService ─────────────────────────────────────────────────────────

    @Test
    fun bibleSelectHoldAndClear() = runTest {
        val ws = FakeWsSender()
        val svc = BibleService(settings, ws, unusedClient)

        svc.selectBibleVerse("John", 3, 16, "For God so loved", verseRange = "16-18").getOrThrow()
        assertEquals(WsMessageType.SELECT_BIBLE_VERSE, ws.lastType)
        assertTrue(ws.lastPayload.contains("John"))
        assertTrue(ws.lastPayload.contains("16-18"))

        svc.setBibleHold(true).getOrThrow()
        assertEquals(WsMessageType.BIBLE_HOLD, ws.lastType)
        assertEquals("""{"hold":true}""", ws.lastPayload)

        svc.clearDisplay().getOrThrow()
        assertEquals(WsMessageType.CLEAR, ws.lastType)
    }

    @Test
    fun bibleProjectAndScheduleRange() = runTest {
        val ws = FakeWsSender()
        val svc = BibleService(settings, ws, unusedClient)

        svc.projectBibleVerse("John", 3, 16, "For God so loved").getOrThrow()
        assertEquals(WsMessageType.PROJECT, ws.lastType)
        assertTrue(ws.lastPayload.contains("John"))

        // Contiguous verses -> "16-17" range in the schedule payload.
        svc.addBibleToSchedule("John", 3, listOf(BibleVerse(verse = 16), BibleVerse(verse = 17))).getOrThrow()
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
        assertTrue(ws.lastPayload.contains("16-17"))

        // Empty selection is a no-op success (no frame sent).
        val callsBefore = ws.calls.size
        assertTrue(svc.addBibleToSchedule("John", 3, emptyList()).isSuccess)
        assertEquals(callsBefore, ws.calls.size)
    }

    // ── PicturesService ──────────────────────────────────────────────────────

    @Test
    fun pictureSelectScheduleAndClear() = runTest {
        val ws = FakeWsSender()
        val svc = PicturesService(settings, ws, unusedClient)

        svc.selectPicture("f1", fileName = "a.jpg", indexFallback = 3).getOrThrow()
        assertEquals(WsMessageType.SELECT_PICTURE, ws.lastType)
        assertTrue(ws.lastPayload.contains("f1"))

        svc.addToSchedule("f1", imageIndex = 2, displayText = "Sunset").getOrThrow()
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
        assertTrue(ws.lastPayload.contains("Sunset"))

        svc.clearDisplay().getOrThrow()
        assertEquals(WsMessageType.CLEAR, ws.lastType)
    }

    // ── DictionaryService ────────────────────────────────────────────────────

    @Test
    fun dictionaryProjectAndSchedule() = runTest {
        val ws = FakeWsSender()
        val svc = DictionaryService(settings, ws, unusedClient)
        val entry = StrongsEntry(number = "H430", word = "Elohim", definition = "God")

        svc.projectEntry(entry).getOrThrow()
        assertEquals(WsMessageType.PROJECT, ws.lastType)
        assertTrue(ws.lastPayload.contains("H430") || ws.lastPayload.contains("Elohim"))

        svc.addEntryToSchedule(entry).getOrThrow()
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
    }

    // ── WebService (WS-only) ─────────────────────────────────────────────────

    @Test
    fun webProjectScheduleAndClear() = runTest {
        val ws = FakeWsSender()
        val svc = WebService(ws)
        val page = WebsiteItemPayload(url = "https://example.com", websiteTitle = "Example")

        svc.projectPage(page).getOrThrow()
        assertEquals(WsMessageType.PROJECT, ws.lastType)
        assertTrue(ws.lastPayload.contains("example.com"))

        svc.addToSchedule(page).getOrThrow()
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)

        svc.clearScreen().getOrThrow()
        assertEquals(WsMessageType.CLEAR, ws.lastType)
        assertTrue(ws.calls.last().third) // fireAndForget
    }

    // ── AnnouncementService (WS-only) ────────────────────────────────────────

    @Test
    fun announcementShowScheduleAndClear() = runTest {
        val ws = FakeWsSender()
        val svc = AnnouncementService(ws)
        val item = AnnouncementItemPayload(id = "", announcementText = "Coffee after service")

        svc.showOnScreen(item).getOrThrow()
        assertEquals(WsMessageType.PROJECT, ws.lastType)
        assertTrue(ws.lastPayload.contains("Coffee after service"))

        svc.addToSchedule(item).getOrThrow()
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)

        svc.clearScreen().getOrThrow()
        assertEquals(WsMessageType.CLEAR, ws.lastType)
    }

    @Test
    fun webActionPropagatesFailure() = runTest {
        val ws = FakeWsSender().apply { failWith(RuntimeException("ws down")) }
        assertTrue(WebService(ws).clearScreen().isFailure)
    }

    // ── MediaCastService (project/schedule/clear + transport controls) ───────

    @Test
    fun mediaGoLiveScheduleClearAndTransport() = runTest {
        val ws = FakeWsSender()
        val svc = MediaCastService(settings, ws)
        val item = MediaItemPayload(mediaUrl = "https://h/clip.mp4", mediaTitle = "Clip")

        svc.goLive(item).getOrThrow()
        assertEquals(WsMessageType.PROJECT, ws.lastType)
        assertTrue(ws.lastPayload.contains("clip.mp4"))

        svc.addToSchedule(item).getOrThrow()
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)

        svc.clearScreen().getOrThrow(); assertEquals(WsMessageType.CLEAR, ws.lastType)
        svc.playPause().getOrThrow(); assertEquals(WsMessageType.MEDIA_PLAY_PAUSE, ws.lastType)
        svc.stop().getOrThrow(); assertEquals(WsMessageType.MEDIA_STOP, ws.lastType)
        svc.seekForward().getOrThrow(); assertEquals(WsMessageType.MEDIA_SEEK_FORWARD, ws.lastType)
        svc.seekBackward().getOrThrow(); assertEquals(WsMessageType.MEDIA_SEEK_BACKWARD, ws.lastType)
        svc.seekTo(1500).getOrThrow()
        assertEquals(WsMessageType.MEDIA_SEEK_TO, ws.lastType); assertEquals("1500", ws.lastPayload)
        svc.setVolume(0.5f).getOrThrow(); assertEquals(WsMessageType.MEDIA_SET_VOLUME, ws.lastType)
        svc.muteToggle().getOrThrow(); assertEquals(WsMessageType.MEDIA_MUTE_TOGGLE, ws.lastType)
        assertTrue(ws.calls.last().third) // transport controls are fire-and-forget
    }

    // ── PresentationService (WS actions) ─────────────────────────────────────

    @Test
    fun presentationSelectScheduleAndClear() = runTest {
        val ws = FakeWsSender()
        val svc = PresentationService(settings, ws, unusedClient)

        svc.selectPresentation("p1", 3).getOrThrow()
        assertEquals(WsMessageType.SELECT_SLIDE, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"index\":3"))

        svc.addToSchedule(Presentation(id = "p1", fileName = "Sermon.pptx")).getOrThrow()
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
        assertTrue(ws.lastPayload.contains("Sermon.pptx"))

        svc.clearDisplay().getOrThrow()
        assertEquals(WsMessageType.CLEAR, ws.lastType)
    }
}
