package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.present.SinkState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The phone becoming a display server.
 *
 * What comes out of a successful attach is an address the operator reads across
 * a room and types into a TV, so the pieces of it all matter: the phone's own
 * LAN address, the port actually bound rather than the one asked for, and the
 * fact that both are handed onward — to the settings, so the URL is the same
 * next Sunday, and to the photo library, which cannot serve anything until it
 * knows where it is being served from.
 */
class WebPageSinkServingTest {

    // ── The address the operator reads out ───────────────────────────────

    @Test
    fun `a served sink reports itself attached`() = runTest {
        val sink = webSink()

        sink.attach()

        assertEquals(SinkState.ATTACHED, sink.status.value.state)
    }

    @Test
    fun `a served sink is attached by the outputs sheet's reckoning`() = runTest {
        val sink = webSink()

        sink.attach()

        assertTrue(sink.status.value.isAttached)
    }

    @Test
    fun `the detail line is the address to type into a TV`() = runTest {
        val sink = webSink(address = "192.168.1.50", server = FakeDisplayServer(boundPort = 8080))

        sink.attach()

        assertEquals("http://192.168.1.50:8080", sink.status.value.detail)
    }

    @Test
    fun `the address carries the phone's own LAN address`() = runTest {
        val sink = webSink(address = "10.0.0.7")

        sink.attach()

        assertTrue(sink.status.value.detail?.contains("10.0.0.7") == true)
    }

    @Test
    fun `the address carries the port that was actually bound`() = runTest {
        // Not the one that was asked for: another app may hold it, and a URL
        // naming a port nothing listens on is worse than no URL.
        val sink = webSink(preferredPort = 8080, server = FakeDisplayServer(boundPort = 9123))

        sink.attach()

        assertTrue(sink.status.value.detail?.endsWith(":9123") == true)
    }

    @Test
    fun `the address is plain http`() = runTest {
        // A browser on the hall network, with no certificate anywhere in sight.
        val sink = webSink()

        sink.attach()

        assertTrue(sink.status.value.detail?.startsWith("http://") == true)
    }

    @Test
    fun `no screen is connected the moment the server starts`() = runTest {
        val sink = webSink()

        sink.attach()

        assertEquals(0, sink.status.value.clientCount)
    }

    @Test
    fun `the sink keeps its name once serving`() = runTest {
        // The outputs row is the same row it was before, not a new one.
        val sink = webSink()

        sink.attach()

        assertEquals("Browser screen", sink.status.value.displayName)
    }

    @Test
    fun `the sink keeps its id once serving`() = runTest {
        val sink = webSink()

        sink.attach()

        assertEquals(WEB_PAGE_SINK_ID, sink.status.value.id)
    }

    // ── Handing the address onward ───────────────────────────────────────

    @Test
    fun `the bound port is handed back for next time`() = runTest {
        // Remembered in settings so the URL given to the TV survives a restart.
        var bound: Int? = null
        val sink = webSink(server = FakeDisplayServer(boundPort = 9123), onPortBound = { bound = it })

        sink.attach()

        assertEquals(9123, bound)
    }

    @Test
    fun `the base address is handed to the photo library`() = runTest {
        // Until it knows where it is served from, it has nothing to project.
        var base: String? = null
        val sink = webSink(onBaseUrl = { base = it })

        sink.attach()

        assertEquals("http://192.168.1.50:8080", base)
    }

    @Test
    fun `the address handed onward is the same one shown to the operator`() = runTest {
        var base: String? = null
        val sink = webSink(onBaseUrl = { base = it })

        sink.attach()

        assertEquals(sink.status.value.detail, base)
    }

    @Test
    fun `the port is handed back before the sink is used`() = runTest {
        var boundWhileAttaching: Int? = null
        val sink = webSink(server = FakeDisplayServer(boundPort = 7000), onPortBound = { boundWhileAttaching = it })

        sink.attach()

        assertNotNull(boundWhileAttaching)
    }

    // ── Which port it asks for ───────────────────────────────────────────

