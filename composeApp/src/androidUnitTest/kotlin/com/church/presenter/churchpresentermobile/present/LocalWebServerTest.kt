package com.church.presenter.churchpresentermobile.present

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The phone's own web server, started for real on a loopback port.
 *
 * This is the audience screen in standalone mode: a TV, a laptop wired to a
 * projector, or a spare phone opens the URL and becomes a display. Nothing is
 * fetched from the internet, which is the whole point — a hall's Wi-Fi often
 * has no uplink at all.
 *
 * Run against a live CIO server and a real client rather than mocked, because
 * every failure this guards against is a protocol-level one: a path that 404s
 * because the browser appended a cache-buster, a photo refetched on every
 * reconnect for want of a cache header, a display that connects between slides
 * and sits blank because nothing was replayed to it.
 *
 * `runBlocking`, not `runTest`: the server's own bind timeout is a real one, and
 * a virtual clock would expire it before the socket ever opened.
 */
class LocalWebServerTest {

    private val page = "<!doctype html><title>Display</title>".encodeToByteArray()
    private val script = "console.log('display')".encodeToByteArray()
    private val photoBytes = byteArrayOf(1, 2, 3, 4)

    private val assets = WebAssets(
        mapOf(
            "index.html" to WebAsset(page, "text/html; charset=utf-8"),
            "app.js" to WebAsset(script, "application/javascript; charset=utf-8"),
        )
    )

    private val photos = PhotoSource { id ->
        if (id == "p1") ServedPhoto(photoBytes, "image/jpeg") else null
    }

    private val client = HttpClient(OkHttp) { install(WebSockets) }

    private val servers = mutableListOf<LocalWebServer>()

    @AfterTest
    fun stopEverything() = runBlocking {
        servers.forEach { it.stop() }
        client.close()
    }

    /** A running server and the port it bound, cleaned up after the test. */
    private suspend fun serving(
        preferredPort: Int = 0,
        candidates: List<Int> = emptyList(),
    ): Pair<LocalWebServer, Int> {
        val server = LocalWebServer(assets, photos).also { servers += it }
        return server to server.start(preferredPort, candidates)
    }

    // ── Binding a port ───────────────────────────────────────────────────

    @Test
    fun `the server binds a port and says which`() = runBlocking {
        val (_, port) = serving()

        assertTrue(port > 0, "no port was reported")
    }

    @Test
    fun `starting an already running server returns the same port`() = runBlocking {
        // attach() is called on every mode change; a second bind would take a
        // different port and change the URL the room was just given.
        val (server, port) = serving()

        assertEquals(port, server.start(0, emptyList()))
    }

    @Test
    fun `a port already taken is stepped over rather than giving up`() = runBlocking {
        // The failure this replaced: an occupied 8766 left the sink in ERROR
        // instead of moving to 8767, because CIO reports a failed bind by
        // cancelling its own engine job.
        val (_, taken) = serving()

        val (_, second) = serving(preferredPort = taken)

        assertTrue(second != taken, "the second server rebound the occupied port")
        assertTrue(second > 0)
    }

    @Test
    fun `a preferred port that is free is the one used`() = runBlocking {
        // The port is remembered between services so the operator can write the
        // URL down once.
        val (_, first) = serving()
        val server = LocalWebServer(assets, photos).also { servers += it }
        val firstServerStopped = servers.first().also { it.stop() }
        assertTrue(firstServerStopped === servers.first())

        assertEquals(first, server.start(preferredPort = first, candidates = emptyList()))
    }

    @Test
    fun `stopping releases the port`() = runBlocking {
        val (server, port) = serving()

        server.stop()

        val other = LocalWebServer(assets, photos).also { servers += it }
        assertEquals(port, other.start(preferredPort = port, candidates = emptyList()))
    }

    @Test
    fun `stopping a server that never started is harmless`() = runBlocking {
        LocalWebServer(assets, photos).stop()
    }

    // ── Serving the display page ─────────────────────────────────────────

