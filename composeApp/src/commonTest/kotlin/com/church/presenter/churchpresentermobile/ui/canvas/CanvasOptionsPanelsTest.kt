package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasFont
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.standalone.COLOR_PICKER_CANCEL
import com.church.presenter.churchpresentermobile.ui.standalone.COLOR_PICKER_SAVE
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The options panels. The first test is the broad one — every control in every
 * panel does *something* to its tool's settings, which is the bug a "does it
 * render" test never sees — and the rest pin down what particular controls do.
 */
@OptIn(ExperimentalTestApi::class)
class CanvasOptionsPanelsTest {

    @Test
    fun everyControlInEveryPanelChangesItsToolsSettings() = runComposeUiTest {
        val vm = showCanvas()
        val defaults = CanvasToolOptions()

        SETTINGS.forEach { (tool, section) ->
            pick(tool)
            pressEveryControl { closeColorPicker() }
            assertNotEquals(section(defaults), section(vm.state.value.options), "$tool's panel changed nothing")
            pick(tool)
        }
    }

    @Test
    fun slidersSetTheirValues() = runComposeUiTest {
        val vm = showCanvas()
        pick(CanvasTool.PEN)

        val sliders = onAllNodes(sliderInPanel)
        val count = sliders.fetchSemanticsNodes().size
        assertEquals(PEN_SLIDERS, count, "width, opacity and smoothing")
        repeat(count) { i -> sliders[i].performSemanticsAction(SemanticsActions.SetProgress) { it(SLIDER_AT) } }

        val pen = vm.state.value.options.pen
        assertEquals(SLIDER_AT, pen.opacity)
        assertEquals(SLIDER_AT, pen.smoothing)
        assertEquals(PEN_WIDTH_AT_SLIDER, pen.width)
    }

    @Test
    fun theCustomSwatchMixesAColourInThePicker() = runComposeUiTest {
        val vm = showCanvas()
        pick(CanvasTool.PEN)
        var mixed: String? = null

        pressEveryControl {
            if (exists(COLOR_PICKER_SAVE)) {
                click(COLOR_PICKER_SAVE)
                mixed = vm.state.value.options.pen.custom
            }
        }

        assertNotNull(mixed, "saving the picker set a mixed colour on the pen")
    }

    @Test
    fun arrangeButtonsWaitForASelectionAndThenActOnIt() = runComposeUiTest {
        val vm = showCanvas()
        pick(CanvasTool.SELECT)
        assertEquals(ARRANGE_BUTTONS, onAllNodes(disabledInPanel).fetchSemanticsNodes().size, "nothing to arrange")

        vm.given(CanvasIntent.SelectMany(listOf(Sample.IMAGE, Sample.SHAPE)))
        waitForIdle()
        assertEquals(1, onAllNodes(disabledInPanel).fetchSemanticsNodes().size, "Distribute needs a third")

        pressEveryControl()

        val elements = vm.state.value.page.elements
        val image = elements.first { it.id == Sample.IMAGE } as CanvasElement.Image
        val shape = elements.first { it.id == Sample.SHAPE } as CanvasElement.Shape
        assertEquals(image.x, shape.x, "Align lined their left edges up")
        assertNotNull(image.group, "Group tied them together")
        assertEquals(image.group, shape.group)
    }

    @Test
    fun clearPageInTheEraserPanelEmptiesThePage() = runComposeUiTest {
        val vm = showCanvas()
        pick(CanvasTool.ERASER)

        pressEveryControl()

        assertTrue(vm.state.value.page.elements.isEmpty())
    }

    @Test
    fun theImagePanelInsertsPicturesAndAdjustsTheSelectedOne() = runComposeUiTest {
        val vm = showCanvas()
        val before = vm.state.value.page.elements.size
        pick(CanvasTool.IMAGE)

        pressEveryControl()

        val images = vm.state.value.page.elements.filterIsInstance<CanvasElement.Image>()
        assertEquals(before + INSERT_SOURCES, vm.state.value.page.elements.size, "one picture per source")
        val adjusted = images.last()
        assertTrue(adjusted.flipped, "Flip")
        assertEquals(QUARTER_TURN, adjusted.rotation, "Rotate")
        assertTrue(adjusted.cutOut, "Remove background")
        assertEquals(adjusted.width, adjusted.height, "Crop squared it")
    }

    @Test
    fun theFontAndSizeListsChangeTheTextTool() = runComposeUiTest {
        val vm = showCanvas()
        pick(CanvasTool.TEXT)
        val lists = onAllNodes(dropdownInPanel)
        assertEquals(2, lists.fetchSemanticsNodes().size, "a font list and a size list")

        lists[0].performSemanticsAction(SemanticsActions.OnClick)
        onAllNodes(hasClickAction() and hasAnyAncestor(isPopup())).onLast()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(CanvasFont.HANDWRITING, vm.state.value.options.text.font)

        onAllNodes(dropdownInPanel)[1].performSemanticsAction(SemanticsActions.OnClick)
        onAllNodes(hasClickAction() and hasAnyAncestor(isPopup())).onLast()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(LARGEST_TEXT, vm.state.value.options.text.size)
    }

    private fun ComposeUiTest.closeColorPicker() {
        if (exists(COLOR_PICKER_CANCEL)) click(COLOR_PICKER_CANCEL)
    }

    private companion object {
        /** Each tool with a panel of its own settings, and where those settings live. */
        val SETTINGS: List<Pair<CanvasTool, (CanvasToolOptions) -> Any>> = listOf(
            CanvasTool.SELECT to { it.select },
            CanvasTool.LASSO to { it.lasso },
            CanvasTool.PEN to { it.pen },
            CanvasTool.HIGHLIGHTER to { it.highlighter },
            CanvasTool.ERASER to { it.eraser },
            CanvasTool.SHAPES to { it.shapes },
            CanvasTool.LINE to { it.line },
            CanvasTool.TEXT to { it.text },
            CanvasTool.STICKY to { it.sticky },
            CanvasTool.IMAGE to { it.image },
        )

        val inPanel = hasAnyAncestor(hasTestTag(CanvasTags.OPTIONS))
        val sliderInPanel = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress) and inPanel
        val disabledInPanel = hasClickAction() and !isEnabled() and inPanel
        val dropdownInPanel = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList) and inPanel

        const val PEN_SLIDERS = 3
        const val SLIDER_AT = 0.5f
        /** Half-way along the pen's 0–14dp width slider. */
        const val PEN_WIDTH_AT_SLIDER = 7f
        const val ARRANGE_BUTTONS = 5
        const val INSERT_SOURCES = 4
        const val QUARTER_TURN = 90
        const val LARGEST_TEXT = 80
    }
}
