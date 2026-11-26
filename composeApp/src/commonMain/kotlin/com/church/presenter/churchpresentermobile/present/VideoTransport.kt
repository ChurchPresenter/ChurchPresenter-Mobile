package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.VideoCue
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.VideoSound
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How far the ±10 buttons jump. */
const val VIDEO_SKIP_MS = 10_000L

/**
 * The transport as the operator last set it.
 *
 * The position is not stored as a number that ticks: it is where the video was
 * at [anchorAt] plus however long it has been playing since, so nothing has to
 * run every frame to keep it right, and every reader agrees on it.
 */
data class VideoTransportState(
    val video: LocalVideo? = null,
    val url: String? = null,
    val playing: Boolean = false,
    val anchorPositionMs: Long = 0L,
    val anchorAt: Long = 0L,
    val endAction: VideoEndAction = VideoEndAction.BLACK,
    val volume: Float = 1f,
    val sound: VideoSound = VideoSound.HALL,
    val seq: Long = 0L,
) {
    val isLoaded: Boolean get() = video != null && url != null
    val durationMs: Long get() = video?.durationMs ?: 0L
}

/**
 * The phone's video player — the one clock the hall screen follows.
 *
 * Owns the play/pause/seek/loop/volume state for the loaded video and turns it
 * into a [VideoCue] for the outputs. Deliberately knows nothing about the
 * engine: [onChange] is told after every change, and whoever owns both (the app
 * shell) decides whether that change reaches a screen.
 *
 * @param clock Milliseconds, monotonic enough to measure a few minutes. Injected
 *   so the position maths can be tested without waiting.
 */
class VideoTransport(private val clock: () -> Long) {

    private val _state = MutableStateFlow(VideoTransportState())
    val state: StateFlow<VideoTransportState> = _state.asStateFlow()

    /** Told after every change that an output should hear about. */
    var onChange: () -> Unit = {}

    /** Puts [video] in the player, paused at the start. */
    fun load(video: LocalVideo, url: String) {
        val current = _state.value
        _state.value = current.copy(
            video = video,
            url = url,
            playing = false,
            anchorPositionMs = 0L,
            anchorAt = clock(),
            seq = current.seq + 1,
        )
        onChange()
    }

    /** Takes the video out of the player. */
    fun unload() {
        _state.value = _state.value.copy(video = null, url = null, playing = false, anchorPositionMs = 0L)
    }

    /** Where the video is now, in milliseconds. */
    fun positionMs(): Long = positionOf(_state.value, clock())

    /**
     * Plays or pauses, from [fromMs] when given and from where it is otherwise.
     *
     * Play at the end means play again: a finished clip whose play button did
     * nothing would read as broken.
     */
    fun setPlaying(playing: Boolean, fromMs: Long? = null) {
        val s = _state.value
        if (!s.isLoaded) return
        val here = fromMs ?: positionMs()
        val from = if (playing && s.durationMs > 0 && here >= s.durationMs) 0L else here
        update { it.copy(playing = playing, anchorPositionMs = from, anchorAt = clock()) }
    }

    fun seekTo(positionMs: Long) {
        val s = _state.value
        if (!s.isLoaded) return
        val clamped = if (s.durationMs > 0) positionMs.coerceIn(0L, s.durationMs) else positionMs.coerceAtLeast(0L)
        update { it.copy(anchorPositionMs = clamped, anchorAt = clock()) }
    }

    fun setEndAction(action: VideoEndAction) {
        // Re-anchored so a loop that has already wrapped does not jump back to its
        // unwrapped position the moment it stops looping.
        update { it.copy(endAction = action, anchorPositionMs = positionMs(), anchorAt = clock()) }
    }

    fun setVolume(volume: Float) = update { it.copy(volume = volume.coerceIn(0f, 1f)) }

    fun setSound(sound: VideoSound) = update { it.copy(sound = sound) }

