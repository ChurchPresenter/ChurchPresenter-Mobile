package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.min

/**
 * A video the operator copied onto this device for standalone mode.
 *
 * The file itself sits in the app's own storage under [fileName], so it plays
 * with no network and survives the picker's temporary copy being cleaned up.
 * Everything the list shows — duration, resolution, size — is read once at
 * import, because probing a 1 GB file on every recomposition is not free.
 *
 * @property thumbName The still shown in the list and the preview, or null when
 *   the platform could not grab one.
 */
@Serializable
data class LocalVideo(
    val id: String,
    val title: String,
    val fileName: String,
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val thumbName: String? = null,
    val addedAt: Long = 0L,
) {
    /** "MP4", "MOV" — the container, from the extension. */
    val format: String get() = fileName.substringAfterLast('.', "").uppercase()
}

/** What the hall screen does when a video reaches its end. */
@Serializable
enum class VideoEndAction {
    /** Fade the video away and leave the screen black. */
    BLACK,

    /** Stop on the last frame. */
    HOLD,

    /** Start again from the top, for as long as it is up. */
    LOOP,
}

/** Where a video's sound comes out. */
@Serializable
enum class VideoSound {
    /** The hall screen — HDMI, a TV, a browser — carries the sound. */
    HALL,

    /** Only this phone plays it, e.g. into the desk from its headphone jack. */
    PHONE,

    /** Nowhere: the video plays silently. */
    OFF,
}

/**
 * How a video slide should be playing, carried inside the [Slide] so every
 * output — an attached screen, a TV app, a browser — follows the phone's
 * transport without a second channel.
 *
 * An output seeks only when it has drifted further than
 * [VIDEO_SYNC_TOLERANCE_MS] from [expectedPositionMs], and otherwise just plays
 * or pauses. That keeps a volume change from stuttering the
 * picture, and lets a display that joins late (or was blanked) land where the
 * phone says the video is.
 *
 * @property seq Bumped on every transport change, so a renderer can tell a new
 *   instruction from the same envelope replayed.
 */
@Serializable
data class VideoCue(
    val playing: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val seq: Long = 0L,
    val endAction: VideoEndAction = VideoEndAction.BLACK,
    val volume: Float = 1f,
    val muted: Boolean = false,
) {
    /** Whether the video has run out and the screen should be black, not on its last frame. */
    val endedToBlack: Boolean
        get() = !playing && endAction == VideoEndAction.BLACK &&
            durationMs > 0L && positionMs >= durationMs - VIDEO_END_TOLERANCE_MS

    /** The volume an output should actually play at, 0 when muted. */
    val effectiveVolume: Float get() = if (muted) 0f else volume.coerceIn(0f, 1f)
}

/**
 * How far a player may wander from where the phone says it should be before it
 * seeks. Half a second: closer than that and players would keep seeking, which
 * stutters; much looser and the phone and the hall screen visibly disagree.
 */
const val VIDEO_SYNC_TOLERANCE_MS = 500L

/** How often a playing player checks itself against the cue, between cues. */
const val VIDEO_DRIFT_CHECK_MS = 2_000L

/** How close to the end counts as "ended" — a player rarely reports the exact last millisecond. */
const val VIDEO_END_TOLERANCE_MS = 300L

/**
 * Where a player should be [elapsedMs] after it received [cue].
 *
 * The cue says where the video was when it was sent. A player that took a second
 * to open the clip, or a preview that appears a minute after Play was pressed,
 * must add the time since — measured on its own clock from when the cue arrived,
 * so a TV whose clock disagrees with the phone's still lands in the right place.
 */
fun expectedPositionMs(cue: VideoCue, elapsedMs: Long): Long {
    if (!cue.playing) return cue.positionMs
    val raw = cue.positionMs + elapsedMs.coerceAtLeast(0L)
    val duration = cue.durationMs
    return when {
        duration <= 0L -> raw
        cue.endAction == VideoEndAction.LOOP -> raw % duration
        else -> min(raw, duration)
    }
}

/**
 * Whether a player at [currentMs] should seek to [targetMs].
 *
 * Shared by every in-process player so the rule cannot differ between them;
 * the browser page encodes the same rule in `app.js`. A looping clip measures
 * the short way round, so 29.9s and 0.1s of a 30s loop count as in step.
 */
fun shouldSeek(currentMs: Long, targetMs: Long, cue: VideoCue): Boolean {
    val gap = abs(currentMs - targetMs)
    val loopGap = if (cue.endAction == VideoEndAction.LOOP && cue.durationMs > 0L) cue.durationMs - gap else gap
    return min(gap, loopGap) > VIDEO_SYNC_TOLERANCE_MS
}
