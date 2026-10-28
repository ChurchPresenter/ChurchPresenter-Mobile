package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.viewmodel.CanvasChrome
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Transform numbers in the Select panel, and tucking the rail, the dock and a panel away. */
@OptIn(ExperimentalTestApi::class)
class CanvasTransformAndMinimizeTest {

    private fun ComposeUiTest.type(tag: String, value: String) {
        onNodeWithTag(tag).performTextReplacement(value)
        onNodeWithTag(tag).performImeAction()
        waitForIdle()
    }

    private fun CanvasViewModel.shape() =
        state.value.page.elements.first { it.id == Sample.SHAPE } as CanvasElement.Shape

    /** The canvas with the sample's shape selected and the Select panel out. */
    private fun ComposeUiTest.withShapeSelected(): CanvasViewModel {
        val vm = showCanvas()
        vm.given(CanvasIntent.Select(Sample.SHAPE))
        pick(CanvasTool.SELECT)
        return vm
    }

    @Test
    fun typingXMovesTheSelectedShape() = runComposeUiTest {
        val vm = withShapeSelected()

        type(CanvasTags.TRANSFORM_X, "600")

        assertEquals(600f, vm.shape().x)
    }

    @Test
    fun typingAWidthResizesIt() = runComposeUiTest {
        val vm = withShapeSelected()

        type(CanvasTags.TRANSFORM_WIDTH, "300")

        assertEquals(300f, vm.shape().width)
    }

    @Test
    fun typingARotationTurnsIt() = runComposeUiTest {
        val vm = withShapeSelected()

        type(CanvasTags.TRANSFORM_ROTATION, "45")

        assertEquals(45f, vm.shape().angle)
    }

    @Test
    fun aShapeLeansFromItsSkewFields() = runComposeUiTest {
        val vm = withShapeSelected()

        type(CanvasTags.TRANSFORM_SKEW_X, "-20")

        assertEquals(-20f, vm.shape().skewX)
    }

    @Test
    fun anythingButOneUnlockedItemHasNoTransform() = runComposeUiTest {
        val vm = withShapeSelected()
        assertTrue(exists(CanvasTags.TRANSFORM_X))

        vm.given(CanvasIntent.SelectMany(listOf(Sample.SHAPE, Sample.IMAGE)))
        waitForIdle()
        assertFalse(exists(CanvasTags.TRANSFORM_X), "two items have no one position")

        vm.given(CanvasIntent.Select(Sample.SHAPE), CanvasIntent.ToggleLock(Sample.SHAPE))
        waitForIdle()
        assertFalse(exists(CanvasTags.TRANSFORM_X), "a locked item stays where it is")
    }

    @Test
    fun theRailTucksAwayKeepingTheToolInHand() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.PEN), CanvasIntent.CloseOptions)

        click(CanvasTags.chrome(CanvasChrome.RAIL, minimized = false))
        assertTrue(CanvasChrome.RAIL in vm.state.value.minimized)
        assertTrue(exists(CanvasTags.MINIMIZED_RAIL))
        assertFalse(exists(CanvasTags.tool(CanvasTool.ERASER)), "only the tool in hand is left")
        assertTrue(exists(CanvasTags.tool(CanvasTool.PEN)))

        click(CanvasTags.chrome(CanvasChrome.RAIL, minimized = true))
        assertFalse(exists(CanvasTags.MINIMIZED_RAIL))
        assertTrue(exists(CanvasTags.tool(CanvasTool.ERASER)))
    }

    @Test
    fun theDockTucksAwayAndComesBack() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.PEN), CanvasIntent.CloseOptions)

        click(CanvasTags.chrome(CanvasChrome.DOCK, minimized = false))
        assertTrue(exists(CanvasTags.MINIMIZED_DOCK))
        assertFalse(exists(CanvasTags.DOCK))

        click(CanvasTags.chrome(CanvasChrome.DOCK, minimized = true))
        assertTrue(exists(CanvasTags.DOCK))
        assertFalse(CanvasChrome.DOCK in vm.state.value.minimized)
    }

    @Test
    fun aMinimizedPanelKeepsOnlyItsTitle() = runComposeUiTest {
        val vm = withShapeSelected()
        assertTrue(exists(CanvasTags.TRANSFORM_X))

        click(CanvasTags.chrome(CanvasChrome.OPTIONS, minimized = false))
        assertTrue(exists(CanvasTags.OPTIONS), "the panel is still there")
        assertFalse(exists(CanvasTags.TRANSFORM_X), "its body is not")

        click(CanvasTags.chrome(CanvasChrome.OPTIONS, minimized = true))
        assertTrue(exists(CanvasTags.TRANSFORM_X))
        assertFalse(CanvasChrome.OPTIONS in vm.state.value.minimized)
    }
}
