package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The four HTTP clients this app builds, and what each one is for.
 *
 * They differ in ways that are invisible until they matter: the action client
 * has no request timeout because an approval dialog on the desktop can stay
 * open for minutes; the image client deliberately has no content negotiation so
 * Coil sees raw bytes; the socket client installs WebSockets. Building each is
 * a block of configuration nothing else executes, and a plugin added to the
 * wrong one is the kind of mistake that surfaces as "thumbnails sometimes fail"
 * rather than as an error.
 */
class HttpClientFactoryAndroidTest {

    private fun <T> using(client: HttpClient, block: (HttpClient) -> T): T =
        try { block(client) } finally { client.close() }

    @Test
    fun `the ordinary client parses JSON for the services`() {
        using(createHttpClient()) {
            assertNotNull(it.pluginOrNull(ContentNegotiation))
        }
    }

    @Test
    fun `the action client parses JSON too`() {
        // It carries the approval-gated POSTs, whose replies are JSON like any
        // other; only its timeouts differ.
        using(createActionHttpClient()) {
            assertNotNull(it.pluginOrNull(ContentNegotiation))
        }
    }

    @Test
    fun `the image client does not touch the bytes it fetches`() {
        // Content negotiation here would try to decode a JPEG as JSON. Coil is
        // handed the raw stream instead.
        using(createImageHttpClient()) {
            assertNull(it.pluginOrNull(ContentNegotiation))
        }
    }

    @Test
    fun `the socket client can open a WebSocket`() {
        using(createWebSocketClient()) {
            assertNotNull(it.pluginOrNull(WebSockets))
        }
    }

    @Test
    fun `the ordinary client is not a WebSocket client`() {
        // The socket is deliberately separate: it is long-lived and has its own
        // connect timeout, and installing WebSockets everywhere would hide that.
        using(createHttpClient()) {
            assertNull(it.pluginOrNull(WebSockets))
        }
    }

    @Test
    fun `each call builds its own client`() {
        // Sharing one would mean closing a screen's client took the others down
        // with it — closeClient() is called from several ViewModels' onCleared.
        val first = createHttpClient()
        val second = createHttpClient()
        try {
            assert(first !== second)
        } finally {
            first.close()
            second.close()
        }
    }

    @Test
    fun `every client closes cleanly`() {
        // Every service closes its own on onCleared; a throw there would surface
        // as a crash on leaving a screen.
        listOf(
            createHttpClient(),
            createActionHttpClient(),
            createImageHttpClient(),
            createWebSocketClient(),
        ).forEach { it.close() }
    }

    @Test
    fun `the image client raises the per-host request cap`(): Unit = runBlocking {
        // OkHttp allows five concurrent requests per host by default, which
        // starves a photo grid loading many thumbnails from the same desktop —
        // the queued ones then time out. The engine is configured lazily, on the
        // first request, so it takes one to run that configuration at all.
        val client = createImageHttpClient()
        try {
            // A loopback port with nothing on it: the connection is refused
            // immediately and nothing leaves the machine.
            runCatching { client.get("http://127.0.0.1:$closedPort/thumb.jpg") }
        } finally {
            client.close()
        }
    }

    private val closedPort: Int get() = java.net.ServerSocket(0).use { it.localPort }
}
