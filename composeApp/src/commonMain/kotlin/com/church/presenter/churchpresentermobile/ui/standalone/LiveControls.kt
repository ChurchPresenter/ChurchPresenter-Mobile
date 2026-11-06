package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.DesktopAccessDisabled
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.standalone_blank
import churchpresentermobile.composeapp.generated.resources.standalone_clear
import churchpresentermobile.composeapp.generated.resources.standalone_empty_body
import churchpresentermobile.composeapp.generated.resources.standalone_empty_title
import churchpresentermobile.composeapp.generated.resources.standalone_live
import churchpresentermobile.composeapp.generated.resources.standalone_next
import churchpresentermobile.composeapp.generated.resources.standalone_prev
import churchpresentermobile.composeapp.generated.resources.standalone_preview
import churchpresentermobile.composeapp.generated.resources.standalone_slide_position
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.ui.OverlineRow
import com.church.presenter.churchpresentermobile.ui.SCREEN_ASPECT_RATIO
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/*
 * The pieces of the live surface, shared by the live bar and the full live view.
 *
 * They were the Present tab's. The tab is gone — the live controls now ride
 * along every standalone tab — but a preview, a section list and the transport
 * buttons are the same things wherever they are drawn, so they are written once.
 */

/** A 16:9 window onto the real renderer — not a mock-up of it. */
@Composable
internal fun SlidePreview(slide: Slide, total: Int, index: Int, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OverlineRow(
            label = stringResource(Res.string.standalone_preview),
            trailing = if (total > 0) {
                stringResource(
                    Res.string.standalone_slide_position,
                    (index + 1).toString(),
                    total.toString(),
                )
            } else null,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(SCREEN_ASPECT_RATIO)
                .clip(RoundedCornerShape(AppDimens.radiusChip))
                .border(1.dp, colors.border, RoundedCornerShape(AppDimens.radiusChip)),
        ) {
            StandaloneOutputScreen(slide)
        }
    }
}

/** The steppable list of sections/verses — the operator's main working surface. */
@Composable
internal fun SectionList(
    title: String,
    slides: List<Slide>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        OverlineRow(title)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(slides) { i, slide ->
                val active = i == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(StandaloneTags.section(i))
                        .semantics { selected = active }
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) colors.accent else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = (i + 1).toString(),
                        color = if (active) colors.onAccent.copy(alpha = 0.6f) else colors.muted,
                        fontSize = 11.sp,
                        modifier = Modifier.size(width = 26.dp, height = 16.dp),
                    )
                    Text(
                        text = slide.body.lineSequence().firstOrNull()?.trim().orEmpty(),
                        color = if (active) colors.onAccent else colors.text,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyDeckHint(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space16),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.standalone_empty_title),
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.standalone_empty_body),
            color = colors.muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
internal fun TransportRow(
    canStepBack: Boolean,
    canStepForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        ControlButton(
            label = stringResource(Res.string.standalone_prev),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            enabled = canStepBack,
            onClick = onPrevious,
            modifier = Modifier.weight(1f).testTag(StandaloneTags.PREV),
        )
        ControlButton(
            label = stringResource(Res.string.standalone_next),
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            enabled = canStepForward,
            onClick = onNext,
            modifier = Modifier.weight(1f).testTag(StandaloneTags.NEXT),
        )
    }
}

@Composable
internal fun StateRow(
    isBlank: Boolean,
    isLive: Boolean,
    hasContent: Boolean,
    onToggleBlank: () -> Unit,
    onClear: () -> Unit,
    onToggleLive: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = AppDimens.space16),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        ControlButton(
            label = stringResource(Res.string.standalone_blank),
            icon = Icons.Filled.HideSource,
            onClick = onToggleBlank,
            modifier = Modifier.weight(1f).testTag(StandaloneTags.BLANK),
            background = if (isBlank) Color.Black else colors.surface,
            contentColor = if (isBlank) Color.White else colors.text,
        )
        // Blank and Clear are not the same act, and the difference matters
        // mid-service: Blank hides the words and keeps the song, so the next
        // press brings it straight back. Clear puts the deck down — the screen
        // is empty and the song has to be opened again. Disabled with nothing
        // loaded, where it would only look like a button that does nothing.
        ControlButton(
            label = stringResource(Res.string.standalone_clear),
            icon = Icons.Filled.DesktopAccessDisabled,
            enabled = hasContent,
            onClick = onClear,
            modifier = Modifier.weight(1f).testTag(StandaloneTags.CLEAR),
        )
        ControlButton(
            label = stringResource(Res.string.standalone_live),
            icon = Icons.Filled.Sensors,
            onClick = onToggleLive,
            modifier = Modifier.weight(1f).testTag(StandaloneTags.LIVE),
            background = if (isLive) colors.danger else colors.surface,
            contentColor = if (isLive) Color.White else colors.text,
        )
    }
}

@Composable
internal fun ControlButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color? = null,
    contentColor: Color? = null,
) {
    val colors = LocalAppColors.current
    val bg = background ?: colors.surface
    val fg = (contentColor ?: colors.text).copy(alpha = if (enabled) 1f else 0.35f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .border(1.dp, colors.border, RoundedCornerShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
