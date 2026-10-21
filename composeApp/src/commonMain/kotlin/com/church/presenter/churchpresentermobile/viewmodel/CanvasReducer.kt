package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.broughtToFront
import com.church.presenter.churchpresentermobile.model.editPage
import com.church.presenter.churchpresentermobile.model.strokePathData
import com.church.presenter.churchpresentermobile.model.updated
import com.church.presenter.churchpresentermobile.model.withDuplicate
import com.church.presenter.churchpresentermobile.model.withNewPage
import com.church.presenter.churchpresentermobile.model.without

/**
 * The canvas after [intent], as a plain function of the canvas before it.
 *
 * Undo and Redo are not handled here — they need the history, which is the
 * ViewModel's — and come back unchanged. [newId] names any element an intent
 * creates; the ViewModel hands out ids no element already has.
 */
internal fun CanvasUiState.reduce(intent: CanvasIntent, newId: () -> String): CanvasUiState = when (intent) {
    is CanvasIntent.PickTool -> pickTool(intent.tool)
    CanvasIntent.CloseOptions -> copy(optionsOpen = false)
    is CanvasIntent.SetOptions -> copy(options = intent.options)
    is CanvasIntent.SetZoom -> copy(zoom = intent.percent)
    is CanvasIntent.ShowPanel -> copy(panel = intent.panel)
    is CanvasIntent.TurnToPage ->
        if (intent.index in document.pages.indices) copy(pageIndex = intent.index, selectedId = null) else this
    CanvasIntent.AddPage -> copy(document = document.withNewPage(), pageIndex = document.pages.size, selectedId = null)
    is CanvasIntent.Select -> copy(selectedId = intent.id)
    is CanvasIntent.Stroke -> stroke(intent.points, newId)
    is CanvasIntent.OnSelection -> onSelection(intent.action, newId)
    is CanvasIntent.ToggleLock -> editPage { it.updated(intent.id) { e -> e.withFlags(locked = !e.locked) } }
    is CanvasIntent.ToggleHidden -> editPage { it.updated(intent.id) { e -> e.withFlags(hidden = !e.hidden) } }
    CanvasIntent.Undo, CanvasIntent.Redo -> this
}

/**
 * Picking the tool already active opens or closes its options; picking another
 * closes them and drops the selection, which belonged to the select tool. The
 * image tool brings its Insert panel with it, and leaving it takes the panel away.
 */
private fun CanvasUiState.pickTool(tool: CanvasTool): CanvasUiState =
    if (tool == this.tool) {
        copy(optionsOpen = !optionsOpen)
    } else {
        copy(
            tool = tool,
            optionsOpen = false,
            selectedId = null,
            panel = when {
                tool == CanvasTool.IMAGE -> CanvasPanel.INSERT_IMAGE
                panel == CanvasPanel.INSERT_IMAGE -> CanvasPanel.NONE
                else -> panel
            },
        )
    }

/** A stroke through [points], drawn with the pen or the highlighter. Other tools draw nothing yet. */
private fun CanvasUiState.stroke(points: List<CanvasPoint>, newId: () -> String): CanvasUiState {
    if (points.isEmpty()) return this
    val pathData = strokePathData(points)
    val ink = when (tool) {
        CanvasTool.PEN -> CanvasElement.Ink(
            id = newId(),
            x = 0f,
            y = 0f,
            pathData = pathData,
            ink = options.pen.ink,
            width = options.pen.width,
            opacity = options.pen.opacity,
        )
        CanvasTool.HIGHLIGHTER -> CanvasElement.Ink(
            id = newId(),
            x = 0f,
            y = 0f,
            pathData = pathData,
            ink = options.pen.ink,
            width = options.highlighter.width,
            opacity = options.highlighter.opacity,
            kind = InkKind.HIGHLIGHTER,
            highlight = options.highlighter.color,
        )
        else -> return this
    }
    return editPage { it + ink }
}

/**
 * [action] applied to the selected element. Crop and Replace need the image
 * pipeline, which does not exist yet, so they leave the page as it is.
 */
private fun CanvasUiState.onSelection(action: SelectionAction, newId: () -> String): CanvasUiState {
    val id = selected?.id ?: return this
    return when (action) {
        SelectionAction.DELETE -> editPage { it.without(id) }.copy(selectedId = null)
        SelectionAction.DUPLICATE -> {
            val copyId = newId()
            editPage { it.withDuplicate(id, copyId) }.copy(selectedId = copyId)
        }
        SelectionAction.BRING_TO_FRONT -> editPage { it.broughtToFront(id) }
        SelectionAction.LOCK -> editPage { it.updated(id) { e -> e.withFlags(locked = !e.locked) } }
        SelectionAction.CROP, SelectionAction.REPLACE -> this
    }
}

private fun CanvasUiState.editPage(transform: (List<CanvasElement>) -> List<CanvasElement>): CanvasUiState =
    copy(document = document.editPage(pageIndex, transform))
