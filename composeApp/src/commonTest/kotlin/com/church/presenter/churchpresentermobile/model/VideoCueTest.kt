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
    fun `drift between cues is held to a looser tolerance than a cue`() {
        // A seek costs a rebuffer: correcting every half-second gap made a player
        // stall, fall behind and seek again, every two seconds.
        assertTrue(shouldSeek(10_800L, 10_000L, playing))
        assertFalse(shouldSeek(10_800L, 10_000L, playing, toleranceMs = VIDEO_DRIFT_TOLERANCE_MS))
        assertTrue(shouldSeek(11_600L, 10_000L, playing, toleranceMs = VIDEO_DRIFT_TOLERANCE_MS))
    }

    @Test
    fun `a loop measures the short way round its seam`() {
        val loop = playing.copy(durationMs = 30_000L, endAction = VideoEndAction.LOOP)
        assertFalse(shouldSeek(29_900L, 100L, loop))
        assertTrue(shouldSeek(29_900L, 100L, playing.copy(durationMs = 30_000L)))
    }

    @Test
    fun `only a stopped clip at its end with a known length is ended`() {
        val ended = VideoCue(playing = false, positionMs = 59_900L, durationMs = 60_000L)
        assertFalse(ended.copy(playing = true).endedToBlack, "still playing")
        assertFalse(ended.copy(durationMs = 0L).endedToBlack, "length unknown")
        assertFalse(ended.copy(positionMs = 30_000L).endedToBlack, "paused mid-clip")
        assertEquals(0.5f, ended.copy(volume = 0.5f).effectiveVolume)
        assertEquals(1f, ended.copy(volume = 3f).effectiveVolume)
    }

    @Test
    fun `a clip of unknown length keeps counting`() {
        assertEquals(15_000L, expectedPositionMs(playing.copy(durationMs = 0L), elapsedMs = 5_000L))
        assertEquals(10_000L, expectedPositionMs(playing, elapsedMs = -50L), "a clock that went backwards adds nothing")
    }

    @Test
    fun `a finished clip set to black reads as ended`() {
        val ended = VideoCue(playing = false, positionMs = 59_900L, durationMs = 60_000L)
        assertTrue(ended.endedToBlack)
        assertFalse(ended.copy(endAction = VideoEndAction.HOLD).endedToBlack)
        assertEquals(0f, ended.copy(muted = true).effectiveVolume)
    }
}
