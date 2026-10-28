package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The canvas going live wherever the app's mode says, decided per call. */
class CanvasProjectionRouterTest {

    private class Recording(override val asksFirst: Boolean) : CanvasProjection {
        val shown = mutableListOf<Boolean>()
        var stops = 0
        val live = MutableStateFlow(false)
        override val showing = live
        override suspend fun show(png: ByteArray, paper: String, title: String, takeOver: Boolean): CanvasShowResult {
            shown += takeOver
            return CanvasShowResult.SHOWN
        }
        override suspend fun stop() {
            stops++
        }
    }

    private val mode = MutableStateFlow(AppMode.STANDALONE)
    private val standalone = Recording(asksFirst = false)
    private val remote = Recording(asksFirst = true)
    private val router = CanvasProjectionRouter(mode, standalone, remote)

    @Test
    fun `each mode reaches its own outputs`() = runTest {
        router.show(byteArrayOf(1), "#000000", "", takeOver = true)
        assertEquals(listOf(true), standalone.shown)
        assertFalse(router.asksFirst)

        mode.value = AppMode.REMOTE
        router.show(byteArrayOf(1), "#000000", "", takeOver = false)
        router.stop()
        assertEquals(listOf(false), remote.shown)
        assertEquals(1, remote.stops)
        assertTrue(router.asksFirst, "the desktop operator approves")
    }

    @Test
    fun `calendar mode has nowhere to project`() = runTest {
        mode.value = AppMode.CALENDAR
        assertEquals(CanvasShowResult.FAILED, router.show(byteArrayOf(1), "#000000", "", takeOver = true))
        router.stop()
        assertFalse(router.asksFirst)
        assertFalse(router.showing.first())
        assertEquals(0, standalone.stops + remote.stops)
    }

    @Test
    fun `showing follows the projection of the current mode`() = runTest {
        remote.live.value = true
        assertFalse(router.showing.first(), "standalone is not showing")
        mode.value = AppMode.REMOTE
        assertTrue(router.showing.first())
    }
}
