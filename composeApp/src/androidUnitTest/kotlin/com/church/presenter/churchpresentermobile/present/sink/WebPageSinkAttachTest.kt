package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.LocalWebServer
import com.church.presenter.churchpresentermobile.present.PhotoSource
import com.church.presenter.churchpresentermobile.present.ServedPhoto
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.WebAsset
import com.church.presenter.churchpresentermobile.present.WebAssets
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The browser-screen sink attaching for real, over a live server.
 *
 * This is the whole standalone output path in one object: find the phone's LAN
 * address, load the bundled page, bind a port, hand the operator a URL, hold the
 * locks, and replay whatever is already projected to a display that connects
 * afterwards. Each of those is a separate way for the TV to end up blank while
 * the phone looks fine, so the sink is driven end to end here rather than
 * through its status flow alone.
 *
 * The three things it cannot do off a device — read the LAN address, read the
 * bundled assets — are supplied through the sink's own seams. The server is
 * real, and so is the browser hitting it.
 *
 * `runBlocking`, not `runTest`: the server's bind timeout is a real one.
 */
class WebPageSinkAttachTest {

    private val page = "<!doctype html><title>Display</title>".encodeToByteArray()

    private val assets = WebAssets(mapOf("index.html" to WebAsset(page, "text/html; charset=utf-8")))

    private val photos = PhotoSource { id ->
        if (id == "p1") ServedPhoto(byteArrayOf(9), "image/png") else null
    }

    private val client = HttpClient(OkHttp) { install(WebSockets) }

    private val sinks = mutableListOf<WebPageSink>()

    @AfterTest
    fun cleanUp() = runBlocking {
        sinks.forEach { it.detach(); it.dispose() }
        client.close()
    }

    /**
     * A sink over a real server, told it is on [address] and given [assets].
     *
     * @param assets Empty stands for a build whose display page did not ship.
     */
    private fun sink(
        address: String? = "127.0.0.1",
        assets: WebAssets = this.assets,
        onPortBound: (Int) -> Unit = {},
        onBaseUrl: (String?) -> Unit = {},
    ) = WebPageSink(
        preferredPort = 0,
        onPortBound = onPortBound,
        photos = photos,
        onBaseUrl = onBaseUrl,
        address = { address },
        loadAssets = { assets },
        serverFactory = { a, p -> LocalWebServer(a, p) },
    ).also { sinks += it }

    /** The port out of an attached sink's URL. */
    private fun WebPageSink.port(): Int =
        status.value.detail!!.substringAfterLast(':').toInt()

    private fun envelope(body: String) =
        SlideEnvelope(slide = Slide(kind = SlideKind.SONG, body = body))

    // ── Serving ──────────────────────────────────────────────────────────

    @Test
    fun `attaching starts a server and reports it as attached`() = runBlocking {
        val webSink = sink()

        webSink.attach()

        assertEquals(SinkState.ATTACHED, webSink.status.value.state)
        assertTrue(webSink.status.value.isAttached)
    }

    @Test
    fun `the URL the operator is given is the one that answers`() = runBlocking {
        // The detail line on the outputs row is read out loud and typed into a
        // TV; if it does not resolve there is nothing else to go on.
        val webSink = sink()
        webSink.attach()

        val url = webSink.status.value.detail!!
        val response = client.get(url)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(page.decodeToString(), response.bodyAsText())
    }

    @Test
    fun `the bound port is reported so it can be reused next service`() = runBlocking {
        var bound: Int? = null
        val webSink = sink(onPortBound = { bound = it })

        webSink.attach()

        assertEquals(webSink.port(), bound)
    }

    @Test
    fun `the base address is handed to the photo library`() = runBlocking {
        // Until this arrives the Photos screen refuses to project, because a
        // photo travels as a URL and there is no server to serve it from.
        var base: String? = null
        val webSink = sink(onBaseUrl = { base = it })

        webSink.attach()

        assertEquals(webSink.status.value.detail, base)
    }

