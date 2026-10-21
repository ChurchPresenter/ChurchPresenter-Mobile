package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.display_align_horizontal
import churchpresentermobile.composeapp.generated.resources.display_align_vertical
import churchpresentermobile.composeapp.generated.resources.display_backing
import churchpresentermobile.composeapp.generated.resources.display_backing_hint
import churchpresentermobile.composeapp.generated.resources.display_backing_opacity
import churchpresentermobile.composeapp.generated.resources.display_bold
import churchpresentermobile.composeapp.generated.resources.display_case_lower
import churchpresentermobile.composeapp.generated.resources.display_case_original
import churchpresentermobile.composeapp.generated.resources.display_case_title
import churchpresentermobile.composeapp.generated.resources.display_case_upper
import churchpresentermobile.composeapp.generated.resources.display_italic
import churchpresentermobile.composeapp.generated.resources.display_letter_case
import churchpresentermobile.composeapp.generated.resources.display_letter_spacing
import churchpresentermobile.composeapp.generated.resources.display_outline
import churchpresentermobile.composeapp.generated.resources.display_outline_width
import churchpresentermobile.composeapp.generated.resources.display_section_alignment
import churchpresentermobile.composeapp.generated.resources.display_section_color
import churchpresentermobile.composeapp.generated.resources.display_section_effects
import churchpresentermobile.composeapp.generated.resources.display_section_spacing
import churchpresentermobile.composeapp.generated.resources.display_section_type
import churchpresentermobile.composeapp.generated.resources.display_shadow
import churchpresentermobile.composeapp.generated.resources.display_shadow_blur
import churchpresentermobile.composeapp.generated.resources.display_shadow_offset
import churchpresentermobile.composeapp.generated.resources.display_size
import churchpresentermobile.composeapp.generated.resources.display_size_hint
import churchpresentermobile.composeapp.generated.resources.display_style
import churchpresentermobile.composeapp.generated.resources.display_underline
import churchpresentermobile.composeapp.generated.resources.display_value_percent
import churchpresentermobile.composeapp.generated.resources.display_value_px
import churchpresentermobile.composeapp.generated.resources.display_word_spacing
import churchpresentermobile.composeapp.generated.resources.standalone_align_center
import churchpresentermobile.composeapp.generated.resources.standalone_align_left
import churchpresentermobile.composeapp.generated.resources.standalone_align_right
import churchpresentermobile.composeapp.generated.resources.standalone_font
import churchpresentermobile.composeapp.generated.resources.standalone_font_sans
import churchpresentermobile.composeapp.generated.resources.standalone_font_serif
import churchpresentermobile.composeapp.generated.resources.standalone_valign_bottom
import churchpresentermobile.composeapp.generated.resources.standalone_valign_middle
import churchpresentermobile.composeapp.generated.resources.standalone_valign_top
import com.church.presenter.churchpresentermobile.model.DisplayTextTheme
import com.church.presenter.churchpresentermobile.model.LetterCase
import com.church.presenter.churchpresentermobile.model.ReferenceFormat
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideTextAlign
import com.church.presenter.churchpresentermobile.model.SlideVerticalAlign
import com.church.presenter.churchpresentermobile.model.TextElement
import com.church.presenter.churchpresentermobile.model.TextElementStyle
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/** Which of an element's colours the picker is changing. */
internal enum class ColorTarget { FILL, OUTLINE, BACKING, SHADOW }

/** What the controls can do: change the element, change the whole, move on, or pick a colour. */
internal class EditorActions(
    val onStyle: ((TextElementStyle) -> TextElementStyle) -> Unit,
    val onDisplay: ((DisplayTextTheme) -> DisplayTextTheme) -> Unit,
    val onSelect: (TextElement) -> Unit,
    val onPickColor: (ColorTarget) -> Unit,
)

/**
 * Everything the controls for one element read and write, gathered so each section takes one
 * value instead of six.
 *
 * @param referenceSamples What each [ReferenceFormat] looks like, for the format menu.
 */
internal class ElementEditor(
    val element: TextElement,
    val display: DisplayTextTheme,
    val referenceSamples: List<String>,
    private val actions: EditorActions,
) {
    val style: TextElementStyle get() = display.style(element)
    val onStyle: ((TextElementStyle) -> TextElementStyle) -> Unit get() = actions.onStyle
    val onDisplay: ((DisplayTextTheme) -> DisplayTextTheme) -> Unit get() = actions.onDisplay
    val onSelect: (TextElement) -> Unit get() = actions.onSelect
    val onPickColor: (ColorTarget) -> Unit get() = actions.onPickColor
}

