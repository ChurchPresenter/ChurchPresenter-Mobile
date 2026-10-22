package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.CANVAS_FILE
import com.church.presenter.churchpresentermobile.library.CanvasRepository
import com.church.presenter.churchpresentermobile.library.FileStore
import com.church.presenter.churchpresentermobile.library.SavedCanvas
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.ink
import com.church.presenter.churchpresentermobile.model.CanvasLogicFixtures.shape
import com.church.presenter.churchpresentermobile.model.CanvasPage
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.CanvasOutput
import com.church.presenter.churchpresentermobile.present.CanvasProjector
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The canvas ViewModel: the undo history and how edits are grouped on it, fresh
 * ids, saving to and opening from the disk, and handing over to the projector.
 * Projecting a captured frame needs an image pipeline, so that half lives in
 * jvmTest (CanvasViewModelProjectionTest).
 */
class CanvasViewModelTest {

    private val doc = CanvasDocument(pages = listOf(CanvasPage(listOf(shape("s")))))
    private val stroke = listOf(CanvasPoint(0f, 0f), CanvasPoint(10f, 10f))

    private fun vm(
        document: CanvasDocument = doc,
        store: CanvasRepository? = null,
        projector: CanvasProjector? = null,
    ) = CanvasViewModel(
        document = document,
        projector = projector,
        frameWork = EmptyCoroutineContext,
        store = store,
        saveWork = EmptyCoroutineContext,
    )

    private val CanvasViewModel.elements get() = state.value.page.elements

    // ── Undo history ─────────────────────────────────────────────────────

