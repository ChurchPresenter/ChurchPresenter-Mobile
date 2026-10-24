package com.church.presenter.churchpresentermobile.model

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The rotate knob's geometry: angles, turned boxes and resizing a turned element. */
class CanvasRotateTest {

    private fun assertNear(expected: Float, actual: Float, message: String? = null) =
        assertTrue(abs(expected - actual) < 0.01f, "${message.orEmpty()} expected $expected, was $actual")

    private fun assertNear(expected: CanvasBounds, actual: CanvasBounds) {
        assertNear(expected.left, actual.left, "left")
        assertNear(expected.top, actual.top, "top")
        assertNear(expected.right, actual.right, "right")
        assertNear(expected.bottom, actual.bottom, "bottom")
    }

    @Test
    fun anglesAreKeptBetweenMinusAndPlusAHalfTurn() {
        assertEquals(0f, normalizedAngle(360f))
        assertEquals(-90f, normalizedAngle(270f))
        assertEquals(180f, normalizedAngle(-180f))
        assertEquals(45f, normalizedAngle(405f))
    }

    @Test
    fun theKnobSnapsToFifteenDegreesAndLandsOnQuarterTurns() {
        assertEquals(15f, snappedAngle(19f))
        assertEquals(90f, snappedAngle(84f))
        assertEquals(0f, snappedAngle(-7f))
        assertEquals(180f, snappedAngle(-176f), "a half turn is stored one way")
    }

    @Test
    fun aQuarterTurnComesBackRoundAfterFour() {
        assertEquals(90f, quarterTurned(0f))
        assertEquals(-90f, quarterTurned(180f))
        assertEquals(0f, (1..4).fold(0f) { a, _ -> quarterTurned(a) })
    }

    @Test
    fun aFingerAboveTheCentreIsUprightAndToTheRightIsAQuarterTurn() {
        val centre = CanvasPoint(100f, 100f)
        assertNear(0f, knobAngle(centre, CanvasPoint(100f, 0f)))
        assertNear(90f, knobAngle(centre, CanvasPoint(200f, 100f)))
        assertNear(-90f, knobAngle(centre, CanvasPoint(0f, 100f)))
        assertNear(180f, knobAngle(centre, CanvasPoint(100f, 200f)))
    }

    @Test
    fun aTurnedBoxCoversItsTurnedCorners() {
        val box = CanvasBounds(0f, 0f, 200f, 100f)
        assertEquals(box, rotatedBounds(box, 0f), "upright stays as it is")
        assertNear(CanvasBounds(50f, -50f, 150f, 150f), rotatedBounds(box, 90f))
    }

    @Test
    fun aTurnedElementIsHitWhereItIsDrawn() {
        // A long thin bar turned upright: its old ends are no longer on it.
        val bar = CanvasBounds(0f, 90f, 200f, 110f)
        assertTrue(turnedContains(bar, 0f, CanvasPoint(10f, 100f)))
        assertFalse(turnedContains(bar, 90f, CanvasPoint(10f, 100f)), "the end turned away")
        assertTrue(turnedContains(bar, 90f, CanvasPoint(100f, 10f)), "where the end turned to")
    }

    @Test
    fun resizingAnUprightElementIsTheUsualResize() {
        val box = CanvasBounds(0f, 0f, 100f, 100f)
        assertEquals(resizedBounds(box, 4, 20f, 10f), rotatedResize(box, 4, 20f, 10f, 0f))
    }

    @Test
    fun resizingATurnedElementStretchesAlongItsOwnEdgeAndKeepsTheOppositeHandle() {
        val box = CanvasBounds(0f, 0f, 100f, 50f)
        val angle = 90f
        // Turned a quarter, the right-edge handle points down the page: dragging down widens it.
        val resized = rotatedResize(box, 3, 0f, 40f, angle)
        assertNear(140f, resized.width, "width")
        assertNear(50f, resized.height, "height")
        val before = rotatedPoint(handlePoint(box, 7), angle, box.centre)
        val after = rotatedPoint(handlePoint(resized, 7), angle, resized.centre)
        assertNear(before.x, after.x, "opposite handle x")
        assertNear(before.y, after.y, "opposite handle y")
    }

    @Test
    fun everyKindOfElementTurns() {
        val elements = listOf<CanvasElement>(
            CanvasElement.Text("t", 0f, 0f, "Hi"),
            CanvasElement.Ink("i", 0f, 0f, "M0 0 L10 10", CanvasInk.RED, 4f),
            CanvasElement.Image("p", 0f, 0f, 10f, 10f, fileName = ""),
            CanvasElement.Shape("s", 0f, 0f, 10f, 10f, CanvasShape.STAR, CanvasInk.SKY),
            CanvasElement.Sticky("n", 0f, 0f, 10f, 10f, "Note"),
        )
        elements.forEach { assertEquals(30f, it.withAngle(30f).angle, it::class.simpleName) }
    }
}
