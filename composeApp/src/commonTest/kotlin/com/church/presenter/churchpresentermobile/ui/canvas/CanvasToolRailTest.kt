package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.viewmodel.CanvasPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The tool rail, the options panel each tool opens, and the pen dock and page strip along the bottom. */
@OptIn(ExperimentalTestApi::class)
class CanvasToolRailTest {

    @Test
    fun pickingAToolSelectsItAndOpensItsOptionsAtOnce() = runComposeUiTest {
        val vm = showCanvas()

        CanvasTool.entries.filter { it != CanvasTool.PEN }.forEach { tool ->
            pick(tool)
            assertEquals(tool, vm.state.value.tool)
            onNodeWithTag(CanvasTags.tool(tool)).assertIsSelected()
            assertTrue(exists(CanvasTags.OPTIONS), "$tool opened its options in one tap")
        }
    }

    @Test
    fun tappingTheActiveToolClosesItsOptionsAndOpensThemAgain() = runComposeUiTest {
        showCanvas()
        pick(CanvasTool.HIGHLIGHTER)
        assertTrue(exists(CanvasTags.OPTIONS))

        pick(CanvasTool.HIGHLIGHTER)
        assertFalse(exists(CanvasTags.OPTIONS))

        pick(CanvasTool.HIGHLIGHTER)
        assertTrue(exists(CanvasTags.OPTIONS))
    }

    @Test
    fun theDockIsOutOnlyForThePenAndTheHighlighter() = runComposeUiTest {
        showCanvas()
        assertTrue(exists(CanvasTags.DOCK), "the pen is the starting tool")

        pick(CanvasTool.HIGHLIGHTER)
        assertTrue(exists(CanvasTags.DOCK))

        pick(CanvasTool.ERASER)
        assertFalse(exists(CanvasTags.DOCK))
    }

    @Test
    fun theImageToolBringsTheInsertPanelAndLeavingItTakesItAway() = runComposeUiTest {
        val vm = showCanvas()

        pick(CanvasTool.IMAGE)
        assertTrue(exists(CanvasTags.INSERT_PANEL))
        assertEquals(CanvasPanel.INSERT_IMAGE, vm.state.value.panel)

        pick(CanvasTool.SELECT)
        assertFalse(exists(CanvasTags.INSERT_PANEL))
    }

    @Test
    fun theDockChangesThePenItIsShowing() = runComposeUiTest {
        val vm = showCanvas()
        val before = vm.state.value.options.pen

        // Every swatch and width in the dock is pressed once; the last of each wins.
        val dockControls = onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag(CanvasTags.DOCK)))
        val count = dockControls.fetchSemanticsNodes().size
        assertTrue(count >= DOCK_CONTROLS, "colours and widths are pressable, found $count")
        repeat(count) { i ->
            dockControls[i].performSemanticsAction(SemanticsActions.OnClick)
        }

        val after = vm.state.value.options.pen
        assertTrue(after.ink != before.ink || after.width != before.width, "the dock changed the pen")
    }

    @Test
    fun thePageStripTurnsPagesAndAddsOne() = runComposeUiTest {
        val vm = showCanvas()

        click(CanvasTags.page(1))
        assertEquals(1, vm.state.value.pageIndex)
        onNodeWithTag(CanvasTags.page(1)).assertIsSelected()

        click(CanvasTags.ADD_PAGE)
        assertEquals(3, vm.state.value.document.pages.size)
        assertEquals(2, vm.state.value.pageIndex)
        assertTrue(exists(CanvasTags.page(2)))
    }

    private companion object {
        /** Seven inks and three widths. */
        const val DOCK_CONTROLS = 10
    }
}
