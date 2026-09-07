package com.church.presenter.churchpresentermobile.ui.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The single button at the foot of the sync sheet.
 *
 * It is three buttons in one — Copy, Cancel, Done — and which one it is decides
 * what the tap does. The wrong branch either cancels a sync the operator meant
 * to start, or reports success for a copy that never ran.
 */
class SyncButtonTest {

    @Test
    fun `nothing running yet offers to start the copy`() {
        val button = syncButtonFor(isRunning = false, isFinished = false, canStart = true)

        assertEquals(SyncButton.Action.START, button.action)
        assertTrue(button.isEnabled)
    }

    @Test
    fun `a copy in flight offers to cancel it`() {
        val button = syncButtonFor(isRunning = true, isFinished = false, canStart = false)

        assertEquals(SyncButton.Action.CANCEL, button.action)
    }

    @Test
    fun `a finished copy offers to close the sheet`() {
        val button = syncButtonFor(isRunning = false, isFinished = true, canStart = false)

        assertEquals(SyncButton.Action.CLOSE, button.action)
    }

    @Test
    fun `a copy still running wins over a previous run's outcome`() {
        // The last run's result stays on screen while the next one is in
        // flight; the button has to be the way out of the running one.
        val button = syncButtonFor(isRunning = true, isFinished = true, canStart = true)

        assertEquals(SyncButton.Action.CANCEL, button.action)
    }

    @Test
    fun `with nothing selected there is nothing to copy`() {
        // Every book unticked is not a sync. Copying nothing and reporting
        // success reads as the feature being broken.
        val button = syncButtonFor(isRunning = false, isFinished = false, canStart = false)

        assertEquals(SyncButton.Action.START, button.action)
        assertFalse(button.isEnabled)
    }

    @Test
    fun `cancel is always pressable`() {
        // Whatever else is true, an operator must never be trapped watching a
        // sync they cannot stop.
        assertTrue(syncButtonFor(isRunning = true, isFinished = false, canStart = false).isEnabled)
        assertTrue(syncButtonFor(isRunning = true, isFinished = true, canStart = false).isEnabled)
    }

    @Test
    fun `close is always pressable`() {
        assertTrue(syncButtonFor(isRunning = false, isFinished = true, canStart = false).isEnabled)
    }

    @Test
    fun `only cancel is styled as destructive`() {
        // The red button stops something; a red Copy or Done would read as
        // deleting the library.
        assertTrue(syncButtonFor(isRunning = true, isFinished = false, canStart = false).isDestructive)
        assertFalse(syncButtonFor(isRunning = false, isFinished = true, canStart = false).isDestructive)
        assertFalse(syncButtonFor(isRunning = false, isFinished = false, canStart = true).isDestructive)
    }

    @Test
    fun `every combination resolves to exactly one action`() {
        // A guard against a future fourth state leaving the button ambiguous.
        val seen = listOf(true, false).flatMap { running ->
            listOf(true, false).flatMap { finished ->
                listOf(true, false).map { canStart -> syncButtonFor(running, finished, canStart).action }
            }
        }

        assertEquals(8, seen.size)
        assertEquals(setOf(SyncButton.Action.START, SyncButton.Action.CANCEL, SyncButton.Action.CLOSE), seen.toSet())
    }
}
