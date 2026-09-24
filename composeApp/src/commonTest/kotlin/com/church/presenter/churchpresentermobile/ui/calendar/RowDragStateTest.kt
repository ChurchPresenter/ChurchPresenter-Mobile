package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RowDragStateTest {

    private class Item(
        override val index: Int,
        override val key: Any,
        override val offset: Int,
        override val size: Int,
    ) : LazyListItemInfo {
        override val contentType: Any? = null
    }

    // Three rows, 100 high with 10 between: a at 0, b at 110, c at 220.
    private val a = Item(0, "a", 0, 100)
    private val b = Item(1, "b", 110, 100)
    private val c = Item(2, "c", 220, 100)
    private val rows = listOf(a, b, c)

    @Test
    fun aRowSwapsOnceItsCentreEntersANeighbour() {
        assertNull(swapTarget(a, 50f, rows), "centre at 100: in the gap, not yet in b")
        assertEquals(b, swapTarget(a, 70f, rows), "centre at 120: inside b")
        assertEquals(c, swapTarget(a, 200f, rows), "centre at 250: past b, inside c")
        assertEquals(a, swapTarget(b, -70f, rows), "dragging up works the same way")
    }

    @Test
    fun aRowNeverSwapsWithItself() {
        assertNull(swapTarget(b, 0f, rows))
    }

    @Test
    fun theNewSlotAllowsForRowsOfDifferentHeights() {
        val tall = Item(1, "tall", 110, 200)
        // Moving a short row down past a tall one, it lands at the tall row's far end.
        assertEquals(210, newSlotOffset(a, tall, movingDown = true))
        // Moving up, it takes the neighbour's start.
        assertEquals(0, newSlotOffset(tall, a, movingDown = false))
    }

    @Test
    fun aDragThatEndsWhereItStartedMovesNothing() {
        val moves = mutableListOf<Pair<Int, Int>>()
        val state = RowDragState(LazyListState()) { from, to -> moves += from to to }

        state.start("b", listOf("a", "b", "c"))
        assertEquals("b", state.draggingId)
        // No rows are laid out in a bare LazyListState, so there is nothing to swap with.
        state.drag(400f)
        state.end()

        assertTrue(moves.isEmpty())
        assertNull(state.draggingId)
        assertEquals(0f, state.offset)
    }

    @Test
    fun aRowThatIsNotInTheListCannotBeDragged() {
        val state = RowDragState(LazyListState()) { _, _ -> error("nothing should move") }
        state.start("gone", listOf("a", "b"))
        assertNull(state.draggingId)
        state.drag(10f)
        state.end()
    }

    @Test
    fun aCancelledDragLeavesNothingBehind() {
        val state = RowDragState(LazyListState()) { _, _ -> error("a cancel must not save") }
        state.start("a", listOf("a", "b"))
        state.reset()
        assertNull(state.draggingId)
        assertEquals(0f, state.offset)
        state.end()
    }
}