    @Test
    fun `the root path serves the page`() = runBlocking {
        val (_, port) = serving()

        val response = client.get("http://127.0.0.1:$port/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(page.decodeToString(), response.bodyAsText())
    }

    @Test
    fun `index dot html serves the same page`() = runBlocking {
        val (_, port) = serving()

        assertEquals(page.decodeToString(), client.get("http://127.0.0.1:$port/index.html").bodyAsText())
    }

    @Test
    fun `the page is served as HTML so a browser renders it`() = runBlocking {
        // Served as octet-stream a browser downloads the file instead.
        val (_, port) = serving()

        val contentType = client.get("http://127.0.0.1:$port/").headers[HttpHeaders.ContentType]

        assertTrue(contentType?.startsWith("text/html") == true, "got $contentType")
    }

    @Test
    fun `a script is served under its own name`() = runBlocking {
        val (_, port) = serving()

        assertEquals(script.decodeToString(), client.get("http://127.0.0.1:$port/app.js").bodyAsText())
    }

    @Test
    fun `a script referenced from a subfolder still resolves`() = runBlocking {
        // The bundled page asks for /assets/app.js; the files are held flat.
        val (_, port) = serving()

        assertEquals(script.decodeToString(), client.get("http://127.0.0.1:$port/assets/app.js").bodyAsText())
    }

    @Test
    fun `a cache-busting query string does not 404`() = runBlocking {
        // A TV browser appends one on reload; answering 404 leaves a blank screen.
        val (_, port) = serving()

        assertEquals(HttpStatusCode.OK, client.get("http://127.0.0.1:$port/app.js?v=12345").status)
    }

    @Test
    fun `a file that is not bundled is a plain 404`() = runBlocking {
        val (_, port) = serving()

        val response = client.get("http://127.0.0.1:$port/favicon.ico")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── Serving the operator's photos ────────────────────────────────────

    @Test
    fun `a picked photo is served by its id`() = runBlocking {
        val (_, port) = serving()

        val response = client.get("http://127.0.0.1:$port/$PHOTO_ROUTE/p1")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(photoBytes.toList(), response.bodyAsText().encodeToByteArray().toList())
    }

    @Test
    fun `a photo is served as an image, not as a download`() = runBlocking {
        val (_, port) = serving()

        val contentType = client.get("http://127.0.0.1:$port/$PHOTO_ROUTE/p1").headers[HttpHeaders.ContentType]

        assertTrue(contentType?.startsWith("image/jpeg") == true, "got $contentType")
    }

    @Test
    fun `a photo is marked immutable so a TV does not refetch it every reconnect`() = runBlocking {
        // The id is a UUID minted when the photo was picked, so the bytes behind
        // a URL can never change.
        val (_, port) = serving()

        val cacheControl = client.get("http://127.0.0.1:$port/$PHOTO_ROUTE/p1").headers[HttpHeaders.CacheControl]

        assertTrue(cacheControl?.contains("immutable") == true, "got $cacheControl")
    }

    @Test
    fun `an unknown photo id is a 404, not the display page`() = runBlocking {
        // The photo route is declared before the catch-all for exactly this: a
        // missing photo answered with the HTML page renders as garbage.
        val (_, port) = serving()

        val response = client.get("http://127.0.0.1:$port/$PHOTO_ROUTE/nope")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── The live slide feed ──────────────────────────────────────────────

    @Test
    fun `a display connecting mid-service is sent what is already on screen`() = runBlocking {
        // The reason the feed is a state flow rather than a queue: a TV switched
        // on between songs would otherwise sit blank until the next slide change.
        val (server, port) = serving()
        server.publish("""{"slide":"verse one"}""")

        var first = ""
        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:$port/live") {
                first = (incoming.receive() as Frame.Text).readText()
            }
        }

        assertEquals("""{"slide":"verse one"}""", first)
    }

    @Test
    fun `a slide published while a display is watching reaches it`() = runBlocking {
        val (server, port) = serving()
        server.publish("""{"slide":"verse one"}""")

        var second = ""
        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:$port/live") {
                (incoming.receive() as Frame.Text).readText()
                server.publish("""{"slide":"verse two"}""")
                second = (incoming.receive() as Frame.Text).readText()
            }
        }

        assertEquals("""{"slide":"verse two"}""", second)
    }

    @Test
    fun `a display that connects before anything is projected simply waits`() = runBlocking {
        // Nothing is sent until there is something to send, rather than a blank
        // frame the page would have to know to ignore.
        val (server, port) = serving()

        var frame = ""
        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:$port/live") {
                server.publish("""{"slide":"first thing"}""")
                frame = (incoming.receive() as Frame.Text).readText()
            }
        }

        assertEquals("""{"slide":"first thing"}""", frame)
    }

    @Test
    fun `a connected display is counted`() = runBlocking {
        // The outputs sheet shows this number, and it is how the operator knows
        // the TV actually opened the page.
        val (server, port) = serving()

        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:$port/live") {
                assertEquals(1, server.clientCount.first { it == 1 })
            }
        }
    }

    @Test
    fun `several displays are all counted`() = runBlocking {
        // The design point of a state flow per session rather than a queue: a
        // TV, a laptop on the projector and a spare phone can all watch at once
        // and stay in step.
        val (server, port) = serving()

        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:$port/live") {
                server.clientCount.first { it == 1 }
                client.webSocket("ws://127.0.0.1:$port/live") {
                    assertEquals(2, server.clientCount.first { it == 2 })
                }
            }
        }
    }

    @Test
    fun `stopping the server forgets its displays`() = runBlocking {
        val (server, port) = serving()
        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:$port/live") { server.clientCount.first { it == 1 } }
        }

        server.stop()

        assertEquals(0, server.clientCount.value)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
