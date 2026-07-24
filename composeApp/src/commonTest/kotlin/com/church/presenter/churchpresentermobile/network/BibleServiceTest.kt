package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