    @Test
    fun `the operator's photos are served from the same server`() = runBlocking {
        val webSink = sink()
        webSink.attach()

        val response = client.get("${webSink.status.value.detail}/photo/p1")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `no display is connected until one opens the page`() = runBlocking {
        val webSink = sink()

        webSink.attach()

        assertEquals(0, webSink.status.value.clientCount)
    }

    @Test
    fun `a display that opens the page is counted on the outputs row`() = runBlocking {
        // How the operator knows the TV actually got there, rather than being on
        // the wrong network with a URL that looks right.
        val webSink = sink()
        webSink.attach()

        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:${webSink.port()}/live") {
                assertEquals(1, webSink.status.first { it.clientCount == 1 }.clientCount)
            }
        }
    }

    @Test
    fun `attaching twice does not start a second server`() = runBlocking {
        // Mode changes call attach again; a second bind would take a different
        // port and change the URL the room was just given.
        val webSink = sink()
        webSink.attach()
        val first = webSink.status.value.detail

        webSink.attach()

        assertEquals(first, webSink.status.value.detail)
    }

    // ── Getting the slide onto the screen ────────────────────────────────

    @Test
    fun `a slide rendered while serving reaches a watching display`() = runBlocking {
        val webSink = sink()
        webSink.attach()

        var frame = ""
        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:${webSink.port()}/live") {
                webSink.render(envelope("verse one"))
                frame = (incoming.receive() as Frame.Text).readText()
            }
        }

        assertTrue("verse one" in frame, frame)
    }

    @Test
    fun `a display that connects late is caught up on what is already projected`() = runBlocking {
        // A TV switched on between songs would otherwise sit blank until the
        // operator moved to the next slide.
        val webSink = sink()
        webSink.attach()
        webSink.render(envelope("already on screen"))

        var frame = ""
        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:${webSink.port()}/live") {
                frame = (incoming.receive() as Frame.Text).readText()
            }
        }

        assertTrue("already on screen" in frame, frame)
    }

    @Test
    fun `a slide rendered before attaching is replayed once the server starts`() = runBlocking {
        // The operator projects, then turns the browser output on. Without the
        // replay the page would open blank on a service already under way.
        val webSink = sink()
        webSink.render(envelope("projected first"))

        webSink.attach()

        var frame = ""
        withTimeout<Unit>(TIMEOUT_MS) {
            client.webSocket("ws://127.0.0.1:${webSink.port()}/live") {
                frame = (incoming.receive() as Frame.Text).readText()
            }
        }

        assertTrue("projected first" in frame, frame)
    }

    // ── Turning it off ───────────────────────────────────────────────────

    @Test
    fun `detaching stops the server answering`() = runBlocking {
        val webSink = sink()
        webSink.attach()
        val url = webSink.status.value.detail!!

        webSink.detach()

        assertEquals(SinkState.DETACHED, webSink.status.value.state)
        assertTrue(runCatching { client.get(url) }.isFailure, "the server was still answering on $url")
    }

    @Test
    fun `detaching tells the photo library it has nowhere to serve from`() = runBlocking {
        val addresses = mutableListOf<String?>()
        val webSink = sink(onBaseUrl = { addresses += it })
        webSink.attach()

        webSink.detach()

        assertNull(addresses.last())
    }

    @Test
    fun `a sink can be attached again after being detached`() = runBlocking {
        // Turning the output off and on again is the operator's first fix.
        val webSink = sink()
        webSink.attach()
        webSink.detach()

        webSink.attach()

        assertEquals(SinkState.ATTACHED, webSink.status.value.state)
        assertEquals(HttpStatusCode.OK, client.get(webSink.status.value.detail!!).status)
    }

    @Test
    fun `rendering after a detach does not throw`() = runBlocking {
        val webSink = sink()
        webSink.attach()
        webSink.detach()

        webSink.render(envelope("nowhere to go"))
    }

    // ── When it cannot serve ─────────────────────────────────────────────

    @Test
    fun `a phone with no LAN address says so rather than starting a server`() = runBlocking {
        // Nothing in the room could reach it anyway, and "Not connected to
        // Wi-Fi" is something the operator can act on.
        val webSink = sink(address = null)

        webSink.attach()

        assertEquals(SinkState.ERROR, webSink.status.value.state)
        assertEquals("Not connected to Wi-Fi", webSink.status.value.detail)
    }

    @Test
    fun `a build whose display page did not ship says so`() = runBlocking {
        val webSink = sink(assets = WebAssets(emptyMap()))

        webSink.attach()

        assertEquals(SinkState.ERROR, webSink.status.value.state)
        assertEquals("Display files missing from this build", webSink.status.value.detail)
    }

    @Test
    fun `a sink that could not start hands out no address`() = runBlocking {
        val addresses = mutableListOf<String?>()

        sink(address = null, onBaseUrl = { addresses += it }).attach()

        assertTrue(addresses.isEmpty(), "an address was offered with no server: $addresses")
    }

    @Test
    fun `a server that will not start is reported with its reason`() = runBlocking {
        val webSink = WebPageSink(
            preferredPort = 0,
            address = { "127.0.0.1" },
            loadAssets = { assets },
            serverFactory = { _, _ -> error("no sockets available") },
        ).also { sinks += it }

        webSink.attach()

        assertEquals(SinkState.ERROR, webSink.status.value.state)
        assertEquals("no sockets available", webSink.status.value.detail)
    }

    @Test
    fun `a failed attach can be retried once the phone joins the network`() = runBlocking {
        var address: String? = null
        val webSink = WebPageSink(
            preferredPort = 0,
            address = { address },
            loadAssets = { assets },
            serverFactory = { a, p -> LocalWebServer(a, p) },
        ).also { sinks += it }
        webSink.attach()
        assertEquals(SinkState.ERROR, webSink.status.value.state)

        address = "127.0.0.1"
        webSink.attach()

        assertEquals(SinkState.ATTACHED, webSink.status.value.state)
    }

    @Test
    fun `the sink identifies itself so the outputs sheet can find it`() {
        assertEquals(WEB_PAGE_SINK_ID, sink().id)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }

    // ── Against a server that misbehaves ─────────────────────────────────
    //
    // A real CIO server cannot fail these ways, which is exactly why they are
    // worth pinning: the guards would otherwise never be exercised until the
    // day something did.

    /** A sink over a stand-in server, so the sink's own handling can be driven. */
    private fun sinkOver(server: LocalWebServer) = WebPageSink(
        preferredPort = 0,
        address = { "127.0.0.1" },
        loadAssets = { assets },
        serverFactory = { _, _ -> server },
    ).also { sinks += it }

    @Test
    fun `a publish that fails leaves the sink serving rather than throwing`() = runBlocking {
        // render() runs on the action path and may fire many times a second
        // while an operator scrubs a deck; a throw there would reach the UI.
        val server = mockk<LocalWebServer>(relaxed = true) {
            every { clientCount } returns MutableStateFlow(0)
            every { publish(any()) } throws IllegalStateException("socket closed")
        }
        val webSink = sinkOver(server)
        webSink.attach()

        webSink.render(envelope("verse one"))

        assertEquals(SinkState.ATTACHED, webSink.status.value.state)
    }

    @Test
    fun `the number of watching displays reaches the outputs row`() = runBlocking {
        val watching = MutableStateFlow(0)
        val webSink = sinkOver(mockk(relaxed = true) { every { clientCount } returns watching })
        webSink.attach()

        watching.value = 3

        assertEquals(3, webSink.status.first { it.clientCount == 3 }.clientCount)
    }

    @Test
    fun `a display leaving lowers the count on the outputs row`() = runBlocking {
        val watching = MutableStateFlow(2)
        val webSink = sinkOver(mockk(relaxed = true) { every { clientCount } returns watching })
        webSink.attach()
        webSink.status.first { it.clientCount == 2 }

        watching.value = 1

        assertEquals(1, webSink.status.first { it.clientCount == 1 }.clientCount)
    }

    @Test
    fun `detaching stops following the count`() = runBlocking {
        // The watcher runs in the sink's own scope; left running it would keep
        // writing a client count onto a row that says the output is off.
        val watching = MutableStateFlow(1)
        val webSink = sinkOver(mockk(relaxed = true) { every { clientCount } returns watching })
        webSink.attach()
        webSink.status.first { it.clientCount == 1 }

        webSink.detach()
        watching.value = 9

        assertEquals(0, webSink.status.value.clientCount)
    }

    @Test
    fun `a server that will not bind is reported with its reason`() = runBlocking {
        val webSink = sinkOver(mockk(relaxed = true) {
            every { clientCount } returns MutableStateFlow(0)
            coEvery { start(any(), any()) } throws IllegalStateException("Could not bind any port")
        })

        webSink.attach()

        assertEquals(SinkState.ERROR, webSink.status.value.state)
        assertEquals("Could not bind any port", webSink.status.value.detail)
    }

    @Test
    fun `the sink builds its own server when nothing is substituted`() = runBlocking {
        // Everything above supplies a server; this is the wiring the app itself
        // uses, so the default has to be exercised at least once.
        val webSink = WebPageSink(
            preferredPort = 0,
            address = { "127.0.0.1" },
            loadAssets = { assets },
        ).also { sinks += it }

        webSink.attach()

        assertEquals(SinkState.ATTACHED, webSink.status.value.state)
        assertEquals(HttpStatusCode.OK, client.get(webSink.status.value.detail!!).status)
    }
}
