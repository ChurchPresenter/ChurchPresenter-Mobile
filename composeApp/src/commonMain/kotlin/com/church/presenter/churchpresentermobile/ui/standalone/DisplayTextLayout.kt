package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.model.DisplayTextTheme
import com.church.presenter.churchpresentermobile.model.ElementPosition
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideTextAlign
import com.church.presenter.churchpresentermobile.model.SlideVerticalAlign
import com.church.presenter.churchpresentermobile.model.TextElement
import com.church.presenter.churchpresentermobile.model.TextElementStyle
import com.church.presenter.churchpresentermobile.model.displayBody
import com.church.presenter.churchpresentermobile.model.formattedReference

/**
 * The words of a Bible or song slide, styled element by element from Display text.
 *
 * Every length in a [TextElementStyle] is at 1080p, so each is multiplied by [scale] — this
 * output's height over 1080 — and the phone's preview strip, a TV and the browser page all show
 * the same proportions.
 *
 * @param highlight The element being edited, outlined so the operator can see what a control
 *   changes. Null on a real output.
 * @param onElementTap Makes each element tappable, to jump to its settings. Null on a real output,
 *   where nothing is tapped.
 */
@Composable
internal fun DisplayTextContent(
    slide: Slide,
    display: DisplayTextTheme,
    scale: Float,
    highlight: TextElement?,
    onElementTap: ((TextElement) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val frame = ElementFrame(scale, highlight, onElementTap)
    Column(modifier) {
        val content = Modifier.fillMaxWidth().weight(1f)
        when (slide.kind) {
            SlideKind.SONG -> SongText(slide, display, frame, content)
            else -> BibleText(slide, display, frame, content)
        }
        // The copyright line is not styled — it is a licence condition, not part of the look —
        // but it must still be there: a song projected without it is a song shown unlicensed.
        slide.footer?.takeIf { it.isNotBlank() }?.let { footer ->
            BasicText(
                text = footer,
                style = TextStyle(
                    fontSize = (FOOTER_PX * scale).sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White.copy(alpha = FOOTER_ALPHA),
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = (FOOTER_GAP_PX * scale).dp),
            )
        }
    }
}

/** What every element on one slide is drawn with, gathered so it is passed as one value. */
internal class ElementFrame(
    val scale: Float,
    val highlight: TextElement?,
    val onTap: ((TextElement) -> Unit)?,
)

@Composable
private fun BibleText(slide: Slide, display: DisplayTextTheme, frame: ElementFrame, modifier: Modifier) {
    val verse = display.bibleVerse
    val referenceStyle = display.bibleReference
    val reference = slide.formattedReference(display.referenceFormat)
        ?.takeIf { referenceStyle.visible }
    val referenceGap = (REFERENCE_GAP_PX * frame.scale).dp
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = verse.verticalAlign.toArrangement(),
        horizontalAlignment = verse.align.toHorizontal(),
    ) {
        if (reference != null && display.referencePosition == ElementPosition.ABOVE) {
            Element(TextElement.BIBLE_REFERENCE, reference, referenceStyle, frame)
            Box(Modifier.padding(top = referenceGap))
        }
        BodyElement(TextElement.BIBLE_VERSE, slide.displayBody(), verse, frame, slide.theme.autoFitText)
        if (reference != null && display.referencePosition == ElementPosition.BELOW) {
            Box(Modifier.padding(top = referenceGap))
            Element(TextElement.BIBLE_REFERENCE, reference, referenceStyle, frame)
        }
    }
}

