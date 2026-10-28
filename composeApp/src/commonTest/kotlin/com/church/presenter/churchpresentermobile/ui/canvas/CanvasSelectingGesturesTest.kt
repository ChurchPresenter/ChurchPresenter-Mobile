package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.EraserMode
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** What a finger on the board picks up, moves or takes away: the select tool, the lasso and the eraser. */
@OptIn(ExperimentalTestApi::class)
class CanvasSelectingGesturesTest {

    @Test
    fun theEraserTakesAwayTheStrokeItCrossesAndNothingElse() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.ERASER), CanvasIntent.CloseOptions)

        onBoard { dragOnPage(630f, 170f, 630f, 230f) }

        val ids = vm.state.value.page.elements.map { it.id }
        assertTrue(Sample.INK !in ids, "the red stroke is erased")
        assertTrue(Sample.HIGHLIGHT in ids, "the highlight below it is not")
    }

    @Test
    fun theAreaEraserTakesAwayWhatItsBoxTouches() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.ERASER), CanvasIntent.CloseOptions)
        val options = vm.state.value.options
        vm.given(CanvasIntent.SetOptions(options.copy(eraser = options.eraser.copy(mode = EraserMode.AREA))))

        onBoard { dragOnPage(540f, 180f, 720f, 280f) }

        val ids = vm.state.value.page.elements.map { it.id }
        assertTrue(Sample.INK !in ids && Sample.HIGHLIGHT !in ids)
        assertTrue(Sample.SHAPE in ids, "the shape outside the box stays")
    }

    @Test
    fun aLassoRoundAnElementSelectsIt() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.LASSO), CanvasIntent.CloseOptions)

        onBoard {
            down(page(540f, 310f))
            moveTo(page(800f, 310f))
            moveTo(page(800f, 490f))
            moveTo(page(540f, 490f))
            moveTo(page(540f, 312f))
            up()
        }

        assertEquals(listOf(Sample.SHAPE), vm.state.value.selectedIds)
    }

    @Test
    fun aTapSelectsWhatIsUnderIt() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.CloseOptions)

        onBoard { click(page(250f, 250f)) }

        assertEquals(listOf(Sample.IMAGE), vm.state.value.selectedIds)
    }

    @Test
    fun aLongPressAddsToTheSelection() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(
            CanvasIntent.PickTool(CanvasTool.SELECT),
            CanvasIntent.CloseOptions,
            CanvasIntent.Select(Sample.IMAGE),
        )

        // The ellipse's lower right, clear of the picture's toolbar: that runs from the
        // picture's left edge under it, and on Linux its labels lay out wide enough to
        // reach x 684 — a long press at (670, 400) landed on its Delete, not the shape.
        onBoard { longClick(page(740f, 445f)) }

        assertEquals(setOf(Sample.IMAGE, Sample.SHAPE), vm.state.value.selectedIds.toSet())
    }

    @Test
    fun aTapOnEmptyBoardClearsTheSelection() = runComposeUiTest {
        // Between the picture and the ink, clear of every element and of the dock.
        val vm = showCanvas()
        vm.given(
            CanvasIntent.PickTool(CanvasTool.SELECT),
            CanvasIntent.CloseOptions,
            CanvasIntent.Select(Sample.IMAGE),
        )

        onBoard { click(page(500f, 300f)) }

        assertTrue(vm.state.value.selectedIds.isEmpty(), "a tap on empty board clears the selection")
    }

    @Test
    fun draggingAnElementMovesIt() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.CloseOptions)
        // Grid only: a smart guide could pull the note onto a neighbour's edge instead.
        val options = vm.state.value.options
        vm.given(CanvasIntent.SetOptions(options.copy(select = options.select.copy(smartGuides = false))))

        onBoard { dragOnPage(250f, 490f, 340f, 520f) }

        val note = vm.state.value.page.elements.first { it.id == Sample.NOTE } as CanvasElement.Sticky
        assertNotEquals(150f, note.x, "the note moved across")
        assertEquals(0f, note.x % GRID, "snapped to the grid")
        assertEquals(listOf(Sample.NOTE), vm.state.value.selectedIds)
    }

    @Test
    fun aBoxDraggedOnEmptyBoardSelectsWhatItEncloses() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.CloseOptions)

        onBoard { dragOnPage(120f, 130f, 470f, 370f) }

        assertEquals(listOf(Sample.IMAGE), vm.state.value.selectedIds)
    }

    @Test
    fun aDoubleTapOnANoteOpensItForTyping() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.CloseOptions)

        onBoard { doubleClick(page(250f, 490f)) }

        assertEquals(Sample.NOTE, vm.state.value.editingId)
    }

    @Test
    fun aTapOnTheSelectedNoteStartsTyping() = runComposeUiTest {
        // However slow the second tap: a double tap inside 350 ms was the only way in.
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.CloseOptions)

        onBoard { click(page(250f, 490f)) }
        assertEquals(listOf(Sample.NOTE), vm.state.value.selectedIds)
        assertEquals(null, vm.state.value.editingId, "the first tap only selects")
        mainClock.advanceTimeBy(SLOW_SECOND_TAP_MS)
        onBoard { click(page(250f, 490f)) }

        assertEquals(Sample.NOTE, vm.state.value.editingId)
    }

    @Test
    fun aTapInsideAStrokesBoxButOffItsLineSelectsWhatIsUnder() = runComposeUiTest {
        // A loop drawn round the picture: its box covers the picture, its line does not.
        // Smoothing is off so five corner points stay a rectangle rather than being
        // averaged into a line across the middle.
        val vm = showCanvas()
        val options = vm.state.value.options
        vm.given(
            CanvasIntent.PickTool(CanvasTool.PEN),
            CanvasIntent.SetOptions(options.copy(pen = options.pen.copy(smoothing = 0f))),
            CanvasIntent.Stroke(
                listOf(
                    CanvasPoint(130f, 130f), CanvasPoint(470f, 130f), CanvasPoint(470f, 370f),
                    CanvasPoint(130f, 370f), CanvasPoint(130f, 130f),
                ),
            ),
            CanvasIntent.PickTool(CanvasTool.SELECT),
            CanvasIntent.CloseOptions,
        )

        onBoard { click(page(300f, 250f)) }

        assertEquals(listOf(Sample.IMAGE), vm.state.value.selectedIds)
    }

    @Test
    fun aTapOnTheLoopsLineStillSelectsTheLoop() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(
            CanvasIntent.PickTool(CanvasTool.PEN),
            CanvasIntent.Stroke(listOf(CanvasPoint(130f, 130f), CanvasPoint(470f, 130f))),
            CanvasIntent.PickTool(CanvasTool.SELECT),
            CanvasIntent.CloseOptions,
        )
        val loop = vm.state.value.page.elements.last().id

        onBoard { click(page(300f, 132f)) }

        assertEquals(listOf(loop), vm.state.value.selectedIds)
    }

    @Test
    fun anEraserMarkCannotBeSelected() = runComposeUiTest {
        // It is invisible, and deleting it brought back the ink it had erased.
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.ERASER), CanvasIntent.CloseOptions)
        val options = vm.state.value.options
        vm.given(CanvasIntent.SetOptions(options.copy(eraser = options.eraser.copy(mode = EraserMode.PIXEL))))
        onBoard { dragOnPage(200f, 250f, 400f, 250f) }
        val mark = vm.state.value.page.elements.last()
        assertTrue(mark is CanvasElement.Ink && mark.kind == InkKind.ERASER, "the eraser left its mark")
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.CloseOptions)

        onBoard { click(page(300f, 250f)) }

        assertEquals(listOf(Sample.IMAGE), vm.state.value.selectedIds)
    }

    @Test
    fun draggingAHandleResizesTheSelectedPicture() = runComposeUiTest {
        val vm = showCanvas()
        vm.given(CanvasIntent.PickTool(CanvasTool.SELECT), CanvasIntent.CloseOptions, CanvasIntent.Select(Sample.IMAGE))
        waitForIdle()

        // The bottom-right handle of the 150,150 – 450,350 picture.
        onBoard { dragOnPage(450f, 350f, 500f, 400f) }

        val image = vm.state.value.page.elements.first { it.id == Sample.IMAGE } as CanvasElement.Image
        assertTrue(
            image.width > PICTURE_WIDTH && image.height > PICTURE_HEIGHT,
            "grew to ${image.width}×${image.height}",
        )
        assertEquals(150f, image.x, "the opposite corner stays put")
    }

    private companion object {
        const val GRID = 28f
        const val PICTURE_WIDTH = 300f
        const val PICTURE_HEIGHT = 200f
    }
}

/** Well past a double tap, so the second tap is a tap of its own. */
private const val SLOW_SECOND_TAP_MS = 1_000L
