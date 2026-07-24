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

/** Tests [DictionaryService] search / lookup / getVerses decoding. */
class DictionaryServiceTest {

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK): DictionaryService {
        val settings = AppSettings(InMemorySettingsStorage())
        return DictionaryService(settings, ServerEventService(settings), mockClient { respond(body, status) })
    }

    @Test
    fun searchDecodesEntries() = runTest {
        val body = """[{"number":"H430","word":"Elohim"},{"number":"G26","word":"agape"}]"""
        val entries = service(body).search(query = "love").getOrThrow()
        assertEquals(2, entries.size)
        assertTrue(entries[0].isHebrew)
        assertTrue(entries[1].isGreek)
    }

    @Test
    fun searchNonSuccessIsFailure() = runTest {
        assertTrue(service("x", HttpStatusCode.InternalServerError).search(query = "").isFailure)
    }

    @Test
    fun lookupDecodesSingleEntry() = runTest {
        val e = service("""{"number":"H430","word":"Elohim","occurrences":2606}""").lookup("H430").getOrThrow()
        assertEquals("Elohim", e.word)
        assertEquals(430, e.numericValue)
        assertEquals(2606, e.occurrences)
    }

    @Test
    fun getVersesDecodesResponse() = runTest {
        val body = """{"number":"H430","total":2,"verses":[
            {"bookName":"Genesis","chapter":1,"verse":1,"reference":"Gen 1:1","text":"..."}]}"""
        val r = service(body).getVerses("H430").getOrThrow()
        assertEquals("H430", r.number)
        assertEquals(2, r.total)
        assertEquals(1, r.verses.size)
        assertEquals("Genesis", r.verses[0].bookName)
    }

    @Test
    fun lookupNonSuccessIsFailure() = runTest {
        assertTrue(service("no", HttpStatusCode.NotFound).lookup("H1").isFailure)
    }
}
