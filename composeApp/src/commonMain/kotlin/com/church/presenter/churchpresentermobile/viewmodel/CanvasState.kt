package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasPage
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions

/** The panel open down the right-hand side of the canvas, if any. */
enum class CanvasPanel { NONE, LAYERS, INSERT_IMAGE }

/** What the bar over a selected element can do to it. */
enum class SelectionAction { CROP, REPLACE, BRING_TO_FRONT, DUPLICATE, LOCK, DELETE }

/**
 * Everything the canvas screen draws.
 *
 * @param optionsOpen Whether the active tool's options panel is out beside the rail.
 *   Tapping the tool that is already active opens it, as the design has it.
 * @param zoom Percent, one of the steps − and + move between.
 */
data class CanvasUiState(
    val document: CanvasDocument = CanvasDocument(),
    val pageIndex: Int = 0,
    val tool: CanvasTool = CanvasTool.PEN,
    val optionsOpen: Boolean = false,
    val panel: CanvasPanel = CanvasPanel.NONE,
    val selectedId: String? = null,
    val zoom: Int = 100,
    val options: CanvasToolOptions = CanvasToolOptions(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
) {
    /** The page on screen. */
    val page: CanvasPage get() = document.pages[pageIndex]

    /** The selected element, if it is still on the page. */
    val selected: CanvasElement? get() = selectedId?.let { id -> page.elements.firstOrNull { it.id == id } }
}

/**
 * Everything the operator can ask of the canvas.
 *
 * One type, handed to one function, so the screen needs a single callback rather
 * than a dozen and every edit is a value a test can construct and inspect.
 */
sealed interface CanvasIntent {
    /** Picks a tool — or, for the tool already active, opens or closes its options. */
    data class PickTool(val tool: CanvasTool) : CanvasIntent

    data object CloseOptions : CanvasIntent

    data class SetOptions(val options: CanvasToolOptions) : CanvasIntent

    data class SetZoom(val percent: Int) : CanvasIntent

    data class ShowPanel(val panel: CanvasPanel) : CanvasIntent

    data class TurnToPage(val index: Int) : CanvasIntent

    data object AddPage : CanvasIntent

    /** Selects an element, or clears the selection for null. */
    data class Select(val id: String?) : CanvasIntent

    /** A finished stroke through these points, drawn with the active tool. */
    data class Stroke(val points: List<CanvasPoint>) : CanvasIntent

    data class OnSelection(val action: SelectionAction) : CanvasIntent

    data class ToggleLock(val id: String) : CanvasIntent

    data class ToggleHidden(val id: String) : CanvasIntent

    data object Undo : CanvasIntent

    data object Redo : CanvasIntent
}
