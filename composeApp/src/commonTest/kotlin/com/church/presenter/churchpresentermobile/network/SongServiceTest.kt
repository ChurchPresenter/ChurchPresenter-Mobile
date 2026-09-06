package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests [SongService.getSongs] (catalog flatten) and [SongService.getSongDetail] (2-pass parse). */
class SongServiceTest {

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK): SongService {
        val settings = AppSettings(InMemorySettingsStorage())
        return SongService(settings, ServerEventService(settings), mockClient { respond(body, status) })
    }

    @Test
    fun getSongsFlattensBooksAndStampsBookName() = runTest {
        val body = """
            {"song-book":[
              {"book-name":"Hymns","song-total":2,"songs":[
                {"id":1,"number":"1","title":"Amazing Grace"},
                {"id":2,"number":"2","title":"How Great"}
              ]},
              {"book-name":"Chorus","song-total":1,"songs":[
                {"id":3,"number":"10","title":"Shout"}
              ]}
            ]}
        """.trimIndent()
        val songs = service(body).getSongs().getOrThrow()
        assertEquals(3, songs.size)
        assertEquals("Amazing Grace", songs[0].title)
        assertEquals("Hymns", songs[0].bookName)
        assertEquals("Chorus", songs[2].bookName)
    }

    @Test
    fun getSongsNonSuccessIsFailure() = runTest {
        assertTrue(service("boom", HttpStatusCode.ServiceUnavailable).getSongs().isFailure)
    }

    @Test
    fun getSongDetailParsesVersesAndBookName() = runTest {
        val body = """
            {"number":"1","title":"Amazing Grace","book-name":"Hymns",
             "verses":[{"number":1,"lines":["Amazing grace","how sweet"]}]}
        """.trimIndent()
        val detail = service(body).getSongDetail("1", "Hymns").getOrThrow()
        assertEquals("Hymns", detail.bookName)
        assertTrue(detail.hasLyrics)
        assertEquals(1, detail.allVerses.size)
        assertEquals("Amazing grace\nhow sweet", detail.allVerses[0].displayText)
    }

    @Test
    fun getSongDetailFlexibleScanRecoversVersesFromUnknownKey() = runTest {
        // No recognised verse container — the flexible second pass should still find lyrics.
        val body = """
            {"number":"1","title":"T","customStanzas":[{"lines":["hello world"]}]}
        """.trimIndent()
        val detail = service(body).getSongDetail("1", null).getOrThrow()
        assertTrue(detail.hasLyrics)
        assertEquals("hello world", detail.allVerses.first().displayText)
    }

    @Test
    fun getSongDetailNonSuccessIsFailure() = runTest {
        assertTrue(service("nope", HttpStatusCode.NotFound).getSongDetail("1", null).isFailure)
    }

    // ── getSongDetail: request shape ─────────────────────────────────────

    @Test
    fun getSongDetailAsksForTheNumberedSong() = runTest {
        var requested = ""
        val settings = AppSettings(InMemorySettingsStorage())
        val svc = SongService(settings, FakeWsSender(), mockClient { path ->
            requested = path
            respond("""{"number":"42","title":"Grace","text":"line"}""")
        })

        svc.getSongDetail("42", null, -1, null).getOrThrow()

        assertTrue(requested.endsWith("/songs/42"), "requested $requested")
    }

    @Test
    fun getSongDetailWithNoNumberUsesThePlaceholderSegment() = runTest {
        // A library song has no songbook number; the desktop resolves it by title
        // instead, and the path still has to be well-formed.
        var requested = ""
        val settings = AppSettings(InMemorySettingsStorage())
        val svc = SongService(settings, FakeWsSender(), mockClient { path ->
            requested = path
            respond("""{"title":"Grace","text":"line"}""")
        })

        svc.getSongDetail("", null, -1, "Grace").getOrThrow()

        assertTrue(requested.endsWith("/songs/_"), "requested $requested")
    }

    // ── getSongDetail: the flexible second pass ──────────────────────────
    //
    // Pass 1 is a plain decode covering every @SerialName the model knows
    // ("verses", "stanzas", "text", "body", …). Only when that finds nothing does
    // pass 2 walk every key looking for something verse-shaped — the safety net
    // for an older or customised desktop. So these payloads deliberately use key
    // names the model does NOT recognise; a recognised one would never get here.

    @Test
    fun lyricsUnderAnUnrecognisedArrayKeyAreStillFound() = runTest {
        val body = """{"number":"42","title":"Grace","blocks":[{"label":"Verse 1","lines":["Amazing grace"]}]}"""

        val detail = service(body).getSongDetail("42", null, -1, null).getOrThrow()

        assertTrue(detail.hasLyrics, "the flexible scan should have found the blocks array")
        assertEquals(1, detail.allVerses.size)
    }

    @Test
    fun anArrayOfBlankVersesIsNotMistakenForLyrics() = runTest {
        // Decodes fine but says nothing — must not count as having found lyrics,
        // or the song opens to a set of empty slides.
        val body = """{"number":"42","title":"Grace","blocks":[{"label":"Verse 1","lines":[]}]}"""

        val detail = service(body).getSongDetail("42", null, -1, null).getOrThrow()

        assertFalse(detail.hasLyrics)
    }

    @Test
    fun plainMultiLineLyricsUnderAnUnrecognisedStringKeyAreFound() = runTest {
        val lyrics = "Amazing grace, how sweet the sound\\nThat saved a wretch like me\\nI once was lost"
        val body = """{"number":"42","title":"Grace","freeform":"$lyrics"}"""

        val detail = service(body).getSongDetail("42", null, -1, null).getOrThrow()

        assertTrue(detail.hasLyrics)
    }

    @Test
    fun aShortSingleLineStringIsNotMistakenForLyrics() = runTest {
        // The scan wants multi-line text over 30 characters, so an author or a
        // copyright line is never projected as the song.
        val body = """{"number":"42","title":"Grace","credit":"John Newton","note":"Public domain"}"""

        val detail = service(body).getSongDetail("42", null, -1, null).getOrThrow()

        assertFalse(detail.hasLyrics)
    }

    @Test
    fun theLongestCandidateStringWins() = runTest {
        val short = "Line one here padded out\\nLine two here padded"
        val long = "Amazing grace, how sweet the sound\\nThat saved a wretch like me\\n" +
            "I once was lost, but now am found"
        val body = """{"number":"42","title":"Grace","aside":"$short","freeform":"$long"}"""

        val detail = service(body).getSongDetail("42", null, -1, null).getOrThrow()

        assertTrue(detail.plainText!!.contains("but now am found"), detail.plainText!!)
    }

    @Test
    fun versesAreCheckedBeforePlainText() = runTest {
        // Structured verses project as separate slides; a flat string does not,
        // so the array has to win when an unrecognised payload carries both.
        val lyrics = "Amazing grace, how sweet the sound\\nThat saved a wretch like me"
        val body = """
            {"number":"42","title":"Grace",
             "blocks":[{"label":"Verse 1","lines":["Amazing grace"]}],
             "freeform":"$lyrics"}
        """.trimIndent()

        val detail = service(body).getSongDetail("42", null, -1, null).getOrThrow()

        assertEquals(1, detail.allVerses.size)
        assertEquals(null, detail.plainText, "plain text must not be filled in when verses were found")
    }

    @Test
    fun aPayloadWithNothingLyricLikeReturnsTheBaseSongUnchanged() = runTest {
        val body = """{"number":"42","title":"Grace"}"""

        val detail = service(body).getSongDetail("42", null, -1, null).getOrThrow()

        assertFalse(detail.hasLyrics)
        assertEquals("Grace", detail.title)
    }

    @Test
    fun aRecognisedPayloadNeverReachesTheFlexibleScan() = runTest {
        // Pass 1 succeeding short-circuits, so a stray array elsewhere in the
        // payload cannot overwrite verses the model already understood.
        val body = """
            {"number":"42","title":"Grace",
             "verses":[{"label":"Verse 1","lines":["Amazing grace"]}],
             "blocks":[{"label":"Wrong","lines":["Should not win"]}]}
        """.trimIndent()

        val detail = service(body).getSongDetail("42", null, -1, null).getOrThrow()

        assertTrue(
            detail.allVerses.none { it.displayText.contains("Should not win") },
            detail.allVerses.toString(),
        )
    }

    // ── WebSocket actions ────────────────────────────────────────────────

    private fun wsService(ws: FakeWsSender): SongService =
        SongService(AppSettings(InMemorySettingsStorage()), ws, mockClient { respond("{}") })

    @Test
    fun selectSongSendsTheSongTheDesktopWillOpen() = runTest {
        val ws = FakeWsSender()
        val song = Song(id = 1, number = "42", title = "Amazing Grace", bookName = "Hymns")

        wsService(ws).selectSong(song).getOrThrow()

        assertEquals(WsMessageType.SELECT_SONG, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"songNumber\":42"), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"songbook\":\"Hymns\""), ws.lastPayload)
        assertTrue(ws.calls.last().third, "opening a song should not wait for an ack")
    }

    @Test
    fun aNonNumericSongNumberIsSentAsZero() = runTest {
        // Hymnals use "10b"; the desktop's numeric field cannot carry that, so the
        // string id is what identifies it and the number degrades to 0.
        val ws = FakeWsSender()
        val song = Song(id = 1, number = "10b", title = "Be Thou My Vision")

        wsService(ws).selectSong(song).getOrThrow()

        assertTrue(ws.lastPayload.contains("\"songNumber\":0"), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"id\":\"10b\""), ws.lastPayload)
    }

    @Test
    fun selectVerseSendsTheSectionIndex() = runTest {
        val ws = FakeWsSender()

        wsService(ws).selectVerse("42", "Hymns", verseIndex = 3).getOrThrow()

        assertEquals(WsMessageType.SELECT_SONG_SECTION, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"section\":3"), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"number\":\"42\""), ws.lastPayload)
        assertTrue(ws.calls.last().third)
    }

    @Test
    fun aSocketFailureIsReportedByBoth() = runTest {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("socket closed"))

        assertTrue(wsService(ws).selectSong(Song(id = 1, number = "1", title = "T")).isFailure)
        assertTrue(wsService(ws).selectVerse("1", null, 0).isFailure)
    }
}