@Composable
private fun SongText(slide: Slide, display: DisplayTextTheme, frame: ElementFrame, modifier: Modifier) {
    val lyrics = display.songLyrics
    val second = slide.secondaryBody
        ?.takeIf { display.showSecondLanguage && it.isNotBlank() }
        ?.let { if (slide.theme.ignoreLineBreaks) it.joinLines() else it }
    val secondStyle = display.songLyricsSecond.copy(sizePt = display.secondLanguageSizePt)
    val languageGap = (LANGUAGE_GAP_PX * frame.scale).dp
    Column(modifier = modifier.fillMaxSize()) {
        SongHeader(slide, display, frame)
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = lyrics.verticalAlign.toArrangement(),
            horizontalAlignment = lyrics.align.toHorizontal(),
        ) {
            if (second != null && display.secondLanguagePosition == ElementPosition.ABOVE) {
                Element(TextElement.SONG_LYRICS_SECOND, second, secondStyle, frame, BODY_LINE_HEIGHT)
                Box(Modifier.padding(top = languageGap))
            }
            val chordText = slide.chordBody?.takeIf { slide.theme.showChords }
            if (chordText != null) {
                OutputChordChart(
                    text = chordText,
                    bodySize = (lyrics.sizePt * frame.scale).sp,
                    textColor = parseHexColor(lyrics.color, Color.White),
                    chordColor = parseHexColor(slide.theme.accentColor, Color.White),
                    fontFamily = lyrics.font.toFamily(),
                    horizontalAlignment = lyrics.align.toHorizontal(),
                )
            } else {
                BodyElement(TextElement.SONG_LYRICS, slide.displayBody(), lyrics, frame, slide.theme.autoFitText)
            }
            if (second != null && display.secondLanguagePosition == ElementPosition.BELOW) {
                Box(Modifier.padding(top = languageGap))
                Element(TextElement.SONG_LYRICS_SECOND, second, secondStyle, frame, BODY_LINE_HEIGHT)
            }
        }
    }
}

/** The number badge and the title, on one line at the top of a song slide. */
@Composable
private fun SongHeader(slide: Slide, display: DisplayTextTheme, frame: ElementFrame) {
    val number = slide.songNumber?.takeIf { it.isNotBlank() && display.songNumber.visible }
    val title = slide.songTitle?.takeIf { it.isNotBlank() && display.songTitle.visible }
    if (number == null && title == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = (HEADER_GAP_PX * frame.scale).dp),
        horizontalArrangement = Arrangement.spacedBy(
            (HEADER_SPACING_PX * frame.scale).dp,
            display.songTitle.align.toHorizontal(),
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        number?.let { Element(TextElement.SONG_NUMBER, it, display.songNumber, frame, ownLine = false) }
        title?.let { Element(TextElement.SONG_TITLE, it, display.songTitle, frame, ownLine = false) }
    }
}

/** The main words, which alone may shrink to fit the screen. */
@Composable
private fun ColumnScope.BodyElement(
    element: TextElement,
    text: String,
    style: TextElementStyle,
    frame: ElementFrame,
    autoFit: Boolean,
) {
    if (text.isBlank()) return
    Element(
        element = element,
        text = text,
        style = style,
        frame = frame,
        lineHeight = BODY_LINE_HEIGHT,
        autoFit = autoFit,
        modifier = if (autoFit) Modifier.weight(1f, fill = false) else Modifier,
    )
}

private fun String.joinLines(): String = replace(Regex("\\s*\\n\\s*"), " ").trim()

private fun SlideTextAlign.toHorizontal(): Alignment.Horizontal = when (this) {
    SlideTextAlign.LEFT -> AbsoluteAlignment.Left
    SlideTextAlign.RIGHT -> AbsoluteAlignment.Right
    SlideTextAlign.CENTER -> Alignment.CenterHorizontally
}

private fun SlideVerticalAlign.toArrangement(): Arrangement.Vertical = when (this) {
    SlideVerticalAlign.TOP -> Arrangement.Top
    SlideVerticalAlign.BOTTOM -> Arrangement.Bottom
    SlideVerticalAlign.MIDDLE -> Arrangement.Center
}

/**
 * This output's pixels-per-1080p-pixel, from its height.
 *
 * In dp, the unit the renderer lays out in, so a style's "72" is 72/1080 of the screen's height
 * whatever the screen's density.
 */
internal fun displayScale(height: Dp): Float = height.value / OUTPUT_HEIGHT_PX

private const val OUTPUT_HEIGHT_PX = 1080f

private const val BODY_LINE_HEIGHT = 1.32f

private const val REFERENCE_GAP_PX = 28f
private const val LANGUAGE_GAP_PX = 18f
private const val HEADER_GAP_PX = 24f
private const val HEADER_SPACING_PX = 18f

private const val FOOTER_PX = 22f
private const val FOOTER_GAP_PX = 16f
private const val FOOTER_ALPHA = 0.55f
