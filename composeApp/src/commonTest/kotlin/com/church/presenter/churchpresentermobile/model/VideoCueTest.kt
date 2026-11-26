package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCueTest {

    private val playing = VideoCue(playing = true, positionMs = 10_000L, durationMs = 60_000L)

    @Test
    fun `a playing cue is ahead by the time since it arrived`() {
        // The preview that took a second to open, or appeared a minute after Play,
        // must start where the phone is now — this was the drift.
        assertEquals(11_200L, expectedPositionMs(playing, elapsedMs = 1_200L))
        assertEquals(10_000L, expectedPositionMs(playing.copy(playing = false), elapsedMs = 5_000L))
    }

    @Test
    fun `the expected position stops at the end, or wraps when looping`() {
        assertEquals(60_000L, expectedPositionMs(playing, elapsedMs = 90_000L))
        assertEquals(40_000L, expectedPositionMs(playing.copy(endAction = VideoEndAction.LOOP), elapsedMs = 90_000L))
    }

    @Test
    fun `a gap inside the tolerance is left alone and a wider one is corrected`() {
        assertFalse(shouldSeek(10_400L, 10_000L, playing))
        assertTrue(shouldSeek(10_700L, 10_000L, playing))
    }

    @Test
    fun `a loop measures the short way round its seam`() {
        val loop = playing.copy(durationMs = 30_000L, endAction = VideoEndAction.LOOP)
        assertFalse(shouldSeek(29_900L, 100L, loop))
        assertTrue(shouldSeek(29_900L, 100L, playing.copy(durationMs = 30_000L)))
    }

    @Test
    fun `a finished clip set to black reads as ended`() {
        val ended = VideoCue(playing = false, positionMs = 59_900L, durationMs = 60_000L)
        assertTrue(ended.endedToBlack)
        assertFalse(ended.copy(endAction = VideoEndAction.HOLD).endedToBlack)
        assertEquals(0f, ended.copy(muted = true).effectiveVolume)
    }
}
