package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.VideoSound
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoTransportTest {

    private var now = 1_000_000L
    private val transport = VideoTransport(clock = { now })
    private val video = LocalVideo(id = "v1", title = "Welcome", fileName = "v1.mp4", durationMs = 180_000L)
    private val url = "http://10.0.0.2:8766/video/v1.mp4"

    private fun loaded() = transport.also { it.load(video, url) }

    @Test
    fun `a loaded video waits at the start, paused`() {
        loaded()
        assertTrue(transport.state.value.isLoaded)
        assertFalse(transport.state.value.playing)
        assertEquals(0L, transport.positionMs())
    }

    @Test
    fun `the position advances with the clock only while playing`() {
        loaded().play()
        now += 42_000L
        assertEquals(42_000L, transport.positionMs())

        transport.pause()
        now += 10_000L
        assertEquals(42_000L, transport.positionMs(), "a paused video must not keep counting")
    }

    @Test
    fun `skip moves by ten seconds and stays inside the clip`() {
        loaded().seekTo(5_000L)
        transport.skip(-VIDEO_SKIP_MS)
        assertEquals(0L, transport.positionMs())

        transport.seekTo(175_000L)
        transport.skip(VIDEO_SKIP_MS)
        assertEquals(180_000L, transport.positionMs())
    }

    @Test
    fun `stop pauses and rewinds`() {
        loaded().play()
        now += 30_000L
        transport.stop()
        assertFalse(transport.state.value.playing)
        assertEquals(0L, transport.positionMs())
    }

    @Test
    fun `play at the end starts again from the top`() {
        loaded().seekTo(180_000L)
        transport.play()
        assertEquals(0L, transport.positionMs())
        assertTrue(transport.state.value.playing)
    }

    @Test
    fun `a looping video wraps instead of stopping`() {
        loaded().toggleLoop()
        transport.play()
        now += 190_000L
        assertEquals(10_000L, transport.positionMs())
        assertFalse(transport.checkEnded(), "a loop never ends")
    }

    @Test
    fun `turning loop off after a wrap keeps the wrapped position`() {
        loaded().toggleLoop()
        transport.play()
        now += 190_000L
        transport.toggleLoop()
        assertEquals(VideoEndAction.BLACK, transport.state.value.endAction)
        assertEquals(10_000L, transport.positionMs(), "must not jump back to the unwrapped position")
    }

    @Test
    fun `the end is noticed once and stops the clip at its last frame`() {
        loaded().play()
        now += 200_000L
        assertTrue(transport.checkEnded())
        assertFalse(transport.state.value.playing)
        assertEquals(180_000L, transport.positionMs())
        assertFalse(transport.checkEnded(), "already stopped")
    }

    @Test
    fun `the cue tells the screens what the phone is doing`() {
        loaded().setVolume(0.8f)
        transport.play()
        now += 12_000L
        val cue = transport.cue()
        assertTrue(cue.playing)
        assertEquals(12_000L, cue.positionMs)
        assertEquals(180_000L, cue.durationMs)
        assertEquals(0.8f, cue.volume)
        assertFalse(cue.muted, "the hall screen carries the sound by default")
    }

    @Test
    fun `the hall screen is muted unless it is the one with the sound`() {
        loaded().setSound(VideoSound.PHONE)
        assertTrue(transport.cue().muted)
        transport.setSound(VideoSound.OFF)
        assertTrue(transport.cue().muted)
        transport.setSound(VideoSound.HALL)
        assertFalse(transport.cue().muted)
    }

    @Test
    fun `every change is reported and bumps the sequence`() {
        var changes = 0
        transport.onChange = { changes++ }
        loaded()
        val first = transport.state.value.seq
        transport.play()
        transport.setVolume(0.5f)
        assertEquals(3, changes)
        assertEquals(first + 2, transport.state.value.seq)
    }

    @Test
    fun `controls do nothing with no video loaded`() {
        var changes = 0
        transport.onChange = { changes++ }
        transport.play()
        transport.seekTo(5_000L)
        transport.stop()
        assertEquals(0, changes)
        assertFalse(transport.state.value.playing)
    }

    @Test
    fun `only the loaded video's slide gets a cue`() {
        loaded()
        assertEquals(0L, transport.cueFor(Slide(kind = SlideKind.VIDEO, mediaUrl = url))?.positionMs)
        assertNull(transport.cueFor(Slide(kind = SlideKind.VIDEO, mediaUrl = "http://elsewhere/clip.mp4")))
    }

    @Test
    fun `unloading forgets the video`() {
        loaded().play()
        transport.unload()
        assertFalse(transport.state.value.isLoaded)
        assertNull(transport.cueFor(Slide(kind = SlideKind.VIDEO, mediaUrl = url)))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the clock re-sends a playing video and notices its end`() = runTest {
        val clocked = VideoTransport(clock = { testScheduler.currentTime })
        clocked.load(video.copy(durationMs = 12_000L), url)
        var ended = 0
        clocked.onChange = { if (!clocked.state.value.playing) ended++ }
        clocked.play()
        var resyncs = 0
        val job = launch { runVideoClock(clocked, resync = { resyncs++ }) }

        advanceTimeBy(VIDEO_RESYNC_MS + VIDEO_CLOCK_TICK_MS)
        assertEquals(1, resyncs)

        advanceTimeBy(10_000L)
        assertFalse(clocked.state.value.playing)
        assertEquals(1, ended)
        job.cancel()
    }
}
