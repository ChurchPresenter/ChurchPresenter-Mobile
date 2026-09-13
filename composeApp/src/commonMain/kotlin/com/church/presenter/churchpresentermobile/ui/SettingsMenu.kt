package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_title
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import org.jetbrains.compose.resources.stringResource

/**
 * The tablet's list of sections beside the settings form.
 *
 * Its own pane: a title, one row per section with the open one tinted, and
 * the connection at its foot so the state of the desktop is visible whichever
 * page is open.
 *
 * @param status The desktop connection, or null in a mode that has no desktop.
 */
@Composable
internal fun SettingsSectionList(
    sections: List<SettingsSection>,
    current: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
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
            .statusBarsPadding(),
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            color = colors.text,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.035).em,
            modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 20.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScrollbar(scroll)
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            sections.forEach { section ->
                PaneRow(
                    section = section,
                    isSelected = section == current,
                    onClick = { onSelect(section) },
                    modifier = Modifier.testTag(UiTags.settingsSection(section)),
                )
            }
        }
        if (status != null) {
            HorizontalDivider(color = colors.borderSubtle)
            ConnectionLine(status = status, address = address, modifier = Modifier.padding(26.dp, 20.dp))
        }
    }
}

/** One row of the tablet's list: icon, title over subtitle, chevron. */
@Composable
private fun PaneRow(
    section: SettingsSection,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) colors.accentTint else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = if (isSelected) colors.accent else colors.muted,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(section.title),
                color = if (isSelected) colors.accent else colors.text,
                fontSize = 18.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-0.02).em,
            )
            Text(text = stringResource(section.subtitle), color = colors.muted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isSelected) colors.accent else colors.dim,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * The phone's settings menu: the connection, then the sections as one grouped
 * card. Tapping a row opens that section's page.
 *
 * @param status The desktop connection, or null in a mode that has no desktop.
 */
@Composable
internal fun SettingsMenu(
    sections: List<SettingsSection>,
    onSelect: (SettingsSection) -> Unit,
    status: StatusUiState?,
    address: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val scroll = rememberScrollState()
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .verticalScrollbar(scroll)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (status != null) {
            ConnectionLine(
                status = status,
                address = address,
                compact = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accentTint)
                    .border(1.dp, colors.accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 15.dp, vertical = 13.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.borderSubtle, shape),
        ) {
            sections.forEachIndexed { index, section ->
                if (index > 0) HorizontalDivider(color = colors.borderSubtle)
                MenuRow(
                    section = section,
                    onClick = { onSelect(section) },
                    modifier = Modifier.testTag(UiTags.settingsSection(section)),
                )
            }
        }
    }
}

/** One row of the phone's menu: icon tile, title over subtitle, chevron. */
@Composable
private fun MenuRow(section: SettingsSection, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.inputBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(section.icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(section.title),
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = stringResource(section.subtitle), color = colors.muted, fontSize = 12.sp)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.dim,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** A coloured dot, the connection in one word, and the address under it. */
@Composable
internal fun ConnectionLine(
    status: StatusUiState,
    address: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = LocalAppColors.current
    val summary = connectionSummary(status)
    val tone = when (summary.tone) {
        ConnectionTone.GOOD -> colors.accent
        ConnectionTone.WARNING -> colors.warning
        ConnectionTone.BAD -> colors.danger
        ConnectionTone.PENDING -> colors.muted
    }
    Row(
        modifier = modifier.testTag(UiTags.SETTINGS_CONNECTION),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(if (compact) 9.dp else 10.dp).clip(CircleShape).background(tone))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(summary.label),
                color = tone,
                fontSize = if (compact) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = address,
                color = colors.muted,
                fontSize = if (compact) 11.sp else 13.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
