package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleVerse
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

/** Tests [BibleService.getBooks] / [BibleService.getChapter] decoding. */
class BibleServiceTest {

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK): BibleService {
        val settings = AppSettings(InMemorySettingsStorage())
        return BibleService(settings, ServerEventService(settings), mockClient { respond(body, status) })
    }

    @Test
    fun getBooksParsesBooksKey() = runTest {
        val books = service("""{"books":[{"name":"Genesis","chapter-total":50}]}""").getBooks().getOrThrow()
        assertEquals(1, books.size)
        assertEquals("Genesis", books[0].displayName)
        assertEquals(50, books[0].totalChapters)
    }

    @Test
    fun getBooksFallsBackToBibleKey() = runTest {
        val books = service("""{"bible":[{"name":"John"},{"name":"Acts"}]}""").getBooks().getOrThrow()
        assertEquals(listOf("John", "Acts"), books.map { it.displayName })
    }

    @Test
    fun getBooksNonSuccessIsFailure() = runTest {
        assertTrue(service("boom", HttpStatusCode.InternalServerError).getBooks().isFailure)
    }

    @Test
    fun getChapterParsesVerses() = runTest {
        val body = """{"verses":[{"verse":1,"text":"In the beginning"},{"verse":2,"content":"And the earth"}]}"""
        val verses = service(body).getChapter(1, 1).getOrThrow()
        assertEquals(2, verses.size)
        assertEquals(1, verses[0].number)
        assertEquals("In the beginning", verses[0].displayText)
        assertEquals("And the earth", verses[1].displayText) // content fallback
    }

    @Test
    fun getChapterNonSuccessIsFailure() = runTest {
        assertTrue(service("no", HttpStatusCode.NotFound).getChapter(1, 1).isFailure)
    }

    // ── WebSocket actions ────────────────────────────────────────────────

    private fun wsService(ws: FakeWsSender): BibleService {
        val settings = AppSettings(InMemorySettingsStorage())
        return BibleService(settings, ws, mockClient { respond("{}") })
    }

    private fun verse(number: Int, text: String) = BibleVerse(verse = number, text = text)

    @Test
    fun selectBibleVerseSendsTheVerseOverTheSocket() = runTest {
        val ws = FakeWsSender()

        wsService(ws).selectBibleVerse("Genesis", 1, 1, "In the beginning").getOrThrow()

        assertEquals(WsMessageType.SELECT_BIBLE_VERSE, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"bookName\":\"Genesis\""), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"chapter\":1"), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("In the beginning"), ws.lastPayload)
    }

    @Test
    fun selectBibleVerseIsFireAndForget() = runTest {
        // Browsing verses must feel immediate; waiting on an ack made paging lag.
        val ws = FakeWsSender()

        wsService(ws).selectBibleVerse("Genesis", 1, 1, "text").getOrThrow()

        assertTrue(ws.calls.last().third)
    }

    @Test
    fun clearDisplaySendsClear() = runTest {
        val ws = FakeWsSender()

        wsService(ws).clearDisplay().getOrThrow()

        assertEquals(WsMessageType.CLEAR, ws.lastType)
        assertEquals("", ws.lastPayload)
        assertTrue(ws.calls.last().third)
    }

    @Test
    fun bibleHoldCarriesTheFlagBothWays() = runTest {
        val ws = FakeWsSender()
        val service = wsService(ws)

        service.setBibleHold(true).getOrThrow()
        assertEquals(WsMessageType.BIBLE_HOLD, ws.lastType)
        assertEquals("""{"hold":true}""", ws.lastPayload)

        service.setBibleHold(false).getOrThrow()
        assertEquals("""{"hold":false}""", ws.lastPayload)
    }

    @Test
    fun projectBibleVerseWaitsForTheOperatorsApproval() = runTest {
        // Going live raises a dialog on the desktop, so unlike browsing it cannot
        // be fire-and-forget: the result is what says it was allowed.
        val ws = FakeWsSender()

        wsService(ws).projectBibleVerse("Genesis", 1, 1, "In the beginning").getOrThrow()

        assertEquals(WsMessageType.PROJECT, ws.lastType)
        assertFalse(ws.calls.last().third)
    }

    @Test
    fun aRefusedActionIsReportedAsAFailure() = runTest {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("denied"))

        assertTrue(wsService(ws).projectBibleVerse("Genesis", 1, 1, "t").isFailure)
        assertTrue(wsService(ws).selectBibleVerse("Genesis", 1, 1, "t").isFailure)
    }

    // ── addBibleToSchedule: how a selection is named ─────────────────────

    @Test
    fun addingNoVersesSucceedsWithoutSendingAnything() = runTest {
        // Nothing selected is not a failure to report — there is simply nothing
        // to add, and the caller has already told the user to pick a verse.
        val ws = FakeWsSender()

        wsService(ws).addBibleToSchedule("Genesis", 1, emptyList()).getOrThrow()

        assertTrue(ws.calls.isEmpty())
    }

    @Test
    fun aSingleVerseCarriesNoRange() = runTest {
        val ws = FakeWsSender()

        wsService(ws).addBibleToSchedule("Genesis", 1, listOf(verse(1, "In the beginning"))).getOrThrow()

        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"verseNumber\":1"), ws.lastPayload)
        assertFalse(ws.lastPayload.contains("verseRange"), ws.lastPayload)
    }

    @Test
    fun aContiguousSelectionIsNamedAsAStartToEndRange() = runTest {
        val ws = FakeWsSender()

        wsService(ws).addBibleToSchedule(
            "Genesis", 1,
            listOf(verse(1, "one"), verse(2, "two"), verse(3, "three")),
        ).getOrThrow()

        assertTrue(ws.lastPayload.contains("\"verseRange\":\"1-3\""), ws.lastPayload)
    }

    @Test
    fun aSparseSelectionIsListedRatherThanFlattenedIntoARange() = runTest {
        // "1,3,5" and "1-5" mean different things on the desktop; collapsing the
        // first into the second would project two verses nobody chose.
        val ws = FakeWsSender()

        wsService(ws).addBibleToSchedule(
            "Genesis", 1,
            listOf(verse(1, "one"), verse(3, "three"), verse(5, "five")),
        ).getOrThrow()

        assertTrue(ws.lastPayload.contains("\"verseRange\":\"1,3,5\""), ws.lastPayload)
    }

    @Test
    fun versesAreSortedBeforeBeingNamed() = runTest {
        // Multi-select records tap order, not verse order.
        val ws = FakeWsSender()

        wsService(ws).addBibleToSchedule(
            "Genesis", 1,
            listOf(verse(3, "three"), verse(1, "one"), verse(2, "two")),
        ).getOrThrow()

        assertTrue(ws.lastPayload.contains("\"verseRange\":\"1-3\""), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"verseNumber\":1"), ws.lastPayload)
    }

    @Test
    fun theCombinedTextIsTheVersesInOrder() = runTest {
        val ws = FakeWsSender()

        wsService(ws).addBibleToSchedule(
            "Genesis", 1,
            listOf(verse(2, "second"), verse(1, "first")),
        ).getOrThrow()

        val text = ws.lastPayload.substringAfter("\"verseText\":\"").substringBefore("\"")
        assertTrue(text.startsWith("first"), text)
        assertTrue(text.contains("second"), text)
    }

    @Test
    fun addingWaitsForApproval() = runTest {
        val ws = FakeWsSender()

        wsService(ws).addBibleToSchedule("Genesis", 1, listOf(verse(1, "one"))).getOrThrow()

        assertFalse(ws.calls.last().third)
    }
}