/**
 * The controls for the selected element, top to bottom.
 *
 * @param inlineLanguage Put the Language 1 / 2 switch inside a Language card, as the tablet's
 *   wider column does, rather than leaving it to the phone's row of tabs above.
 */
@Composable
internal fun ElementControls(editor: ElementEditor, inlineLanguage: Boolean, modifier: Modifier = Modifier) {
    val element = editor.element
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            element == TextElement.BIBLE_REFERENCE -> ReferenceSection(editor)
            element == TextElement.SONG_TITLE || element == TextElement.SONG_NUMBER -> VisibilitySection(editor)
            inlineLanguage && (element == TextElement.SONG_LYRICS || element == TextElement.SONG_LYRICS_SECOND) ->
                LanguageSection(editor, withTabs = true)
            element == TextElement.SONG_LYRICS_SECOND -> LanguageSection(editor, withTabs = false)
        }
        TypeSection(editor)
        AlignmentSection(editor)
        ColorSection(editor)
        EffectsSection(editor)
        SpacingSection(editor)
    }
}

@Composable
private fun TypeSection(editor: ElementEditor) {
    val style = editor.style
    DisplayOverline(stringResource(Res.string.display_section_type))
    DisplayCard {
        LabeledRow(stringResource(Res.string.standalone_font)) {
            DisplayDropdown(
                selectedIndex = FONTS.indexOf(style.font),
                options = listOf(
                    stringResource(Res.string.standalone_font_serif),
                    stringResource(Res.string.standalone_font_sans),
                ),
                onSelect = { index -> editor.onStyle { it.copy(font = FONTS[index]) } },
                tag = DisplayTextTags.FONT,
            )
        }
        // The second language's size follows the first's, by the scale in its own card.
        if (editor.element != TextElement.SONG_LYRICS_SECOND) {
            CardDivider()
            LabeledRow(stringResource(Res.string.display_size), stringResource(Res.string.display_size_hint)) {
                DisplayStepper(
                    value = style.sizePt.toString(),
                    onDecrease = { editor.onStyle { it.copy(sizePt = clampSize(it.sizePt - SIZE_STEP)) } },
                    onIncrease = { editor.onStyle { it.copy(sizePt = clampSize(it.sizePt + SIZE_STEP)) } },
                    tag = DisplayTextTags.SIZE,
                    canDecrease = style.sizePt > TextElementStyle.MIN_SIZE_PT,
                    canIncrease = style.sizePt < TextElementStyle.MAX_SIZE_PT,
                )
            }
        }
        CardDivider()
        StackedRow(stringResource(Res.string.display_style)) {
            val labels = listOf(
                stringResource(Res.string.display_bold),
                stringResource(Res.string.display_italic),
                stringResource(Res.string.display_underline),
            )
            TileRow(
                count = 3,
                isOn = { listOf(style.bold, style.italic, style.underline)[it] },
                onClick = { index ->
                    editor.onStyle {
                        when (index) {
                            0 -> it.copy(bold = !it.bold)
                            1 -> it.copy(italic = !it.italic)
                            else -> it.copy(underline = !it.underline)
                        }
                    }
                },
                tag = { listOf(DisplayTextTags.BOLD, DisplayTextTags.ITALIC, DisplayTextTags.UNDERLINE)[it] },
            ) { index, on -> TileIcon(STYLE_ICONS[index], labels[index], on) }
        }
        CardDivider()
        StackedRow(stringResource(Res.string.display_letter_case)) {
            DisplaySegments(
                options = listOf(
                    stringResource(Res.string.display_case_title),
                    stringResource(Res.string.display_case_upper),
                    stringResource(Res.string.display_case_lower),
                    stringResource(Res.string.display_case_original),
                ),
                selectedIndex = CASES.indexOf(style.letterCase),
                onSelect = { index -> editor.onStyle { it.copy(letterCase = CASES[index]) } },
                tag = { DisplayTextTags.letterCase(it) },
                compact = true,
            )
        }
    }
}

