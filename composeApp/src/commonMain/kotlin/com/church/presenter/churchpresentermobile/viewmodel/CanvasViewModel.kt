package com.church.presenter.churchpresentermobile.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.library.CanvasRepository
import com.church.presenter.churchpresentermobile.library.SavedCanvas
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.present.CanvasProjector
import com.church.presenter.churchpresentermobile.present.encodeCanvasFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * The canvas editor's state, held in memory, and — in standalone — its place on
 * this device's outputs.
 *
 * Every edit is [reduce]; this class adds only what a pure function cannot hold:
 * the undo history, a supply of fresh element ids, and projection. Go Live takes
 * the screen with the next frame the board sends ([showFrame]); each frame after
 * that refreshes it while the canvas is still what is showing; End Live clears it.
 * If anything else takes the screen meanwhile — a song from the Present tab — the
 * canvas stops being live by itself, so a later stroke cannot snatch the screen back.
 *
 * @param document What to open. A blank, untitled canvas unless one is given.
 * @param projector Where frames go. Null outside standalone, where Go Live is off.
 * @param frameWork Where a frame is scaled and encoded: never the main thread.
 * @param store Where the canvas is kept between sessions. What it holds is opened
 *   in place of [document]; every change is written back [SAVE_DELAY_MS] after the
 *   last one, so a burst of strokes is one write. Null keeps the canvas in memory.
 * @param saveWork Where the canvas is written: never the main thread.
 */
class CanvasViewModel(
    document: CanvasDocument = CanvasDocument(),
    private val projector: CanvasProjector? = null,
    private val frameWork: CoroutineContext = Dispatchers.Default,
    private val store: CanvasRepository? = null,
    private val saveWork: CoroutineContext = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(opening(document))
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

    /** Whether the next frame should take the screen — set by Go Live until a frame makes it up. */
    private var takeOver = false
    private var frameJob: Job? = null

    init {
        if (store != null) autosave(store)
        projector?.let { p ->
            viewModelScope.launch {
                var wasShowing = false
                p.showing.collect { showing ->
                    val lostScreen = wasShowing && !showing
                    if (lostScreen && _state.value.live && !takeOver) _state.update { it.copy(live = false) }
                    wasShowing = showing
                }
            }
        }
    }

    /**
     * A new picture of the page from the board, framed by [paper]. Projected while
     * live; ignored otherwise. Only the latest counts: a frame still encoding when
     * the next arrives is dropped.
     */
    fun showFrame(frame: ImageBitmap, paper: String) {
        val p = projector ?: return
        if (!_state.value.live) return
        frameJob?.cancel()
        frameJob = viewModelScope.launch {
            val png = withContext(frameWork) { encodeCanvasFrame(frame) } ?: return@launch
            val taking = takeOver
            val shown = p.show(png, paper, _state.value.document.title.orEmpty(), takeOver = taking)
            if (shown) {
                takeOver = false
            } else {
                // Either the server is down (going live failed) or something else
                // has the screen now; either way the canvas is not live.
                takeOver = false
                _state.update { it.copy(live = false) }
            }
        }
    }

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
            is CanvasIntent.SetLive -> {
                if (intent.live && projector == null) return
                _state.value = before.reduce(intent, ::newId)
                if (intent.live) {
                    takeOver = true
                } else {
                    takeOver = false
                    frameJob?.cancel()
                    projector?.stop()
                }
            }
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

    /** The canvas to open: the saved one if there is one, otherwise [document]. */
    private fun opening(document: CanvasDocument): CanvasUiState {
        val saved = store?.load() ?: return CanvasUiState(
            document = document,
            saveStatus = if (store != null) SaveStatus.SAVED else SaveStatus.NONE,
        )
        return CanvasUiState(
            document = saved.document,
            pageIndex = saved.pageIndex,
            options = saved.options,
            saveStatus = SaveStatus.SAVED,
        )
    }

    /**
     * Writes the canvas back after each change — the pages, the open page and the
     * tool settings, but not passing things like the selection or zoom. Waits
     * [SAVE_DELAY_MS] for the changes to stop, and a change arriving meanwhile
     * restarts the wait rather than queueing a second write.
     */
    private fun autosave(store: CanvasRepository) {
        viewModelScope.launch {
            _state
                .map { SavedCanvas(document = it.document, pageIndex = it.pageIndex, options = it.options) }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { canvas ->
                    _state.update { it.copy(saveStatus = SaveStatus.SAVING) }
                    delay(SAVE_DELAY_MS)
                    val saved = withContext(saveWork) { store.save(canvas) }
                    _state.update { it.copy(saveStatus = if (saved) SaveStatus.SAVED else SaveStatus.FAILED) }
                }
        }
    }

    /** A change still waiting out [SAVE_DELAY_MS] is written now, rather than lost with the ViewModel. */
    override fun onCleared() {
        val s = _state.value
        if (store != null && s.saveStatus == SaveStatus.SAVING) {
            store.save(SavedCanvas(document = s.document, pageIndex = s.pageIndex, options = s.options))
        }
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

        /** How long the canvas waits for edits to stop before writing them, in ms. */
        const val SAVE_DELAY_MS = 500L
    }
}
