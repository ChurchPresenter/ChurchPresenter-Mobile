package com.church.presenter.churchpresentermobile.tv

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What a phone sends a TV, and what it makes of the answer. */
class TvControlTest {

    private val tv = FoundService("Lobby", "192.168.1.30", TV_CONTROL_PORT)
    private val source = TvSource("Main hall", "192.168.1.10", 8765, SourceKind.DESKTOP, output = 2, apiKey = "k")
    private val seen = mutableListOf<HttpRequestData>()

    private fun client(status: HttpStatusCode) = HttpClient(
        MockEngine { request ->
            seen += request
            respond("", status)
        },
    )

    @Test
    fun showingOnATvPostsTheSourceItCanReadBack() = runTest {
        val result = showOnTv(client(HttpStatusCode.OK), tv, source)

        assertTrue(result.isSuccess)
        val request = seen.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("http://192.168.1.30:$TV_CONTROL_PORT$TV_SHOW_PATH", request.url.toString())
        assertEquals(source, readShowRequest((request.body as TextContent).text))
    }

    @Test
    fun aTvThatRefusesToShowIsAFailure() = runTest {
        assertTrue(showOnTv(client(HttpStatusCode.InternalServerError), tv, source).isFailure)
    }

    @Test
    fun clearingATvPostsToItsHomePath() = runTest {
        val result = clearTv(client(HttpStatusCode.OK), tv)

        assertTrue(result.isSuccess)
        assertEquals("http://192.168.1.30:$TV_CONTROL_PORT$TV_HOME_PATH", seen.single().url.toString())
    }

    @Test
    fun aTvThatRefusesToClearIsAFailure() = runTest {
        assertTrue(clearTv(client(HttpStatusCode.ServiceUnavailable), tv).isFailure)
    }
}
