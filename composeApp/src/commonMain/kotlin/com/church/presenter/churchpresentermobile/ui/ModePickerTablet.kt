package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.app_title
import churchpresentermobile.composeapp.generated.resources.start_great_for
import churchpresentermobile.composeapp.generated.resources.start_subtitle_tablet
import churchpresentermobile.composeapp.generated.resources.start_switch_hint
import churchpresentermobile.composeapp.generated.resources.start_title
import churchpresentermobile.composeapp.generated.resources.start_welcome_to
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TabletStart(selected: AppMode, tint: Color, onSelect: (AppMode) -> Unit, onContinue: () -> Unit) {
    FitToHeight(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) { overflows ->
        val scroll = rememberScrollState()
        TabletStartContent(
            selected = selected,
            tint = tint,
            onSelect = onSelect,
            onContinue = onContinue,
            modifier = if (overflows) Modifier.verticalScrollbar(scroll).verticalScroll(scroll) else Modifier,
        )
    }
}

/** The picker at its designed size; [TabletStart] shrinks it to fit a landscape tablet's height. */
@Composable
private fun TabletStartContent(
    selected: AppMode,
    tint: Color,
    onSelect: (AppMode) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = LocalAppColors.current
    Column(
        modifier = modifier.padding(start = 64.dp, end = 64.dp, top = 44.dp, bottom = 48.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LogoTile(size = 48.dp, radius = 14.dp, crossHeight = 28.dp)
            Eyebrow(
                "${stringResource(Res.string.start_welcome_to)} ${stringResource(Res.string.app_title)}",
                look.muted,
                size = 13.sp,
            )
        }
        Text(
            text = stringResource(Res.string.start_title),
            color = look.text,
            fontSize = 50.sp,
            lineHeight = 53.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.04).em,
            modifier = Modifier.padding(top = 26.dp),
        )
        Text(
            text = stringResource(Res.string.start_subtitle_tablet),
            color = look.muted,
            fontSize = 19.sp,
            lineHeight = 27.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier
                .padding(top = 38.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            startModes.forEach { mode ->
                TabletModeCard(
                    mode = mode,
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag(UiTags.modeCard(mode)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false).padding(end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Outlined.SwapHoriz,
                    contentDescription = null,
                    tint = look.muted,
                    modifier = Modifier.size(18.dp),
                )
                Text(stringResource(Res.string.start_switch_hint), color = look.muted, fontSize = 15.sp)
            }
            ContinueButton(
                mode = selected,
                tint = tint,
                onClick = onContinue,
                modifier = Modifier.widthIn(min = 340.dp),
            )
        }
    }
}

@Composable
internal fun TabletModeCard(mode: AppMode, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val look = LocalAppColors.current
    val tint = look.modeTint(mode)
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .then(if (selected) Modifier.halo(tint.copy(alpha = 0.14f), 22.dp) else Modifier)
            .clip(shape)
            .background(cardBrush(selected, tint))
            .border(1.5.dp, if (selected) tint else look.border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(look.inputBg)
                .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.16f), Color.Transparent)))
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            ModePicture(mode = mode, tint = tint, large = true)
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp)) {
                RadioMark(selected = selected, tint = tint)
            }
        }
        LookDivider()
        TabletModeBody(mode = mode, tint = tint, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun TabletModeBody(mode: AppMode, tint: Color, modifier: Modifier = Modifier) {
    val look = LocalAppColors.current
    Column(modifier = modifier.padding(start = 28.dp, end = 28.dp, top = 26.dp, bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(mode.icon(), tint, size = 52.dp, radius = 15.dp, iconSize = 26.dp)
            Text(
                stringResource(mode.titleRes()),
                color = look.text,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.03).em,
            )
        }
        Text(
            stringResource(mode.bodyRes()),
            color = look.muted,
            fontSize = 16.5.sp,
            lineHeight = 25.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
        Box(Modifier.padding(top = 20.dp, bottom = 16.dp)) { LookDivider() }
        Eyebrow(stringResource(Res.string.start_great_for), tint)
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            mode.greatFor().forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.padding(top = 2.dp).size(16.dp),
                    )
                    Text(
                        stringResource(line),
                        color = look.muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(mode.needsRes()),
            color = look.muted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}
