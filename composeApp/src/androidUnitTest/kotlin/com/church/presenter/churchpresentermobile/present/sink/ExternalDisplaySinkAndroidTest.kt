package com.church.presenter.churchpresentermobile.present.sink

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.present.OutputSink
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.util.ActivityHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Projecting onto a second screen — HDMI, USB-C DisplayPort, Miracast, or a
 * Chromecast session started from the system's own "Cast screen" tile.
 *
 * What the outputs sheet says about that screen is the whole interface the
 * operator has to it, and the states it moves between are easy to get wrong in
 * ways nobody notices until the TV is unplugged mid-service: a row that still
 * advertises "Overlay #1 · 1920×1080" for a display in someone's bag reads as
 * connected, and the operator keeps pressing Project at nothing.
 *
 * `DisplayManager` and `Display` are final and inert in the unit-test
 * `android.jar`, so they are mocked. The sink itself is real, including the
 * listener it registers.
 */
class ExternalDisplaySinkAndroidTest {

    @AfterTest
    fun cleanUp() {
        ActivityHolder.current?.let { ActivityHolder.detach(it) }
        unmockkAll()
    }

    private val manager = mockk<DisplayManager>(relaxed = true)

    /** A secondary screen Android would offer for presentation. */
    private fun display(
        id: Int = 1,
        name: String? = "Living room TV",
        width: Int = 1920,
        height: Int = 1080,
    ): Display = mockk(relaxed = true) {
        every { displayId } returns id
        every { this@mockk.name } returns name
        every { mode } returns mockk(relaxed = true) {
            every { physicalWidth } returns width
            every { physicalHeight } returns height
        }
    }

