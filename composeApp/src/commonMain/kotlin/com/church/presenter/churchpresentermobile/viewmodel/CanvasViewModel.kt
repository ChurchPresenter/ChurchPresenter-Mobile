package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The canvas editor's state, held in memory.
 *
 * There is no backend yet: nothing is saved, and Go Live is the caller's to wire.
 * What *is* real is the editing — strokes drawn with the pen and highlighter, pages,
 * selection, layer flags and undo — so the screen behaves like the tool it will be.
 *
 * Every edit is [reduce]; this class adds only what a pure function cannot hold:
 * the undo history and a supply of fresh element ids.
 *
 * @param document What to open. A blank, untitled canvas unless one is given.
 */
class CanvasViewModel(document: CanvasDocument = CanvasDocument()) : ViewModel() {

    private val _state = MutableStateFlow(CanvasUiState(document = document))
    val state: StateFlow<CanvasUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<CanvasDocument>()
    private val redoStack = ArrayDeque<CanvasDocument>()
    private var nextId = 1

    /**
     * The run of intents the last change belonged to — a typing session, or one
     * drag of an eraser, a move or a resize. A run is one step on the undo history:
     * only its first change is recorded.
     */
    private var lastRun: String? = null

    /**
     * Applies [intent]. Any change to the document goes on the undo history; a
     * change of tool, zoom or panel does not, as in every drawing app — Undo
     * takes back what was drawn, not where the operator looked.
     */
    fun dispatch(intent: CanvasIntent) {
        val before = _state.value
        when (intent) {
            CanvasIntent.Undo -> step(from = undoStack, to = redoStack)
            CanvasIntent.Redo -> step(from = redoStack, to = undoStack)
            else -> {
                val after = before.reduce(intent, ::newId)
                val run = runOf(intent)
                if (after.document != before.document && (run == null || run != lastRun)) {
                    undoStack.addLast(before.document)
                    redoStack.clear()
                }
                lastRun = when (intent) {
                    // Placing a box already recorded the page without it: undoing the
                    // typing should take the whole box away, not leave an empty one.
                    is CanvasIntent.PlaceText, is CanvasIntent.PlaceSticky -> TYPING
                    else -> run
                }
                _state.value = after.withHistory()
            }
        }
    }

    /** Moves one document from [from] to the screen, and the one it replaces onto [to]. */
    private fun step(from: ArrayDeque<CanvasDocument>, to: ArrayDeque<CanvasDocument>) {
        val document = from.removeLastOrNull() ?: return
        val s = _state.value
        to.addLast(s.document)
        // Keep the page and selection where they still exist in the restored document.
        val pageIndex = s.pageIndex.coerceAtMost(document.pages.lastIndex)
        val ids = document.pages[pageIndex].elements.map { it.id }.toSet()
        _state.value = s.copy(
            document = document,
            pageIndex = pageIndex,
            selectedIds = s.selectedIds.filter { it in ids },
            editingId = null,
        ).withHistory()
        lastRun = null
    }

    /** The run [intent] continues, or null for one that stands alone. */
    private fun runOf(intent: CanvasIntent): String? = when (intent) {
        is CanvasIntent.EditText -> TYPING
        is CanvasIntent.Erase -> intent.session.takeIf { it != CanvasIntent.NO_SESSION }?.let { "erase:$it" }
        is CanvasIntent.Move -> "move:${intent.session}"
        is CanvasIntent.Resize -> "resize:${intent.session}"
        else -> null
    }

    private fun CanvasUiState.withHistory() = copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())

    /** An id no element in the document already has. */
    private fun newId(): String {
        val taken = _state.value.document.pages.flatMap { page -> page.elements.map { it.id } }.toSet()
        var id: String
        do {
            id = "canvas-${nextId++}"
        } while (id in taken)
        return id
    }

    private companion object {
        const val TYPING = "typing"
    }
}
