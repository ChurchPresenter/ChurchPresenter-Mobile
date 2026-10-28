package com.church.presenter.churchpresentermobile.ui.canvas

import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasShape
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What typing a number into the Transform section does to the selected item. */
class TransformFieldTest {

    private val shape = CanvasElement.Shape(
        id = "s", x = 100f, y = 50f, width = 200f, height = 120f, shape = CanvasShape.RECTANGLE, ink = CanvasInk.RED,
        angle = 30f,
    )
    private val box = CanvasBounds(100f, 50f, 300f, 170f)
    private val ink = CanvasElement.Ink("i", x = 0f, y = 0f, pathData = "M0 0 L10 10", ink = CanvasInk.RED, width = 4f)

    private fun edit(field: TransformField, value: Float, element: CanvasElement = shape) =
        transformEdit(element, box, field, value, session = 7)

    @Test
    fun `x and y move the item by the difference`() {
        assertEquals(CanvasIntent.Move(listOf("s"), 40f, 0f, 7), edit(TransformField.X, 140f))
        assertEquals(CanvasIntent.Move(listOf("s"), 0f, -20f, 7), edit(TransformField.Y, 30f))
    }

    @Test
    fun `width and height resize from the top left corner`() {
        assertEquals(CanvasIntent.Resize("s", CanvasBounds(100f, 50f, 350f, 170f), 7), edit(TransformField.WIDTH, 250f))
        assertEquals(CanvasIntent.Resize("s", CanvasBounds(100f, 50f, 300f, 130f), 7), edit(TransformField.HEIGHT, 80f))
    }

    @Test
    fun `a side cannot be typed down to nothing`() {
        val resize = edit(TransformField.WIDTH, 0f) as CanvasIntent.Resize
        assertEquals(8f, resize.bounds.width)
    }

    @Test
    fun `what sizes itself is not resized`() {
        assertNull(edit(TransformField.WIDTH, 50f, element = ink))
    }

    @Test
    fun `a turn is kept between 0 and 360`() {
        assertEquals(CanvasIntent.Rotate("s", 270f, 7), edit(TransformField.ROTATION, -90f))
        assertEquals(CanvasIntent.Rotate("s", 45f, 7), edit(TransformField.ROTATION, 405f))
    }

    @Test
    fun `typing what is there already changes nothing`() {
        assertNull(edit(TransformField.X, 100f))
        assertNull(edit(TransformField.WIDTH, 200f))
        assertNull(edit(TransformField.ROTATION, 390f), "390 is the same turn as 30")
    }
}
