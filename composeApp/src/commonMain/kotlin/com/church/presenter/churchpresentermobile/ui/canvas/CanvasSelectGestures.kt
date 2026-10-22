package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.ImageAdjust
import com.church.presenter.churchpresentermobile.model.contains
import com.church.presenter.churchpresentermobile.model.lassoPick
import com.church.presenter.churchpresentermobile.model.resizable
import com.church.presenter.churchpresentermobile.model.resizedBounds
import com.church.presenter.churchpresentermobile.model.snappedMove
import com.church.presenter.churchpresentermobile.model.withGroupmates
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import kotlin.math.hypot

/** What a touch with the select tool landed on. */
private sealed interface Grab {
    /** One of the eight resize handles round a single picture, shape or note. */
    data class Handle(val index: Int, val id: String, val start: CanvasBounds) : Grab

    /** The round knob above a single picture, which turns it. */
    data object Knob : Grab

    data class Element(val id: String) : Grab

    data object Nothing : Grab
}

/**
 * The select tool on the board, as one gesture handler over the whole page so that
 * a touch is decided once and tracked in coordinates that do not move with what it
 * drags.
 *
 * - **Tap** selects what is under the finger (its whole group); a tap on empty
 *   board clears the selection. A second tap on a text box or note types into it.
 * - **Long-press** adds to the selection, or takes out.
 * - **Drag** an element to move it — with the selection, if it is part of it —
 *   snapping to the grid and lining up with other elements as the select options say.
 * - **Drag a handle** to resize a picture, shape or note; **tap the knob** to turn a picture.
 * - **Drag on empty board** to draw a box; what it encloses is selected.
 */
@Composable
internal fun SelectGestures(
    state: CanvasUiState,
    onIntent: (CanvasIntent) -> Unit,
    measure: (CanvasElement) -> CanvasBounds,
) {
    val colors = canvasColors
    val latest by rememberUpdatedState(state)
    val measured by rememberUpdatedState(measure)
    val marquee = remember { mutableStateListOf<CanvasPoint>() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                fun Offset.toPoint() = CanvasPoint(x / density, y / density)
                var lastTap: Pair<String, Long>? = null
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val start = down.position.toPoint()
                    val grab = grab(latest, start, measured)
                    val drag = SelectDrag(latest, grab, start, measured, onIntent, GestureSessions.next())
                    var dragging = false
                    var finished = false
                    while (!finished) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                        when {
                            // Gone, or taken by the board's two-finger pan: drop the touch.
                            change == null || change.isConsumed -> finished = true
                            !change.pressed -> {
                                if (dragging) {
                                    if (grab == Grab.Nothing) drag.finishMarquee(marquee.toList())
                                } else {
                                    val pressFor = change.uptimeMillis - down.uptimeMillis
                                    val held = pressFor >= viewConfiguration.longPressTimeoutMillis
                                    val tap = Tap(held, change.uptimeMillis)
                                    lastTap = tapped(latest, grab, tap, lastTap, onIntent)
                                }
                                finished = true
                            }
                            else -> {
                                val at = change.position.toPoint()
                                val travelled = (change.position - down.position).getDistance()
                                if (!dragging && travelled > viewConfiguration.touchSlop) {
                                    dragging = true
                                    if (grab == Grab.Nothing) marquee.add(start) else drag.begin()
                                }
                                if (dragging) {
                                    change.consume()
                                    if (grab == Grab.Nothing) marquee.add(at) else drag.to(at)
                                }
                            }
                        }
                    }
                    marquee.clear()
                }
            }
            .drawBehind { if (marquee.isNotEmpty()) drawLasso(marquee, rectangle = true, color = colors.accent) },
    )
}

/** What lies under [at]: a handle or the knob of a single selection first, then the top-most element. */
private fun grab(state: CanvasUiState, at: CanvasPoint, measure: (CanvasElement) -> CanvasBounds): Grab {
    val single = state.selected?.takeIf { !it.locked && !it.hidden }
    if (single != null && resizable(single)) {
        val box = measure(single)
        handlePoints(box).forEachIndexed { index, (x, y) ->
            if (hypot(at.x - x, at.y - y) <= HANDLE_REACH) return Grab.Handle(index, single.id, box)
        }
        val knobX = (box.left + box.right) / 2
        if (single is CanvasElement.Image && hypot(at.x - knobX, at.y - (box.top - KNOB_CENTRE)) <= HANDLE_REACH) {
            return Grab.Knob
        }
    }
    val pickable = state.page.elements.filter { !it.hidden && (!it.locked || state.options.select.selectLocked) }
    return pickable.lastOrNull { measure(it).contains(at) }?.let { Grab.Element(it.id) } ?: Grab.Nothing
}