    /** Attaches an Activity whose display service is [manager], and reports what it offers. */
    private fun withDisplays(vararg displays: Display) {
        every { manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns
            arrayOf(*displays)
        val activity = mockk<Activity>(relaxed = true) {
            every { getSystemService(Context.DISPLAY_SERVICE) } returns manager
            every { isFinishing } returns false
            every { isDestroyed } returns false
        }
        ActivityHolder.attach(activity)
    }

    private fun sink(): OutputSink = assertNotNull(createExternalDisplaySink())

    /** The listener the sink registered, so display changes can be delivered to it. */
    private fun registeredListener(): DisplayManager.DisplayListener {
        val listener = slot<DisplayManager.DisplayListener>()
        verify { manager.registerDisplayListener(capture(listener), any()) }
        return listener.captured
    }

    // ── Before anything is attached ──────────────────────────────────────

    @Test
    fun `a fresh sink is detached and named for the outputs sheet`() {
        val status = sink().status.value

        assertEquals(EXTERNAL_DISPLAY_SINK_ID, status.id)
        assertEquals(SinkState.DETACHED, status.state)
        assertEquals("External display", status.displayName)
    }

    @Test
    fun `nothing is rendered through this sink`() {
        // Content reaches the presentation window through SlideBus, so a display
        // plugged in mid-service is already up to date rather than waiting for
        // the next slide change.
        val externalSink = sink()

        externalSink.render(SlideEnvelope(slide = Slide.BLANK))

        assertEquals(SinkState.DETACHED, externalSink.status.value.state)
    }

    // ── Finding a screen ─────────────────────────────────────────────────

    @Test
    fun `attaching with a screen plugged in reports it by name`() = runTest {
        withDisplays(display(name = "Living room TV"))
        val externalSink = sink()

        externalSink.attach()

        assertEquals(SinkState.ATTACHED, externalSink.status.value.state)
        assertEquals("Living room TV", externalSink.status.value.displayName)
    }

    @Test
    fun `the resolution is reported alongside the name`() = runTest {
        withDisplays(display(width = 3840, height = 2160))
        val externalSink = sink()

        externalSink.attach()

        assertEquals("3840×2160", externalSink.status.value.detail)
    }

    @Test
    fun `an attached screen counts as one client`() = runTest {
        withDisplays(display())
        val externalSink = sink()

        externalSink.attach()

        assertEquals(1, externalSink.status.value.clientCount)
    }

    @Test
    fun `a screen that will not say its name gets the generic one`() = runTest {
        withDisplays(display(name = null))
        val externalSink = sink()

        externalSink.attach()

        assertEquals("External display", externalSink.status.value.displayName)
    }

    @Test
    fun `a screen with a blank name gets the generic one too`() = runTest {
        withDisplays(display(name = "   "))
        val externalSink = sink()

        externalSink.attach()

        assertEquals("External display", externalSink.status.value.displayName)
    }

    @Test
    fun `a screen that reports no mode is still attached to`() = runTest {
        // Some cast receivers do not; showing 0×0 beats refusing to project.
        val screen = mockk<Display>(relaxed = true) {
            every { displayId } returns 1
            every { name } returns "Chromecast"
            every { mode } returns null
        }
        withDisplays(screen)
        val externalSink = sink()

        externalSink.attach()

        assertEquals(SinkState.ATTACHED, externalSink.status.value.state)
        assertEquals("0×0", externalSink.status.value.detail)
    }

    @Test
    fun `the first presentation screen is the one used`() = runTest {
        withDisplays(display(id = 1, name = "First"), display(id = 2, name = "Second"))
        val externalSink = sink()

        externalSink.attach()

        assertEquals("First", externalSink.status.value.displayName)
    }

    // ── Waiting for a screen ─────────────────────────────────────────────

    @Test
    fun `attaching with nothing plugged in keeps looking rather than failing`() = runTest {
        // "Searching" is the honest state: the operator has turned the output on
        // and has yet to plug the cable in.
        withDisplays()
        val externalSink = sink()

        externalSink.attach()

        assertEquals(SinkState.ATTACHING, externalSink.status.value.state)
        assertEquals("External display", externalSink.status.value.displayName)
        assertNull(externalSink.status.value.detail)
    }

    @Test
    fun `a screen plugged in after attaching is picked up`() = runTest {
        // The listener is why the operator does not have to toggle the output off
        // and on again after finding the cable.
        withDisplays()
        val externalSink = sink()
        externalSink.attach()

        every { manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns
            arrayOf(display(name = "Projector"))
        registeredListener().onDisplayAdded(1)

        assertEquals(SinkState.ATTACHED, externalSink.status.value.state)
        assertEquals("Projector", externalSink.status.value.displayName)
    }

    @Test
    fun `a screen whose mode changes is re-read`() = runTest {
        withDisplays(display(width = 1280, height = 720))
        val externalSink = sink()
        externalSink.attach()

        every { manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns
            arrayOf(display(id = 2, width = 1920, height = 1080))
        registeredListener().onDisplayChanged(2)

        assertEquals("1920×1080", externalSink.status.value.detail)
    }

    @Test
    fun `attaching with no Activity to hang the window on does not crash`() = runTest {
        // A Presentation is a Dialog and needs a real Activity; there is none
        // before the first screen is drawn.
        val externalSink = sink()

        externalSink.attach()

        assertEquals(SinkState.ATTACHING, externalSink.status.value.state)
    }

    // ── Turning it off ───────────────────────────────────────────────────

    @Test
    fun `attaching twice does not register a second listener`() = runTest {
        // Every mode change calls attach; stacking listeners would leak them and
        // re-run the search once per stacked copy.
        withDisplays(display())
        val externalSink = sink()

        externalSink.attach()
        externalSink.attach()

        verify(exactly = 1) { manager.registerDisplayListener(any(), any()) }
    }

    @Test
    fun `detaching returns the sink to a clean state`() = runTest {
        withDisplays(display())
        val externalSink = sink()
        externalSink.attach()

        externalSink.detach()

        assertEquals(SinkState.DETACHED, externalSink.status.value.state)
        assertEquals("External display", externalSink.status.value.displayName)
        assertNull(externalSink.status.value.detail)
        assertEquals(0, externalSink.status.value.clientCount)
    }

    @Test
    fun `detaching stops listening for displays`() = runTest {
        withDisplays(display())
        val externalSink = sink()
        externalSink.attach()

        externalSink.detach()

        verify { manager.unregisterDisplayListener(any()) }
    }

    @Test
    fun `detaching twice is harmless`() = runTest {
        withDisplays(display())
        val externalSink = sink()
        externalSink.attach()

        externalSink.detach()
        externalSink.detach()

        assertEquals(SinkState.DETACHED, externalSink.status.value.state)
    }

    @Test
    fun `a sink can be attached again after being detached`() = runTest {
        // Turning the output off and on again is the operator's first fix.
        withDisplays(display())
        val externalSink = sink()
        externalSink.attach()
        externalSink.detach()

        externalSink.attach()

        assertEquals(SinkState.ATTACHED, externalSink.status.value.state)
        assertTrue(externalSink.status.value.isAttached)
    }

    @Test
    fun `detaching before attaching is harmless`() = runTest {
        sink().detach()
    }
}
