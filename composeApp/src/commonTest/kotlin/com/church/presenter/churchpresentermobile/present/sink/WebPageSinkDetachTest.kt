package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.present.SinkState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Giving the screen back.
 *
 * Detaching happens when the operator leaves standalone mode or the app stops,
 * and the state it leaves behind is what the next attach starts from. The thing
 * that must not survive it is the address: the photo library uses it to decide
 * whether it can project at all, and a stale one points at a server that has
 * stopped answering.
 */
class WebPageSinkDetachTest {

    @Test
    fun `detaching stops the server`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.detach()

        assertEquals(1, server.stopCount)
    }

    @Test
    fun `detaching leaves the sink detached`() = runTest {
        val sink = webSink()
        sink.attach()

        sink.detach()

        assertEquals(SinkState.DETACHED, sink.status.value.state)
    }

    @Test
    fun `detaching is not the same as failing`() = runTest {
        // A deliberate stop must not paint the outputs row red.
        val sink = webSink()
        sink.attach()

        sink.detach()

        assertFalse(sink.status.value.state == SinkState.ERROR)
    }

    @Test
    fun `detaching takes the address off the row`() = runTest {
        // It answers nothing now; leaving it up invites the operator to read it
        // out.
        val sink = webSink()
        sink.attach()

        sink.detach()

        assertNull(sink.status.value.detail)
    }

    @Test
    fun `detaching tells the photo library it has nowhere to serve from`() = runTest {
        val handed = mutableListOf<String?>()
        val sink = webSink(onBaseUrl = { handed += it })
        sink.attach()

        sink.detach()

        assertNull(handed.last())
    }

    @Test
    fun `detaching clears the address before the server stops`() = runTest {
        // The other way round leaves a window where a photo slide points at a
        // server that has already gone.
        val server = FakeDisplayServer()
        var stoppedWhenCleared: Int? = null
        val sink = webSink(server = server, onBaseUrl = { if (it == null) stoppedWhenCleared = server.stopCount })
        sink.attach()

        sink.detach()

        assertEquals(0, stoppedWhenCleared)
    }

    @Test
    fun `detaching forgets how many screens were connected`() = runTest {
        val sink = webSink()
        sink.attach()

        sink.detach()

        assertEquals(0, sink.status.value.clientCount)
    }

    @Test
    fun `detaching keeps the row's name`() = runTest {
        // The outputs sheet still lists it, saying the feature exists.
        val sink = webSink()
        sink.attach()

        sink.detach()

        assertEquals("Browser screen", sink.status.value.displayName)
    }

    @Test
    fun `detaching keeps the row's id`() = runTest {
        val sink = webSink()
        sink.attach()

        sink.detach()

        assertEquals(WEB_PAGE_SINK_ID, sink.status.value.id)
    }

    @Test
    fun `detaching an unattached sink is harmless`() = runTest {
        // The registry detaches everything on the way out, attached or not.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)

        sink.detach()

        assertEquals(0, server.stopCount)
    }

    @Test
    fun `detaching an unattached sink leaves it detached`() = runTest {
        val sink = webSink()

        sink.detach()

        assertEquals(SinkState.DETACHED, sink.status.value.state)
    }

    @Test
    fun `detaching an unattached sink still says there is nowhere to serve from`() = runTest {
        val handed = mutableListOf<String?>()
        val sink = webSink(onBaseUrl = { handed += it })

        sink.detach()

        assertEquals(listOf<String?>(null), handed)
    }

    @Test
    fun `detaching twice is harmless`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.detach()

        sink.detach()

        assertEquals(1, server.stopCount)
    }

    @Test
    fun `detaching twice leaves the sink detached`() = runTest {
        val sink = webSink()
        sink.attach()
        sink.detach()

        sink.detach()

        assertEquals(SinkState.DETACHED, sink.status.value.state)
    }

    @Test
    fun `detaching after a failed attach is harmless`() = runTest {
        val server = FakeDisplayServer(failToStart = IllegalStateException("no port free"))
        val sink = webSink(server = server)
        sink.attach()

        sink.detach()

        assertEquals(0, server.stopCount)
    }

    @Test
    fun `detaching after a failed attach clears the error`() = runTest {
        // The row goes back to "off" rather than staying red over a sink nobody
        // is trying to use.
        val sink = webSink(server = FakeDisplayServer(failToStart = IllegalStateException("boom")))
        sink.attach()

        sink.detach()

        assertNull(sink.status.value.detail)
    }

    // ── Coming back ──────────────────────────────────────────────────────

    @Test
    fun `a detached sink can attach again`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.detach()

        sink.attach()

        assertEquals(SinkState.ATTACHED, sink.status.value.state)
    }

    @Test
    fun `re-attaching starts the server again`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.detach()

        sink.attach()

        assertEquals(2, server.startCount)
    }

    @Test
    fun `re-attaching hands out the address again`() = runTest {
        val handed = mutableListOf<String?>()
        val sink = webSink(onBaseUrl = { handed += it })
        sink.attach()
        sink.detach()

        sink.attach()

        assertEquals("http://192.168.1.50:8080", handed.last())
    }

    @Test
    fun `re-attaching on a new port reports the new one`() = runTest {
        // The old port may have been taken while the app was away.
        val server = FakeDisplayServer(boundPort = 8080)
        val sink = webSink(server = server)
        sink.attach()
        sink.detach()

        server.boundPort = 9123
        sink.attach()

        assertEquals("http://192.168.1.50:9123", sink.status.value.detail)
    }

    @Test
    fun `re-attaching hands the new port back to be remembered`() = runTest {
        val bound = mutableListOf<Int>()
        val server = FakeDisplayServer(boundPort = 8080)
        val sink = webSink(server = server, onPortBound = { bound += it })
        sink.attach()
        sink.detach()

        server.boundPort = 9123
        sink.attach()

        assertEquals(listOf(8080, 9123), bound)
    }

    @Test
    fun `re-attaching asks for the port it was given, not the one it bound`() = runTest {
        // The sink is told which port to prefer by whoever remembered it; it
        // does not quietly keep its own.
        val server = FakeDisplayServer(boundPort = 9123)
        val sink = webSink(server = server, preferredPort = 8080)
        sink.attach()
        sink.detach()

        sink.attach()

        assertEquals(8080, server.lastPreferredPort)
    }

    @Test
    fun `a sink that never served can still be detached and attached`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)

        sink.detach()
        sink.attach()

        assertEquals(SinkState.ATTACHED, sink.status.value.state)
    }

    @Test
    fun `detaching after a re-attach stops the server again`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.detach()
        sink.attach()

        sink.detach()

        assertEquals(2, server.stopCount)
    }

    @Test
    fun `a disposed sink still reports its last state`() = runTest {
        // Disposal releases the coroutine scope; it is not a status change.
        val sink = webSink()
        sink.attach()

        sink.dispose()

        assertEquals(SinkState.ATTACHED, sink.status.value.state)
    }

    @Test
    fun `a disposed sink can still be detached`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.dispose()

        sink.detach()

        assertEquals(1, server.stopCount)
    }

    @Test
    fun `disposing without ever attaching is harmless`() = runTest {
        val sink = webSink()

        sink.dispose()

        assertEquals(SinkState.DETACHED, sink.status.value.state)
    }

    @Test
    fun `disposing twice is harmless`() = runTest {
        val sink = webSink()
        sink.attach()

        sink.dispose()
        sink.dispose()

        assertTrue(sink.status.value.isAttached)
    }

    @Test
    fun `a disposed sink still publishes what it is given`() = runTest {
        // Rendering does not go through the scope that was cancelled.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.dispose()

        sink.render(envelope("Still going"))

        assertTrue(server.published.single().contains("Still going"))
    }

    @Test
    fun `a server that cannot be stopped has already had the address withdrawn`() = runTest {
        // Detach clears the address first and stops the server second, so a
        // shutdown that fails still cannot leave a photo slide pointing at a
        // server on its way out. (The exception itself is not swallowed — worth
        // knowing, since detach runs as the app goes away.)
        val handed = mutableListOf<String?>()
        val server = object : DisplayServer by FakeDisplayServer() {
            override suspend fun stop(): Unit = error("already gone")
        }
        val sink = WebPageSink(
            preferredPort = 8080,
            onBaseUrl = { handed += it },
            address = { "192.168.1.50" },
            loadAssets = { bundledAssets() },
            serverFactory = { _, _ -> server },
        )
        sink.attach()

        runCatching { sink.detach() }

        assertNull(handed.last())
    }
}