@Composable
private fun AlignmentSection(editor: ElementEditor) {
    val style = editor.style
    DisplayOverline(stringResource(Res.string.display_section_alignment))
    DisplayCard {
        StackedRow(stringResource(Res.string.display_align_horizontal)) {
            val labels = listOf(
                stringResource(Res.string.standalone_align_left),
                stringResource(Res.string.standalone_align_center),
                stringResource(Res.string.standalone_align_right),
            )
            TileRow(
                count = 3,
                isOn = { ALIGNS[it] == style.align },
                onClick = { index -> editor.onStyle { it.copy(align = ALIGNS[index]) } },
                tag = { DisplayTextTags.align(it) },
            ) { index, on -> TileIcon(ALIGN_ICONS[index], labels[index], on) }
        }
        // Only the words themselves have a place down the screen; everything else sits beside them.
        if (editor.element == TextElement.BIBLE_VERSE || editor.element == TextElement.SONG_LYRICS) {
            CardDivider()
            StackedRow(stringResource(Res.string.display_align_vertical)) {
                val labels = listOf(
                    stringResource(Res.string.standalone_valign_top),
                    stringResource(Res.string.standalone_valign_middle),
                    stringResource(Res.string.standalone_valign_bottom),
                )
                TileRow(
                    count = 3,
                    isOn = { VERTICAL_ALIGNS[it] == style.verticalAlign },
                    onClick = { index -> editor.onStyle { it.copy(verticalAlign = VERTICAL_ALIGNS[index]) } },
                    tag = { DisplayTextTags.verticalAlign(it) },
                ) { index, on -> TileIcon(VERTICAL_ICONS[index], labels[index], on) }
            }
        }
    }
}

@Composable
private fun ColorSection(editor: ElementEditor) {
    val style = editor.style
    DisplayOverline(stringResource(Res.string.display_section_color))
    DisplayCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QUICK_COLORS.forEachIndexed { index, hex ->
                ColorChip(
                    hex = hex,
                    selected = hex.equals(style.color, ignoreCase = true),
                    onClick = { editor.onStyle { it.copy(color = hex) } },
                    tag = DisplayTextTags.swatch(index),
                )
            }
            CustomColorChip(onClick = { editor.onPickColor(ColorTarget.FILL) }, tag = DisplayTextTags.COLOR_CUSTOM)
        }
    }
}

@Composable
private fun EffectsSection(editor: ElementEditor) {
    val style = editor.style
    DisplayOverline(stringResource(Res.string.display_section_effects))
    DisplayCard {
        LabeledRow(stringResource(Res.string.display_outline)) {
            ColorChip(
                style.outlineColor,
                { editor.onPickColor(ColorTarget.OUTLINE) },
                DisplayTextTags.OUTLINE_COLOR,
                size = 26.dp,
            )
            DisplaySwitch(style.outline, { on -> editor.onStyle { it.copy(outline = on) } }, DisplayTextTags.OUTLINE)
        }
        if (style.outline) {
            SliderRow(
                label = stringResource(Res.string.display_outline_width),
                valueText = stringResource(Res.string.display_value_px, oneDecimal(style.outlineWidthPx)),
                value = style.outlineWidthPx,
                range = OUTLINE_RANGE,
                onChange = { width -> editor.onStyle { it.copy(outlineWidthPx = (width * 2).roundToInt() / 2f) } },
                tag = DisplayTextTags.OUTLINE_WIDTH,
            )
        }
        CardDivider()
        LabeledRow(stringResource(Res.string.display_backing), stringResource(Res.string.display_backing_hint)) {
            ColorChip(
                style.backingColor,
                { editor.onPickColor(ColorTarget.BACKING) },
                DisplayTextTags.BACKING_COLOR,
                size = 26.dp,
            )
            DisplaySwitch(style.backing, { on -> editor.onStyle { it.copy(backing = on) } }, DisplayTextTags.BACKING)
        }
        if (style.backing) {
            SliderRow(
                label = stringResource(Res.string.display_backing_opacity),
                valueText = stringResource(Res.string.display_value_percent, style.backingOpacity.toString()),
                value = style.backingOpacity.toFloat(),
                range = 0f..PERCENT,
                onChange = { value -> editor.onStyle { it.copy(backingOpacity = value.roundToInt()) } },
                tag = DisplayTextTags.BACKING_OPACITY,
            )
        }
        CardDivider()
        LabeledRow(stringResource(Res.string.display_shadow)) {
            ColorChip(
                style.shadowColor,
                { editor.onPickColor(ColorTarget.SHADOW) },
                "${DisplayTextTags.SHADOW}:color",
                size = 26.dp,
            )
            DisplaySwitch(style.shadow, { on -> editor.onStyle { it.copy(shadow = on) } }, DisplayTextTags.SHADOW)
        }
        if (style.shadow) {
            SliderRow(
                label = stringResource(Res.string.display_shadow_blur),
                valueText = stringResource(Res.string.display_value_px, style.shadowBlurPx.toString()),
                value = style.shadowBlurPx.toFloat(),
                range = SHADOW_BLUR_RANGE,
                onChange = { value -> editor.onStyle { it.copy(shadowBlurPx = value.roundToInt()) } },
                tag = DisplayTextTags.SHADOW_BLUR,
            )
            SliderRow(
                label = stringResource(Res.string.display_shadow_offset),
                valueText = stringResource(Res.string.display_value_px, style.shadowOffsetPx.toString()),
                value = style.shadowOffsetPx.toFloat(),
                range = SHADOW_OFFSET_RANGE,
                onChange = { value -> editor.onStyle { it.copy(shadowOffsetPx = value.roundToInt()) } },
                tag = DisplayTextTags.SHADOW_OFFSET,
            )
        }
    }
}

