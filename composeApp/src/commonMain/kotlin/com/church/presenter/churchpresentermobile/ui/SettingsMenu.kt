package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.mode_section_title
import churchpresentermobile.composeapp.generated.resources.settings_connection_tap_to_fix
import churchpresentermobile.composeapp.generated.resources.settings_done
import churchpresentermobile.composeapp.generated.resources.settings_title
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsSectionList(
    sections: List<SettingsSection>,
    current: SettingsSection,
    appMode: AppMode,
    onSelect: (SettingsSection) -> Unit,
    onDone: () -> Unit,
    status: StatusUiState?,
    address: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.surface)
            .statusBarsPadding()
            .verticalScrollbar(scroll)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        MenuTitle(
            size = 28,
            onDone = onDone,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 18.dp),
        )
        MenuTop(sections, appMode, status, address, onSelect, modeSelected = current == SettingsSection.MODE)
        groupsOf(sections).forEach { (group, rows) ->
            Eyebrow(
                stringResource(group.label),
                colors.muted,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                rows.forEach { section ->
                    RailRow(
                        section = section,
                        tint = colors.groupTint(group, appMode),
                        selected = section == current,
                        onClick = { onSelect(section) },
                        modifier = Modifier.testTag(UiTags.settingsSection(section)),
                    )
                }
            }
        }
    }
}

@Composable
private fun RailRow(
    section: SettingsSection,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.surfaceStrong else Color.Transparent)
            .border(1.dp, if (selected) colors.accent.copy(alpha = 0.45f) else Color.Transparent, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(section.icon, tint, size = 34.dp, radius = 10.dp, iconSize = 18.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(section.title), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(section.subtitle),
                color = colors.muted,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
internal fun SettingsMenu(
    sections: List<SettingsSection>,
    appMode: AppMode,
    onSelect: (SettingsSection) -> Unit,
    onDone: () -> Unit,
    status: StatusUiState?,
    address: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .statusBarsPadding()
            .verticalScrollbar(scroll)
            .verticalScroll(scroll)
            .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 30.dp),
    ) {
        MenuTitle(size = 32, onDone = onDone, modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 18.dp))
        MenuTop(sections, appMode, status, address, onSelect, modeSelected = false)
        groupsOf(sections).forEach { (group, rows) ->
            Eyebrow(
                stringResource(group.label),
                colors.muted,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 22.dp, bottom = 8.dp),
            )
            LookCard(padding = PaddingValues(0.dp)) {
                rows.forEachIndexed { index, section ->
                    if (index > 0) LookDivider()
                    MenuRow(
                        section = section,
                        tint = colors.groupTint(group, appMode),
                        onClick = { onSelect(section) },
                        modifier = Modifier.testTag(UiTags.settingsSection(section)),
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuTitle(size: Int, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(Res.string.settings_title),
            color = colors.text,
            fontSize = size.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.035).em,
        )
        Text(
            stringResource(Res.string.settings_done),
            color = colors.accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .testTag(UiTags.SETTINGS_CANCEL)
                .clickable(role = Role.Button, onClick = onDone)
                .padding(horizontal = 4.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun MenuTop(
    sections: List<SettingsSection>,
    appMode: AppMode,
    status: StatusUiState?,
    address: String,
    onSelect: (SettingsSection) -> Unit,
    modeSelected: Boolean,
) {
    if (SettingsSection.MODE in sections) {
        ModeRow(
            appMode = appMode,
            selected = modeSelected,
            onClick = { onSelect(SettingsSection.MODE) },
            modifier = Modifier.testTag(UiTags.settingsSection(SettingsSection.MODE)),
        )
    }
    if (status != null) {
        Box(Modifier.padding(top = 10.dp)) {
            ConnectionLine(status = status, address = address, onClick = { onSelect(SettingsSection.SERVER) })
        }
    }
}

@Composable
private fun ModeRow(appMode: AppMode, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val tint = colors.modeTint(appMode)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.halo(tint.copy(alpha = 0.14f), 16.dp, 3.dp) else Modifier)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = if (selected) 0.16f else 0.13f), tint.copy(alpha = 0.04f)),
                ),
            )
            .border(if (selected) 1.5.dp else 1.dp, if (selected) tint else tint.copy(alpha = 0.45f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IconTile(appMode.icon(), tint, size = 42.dp, radius = 12.dp, iconSize = 22.dp)
        Column(modifier = Modifier.weight(1f)) {
            Eyebrow(stringResource(Res.string.mode_section_title), tint)
            Text(
                stringResource(appMode.titleRes()),
                color = colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02).em,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun MenuRow(section: SettingsSection, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IconTile(section.icon, tint, size = 32.dp, radius = 9.dp, iconSize = 17.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(section.title), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(section.subtitle),
                color = colors.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun ConnectionLine(
    status: StatusUiState,
    address: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val summary = connectionSummary(status)
    val tone = colors.toneColor(summary.tone)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.copy(alpha = 0.1f))
            .border(1.dp, tone.copy(alpha = 0.35f), shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .testTag(UiTags.SETTINGS_CONNECTION)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusDot(tone)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(summary.label), color = tone, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (summary.tone == ConnectionTone.BAD) {
                    stringResource(Res.string.settings_connection_tap_to_fix, address)
                } else {
                    address
                },
                color = colors.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
