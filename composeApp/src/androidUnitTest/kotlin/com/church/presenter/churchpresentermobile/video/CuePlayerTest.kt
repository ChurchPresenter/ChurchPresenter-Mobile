package com.church.presenter.churchpresentermobile.video

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.widget.VideoView
import com.church.presenter.churchpresentermobile.model.VideoCue
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.ui.standalone.CuePlayer
import com.church.presenter.churchpresentermobile.ui.standalone.phoneOutput
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Lives outside the `ui` package on purpose: that package is the Compose UI
 * tests, excluded from the Android run, and this is plain logic over a mock.
 *
 * How the Android player follows the phone's transport, against a mocked
 * VideoView: what volume it plays at, whether it loops, when it seeks and to
 * where, and which speaker its sound goes to.
 */
class CuePlayerTest {

    private var now = 10_000L
    private val prepared = slot<MediaPlayer.OnPreparedListener>()
    private val completed = slot<MediaPlayer.OnCompletionListener>()
    private var position = 0
    private var playing = false
    private val speaker = device(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
    private val headphones = device(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
    private var outputs = arrayOf(speaker)
    private val audio = mockk<AudioManager> { every { getDevices(any()) } answers { outputs } }
    private val context = mockk<Context> { every { getSystemService(Context.AUDIO_SERVICE) } returns audio }
    private val player = mockk<MediaPlayer>(relaxed = true)
    private val view = mockk<VideoView>(relaxed = true) {
        every { setOnPreparedListener(capture(prepared)) } answers { }
        every { setOnCompletionListener(capture(completed)) } answers { }
        every { currentPosition } answers { position }
        every { isPlaying } answers { playing }
        every { start() } answers { playing = true }
        every { pause() } answers { playing = false }
        every { context } returns this@CuePlayerTest.context
    }

    private fun device(type: Int) = mockk<AudioDeviceInfo> { every { this@mockk.type } returns type }

    private fun cuePlayer(sdk: Int = 34) = CuePlayer(clock = { now }, sdk = sdk).also { it.attach(view) }

    private fun CuePlayer.open(cue: VideoCue?, muted: Boolean = false, soundOnDevice: Boolean = false) {
        update(view, URL, cue, muted, soundOnDevice)
        prepared.captured.onPrepared(player)
    }

    @Test
    fun `a video from elsewhere plays muted and looping, as it always has`() {
        cuePlayer().open(cue = null)

        verify { player.isLooping = true }
        verify { player.setVolume(0f, 0f) }
        assertTrue(playing)
    }

    @Test
    fun `a cue sets the volume, and muting silences it`() {
        val cue = VideoCue(playing = true, volume = 0.6f, durationMs = 60_000L)
        cuePlayer().open(cue)
        verify { player.setVolume(0.6f, 0.6f) }

        cuePlayer().open(cue, muted = true)
        verify { player.setVolume(0f, 0f) }
    }

    @Test
    fun `only a looping cue loops`() {
        cuePlayer().open(VideoCue(endAction = VideoEndAction.LOOP))
        verify { player.isLooping = true }

        cuePlayer().open(VideoCue(endAction = VideoEndAction.BLACK))
        verify { player.isLooping = false }
    }

    @Test
    fun `opening the clip is allowed for when it starts`() {
        val p = cuePlayer()
        p.update(view, URL, VideoCue(playing = true, positionMs = 30_000L, durationMs = 60_000L), false, false)
        now += 1_500L // the time VideoView took to prepare
        prepared.captured.onPrepared(player)

        verify { view.seekTo(31_500) }
    }

    @Test
    fun `a player already close enough is left alone`() {
        position = 30_200
        cuePlayer().open(VideoCue(playing = true, positionMs = 30_000L, durationMs = 60_000L))

        verify(exactly = 0) { view.seekTo(any<Int>()) }
    }

    @Test
    fun `a paused cue pauses a playing clip`() {
        val p = cuePlayer()
        p.open(VideoCue(playing = true, durationMs = 60_000L))
        assertTrue(playing)

        p.update(view, URL, VideoCue(playing = false, durationMs = 60_000L, seq = 2), false, false)
        assertFalse(playing)
    }

    @Test
    fun `the same address is not reopened when only the cue changes`() {
        val p = cuePlayer()
        p.open(VideoCue(playing = true))
        p.update(view, URL, VideoCue(playing = true, volume = 0.3f, seq = 2), false, false)

        verify(exactly = 1) { view.setVideoURI(any()) }
    }

    @Test
    fun `drift is corrected only past the looser tolerance, and not straight after a seek`() {
        val p = cuePlayer()
        p.open(VideoCue(playing = true, positionMs = 0L, durationMs = 600_000L))
        position = 0
        now += 10_000L

        // 10s expected, 0s actual — but a seek just happened? No: none yet, so it corrects.
        p.correctDrift()
        verify(exactly = 1) { view.seekTo(10_000) }

        // Straight after that seek it is left to settle, however far off it looks.
        now += 1_000L
        p.correctDrift()
        verify(exactly = 1) { view.seekTo(any<Int>()) }

        // Settled, and within the looser tolerance: left alone.
        now += 6_000L
        position = 16_000
        p.correctDrift()
        verify(exactly = 1) { view.seekTo(any<Int>()) }
    }

    @Test
    fun `a detached or paused player is not corrected`() {
        val p = cuePlayer()
        p.open(VideoCue(playing = false, durationMs = 60_000L))
        now += 20_000L
        p.correctDrift()
        p.detach()
        p.correctDrift()

        verify(exactly = 0) { view.seekTo(any<Int>()) }
    }

    @Test
    fun `reaching the end goes to black only when asked to`() {
        var black = 0
        val p = cuePlayer().also { it.onEndedToBlack = { black++ } }
        p.open(VideoCue(endAction = VideoEndAction.HOLD))
        completed.captured.onCompletion(player)
        assertEquals(0, black)

        p.update(view, URL, VideoCue(endAction = VideoEndAction.BLACK, seq = 2), false, false)
        completed.captured.onCompletion(player)
        assertEquals(1, black)
    }

    @Test
    fun `phone sound goes to the phone's own speaker`() {
        cuePlayer().open(VideoCue(playing = true), soundOnDevice = true)

        verify { player.setPreferredDevice(speaker) }
    }

    @Test
    fun `phone sound prefers headphones when they are plugged in`() {
        outputs = arrayOf(speaker, headphones)
        cuePlayer().open(VideoCue(playing = true), soundOnDevice = true)

        verify { player.setPreferredDevice(headphones) }
        assertEquals(headphones, phoneOutput(outputs))
    }

    @Test
    fun `the hall screen's player follows the system's route`() {
        cuePlayer().open(VideoCue(playing = true), soundOnDevice = false)

        verify { player.setPreferredDevice(null) }
    }

    @Test
    fun `before Android 9 the route is left to the system`() {
        cuePlayer(sdk = 26).open(VideoCue(playing = true), soundOnDevice = true)

        verify(exactly = 0) { player.setPreferredDevice(any()) }
    }

    private companion object {
        const val URL = "file:///data/videos/v1.mp4"
    }
}
