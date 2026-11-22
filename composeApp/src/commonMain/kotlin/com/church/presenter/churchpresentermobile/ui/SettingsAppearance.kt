package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_device_name_hint
import churchpresentermobile.composeapp.generated.resources.settings_device_name_label
import churchpresentermobile.composeapp.generated.resources.settings_device_name_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_device_preview
import churchpresentermobile.composeapp.generated.resources.settings_device_preview_allow
import churchpresentermobile.composeapp.generated.resources.settings_device_preview_body
import churchpresentermobile.composeapp.generated.resources.settings_device_preview_deny
import churchpresentermobile.composeapp.generated.resources.settings_device_preview_title
import churchpresentermobile.composeapp.generated.resources.settings_display_name_hint
import churchpresentermobile.composeapp.generated.resources.settings_display_name_label
import churchpresentermobile.composeapp.generated.resources.settings_display_name_placeholder
import churchpresentermobile.composeapp.generated.resources.settings_theme_dark
import churchpresentermobile.composeapp.generated.resources.settings_theme_footnote
import churchpresentermobile.composeapp.generated.resources.settings_theme_light
import churchpresentermobile.composeapp.generated.resources.settings_theme_system
import churchpresentermobile.composeapp.generated.resources.settings_theme_title
import churchpresentermobile.composeapp.generated.resources.settings_this_device
import com.church.presenter.churchpresentermobile.deviceName
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppColors
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.ui.theme.appColorsFor
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceSection(draft: ServerDraft, edits: ServerDraftEdits) {
    val colors = LocalAppColors.current
    Column {
        GroupLabel(stringResource(Res.string.settings_this_device))
        LookCard(padding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 10.dp)) {
            LabelledField(
                label = stringResource(Res.string.settings_device_name_label),
                value = draft.customDeviceName,
                onValueChange = edits.onCustomDeviceName,
                placeholder = deviceName().ifBlank { stringResource(Res.string.settings_device_name_placeholder) },
                hint = stringResource(Res.string.settings_device_name_hint),
                fieldModifier = Modifier.testTag(UiTags.SETTINGS_DEVICE_NAME),
            )
            LabelledField(
                label = stringResource(Res.string.settings_display_name_label),
                value = draft.displayName,
                onValueChange = edits.onDisplayName,
                placeholder = stringResource(Res.string.settings_display_name_placeholder),
                hint = stringResource(Res.string.settings_display_name_hint),
                fieldModifier = Modifier.testTag(UiTags.SETTINGS_DISPLAY_NAME),
            )
        }
    }
    val previewName = draft.customDeviceName.ifBlank { deviceName() }
    if (previewName.isNotBlank()) {
        val tint = colors.greekAccent
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surfaceStrong)
                .border(1.dp, colors.border, shape)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Eyebrow(stringResource(Res.string.settings_device_preview), tint)
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconTile(Icons.Outlined.Smartphone, tint, size = 40.dp, radius = 12.dp, iconSize = 20.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.settings_device_preview_title, previewName),
                        color = colors.text,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(Res.string.settings_device_preview_body),
                        color = colors.muted,
                        fontSize = 13.5.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                PreviewChip(stringResource(Res.string.settings_device_preview_deny), filled = null)
                PreviewChip(stringResource(Res.string.settings_device_preview_allow), filled = tint)
            }
        }
    }
}

@Composable
internal fun PreviewChip(label: String, filled: Color?) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(filled ?: colors.surface)
            .border(1.dp, if (filled == null) colors.border else Color.Transparent, shape)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (filled == null) colors.text else colors.onAccent,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun AppearanceSection(themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit) {
    val colors = LocalAppColors.current
    val options = listOf(
        ThemeMode.SYSTEM to Res.string.settings_theme_system,
        ThemeMode.LIGHT to Res.string.settings_theme_light,
        ThemeMode.DARK to Res.string.settings_theme_dark,
    )
    Column {
        GroupLabel(stringResource(Res.string.settings_theme_title))
        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEachIndexed { index, (mode, label) ->
                val selected = mode == themeMode
                val shape = RoundedCornerShape(16.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .background(colors.surface)
                        .border(2.dp, if (selected) colors.accent else colors.border, shape)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onThemeMode(mode) })
                        .testTag(UiTags.settingsTheme(index))
                        .padding(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                    ) {
                        if (mode != ThemeMode.DARK) ThemeSketch(appColorsFor(isDark = false))
                        if (mode != ThemeMode.LIGHT) ThemeSketch(appColorsFor(isDark = true))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 2.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(label), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (selected) RadioMark(selected = true, tint = colors.accent)
                    }
                }
            }
        }
        Footnote(stringResource(Res.string.settings_theme_footnote), modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
internal fun RowScope.ThemeSketch(theme: AppColors) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(theme.background)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth(SKETCH_TITLE_WIDTH)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(theme.text),
        )
        repeat(2) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(theme.surfaceStrong)
                    .border(1.dp, theme.border, RoundedCornerShape(6.dp)),
            )
        }
        Box(
            Modifier
                .fillMaxWidth(SKETCH_ACCENT_WIDTH)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(theme.accent),
        )
    }
}

private const val SKETCH_TITLE_WIDTH = 0.6f
private const val SKETCH_ACCENT_WIDTH = 0.4f
