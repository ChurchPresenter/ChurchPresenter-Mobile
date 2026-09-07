package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.sink.WEB_PAGE_SINK_ID
import com.church.presenter.churchpresentermobile.present.sink.WebPageSink
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the browser-screen sink reports when it cannot serve.
 *
 * The sink's whole value is the address it hands the operator to type into a
 * TV, so the failure that matters most is a *silent* one: a sink that says it
 * is attached without a server behind it leaves the operator reading out a URL
 * that answers nothing, mid-service, with no error anywhere to explain it.
 *
 * There is no server in a unit test — no LAN address on a build agent, and no
 * bundled display assets to read — so every attach here fails. That is exactly
 * the state under test. Which of the two reasons is reported depends on the
 * machine, so these assert the parts that must hold either way.
 */
class WebPageSinkTest {

    private fun sink(
        onPortBound: (Int) -> Unit = {},
        onBaseUrl: (String?) -> Unit = {},
    ) = WebPageSink(preferredPort = 0, onPortBound = onPortBound, onBaseUrl = onBaseUrl)

    private fun envelope(body: String) =
        SlideEnvelope(slide = Slide(kind = SlideKind.SONG, body = body))

    @Test
    fun `a fresh sink is detached and named for the outputs sheet`() {
        val status = sink().status.value

        assertEquals(WEB_PAGE_SINK_ID, status.id)
        assertEquals(SinkState.DETACHED, status.state)
        assertEquals("Browser screen", status.displayName)
        assertFalse(status.isAttached)
    }

    @Test
    fun `no screen is connected before anything is attached`() {
        assertEquals(0, sink().status.value.clientCount)
        assertNull(sink().status.value.detail)
    }

    @Test
    fun `an attach with no server behind it never reports as attached`() = runTest {
        // The failure this exists to prevent: an operator reading a URL off the
        // phone that nothing answers.
        val webSink = sink()

        webSink.attach()

        assertFalse(webSink.status.value.isAttached)
        assertEquals(SinkState.ERROR, webSink.status.value.state)
    }

    @Test
    fun `a failed attach says why`() = runTest {
        // The reason is what the outputs row shows in red; without it the row is
        // just a red dot.
        val webSink = sink()

        webSink.attach()

        assertTrue(webSink.status.value.detail?.isNotBlank() == true)
    }

    @Test
    fun `a failed attach hands out no address`() = runTest {
        // onBaseUrl is what tells the Photos screen it has somewhere to serve
        // from; a URL here would make it offer to project photos over a server
        // that never started.
        val urls = mutableListOf<String?>()

        sink(onBaseUrl = { urls += it }).attach()

        assertTrue(urls.isEmpty(), "an address was offered for a server that never started: $urls")
    }

    @Test
    fun `a failed attach reports no bound port`() = runTest {
        // The port is remembered between services so the operator gets a stable
        // URL; remembering one that was never bound would be worse than none.
        var port: Int? = null

        sink(onPortBound = { port = it }).attach()

        assertNull(port)
    }

    @Test
    fun `attaching twice after a failure is not treated as already attached`() = runTest {
        // The idempotence guard keys off a running server, so a failed attach
        // must leave the sink retryable — the operator's fix is usually to join
        // the Wi-Fi and press it again.
        val webSink = sink()

        webSink.attach()
        webSink.attach()

        assertEquals(SinkState.ERROR, webSink.status.value.state)
    }

    @Test
    fun `rendering to a sink that never attached is silently ignored`() {
        // render() is called from the action path many times a second while an
        // operator scrubs a deck; it must never throw or block.
        val webSink = sink()

        webSink.render(envelope("verse one"))
        webSink.render(envelope("verse two"))

        assertEquals(SinkState.DETACHED, webSink.status.value.state)
    }

    @Test
    fun `detaching a sink that never attached returns it to a clean state`() = runTest {
        val webSink = sink()

        webSink.detach()

        assertEquals(SinkState.DETACHED, webSink.status.value.state)
        assertNull(webSink.status.value.detail)
        assertEquals(0, webSink.status.value.clientCount)
    }

    @Test
    fun `detaching clears the address so the photo screen stops offering to serve`() = runTest {
        val urls = mutableListOf<String?>()

        sink(onBaseUrl = { urls += it }).detach()

        assertEquals(listOf<String?>(null), urls)
    }

    @Test
    fun `detaching after a failed attach clears the error`() = runTest {
        // Leaving the red row up after the operator has turned the output off
        // reads as a fault that is still happening.
        val webSink = sink()
        webSink.attach()

        webSink.detach()

        assertEquals(SinkState.DETACHED, webSink.status.value.state)
        assertNull(webSink.status.value.detail)
    }

    @Test
    fun `detaching twice is harmless`() = runTest {
        val webSink = sink()

        webSink.detach()
        webSink.detach()

        assertEquals(SinkState.DETACHED, webSink.status.value.state)
    }

    @Test
    fun `disposing releases the sink and can be done after a detach`() = runTest {
        val webSink = sink()
        webSink.detach()

        webSink.dispose()

        assertEquals(SinkState.DETACHED, webSink.status.value.state)
    }
}
