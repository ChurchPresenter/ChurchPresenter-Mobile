package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.image
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.ink
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.shape
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.sticky
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Where a dragged selection lands, and the box a resize handle drags out. */
class CanvasSnapTest {

    private val box = CanvasBounds(10f, 10f, 60f, 40f)

    @Test
    fun `with nothing to snap to, a move is the finger's`() {
        assertEquals(13f to -7f, snappedMove(box, 13f, -7f, grid = false, guides = emptyList()))
    }

    @Test
    fun `the grid puts the top-left corner on a grid line`() {
        val (dx, dy) = snappedMove(box, 30f, 5f, grid = true, guides = emptyList())
        assertEquals(0f, (box.left + dx) % CANVAS_GRID)
        assertEquals(0f, (box.top + dy) % CANVAS_GRID)
    }

    @Test
    fun `a guide pulls a nearby edge into line`() {
        val other = CanvasBounds(100f, 200f, 150f, 260f)
        // Moving right by 37 puts the right edge at 97 — 3 short of the other's left edge.
        val (dx, dy) = snappedMove(box, 37f, 0f, grid = false, guides = listOf(other))
        assertEquals(40f, dx)
        assertEquals(0f, dy)
    }

    @Test
    fun `a guide takes priority over the grid, and an edge out of reach is left alone`() {
        val other = CanvasBounds(100f, 500f, 150f, 560f)
        val (dx, _) = snappedMove(box, 37f, 0f, grid = true, guides = listOf(other))
        assertEquals(40f, dx)
        val (far, _) = snappedMove(box, 20f, 0f, grid = false, guides = listOf(other))
        assertEquals(20f, far)
    }

    @Test
    fun `every handle moves only the edges it sits on`() {
        val start = CanvasBounds(100f, 100f, 200f, 200f)
        val expected = listOf(
            CanvasBounds(110f, 120f, 200f, 200f),
            CanvasBounds(100f, 120f, 200f, 200f),
            CanvasBounds(100f, 120f, 210f, 200f),
            CanvasBounds(100f, 100f, 210f, 200f),
            CanvasBounds(100f, 100f, 210f, 220f),
            CanvasBounds(100f, 100f, 200f, 220f),
            CanvasBounds(110f, 100f, 200f, 220f),
            CanvasBounds(110f, 100f, 200f, 200f),
        )
        expected.forEachIndexed { handle, bounds ->
            assertEquals(bounds, resizedBounds(start, handle, 10f, 20f), "handle $handle")
        }
    }

    @Test
    fun `a resize never shrinks below the minimum size`() {
        val start = CanvasBounds(100f, 100f, 200f, 200f)
        val squashed = resizedBounds(start, 4, -500f, -500f)
        assertEquals(MIN_ELEMENT_SIZE, squashed.width)
        assertEquals(MIN_ELEMENT_SIZE, squashed.height)
        assertEquals(100f, squashed.left, "the opposite corner stays put")
        val fromTopLeft = resizedBounds(start, 0, 500f, 500f)
        assertEquals(200f, fromTopLeft.right)
        assertEquals(MIN_ELEMENT_SIZE, fromTopLeft.width)
    }

    @Test
    fun `only boxed elements resize`() {
        assertTrue(resizable(image("i")))
        assertTrue(resizable(shape("s")))
        assertTrue(resizable(sticky("n")))
        assertFalse(resizable(text("t")))
        assertFalse(resizable(ink("k")))
        val bounds = CanvasBounds(1f, 2f, 31f, 42f)
        listOf(image("i"), shape("s"), sticky("n")).forEach { element ->
            val fitted = resized(element, bounds)
            assertEquals(bounds.width, fitted.boxWidth(), "${element.id} width")
        }
        val text = text("t")
        assertSame(text, resized(text, bounds))
    }

    private fun CanvasElement.boxWidth(): Float = when (this) {
        is CanvasElement.Image -> width
        is CanvasElement.Shape -> width
        is CanvasElement.Sticky -> width
        else -> 0f
    }
}
