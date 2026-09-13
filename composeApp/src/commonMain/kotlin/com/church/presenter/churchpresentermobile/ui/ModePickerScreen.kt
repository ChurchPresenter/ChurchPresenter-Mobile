package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import churchpresentermobile.composeapp.generated.resources.mode_diagram_tv
import churchpresentermobile.composeapp.generated.resources.mode_diagram_tablet
import churchpresentermobile.composeapp.generated.resources.mode_diagram_screen
import churchpresentermobile.composeapp.generated.resources.mode_diagram_computer
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.platform.testTag
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
    twoPane: Boolean = false,
) {
    val colors = LocalAppColors.current
    var selected by remember { mutableStateOf(initialMode) }

    if (twoPane) {
        TabletModePicker(
            selected = selected,
            onSelect = { selected = it },
            onContinue = { onModeChosen(selected) },
            modifier = modifier,
        )
        return
    }

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
            modifier = Modifier.testTag(UiTags.modeCard(AppMode.REMOTE)),
        )
        Box(Modifier.size(AppDimens.space12))
        ModeCard(
            title = stringResource(Res.string.mode_standalone_title),
            body = stringResource(Res.string.mode_standalone_body),
            icon = Icons.Filled.Cast,
            selected = selected == AppMode.STANDALONE,
            onClick = { selected = AppMode.STANDALONE },
            modifier = Modifier.testTag(UiTags.modeCard(AppMode.STANDALONE)),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.space24)
                .clip(RoundedCornerShape(AppDimens.radiusButton))
                .background(colors.accent)
                .testTag(UiTags.MODE_CONTINUE)
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
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
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

// ─────────────────────────────────────────────────────────────────────────────
// Tablet: the two modes side by side, each with a diagram of the signal path
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The tablet's arrangement: the two modes as cards in a row, each drawn with
 * the path a slide takes in that mode, and Continue at the bottom right.
 */
@Composable
private fun TabletModePicker(
    selected: AppMode,
    onSelect: (AppMode) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScrollbar(scroll)
            .verticalScroll(scroll)
            .padding(horizontal = 70.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.mode_picker_title),
            color = colors.text,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.04).em,
        )
        Text(
            text = stringResource(Res.string.mode_picker_subtitle),
            color = colors.muted,
            fontSize = 19.sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 36.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            TabletModeCard(
                title = stringResource(Res.string.mode_remote_title),
                body = stringResource(Res.string.mode_remote_body),
                icon = Icons.Filled.SettingsRemote,
                selected = selected == AppMode.REMOTE,
                onClick = { onSelect(AppMode.REMOTE) },
                modifier = Modifier.weight(1f).fillMaxHeight().testTag(UiTags.modeCard(AppMode.REMOTE)),
            ) { accent ->
                DiagramBox(stringResource(Res.string.mode_diagram_tablet), accent, width = 54.dp, height = 88.dp)
                DiagramArrow(accent)
                DiagramBox(stringResource(Res.string.mode_diagram_computer), accent, width = 96.dp, height = 72.dp)
                DiagramArrow(accent)
                DiagramBox(stringResource(Res.string.mode_diagram_screen), accent, width = 120.dp, height = 72.dp)
            }
            TabletModeCard(
                title = stringResource(Res.string.mode_standalone_title),
                body = stringResource(Res.string.mode_standalone_body),
                icon = Icons.Filled.Cast,
                selected = selected == AppMode.STANDALONE,
                onClick = { onSelect(AppMode.STANDALONE) },
                modifier = Modifier.weight(1f).fillMaxHeight().testTag(UiTags.modeCard(AppMode.STANDALONE)),
            ) { accent ->
                DiagramBox(stringResource(Res.string.mode_diagram_tablet), accent, width = 54.dp, height = 88.dp)
                DiagramArrow(accent)
                DiagramBox(stringResource(Res.string.mode_diagram_tv), accent, width = 140.dp, height = 84.dp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.accent)
                    .testTag(UiTags.MODE_CONTINUE)
                    .clickable(onClick = onContinue)
                    .padding(horizontal = 62.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.mode_continue),
                    color = colors.onAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

/**
 * One mode as a tablet card: icon tile, title and body, a radio at the far
 * end, and under them the [diagram] of where a slide goes in this mode.
 *
 * @param diagram The boxes and arrows of the signal path, given whether they
 *   are drawn in the accent (the chosen mode) or muted.
 */
@Composable
private fun TabletModeCard(
    title: String,
    body: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diagram: @Composable RowScope.(accent: Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accentTint else colors.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.accent else colors.borderSubtle,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 28.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) colors.accentTint else colors.inputBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) colors.accent else colors.muted,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.03).em,
                )
                Text(text = body, color = colors.muted, fontSize = 17.sp, lineHeight = 26.sp)
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(26.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(2.dp, colors.border, CircleShape),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.scrim.copy(alpha = 0.22f))
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            diagram(selected)
        }
    }
}

/** One device in a signal-path diagram: a labelled rounded box. */
@Composable
private fun DiagramBox(label: String, accent: Boolean, width: Dp, height: Dp) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(8.dp))
            .background(if (accent) colors.accentTint else colors.inputBg)
            .border(1.5.dp, if (accent) colors.accent else colors.borderSubtle, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (accent) colors.accent else colors.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

/** The arrow between two devices in a signal-path diagram. */
@Composable
private fun DiagramArrow(accent: Boolean) {
    val colors = LocalAppColors.current
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = if (accent) colors.accent else colors.muted,
        modifier = Modifier.size(24.dp),
    )
}
