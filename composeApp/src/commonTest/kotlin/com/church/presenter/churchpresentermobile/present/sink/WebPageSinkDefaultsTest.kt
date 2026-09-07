package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.present.PhotoSource
import com.church.presenter.churchpresentermobile.present.SinkState
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The sink as the app actually builds it, with nothing injected.
 *
 * Every other test here replaces the server with a stand-in, which is the only
 * way to reach the serving path — so something has to exercise the wiring that
 * ships: the real factory, the real platform server behind it, and the default
 * arguments an ordinary caller leaves alone. On a host that cannot serve the
 * page the result is a clean, explained failure, and that it is clean is the
 * point: this runs at app startup, inside a loop that attaches every sink.
 */
class WebPageSinkDefaultsTest {

    private val sinks = mutableListOf<WebPageSink>()

    @AfterTest
    fun releaseAnyPortsBound() = runTest {
        // A host that really did bind one must give it back before the next test.
        sinks.forEach { it.detach() }
    }

    /** As `App` builds it, minus the two seams a test host cannot satisfy. */
    private fun realSink(
        onPortBound: (Int) -> Unit = {},
        onBaseUrl: (String?) -> Unit = {},
    ) = WebPageSink(
        preferredPort = 8080,
        onPortBound = onPortBound,
        photos = PhotoSource.NONE,
        onBaseUrl = onBaseUrl,
        address = { "192.168.1.50" },
        loadAssets = { bundledAssets() },
    ).also { sinks += it }

    @Test
    fun `the shipped wiring builds a server without being handed one`() = runTest {
        // The default factory is what every real attach goes through.
        val sink = realSink()

        sink.attach()

        assertTrue(sink.status.value.state == SinkState.ERROR || sink.status.value.isAttached)
    }

    @Test
    fun `an attach settles on one answer or the other`() = runTest {
        // Attach runs in a loop over every sink at startup; a throw here would
        // take the external display down with it, and a sink left "attaching"
        // would leave the row spinning forever.
        val sink = realSink()

        sink.attach()

        assertTrue(sink.status.value.state == SinkState.ATTACHED || sink.status.value.state == SinkState.ERROR)
    }

    @Test
    fun `an attach always leaves something on the row`() = runTest {
        // Either the address to type into a TV, or the reason there is none.
        val sink = realSink()

        sink.attach()

        assertTrue(sink.status.value.detail?.isNotBlank() == true)
    }

    @Test
    fun `the address handed to the photo library matches the state`() = runTest {
        // A URL for a server that never started would make the Photos screen
        // offer to project over nothing.
        var handed: String? = "unset"
        val sink = realSink(onBaseUrl = { handed = it })

        sink.attach()

        if (sink.status.value.isAttached) assertEquals(sink.status.value.detail, handed)
        else assertEquals("unset", handed)
    }

    @Test
    fun `a port is handed back only when one was bound`() = runTest {
        // It is remembered in settings and reused next service, so a port that
        // was never bound must not be written there.
        var bound: Int? = null
        val sink = realSink(onPortBound = { bound = it })

        sink.attach()

        assertEquals(sink.status.value.isAttached, bound != null)
    }

    @Test
    fun `the shipped wiring can be detached after failing`() = runTest {
        val sink = realSink()
        sink.attach()

        sink.detach()

        assertEquals(SinkState.DETACHED, sink.status.value.state)
    }

    @Test
    fun `attaching the shipped wiring twice settles the same way`() = runTest {
        // Idempotent by contract: the registry attaches on every mode change.
        val sink = realSink()
        sink.attach()
        val first = sink.status.value.state

        sink.attach()

        assertEquals(first, sink.status.value.state)
    }

    @Test
    fun `rendering through the shipped wiring never throws`() = runTest {
        // render() runs on the action path and fires many times a second while
        // an operator scrubs a deck.
        val sink = realSink()
        sink.attach()

        sink.render(envelope("Amazing grace"))

        assertTrue(sink.status.value.detail?.isNotBlank() == true)
    }

    @Test
    fun `a sink built with only a port is still a complete sink`() = runTest {
        // Every other argument has a default, and this is the shape a caller
        // writing one line gets.
        val sink = WebPageSink(preferredPort = 8080)

        assertEquals(WEB_PAGE_SINK_ID, sink.id)
    }

    @Test
    fun `a sink built with only a port starts detached`() = runTest {
        val sink = WebPageSink(preferredPort = 8080)

        assertEquals(SinkState.DETACHED, sink.status.value.state)
    }

    @Test
    fun `a sink built with only a port is named for the outputs sheet`() = runTest {
        val sink = WebPageSink(preferredPort = 8080)

        assertEquals("Browser screen", sink.status.value.displayName)
    }

    @Test
    fun `a sink built with only a port can be rendered to harmlessly`() = runTest {
        // Nothing is attached; the frame is kept rather than lost or thrown.
        val sink = WebPageSink(preferredPort = 8080)

        sink.render(envelope())

        assertEquals(0, sink.status.value.clientCount)
    }

    @Test
    fun `a sink built with only a port can be detached harmlessly`() = runTest {
        val sink = WebPageSink(preferredPort = 8080)

        sink.detach()

        assertEquals(SinkState.DETACHED, sink.status.value.state)
    }

    @Test
    fun `a sink built with only a port can be disposed harmlessly`() = runTest {
        val sink = WebPageSink(preferredPort = 8080)

        sink.dispose()

        assertNull(sink.status.value.detail)
    }

    @Test
    fun `the photo source defaults to serving no photos`() = runTest {
        // A sink built without one still serves the display page; it simply has
        // no pictures behind it.
        val sink = WebPageSink(preferredPort = 8080)

        assertEquals(WEB_PAGE_SINK_ID, sink.id)
        assertNull(PhotoSource.NONE.photo("anything"))
    }
}
