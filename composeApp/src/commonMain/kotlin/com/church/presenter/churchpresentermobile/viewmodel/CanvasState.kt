package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasPage
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions
import com.church.presenter.churchpresentermobile.model.ImageAdjust
import com.church.presenter.churchpresentermobile.model.ImageSource
import com.church.presenter.churchpresentermobile.model.StoredPicture

/** The panel open down the right-hand side of the canvas, if any. */
enum class CanvasPanel { NONE, LAYERS, INSERT_IMAGE }

/** Where the canvas stands with the copy on this device's disk. */
enum class SaveStatus {
    /** Nothing to save to — a canvas opened without a store, as in a test. */
    NONE,
    SAVED,
    /** A change is waiting to be written, or being written now. */
    SAVING,
    /** The last write failed; the next change tries again. */
    FAILED,
}

/** Why the canvas came off — or never got onto — the screen, said once in the header. */
enum class LiveNote {
    /** The desktop operator declined to show it. */
    DECLINED,

    /** It could not be shown: the server is down, unreachable, or too old to know canvases. */
    FAILED,
}

/** What the bar over the selection can do to it. */
enum class SelectionAction { CROP, REPLACE, BRING_TO_FRONT, DUPLICATE, LOCK, DELETE }

/** The select tool's Arrange row. */
enum class ArrangeAction { ALIGN, DISTRIBUTE, GROUP, FRONT, BACK }

/**
 * Everything the canvas screen draws.
 *
 * @param optionsOpen Whether the active tool's options panel is out beside the rail.
 *   Picking a tool opens it; tapping the tool that is already active closes it.
 * @param zoom Percent, one of the steps − and + move between.
 * @param selectedIds What is selected, bottom-most first. A group is always whole.
 * @param editingId The text box or sticky note being typed into, if any.
 * @param replaceTarget The picture Replace was pressed on, which the Insert panel's
 *   next choice replaces instead of adding a new one.
 * @param awaitingApproval Whether Go Live is waiting for the desktop operator to allow it.
 * @param liveNote Why the last Go Live did not stay on screen, until the next one.
 * @param saveStatus Whether the latest change has reached the disk.
 * @param picking The picture picker the canvas is waiting on, if any — see [ImagePick].
 * @param pictureFailed Whether the last picture picked could not be opened, until the next is.
 * @param live Whether the canvas is on screen. Nothing is projected yet — this is
 *   the button's state, ready for the output it will drive.
 */
data class CanvasUiState(
    val document: CanvasDocument = CanvasDocument(),
    val pageIndex: Int = 0,
    val tool: CanvasTool = CanvasTool.PEN,
    val optionsOpen: Boolean = false,
    val panel: CanvasPanel = CanvasPanel.NONE,
    val selectedIds: List<String> = emptyList(),
    val editingId: String? = null,
    val replaceTarget: String? = null,
    val zoom: Int = 100,
    val options: CanvasToolOptions = CanvasToolOptions(),
    val live: Boolean = false,
    val awaitingApproval: Boolean = false,
    val liveNote: LiveNote? = null,
    val saveStatus: SaveStatus = SaveStatus.NONE,
    val picking: ImagePick? = null,
    val pictureFailed: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
) {
    /** The page on screen. */
    val page: CanvasPage get() = document.pages[pageIndex]

    /** The selected elements still on the page, bottom-most first. */
    val selection: List<CanvasElement> get() = page.elements.filter { it.id in selectedIds }

    /** The selection when it is exactly one element — what Crop and Replace act on. */
    val selected: CanvasElement? get() = selection.singleOrNull()
}

/**
 * A picture asked for from [source], to be placed at [at] — or in place of the
 * replace target — once the platform's picker hands it back.
 */
data class ImagePick(val source: ImageSource, val at: CanvasPoint?)

/** Where a picture can be picked from on this device: the photo library and the files. */
val PickableSources: List<ImageSource> = listOf(ImageSource.PHOTOS, ImageSource.FILES)

/**
 * Everything the operator can ask of the canvas.
 *
 * One type, handed to one function, so the screen needs a single callback rather
 * than dozens and every edit is a value a test can construct and inspect.
 */
sealed interface CanvasIntent {
    /** Picks a tool — or, for the tool already active, opens or closes its options. */
    data class PickTool(val tool: CanvasTool) : CanvasIntent

