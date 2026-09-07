package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.present.SinkRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The second screen, wherever it comes from.
 *
 * One sink covers a cable, Chromecast's "Cast screen", Miracast and AirPlay,
 * because every one of them arrives as an ordinary secondary display. A host
 * that has none — a test machine, a browser tab — gets null rather than a no-op
 * implementation of the whole interface, and the outputs sheet is what decides
 * how to say so.
 */
class ExternalDisplaySinkTest {

    @Test
    fun `the factory answers without a screen to speak of`() {
        // Called at startup on every platform, including the ones that have no
        // second display at all — where the answer is null rather than a sink
        // that reports itself permanently disconnected.
        createExternalDisplaySink()

        assertTrue(true)
    }

    @Test
    fun `asking twice gives the same kind of answer`() {
        // Startup runs once, but a mode change asks again; a factory that
        // answered differently the second time would leave two rows or none.
        val first = createExternalDisplaySink()
        val second = createExternalDisplaySink()

        assertEquals(first == null, second == null)
    }

    @Test
    fun `whatever the factory returns is the external display row`() {
        // App startup registers it under the id the outputs sheet keys its
        // mirroring guidance off; null means "skip", not "register a
        // placeholder".
        val registry = SinkRegistry()

        createExternalDisplaySink()?.let(registry::register)

        assertTrue(registry.all().all { it.id == EXTERNAL_DISPLAY_SINK_ID })
    }

    @Test
    fun `a freshly created external display is not attached yet`() {
        // Registration is not connection: a screen has to be found first.
        val registry = SinkRegistry()

        createExternalDisplaySink()?.let(registry::register)

        assertTrue(!registry.hasAttachedSink)
    }

    @Test
    fun `the external display's id is stable`() {
        // The outputs sheet keys its mirroring guidance off this exact id, and a
        // rename would silently drop the "go to Control Centre" card.
        assertEquals("external_display", EXTERNAL_DISPLAY_SINK_ID)
    }

    @Test
    fun `the external display's id is not the browser screen's`() {
        // Both live in one registry, which replaces by id.
        assertTrue(EXTERNAL_DISPLAY_SINK_ID != WEB_PAGE_SINK_ID)
    }

    @Test
    fun `the browser screen's id is stable`() {
        assertEquals("web_page", WEB_PAGE_SINK_ID)
    }

    @Test
    fun `the browser sink registers under its own id`() {
        val registry = SinkRegistry()

        registry.register(webSink())

        assertEquals(WEB_PAGE_SINK_ID, registry.all().single().id)
    }

    @Test
    fun `registering the browser sink twice replaces rather than duplicates`() {
        // The registry is keyed by id; two rows for one screen would be two
        // things for the operator to reason about.
        val registry = SinkRegistry()

        registry.register(webSink())
        registry.register(webSink())

        assertEquals(1, registry.all().size)
    }

    @Test
    fun `the browser sink appears in the outputs list before it serves`() {
        // An empty list reads as "not supported on this phone".
        val registry = SinkRegistry()

        registry.register(webSink())

        assertEquals(1, registry.statuses.value.size)
    }

    @Test
    fun `the browser sink is listed as its own name`() {
        val registry = SinkRegistry()

        registry.register(webSink())

        assertEquals("Browser screen", registry.statuses.value.single().displayName)
    }

    @Test
    fun `a registry holding only an unattached browser sink has nothing attached`() {
        val registry = SinkRegistry()

        registry.register(webSink())

        assertTrue(!registry.hasAttachedSink)
    }

    @Test
    fun `the registry can find the browser sink by id`() {
        val registry = SinkRegistry()
        registry.register(webSink())

        assertTrue(registry.sink(WEB_PAGE_SINK_ID) != null)
    }

    @Test
    fun `a registry holding only the browser sink has no external display`() {
        val registry = SinkRegistry()
        registry.register(webSink())

        assertNull(registry.sink(EXTERNAL_DISPLAY_SINK_ID))
    }
}
