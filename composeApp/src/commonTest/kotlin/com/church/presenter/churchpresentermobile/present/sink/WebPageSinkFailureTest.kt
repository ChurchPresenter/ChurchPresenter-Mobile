package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.present.SinkState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The sink refusing to serve, and saying why.
 *
 * The failure that matters is a silent one: a sink reporting itself attached
 * with no server behind it leaves the operator reading out a URL that answers
 * nothing, mid-service, with nothing anywhere to explain it. Each reason is also
 * different advice — no Wi-Fi is something they can fix in the next minute, a
 * missing display page in the build is not — so they must not collapse into one
 * red dot.
 */
class WebPageSinkFailureTest {

    // ── No network ───────────────────────────────────────────────────────

    @Test
    fun `a phone with no LAN address does not serve`() = runTest {
        // No device could reach it anyway; starting a server nobody can find
        // just hands out a URL that fails.
        val sink = webSink(address = null)

        sink.attach()

        assertFalse(sink.status.value.isAttached)
    }

    @Test
    fun `a phone with no LAN address reports an error`() = runTest {
        val sink = webSink(address = null)

        sink.attach()

        assertEquals(SinkState.ERROR, sink.status.value.state)
    }

    @Test
    fun `a phone with no LAN address says it is not on Wi-Fi`() = runTest {
        val sink = webSink(address = null)

        sink.attach()

        assertEquals("Not connected to Wi-Fi", sink.status.value.detail)
    }

    @Test
    fun `a phone with no LAN address never builds a server`() = runTest {
        var built = 0
        val sink = WebPageSink(
            preferredPort = 8080,
            address = { null },
            loadAssets = { bundledAssets() },
            serverFactory = { _, _ -> built++; FakeDisplayServer() },
        )

        sink.attach()

        assertEquals(0, built)
    }

    @Test
    fun `a phone with no LAN address does not read the bundle`() = runTest {
        // There is nothing to serve it to.
        var loaded = 0
        val sink = webSink(address = null, loadAssets = { loaded++; bundledAssets() })

        sink.attach()

        assertEquals(0, loaded)
    }

    @Test
    fun `a phone with no LAN address hands out no address`() = runTest {
        // A URL here would make the Photos screen offer to project over a server
        // that does not exist.
        var handed: String? = "unset"
        val sink = webSink(address = null, onBaseUrl = { handed = it })

        sink.attach()

        assertEquals("unset", handed)
    }

    @Test
    fun `a phone with no LAN address hands back no port`() = runTest {
        var bound: Int? = null
        val sink = webSink(address = null, onPortBound = { bound = it })

        sink.attach()

        assertNull(bound)
    }

    @Test
    fun `a phone with no LAN address has no client count to show`() = runTest {
        val sink = webSink(address = null)

        sink.attach()

        assertEquals(0, sink.status.value.clientCount)
    }

    // ── No display page in the build ─────────────────────────────────────

    @Test
    fun `a build with no display page does not serve`() = runTest {
        val sink = webSink(assets = missingAssets())

        sink.attach()

        assertFalse(sink.status.value.isAttached)
    }

    @Test
    fun `a build with no display page reports an error`() = runTest {
        val sink = webSink(assets = missingAssets())

        sink.attach()

        assertEquals(SinkState.ERROR, sink.status.value.state)
    }

    @Test
    fun `a build with no display page says what is missing`() = runTest {
        // Different advice from "no Wi-Fi": this one is not the operator's to fix.
        val sink = webSink(assets = missingAssets())

        sink.attach()

        assertEquals("Display files missing from this build", sink.status.value.detail)
    }

    @Test
    fun `a build with no display page never builds a server`() = runTest {
        var built = 0
        val sink = WebPageSink(
            preferredPort = 8080,
            address = { "192.168.1.50" },
            loadAssets = { missingAssets() },
            serverFactory = { _, _ -> built++; FakeDisplayServer() },
        )

        sink.attach()

        assertEquals(0, built)
    }

    @Test
    fun `a build with no display page hands out no address`() = runTest {
        var handed: String? = "unset"
        val sink = webSink(assets = missingAssets(), onBaseUrl = { handed = it })

        sink.attach()

        assertEquals("unset", handed)
    }

    @Test
    fun `the two failures do not read the same`() = runTest {
        // One is fixable in the next minute; the other needs a new build.
        val noNetwork = webSink(address = null)
        val noAssets = webSink(assets = missingAssets())

        noNetwork.attach()
        noAssets.attach()

        assertTrue(noNetwork.status.value.detail != noAssets.status.value.detail)
    }

    // ── A port that could not be bound ───────────────────────────────────

    @Test
    fun `a server that will not start does not report as attached`() = runTest {
        val sink = webSink(server = FakeDisplayServer(failToStart = IllegalStateException("no port free")))

        sink.attach()

        assertFalse(sink.status.value.isAttached)
    }

    @Test
    fun `a server that will not start reports an error`() = runTest {
        val sink = webSink(server = FakeDisplayServer(failToStart = IllegalStateException("no port free")))

        sink.attach()

        assertEquals(SinkState.ERROR, sink.status.value.state)
    }

    @Test
    fun `a server that will not start says why`() = runTest {
        // The reason comes from the server, not from a generic sentence here.
        val sink = webSink(server = FakeDisplayServer(failToStart = IllegalStateException("no port free")))

        sink.attach()

        assertEquals("no port free", sink.status.value.detail)
    }

    @Test
    fun `a server that will not start does not escape into the caller`() = runTest {
        // The registry attaches sinks in a loop; one throwing would take the
        // others with it.
        val sink = webSink(server = FakeDisplayServer(failToStart = RuntimeException("boom")))

        sink.attach()

        assertEquals(SinkState.ERROR, sink.status.value.state)
    }

