package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideFont
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** What the room is seeing, in one word. */
enum class ScreenState { LIVE, BLANK, OFF }

/**
 * What is on the audience screen, boiled down for the system's own surfaces —
 * on Android, the notification shade, where it reads like a music player's:
 * a picture of the slide where the album art goes, the song where the track
 * title goes, and the words on screen under it.
 *
 * Plain values only, so a platform can draw it without knowing anything about
 * decks or engines, and so what it says can be tested without one.
 *
 * @property title The song, passage or deck — "Amazing Grace", "John 3".
 * @property words The first lines on screen now, or empty when there are none.
 * @property position "2/5", or null for a single slide.
 * @property backgroundTop The backdrop's colours as `#RRGGBB`, top then bottom.
 * @property serif Whether the words are set in the serif face.
 */
data class NowShowing(
    val title: String,
    val words: String,
    val position: String?,
    val state: ScreenState,
    val backgroundTop: String,
    val backgroundBottom: String,
    val textColor: String,
    val serif: Boolean,
)

/** How many lines of a slide the shade has room for before it is a paragraph. */
private const val WORD_LINES = 2

/**
 * What is showing, or null when nothing is loaded — then there is nothing to
 * picture, and the indicator falls back to saying the screens are connected.
 */
internal fun nowShowing(deckTitle: String, slide: Slide): NowShowing? {
    if (slide.total == 0 && slide.body.isBlank() && deckTitle.isBlank()) return null
    val words = slide.body.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .take(WORD_LINES)
        .joinToString(" / ")
    val black = slide.backdrop == SlideBackdrop.BLACK
    return NowShowing(
        title = deckTitle.ifBlank { slide.reference.orEmpty() },
        words = words,
        position = if (slide.total > 1) "${slide.index + 1}/${slide.total}" else null,
        state = when {
            !slide.isLive -> ScreenState.OFF
            slide.isBlank -> ScreenState.BLANK
            else -> ScreenState.LIVE
        },
        backgroundTop = if (black) BLACK else slide.theme.gradientTop,
        backgroundBottom = if (black) BLACK else slide.theme.gradientBottom,
        textColor = slide.theme.textColor,
        serif = slide.theme.font == SlideFont.SERIF,
    )
}

/**
 * Hands [report] what is showing every time it changes, for as long as it is
 * collected. Repeats are dropped: redrawing a notification for a slide it
 * already shows only flickers it.
 */
internal suspend fun reportNowShowing(engine: StandaloneEngine, report: (NowShowing?) -> Unit) {
    combine(engine.deck, engine.currentSlide) { deck, slide -> nowShowing(deck.title, slide) }
        .distinctUntilChanged()
        .collect(report)
}

private const val BLACK = "#000000"
