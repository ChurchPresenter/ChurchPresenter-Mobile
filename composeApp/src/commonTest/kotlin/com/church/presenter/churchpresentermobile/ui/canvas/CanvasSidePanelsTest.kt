package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.ImageSource
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasPanel
import com.church.presenter.churchpresentermobile.viewmodel.SelectionAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Layers and Insert panels down the right, and the bar under a selection. */
@OptIn(ExperimentalTestApi::class)
class CanvasSidePanelsTest {

    @Test
    fun layersListsEveryElementAndSelectsTheOneTapped() = runComposeUiTest {
        val vm = showCanvas()
        click(CanvasTags.LAYERS)

        sampleCanvas().pages[0].elements.forEach { assertTrue(exists(CanvasTags.layer(it.id)), "${it.id} is listed") }
        click(CanvasTags.layer(Sample.NOTE))

        assertEquals(listOf(Sample.NOTE), vm.state.value.selectedIds)
        onNodeWithTag(CanvasTags.layer(Sample.NOTE)).assertIsSelected()
    }

    @Test
    fun theEyeHidesAnElementFromTheBoardAndShowsItAgain() = runComposeUiTest {
        val vm = showCanvas()
        click(CanvasTags.LAYERS)

        click(CanvasTags.layerVisibility(Sample.IMAGE))
        assertTrue(vm.state.value.page.elements.first { it.id == Sample.IMAGE }.hidden)
        assertFalse(exists(CanvasTags.element(Sample.IMAGE)), "hidden from the board")

        click(CanvasTags.layerVisibility(Sample.IMAGE))
        assertTrue(exists(CanvasTags.element(Sample.IMAGE)))
    }

    @Test
    fun aLockedElementShowsALockThatUnlocksIt() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.ToggleLock(Sample.SHAPE))
        click(CanvasTags.LAYERS)
        assertTrue(exists(CanvasTags.layerLock(Sample.SHAPE)))
        assertFalse(exists(CanvasTags.layerLock(Sample.NOTE)), "only locked rows wear a lock")

        click(CanvasTags.layerLock(Sample.SHAPE))

        assertFalse(vm.state.value.page.elements.first { it.id == Sample.SHAPE }.locked)
    }

    @Test
    fun doneClosesTheLayersPanel() = runComposeUiTest {
        val vm = showCanvas()
        click(CanvasTags.LAYERS)

        click(CanvasTags.LAYERS_DONE)

        assertEquals(CanvasPanel.NONE, vm.state.value.panel)
        assertFalse(exists(CanvasTags.LAYERS_PANEL))
    }

    @Test
    fun eachInsertRowBringsAPictureInFromItsSource() = runComposeUiTest {
        val vm = showCanvas()
        pick(CanvasTool.IMAGE)

        ImageSource.entries.forEach { click(CanvasTags.insertSource(it)) }

        val inserted = vm.state.value.page.elements.filterIsInstance<CanvasElement.Image>().mapNotNull { it.source }
        assertEquals(ImageSource.entries, inserted)
    }

    @Test
    fun replacePutsTheNextPictureInTheSelectedOnesPlace() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.Select(Sample.IMAGE))
        waitForIdle()

        click(CanvasTags.selection(SelectionAction.REPLACE))
        assertTrue(exists(CanvasTags.INSERT_PANEL), "Replace opens the panel")
        click(CanvasTags.insertSource(ImageSource.CAMERA))

        val images = vm.state.value.page.elements.filterIsInstance<CanvasElement.Image>()
        assertEquals(1, images.size, "replaced, not added")
        assertEquals(ImageSource.CAMERA, images.single().source)
        assertFalse(exists(CanvasTags.INSERT_PANEL))
    }

    @Test
    fun theSelectionBarActsOnTheSelectedPicture() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.Select(Sample.IMAGE))
        waitForIdle()
        assertTrue(exists(CanvasTags.SELECTION_BAR))

        click(CanvasTags.selection(SelectionAction.CROP))
        click(CanvasTags.selection(SelectionAction.BRING_TO_FRONT))
        val image = vm.state.value.page.elements.last()
        assertEquals(Sample.IMAGE, image.id, "brought to the top")
        image as CanvasElement.Image
        assertEquals(image.width, image.height, "cropped square")

        click(CanvasTags.selection(SelectionAction.DUPLICATE))
        assertEquals(2, vm.state.value.page.elements.count { it is CanvasElement.Image })

        click(CanvasTags.selection(SelectionAction.LOCK))
        assertTrue(vm.state.value.selection.all { it.locked })
        click(CanvasTags.selection(SelectionAction.LOCK))
        click(CanvasTags.selection(SelectionAction.DELETE))
        assertEquals(1, vm.state.value.page.elements.count { it is CanvasElement.Image }, "the copy is deleted")
        assertFalse(exists(CanvasTags.SELECTION_BAR))
    }

    @Test
    fun cropAndReplaceAreOfferedOnlyForASinglePicture() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.Select(Sample.NOTE))
        waitForIdle()

        assertTrue(exists(CanvasTags.selection(SelectionAction.DELETE)))
        assertFalse(exists(CanvasTags.selection(SelectionAction.CROP)))
        assertFalse(exists(CanvasTags.selection(SelectionAction.REPLACE)))
    }
}
