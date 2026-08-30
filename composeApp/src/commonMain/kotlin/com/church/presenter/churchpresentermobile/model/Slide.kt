package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** Protocol version of [SlideEnvelope]. Bump only on a breaking wire change. */
const val SLIDE_PROTOCOL_VERSION: Int = 1

/** What kind of content a [Slide] carries. Drives font and layout choices. */
@Serializable
enum class SlideKind {
    SONG,
    BIBLE,
    ANNOUNCEMENT,
    IMAGE,
    /** A web page, shown live on the output rather than captured. */
    WEB,
    /** A video played on the output. */
    VIDEO,
    LOGO,
    BLANK,
}

/** Discrete text-size ladder. Chosen by the operator, applied by the renderer. */
@Serializable
enum class SlideTextSize {
    SMALL,
    MEDIUM,
    LARGE,
}

/** What sits behind the slide text. */
@Serializable
enum class SlideBackdrop {
    GRADIENT,
    IMAGE,
    BLACK,
}

/** Typeface family for the slide body. */
@Serializable
enum class SlideFont {
    SERIF,
    SANS,
}

/** Where the words sit across the screen. Mirrors the desktop's horizontalAlignment. */
@Serializable
enum class SlideTextAlign {
    LEFT,
    CENTER,
    RIGHT,
}

/** Where the words sit down the screen. Mirrors the desktop's verticalAlignment. */
@Serializable
enum class SlideVerticalAlign {
    TOP,
    MIDDLE,
    BOTTOM,
}

/**
 * Presentation styling shared by every slide in a service.
 *
 * @property font Typeface family for the body text.
 * @property textColor Body text colour as `#RRGGBB`.
 * @property accentColor Colour used for the reference/footer line.
 * @property brandLine Optional church name rendered in the top-left corner.
 * @property showClock Whether the output shows a wall clock in the top-right corner.
 * @property showSongReference Whether a song's title and section line is shown.
 * @property showBibleReference Whether a Bible reference is shown. Kept separate
 *   from the song one because most churches always want "John 3:16" on screen
 *   while wanting nothing above a hymn's words.
 * @property showOtherReference Whether anything else — a notice, a photo — shows
 *   its line.
 * @property showChords Whether chords are drawn with the words, on every output
 *   as well as the phone.
 * @property textAlign Where the words sit across the screen.
 * @property verticalAlign Where the words sit down the screen.
 *
 * New properties must carry a default: the theme travels inside every slide and
 * is persisted as JSON, so a defaulted field keeps an older payload — and an
 * older saved theme — readable without touching [SLIDE_PROTOCOL_VERSION].
 */
@Serializable
data class SlideTheme(
    val font: SlideFont = SlideFont.SERIF,
    val textColor: String = "#FFFFFF",
    val accentColor: String = "#7C5CFF",
    val brandLine: String? = null,
    val showClock: Boolean = true,
    val showSongReference: Boolean = true,
    val showBibleReference: Boolean = true,
    val showOtherReference: Boolean = true,
    val showChords: Boolean = false,
    val gradientTop: String = DEFAULT_GRADIENT_TOP,
    val gradientBottom: String = DEFAULT_GRADIENT_BOTTOM,
    val textAlign: SlideTextAlign = SlideTextAlign.CENTER,
    val verticalAlign: SlideVerticalAlign = SlideVerticalAlign.MIDDLE,
)

/**
 * Whether this slide's reference line should be drawn, by what kind of thing it is.
 *
 * Songs, Bible and everything else are asked separately: a church that wants no
 * heading over a hymn usually still wants the chapter and verse over scripture.
 */
fun Slide.showsReference(): Boolean = when (kind) {
    SlideKind.SONG -> theme.showSongReference
    SlideKind.BIBLE -> theme.showBibleReference
    else -> theme.showOtherReference
}

/** The wash every renderer falls back to, and what the pickers start on. */
const val DEFAULT_GRADIENT_TOP: String = "#2A1D5E"
const val DEFAULT_GRADIENT_BOTTOM: String = "#05060D"

