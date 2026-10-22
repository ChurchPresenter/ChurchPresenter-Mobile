package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Which elements a lasso or an eraser touches. */
class CanvasHitTestTest {

    private fun p(x: Float, y: Float) = CanvasPoint(x, y)

    private val inside = CanvasBounds(20f, 20f, 40f, 40f)
    private val straddling = CanvasBounds(90f, 20f, 130f, 40f)
    private val outside = CanvasBounds(200f, 200f, 220f, 220f)
    private val candidates = mapOf("inside" to inside, "straddling" to straddling, "outside" to outside)

    /** A square loop from (0,0) to (100,100). */
    private val loop = listOf(p(0f, 0f), p(100f, 0f), p(100f, 100f), p(0f, 100f))

    @Test
    fun `bounds of points span them, and nothing has no bounds`() {
        assertEquals(CanvasBounds(-1f, 2f, 5f, 9f), boundsOf(listOf(p(5f, 2f), p(-1f, 9f))))
        assertNull(boundsOf(emptyList()))
    }

    @Test
    fun `contains and intersects`() {
        val box = CanvasBounds(0f, 0f, 100f, 100f)
        assertTrue(box.contains(p(100f, 0f)), "edges count")
        assertFalse(box.contains(p(101f, 50f)))
        assertTrue(box.contains(inside))
        assertFalse(box.contains(straddling))
        assertTrue(box.intersects(straddling))
        assertFalse(box.intersects(outside))
    }

    @Test
    fun `points inside a loop are inside`() {
        assertTrue(inPolygon(p(50f, 50f), loop))
        assertFalse(inPolygon(p(150f, 50f), loop))
        val triangle = listOf(p(0f, 0f), p(100f, 0f), p(0f, 100f))
        assertFalse(inPolygon(p(90f, 90f), triangle), "outside the hypotenuse")
    }

    @Test
    fun `a rectangle lasso picks what it encloses, or touches when partial`() {
        assertEquals(listOf("inside"), lassoPick(candidates, loop, rectangle = true, partial = false))
        assertEquals(listOf("inside", "straddling"), lassoPick(candidates, loop, rectangle = true, partial = true))
    }

    @Test
    fun `a freeform lasso picks what lies within its loop`() {
        assertEquals(listOf("inside"), lassoPick(candidates, loop, rectangle = false, partial = false))
        assertEquals(listOf("inside", "straddling"), lassoPick(candidates, loop, rectangle = false, partial = true))
    }

    @Test
    fun `a lasso with no points picks nothing`() {
        assertTrue(lassoPick(candidates, emptyList(), rectangle = true, partial = true).isEmpty())
    }

    @Test
    fun `the stroke eraser reaches as far as its tip and the stroke's own width`() {
        val strokes = mapOf(
            "near" to (listOf(p(0f, 0f), p(10f, 0f)) to 2f),
            "far" to (listOf(p(0f, 50f)) to 2f),
        )
        assertEquals(listOf("near"), strokeHits(strokes, listOf(p(10f, 8f)), radius = 6f))
        assertTrue(strokeHits(strokes, listOf(p(10f, 9f)), radius = 6f).isEmpty(), "8 dp away is out of reach")
    }

    @Test
    fun `the area eraser takes what its box touches`() {
        assertEquals(listOf("inside", "straddling"), areaHits(candidates, listOf(p(0f, 0f), p(100f, 100f))))
        assertTrue(areaHits(candidates, emptyList()).isEmpty())
    }

    @Test
    fun `an eraser passing near a box takes it`() {
        assertEquals(listOf("inside"), boundsHits(candidates, listOf(p(15f, 30f)), radius = 6f))
        assertTrue(boundsHits(candidates, listOf(p(10f, 30f)), radius = 6f).isEmpty())
    }
}
