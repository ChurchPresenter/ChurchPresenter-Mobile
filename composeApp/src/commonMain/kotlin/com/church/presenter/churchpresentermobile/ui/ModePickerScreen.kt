package com.church.presenter.churchpresentermobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.app_title
import churchpresentermobile.composeapp.generated.resources.mode_calendar_title
import churchpresentermobile.composeapp.generated.resources.mode_remote_body
import churchpresentermobile.composeapp.generated.resources.mode_remote_title
import churchpresentermobile.composeapp.generated.resources.mode_standalone_title
import churchpresentermobile.composeapp.generated.resources.start_calendar_body
import churchpresentermobile.composeapp.generated.resources.start_calendar_great_1
import churchpresentermobile.composeapp.generated.resources.start_calendar_great_2
import churchpresentermobile.composeapp.generated.resources.start_calendar_needs
import churchpresentermobile.composeapp.generated.resources.start_calendar_short
import churchpresentermobile.composeapp.generated.resources.start_remote_great_1
import churchpresentermobile.composeapp.generated.resources.start_remote_great_2
import churchpresentermobile.composeapp.generated.resources.start_remote_needs
import churchpresentermobile.composeapp.generated.resources.start_remote_short
import churchpresentermobile.composeapp.generated.resources.start_standalone_body
import churchpresentermobile.composeapp.generated.resources.start_standalone_great_1
import churchpresentermobile.composeapp.generated.resources.start_standalone_great_2
import churchpresentermobile.composeapp.generated.resources.start_standalone_needs
import churchpresentermobile.composeapp.generated.resources.start_standalone_short
import churchpresentermobile.composeapp.generated.resources.start_subtitle
import churchpresentermobile.composeapp.generated.resources.start_title
import churchpresentermobile.composeapp.generated.resources.start_welcome_to
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * First-launch choice of how the app is used: remote control, standalone or calendar.
 *
 * Shown only where standalone is actually possible — the web build skips
 * straight to the existing connect flow, so its users never see a choice the
 * platform cannot honour.
 */
@Composable
fun ModePickerScreen(
    onModeChosen: (AppMode) -> Unit,
    modifier: Modifier = Modifier,
    initialMode: AppMode = AppMode.REMOTE,
    twoPane: Boolean = false,
) {
    var selected by remember { mutableStateOf(initialMode) }
    val look = LocalAppColors.current
    val tint by animateColorAsState(look.modeTint(selected))
    Box(modifier = modifier.fillMaxSize().background(look.background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (twoPane) 340.dp else 260.dp)
                .background(
                    Brush.radialGradient(
                        listOf(tint.copy(alpha = if (look.isDark) 0.22f else 0.14f), Color.Transparent),
                    ),
                ),
        )
        if (twoPane) {
            TabletStart(selected = selected, tint = tint, onSelect = { selected = it }) { onModeChosen(selected) }
        } else {
            PhoneStart(selected = selected, tint = tint, onSelect = { selected = it }) { onModeChosen(selected) }
        }
    }
}

internal val startModes = listOf(AppMode.REMOTE, AppMode.STANDALONE, AppMode.CALENDAR)

internal fun AppMode.icon(): ImageVector = when (this) {
    AppMode.REMOTE -> Icons.Filled.SettingsRemote
    AppMode.STANDALONE -> Icons.Filled.Cast
    AppMode.CALENDAR -> Icons.Filled.CalendarMonth
}

internal fun AppMode.titleRes(): StringResource = when (this) {
    AppMode.REMOTE -> Res.string.mode_remote_title
    AppMode.STANDALONE -> Res.string.mode_standalone_title
    AppMode.CALENDAR -> Res.string.mode_calendar_title
}

private fun AppMode.shortRes(): StringResource = when (this) {
    AppMode.REMOTE -> Res.string.start_remote_short
    AppMode.STANDALONE -> Res.string.start_standalone_short
    AppMode.CALENDAR -> Res.string.start_calendar_short
}

internal fun AppMode.bodyRes(): StringResource = when (this) {
    AppMode.REMOTE -> Res.string.mode_remote_body
    AppMode.STANDALONE -> Res.string.start_standalone_body
    AppMode.CALENDAR -> Res.string.start_calendar_body
}

internal fun AppMode.needsRes(): StringResource = when (this) {
    AppMode.REMOTE -> Res.string.start_remote_needs
    AppMode.STANDALONE -> Res.string.start_standalone_needs
    AppMode.CALENDAR -> Res.string.start_calendar_needs
}

internal fun AppMode.greatFor(): List<StringResource> = when (this) {
    AppMode.REMOTE -> listOf(Res.string.start_remote_great_1, Res.string.start_remote_great_2)
    AppMode.STANDALONE -> listOf(Res.string.start_standalone_great_1, Res.string.start_standalone_great_2)
    AppMode.CALENDAR -> listOf(Res.string.start_calendar_great_1, Res.string.start_calendar_great_2)
}

@Composable
private fun PhoneStart(selected: AppMode, tint: Color, onSelect: (AppMode) -> Unit, onContinue: () -> Unit) {
    val look = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 30.dp),
    ) {
        val scroll = rememberScrollState()
        Column(modifier = Modifier.weight(1f).verticalScrollbar(scroll).verticalScroll(scroll)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LogoTile(size = 44.dp, radius = 13.dp, crossHeight = 26.dp)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Eyebrow(stringResource(Res.string.start_welcome_to), look.muted)
                    Text(
                        stringResource(Res.string.app_title),
                        color = look.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.01).em,
                    )
                }
            }
            Text(
                text = stringResource(Res.string.start_title),
                color = look.text,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.035).em,
                modifier = Modifier.padding(top = 26.dp, end = 60.dp),
            )
            Text(
                text = stringResource(Res.string.start_subtitle),
                color = look.muted,
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Column(
                modifier = Modifier.padding(top = 22.dp, bottom = 16.dp).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                startModes.forEach { mode ->
                    PhoneModeCard(
                        mode = mode,
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.testTag(UiTags.modeCard(mode)),
                    )
                }
            }
        }
        ContinueButton(mode = selected, tint = tint, onClick = onContinue, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PhoneModeCard(mode: AppMode, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val look = LocalAppColors.current
    val tint = look.modeTint(mode)
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.halo(tint.copy(alpha = 0.14f), 20.dp) else Modifier)
            .clip(shape)
            .background(cardBrush(selected, tint))
            .border(1.5.dp, if (selected) tint else look.border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(mode.icon(), tint, size = 46.dp, radius = 14.dp, iconSize = 24.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(mode.titleRes()),
                    color = look.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).em,
                )
                Text(
                    stringResource(mode.shortRes()),
                    color = look.muted,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            RadioMark(selected = selected, tint = tint)
        }
        AnimatedVisibility(visible = selected) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                val panelShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(panelShape)
                        .background(look.inputBg)
                        .border(1.dp, look.border, panelShape)
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ModePicture(mode = mode, tint = tint, large = false)
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = look.muted,
                        modifier = Modifier.padding(top = 2.dp).size(14.dp),
                    )
                    Text(
                        stringResource(mode.needsRes()),
                        color = look.muted,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
