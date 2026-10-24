package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.editPage
import com.church.presenter.churchpresentermobile.model.lineElement
import com.church.presenter.churchpresentermobile.model.movedInStack
import com.church.presenter.churchpresentermobile.model.normalizedAngle
import com.church.presenter.churchpresentermobile.model.resized
import com.church.presenter.churchpresentermobile.model.shapeElement
import com.church.presenter.churchpresentermobile.model.stickyElement
import com.church.presenter.churchpresentermobile.model.textElement
import com.church.presenter.churchpresentermobile.model.updated
import com.church.presenter.churchpresentermobile.model.withGroupmates
import com.church.presenter.churchpresentermobile.model.withNewPage
import com.church.presenter.churchpresentermobile.model.withPageDuplicated
import com.church.presenter.churchpresentermobile.model.without
import com.church.presenter.churchpresentermobile.model.withoutPage

/**
 * The canvas after [intent], as a plain function of the canvas before it.
 *
 * Undo and Redo are not handled here — they need the history, which is the
 * ViewModel's — and come back unchanged. [newId] names any element an intent
 * creates; the ViewModel hands out ids no element already has.
 */
internal fun CanvasUiState.reduce(intent: CanvasIntent, newId: () -> String): CanvasUiState = when (intent) {
    is CanvasIntent.PickTool -> finishEditing().pickTool(intent.tool)
    CanvasIntent.CloseOptions -> copy(optionsOpen = false)
    is CanvasIntent.SetOptions -> withOptions(intent.options)
    is CanvasIntent.SetZoom -> copy(zoom = intent.percent)
    is CanvasIntent.ShowPanel -> copy(panel = intent.panel, replaceTarget = null)
    is CanvasIntent.Rename -> copy(document = document.copy(title = intent.title.trim().ifEmpty { null }))
    is CanvasIntent.TurnToPage -> if (intent.index in document.pages.indices) {
        finishEditing().copy(pageIndex = intent.index, selectedIds = emptyList())
    } else {
        this
    }
    CanvasIntent.AddPage ->
        finishEditing().let { it.copy(document = it.document.withNewPage(), pageIndex = it.document.pages.size) }
            .copy(selectedIds = emptyList())
    CanvasIntent.DuplicatePage -> finishEditing().let {
        it.copy(document = it.document.withPageDuplicated(pageIndex, newId), pageIndex = pageIndex + 1)
    }.copy(selectedIds = emptyList())
    CanvasIntent.DeletePage -> document.withoutPage(pageIndex).let {
        copy(document = it, pageIndex = pageIndex.coerceAtMost(it.pages.lastIndex), selectedIds = emptyList())
    }.copy(editingId = null)
    CanvasIntent.ClearPage -> editPage { emptyList() }.copy(selectedIds = emptyList(), editingId = null)
    CanvasIntent.ClearCanvas -> copy(
        document = CanvasDocument(),
        pageIndex = 0,
        selectedIds = emptyList(),
        editingId = null,
        replaceTarget = null,
    )
    is CanvasIntent.Select ->
        copy(selectedIds = intent.id?.let { page.elements.withGroupmates(listOf(it)) }.orEmpty()).withOptionsFrom()
    is CanvasIntent.ToggleSelect -> toggleSelect(intent.id).withOptionsFrom()
    is CanvasIntent.SelectMany -> copy(selectedIds = page.elements.withGroupmates(intent.ids)).withOptionsFrom()
    is CanvasIntent.Stroke -> stroke(intent.points, intent.pressure, newId)
    is CanvasIntent.Move -> {
        val movable = page.elements.filter { it.id in intent.ids && !it.locked }.map { it.id }
        editPage { it.updated(movable) { e -> e.moved(intent.dx, intent.dy) } }
    }
    is CanvasIntent.Rotate -> editPage { elements ->
        elements.updated(listOf(intent.id)) { if (it.locked) it else it.withAngle(normalizedAngle(intent.angle)) }
    }
    is CanvasIntent.Resize -> editPage { elements ->
        elements.updated(listOf(intent.id)) { if (it.locked) it else resized(it, intent.bounds) }
    }
    is CanvasIntent.Erase -> {
        val erasable = page.elements.filter { it.id in intent.ids && !it.locked }.map { it.id }
        editPage { it.without(erasable) }.copy(selectedIds = selectedIds - erasable.toSet())
    }
    is CanvasIntent.PlaceShape -> place(shapeElement(newId(), intent.bounds, options.shapes))
    is CanvasIntent.PlaceLine -> place(lineElement(newId(), intent.start, intent.end, options.line))
    is CanvasIntent.PlaceText -> place(textElement(newId(), intent.at, options.text), edit = true)
    is CanvasIntent.PlaceSticky -> place(stickyElement(newId(), intent.at, options.sticky), edit = true)
    is CanvasIntent.InsertImage -> insertImage(intent, newId)
    // Tapping into the box already being typed in is not a new start: finishing it
    // first would throw away a box that is still empty.
    is CanvasIntent.StartEditing -> if (intent.id == editingId) {
        this
    } else {
        finishEditing().copy(editingId = intent.id, selectedIds = listOf(intent.id)).withOptionsFrom()
    }
    is CanvasIntent.EditText -> editText(intent.id, intent.text)
    CanvasIntent.FinishEditing -> finishEditing()
    is CanvasIntent.OnSelection -> onSelection(intent.action, newId)
    is CanvasIntent.Arrange -> arrange(intent.action, intent.bounds, newId)
    is CanvasIntent.AdjustImage -> adjustImages(intent.adjust)
    is CanvasIntent.ToggleLock -> editPage { it.updated(listOf(intent.id)) { e -> e.withFlags(locked = !e.locked) } }
    is CanvasIntent.ToggleHidden -> editPage { it.updated(listOf(intent.id)) { e -> e.withFlags(hidden = !e.hidden) } }
    is CanvasIntent.MoveLayer -> editPage { it.movedInStack(intent.id, intent.steps) }
    is CanvasIntent.SetLive -> if (intent.live) {
        copy(live = true, liveNote = null)
    } else {
        copy(live = false, awaitingApproval = false)
    }
    CanvasIntent.Undo, CanvasIntent.Redo -> this
}

