package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What a finger on the board puts there, tool by tool. Each tool is picked with
 * its options closed, so the gesture lands on the board and not on the panel.
 */
@OptIn(ExperimentalTestApi::class)
class CanvasDrawingGesturesTest {

    @Test
    fun aPenStrokeAddsInkToThePage() = runComposeUiTest {
        val vm = showCanvas()
        val before = vm.state.value.page.elements.size

        onBoard { dragOnPage(500f, 560f, 700f, 600f) }

        val added = vm.state.value.page.elements.last()
        assertEquals(before + 1, vm.state.value.page.elements.size)
        assertTrue(added is CanvasElement.Ink && added.kind == InkKind.PEN)
        assertTrue(exists(CanvasTags.element(added.id)), "the stroke is drawn on the board")
    }

    @Test
    fun aHighlighterStrokeIsAHighlight() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.HIGHLIGHTER), CanvasIntent.CloseOptions)

        onBoard { dragOnPage(500f, 560f, 700f, 560f) }

        val highlights = vm.state.value.page.elements
            .filter { it is CanvasElement.Ink && it.kind == InkKind.HIGHLIGHTER }
        assertEquals(2, highlights.size, "the sample's and the new one")
    }

    @Test
    fun aTapWithTheTextToolOpensABoxToTypeIn() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.TEXT), CanvasIntent.CloseOptions)

        onBoard { click(page(800f, 140f)) }
        val box = vm.state.value.page.elements.last()
        assertTrue(box is CanvasElement.Text)
        assertEquals(box.id, vm.state.value.editingId)

        onNode(hasSetTextAction()).performTextInput("Psalm 23")

        assertEquals("Psalm 23", (vm.state.value.page.elements.last() as CanvasElement.Text).text)
    }

    @Test
    fun aTapWithTheStickyToolPlacesANoteToWriteOn() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.STICKY), CanvasIntent.CloseOptions)

        onBoard { click(page(820f, 160f)) }

        val note = vm.state.value.page.elements.last()
        assertTrue(note is CanvasElement.Sticky)
        assertEquals(note.id, vm.state.value.editingId)
    }

    @Test
    fun draggingWithTheShapeToolDrawsAShapeThatSize() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SHAPES), CanvasIntent.CloseOptions)

        onBoard { dragOnPage(600f, 500f, 760f, 600f) }

        val shape = vm.state.value.page.elements.last() as CanvasElement.Shape
        assertTrue(shape.width > MIN_DRAWN && shape.height > MIN_DRAWN, "drawn ${shape.width}×${shape.height}")
    }

    @Test
    fun draggingWithTheLineToolDrawsALine() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.LINE), CanvasIntent.CloseOptions)

        onBoard { dragOnPage(600f, 600f, 800f, 620f) }

        val line = vm.state.value.page.elements.last()
        assertTrue(line is CanvasElement.Ink && line.kind == InkKind.ARROW)
    }

    @Test
    fun aTapWithTheImageToolPlacesAPictureThere() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.IMAGE), CanvasIntent.CloseOptions)

        onBoard { click(page(500f, 600f)) }

        val image = vm.state.value.page.elements.last() as CanvasElement.Image
        assertEquals(500f, image.x + image.width / 2, "centred on the tap")
        assertEquals(listOf(image.id), vm.state.value.selectedIds)
    }

    private companion object {
        /** Comfortably over the 20dp below which a drag counts as a tap. */
        const val MIN_DRAWN = 50f
    }
}
