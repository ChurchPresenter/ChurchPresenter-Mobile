package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** How a text element's letters are cased on the output. */
@Serializable
enum class LetterCase {
    /** "Amazing Grace" — every word capitalised. */
    TITLE,
    UPPER,
    LOWER,

    /** The words exactly as written. */
    ORIGINAL,
}

/** Which side of the main words a secondary element sits on. */
@Serializable
enum class ElementPosition {
    ABOVE,
    BELOW,
}

/** How a Bible reference is written on the output. */
@Serializable
enum class ReferenceFormat {
    /** "John 3:16". */
    BOOK_CHAPTER_VERSE,

    /** "John 3:16 · KJV" — with the translation's name, when it is known. */
    WITH_VERSION,
}

/**
 * The look of one piece of text on the audience screen.
 *
 * Every length is in pixels on a 1080-pixel-high output, the unit the desktop's own settings
 * use, so "72" means the same thing on the phone's preview strip, a TV and a browser: each
 * renderer multiplies by its height over 1080.
 *
 * @property sizePt Type size at 1080p.
 * @property letterSpacingPercent Extra space between letters, as a percentage of the type size.
 * @property wordSpacingPx Extra space between words, at 1080p.
 * @property backingOpacity How opaque the box behind the words is, 0–100.
 * @property visible Whether the element is drawn at all. Always true for the main words; a
 *   reference, a title or a number can be switched off.
 */
@Serializable
data class TextElementStyle(
    val font: SlideFont = SlideFont.SERIF,
    val sizePt: Int = DEFAULT_SIZE_PT,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val letterCase: LetterCase = LetterCase.ORIGINAL,
    val align: SlideTextAlign = SlideTextAlign.CENTER,
    val verticalAlign: SlideVerticalAlign = SlideVerticalAlign.MIDDLE,
    val color: String = "#FFFFFF",
    val outline: Boolean = false,
    val outlineColor: String = "#000000",
    val outlineWidthPx: Float = DEFAULT_OUTLINE_PX,
    val backing: Boolean = false,
    val backingColor: String = "#000000",
    val backingOpacity: Int = DEFAULT_BACKING_OPACITY,
    val shadow: Boolean = false,
    val shadowColor: String = "#000000",
    val shadowBlurPx: Int = DEFAULT_SHADOW_BLUR_PX,
    val shadowOffsetPx: Int = DEFAULT_SHADOW_OFFSET_PX,
    val letterSpacingPercent: Int = 0,
    val wordSpacingPx: Int = 0,
    val visible: Boolean = true,
) {
    /** [text] in this element's letter case. */
    fun cased(text: String): String = when (letterCase) {
        LetterCase.ORIGINAL -> text
        LetterCase.UPPER -> text.uppercase()
        LetterCase.LOWER -> text.lowercase()
        LetterCase.TITLE -> text.split(' ').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercaseChar() }
        }
    }

    companion object {
        const val DEFAULT_SIZE_PT = 72
        const val MIN_SIZE_PT = 12
        const val MAX_SIZE_PT = 240
        const val DEFAULT_OUTLINE_PX = 1.5f
        const val DEFAULT_BACKING_OPACITY = 45
        const val DEFAULT_SHADOW_BLUR_PX = 10
        const val DEFAULT_SHADOW_OFFSET_PX = 3
    }
}

/** Every text element Display text styles, in the order its element list shows them. */
@Serializable
enum class TextElement(val kind: SlideKind) {
    BIBLE_VERSE(SlideKind.BIBLE),
    BIBLE_REFERENCE(SlideKind.BIBLE),
    SONG_LYRICS(SlideKind.SONG),
    SONG_LYRICS_SECOND(SlideKind.SONG),
    SONG_TITLE(SlideKind.SONG),
    SONG_NUMBER(SlideKind.SONG),
    ;

    /** The words themselves rather than something placed around them. */
    val isBody: Boolean get() = this == BIBLE_VERSE || this == SONG_LYRICS || this == SONG_LYRICS_SECOND

    /** Whether the operator may hide this element. The words themselves always show. */
    val canHide: Boolean get() = this == BIBLE_REFERENCE || this == SONG_TITLE || this == SONG_NUMBER
}

/**
 * Per-element styling for Bible and song slides on the standalone output.
 *
 * Absent from a [SlideTheme] until the operator first saves a style in Display text; while it
 * is absent every renderer draws exactly what it drew before the feature existed, from the
 * theme's single font and colour. That is what keeps an older saved theme — and an older
 * display page — looking the way it did.
 *
 * @property secondLanguageScalePercent The second language's type size as a share of the
 *   first's, so changing one size keeps the pair in proportion.
 */
