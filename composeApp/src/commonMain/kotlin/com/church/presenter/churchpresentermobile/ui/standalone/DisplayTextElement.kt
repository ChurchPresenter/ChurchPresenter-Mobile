package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideTextAlign
import com.church.presenter.churchpresentermobile.model.TextElement
import com.church.presenter.churchpresentermobile.model.TextElementStyle

/**
 * One styled run of text: its box behind, its outline, its shadow, and — while editing — its
 * highlight and its tap.
 */
@Composable
internal fun Element(
    element: TextElement,
    text: String,
    style: TextElementStyle,
    frame: ElementFrame,
    lineHeight: Float = ELEMENT_LINE_HEIGHT,
    autoFit: Boolean = false,
    modifier: Modifier = Modifier,
    /**
     * Give the element a line of its own, and place it on that line by its own alignment. The
     * box round the words is only as wide as they are, so without this a reference set to the
     * left would sit wherever the verse above it put it. False inside the song header, where the
     * number and title share a line.
     */
    ownLine: Boolean = true,
) {
    if (ownLine) {
        Box(modifier.fillMaxWidth(), contentAlignment = style.align.toBoxAlignment()) {
            StyledRun(element, text, style, frame, lineHeight, autoFit, Modifier)
        }
    } else {
        StyledRun(element, text, style, frame, lineHeight, autoFit, modifier)
    }
}

@Suppress("LongParameterList") // Element's own parameters, passed straight through.
@Composable
private fun StyledRun(
    element: TextElement,
    text: String,
    style: TextElementStyle,
    frame: ElementFrame,
    lineHeight: Float,
    autoFit: Boolean,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val size = (style.sizePt * frame.scale).sp
    val textStyle = style.textStyle(size, lineHeight, pxPerUnit = density.density * frame.scale)
    val content = remember(text, style) { style.annotated(style.cased(text)) }
    val autoSize = if (autoFit) {
        TextAutoSize.StepBased(minFontSize = size * AUTO_FIT_FLOOR, maxFontSize = size)
    } else {
        null
    }
    val outlinePx = with(density) { (style.outlineWidthPx * frame.scale).dp.toPx() } * 2

    Box(
        modifier = modifier
            .testTag(DisplayTextTags.element(element))
            .highlighted(element == frame.highlight, frame.scale)
            .tappable(element, frame.onTap)
            .backing(style, frame.scale),
    ) {
        if (style.outline) {
            // Drawn underneath the fill, with the stroke straddling the glyph's edge: doubling the
            // width leaves the requested width showing outside the letters.
            BasicText(
                text = content,
                style = textStyle.copy(
                    color = parseHexColor(style.outlineColor, Color.Black),
                    drawStyle = Stroke(width = outlinePx),
                ),
                autoSize = autoSize,
            )
        }
        BasicText(
            text = content,
            style = if (style.outline) textStyle.copy(shadow = null) else textStyle,
            autoSize = autoSize,
        )
    }
}

/** Absolute, like the text alignment: the side of the hall screen does not mirror. */
private fun SlideTextAlign.toBoxAlignment(): Alignment = when (this) {
    SlideTextAlign.LEFT -> AbsoluteAlignment.CenterLeft
    SlideTextAlign.RIGHT -> AbsoluteAlignment.CenterRight
    SlideTextAlign.CENTER -> Alignment.Center
}

/** [pxPerUnit] turns a 1080p length into this output's pixels, which is what a shadow is drawn in. */
private fun TextElementStyle.textStyle(size: TextUnit, lineHeight: Float, pxPerUnit: Float): TextStyle = TextStyle(
    fontSize = size,
    lineHeight = size * lineHeight,
    fontFamily = font.toFamily(),
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    textDecoration = if (underline) TextDecoration.Underline else null,
    letterSpacing = (letterSpacingPercent / PERCENT).em,
    color = parseHexColor(color, Color.White),
    textAlign = align.toTextAlign(),
    shadow = if (shadow) {
        Shadow(
            color = parseHexColor(shadowColor, Color.Black).copy(alpha = SHADOW_ALPHA),
            offset = Offset(0f, shadowOffsetPx * pxPerUnit),
            blurRadius = shadowBlurPx * pxPerUnit,
        )
    } else {
        null
    },
)

/**
 * The words with the extra word spacing applied to their spaces.
 *
 * Compose has no word-spacing of its own; widening only the space characters is the same thing
 * CSS `word-spacing` does, so the browser page and the phone agree.
 */
private fun TextElementStyle.annotated(text: String): AnnotatedString {
    if (wordSpacingPx == 0) return AnnotatedString(text)
    val extra = (letterSpacingPercent / PERCENT + wordSpacingPx.toFloat() / sizePt).em
    return buildAnnotatedString {
        text.forEach { char ->
            if (char == ' ') {
                pushStyle(SpanStyle(letterSpacing = extra))
                append(char)
                pop()
            } else {
                append(char)
            }
        }
    }
}

private fun Modifier.backing(style: TextElementStyle, scale: Float): Modifier {
    if (!style.backing) return this
    val colour = parseHexColor(style.backingColor, Color.Black)
        .copy(alpha = style.backingOpacity.coerceIn(0, PERCENT_INT) / PERCENT)
    return background(colour, RoundedCornerShape((BACKING_RADIUS_PX * scale).dp))
        .padding(horizontal = (BACKING_PAD_H_PX * scale).dp, vertical = (BACKING_PAD_V_PX * scale).dp)
}

/** A dashed box round the element being edited, drawn outside it so the text does not move. */
private fun Modifier.highlighted(on: Boolean, scale: Float): Modifier {
    if (!on) return this
    return drawBehind {
        val inset = (HIGHLIGHT_INSET_PX * scale).dp.toPx()
        val dash = HIGHLIGHT_DASH.dp.toPx()
        drawRoundRect(
            color = HIGHLIGHT,
            topLeft = Offset(-inset, -inset),
            size = size.copy(width = size.width + inset * 2, height = size.height + inset * 2),
            cornerRadius = CornerRadius(HIGHLIGHT_RADIUS.dp.toPx()),
            style = Stroke(
                width = HIGHLIGHT_STROKE.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
            ),
        )
    }
}

@Composable
private fun Modifier.tappable(element: TextElement, onTap: ((TextElement) -> Unit)?): Modifier {
    if (onTap == null) return this
    return clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap(element) }
}

internal fun SlideFont.toFamily(): FontFamily = when (this) {
    SlideFont.SERIF -> FontFamily.Serif
    SlideFont.SANS -> FontFamily.SansSerif
}

/** Absolute, never mirrored — see [StandaloneOutputScreen]'s note on the side of the hall screen. */
private fun SlideTextAlign.toTextAlign(): TextAlign = when (this) {
    SlideTextAlign.LEFT -> TextAlign.Left
    SlideTextAlign.RIGHT -> TextAlign.Right
    SlideTextAlign.CENTER -> TextAlign.Center
}

private const val ELEMENT_LINE_HEIGHT = 1.2f
private const val AUTO_FIT_FLOOR = 0.45f
private const val BACKING_RADIUS_PX = 10f
private const val BACKING_PAD_H_PX = 18f
private const val BACKING_PAD_V_PX = 6f
private const val HIGHLIGHT_INSET_PX = 12f
private const val HIGHLIGHT_DASH = 4f
private const val HIGHLIGHT_RADIUS = 6f
private const val HIGHLIGHT_STROKE = 1.5f
private const val SHADOW_ALPHA = 0.7f
private val HIGHLIGHT = Color(0xFF86EFAC)
private const val PERCENT = 100f
private const val PERCENT_INT = 100