/**
 * One projected screen of content — everything a renderer needs, with no
 * lookups back into a library or a server.
 *
 * This is deliberately self-contained: the phone-hosted web page and the
 * external-display window both receive a [Slide] and render it directly, so a
 * display that connects mid-service can be brought fully up to date by
 * replaying a single value.
 *
 * @property body The slide text. Line breaks are significant and preserved.
 * @property reference Attribution line, e.g. `JOHN 3:16` or `AMAZING GRACE · VERSE 2`.
 * @property footer Optional secondary line, e.g. a copyright or CCLI number.
 * @property backdropUrl Image URL — only meaningful when [backdrop] is [SlideBackdrop.IMAGE].
 * @property mediaUrl The page or video this slide *is*, for [SlideKind.WEB] and
 *   [SlideKind.VIDEO]. Separate from [backdropUrl] because a backdrop sits
 *   behind text, while this replaces it: the renderer hands the URL to a browser
 *   view or a player instead of drawing a slide.
 * @property isBlank True while the operator has blacked the screen out.
 * @property isLive False while output is held back from the audience screen.
 * @property sourceId Identifier of the originating item (song number, `John:3:16`, announcement id).
 * @property index Zero-based position within the owning [SlideDeck].
 * @property showReference Whether the line naming the song section or Bible verse is shown.
 * @property gradientTop Top colour of the gradient backdrop as `#RRGGBB`.
 * @property gradientBottom Bottom colour of the gradient backdrop as `#RRGGBB`.
 * @property total Number of slides in the owning [SlideDeck].
 */
@Serializable
data class Slide(
    val kind: SlideKind = SlideKind.BLANK,
    val body: String = "",
    /**
     * The same words with their chord markup still in them, when the source had
     * any. [body] is always the clean words, so anything that has no chord
     * rendering keeps working untouched.
     */
    val chordBody: String? = null,
    val reference: String? = null,
    val footer: String? = null,
    val textSize: SlideTextSize = SlideTextSize.MEDIUM,
    val backdrop: SlideBackdrop = SlideBackdrop.GRADIENT,
    val backdropUrl: String? = null,
    val mediaUrl: String? = null,
    val isBlank: Boolean = false,
    val isLive: Boolean = true,
    val theme: SlideTheme = SlideTheme(),
    val sourceId: String? = null,
    val index: Int = 0,
    val total: Int = 0,
) {
    /** True when the renderer should show nothing but the backdrop. */
    val isHidden: Boolean get() = isBlank || !isLive

    companion object {
        /** The slide shown before anything has been projected. */
        val BLANK: Slide = Slide(kind = SlideKind.BLANK, isBlank = true)
    }
}

/**
 * An ordered set of slides the operator steps through — a song's sections, a
 * chapter's verses, or a list of announcements.
 *
 * @property title Header shown above the section list on the controller,
 *   e.g. `Sections — 42 Amazing Grace`.
 */
@Serializable
data class SlideDeck(
    val kind: SlideKind = SlideKind.BLANK,
    val title: String = "",
    val slides: List<Slide> = emptyList(),
) {
    val isEmpty: Boolean get() = slides.isEmpty()

    /** Returns the slide at [index], or `null` when the deck is empty or the index is out of range. */
    fun slideAt(index: Int): Slide? = slides.getOrNull(index)

    /** Clamps [index] into the deck's valid range. Returns 0 for an empty deck. */
    fun clampIndex(index: Int): Int =
        if (slides.isEmpty()) 0 else index.coerceIn(0, slides.size - 1)

    companion object {
        val EMPTY: SlideDeck = SlideDeck()
    }
}

/** What a [SlideEnvelope] is asking the renderer to do. */
@Serializable
enum class SlideMessageType {
    SLIDE,
    BLANK,
    CLEAR,
    THEME,
    PING,
    BYE,
}

/**
 * The wire format broadcast to every output sink, and the exact JSON pushed
 * over the phone-hosted WebSocket.
 *
 * @property v Protocol version, so a stale display page can refuse politely.
 * @property rev Monotonically increasing revision. Receivers drop any frame
 *   whose [rev] is not greater than the last one applied, which makes the
 *   protocol safe to replay and immune to out-of-order delivery.
 */
@Serializable
data class SlideEnvelope(
    val v: Int = SLIDE_PROTOCOL_VERSION,
    val type: SlideMessageType = SlideMessageType.SLIDE,
    val rev: Long = 0L,
    val slide: Slide? = null,
) {
    companion object {
        val INITIAL: SlideEnvelope = SlideEnvelope(
            type = SlideMessageType.CLEAR,
            rev = 0L,
            slide = Slide.BLANK,
        )
    }
}