/** How far from a handle's centre a finger still catches it, in dp. */
private const val HANDLE_REACH = 18f

/**
 * A touch that lifted without moving. Returns the tap to remember for spotting a
 * double tap next time.
 */
private fun tapped(
    state: CanvasUiState,
    grab: Grab,
    tap: Tap,
    lastTap: Pair<String, Long>?,
    onIntent: (CanvasIntent) -> Unit,
): Pair<String, Long>? {
    val held = tap.held
    val time = tap.time
    when (grab) {
        Grab.Knob -> onIntent(CanvasIntent.AdjustImage(ImageAdjust.ROTATE))
        is Grab.Handle -> Unit
        Grab.Nothing -> {
            onIntent(CanvasIntent.FinishEditing)
            onIntent(CanvasIntent.Select(null))
        }
        is Grab.Element -> {
            val element = state.page.elements.firstOrNull { it.id == grab.id }
            val typeable = element is CanvasElement.Text || element is CanvasElement.Sticky
            val again = lastTap?.first == grab.id && time - lastTap.second <= DOUBLE_TAP_MS
            when {
                held -> onIntent(CanvasIntent.ToggleSelect(grab.id))
                again && typeable -> onIntent(CanvasIntent.StartEditing(grab.id))
                else -> {
                    if (state.editingId != grab.id) onIntent(CanvasIntent.FinishEditing)
                    onIntent(CanvasIntent.Select(grab.id))
                }
            }
            return if (held) null else grab.id to time
        }
    }
    return null
}

private const val DOUBLE_TAP_MS = 350L

/** A touch that lifted without moving: whether it was held long enough to be a long press, and when it lifted. */
private class Tap(val held: Boolean, val time: Long)

/** One drag with the select tool: a move, a resize or a selection box. */
private class SelectDrag(
    private val state: CanvasUiState,
    private val grab: Grab,
    private val origin: CanvasPoint,
    private val measure: (CanvasElement) -> CanvasBounds,
    private val onIntent: (CanvasIntent) -> Unit,
    private val session: Int,
) {
    private var ids: List<String> = emptyList()
    private var startBox: CanvasBounds? = null
    private var guides: List<CanvasBounds> = emptyList()
    private var applied = 0f to 0f

    /** Works out what the drag carries: the selection, if it grabbed part of it, or what it grabbed. */
    fun begin() {
        val element = grab as? Grab.Element ?: return
        ids = if (element.id in state.selectedIds) {
            state.selectedIds
        } else {
            state.page.elements.withGroupmates(listOf(element.id)).also { onIntent(CanvasIntent.Select(element.id)) }
        }
        val moving = state.page.elements.filter { it.id in ids && !it.locked }
        ids = moving.map { it.id }
        startBox = union(moving.map(measure))
        guides = if (state.options.select.smartGuides) {
            state.page.elements.filter { it.id !in ids && !it.hidden }.map(measure)
        } else {
            emptyList()
        }
    }

    fun to(at: CanvasPoint) {
        val dx = at.x - origin.x
        val dy = at.y - origin.y
        when (grab) {
            is Grab.Handle ->
                onIntent(CanvasIntent.Resize(grab.id, resizedBounds(grab.start, grab.index, dx, dy), session))
            is Grab.Element -> {
                val box = startBox ?: return
                val (x, y) = snappedMove(box, dx, dy, state.options.select.snapToGrid, guides)
                if (x != applied.first || y != applied.second) {
                    onIntent(CanvasIntent.Move(ids, x - applied.first, y - applied.second, session))
                    applied = x to y
                }
            }
            else -> Unit
        }
    }

    /** The box dragged on empty board selects what it wholly encloses. */
    fun finishMarquee(points: List<CanvasPoint>) {
        val candidates = state.page.elements
            .filter { !it.hidden && (!it.locked || state.options.select.selectLocked) }
            .associate { it.id to measure(it) }
        onIntent(CanvasIntent.SelectMany(lassoPick(candidates, points, rectangle = true, partial = false)))
    }
}
