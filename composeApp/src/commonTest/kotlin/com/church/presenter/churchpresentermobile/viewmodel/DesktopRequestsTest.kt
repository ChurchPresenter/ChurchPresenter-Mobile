package com.church.presenter.churchpresentermobile.viewmodel

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * One request of each kind on its way to the desktop at a time.
 *
 * The bug this exists for: a request can wait minutes on the desktop's approval
 * prompt, the button looked untouched, and operators pressed it again — a second
 * Go Live sent a clear, a second Add queued the song twice. On virtual time, so
 * the slow threshold costs nothing.
 */
class DesktopRequestsTest {

    @Test
    fun `a request is pending until the desktop answers`() = runTest {
        val requests = DesktopRequests()
        val answer = CompletableDeferred<Unit>()

        requests.launch(backgroundScope, DesktopRequest.PROJECT) { answer.await() }
        runCurrent()

        assertTrue(requests.isPending(DesktopRequest.PROJECT))
        answer.complete(Unit)
        runCurrent()
        assertFalse(requests.isPending(DesktopRequest.PROJECT))
    }

    @Test
    fun `a second press while waiting is ignored`() = runTest {
        val requests = DesktopRequests()
        val answer = CompletableDeferred<Unit>()
        var sent = 0

        val first = requests.launch(backgroundScope, DesktopRequest.PROJECT) { sent++; answer.await() }
        val second = requests.launch(backgroundScope, DesktopRequest.PROJECT) { sent++ }
        runCurrent()

        assertTrue(first)
        assertFalse(second)
        assertEquals(1, sent)
    }

    @Test
    fun `once answered the button works again`() = runTest {
        val requests = DesktopRequests()
        var sent = 0
        requests.launch(backgroundScope, DesktopRequest.SCHEDULE) { sent++ }
        runCurrent()

        assertTrue(requests.launch(backgroundScope, DesktopRequest.SCHEDULE) { sent++ })
        runCurrent()

        assertEquals(2, sent)
    }

    @Test
    fun `waiting on one kind does not hold up the other`() = runTest {
        // Go Live waiting on approval must not stop Add to schedule.
        val requests = DesktopRequests()
        val answer = CompletableDeferred<Unit>()
        requests.launch(backgroundScope, DesktopRequest.PROJECT) { answer.await() }
        runCurrent()

        assertTrue(requests.launch(backgroundScope, DesktopRequest.SCHEDULE) {})
    }

    @Test
    fun `a request torn down mid-wait frees the button`() = runTest {
        // Leaving the screen cancels the wait; the button must not stay stuck.
        val requests = DesktopRequests()
        val screen = Job()
        requests.launch(CoroutineScope(backgroundScope.coroutineContext + screen), DesktopRequest.PROJECT) {
            awaitCancellation()
        }
        runCurrent()

        screen.cancel()
        runCurrent()

        assertFalse(requests.isPending(DesktopRequest.PROJECT))
    }

    @Test
    fun `a quick answer says nothing about waiting`() = runTest {
        // Most answers are instant; "waiting…" flashing up before "Song is live"
        // is noise.
        val requests = DesktopRequests()
        var toldWaiting = false

        requests.launch(backgroundScope, DesktopRequest.PROJECT, onSlow = { toldWaiting = true }) {}
        advanceTimeBy(SLOW_REQUEST_MS * 5)
        runCurrent()

        assertFalse(toldWaiting)
    }

    @Test
    fun `a slow answer says it was sent and is waiting, once`() = runTest {
        val requests = DesktopRequests()
        val answer = CompletableDeferred<Unit>()
        var told = 0

        requests.launch(backgroundScope, DesktopRequest.PROJECT, onSlow = { told++ }) { answer.await() }
        advanceTimeBy(SLOW_REQUEST_MS + 1)
        runCurrent()
        advanceTimeBy(SLOW_REQUEST_MS * 10)
        runCurrent()

        assertEquals(1, told)
    }
}