/**
 * Picking a tool opens its options at once, so moving from one tool to the next
 * is one tap; picking the tool already active closes them again (or reopens them).
 * The selection stays, except for the tools that draw over the page. The image
 * tool brings its Insert panel with it, and leaving it takes the panel away.
 */
private fun CanvasUiState.pickTool(tool: CanvasTool): CanvasUiState =
    if (tool == this.tool) {
        copy(optionsOpen = !optionsOpen)
    } else {
        copy(
            tool = tool,
            optionsOpen = true,
            // Only the drawing tools drop the selection. The rest keep it, so their options
            // panel restyles what is selected — a text box's font, a note's colour.
            selectedIds = if (tool in DRAWING_TOOLS) emptyList() else selectedIds,
            replaceTarget = null,
            panel = when {
                tool == CanvasTool.IMAGE -> CanvasPanel.INSERT_IMAGE
                panel == CanvasPanel.INSERT_IMAGE -> CanvasPanel.NONE
                else -> panel
            },
        )
    }

/** The tools that draw over the page rather than act on what is on it. */
private val DRAWING_TOOLS = setOf(CanvasTool.PEN, CanvasTool.HIGHLIGHTER, CanvasTool.ERASER)

/** [id] and its group added to the selection, or taken out if already in it. */
private fun CanvasUiState.toggleSelect(id: String): CanvasUiState {
    val members = page.elements.withGroupmates(listOf(id))
    return copy(selectedIds = if (id in selectedIds) selectedIds - members.toSet() else selectedIds + members)
}

/** [element] put on top of the page and selected — and, for a text box or note, opened for typing. */
private fun CanvasUiState.place(element: CanvasElement, edit: Boolean = false): CanvasUiState =
    finishEditing().editPage { it + element }.copy(
        selectedIds = listOf(element.id),
        editingId = if (edit) element.id else null,
    )

private fun CanvasUiState.editText(id: String, text: String): CanvasUiState = editPage { elements ->
    elements.updated(listOf(id)) {
        when (it) {
            is CanvasElement.Text -> it.copy(text = text)
            is CanvasElement.Sticky -> it.copy(text = text)
            else -> it
        }
    }
}

/** Stops typing, removing the box or note being typed in if it was left empty. */
internal fun CanvasUiState.finishEditing(): CanvasUiState {
    val id = editingId ?: return this
    val element = page.elements.firstOrNull { it.id == id }
    val empty = when (element) {
        is CanvasElement.Text -> element.text.isBlank()
        is CanvasElement.Sticky -> element.text.isBlank()
        else -> false
    }
    val done = copy(editingId = null)
    return if (empty) done.editPage { it.without(listOf(id)) }.copy(selectedIds = selectedIds - id) else done
}

internal fun CanvasUiState.editPage(transform: (List<CanvasElement>) -> List<CanvasElement>): CanvasUiState =
    copy(document = document.editPage(pageIndex, transform))
