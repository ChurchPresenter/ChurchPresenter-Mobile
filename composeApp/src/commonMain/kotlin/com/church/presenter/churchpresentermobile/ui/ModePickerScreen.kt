package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SettingsRemote
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.mode_continue
import churchpresentermobile.composeapp.generated.resources.mode_picker_subtitle
import churchpresentermobile.composeapp.generated.resources.mode_picker_title
import churchpresentermobile.composeapp.generated.resources.mode_remote_body
import churchpresentermobile.composeapp.generated.resources.mode_remote_title
import churchpresentermobile.composeapp.generated.resources.mode_standalone_body
import churchpresentermobile.composeapp.generated.resources.mode_standalone_title
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * First-launch choice between driving a desktop and presenting from the phone.
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
) {
    val colors = LocalAppColors.current
    var selected by remember { mutableStateOf(initialMode) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.space20),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.mode_picker_title),
            color = colors.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.mode_picker_subtitle),
            color = colors.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = AppDimens.space24),
        )

        ModeCard(
            title = stringResource(Res.string.mode_remote_title),
            body = stringResource(Res.string.mode_remote_body),
            icon = Icons.Filled.SettingsRemote,
            selected = selected == AppMode.REMOTE,
            onClick = { selected = AppMode.REMOTE },
        )
        Box(Modifier.size(AppDimens.space12))
        ModeCard(
            title = stringResource(Res.string.mode_standalone_title),
            body = stringResource(Res.string.mode_standalone_body),
            icon = Icons.Filled.Cast,
            selected = selected == AppMode.STANDALONE,
            onClick = { selected = AppMode.STANDALONE },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.space24)
                .clip(RoundedCornerShape(AppDimens.radiusButton))
                .background(colors.accent)
                .clickable { onModeChosen(selected) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.mode_continue),
                color = colors.onAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    body: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(if (selected) colors.accentTint else colors.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.accent else colors.border,
                shape = RoundedCornerShape(AppDimens.radiusCard),
            )
            .clickable(onClick = onClick)
            .padding(AppDimens.space16),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space14),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) colors.accent else colors.muted,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = body, color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
