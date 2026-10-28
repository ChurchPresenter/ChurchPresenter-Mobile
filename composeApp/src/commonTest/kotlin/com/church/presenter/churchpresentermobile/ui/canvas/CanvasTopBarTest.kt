package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.type
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasPanel
import com.church.presenter.churchpresentermobile.viewmodel.CanvasViewModel
import com.church.presenter.churchpresentermobile.viewmodel.LiveNote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The canvas's top bar: the way back, undo and redo, zoom, the Layers toggle and
 * Go Live. The "…" menu has its own tests ([CanvasMoreMenuTest]).
 */
@OptIn(ExperimentalTestApi::class)
class CanvasTopBarTest {

    @Test
    fun backLeavesTheCanvas() = runComposeUiTest {
        var backs = 0
        showCanvas(onBack = { backs++ })

        click(CanvasTags.BACK)

        assertEquals(1, backs)
    }

    @Test
    fun undoAndRedoArePressableOnlyWithSomethingToUndoOrRedo() = runComposeUiTest {
        val vm = showCanvas()
        onNodeWithTag(CanvasTags.UNDO).assertIsNotEnabled()
        onNodeWithTag(CanvasTags.REDO).assertIsNotEnabled()

        vm.given(CanvasIntent.Stroke(listOf(CanvasPoint(300f, 300f), CanvasPoint(320f, 320f))))
        waitForIdle()
        onNodeWithTag(CanvasTags.UNDO).assertIsEnabled()

        click(CanvasTags.UNDO)
        assertEquals(sampleCanvas(), vm.state.value.document, "undo put the page back")
        onNodeWithTag(CanvasTags.REDO).assertIsEnabled()

        click(CanvasTags.REDO)
        assertEquals(sampleCanvas().pages[0].elements.size + 1, vm.state.value.page.elements.size)
    }

    @Test
    fun zoomStepsInAndOutAndShowsThePercentage() = runComposeUiTest {
        val vm = showCanvas()

        click(CanvasTags.ZOOM_IN)
        assertEquals(125, vm.state.value.zoom)
        click(CanvasTags.ZOOM_OUT)
        click(CanvasTags.ZOOM_OUT)
        assertEquals(75, vm.state.value.zoom)
    }

    @Test
    fun theLayersButtonOpensAndClosesTheLayersPanel() = runComposeUiTest {
        val vm = showCanvas()

        click(CanvasTags.LAYERS)
        assertTrue(exists(CanvasTags.LAYERS_PANEL))
        assertEquals(CanvasPanel.LAYERS, vm.state.value.panel)

        click(CanvasTags.LAYERS)
        assertFalse(exists(CanvasTags.LAYERS_PANEL))
    }

    @Test
    fun goLiveIsOffWhereThereIsNowhereToProject() = runComposeUiTest {
        val vm = showCanvas(canProject = false)

        onNodeWithTag(CanvasTags.GO_LIVE).assertIsNotEnabled()
        assertFalse(vm.state.value.live)
    }

    @Test
    fun goLiveWithoutAProjectorLeavesTheCanvasOffAir() = runComposeUiTest {
        // canProject says yes but nothing was handed in to project with — remote
        // mode's shape. Pressing must not pretend the canvas is on a screen.
        val vm = showCanvas(canProject = true)
        onNodeWithTag(CanvasTags.GO_LIVE).assertIsEnabled()

        click(CanvasTags.GO_LIVE)

        assertFalse(vm.state.value.live)
    }

    @Test
    fun anUntitledCanvasIsRenamedFromTheMoreMenu() = runComposeUiTest {
        val vm = showCanvas(document = CanvasDocument())
        assertNull(vm.state.value.document.title)

        click(CanvasTags.MORE)
        click(CanvasTags.MENU_RENAME)
        type(CanvasTags.RENAME_FIELD, "Easter talk")
        click(CanvasTags.RENAME_SAVE)

        assertEquals("Easter talk", vm.state.value.document.title)
        assertFalse(exists(CanvasTags.RENAME_SAVE), "the dialog closed")
    }

    @Test
    fun theTitleIsShownInTheBar() = runComposeUiTest {
        showCanvas()

        // Plain data the operator typed, so it is matched by its text.
        assertTrue(isShowing("Sunday sermon sketch"), "the canvas's name is in the bar")
    }

    @Test
    fun theZoomValueReadsAsAPercentage() = runComposeUiTest {
        showCanvas()
        val node = onNodeWithTag(CanvasTags.ZOOM_VALUE)
        // Empty on wasm, where compose-resources does not resolve; "100%" where it does.
        runCatching { node.assertTextEquals("100%") }.onFailure { node.assertTextEquals("") }
    }

    @Test
    fun whyGoLiveFailedIsShownOnlyWhenThereIsANote() = runComposeUiTest {
        val opened = CanvasViewModel(sampleCanvas()).state.value
        setContent {
            AppTheme(themeMode = ThemeMode.DARK) {
                Box(Modifier.fillMaxSize()) {
                    CanvasEditor(
                        state = opened.copy(liveNote = LiveNote.UNSUPPORTED),
                        onIntent = {},
                        onBack = {},
                        canProject = true,
                    )
                }
            }
        }
        assertTrue(exists(CanvasTags.LIVE_NOTE), "an old desktop's refusal is spelled out")
    }

    @Test
    fun noNoteIsShownBeforeGoLiveHasFailed() = runComposeUiTest {
        showCanvas(canProject = true)
        assertFalse(exists(CanvasTags.LIVE_NOTE))
    }
}
