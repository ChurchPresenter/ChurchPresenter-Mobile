package com.church.presenter.churchpresentermobile.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import com.church.presenter.churchpresentermobile.present.CanvasProjection
import com.church.presenter.churchpresentermobile.present.CanvasShowResult
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Going live where someone has to allow it first — the desktop operator approving a
 * remote device. On the JVM for the same reason as [CanvasViewModelProjectionTest]:
 * a frame is an [ImageBitmap] that only Skia here can encode.
 */
class CanvasViewModelApprovalTest {

    /** A projection whose take-over waits for [answer], as the desktop waits for its operator. */
    private class Desk : CanvasProjection {
        override val asksFirst = true
        val live = MutableStateFlow(false)
        override val showing = live
        val sent = mutableListOf<Boolean>()
        var answer = CompletableDeferred<CanvasShowResult>()
        var refreshResult = CanvasShowResult.NOT_SHOWING
        var stops = 0

        override suspend fun show(png: ByteArray, paper: String, title: String, takeOver: Boolean): CanvasShowResult {
            sent += takeOver
            if (!takeOver) return refreshResult
            val result = answer.await()
            if (result == CanvasShowResult.SHOWN) live.value = true
            return result
        }

        override suspend fun stop() {
            stops++
            live.value = false
        }
    }

    private val frame = ImageBitmap(4, 3)

    private fun vm(desk: Desk) = CanvasViewModel(
        projector = desk,
        frameWork = EmptyCoroutineContext,
        saveWork = EmptyCoroutineContext,
    )

    @Test
    fun goLiveWaitsForTheOperatorAndSaysSo() = runVmTest {
        val desk = Desk()
        val vm = vm(desk)
        try {
            runCurrent()
            vm.dispatch(CanvasIntent.SetLive(true))
            vm.showFrame(frame, "#FFFFFF")
            runCurrent()
            assertTrue(vm.state.value.awaitingApproval, "the button says it is waiting")
            assertTrue(vm.state.value.live)

            desk.answer.complete(CanvasShowResult.SHOWN)
            advanceUntilIdle()
            assertFalse(vm.state.value.awaitingApproval)
            assertTrue(vm.state.value.live)
            assertNull(vm.state.value.liveNote)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun framesDrawnWhileWaitingGoAsRefreshesAndDoNotEndIt() = runVmTest {
        val desk = Desk()
        val vm = vm(desk)
        try {
            runCurrent()
            vm.dispatch(CanvasIntent.SetLive(true))
            vm.showFrame(frame, "#FFFFFF")
            runCurrent()
            // The desktop keeps these for when the operator answers, and says "not showing".
            vm.showFrame(frame, "#FFFFFF")
            vm.showFrame(frame, "#FFFFFF")
            runCurrent()
            assertEquals(true, desk.sent.first(), "the first frame after Go Live is the take-over")
            assertEquals(1, desk.sent.count { it }, "the take-over is sent once and never re-sent")
            assertTrue(desk.sent.drop(1).isNotEmpty(), "frames drawn meanwhile still go, as refreshes")
            assertTrue(vm.state.value.live, "a not-showing refresh during the wait is not the screen lost")
            assertTrue(vm.state.value.awaitingApproval)

            desk.answer.complete(CanvasShowResult.SHOWN)
            advanceUntilIdle()
            assertTrue(vm.state.value.live)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aDeclineEndsLiveAndSaysWhy() = runVmTest {
        val desk = Desk()
        val vm = vm(desk)
        try {
            runCurrent()
            vm.dispatch(CanvasIntent.SetLive(true))
            vm.showFrame(frame, "#FFFFFF")
            desk.answer.complete(CanvasShowResult.DECLINED)
            advanceUntilIdle()
            assertFalse(vm.state.value.live)
            assertFalse(vm.state.value.awaitingApproval)
            assertEquals(LiveNote.DECLINED, vm.state.value.liveNote)

            // Trying again clears the note.
            vm.dispatch(CanvasIntent.SetLive(true))
            assertNull(vm.state.value.liveNote)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aFailureEndsLiveAndSaysWhy() = runVmTest {
        val desk = Desk()
        val vm = vm(desk)
        try {
            runCurrent()
            vm.dispatch(CanvasIntent.SetLive(true))
            vm.showFrame(frame, "#FFFFFF")
            desk.answer.complete(CanvasShowResult.FAILED)
            advanceUntilIdle()
            assertFalse(vm.state.value.live)
            assertEquals(LiveNote.FAILED, vm.state.value.liveNote)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun endLiveWhileWaitingWithdrawsTheRequest() = runVmTest {
        val desk = Desk()
        val vm = vm(desk)
        try {
            runCurrent()
            vm.dispatch(CanvasIntent.SetLive(true))
            vm.showFrame(frame, "#FFFFFF")
            runCurrent()
            vm.dispatch(CanvasIntent.SetLive(false))
            advanceUntilIdle()
            assertEquals(1, desk.stops, "the desktop is told to withdraw it")
            assertFalse(vm.state.value.live)
            assertFalse(vm.state.value.awaitingApproval)
            desk.answer.complete(CanvasShowResult.SHOWN) // too late: nothing reopens it
            advanceUntilIdle()
            assertFalse(vm.state.value.live)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun theOperatorPuttingSomethingElseUpEndsLive() = runVmTest {
        val desk = Desk()
        val vm = vm(desk)
        try {
            runCurrent()
            vm.dispatch(CanvasIntent.SetLive(true))
            vm.showFrame(frame, "#FFFFFF")
            desk.answer.complete(CanvasShowResult.SHOWN)
            advanceUntilIdle()
            assertTrue(vm.state.value.live)

            desk.live.value = false // the desktop now shows a song
            advanceUntilIdle()
            assertFalse(vm.state.value.live)
            assertNull(vm.state.value.liveNote, "taken over is not a failure")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aRefreshTheDesktopNoLongerShowsEndsLive() = runVmTest {
        val desk = Desk()
        val vm = vm(desk)
        try {
            runCurrent()
            vm.dispatch(CanvasIntent.SetLive(true))
            vm.showFrame(frame, "#FFFFFF")
            desk.answer.complete(CanvasShowResult.SHOWN)
            advanceUntilIdle()

            desk.refreshResult = CanvasShowResult.NOT_SHOWING
            vm.showFrame(frame, "#FFFFFF")
            advanceUntilIdle()
            assertFalse(vm.state.value.live)
        } finally {
            tearDown(vm)
        }
    }
}