    @Test
    fun `a server that will not start hands out no address`() = runTest {
        var handed: String? = "unset"
        val sink = webSink(
            server = FakeDisplayServer(failToStart = IllegalStateException("no port free")),
            onBaseUrl = { handed = it },
        )

        sink.attach()

        assertEquals("unset", handed)
    }

    @Test
    fun `a server that will not start hands back no port`() = runTest {
        var bound: Int? = null
        val sink = webSink(
            server = FakeDisplayServer(failToStart = IllegalStateException("no port free")),
            onPortBound = { bound = it },
        )

        sink.attach()

        assertNull(bound)
    }

    @Test
    fun `a factory that throws is a failure like any other`() = runTest {
        // Building the server is inside the same guard as starting it.
        val sink = WebPageSink(
            preferredPort = 8080,
            address = { "192.168.1.50" },
            loadAssets = { bundledAssets() },
            serverFactory = { _, _ -> throw IllegalStateException("cannot build a server") },
        )

        sink.attach()

        assertEquals(SinkState.ERROR, sink.status.value.state)
    }

    @Test
    fun `a factory that throws says why`() = runTest {
        val sink = WebPageSink(
            preferredPort = 8080,
            address = { "192.168.1.50" },
            loadAssets = { bundledAssets() },
            serverFactory = { _, _ -> throw IllegalStateException("cannot build a server") },
        )

        sink.attach()

        assertEquals("cannot build a server", sink.status.value.detail)
    }

    @Test
    fun `a failure with no message still leaves the row readable`() = runTest {
        // A red row with nothing in it is the same as no explanation at all;
        // the sink falls back to its own name rather than showing null.
        val sink = webSink(server = FakeDisplayServer(failToStart = RuntimeException()))

        sink.attach()

        assertEquals(SinkState.ERROR, sink.status.value.state)
        assertTrue(sink.status.value.displayName.isNotBlank())
    }

    // ── Trying again ─────────────────────────────────────────────────────

    @Test
    fun `a failed sink can be attached again`() = runTest {
        // The operator joins the Wi-Fi and tries once more, which is the whole
        // reason the failure is not final.
        val server = FakeDisplayServer(failToStart = IllegalStateException("no port free"))
        val sink = webSink(server = server)
        sink.attach()

        server.failToStart = null
        sink.attach()

        assertEquals(SinkState.ATTACHED, sink.status.value.state)
    }

    @Test
    fun `a second attempt after a failure starts the server again`() = runTest {
        val server = FakeDisplayServer(failToStart = IllegalStateException("no port free"))
        val sink = webSink(server = server)
        sink.attach()

        server.failToStart = null
        sink.attach()

        assertEquals(2, server.startCount)
    }

    @Test
    fun `a successful retry hands out the address it could not before`() = runTest {
        var handed: String? = null
        val server = FakeDisplayServer(failToStart = IllegalStateException("no port free"))
        val sink = webSink(server = server, onBaseUrl = { handed = it })
        sink.attach()

        server.failToStart = null
        sink.attach()

        assertEquals("http://192.168.1.50:8080", handed)
    }

    @Test
    fun `a successful retry clears the error from the row`() = runTest {
        val server = FakeDisplayServer(failToStart = IllegalStateException("no port free"))
        val sink = webSink(server = server)
        sink.attach()

        server.failToStart = null
        sink.attach()

        assertEquals("http://192.168.1.50:8080", sink.status.value.detail)
    }

    @Test
    fun `a retry after joining a network serves`() = runTest {
        var lanAddress: String? = null
        val sink = WebPageSink(
            preferredPort = 8080,
            address = { lanAddress },
            loadAssets = { bundledAssets() },
            serverFactory = { _, _ -> FakeDisplayServer() },
        )
        sink.attach()
        assertEquals(SinkState.ERROR, sink.status.value.state)

        lanAddress = "192.168.1.50"
        sink.attach()

        assertEquals(SinkState.ATTACHED, sink.status.value.state)
    }

    @Test
    fun `a failed attach leaves the sink able to buffer a frame`() = runTest {
        // Nothing is serving, but the operator is still working; the frame has
        // to be there when a server finally starts.
        val server = FakeDisplayServer(failToStart = IllegalStateException("no port free"))
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope("Amazing grace"))
        server.failToStart = null
        sink.attach()

        assertTrue(server.published.single().contains("Amazing grace"))
    }

    @Test
    fun `a failed attach publishes nothing`() = runTest {
        val server = FakeDisplayServer(failToStart = IllegalStateException("no port free"))
        val sink = webSink(server = server)
        sink.render(envelope())

        sink.attach()

        assertTrue(server.published.isEmpty())
    }

    @Test
    fun `assets that fail to load are not a crash`() = runTest {
        // The bundle read happens on a device and can fail; the outputs row is
        // where that shows up, not a stack trace.
        val sink = webSink(loadAssets = { missingAssets() })

        sink.attach()

        assertEquals(SinkState.ERROR, sink.status.value.state)
    }

    @Test
    fun `a failed sink reports no clients`() = runTest {
        val sink = webSink(server = FakeDisplayServer(failToStart = IllegalStateException("boom")))

        sink.attach()

        assertEquals(0, sink.status.value.clientCount)
    }

    @Test
    fun `a failed sink keeps its id`() = runTest {
        // The outputs list matches rows by id; a failure must not orphan the row.
        val sink = webSink(address = null)

        sink.attach()

        assertEquals(WEB_PAGE_SINK_ID, sink.status.value.id)
    }

    @Test
    fun `a failed sink keeps its name`() = runTest {
        val sink = webSink(address = null)

        sink.attach()

        assertEquals("Browser screen", sink.status.value.displayName)
    }
}
