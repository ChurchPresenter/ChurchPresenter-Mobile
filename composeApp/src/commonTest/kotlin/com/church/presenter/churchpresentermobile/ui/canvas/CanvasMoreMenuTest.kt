package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The "…" menu: duplicating, deleting and clearing pages, and clearing the whole canvas. */
@OptIn(ExperimentalTestApi::class)
class CanvasMoreMenuTest {

    @Test
    fun duplicatePageAddsACopyAfterTheOneOpenAndTurnsToIt() = runComposeUiTest {
        val vm = showCanvas()

        click(CanvasTags.MORE)
        click(CanvasTags.MENU_DUPLICATE_PAGE)

        val s = vm.state.value
        assertEquals(3, s.document.pages.size)
        assertEquals(1, s.pageIndex)
        assertEquals(s.document.pages[0].elements.size, s.page.elements.size)
    }

    @Test
    fun deletePageIsOfferedOnlyWhileThereIsAnotherPage() = runComposeUiTest {
        val vm = showCanvas()

        click(CanvasTags.MORE)
        onNodeWithTag(CanvasTags.MENU_DELETE_PAGE).assertIsEnabled()
        click(CanvasTags.MENU_DELETE_PAGE)
        assertEquals(1, vm.state.value.document.pages.size)

        click(CanvasTags.MORE)
        onNodeWithTag(CanvasTags.MENU_DELETE_PAGE).assertIsNotEnabled()
    }

    @Test
    fun clearPageEmptiesTheOpenPageOnly() = runComposeUiTest {
        val vm = showCanvas()

        click(CanvasTags.MORE)
        click(CanvasTags.MENU_CLEAR_PAGE)

        assertTrue(vm.state.value.page.elements.isEmpty())
        assertEquals(2, vm.state.value.document.pages.size)
        assertFalse(exists(CanvasTags.element(Sample.TITLE)), "the title is gone from the board")
    }

    @Test
    fun clearCanvasAsksFirstAndThenStartsAgainBlank() = runComposeUiTest {
        val vm = showCanvas()

        click(CanvasTags.MORE)
        click(CanvasTags.MENU_CLEAR_CANVAS)
        assertTrue(exists(CanvasTags.CLEAR_CANVAS_CONFIRM), "it asks before clearing every page")
        assertEquals(sampleCanvas(), vm.state.value.document, "nothing cleared before the answer")

        click(CanvasTags.CLEAR_CANVAS_CONFIRM)

        assertEquals(CanvasDocument(), vm.state.value.document)
        assertNull(vm.state.value.document.title)
        assertFalse(exists(CanvasTags.CLEAR_CANVAS_CONFIRM))
    }

    @Test
    fun aClearedCanvasComesBackWithUndo() = runComposeUiTest {
        val vm = showCanvas()

        click(CanvasTags.MORE)
        click(CanvasTags.MENU_CLEAR_CANVAS)
        click(CanvasTags.CLEAR_CANVAS_CONFIRM)
        click(CanvasTags.UNDO)

        assertEquals(sampleCanvas(), vm.state.value.document)
    }

    @Test
    fun theClearItemsAreOffWhenThereIsNothingToClear() = runComposeUiTest {
        showCanvas(document = CanvasDocument())

        click(CanvasTags.MORE)

        onNodeWithTag(CanvasTags.MENU_CLEAR_PAGE).assertIsNotEnabled()
        onNodeWithTag(CanvasTags.MENU_CLEAR_CANVAS).assertIsNotEnabled()
        onNodeWithTag(CanvasTags.MENU_DUPLICATE_PAGE).assertIsEnabled()
    }
}
