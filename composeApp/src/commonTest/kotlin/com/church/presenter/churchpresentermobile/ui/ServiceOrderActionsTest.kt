package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Editing the running order from the drawer.
 *
 * This is the device's own list, so removing and reordering happen here. Every
 * test asserts the callback carried the row that was pressed: an off-by-one in
 * a running order removes the wrong item, mid-service, with no undo.
 */
@OptIn(ExperimentalTestApi::class)
class ServiceOrderActionsTest {

    @Test
    fun tappingAnEntryOpensIt() = runComposeUiTest {
        var opened: LocalSetlistEntry? = null
        showServiceOrder(onItemClick = { opened = it })

        click(UiTags.orderRow(0))

        assertEquals("Amazing Grace", opened?.title)
    }

    @Test
    fun tappingReportsTheEntryThatWasTapped() = runComposeUiTest {
        var opened: LocalSetlistEntry? = null
        showServiceOrder(onItemClick = { opened = it })

        click(UiTags.orderRow(2))

        assertEquals("Welcome notice", opened?.title)
    }

    @Test
    fun openingAnEntryDoesNotRemoveIt() = runComposeUiTest {
        var removed: Int? = null
        showServiceOrder(onRemove = { removed = it })

        click(UiTags.orderRow(0))

        assertNull(removed)
    }

    // ── Removing ─────────────────────────────────────────────────────────

    @Test
    fun removingReportsThePositionPressed() = runComposeUiTest {
        var removed: Int? = null
        showServiceOrder(onRemove = { removed = it })

        click(UiTags.orderRemove(1))

        assertEquals(1, removed)
    }

    @Test
    fun removingTheLastEntryReportsItsOwnPosition() = runComposeUiTest {
        var removed: Int? = null
        showServiceOrder(onRemove = { removed = it })

        click(UiTags.orderRemove(2))

        assertEquals(2, removed)
    }

    @Test
    fun everyRowCanBeRemoved() = runComposeUiTest {
        val removed = mutableListOf<Int>()
        showServiceOrder(onRemove = { removed += it })

        click(UiTags.orderRemove(0))
        click(UiTags.orderRemove(1))
        click(UiTags.orderRemove(2))

        assertEquals(listOf(0, 1, 2), removed)
    }

    @Test
    fun removingDoesNotAlsoOpenTheEntry() = runComposeUiTest {
        // The delete button sits inside a clickable row; the tap must not do both.
        var opened: LocalSetlistEntry? = null
        showServiceOrder(onItemClick = { opened = it })

        click(UiTags.orderRemove(1))

        assertNull(opened)
    }

    // ── Reordering ───────────────────────────────────────────────────────

    @Test
    fun movingUpSwapsWithTheRowAbove() = runComposeUiTest {
        var move: Pair<Int, Int>? = null
        showServiceOrder(onMove = { from, to -> move = from to to })

        click(UiTags.orderMoveUp(1))

        assertEquals(1 to 0, move)
    }

    @Test
    fun movingDownSwapsWithTheRowBelow() = runComposeUiTest {
        var move: Pair<Int, Int>? = null
        showServiceOrder(onMove = { from, to -> move = from to to })

        click(UiTags.orderMoveDown(1))

        assertEquals(1 to 2, move)
    }

    @Test
    fun theFirstRowIsNotOfferedAMoveUp() = runComposeUiTest {
        // Nothing is above it, and a button that does nothing is worse than no
        // button once the service has started.
        showServiceOrder()

        assertFalse(exists(UiTags.orderMoveUp(0)))
    }

    @Test
    fun theFirstRowIsStillOfferedAMoveDown() = runComposeUiTest {
        showServiceOrder()

        assertTrue(exists(UiTags.orderMoveDown(0)))
    }

    @Test
    fun theLastRowIsNotOfferedAMoveDown() = runComposeUiTest {
        showServiceOrder()

        assertFalse(exists(UiTags.orderMoveDown(2)))
    }

    @Test
    fun theLastRowIsStillOfferedAMoveUp() = runComposeUiTest {
        showServiceOrder()

        assertTrue(exists(UiTags.orderMoveUp(2)))
    }

    @Test
    fun aLoneEntryIsOfferedNeitherMove() = runComposeUiTest {
        // It is both the first row and the last one.
        showServiceOrder(entries = listOf(entry("Only hymn")))

        assertFalse(exists(UiTags.orderMoveUp(0)))
        assertFalse(exists(UiTags.orderMoveDown(0)))
    }

    @Test
    fun aLoneEntryCanStillBeRemoved() = runComposeUiTest {
        var removed: Int? = null
        showServiceOrder(entries = listOf(entry("Only hymn")), onRemove = { removed = it })

        click(UiTags.orderRemove(0))

        assertEquals(0, removed)
    }

    @Test
    fun theMiddleRowIsOfferedBothMoves() = runComposeUiTest {
        showServiceOrder()

        assertTrue(exists(UiTags.orderMoveUp(1)))
        assertTrue(exists(UiTags.orderMoveDown(1)))
    }

    // ── Clearing and closing ─────────────────────────────────────────────

    @Test
    fun theOrderCanBeCleared() = runComposeUiTest {
        var cleared = false
        showServiceOrder(onClear = { cleared = true })

        click(UiTags.DRAWER_CLEAR)

        assertTrue(cleared)
    }

    @Test
    fun clearIsNotOfferedWhenThereIsNothingToClear() = runComposeUiTest {
        showServiceOrder(entries = emptyList())

        assertFalse(exists(UiTags.DRAWER_CLEAR))
    }

    @Test
    fun theDrawerCanBeClosed() = runComposeUiTest {
        var closed = false
        showServiceOrder(onClose = { closed = true })

        click(UiTags.DRAWER_CLOSE)

        assertTrue(closed)
    }

    @Test
    fun theDrawerCanStillBeClosedWhenTheOrderIsEmpty() = runComposeUiTest {
        // The empty state returns early from the column; the header above it
        // has to survive that.
        var closed = false
        showServiceOrder(entries = emptyList(), onClose = { closed = true })

        click(UiTags.DRAWER_CLOSE)

        assertTrue(closed)
    }

    @Test
    fun closingDoesNotClearTheOrder() = runComposeUiTest {
        var cleared = false
        showServiceOrder(onClear = { cleared = true })

        click(UiTags.DRAWER_CLOSE)

        assertFalse(cleared)
    }
}
