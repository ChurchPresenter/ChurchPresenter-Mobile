package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.BorderStyle
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasShape
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.HighlightColor
import com.church.presenter.churchpresentermobile.model.ShapeFill
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The color rows: the operator's mixes at the front, tapping one puts it back in
 * use, and tapping a named color drops the mix — in the panels and the dock alike.
 */
@OptIn(ExperimentalTestApi::class)
class CanvasPaletteRowsTest {

    private val mixes = listOf("#0EA5E9", "#9333EA")

    /** Presses the swatch tagged [tag] inside the part of the screen tagged [within]. */
    private fun ComposeUiTest.press(tag: String, within: String) =
        onNode(hasTestTag(tag) and hasAnyAncestor(hasTestTag(within))).performSemanticsAction(SemanticsActions.OnClick)

    @Test
    fun aMixInThePensRowPutsThatColorBackInUse() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.SetOptions(vm.state.value.options.copy(mixedInks = mixes)))
        pick(CanvasTool.PEN)

        press(CanvasTags.mix("#9333EA"), within = CanvasTags.OPTIONS)

        assertEquals("#9333EA", vm.state.value.options.pen.custom)
        assertEquals(mixes, vm.state.value.options.mixedInks, "reusing a mix does not reorder the row")
    }

    @Test
    fun aNamedInkDropsTheMix() = runComposeUiTest {
        val vm = showCanvas()
        val options = vm.state.value.options
        vm.given(CanvasIntent.SetOptions(options.copy(pen = options.pen.copy(custom = "#9333EA"), mixedInks = mixes)))
        pick(CanvasTool.PEN)

        press(CanvasTags.preset(CanvasInk.TEAL.name), within = CanvasTags.OPTIONS)

        assertNull(vm.state.value.options.pen.custom)
        assertEquals(CanvasInk.TEAL, vm.state.value.options.pen.ink)
    }

    @Test
    fun mixesPushTheLastPresetsOffTheRow() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.SetOptions(vm.state.value.options.copy(mixedInks = mixes)))
        pick(CanvasTool.PEN)

        assertTrue(exists(CanvasTags.mix("#0EA5E9")))
        assertTrue(exists(CanvasTags.preset(CanvasInk.VIOLET.name)), "the eighth preset still shows")
        assertFalse(exists(CanvasTags.preset(CanvasInk.PINK.name)), "two mixes pushed the last two presets off")
        assertFalse(exists(CanvasTags.preset(CanvasInk.BROWN.name)))
    }

    @Test
    fun theHighlightersRowWorksTheSameWay() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.SetOptions(vm.state.value.options.copy(mixedHighlights = listOf("#FDE68A"))))
        pick(CanvasTool.HIGHLIGHTER)

        press(CanvasTags.mix("#FDE68A"), within = CanvasTags.OPTIONS)
        assertEquals("#FDE68A", vm.state.value.options.highlighter.custom)

        press(CanvasTags.preset(HighlightColor.LIME.name), within = CanvasTags.OPTIONS)
        assertNull(vm.state.value.options.highlighter.custom)
        assertEquals(HighlightColor.LIME, vm.state.value.options.highlighter.color)
    }

    @Test
    fun theDockOffersTheSameMixes() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(
            CanvasIntent.PickTool(CanvasTool.PEN),
            CanvasIntent.CloseOptions,
            CanvasIntent.SetOptions(vm.state.value.options.copy(mixedInks = mixes)),
        )

        press(CanvasTags.mix("#0EA5E9"), within = CanvasTags.DOCK)
        assertEquals("#0EA5E9", vm.state.value.options.pen.custom)

        press(CanvasTags.preset(CanvasInk.CONTRAST.name), within = CanvasTags.DOCK)
        assertNull(vm.state.value.options.pen.custom)
        assertEquals(CanvasInk.CONTRAST, vm.state.value.options.pen.ink)
    }

    @Test
    fun theHighlightersDockRowTakesMixesToo() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(
            CanvasIntent.PickTool(CanvasTool.HIGHLIGHTER),
            CanvasIntent.CloseOptions,
            CanvasIntent.SetOptions(vm.state.value.options.copy(mixedHighlights = listOf("#FDE68A"))),
        )

        press(CanvasTags.mix("#FDE68A"), within = CanvasTags.DOCK)
        assertEquals("#FDE68A", vm.state.value.options.highlighter.custom)

        press(CanvasTags.preset(HighlightColor.TEAL.name), within = CanvasTags.DOCK)
        assertEquals(HighlightColor.TEAL, vm.state.value.options.highlighter.color)
    }

    @Test
    fun everyShapeDrawsLeaningWithEachKindOfBorder() = runComposeUiTest {
        // Placed through the shape tool so each lands with its own fill, border and lean.
        val vm = showCanvas()
        val borders = BorderStyle.entries
        CanvasShape.entries.forEachIndexed { i, shape ->
            val options = vm.state.value.options
            val shapes = options.shapes.copy(
                shape = shape,
                border = borders[i % borders.size],
                fill = if (i % 2 == 0) ShapeFill.SKY else ShapeFill.NONE,
                fillCustom = if (i % 3 == 0) "#F97316" else null,
                skewX = if (i % 2 == 0) -20f else 0f,
                skewY = if (i % 2 == 0) 0f else 10f,
            )
            vm.given(
                CanvasIntent.PickTool(CanvasTool.SHAPES),
                CanvasIntent.Select(null),
                CanvasIntent.SetOptions(options.copy(shapes = shapes)),
                CanvasIntent.PlaceShape(CanvasBounds(40f + i * 30f, 600f, 140f + i * 30f, 680f)),
            )
        }
        waitForIdle()

        val placed = vm.state.value.page.elements.filterIsInstance<CanvasElement.Shape>().filter { it.y == 600f }
        assertEquals(CanvasShape.entries.toSet(), placed.map { it.shape }.toSet())
        placed.forEach { assertTrue(exists(CanvasTags.element(it.id)), "${it.shape} is on the board") }
        assertTrue(placed.any { it.skewX != 0f } && placed.any { it.skewY != 0f })
    }
}