    /**
     * Notices the video running out. Called on a timer by [runVideoClock];
     * returns true when it stopped, so the caller knows the screens were told.
     */
    fun checkEnded(): Boolean {
        val s = _state.value
        if (!s.playing || s.endAction == VideoEndAction.LOOP || s.durationMs <= 0L) return false
        if (positionOf(s, clock()) < s.durationMs) return false
        update { it.copy(playing = false, anchorPositionMs = s.durationMs, anchorAt = clock()) }
        return true
    }

    private inline fun update(block: (VideoTransportState) -> VideoTransportState) {
        val next = block(_state.value)
        _state.value = next.copy(seq = next.seq + 1)
        onChange()
    }

    internal companion object {
        /** The position [state] implies at [now]: anchored, advanced while playing, wrapped or clamped at the end. */
        fun positionOf(state: VideoTransportState, now: Long): Long {
            val elapsed = if (state.playing) (now - state.anchorAt).coerceAtLeast(0L) else 0L
            val raw = state.anchorPositionMs + elapsed
            val duration = state.durationMs
            return when {
                duration <= 0L -> raw
                state.endAction == VideoEndAction.LOOP -> raw % duration
                else -> raw.coerceAtMost(duration)
            }
        }
    }
}

/** What the outputs should be doing, with the position as of now. */
fun VideoTransport.cue(): VideoCue {
    val s = state.value
    return VideoCue(
        playing = s.playing,
        positionMs = positionMs(),
        durationMs = s.durationMs,
        seq = s.seq,
        endAction = s.endAction,
        volume = s.volume,
        muted = s.sound != VideoSound.HALL,
    )
}

fun VideoTransport.play() = setPlaying(true)

fun VideoTransport.pause() = setPlaying(false)

fun VideoTransport.togglePlay() = setPlaying(!state.value.playing)

/** Stops and rewinds — the square button. One change, so the screens hear of it once. */
fun VideoTransport.stop() = setPlaying(false, fromMs = 0L)

fun VideoTransport.skip(deltaMs: Long) = seekTo(positionMs() + deltaMs)

/** The loop button: on means [VideoEndAction.LOOP], off goes back to black. */
fun VideoTransport.toggleLoop() = setEndAction(
    if (state.value.endAction == VideoEndAction.LOOP) VideoEndAction.BLACK else VideoEndAction.LOOP,
)

/**
 * The cue for [slide], or null when it is not the video this transport has
 * loaded — a video slide from anywhere else keeps its old muted, looping play.
 */
fun VideoTransport.cueFor(slide: Slide): VideoCue? {
    val s = state.value
    if (!s.isLoaded || slide.mediaUrl != s.url) return null
    return cue()
}

/** How often the clock looks for the end of the clip. */
internal const val VIDEO_CLOCK_TICK_MS = 250L

/** How often a playing video's position is re-sent, so screens that drift or join late catch up. */
internal const val VIDEO_RESYNC_MS = 5_000L

/**
 * Runs the transport's clock for as long as it is collected: notices the clip
 * ending, and re-sends the playing video every [VIDEO_RESYNC_MS] so a TV that
 * drifted, or a browser that joined late, is brought back into step.
 *
 * Lives with the app shell rather than the Media screen, so a video keeps its
 * time when the operator goes to another tab mid-clip.
 *
 * @param resync What to call to re-send — the engine's `refreshVideo`.
 */
suspend fun runVideoClock(transport: VideoTransport, resync: () -> Unit) {
    var sinceResync = 0L
    while (true) {
        delay(VIDEO_CLOCK_TICK_MS)
        // checkEnded() tells the screens itself, through onChange.
        val ended = transport.checkEnded()
        sinceResync = when {
            ended || !transport.state.value.playing -> 0L
            sinceResync + VIDEO_CLOCK_TICK_MS >= VIDEO_RESYNC_MS -> {
                resync()
                0L
            }
            else -> sinceResync + VIDEO_CLOCK_TICK_MS
        }
    }
}