@Composable
private fun SpacingSection(editor: ElementEditor) {
    val style = editor.style
    DisplayOverline(stringResource(Res.string.display_section_spacing))
    DisplayCard {
        SliderRow(
            label = stringResource(Res.string.display_letter_spacing),
            valueText = stringResource(Res.string.display_value_percent, signed(style.letterSpacingPercent)),
            value = style.letterSpacingPercent.toFloat(),
            range = LETTER_SPACING_RANGE,
            onChange = { value -> editor.onStyle { it.copy(letterSpacingPercent = value.roundToInt()) } },
            tag = DisplayTextTags.LETTER_SPACING,
        )
        SliderRow(
            label = stringResource(Res.string.display_word_spacing),
            valueText = stringResource(Res.string.display_value_px, style.wordSpacingPx.toString()),
            value = style.wordSpacingPx.toFloat(),
            range = WORD_SPACING_RANGE,
            onChange = { value -> editor.onStyle { it.copy(wordSpacingPx = value.roundToInt()) } },
            tag = DisplayTextTags.WORD_SPACING,
        )
    }
}

private fun clampSize(size: Int): Int = size.coerceIn(TextElementStyle.MIN_SIZE_PT, TextElementStyle.MAX_SIZE_PT)

/** "+1", "0", "-3": a change from the default reads as a change. */
internal fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

/** 1.5 → "1.5", 2.0 → "2.0". Common code has no String.format. */
internal fun oneDecimal(value: Float): String {
    val tenths = (value * TENTHS).roundToInt()
    return "${tenths / TENTHS.toInt()}.${abs(tenths % TENTHS.toInt())}"
}

private const val SIZE_STEP = 2

private const val PERCENT = 100f
private const val TENTHS = 10f
private val OUTLINE_RANGE = 0.5f..6f
private val SHADOW_BLUR_RANGE = 0f..30f
private val SHADOW_OFFSET_RANGE = 0f..15f
private val LETTER_SPACING_RANGE = -5f..30f
private val WORD_SPACING_RANGE = 0f..40f

/** The colours a church reaches for first; the rainbow chip beside them opens everything else. */
internal val QUICK_COLORS = listOf("#FFFFFF", "#FDE68A", "#86EFAC", "#BAE6FD", "#F9A8D4", "#000000")

/** Option orders — each must match its labels. */
private val FONTS = listOf(SlideFont.SERIF, SlideFont.SANS)
private val CASES = listOf(LetterCase.TITLE, LetterCase.UPPER, LetterCase.LOWER, LetterCase.ORIGINAL)
private val ALIGNS = listOf(SlideTextAlign.LEFT, SlideTextAlign.CENTER, SlideTextAlign.RIGHT)
private val VERTICAL_ALIGNS = listOf(SlideVerticalAlign.TOP, SlideVerticalAlign.MIDDLE, SlideVerticalAlign.BOTTOM)

internal val REFERENCE_FORMATS = listOf(ReferenceFormat.BOOK_CHAPTER_VERSE, ReferenceFormat.WITH_VERSION)
private val STYLE_ICONS = listOf(Icons.Filled.FormatBold, Icons.Filled.FormatItalic, Icons.Filled.FormatUnderlined)
private val ALIGN_ICONS = listOf(
    Icons.AutoMirrored.Filled.FormatAlignLeft,
    Icons.Filled.FormatAlignCenter,
    Icons.AutoMirrored.Filled.FormatAlignRight,
)
private val VERTICAL_ICONS = listOf(
    Icons.Filled.VerticalAlignTop,
    Icons.Filled.VerticalAlignCenter,
    Icons.Filled.VerticalAlignBottom,
)
