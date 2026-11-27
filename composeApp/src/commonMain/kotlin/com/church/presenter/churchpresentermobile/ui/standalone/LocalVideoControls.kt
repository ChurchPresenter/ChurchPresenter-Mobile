package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.local_media_end_black
import churchpresentermobile.composeapp.generated.resources.local_media_end_hold
import churchpresentermobile.composeapp.generated.resources.local_media_end_loop
import churchpresentermobile.composeapp.generated.resources.local_media_no_video
import churchpresentermobile.composeapp.generated.resources.local_media_on_screen
import churchpresentermobile.composeapp.generated.resources.local_media_sound_hall
import churchpresentermobile.composeapp.generated.resources.local_media_sound_off
import churchpresentermobile.composeapp.generated.resources.local_media_sound_on
import churchpresentermobile.composeapp.generated.resources.local_media_sound_phone
import churchpresentermobile.composeapp.generated.resources.local_media_sound_phone_hint
import churchpresentermobile.composeapp.generated.resources.local_media_status_empty
import churchpresentermobile.composeapp.generated.resources.local_media_status_paused_live
import churchpresentermobile.composeapp.generated.resources.local_media_status_playing_live
import churchpresentermobile.composeapp.generated.resources.local_media_status_previewing
import churchpresentermobile.composeapp.generated.resources.local_media_status_ready
import churchpresentermobile.composeapp.generated.resources.local_media_when_ends
import churchpresentermobile.composeapp.generated.resources.standalone_blank
import churchpresentermobile.composeapp.generated.resources.standalone_clear
import churchpresentermobile.composeapp.generated.resources.standalone_live
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.VideoSound
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LocalVideoStatus
import com.church.presenter.churchpresentermobile.viewmodel.VideoAction
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The "on screen" button's tint, matching the red frame round the preview. */
private const val ON_SCREEN_FILL_ALPHA = 0.14f
private const val ON_SCREEN_BORDER_ALPHA = 0.55f

/** What is loaded, and what the hall screen is doing with it. */
@Composable
internal fun VideoNowPlaying(title: String?, status: LocalVideoStatus) {
    val colors = LocalAppColors.current
    val (label, tint) = when (status) {
        LocalVideoStatus.EMPTY -> Res.string.local_media_status_empty to colors.muted
        LocalVideoStatus.READY -> Res.string.local_media_status_ready to colors.muted
        LocalVideoStatus.PREVIEWING -> Res.string.local_media_status_previewing to colors.muted
        LocalVideoStatus.PLAYING_LIVE -> Res.string.local_media_status_playing_live to colors.accent
        LocalVideoStatus.PAUSED_LIVE -> Res.string.local_media_status_paused_live to colors.warning
    }
    Text(
        title ?: stringResource(Res.string.local_media_no_video),
        color = colors.text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag(VideoTags.TITLE),
    )
    Spacer(Modifier.height(5.dp))
    Text(
        stringResource(label),
        color = tint,
        fontSize = 12.sp,
        fontWeight = if (status == LocalVideoStatus.PLAYING_LIVE) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.testTag(VideoTags.STATUS),
    )
}

/**
 * Blank · Clear · Live — the same three the live bar has always had, in the same
 * order, so the operator's hands already know them. Live turns into "On screen"
 * while the video is up; pressing it then takes the screen off-air.
 */
