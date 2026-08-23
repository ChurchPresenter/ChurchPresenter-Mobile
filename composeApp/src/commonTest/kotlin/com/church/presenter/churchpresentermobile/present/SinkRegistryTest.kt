package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.present.sink.EXTERNAL_DISPLAY_SINK_ID
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.testutil.FakeOutputSink
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SinkRegistryTest {

    private fun envelope(rev: Long) = SlideEnvelope(rev = rev, slide = Slide(body = "slide $rev"))

    @Test
    fun `a new registry has no sinks and none attached`() {
        val registry = SinkRegistry()
        assertTrue(registry.statuses.value.isEmpty())
        assertFalse(registry.hasAttachedSink)
        assertNull(registry.sink("missing"))
    }

    @Test
    fun `broadcast reaches every registered sink`() {
        val registry = SinkRegistry()
        val a = FakeOutputSink(id = "a")
        val b = FakeOutputSink(id = "b")
        registry.register(a)
        registry.register(b)

        registry.broadcast(envelope(1))
        registry.broadcast(envelope(2))

        assertEquals(listOf(1L, 2L), a.rendered.map { it.rev })
        assertEquals(listOf(1L, 2L), b.rendered.map { it.rev })
    }

    @Test
    fun `a throwing sink does not stop the others`() {
        val registry = SinkRegistry()
        val broken = FakeOutputSink(id = "broken", failOnRender = true)
        val healthy = FakeOutputSink(id = "healthy")
        registry.register(broken)
        registry.register(healthy)

        registry.broadcast(envelope(1))

        assertEquals(1, healthy.rendered.size)
        assertTrue(broken.rendered.isEmpty())
    }

    @Test
    fun `registering the same id replaces the previous sink`() {
        val registry = SinkRegistry()
        val first = FakeOutputSink(id = "display")
        val second = FakeOutputSink(id = "display")
        registry.register(first)
        registry.register(second)

        registry.broadcast(envelope(1))

        assertEquals(1, registry.all().size)
        assertTrue(first.rendered.isEmpty())
        assertEquals(1, second.rendered.size)
    }

    @Test
    fun `unregistered sinks stop receiving broadcasts`() {
        val registry = SinkRegistry()
        val sink = FakeOutputSink(id = "web")
        registry.register(sink)
        registry.broadcast(envelope(1))

        registry.unregister("web")
        registry.broadcast(envelope(2))

        assertEquals(listOf(1L), sink.rendered.map { it.rev })
        assertTrue(registry.statuses.value.isEmpty())
    }

    @Test
    fun `statuses reflect attachment after a refresh`() = runTest {
        val registry = SinkRegistry()
        val sink = FakeOutputSink(id = "hdmi", displayName = "Sanctuary TV")
        registry.register(sink)

        assertEquals(SinkState.DETACHED, registry.statuses.value.single().state)
        assertFalse(registry.hasAttachedSink)

        sink.attach()
        registry.refreshStatuses()

        val status = registry.statuses.value.single()
        assertEquals(SinkState.ATTACHED, status.state)
        assertEquals("Sanctuary TV", status.displayName)
        assertEquals(1, status.clientCount)
        assertTrue(registry.hasAttachedSink)
    }

    @Test
    fun `detaching the only sink clears hasAttachedSink`() = runTest {
        val registry = SinkRegistry()
        val sink = FakeOutputSink(id = "hdmi")
        registry.register(sink)
        sink.attach()
        registry.refreshStatuses()

        sink.detach()
        registry.refreshStatuses()

        assertFalse(registry.hasAttachedSink)
        assertEquals(0, registry.statuses.value.single().clientCount)
    }

    @Test
    fun `sink lookup returns the registered instance`() {
        val registry = SinkRegistry()
        val sink = FakeOutputSink(id = "web")
        registry.register(sink)
        assertEquals(sink, registry.sink("web"))
    }
    @Test
    fun losingAScreenForgetsItsNameAndResolution() {
        // Unplugging a TV cleared the state and the client count but left "Overlay #1 ·
        // 1920x1080" sitting in the Screens row, which reads as still-connected. Both platforms
        // lose a display in two places each, so the forgetting lives here rather than in four.
        val attached = SinkStatus(
            id = EXTERNAL_DISPLAY_SINK_ID,
            displayName = "Overlay #1",
            state = SinkState.ATTACHED,
            detail = "1920x1080",
            clientCount = 1,
        )

        val searching = attached.searching("External display")

        assertEquals("External display", searching.displayName)
        assertEquals(SinkState.ATTACHING, searching.state)
        assertNull(searching.detail)
        assertEquals(0, searching.clientCount)
    }

    @Test
    fun aSinkLookingForAScreenIsNotReportedAsAttached() {
        val searching = SinkStatus(
            id = EXTERNAL_DISPLAY_SINK_ID,
            displayName = "Overlay #1",
            state = SinkState.ATTACHED,
        ).searching("External display")

        assertFalse(searching.isAttached)
    }

    @Test
    fun theSinksIdentitySurvivesLosingItsScreen() {
        // Only what described the screen is dropped — the row itself must stay put in the list.
        val searching = SinkStatus(
            id = EXTERNAL_DISPLAY_SINK_ID,
            displayName = "Overlay #1",
        ).searching("External display")

        assertEquals(EXTERNAL_DISPLAY_SINK_ID, searching.id)
    }
}
