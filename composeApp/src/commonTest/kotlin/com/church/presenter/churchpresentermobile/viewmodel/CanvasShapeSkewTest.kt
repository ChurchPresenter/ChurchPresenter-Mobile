package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasPage
import com.church.presenter.churchpresentermobile.model.CanvasTool
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Skew is a shape option like its fill: new shapes take it, a selected one is
 * changed by it, and the panel shows it.
 */
class CanvasShapeSkewTest {

    private var next = 0
    private val newId: () -> String = { "new-${++next}" }

    private fun CanvasUiState.go(vararg intents: CanvasIntent): CanvasUiState =
        intents.fold(this) { s, intent -> s.reduce(intent, newId) }

    private val shapesTool = CanvasUiState(document = CanvasDocument(pages = listOf(CanvasPage()))).go(
        CanvasIntent.PickTool(CanvasTool.SHAPES),
    )

    private fun CanvasUiState.leaning(x: Float, y: Float) =
        go(CanvasIntent.SetOptions(options.copy(shapes = options.shapes.copy(skewX = x, skewY = y))))

    private fun CanvasUiState.onlyShape() = page.elements.single() as CanvasElement.Shape

    @Test
    fun `a new shape leans as the options say`() {
        val placed = shapesTool.leaning(-15f, 10f).go(CanvasIntent.PlaceShape(CanvasBounds(0f, 0f, 100f, 80f)))

        assertEquals(-15f, placed.onlyShape().skewX)
        assertEquals(10f, placed.onlyShape().skewY)
    }

    @Test
    fun `a shape placed with no skew stands straight`() {
        val placed = shapesTool.go(CanvasIntent.PlaceShape(CanvasBounds(0f, 0f, 100f, 80f)))

        assertEquals(0f, placed.onlyShape().skewX)
        assertEquals(0f, placed.onlyShape().skewY)
    }

    @Test
    fun `changing the skew leans the selected shape`() {
        val placed = shapesTool.go(CanvasIntent.PlaceShape(CanvasBounds(0f, 0f, 100f, 80f)))
        val selected = placed.go(CanvasIntent.Select(placed.onlyShape().id))

        val leaned = selected.leaning(20f, 0f)

        assertEquals(20f, leaned.onlyShape().skewX)
    }

    @Test
    fun `selecting a leaning shape shows its skew in the panel`() {
        val placed = shapesTool.leaning(-30f, 5f).go(CanvasIntent.PlaceShape(CanvasBounds(0f, 0f, 100f, 80f)))
        // A placed shape is selected; let go of it first so the reset changes only the panel.
        val reset = placed.go(CanvasIntent.Select(null)).leaning(0f, 0f)

        val selected = reset.go(CanvasIntent.Select(reset.onlyShape().id))

        assertEquals(-30f, selected.options.shapes.skewX)
        assertEquals(5f, selected.options.shapes.skewY)
    }
}