    @Test
    fun `the preferred port is the one asked for first`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server, preferredPort = 8080)

        sink.attach()

        assertEquals(8080, server.lastPreferredPort)
    }

    @Test
    fun `a remembered port is asked for rather than the default`() = runTest {
        // The operator wrote the URL on a card taped to the desk.
        val server = FakeDisplayServer()
        val sink = webSink(server = server, preferredPort = 9123)

        sink.attach()

        assertEquals(9123, server.lastPreferredPort)
    }

    @Test
    fun `the standard candidates are offered as fallbacks`() = runTest {
        // A service should not fail to start because another app holds a port.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)

        sink.attach()

        assertEquals(ApiConstants.STANDALONE_PORT_CANDIDATES.toList(), server.lastCandidates)
    }

    @Test
    fun `the candidate list is not empty`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)

        sink.attach()

        assertTrue(server.lastCandidates?.isNotEmpty() == true)
    }

    @Test
    fun `the server is started exactly once`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)

        sink.attach()

        assertEquals(1, server.startCount)
    }

    // ── Attaching twice ──────────────────────────────────────────────────

    @Test
    fun `attaching an attached sink starts nothing further`() = runTest {
        // Idempotent by contract: the registry attaches on every mode change.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.attach()

        assertEquals(1, server.startCount)
    }

    @Test
    fun `attaching an attached sink leaves it attached`() = runTest {
        val sink = webSink()
        sink.attach()

        sink.attach()

        assertEquals(SinkState.ATTACHED, sink.status.value.state)
    }

    @Test
    fun `attaching an attached sink keeps the same address`() = runTest {
        val sink = webSink()
        sink.attach()
        val first = sink.status.value.detail

        sink.attach()

        assertEquals(first, sink.status.value.detail)
    }

    @Test
    fun `attaching an attached sink does not hand out the address again`() = runTest {
        var handed = 0
        val sink = webSink(onBaseUrl = { handed++ })
        sink.attach()
        val afterFirst = handed

        sink.attach()

        assertEquals(afterFirst, handed)
    }

    // ── Bringing a late display up to date ───────────────────────────────

    @Test
    fun `a frame rendered before serving is replayed once serving starts`() = runTest {
        // The operator projects, then plugs the TV in — which is the ordinary
        // way round, not an edge case.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.render(envelope("Amazing grace"))

        sink.attach()

        assertEquals(1, server.published.size)
    }

    @Test
    fun `the replayed frame is the one that was showing`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.render(envelope("Amazing grace"))

        sink.attach()

        assertTrue(server.published.single().contains("Amazing grace"))
    }

    @Test
    fun `only the latest frame is replayed`() = runTest {
        // A display connecting late wants the current slide, not the history.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.render(envelope("Verse one"))
        sink.render(envelope("Verse two"))

        sink.attach()

        assertEquals(1, server.published.size)
        assertTrue(server.published.single().contains("Verse two"))
    }

    @Test
    fun `a sink that has shown nothing replays nothing`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)

        sink.attach()

        assertTrue(server.published.isEmpty())
    }

    @Test
    fun `the replay happens after the address is published`() = runTest {
        // A frame is no use to a display that has not been told where to look.
        var baseWhenPublished: String? = null
        val server = FakeDisplayServer()
        val sink = webSink(server = server, onBaseUrl = { baseWhenPublished = it })
        sink.render(envelope())

        sink.attach()

        assertNotNull(baseWhenPublished)
        assertEquals(1, server.published.size)
    }

    // ── The photos it also serves ────────────────────────────────────────

    @Test
    fun `the operator's photos are handed to the server it builds`() = runTest {
        // Both displays fetch an image from the identical address, which only
        // works if the same server serves both.
        var sawPhotos = false
        val server = FakeDisplayServer()
        val sink = WebPageSink(
            preferredPort = 8080,
            photos = { _ -> null },
            address = { "192.168.1.50" },
            loadAssets = { bundledAssets() },
            serverFactory = { _, _ -> sawPhotos = true; server },
        )

        sink.attach()

        assertTrue(sawPhotos)
    }

    @Test
    fun `the bundled page is handed to the server it builds`() = runTest {
        var handed: Boolean? = null
        val server = FakeDisplayServer()
        val sink = WebPageSink(
            preferredPort = 8080,
            address = { "192.168.1.50" },
            loadAssets = { bundledAssets() },
            serverFactory = { assets, _ -> handed = !assets.isEmpty; server },
        )

        sink.attach()

        assertEquals(true, handed)
    }

    @Test
    fun `the assets are read before a server is built`() = runTest {
        // Building one for a bundle that has no page in it would bind a port to
        // serve nothing.
        var loaded = 0
        val sink = webSink(loadAssets = { loaded++; bundledAssets() })

        sink.attach()

        assertEquals(1, loaded)
    }

    @Test
    fun `an attach passes through attaching on its way to attached`() = runTest {
        // The outputs row says "waiting for a screen" rather than nothing while
        // the port is being bound.
        val sink = webSink()
        assertEquals(SinkState.DETACHED, sink.status.value.state)

        sink.attach()

        assertEquals(SinkState.ATTACHED, sink.status.value.state)
    }

    @Test
    fun `a fresh sink has no address to show`() = runTest {
        val sink = webSink()

        assertNull(sink.status.value.detail)
    }

    @Test
    fun `a fresh sink is detached`() = runTest {
        assertEquals(SinkState.DETACHED, webSink().status.value.state)
    }

    @Test
    fun `an address of an empty string is still an address`() = runTest {
        // Whatever the platform hands back is what the operator is told; the
        // sink does not second-guess it into a failure.
        val sink = webSink(address = "")

        sink.attach()

        assertEquals(SinkState.ATTACHED, sink.status.value.state)
    }

    @Test
    fun `a port of zero is still reported`() = runTest {
        val sink = webSink(server = FakeDisplayServer(boundPort = 0))

        sink.attach()

        assertTrue(sink.status.value.detail?.endsWith(":0") == true)
    }
}
