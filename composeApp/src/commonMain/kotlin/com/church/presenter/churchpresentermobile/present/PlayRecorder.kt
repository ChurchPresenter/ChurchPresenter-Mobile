package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.library.PlayLogRepository
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

private const val TAG = "PlayRecorder"

/**
 * Writes a play to the [PlayLogRepository] when — and only when — a song or a
 * verse is actually in front of the congregation.
 *
 * "Projected" means all of the following at once: the engine is showing the
 * slide (not blanked, not held off-air), and at least one output is attached
 * and has a screen on the end of it. A song loaded and stepped through with
 * nothing plugged in is a rehearsal, and a CCLI report that counted it would
 * over-report the licence.
 *
 * Counting follows the desktop: a song is one play however many sections are
 * shown, and every verse put on the screen is one play. What separates one
 * play of a song from the next is *something else* being projected in between
 * — another song, a verse, a photo, a cleared screen. Toggling blank, taking
 * the output off-air and back, restyling, or a display dropping and returning
 * all leave the same thing on the screen and count nothing.
 *
 * A screen that attaches while a song is already up records it then: the
 * moment it reaches a screen is the moment it was projected.
 *
 * The engine's slide is a state, not a stream of events, so a slide replaced
 * before the recorder gets to look — a chapter loaded at verse 1 and moved to
 * verse 5 in the same breath — is never seen here. That is the right answer:
 * it was never in front of anyone either.
 *
 * @param slides The slide as the engine is projecting it, overrides applied.
 * @param outputs Sink status, for whether anything is attached.
 */
class PlayRecorder(
    private val slides: StateFlow<Slide>,
    private val outputs: StateFlow<List<SinkStatus>>,
    private val log: PlayLogRepository,
) {
    /** The content most recently seen on a screen, so the same thing is not counted twice. */
    private var lastShown: String? = null

    /**
     * Watches the engine and the outputs until cancelled. Call from a scope
     * that lives as long as the app does.
     */
    suspend fun run() {
        combine(slides, outputs) { slide, sinks -> slide to sinks.any { it.isOnScreen } }
            .distinctUntilChanged()
            .collect { (slide, onScreen) -> observe(slide, onScreen) }
    }

    /**
     * One observation of the engine and the outputs. Pulled out of [run] so the
     * rule is a plain function of plain values.
     */
    internal fun observe(slide: Slide, onScreen: Boolean) {
        if (!onScreen || slide.isHidden) return
        val key = contentKey(slide)
        if (key == lastShown) return
        lastShown = key
        when {
            slide.songCredit != null -> log.recordSong(slide.songCredit)
            slide.verseCredit != null -> log.recordVerse(slide.verseCredit)
            else -> Logger.d(TAG, "observe — ${slide.kind} on screen, nothing to credit")
        }
    }

    /**
     * What is on the screen, for telling one play from the next.
     *
     * A song's sections share a key, so stepping through them is one play; a
     * chapter's verses each have their own, so each verse shown is one. Anything
     * else is keyed by what it shows, so a photo between two plays of the same
     * song is what makes them two.
     */
    private fun contentKey(slide: Slide): String = when {
        slide.songCredit != null -> "song:${slide.songCredit.key}"
        slide.verseCredit != null -> "verse:${slide.verseCredit.key}"
        slide.kind == SlideKind.BLANK -> "blank"
        else -> "${slide.kind}:${slide.sourceId ?: slide.mediaUrl ?: slide.backdropUrl ?: slide.body}"
    }
}

/**
 * True when this sink has a real screen on the end of it.
 *
 * Attached is not enough: the web sink is attached the moment its server is
 * up, and reports the browsers actually watching as its client count. A page
 * nobody has opened is not a projection.
 */
internal val SinkStatus.isOnScreen: Boolean
    get() = isAttached && clientCount > 0
