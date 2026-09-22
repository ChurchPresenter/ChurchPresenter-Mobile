package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_chip_after_prev
import churchpresentermobile.composeapp.generated.resources.calendar_chip_blank
import churchpresentermobile.composeapp.generated.resources.calendar_chip_loop
import churchpresentermobile.composeapp.generated.resources.calendar_chip_minus_minutes
import churchpresentermobile.composeapp.generated.resources.calendar_chip_next_item
import churchpresentermobile.composeapp.generated.resources.calendar_chip_on_time
import churchpresentermobile.composeapp.generated.resources.calendar_move_down
import churchpresentermobile.composeapp.generated.resources.calendar_move_up
import churchpresentermobile.composeapp.generated.resources.calendar_remove_row
import com.church.presenter.churchpresentermobile.calendar.ClockedRow
import com.church.presenter.churchpresentermobile.calendar.clockText
import com.church.presenter.churchpresentermobile.calendar.formatDuration
import com.church.presenter.churchpresentermobile.calendar.minutesOfDay
import com.church.presenter.churchpresentermobile.calendar.timeFromMinutes
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.RowEnd
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** A section heading: a drag handle, a color bar and the name in small caps. */
@Composable
internal fun SectionRow(row: PlanRow.Section, actions: RowActions, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val tint = colorOf(row.color)
    Row(
        modifier = modifier.fillMaxWidth()
            .clickable(onClick = actions.onOpen)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.DragIndicator, contentDescription = null, tint = colors.dim, modifier = Modifier.size(18.dp))
        Box(modifier = Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(tint))
        Text(
            text = row.title.uppercase(),
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.em,
            modifier = Modifier.weight(1f),
        )
        InlineRowActions(actions)
    }
}

/** An item row: its projected time, kind, title, length, and the chips that say how it runs. */
@Composable
internal fun ItemRow(clocked: ClockedRow, serviceStart: String, actions: RowActions, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val row = clocked.row
    val isCue = row is PlanRow.Ref && row.kind == RowKind.CUE
    val stroke = if (isCue) colors.accent else colors.borderSubtle
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (isCue) colors.accentTint else colors.surface)
            .border(1.dp, stroke, CardShape)
            .clickable(onClick = actions.onOpen)
            .padding(start = 8.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.DragIndicator, contentDescription = null, tint = colors.dim, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = clocked.startMinutes?.let { clockText(timeFromMinutes(it)) }.orEmpty(),
                    color = if (isCue) colors.accent else colors.muted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(56.dp),
                    maxLines = 1,
                )
                KindBadge(row.kindKey, size = 26.dp)
                Text(
                    text = row.title,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isCue) {
                    Text(
                        text = row.subtitle.ifEmpty { row.title }.uppercase(),
                        color = colors.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.em,
                    )
                } else if (clocked.seconds != null) {
                    Text(
                        text = formatDuration(clocked.seconds),
                        color = colors.text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(start = 64.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MutedText(rowSubtitle(row), modifier = Modifier.weight(1f, fill = false))
                timingBadges(clocked.timing, serviceStart).forEach { TimingBadge(it) }
            }
        }
        InlineRowActions(actions)
    }
}

@Composable
private fun InlineRowActions(actions: RowActions) {
    val colors = LocalAppColors.current
    if (actions.onMoveUp == null && actions.onMoveDown == null && actions.onRemove == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        SmallIconButton(
            Icons.Filled.KeyboardArrowUp,
            stringResource(Res.string.calendar_move_up),
            actions.onMoveUp,
            colors.muted,
        )
        SmallIconButton(
            Icons.Filled.KeyboardArrowDown,
            stringResource(Res.string.calendar_move_down),
            actions.onMoveDown,
            colors.muted,
        )
        SmallIconButton(
            Icons.Outlined.Close,
            stringResource(Res.string.calendar_remove_row),
            actions.onRemove,
            colors.danger,
        )
    }
}

@Composable
private fun SmallIconButton(
    icon: ImageVector,
    description: String,
    onClick: (() -> Unit)?,
    tint: Color,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (onClick == null) colors.dim else tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TimingBadge(label: String) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceStrong)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, color = colors.secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** The second line under a title: the songbook, the passage's first words, who is ministering. */
internal fun rowSubtitle(row: PlanRow): String = when (row) {
    is PlanRow.Song -> row.songbook
    is PlanRow.Bible -> row.preview
    is PlanRow.Ministry -> row.detail
    is PlanRow.Preset -> row.title
    is PlanRow.Ref -> row.subtitle
    is PlanRow.Section -> ""
}

/** `Loop` · `Next` · `−15` — only what differs from a row cued by hand. */
@Composable
internal fun timingBadges(timing: RowTiming, serviceStart: String): List<String> {
    val badges = mutableListOf<String>()
    if (timing.repeats == 0) badges += stringResource(Res.string.calendar_chip_loop)
    if (timing.repeats > 1) badges += "${timing.repeats}x"
    when (timing.atEnd) {
        RowEnd.NEXT -> badges += stringResource(Res.string.calendar_chip_next_item)
        RowEnd.BLANK -> badges += stringResource(Res.string.calendar_chip_blank)
    }
    val startMinutes = minutesOfDay(serviceStart)
    val rowMinutes = timing.startAt.takeIf { it.isNotEmpty() }?.let(::minutesOfDay)
    when {
        rowMinutes != null && startMinutes != null && rowMinutes < startMinutes ->
            badges += stringResource(Res.string.calendar_chip_minus_minutes, startMinutes - rowMinutes)
        rowMinutes != null && startMinutes != null && rowMinutes == startMinutes ->
            badges += stringResource(Res.string.calendar_chip_on_time)
        rowMinutes != null -> badges += clockText(timing.startAt)
        timing.followsPrevious -> badges += stringResource(Res.string.calendar_chip_after_prev)
    }
    return badges
}