    @Test
    fun undoAndRedoStepThroughEdits() = runVmTest {
        val vm = vm()
        try {
            assertFalse(vm.state.value.canUndo)
            vm.dispatch(CanvasIntent.PickTool(CanvasTool.PEN))
            vm.dispatch(CanvasIntent.Stroke(stroke))
            assertEquals(2, vm.elements.size)
            assertTrue(vm.state.value.canUndo)

            vm.dispatch(CanvasIntent.Undo)
            assertEquals(1, vm.elements.size)
            assertTrue(vm.state.value.canRedo)
            vm.dispatch(CanvasIntent.Redo)
            assertEquals(2, vm.elements.size)
            assertFalse(vm.state.value.canRedo)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aNewEditClearsWhatCouldBeRedone() = runVmTest {
        val vm = vm()
        try {
            vm.dispatch(CanvasIntent.PickTool(CanvasTool.PEN))
            vm.dispatch(CanvasIntent.Stroke(stroke))
            vm.dispatch(CanvasIntent.Undo)
            vm.dispatch(CanvasIntent.Stroke(stroke))
            assertFalse(vm.state.value.canRedo)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun lookingAroundIsNotAnEdit() = runVmTest {
        val vm = vm()
        try {
            vm.dispatch(CanvasIntent.SetZoom(200))
            vm.dispatch(CanvasIntent.ShowPanel(CanvasPanel.LAYERS))
            vm.dispatch(CanvasIntent.Select("s"))
            assertFalse(vm.state.value.canUndo, "zoom, panels and selection are not undoable")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun undoKeepsTheSelectionOnlyWhereItStillExists() = runVmTest {
        val vm = vm()
        try {
            vm.dispatch(CanvasIntent.PickTool(CanvasTool.SHAPES))
            vm.dispatch(CanvasIntent.PlaceShape(CanvasBounds(0f, 0f, 40f, 40f)))
            assertEquals(1, vm.state.value.selectedIds.size)
            vm.dispatch(CanvasIntent.Undo)
            assertTrue(vm.state.value.selectedIds.isEmpty(), "the placed shape is gone, so is its selection")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun typingIsOneStepAndUndoTakesThePlacedBoxAway() = runVmTest {
        val vm = vm()
        try {
            vm.dispatch(CanvasIntent.PickTool(CanvasTool.TEXT))
            vm.dispatch(CanvasIntent.PlaceText(CanvasPoint(0f, 0f)))
            val id = vm.state.value.editingId!!
            listOf("G", "Gr", "Gra", "Grace").forEach { vm.dispatch(CanvasIntent.EditText(id, it)) }
            vm.dispatch(CanvasIntent.Undo)
            assertEquals(listOf("s"), vm.elements.map { it.id }, "one undo removes the box and its typing")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun retypingAnExistingBoxIsItsOwnStep() = runVmTest {
        val vm = vm()
        try {
            vm.dispatch(CanvasIntent.PickTool(CanvasTool.STICKY))
            vm.dispatch(CanvasIntent.PlaceSticky(CanvasPoint(0f, 0f)))
            val id = vm.state.value.editingId!!
            vm.dispatch(CanvasIntent.EditText(id, "One"))
            vm.dispatch(CanvasIntent.FinishEditing)
            vm.dispatch(CanvasIntent.StartEditing(id))
            vm.dispatch(CanvasIntent.EditText(id, "One two"))
            vm.dispatch(CanvasIntent.EditText(id, "One two three"))
            vm.dispatch(CanvasIntent.Undo)
            val note = vm.elements.last() as CanvasElement.Sticky
            assertEquals("One", note.text, "undo goes back to before the second session, not to an empty note")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aDragIsOneStepAndTwoDragsAreTwo() = runVmTest {
        val vm = vm()
        try {
            repeat(5) { vm.dispatch(CanvasIntent.Move(listOf("s"), 1f, 0f, session = 1)) }
            repeat(3) { vm.dispatch(CanvasIntent.Move(listOf("s"), 1f, 0f, session = 2)) }
            fun x() = (vm.elements.single() as CanvasElement.Shape).x
            assertEquals(8f, x())
            vm.dispatch(CanvasIntent.Undo)
            assertEquals(5f, x())
            vm.dispatch(CanvasIntent.Undo)
            assertEquals(0f, x())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun resizeAndEraseGroupBySessionToo() = runVmTest {
        val vm = vm(CanvasDocument(pages = listOf(CanvasPage(listOf(shape("s"), ink("a"), ink("b"))))))
        try {
            vm.dispatch(CanvasIntent.Resize("s", CanvasBounds(0f, 0f, 50f, 50f), session = 7))
            vm.dispatch(CanvasIntent.Resize("s", CanvasBounds(0f, 0f, 60f, 60f), session = 7))
            vm.dispatch(CanvasIntent.Erase(listOf("a"), session = 8))
            vm.dispatch(CanvasIntent.Erase(listOf("b"), session = 8))
            vm.dispatch(CanvasIntent.Undo)
            assertEquals(3, vm.elements.size, "one undo brings back both erased strokes")
            vm.dispatch(CanvasIntent.Undo)
            val shape = vm.elements.first() as CanvasElement.Shape
            assertEquals(100f, shape.width, "and one more undoes the whole resize")
            assertFalse(vm.state.value.canUndo)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun singleErasesAreSeparateSteps() = runVmTest {
        val vm = vm(CanvasDocument(pages = listOf(CanvasPage(listOf(ink("a"), ink("b"))))))
        try {
            vm.dispatch(CanvasIntent.Erase(listOf("a")))
            vm.dispatch(CanvasIntent.Erase(listOf("b")))
            vm.dispatch(CanvasIntent.Undo)
            assertEquals(listOf("b"), vm.elements.map { it.id })
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun newIdsNeverCollideWithOnesAlreadyThere() = runVmTest {
        val vm = vm(CanvasDocument(pages = listOf(CanvasPage(listOf(shape("canvas-1"), shape("canvas-2"))))))
        try {
            vm.dispatch(CanvasIntent.PickTool(CanvasTool.SHAPES))
            vm.dispatch(CanvasIntent.PlaceShape(CanvasBounds(0f, 0f, 40f, 40f)))
            vm.dispatch(CanvasIntent.PlaceShape(CanvasBounds(0f, 0f, 40f, 40f)))
            val ids = vm.elements.map { it.id }
            assertEquals(ids.toSet().size, ids.size)
        } finally {
            tearDown(vm)
        }
    }

    // ── Saving ───────────────────────────────────────────────────────────

    @Test
    fun withoutAStoreThereIsNoSaveStatus() = runVmTest {
        val vm = vm()
        try {
            assertEquals(SaveStatus.NONE, vm.state.value.saveStatus)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aBurstOfEditsIsOneWriteAfterThePause() = runVmTest {
        val files = InMemoryFileStorage()
        val vm = vm(store = CanvasRepository(files))
        try {
            runCurrent()
            assertEquals(SaveStatus.SAVED, vm.state.value.saveStatus)
            vm.dispatch(CanvasIntent.PickTool(CanvasTool.PEN))
            repeat(3) { vm.dispatch(CanvasIntent.Stroke(stroke)) }
            advanceTimeBy(100)
            runCurrent()
            assertEquals(SaveStatus.SAVING, vm.state.value.saveStatus)
            assertEquals(0, files.writeCount)
            advanceUntilIdle()
            assertEquals(SaveStatus.SAVED, vm.state.value.saveStatus)
            assertEquals(1, files.writeCount, "three strokes in a burst are one write")
            assertTrue(files.contains(CANVAS_FILE))
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun theCanvasOpensAsItWasLeft() = runVmTest {
        val files = InMemoryFileStorage()
        val first = vm(store = CanvasRepository(files))
        val second: CanvasViewModel
        try {
            // Let autosave start watching before the edits, as it does on the main thread.
            runCurrent()
            first.dispatch(CanvasIntent.Rename("Sermon"))
            first.dispatch(CanvasIntent.AddPage)
            val options = first.state.value.options
            first.dispatch(CanvasIntent.SetOptions(options.copy(pen = options.pen.copy(width = 9f))))
            advanceUntilIdle()
        } finally {
            tearDown(first)
        }
        second = vm(document = CanvasDocument(), store = CanvasRepository(files))
        try {
            val s = second.state.value
            assertEquals("Sermon", s.document.title)
            assertEquals(2, s.document.pages.size)
            assertEquals(1, s.pageIndex)
            assertEquals(9f, s.options.pen.width)
            assertEquals(SaveStatus.SAVED, s.saveStatus)
        } finally {
            tearDown(second)
        }
    }

    @Test
    fun aFailedWriteSaysSo() = runVmTest {
        val vm = vm(store = CanvasRepository(BrokenDisk()))
        try {
            runCurrent()
            vm.dispatch(CanvasIntent.Rename("Sermon"))
            advanceUntilIdle()
            assertEquals(SaveStatus.FAILED, vm.state.value.saveStatus)
        } finally {
            tearDown(vm)
        }
    }

    /** A disk every write to fails — full, or read-only. */
    private class BrokenDisk : FileStore {
        override fun read(name: String): String? = null
        override fun write(name: String, text: String) = error("disk full")
        override fun delete(name: String) = Unit
        override fun list(): List<String> = emptyList()
        override fun sizeBytes(name: String): Long = 0
    }

    // ── Projection ───────────────────────────────────────────────────────

    @Test
    fun withoutAProjectorGoLiveDoesNothing() = runVmTest {
        val vm = vm()
        try {
            vm.dispatch(CanvasIntent.SetLive(true))
            assertFalse(vm.state.value.live)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun endLiveTakesTheCanvasOffTheScreen() = runVmTest {
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry(), publish = {})
        val projector = CanvasProjector(CanvasOutput(MutableStateFlow("http://phone:1")), engine)
        val vm = vm(projector = projector)
        try {
            vm.dispatch(CanvasIntent.SetLive(true))
            assertTrue(vm.state.value.live)
            assertTrue(projector.show(byteArrayOf(1), "#000000", "", takeOver = true))
            assertEquals(SlideKind.CANVAS, engine.currentSlide.value.kind)

            vm.dispatch(CanvasIntent.SetLive(false))
            assertFalse(vm.state.value.live)
            assertEquals(SlideKind.BLANK, engine.currentSlide.value.kind, "End Live clears the screen")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun savedFormKeepsOnlyWhatIsWorthFindingAgain() {
        val saved = SavedCanvas()
        assertEquals(CanvasDocument(), saved.document)
        assertEquals(0, saved.pageIndex)
    }
}
