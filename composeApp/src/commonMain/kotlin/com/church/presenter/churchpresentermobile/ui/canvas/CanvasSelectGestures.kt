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
import com.church.presenter.churchpresentermobile.model.lassoPick
import com.church.presenter.churchpresentermobile.model.ROTATE_SNAP
import com.church.presenter.churchpresentermobile.model.centre
import com.church.presenter.churchpresentermobile.model.knobAngle
import com.church.presenter.churchpresentermobile.model.quarterTurned
import com.church.presenter.churchpresentermobile.model.resizable
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.rotatedPoint
import com.church.presenter.churchpresentermobile.model.rotatedResize
import com.church.presenter.churchpresentermobile.model.snappedAngle
import com.church.presenter.churchpresentermobile.model.turnedContains
import com.church.presenter.churchpresentermobile.model.snappedMove
import com.church.presenter.churchpresentermobile.model.withGroupmates
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import kotlin.math.hypot

/** What a touch with the select tool landed on. */
private sealed interface Grab {
    /** One of the eight resize handles round a single picture, shape or note, turned [angle] degrees. */
    data class Handle(val index: Int, val id: String, val start: CanvasBounds, val angle: Float) : Grab

    /** The round knob above a single element, which turns it about [centre]. */
    data class Knob(val id: String, val centre: CanvasPoint, val angle: Float) : Grab

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
 * - **Drag a handle** to resize a picture, shape or note. **Drag the knob** to turn
 *   anything, in [ROTATE_SNAP]° steps; **tap it** for a quarter turn.
 * - **Drag on empty board** to draw a box; what it encloses is selected.
 */
@Composable
internal fun SelectGestures(
    state: CanvasUiState,
    onIntent: (CanvasIntent) -> Unit,
    measure: (CanvasElement) -> CanvasBounds,
    measureBox: (CanvasElement) -> CanvasBounds,
    paths: InkPaths,
) {
    val colors = canvasColors
    val latest by rememberUpdatedState(state)
    val measured by rememberUpdatedState(measure)
    val boxed by rememberUpdatedState(measureBox)
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
                    val grab = grab(latest, start, boxed, paths)
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

/**
 * What lies under [at]: a handle or the knob of a single selection first, then the
 * top-most element. A turned element is hit where it is drawn, not by the upright
 * box round it, so [at] is turned back into its frame before it is compared.
 */
private fun grab(
    state: CanvasUiState,
    at: CanvasPoint,
    measureBox: (CanvasElement) -> CanvasBounds,
    paths: InkPaths,
): Grab {
    val single = state.selected?.takeIf { !it.locked && !it.hidden }
    if (single != null) {
        val box = measureBox(single)
        val local = rotatedPoint(at, -single.angle, box.centre)
        if (resizable(single)) {
            handlePoints(box).forEachIndexed { index, (x, y) ->
                if (hypot(local.x - x, local.y - y) <= HANDLE_REACH) {
                    return Grab.Handle(index, single.id, box, single.angle)
                }
            }
        }
        if (hypot(local.x - box.centre.x, local.y - (box.top - KNOB_CENTRE)) <= HANDLE_REACH) {
            return Grab.Knob(single.id, box.centre, single.angle)
        }
    }
    return pickable(state).lastOrNull { touches(it, at, measureBox, paths) }?.let { Grab.Element(it.id) }
        ?: Grab.Nothing
}

/**
 * What Select can pick: nothing hidden, nothing locked unless the operator allows
 * it, and never a pixel eraser's mark — it is invisible, and picking it instead of
 * what it erased, then deleting it, brought the erased ink back.
 */
private fun pickable(state: CanvasUiState): List<CanvasElement> = state.page.elements.filter {
    !it.hidden && (!it.locked || state.options.select.selectLocked) &&
        !(it is CanvasElement.Ink && it.kind == InkKind.ERASER)
}

/**
 * Whether [at] is on [element]. A stroke is hit along its line, not anywhere in
 * the box round it: a circle or a long stroke drawn across a shape, note or
 * picture used to catch every tap inside its box, so what lay under it could not
 * be selected at all.
 */
private fun touches(
    element: CanvasElement,
    at: CanvasPoint,
    measureBox: (CanvasElement) -> CanvasBounds,
    paths: InkPaths,
): Boolean {
    val box = measureBox(element)
    if (!turnedContains(box, element.angle, at)) return false
    if (element !is CanvasElement.Ink) return true
    val local = rotatedPoint(at, -element.angle, box.centre)
    val reach = element.width / 2f + STROKE_TAP_REACH
    return paths.samples(element.pathData).any {
        hypot(element.x + it.x - local.x, element.y + it.y - local.y) <= reach
    }
}

/** How far beside a stroke's edge a fingertip still picks it, in dp. */
private const val STROKE_TAP_REACH = 12f

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
        is Grab.Knob -> onIntent(CanvasIntent.Rotate(grab.id, quarterTurned(grab.angle)))
        is Grab.Handle -> Unit
        Grab.Nothing -> {
            onIntent(CanvasIntent.FinishEditing)
            onIntent(CanvasIntent.Select(null))
        }
        is Grab.Element -> {
            val element = state.page.elements.firstOrNull { it.id == grab.id }
            val typeable = element is CanvasElement.Text || element is CanvasElement.Sticky
            val again = lastTap?.first == grab.id && time - lastTap.second <= DOUBLE_TAP_MS
            // A note or text box already selected types on the next tap, however slow: a
            // double tap inside 350 ms was the only way in, and it was easy to miss.
            val alreadyPicked = state.selectedIds == listOf(grab.id)
            when {
                held -> onIntent(CanvasIntent.ToggleSelect(grab.id))
                (again || alreadyPicked) && typeable -> onIntent(CanvasIntent.StartEditing(grab.id))
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
    private var turned: Float? = null

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
            is Grab.Handle -> onIntent(
                CanvasIntent.Resize(grab.id, rotatedResize(grab.start, grab.index, dx, dy, grab.angle), session),
            )
            is Grab.Knob -> {
                val angle = snappedAngle(knobAngle(grab.centre, at))
                if (angle != (turned ?: grab.angle)) {
                    onIntent(CanvasIntent.Rotate(grab.id, angle, session))
                    turned = angle
                }
            }
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
        val candidates = pickable(state).associate { it.id to measure(it) }
        onIntent(CanvasIntent.SelectMany(lassoPick(candidates, points, rectangle = true, partial = false)))
    }
}
