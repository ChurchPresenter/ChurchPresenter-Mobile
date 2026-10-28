package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The canvas going live on the desktop: what is sent to `/api/drawing/frame`, and what
 * each answer means for the canvas — including the operator's decision on a Go Live.
 */
class RemoteCanvasProjectorTest {

    private val settings = AppSettings(InMemorySettingsStorage()).apply { apiKey = "secret" }
    private val live = MutableStateFlow<String?>(null)
    private val requests = mutableListOf<HttpRequestData>()

    private fun projector(status: HttpStatusCode = HttpStatusCode.OK, body: String = """{"ok":true,"showing":true}"""):
        RemoteCanvasProjector {
        val client = HttpClient(
            MockEngine { request ->
                requests += request
                respond(body, status, headersOf("Content-Type", "application/json"))
            },
        )
        return RemoteCanvasProjector(settings, live, client)
    }

    @Test
    fun `going live sends the page as a PNG and asks for a take-over`() = runTest {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val result = projector().show(png, "#1B1D26", "Sermon", takeOver = true)

        assertEquals(CanvasShowResult.SHOWN, result)
        val request = requests.single()
        assertEquals("/api/drawing/frame", request.url.encodedPath)
        assertEquals("true", request.url.parameters["takeOver"])
        assertEquals("#1B1D26", request.url.parameters["paper"])
        assertEquals("Sermon", request.url.parameters["title"])
        assertEquals("secret", request.headers["X-Api-Key"])
        assertEquals("image/png", request.body.contentType.toString())
        assertContentEquals(png, request.body.toByteArray())
    }

    @Test
    fun `a refresh is not a take-over and an untitled page sends no title`() = runTest {
        projector().show(byteArrayOf(1), "#FFFFFF", "", takeOver = false)
        val request = requests.single()
        assertEquals("false", request.url.parameters["takeOver"])
        assertFalse(request.url.parameters.contains("title"))
    }

    @Test
    fun `remote asks the operator first`() {
        assertTrue(projector().asksFirst)
    }

    @Test
    fun `the operator declining is told apart from a failure`() = runTest {
        val declined = projector(HttpStatusCode.Forbidden, """{"ok":false,"reason":"denied","showing":false}""")
        assertEquals(CanvasShowResult.DECLINED, declined.show(byteArrayOf(1), "#000000", "", takeOver = true))

        val refused = projector(HttpStatusCode.Forbidden, """{"ok":false,"reason":"not_permitted"}""")
        assertEquals(CanvasShowResult.FAILED, refused.show(byteArrayOf(1), "#000000", "", takeOver = true))
    }

    @Test
    fun `an older desktop without the route is unsupported rather than hanging`() = runTest {
        val old = projector(HttpStatusCode.NotFound, "Not Found")
        assertEquals(CanvasShowResult.UNSUPPORTED, old.show(byteArrayOf(1), "#000000", "", takeOver = true))
    }

    @Test
    fun `an unreachable desktop fails`() = runTest {
        val client = HttpClient(MockEngine { throw IOException("connection refused") })
        val projector = RemoteCanvasProjector(settings, live, client)
        assertEquals(CanvasShowResult.FAILED, projector.show(byteArrayOf(1), "#000000", "", takeOver = true))
        projector.stop() // must not throw either
    }

    @Test
    fun `stop asks the desktop to end the drawing`() = runTest {
        projector(body = """{"ok":true}""").stop()
        assertEquals("/api/drawing/end", requests.single().url.encodedPath)
        assertEquals("secret", requests.single().headers["X-Api-Key"])
    }

    @Test
    fun `showing follows what the desktop says it has live`() = runTest {
        val projector = projector()
        assertFalse(projector.showing.first())
        live.value = DESKTOP_DRAWING
        assertTrue(projector.showing.first())
        live.value = "LYRICS"
        assertFalse(projector.showing.first())
    }

    @Test
    fun `answers are read from the status and the showing flag`() {
        assertEquals(CanvasShowResult.SHOWN, showResultOf(HttpStatusCode.OK, """{"ok":true,"showing":true}"""))
        assertEquals(CanvasShowResult.NOT_SHOWING, showResultOf(HttpStatusCode.OK, """{"ok":true,"showing":false}"""))
        assertEquals(CanvasShowResult.NOT_SHOWING, showResultOf(HttpStatusCode.OK, "not json"))
        assertEquals(CanvasShowResult.DECLINED, showResultOf(HttpStatusCode.Forbidden, """{"reason":"denied"}"""))
        assertEquals(
            CanvasShowResult.FAILED,
            showResultOf(HttpStatusCode.PayloadTooLarge, """{"reason":"too_large"}"""),
        )
        assertEquals(CanvasShowResult.FAILED, showResultOf(HttpStatusCode.Unauthorized, ""))
        assertEquals(CanvasShowResult.UNSUPPORTED, showResultOf(HttpStatusCode.NotFound, "Not Found"))
    }
}
