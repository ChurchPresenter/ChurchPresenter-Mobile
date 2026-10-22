package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp

/** Which layer row is being dragged, and how far it has moved from its place. */
@Stable
internal class LayerDrag {
    var id: String? by mutableStateOf(null)
    var offset: Float by mutableFloatStateOf(0f)
}

/**
 * Lets a layer row be dragged up and down the list by its handle — the strip
 * [handleWidth] wide down the rows' leading edge — to change the drawing order.
 *
 * Caught on the list rather than the row, so the row can move under the finger
 * without the gesture moving with it, and entirely in the Initial pass: the list's
 * own scrolling sees every event after this does, so it cannot take a drag over
 * half-way through. Each time the dragged row passes half of its neighbour it swaps
 * with it ([onMove] with +1 for up the list, which is up the drawing order, −1 for
 * down), and [drag] carries the rest of the distance for the row to follow the finger.
 */
internal fun Modifier.reorderLayers(
    list: LazyListState,
    handleWidth: Dp,
    drag: LayerDrag,
    onMove: (id: String, steps: Int) -> Unit,
): Modifier = pointerInput(list) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.position.x > handleWidth.toPx()) return@awaitEachGesture
        val row = list.layoutInfo.visibleItemsInfo.firstOrNull {
            down.position.y.toInt() in it.offset until it.offset + it.size
        } ?: return@awaitEachGesture
        val id = row.key as? String ?: return@awaitEachGesture
        down.consume()
        drag.id = id
        drag.offset = 0f
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                drag.offset += change.positionChange().y
                change.consume()
                step(list, id, drag, onMove)
            }
        } finally {
            drag.id = null
            drag.offset = 0f
        }
    }
}

/** Swaps the dragged row with a neighbour once it has passed half of it. */
private fun step(list: LazyListState, id: String, drag: LayerDrag, onMove: (String, Int) -> Unit) {
    val items = list.layoutInfo.visibleItemsInfo
    val index = items.indexOfFirst { it.key == id }
    if (index < 0) return
    val below = items.getOrNull(index + 1)
    val above = items.getOrNull(index - 1)
    when {
        below != null && drag.offset > below.size / 2f -> {
            onMove(id, -1)
            drag.offset -= below.size
        }
        above != null && drag.offset < -above.size / 2f -> {
            onMove(id, 1)
            drag.offset += above.size
        }
    }
}