@Serializable
data class DisplayTextTheme(
    val bibleVerse: TextElementStyle = TextElementStyle(bold = true, shadow = true),
    val bibleReference: TextElementStyle = REFERENCE_DEFAULT,
    val referenceFormat: ReferenceFormat = ReferenceFormat.WITH_VERSION,
    val referencePosition: ElementPosition = ElementPosition.BELOW,
    val songLyrics: TextElementStyle = TextElementStyle(font = SlideFont.SANS, bold = true, shadow = true),
    val songLyricsSecond: TextElementStyle = TextElementStyle(
        font = SlideFont.SANS,
        italic = true,
        color = "#BAE6FD",
        shadow = true,
    ),
    val showSecondLanguage: Boolean = true,
    val secondLanguagePosition: ElementPosition = ElementPosition.BELOW,
    val secondLanguageScalePercent: Int = DEFAULT_SECOND_SCALE,
    val songTitle: TextElementStyle = TITLE_DEFAULT,
    val songNumber: TextElementStyle = NUMBER_DEFAULT,
) {
    fun style(element: TextElement): TextElementStyle = when (element) {
        TextElement.BIBLE_VERSE -> bibleVerse
        TextElement.BIBLE_REFERENCE -> bibleReference
        TextElement.SONG_LYRICS -> songLyrics
        TextElement.SONG_LYRICS_SECOND -> songLyricsSecond
        TextElement.SONG_TITLE -> songTitle
        TextElement.SONG_NUMBER -> songNumber
    }

    fun withStyle(element: TextElement, style: TextElementStyle): DisplayTextTheme = when (element) {
        TextElement.BIBLE_VERSE -> copy(bibleVerse = style)
        TextElement.BIBLE_REFERENCE -> copy(bibleReference = style)
        TextElement.SONG_LYRICS -> copy(songLyrics = style)
        TextElement.SONG_LYRICS_SECOND -> copy(songLyricsSecond = style)
        TextElement.SONG_TITLE -> copy(songTitle = style)
        TextElement.SONG_NUMBER -> copy(songNumber = style)
    }

    /** The second language's size, from the first's and the scale. */
    val secondLanguageSizePt: Int
        get() = (songLyrics.sizePt * secondLanguageScalePercent / PERCENT).coerceAtLeast(TextElementStyle.MIN_SIZE_PT)

    /** This theme with [element] back at the default the editor starts from. */
    fun resetting(element: TextElement, seed: DisplayTextTheme): DisplayTextTheme {
        val reset = withStyle(element, seed.style(element))
        return when (element) {
            TextElement.BIBLE_REFERENCE -> reset.copy(
                referenceFormat = seed.referenceFormat,
                referencePosition = seed.referencePosition,
            )
            TextElement.SONG_LYRICS_SECOND -> reset.copy(
                showSecondLanguage = seed.showSecondLanguage,
                secondLanguagePosition = seed.secondLanguagePosition,
                secondLanguageScalePercent = seed.secondLanguageScalePercent,
            )
            else -> reset
        }
    }

    companion object {
        const val DEFAULT_SECOND_SCALE = 70
        const val MIN_SECOND_SCALE = 40
        const val MAX_SECOND_SCALE = 120
        private const val PERCENT = 100

        private val REFERENCE_DEFAULT = TextElementStyle(
            font = SlideFont.SANS,
            sizePt = 34,
            bold = true,
            letterCase = LetterCase.UPPER,
            color = "#FDE68A",
            backing = true,
            backingOpacity = 60,
            letterSpacingPercent = 14,
        )
        private val TITLE_DEFAULT = TextElementStyle(
            font = SlideFont.SANS,
            sizePt = 32,
            bold = true,
            align = SlideTextAlign.LEFT,
        )
        private val NUMBER_DEFAULT = TextElementStyle(
            font = SlideFont.SANS,
            sizePt = 32,
            bold = true,
            align = SlideTextAlign.LEFT,
            color = "#052E16",
            backing = true,
            backingColor = "#86EFAC",
            backingOpacity = 100,
        )

        /**
         * Where the editor starts for a theme that has never had Display text saved: the
         * theme's own font, colour and alignment, so the first open shows the screen as it
         * already looks rather than a stranger's defaults. [textSize] is the operator's size
         * ladder, which lives on the engine rather than in the theme.
         */
        fun seededFrom(theme: SlideTheme, textSize: SlideTextSize = SlideTextSize.MEDIUM): DisplayTextTheme {
            val base = DisplayTextTheme()
            val size = when (textSize) {
                SlideTextSize.SMALL -> SMALL_SEED_PT
                SlideTextSize.MEDIUM -> MEDIUM_SEED_PT
                SlideTextSize.LARGE -> LARGE_SEED_PT
            }
            fun body(style: TextElementStyle) = style.copy(
                font = theme.font,
                sizePt = size,
                color = theme.textColor,
                align = theme.textAlign,
                verticalAlign = theme.verticalAlign,
            )
            return base.copy(
                bibleVerse = body(base.bibleVerse),
                songLyrics = body(base.songLyrics),
                songLyricsSecond = body(base.songLyricsSecond),
                bibleReference = base.bibleReference.copy(
                    color = theme.accentColor,
                    align = theme.textAlign,
                    visible = theme.showBibleReference,
                ),
                songTitle = base.songTitle.copy(visible = theme.showSongReference),
            )
        }

        // What each rung of the old size ladder came to on a 1080p screen, so the first save
        // changes nothing the congregation can see.
        private const val SMALL_SEED_PT = 68
        private const val MEDIUM_SEED_PT = 88
        private const val LARGE_SEED_PT = 112
    }
}

/**
 * This theme as a slide drawn the old way — a notice, a web page's caption — should read it.
 *
 * Once Display text is in use, the theme's own font, colour and alignment are no longer edited
 * anywhere, so they come from the Bible verse's style instead: the closest thing to "body text"
 * the operator has chosen. Without Display text the theme is returned as it is.
 */
fun SlideTheme.legacyLook(): SlideTheme {
    val verse = display?.bibleVerse ?: return this
    return copy(
        font = verse.font,
        textColor = verse.color,
        textAlign = verse.align,
        verticalAlign = verse.verticalAlign,
    )
}

/**
 * The reference as [format] writes it.
 *
 * The translation's name rides on the slide's verse credit; a desktop's text carries none, and
 * then the plain reference is all there is to show.
 */
fun Slide.formattedReference(format: ReferenceFormat): String? {
    val base = reference?.takeIf { it.isNotBlank() } ?: return null
    val version = verseCredit?.bibleName?.takeIf { it.isNotBlank() }
    return if (format == ReferenceFormat.WITH_VERSION && version != null) "$base · $version" else base
}
