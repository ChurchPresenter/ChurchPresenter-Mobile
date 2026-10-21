package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.display_language_1
import churchpresentermobile.composeapp.generated.resources.display_language_2
import churchpresentermobile.composeapp.generated.resources.display_language_2_position
import churchpresentermobile.composeapp.generated.resources.display_position
import churchpresentermobile.composeapp.generated.resources.display_position_above_lyrics
import churchpresentermobile.composeapp.generated.resources.display_position_above_verse
import churchpresentermobile.composeapp.generated.resources.display_position_below_lyrics
import churchpresentermobile.composeapp.generated.resources.display_position_below_verse
import churchpresentermobile.composeapp.generated.resources.display_reference_format
import churchpresentermobile.composeapp.generated.resources.display_second_scale
import churchpresentermobile.composeapp.generated.resources.display_section_language
import churchpresentermobile.composeapp.generated.resources.display_section_language_2
import churchpresentermobile.composeapp.generated.resources.display_section_reference
import churchpresentermobile.composeapp.generated.resources.display_section_visibility
import churchpresentermobile.composeapp.generated.resources.display_show_element
import churchpresentermobile.composeapp.generated.resources.display_show_second_language
import churchpresentermobile.composeapp.generated.resources.display_value_percent
import com.church.presenter.churchpresentermobile.model.DisplayTextTheme
import com.church.presenter.churchpresentermobile.model.ElementPosition
import com.church.presenter.churchpresentermobile.model.TextElement
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReferenceSection(editor: ElementEditor) {
    val display = editor.display
    DisplayOverline(stringResource(Res.string.display_section_reference))
    DisplayCard {
        LabeledRow(stringResource(Res.string.display_reference_format)) {
            DisplayDropdown(
                selectedIndex = REFERENCE_FORMATS.indexOf(display.referenceFormat),
                options = editor.referenceSamples,
                onSelect = { index -> editor.onDisplay { it.copy(referenceFormat = REFERENCE_FORMATS[index]) } },
                tag = DisplayTextTags.REFERENCE_FORMAT,
            )
        }
        CardDivider()
        LabeledRow(stringResource(Res.string.display_position)) {
            DisplayDropdown(
                selectedIndex = POSITIONS.indexOf(display.referencePosition),
                options = listOf(
                    stringResource(Res.string.display_position_above_verse),
                    stringResource(Res.string.display_position_below_verse),
                ),
                onSelect = { index -> editor.onDisplay { it.copy(referencePosition = POSITIONS[index]) } },
                tag = DisplayTextTags.REFERENCE_POSITION,
            )
        }
        CardDivider()
        ShowRow(editor)
    }
}

@Composable
internal fun VisibilitySection(editor: ElementEditor) {
    DisplayOverline(stringResource(Res.string.display_section_visibility))
    DisplayCard { ShowRow(editor) }
}

@Composable
private fun ShowRow(editor: ElementEditor) {
    LabeledRow(stringResource(Res.string.display_show_element)) {
        DisplaySwitch(
            checked = editor.style.visible,
            onCheckedChange = { on -> editor.onStyle { it.copy(visible = on) } },
            tag = DisplayTextTags.VISIBLE,
        )
    }
}

/** The second language: whether it shows, where, and how large beside the first. */
@Composable
internal fun LanguageSection(editor: ElementEditor, withTabs: Boolean) {
    val display = editor.display
    val second = editor.element == TextElement.SONG_LYRICS_SECOND
    DisplayOverline(
        stringResource(if (withTabs) Res.string.display_section_language else Res.string.display_section_language_2),
    )
    DisplayCard {
        if (withTabs) {
            Column(Modifier.padding(vertical = 10.dp)) {
                LanguageTabs(editor, compact = true)
            }
        }
        LabeledRow(stringResource(Res.string.display_show_second_language)) {
            DisplaySwitch(
                checked = display.showSecondLanguage,
                onCheckedChange = { on -> editor.onDisplay { it.copy(showSecondLanguage = on) } },
                tag = DisplayTextTags.SECOND_SHOW,
            )
        }
        CardDivider()
        val positionLabel = if (withTabs) Res.string.display_language_2_position else Res.string.display_position
        LabeledRow(stringResource(positionLabel)) {
            DisplayDropdown(
                selectedIndex = POSITIONS.indexOf(display.secondLanguagePosition),
                options = listOf(
                    stringResource(Res.string.display_position_above_lyrics),
                    stringResource(Res.string.display_position_below_lyrics),
                ),
                onSelect = { index -> editor.onDisplay { it.copy(secondLanguagePosition = POSITIONS[index]) } },
                tag = DisplayTextTags.SECOND_POSITION,
            )
        }
        if (second) {
            CardDivider()
            StackedRow(stringResource(Res.string.display_second_scale)) {
                val scale = display.secondLanguageScalePercent
                DisplayStepper(
                    value = stringResource(Res.string.display_value_percent, scale.toString()),
                    onDecrease = { editor.onDisplay { it.copy(secondLanguageScalePercent = scale - SCALE_STEP) } },
                    onIncrease = { editor.onDisplay { it.copy(secondLanguageScalePercent = scale + SCALE_STEP) } },
                    tag = DisplayTextTags.SECOND_SCALE,
                    canDecrease = scale - SCALE_STEP >= DisplayTextTheme.MIN_SECOND_SCALE,
                    canIncrease = scale + SCALE_STEP <= DisplayTextTheme.MAX_SECOND_SCALE,
                )
            }
        }
    }
}

/** Language 1 / Language 2: the two halves of a bilingual song's lyrics. */
@Composable
internal fun LanguageTabs(editor: ElementEditor, compact: Boolean = false) {
    DisplaySegments(
        options = listOf(stringResource(Res.string.display_language_1), stringResource(Res.string.display_language_2)),
        selectedIndex = if (editor.element == TextElement.SONG_LYRICS_SECOND) 1 else 0,
        onSelect = { index ->
            editor.onSelect(if (index == 1) TextElement.SONG_LYRICS_SECOND else TextElement.SONG_LYRICS)
        },
        tag = { DisplayTextTags.elementTab(if (it == 1) TextElement.SONG_LYRICS_SECOND else TextElement.SONG_LYRICS) },
        compact = compact,
    )
}

private val POSITIONS = listOf(ElementPosition.ABOVE, ElementPosition.BELOW)
private const val SCALE_STEP = 5
