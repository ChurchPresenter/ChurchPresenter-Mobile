package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/**
 * Dragging a row of a run of show to a new place.
 *
 * While a finger is down only the order on screen changes; the move is handed to [onMove] once,
 * as from-and-to indices, when it lifts — one save and one sync rather than one per row crossed.
 */
internal class RowDragState(
    private val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
) {
    /** The row being dragged, or null. */
    var draggingId by mutableStateOf<String?>(null)
        private set

    /** How far the dragged row is drawn from the slot it currently occupies. */
    var offset by mutableFloatStateOf(0f)
        private set

    /** Row ids in the order being shown while dragging. */
    var order by mutableStateOf<List<String>>(emptyList())
        private set

    private var startIndex = -1

    fun start(id: String, ids: List<String>) {
        order = ids
        startIndex = ids.indexOf(id)
        offset = 0f
        draggingId = id.takeIf { startIndex >= 0 }
    }

    fun drag(dy: Float) {
        val id = draggingId ?: return
        offset += dy
        val visible = listState.layoutInfo.visibleItemsInfo
        val current = visible.firstOrNull { it.key == id } ?: return
        val target = swapTarget(current, offset, visible) ?: return
        val from = order.indexOf(id)
        val to = order.indexOf(target.key)
        if (from < 0 || to < 0) return
        holdScroll(listState)
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        // The row now sits in the target's slot; keep it under the finger.
        offset -= newSlotOffset(current, target, movingDown = to > from) - current.offset
    }

    fun end() {
        val id = draggingId
        val to = id?.let { order.indexOf(it) } ?: -1
        // Saved before the drag order is let go of, so the list never shows the old order in between.
        if (to >= 0 && to != startIndex) {
            holdScroll(listState)
            onMove(startIndex, to)
        }
        reset()
    }

    fun reset() {
        draggingId = null
        offset = 0f
        startIndex = -1
    }
}

/**
 * Keeps the list where it is across a reorder. A lazy list otherwise follows its first visible
 * row by key: move that row down and the list scrolls after it, pushing the row above off the top,
 * which reads as the move not having happened, or a row having vanished.
 */
internal fun holdScroll(listState: LazyListState) {
    listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
}

/** The neighbour whose area the dragged row's centre has entered, if any. */
internal fun swapTarget(current: LazyListItemInfo, offset: Float, visible: List<LazyListItemInfo>): LazyListItemInfo? {
    val centre = current.offset + offset + current.size / 2f
    return visible.firstOrNull { it.key != current.key && centre >= it.offset && centre < it.offset + it.size }
}

/** Where the dragged row's slot starts once it has swapped with [target], for rows of any height. */
internal fun newSlotOffset(current: LazyListItemInfo, target: LazyListItemInfo, movingDown: Boolean): Int =
    if (movingDown) target.offset + target.size - current.size else target.offset

/**
 * What a row wears to be draggable: a long press anywhere on it, drawn lifted while it moves.
 * [ids] is read when the drag starts, so it is the order the row was picked up from.
 */
internal fun Modifier.draggableRow(state: RowDragState, id: String, ids: () -> List<String>): Modifier =
    this
        .zIndex(if (state.draggingId == id) 1f else 0f)
        .graphicsLayer { translationY = if (state.draggingId == id) state.offset else 0f }
        .pointerInput(id) {
            detectDragGesturesAfterLongPress(
                onDragStart = { state.start(id, ids()) },
                onDrag = { change, amount -> change.consume(); state.drag(amount.y) },
                onDragEnd = state::end,
                onDragCancel = state::reset,
            )
        }

/** The row's handle: dragging it moves the row straight away, no long press. */
internal fun Modifier.dragHandle(state: RowDragState, id: String, ids: () -> List<String>): Modifier =
    pointerInput(id) {
        detectDragGestures(
            onDragStart = { state.start(id, ids()) },
            onDrag = { change, amount -> change.consume(); state.drag(amount.y) },
            onDragEnd = state::end,
            onDragCancel = state::reset,
        )
    }