@Composable
internal fun VideoScreenActions(
    loaded: Boolean,
    onScreen: Boolean,
    blank: Boolean,
    onAction: (VideoAction) -> Unit,
) {
    val colors = LocalAppColors.current
    val live = onScreen && !blank
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionButton(
            label = stringResource(Res.string.standalone_blank),
            icon = Icons.Filled.HideSource,
            enabled = loaded && onScreen,
            background = if (blank && onScreen) Color.Black else colors.surface,
            content = if (blank && onScreen) Color.White else colors.text,
            border = colors.border,
            tag = VideoTags.BLANK,
            onClick = { onAction(VideoAction.ToggleBlank) },
        )
        ActionButton(
            label = stringResource(Res.string.standalone_clear),
            icon = null,
            enabled = loaded,
            background = colors.surface,
            content = colors.text,
            border = colors.border,
            tag = VideoTags.CLEAR,
            onClick = { onAction(VideoAction.Clear) },
        )
        ActionButton(
            label = stringResource(if (live) Res.string.local_media_on_screen else Res.string.standalone_live),
            icon = if (live) null else Icons.Filled.Sensors,
            dot = live,
            enabled = loaded,
            background = if (live) colors.danger.copy(alpha = ON_SCREEN_FILL_ALPHA) else colors.danger,
            content = if (live) colors.danger else Color.White,
            border = if (live) colors.danger.copy(alpha = ON_SCREEN_BORDER_ALPHA) else colors.danger,
            tag = VideoTags.LIVE,
            onClick = { onAction(VideoAction.ToggleLive) },
        )
    }
}

@Composable
private fun RowScope.ActionButton(
    label: String,
    icon: ImageVector?,
    enabled: Boolean,
    background: Color,
    content: Color,
    border: Color,
    tag: String,
    onClick: () -> Unit,
    dot: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    val tint = if (enabled) content else content.copy(alpha = 0.4f)
    Row(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(shape)
            .background(if (enabled) background else background.copy(alpha = 0.5f))
            .border(1.dp, border, shape)
            .testTag(tag)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.size(7.dp))
        }
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(7.dp))
        }
        Text(label, color = tint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** "When it ends" and "Sound plays on" — set once per clip, so tucked into a card. */
@Composable
internal fun VideoOptionsCard(
    endAction: VideoEndAction,
    sound: VideoSound,
    enabled: Boolean,
    onEndAction: (VideoEndAction) -> Unit,
    onSound: (VideoSound) -> Unit,
    phoneSoundAvailable: Boolean = true,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        OptionRow(
            label = stringResource(Res.string.local_media_when_ends),
            options = VideoEndAction.entries,
            selected = endAction,
            optionLabel = ::endActionLabel,
            enabled = enabled,
            accent = true,
            tag = VideoTags.END_ACTION,
            onSelect = onEndAction,
        )
        HorizontalDivider(color = colors.borderSubtle)
        OptionRow(
            label = stringResource(Res.string.local_media_sound_on),
            options = if (phoneSoundAvailable) VideoSound.entries else VideoSound.entries - VideoSound.PHONE,
            selected = sound,
            optionLabel = ::soundLabel,
            enabled = enabled,
            accent = false,
            tag = VideoTags.SOUND,
            onSelect = onSound,
        )
    }
    // An iPad or iPhone has one audio route: with a TV on AirPlay or a cable, the
    // device's sound goes there too, so "This phone" is not offered — say why.
    if (!phoneSoundAvailable) {
        Text(
            stringResource(Res.string.local_media_sound_phone_hint),
            color = colors.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp).testTag(VideoTags.SOUND_HINT),
        )
    }
}


@Composable
private fun <T> OptionRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> StringResource,
    enabled: Boolean,
    accent: Boolean,
    tag: String,
    onSelect: (T) -> Unit,
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(tag)
            .clickable(enabled = enabled) { expanded = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(optionLabel(selected)),
                    color = if (accent) colors.accent else colors.muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(optionLabel(option))) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        modifier = Modifier.testTag(VideoTags.option(tag, option.toString())),
                    )
                }
            }
        }
    }
}

private fun endActionLabel(action: VideoEndAction): StringResource = when (action) {
    VideoEndAction.BLACK -> Res.string.local_media_end_black
    VideoEndAction.HOLD -> Res.string.local_media_end_hold
    VideoEndAction.LOOP -> Res.string.local_media_end_loop
}

private fun soundLabel(sound: VideoSound): StringResource = when (sound) {
    VideoSound.HALL -> Res.string.local_media_sound_hall
    VideoSound.PHONE -> Res.string.local_media_sound_phone
    VideoSound.OFF -> Res.string.local_media_sound_off
}
