package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.standalone_accent_colour
import churchpresentermobile.composeapp.generated.resources.standalone_brand_line
import churchpresentermobile.composeapp.generated.resources.standalone_brand_line_hint
import churchpresentermobile.composeapp.generated.resources.standalone_font
import churchpresentermobile.composeapp.generated.resources.standalone_font_sans
import churchpresentermobile.composeapp.generated.resources.standalone_align
import churchpresentermobile.composeapp.generated.resources.standalone_align_center
import churchpresentermobile.composeapp.generated.resources.standalone_align_left
import churchpresentermobile.composeapp.generated.resources.standalone_align_right
import churchpresentermobile.composeapp.generated.resources.standalone_valign
import churchpresentermobile.composeapp.generated.resources.standalone_valign_bottom
import churchpresentermobile.composeapp.generated.resources.standalone_valign_middle
import churchpresentermobile.composeapp.generated.resources.standalone_valign_top
import churchpresentermobile.composeapp.generated.resources.standalone_font_serif
import churchpresentermobile.composeapp.generated.resources.standalone_gradient_bottom
import churchpresentermobile.composeapp.generated.resources.standalone_gradient_top
import churchpresentermobile.composeapp.generated.resources.standalone_look
import churchpresentermobile.composeapp.generated.resources.standalone_look_reset
import churchpresentermobile.composeapp.generated.resources.standalone_show_reference
import churchpresentermobile.composeapp.generated.resources.standalone_show_reference_bible
import churchpresentermobile.composeapp.generated.resources.standalone_show_reference_hint
import churchpresentermobile.composeapp.generated.resources.standalone_show_reference_other
import churchpresentermobile.composeapp.generated.resources.standalone_show_reference_songs
import churchpresentermobile.composeapp.generated.resources.standalone_show_chords
import churchpresentermobile.composeapp.generated.resources.standalone_show_chords_hint
import churchpresentermobile.composeapp.generated.resources.standalone_show_clock
import churchpresentermobile.composeapp.generated.resources.standalone_themes
import churchpresentermobile.composeapp.generated.resources.standalone_themes_saved
import churchpresentermobile.composeapp.generated.resources.standalone_theme_save
import churchpresentermobile.composeapp.generated.resources.standalone_theme_save_action
import churchpresentermobile.composeapp.generated.resources.standalone_theme_name
import churchpresentermobile.composeapp.generated.resources.standalone_theme_delete
import churchpresentermobile.composeapp.generated.resources.standalone_text_colour
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.model.SlideTextAlign
import com.church.presenter.churchpresentermobile.model.NamedTheme
import com.church.presenter.churchpresentermobile.model.SlideVerticalAlign
import com.church.presenter.churchpresentermobile.ui.OverlineRow
import com.church.presenter.churchpresentermobile.ui.SegmentedControl
import com.church.presenter.churchpresentermobile.ui.SettingsField
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Everything about how the audience screen looks.
 *
 * A sheet rather than more rows on the Present tab: that tab is the live surface during a
 * service, and Next, Blank and Live must not move further down the screen because someone once
 * wanted to change a colour.
 *
 * Every control writes straight through — the theme rides inside each slide, so the phone's own
 * output, an attached screen and any browser on the hosted page all follow the same edit without
 * being told separately. There is no Apply, and no preview to keep in step with.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LookSheet(
    theme: SlideTheme,
    onThemeChange: ((SlideTheme) -> SlideTheme) -> Unit,
    showChords: Boolean,
    onShowChordsChange: (Boolean) -> Unit,
    presets: List<NamedTheme>,
    savedThemes: List<NamedTheme>,
    onApplyTheme: (NamedTheme) -> Unit,
    onSaveTheme: (String) -> Unit,
    onDeleteTheme: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var namingTheme by remember { mutableStateOf(false) }
    var themeName by remember { mutableStateOf("") }
    val colors = LocalAppColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.space20, vertical = AppDimens.space8),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            Text(
                text = stringResource(Res.string.standalone_look),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            // A whole look in one tap. Setting up on a Sunday morning should not
            // mean assembling a readable screen out of six colour fields.
            OverlineRow(stringResource(Res.string.standalone_themes))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                presets.forEach { preset ->
                    ThemeChip(name = preset.name, onClick = { onApplyTheme(preset) })
                }
            }

            if (savedThemes.isNotEmpty()) {
                OverlineRow(stringResource(Res.string.standalone_themes_saved))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                    savedThemes.forEach { saved ->
                        ThemeChip(
                            name = saved.name,
                            onClick = { onApplyTheme(saved) },
                            onDelete = { onDeleteTheme(saved.name) },
                        )
                    }
                }
            }

            if (namingTheme) {
                SettingsField(
                    label = stringResource(Res.string.standalone_theme_name),
                    value = themeName,
                    onValueChange = { themeName = it },
                    placeholder = "",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                )
                Text(
                    text = stringResource(Res.string.standalone_theme_save_action),
                    color = colors.accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        onSaveTheme(themeName)
                        themeName = ""
                        namingTheme = false
                    },
                )
            } else {
                Text(
                    text = stringResource(Res.string.standalone_theme_save),
                    color = colors.accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { namingTheme = true },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                ColorField(
                    label = stringResource(Res.string.standalone_gradient_top),
                    value = theme.gradientTop,
                    onValueChange = { hex -> onThemeChange { it.copy(gradientTop = hex) } },
                    modifier = Modifier.weight(1f),
                )
                ColorField(
                    label = stringResource(Res.string.standalone_gradient_bottom),
                    value = theme.gradientBottom,
                    onValueChange = { hex -> onThemeChange { it.copy(gradientBottom = hex) } },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                ColorField(
                    label = stringResource(Res.string.standalone_text_colour),
                    value = theme.textColor,
                    onValueChange = { hex -> onThemeChange { it.copy(textColor = hex) } },
                    modifier = Modifier.weight(1f),
                )
                ColorField(
                    label = stringResource(Res.string.standalone_accent_colour),
                    value = theme.accentColor,
                    onValueChange = { hex -> onThemeChange { it.copy(accentColor = hex) } },
                    modifier = Modifier.weight(1f),
                )
            }

            OverlineRow(stringResource(Res.string.standalone_font))
            SegmentedControl(
                options = listOf(
                    stringResource(Res.string.standalone_font_serif),
                    stringResource(Res.string.standalone_font_sans),
                ),
                selectedIndex = FONTS.indexOf(theme.font).coerceAtLeast(0),
                onSelect = { index -> onThemeChange { it.copy(font = FONTS[index]) } },
            )

            OverlineRow(stringResource(Res.string.standalone_align))
            SegmentedControl(
                options = listOf(
                    stringResource(Res.string.standalone_align_left),
                    stringResource(Res.string.standalone_align_center),
                    stringResource(Res.string.standalone_align_right),
                ),
                selectedIndex = TEXT_ALIGNS.indexOf(theme.textAlign).coerceAtLeast(0),
                onSelect = { index -> onThemeChange { it.copy(textAlign = TEXT_ALIGNS[index]) } },
            )

            OverlineRow(stringResource(Res.string.standalone_valign))
            SegmentedControl(
                options = listOf(
                    stringResource(Res.string.standalone_valign_top),
                    stringResource(Res.string.standalone_valign_middle),
                    stringResource(Res.string.standalone_valign_bottom),
                ),
                selectedIndex = VERTICAL_ALIGNS.indexOf(theme.verticalAlign).coerceAtLeast(0),
                onSelect = { index -> onThemeChange { it.copy(verticalAlign = VERTICAL_ALIGNS[index]) } },
            )

            SettingsField(
                label = stringResource(Res.string.standalone_brand_line),
                value = theme.brandLine.orEmpty(),
                onValueChange = { typed ->
                    // Blank means "no corner line" rather than an empty one taking up space.
                    onThemeChange { it.copy(brandLine = typed.takeIf { line -> line.isNotBlank() }) }
                },
                placeholder = stringResource(Res.string.standalone_brand_line_hint),
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            )

            // Asked separately: a church that wants no heading over a hymn
            // usually still wants the chapter and verse over scripture.
            ToggleRow(
                label = stringResource(Res.string.standalone_show_reference_songs),
                hint = stringResource(Res.string.standalone_show_reference_hint),
                checked = theme.showSongReference,
                onCheckedChange = { on -> onThemeChange { it.copy(showSongReference = on) } },
            )

            ToggleRow(
                label = stringResource(Res.string.standalone_show_reference_bible),
                checked = theme.showBibleReference,
                onCheckedChange = { on -> onThemeChange { it.copy(showBibleReference = on) } },
            )

            ToggleRow(
                label = stringResource(Res.string.standalone_show_reference_other),
                checked = theme.showOtherReference,
                onCheckedChange = { on -> onThemeChange { it.copy(showOtherReference = on) } },
            )

            ToggleRow(
                label = stringResource(Res.string.standalone_show_clock),
                checked = theme.showClock,
                onCheckedChange = { on -> onThemeChange { it.copy(showClock = on) } },
            )

            // Part of the theme, so one switch reaches the phone, an attached
            // screen and any browser watching — the theme rides inside every
            // slide, so none of them has to be told separately.
            ToggleRow(
                label = stringResource(Res.string.standalone_show_chords),
                hint = stringResource(Res.string.standalone_show_chords_hint),
                checked = showChords,
                onCheckedChange = onShowChordsChange,
            )

            Text(
                text = stringResource(Res.string.standalone_look_reset),
                color = colors.accent,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(top = AppDimens.space8)
                    .clickable { onThemeChange { SlideTheme() } },
            )

            Box(Modifier.padding(bottom = AppDimens.space16))
        }
    }
}

/** A named look. Tapping adopts it; the cross removes a saved one. */
@Composable
private fun ThemeChip(name: String, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .padding(bottom = AppDimens.space8)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = name, color = colors.text, fontSize = 13.sp)
        if (onDelete != null) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.standalone_theme_delete),
                tint = colors.muted,
                modifier = Modifier.size(14.dp).clickable(onClick = onDelete),
            )
        }
    }
}

/** A label, an optional line saying what it does, and the switch. */
@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hint: String? = null,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, color = colors.text, fontSize = 14.sp)
            hint?.let { Text(text = it, color = colors.muted, fontSize = 11.sp, lineHeight = 15.sp) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val FONTS = listOf(SlideFont.SERIF, SlideFont.SANS)

/** Option order for the alignment controls — must match their labels. */
private val TEXT_ALIGNS = listOf(SlideTextAlign.LEFT, SlideTextAlign.CENTER, SlideTextAlign.RIGHT)
private val VERTICAL_ALIGNS =
    listOf(SlideVerticalAlign.TOP, SlideVerticalAlign.MIDDLE, SlideVerticalAlign.BOTTOM)
