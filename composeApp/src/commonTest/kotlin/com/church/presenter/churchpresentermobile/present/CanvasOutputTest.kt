package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.SlideKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The canvas's frames as the outputs fetch them, and the projector that puts them up. */
class CanvasOutputTest {

    private val baseUrl = MutableStateFlow<String?>("http://phone:8080")
    private val output = CanvasOutput(baseUrl)
    private val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry(), publish = {})
    private val projector = CanvasProjector(output, engine)

    private fun png(n: Int) = byteArrayOf(n.toByte())

    private fun idOf(url: String) = url.substringAfterLast('/')

    // ── Output ───────────────────────────────────────────────────────────

    @Test
    fun `no server, no frame`() {
        baseUrl.value = null
        assertFalse(output.canServe)
        assertNull(output.publish(png(1)))
    }

    @Test
    fun `each frame gets its own address on the photo route`() {
        val first = output.publish(png(1))!!
        val second = output.publish(png(2))!!
        assertTrue(first.startsWith("http://phone:8080/$PHOTO_ROUTE/$CANVAS_FRAME_PREFIX"), first)
        assertNotEquals(first, second)
        assertTrue(output.canServe)
    }

    @Test
    fun `a new run never hands out an address an earlier run used`() {
        // The screen's image cache is kept on disk: an address reused after a restart put up
        // the earlier run's picture — shapes since deleted, and none of the new ones.
        val earlier = (1..3).map { output.publish(png(it))!! }
        val later = CanvasOutput(baseUrl)

        val again = (1..3).map { later.publish(png(it))!! }

        assertTrue(again.none { it in earlier }, "$again reuses one of $earlier")
    }

    @Test
    fun `the latest two frames are served as png, older ones are not`() {
        val first = output.publish(png(1))!!
        val second = output.publish(png(2))!!
        val third = output.publish(png(3))!!
        assertNull(output.source.photo(idOf(first)))
        assertContentEquals(png(2), output.source.photo(idOf(second))!!.bytes)
        val served = output.source.photo(idOf(third))!!
        assertContentEquals(png(3), served.bytes)
        assertEquals("image/png", served.contentType)
        assertFalse(output.owns(first))
        assertTrue(output.owns(second))
        assertFalse(output.owns(null))
    }

    @Test
    fun `clearing forgets every frame`() {
        val url = output.publish(png(1))!!
        output.clear()
        assertFalse(output.owns(url))
        assertNull(output.source.photo(idOf(url)))
    }

    // ── Projector ────────────────────────────────────────────────────────

    @Test
    fun `go live takes the screen with a canvas slide`() = runTest {
        engine.setBlank(true)
        assertEquals(CanvasShowResult.SHOWN, projector.show(png(1), "#FFFFFF", "Sermon", takeOver = true))
        val slide = engine.currentSlide.value
        assertEquals(SlideKind.CANVAS, slide.kind)
        assertEquals("#FFFFFF", slide.paper)
        assertFalse(slide.isBlank, "going live lifts blank")
        assertTrue(projector.isShowing)
        assertEquals("Sermon", engine.deck.value.title)
    }

    @Test
    fun `a refresh only replaces a canvas that is still showing`() = runTest {
        assertEquals(
            CanvasShowResult.NOT_SHOWING,
            projector.show(png(1), "#000000", "", takeOver = false),
            "nothing to refresh yet",
        )
        projector.show(png(1), "#000000", "", takeOver = true)
        assertEquals(CanvasShowResult.SHOWN, projector.show(png(2), "#000000", "", takeOver = false))
        engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("Welcome")))
        assertFalse(projector.isShowing)
        assertEquals(
            CanvasShowResult.NOT_SHOWING,
            projector.show(png(3), "#000000", "", takeOver = false),
            "something else has the screen",
        )
    }

    @Test
    fun `this device's own outputs ask nobody's permission`() {
        assertFalse(projector.asksFirst)
    }

    @Test
    fun `nothing goes up while the server is down`() = runTest {
        baseUrl.value = null
        assertEquals(CanvasShowResult.FAILED, projector.show(png(1), "#000000", "", takeOver = true))
        assertFalse(projector.isShowing)
    }

    @Test
    fun `stop clears the screen only if the canvas is on it`() = runTest {
        projector.show(png(1), "#000000", "", takeOver = true)
        projector.stop()
        assertEquals(SlideKind.BLANK, engine.currentSlide.value.kind)

        projector.show(png(2), "#000000", "", takeOver = true)
        engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("Welcome")))
        projector.stop()
        assertEquals(SlideKind.ANNOUNCEMENT, engine.currentSlide.value.kind, "someone else's slide stays up")
    }

    @Test
    fun `showing follows the screen`() = runTest {
        projector.show(png(1), "#000000", "", takeOver = true)
        assertTrue(projector.showing.first())
        engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("Welcome")))
        assertFalse(projector.showing.first())
    }

    @Test
    fun `a canvas deck is one canvas slide carrying the frame and paper`() {
        val deck = SlideDeckBuilder.fromCanvas("http://phone/photo/canvas-1", "#1B1D26", "Talk")
        assertEquals(SlideKind.CANVAS, deck.kind)
        assertEquals("Talk", deck.title)
        val slide = deck.slides.single()
        assertEquals("http://phone/photo/canvas-1", slide.mediaUrl)
        assertEquals("#1B1D26", slide.paper)
        assertEquals(1, slide.total)
    }
}