    data object CloseOptions : CanvasIntent

    data class SetOptions(val options: CanvasToolOptions) : CanvasIntent

    data class SetZoom(val percent: Int) : CanvasIntent

    data class ShowPanel(val panel: CanvasPanel) : CanvasIntent

    data class Rename(val title: String) : CanvasIntent

    data class TurnToPage(val index: Int) : CanvasIntent

    data object AddPage : CanvasIntent

    data object DuplicatePage : CanvasIntent

    data object DeletePage : CanvasIntent

    data object ClearPage : CanvasIntent

    /** Every page removed: the canvas starts again blank and untitled. Undoable, like any edit. */
    data object ClearCanvas : CanvasIntent

    /** Selects an element and its group, or clears the selection for null. */
    data class Select(val id: String?) : CanvasIntent

    /** Adds an element (and its group) to the selection, or takes it out. */
    data class ToggleSelect(val id: String) : CanvasIntent

    /** Replaces the selection with what a lasso picked up. */
    data class SelectMany(val ids: List<String>) : CanvasIntent

    /**
     * A finished stroke through these points, drawn with the pen, highlighter or
     * pixel eraser. [pressure] is the stroke's average stylus pressure — 1 for a
     * finger — which the pen uses when Pressure sensitivity is on.
     */
    data class Stroke(val points: List<CanvasPoint>, val pressure: Float = 1f) : CanvasIntent

    /**
     * Removes the elements an eraser touched. Erasing as the finger moves sends
     * many of these; all those carrying one [session] are one step on the undo history.
     */
    data class Erase(val ids: List<String>, val session: Int = NO_SESSION) : CanvasIntent

    /** Moves [ids] by ([dx], [dy]) — a step of a drag, which is one undo step per [session]. */
    data class Move(val ids: List<String>, val dx: Float, val dy: Float, val session: Int) : CanvasIntent

    /** Turns [id] to [angle] degrees — a step of a knob drag, one undo step per [session]. */
    data class Rotate(val id: String, val angle: Float, val session: Int = NO_SESSION) : CanvasIntent

    /** Fits [id] to [bounds] — a step of a resize-handle drag, one undo step per [session]. */
    data class Resize(val id: String, val bounds: CanvasBounds, val session: Int) : CanvasIntent

    data class PlaceShape(val bounds: CanvasBounds) : CanvasIntent

    data class PlaceLine(val start: CanvasPoint, val end: CanvasPoint) : CanvasIntent

    data class PlaceText(val at: CanvasPoint) : CanvasIntent

    data class PlaceSticky(val at: CanvasPoint) : CanvasIntent

    /**
     * Brings in a picture from [source], centred on [at] (or the middle of the
     * page's usual working area). Replaces the [CanvasUiState.replaceTarget] instead,
     * when there is one.
     *
     * Without a [picture] this asks for one: the ViewModel opens the picker for
     * [source], and places what comes back with the picture filled in.
     */
    data class InsertImage(
        val source: ImageSource,
        val at: CanvasPoint? = null,
        val picture: StoredPicture? = null,
    ) : CanvasIntent

    data class StartEditing(val id: String) : CanvasIntent

    /** The text typed into the box being edited, as it stands. */
    data class EditText(val id: String, val text: String) : CanvasIntent

    /** Stops typing. A box or note left empty is removed, as if never placed. */
    data object FinishEditing : CanvasIntent

    data class OnSelection(val action: SelectionAction) : CanvasIntent

    /**
     * An Arrange button. [bounds] is where each selected element lies, measured by
     * the board — text has no size until it is laid out.
     */
    data class Arrange(val action: ArrangeAction, val bounds: Map<String, CanvasBounds>) : CanvasIntent

    data class AdjustImage(val adjust: ImageAdjust) : CanvasIntent

    data class ToggleLock(val id: String) : CanvasIntent

    data class ToggleHidden(val id: String) : CanvasIntent

    /** Moves an element [steps] places up the drawing order, down for a negative number. */
    data class MoveLayer(val id: String, val steps: Int) : CanvasIntent

    data class SetLive(val live: Boolean) : CanvasIntent

    data object Undo : CanvasIntent

    data object Redo : CanvasIntent

    companion object {
        /** An [Erase] that is a single act rather than a step of a drag. */
        const val NO_SESSION = -1
    }
}
