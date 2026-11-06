package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.mode_calendar_title
import churchpresentermobile.composeapp.generated.resources.mode_remote_title
import churchpresentermobile.composeapp.generated.resources.mode_standalone_title
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Which mode the app is in, beside the settings gear.
 *
 * The three modes share tabs and screens, and a press in the wrong one sends a
 * song to a different place: to the desktop in remote, to this phone's own
 * screens in standalone. So the mode is always on show, not only in Settings.
 */
@Composable
fun ModeBadge(mode: AppMode, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Text(
        text = stringResource(
            when (mode) {
                AppMode.REMOTE -> Res.string.mode_remote_title
                AppMode.STANDALONE -> Res.string.mode_standalone_title
                AppMode.CALENDAR -> Res.string.mode_calendar_title
            }
        ),
        color = colors.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .testTag(UiTags.HEADER_MODE)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
