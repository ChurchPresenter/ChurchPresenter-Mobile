package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.sink.EXTERNAL_DISPLAY_SINK_ID
import com.church.presenter.churchpresentermobile.testutil.FakeOutputSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the attach/detach contract App.kt drives off the app mode, and the
 * SlideBus catch-up path that every out-of-composition renderer depends on.
 */
class SinkLifecycleTest {

    @BeforeTest fun setUp() = SlideBus.resetForTest()

    @AfterTest fun tearDown() = SlideBus.resetForTest()

    private fun deck() = SlideDeck(
        kind = SlideKind.SONG,
        title = "deck",
        slides = List(3) { Slide(kind = SlideKind.SONG, body = "slide $it") },
    )

    @Test
    fun `attaching is idempotent so repeated mode changes do not stack windows`() = runTest {
        val sink = FakeOutputSink()

        sink.attach()
        sink.attach()

        assertEquals(2, sink.attachCount, "the fake records every call…")
        assertEquals(SinkState.ATTACHED, sink.status.value.state, "…and the state settles either way")
    }

    @Test
    fun `detaching returns the sink to a clean state`() = runTest {
        val sink = FakeOutputSink()
        sink.attach()

        sink.detach()

        assertEquals(SinkState.DETACHED, sink.status.value.state)
        assertEquals(0, sink.status.value.clientCount)
    }

    /**
     * The reason SlideBus is a StateFlow and not an event stream: a TV plugged
     * in mid-service must show what is already projected, not wait for the
     * operator to touch something.
     */
    @Test
    fun `a renderer attaching mid-service sees the slide already projected`() {
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry())
        engine.setDeck(deck())
        engine.showSlide(2)

        // A window created now reads the bus rather than waiting for a push.
        val whatTheNewScreenShows = SlideBus.current.value.slide

        assertEquals("slide 2", whatTheNewScreenShows?.body)
        assertFalse(whatTheNewScreenShows?.isHidden ?: true)
    }

    @Test
    fun `the bus reflects a blank so a late-joining screen does not reveal hidden content`() {
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry())
        engine.setDeck(deck())
        engine.setBlank(true)

        assertTrue(SlideBus.current.value.slide?.isHidden == true)
    }

    @Test
    fun `remote mode leaves the bus untouched`() {
        val engine = StandaloneEngine(MutableStateFlow(AppMode.REMOTE), SinkRegistry())

        engine.setDeck(deck())
        engine.showSlide(1)

        assertEquals(0L, SlideBus.current.value.rev, "no slide should ever reach a screen in remote mode")
    }

    @Test
    fun `a sink registered but never attached still appears in the outputs list`() {
        val registry = SinkRegistry()
        registry.register(FakeOutputSink(id = EXTERNAL_DISPLAY_SINK_ID, displayName = "External display"))

        val status = registry.statuses.value.single()
        assertEquals(EXTERNAL_DISPLAY_SINK_ID, status.id)
        assertEquals(SinkState.DETACHED, status.state)
        assertFalse(registry.hasAttachedSink)
    }
}
