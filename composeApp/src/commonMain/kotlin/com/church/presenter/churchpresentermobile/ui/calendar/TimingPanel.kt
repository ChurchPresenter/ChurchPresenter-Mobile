package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_at_end
import churchpresentermobile.composeapp.generated.resources.calendar_chip_after_prev
import churchpresentermobile.composeapp.generated.resources.calendar_chip_blank
import churchpresentermobile.composeapp.generated.resources.calendar_chip_cued
import churchpresentermobile.composeapp.generated.resources.calendar_chip_hold
import churchpresentermobile.composeapp.generated.resources.calendar_chip_hour
import churchpresentermobile.composeapp.generated.resources.calendar_chip_its_own
import churchpresentermobile.composeapp.generated.resources.calendar_chip_loop
import churchpresentermobile.composeapp.generated.resources.calendar_chip_minus_minutes
import churchpresentermobile.composeapp.generated.resources.calendar_chip_minutes
import churchpresentermobile.composeapp.generated.resources.calendar_chip_next_item
import churchpresentermobile.composeapp.generated.resources.calendar_chip_on_time
import churchpresentermobile.composeapp.generated.resources.calendar_chip_once
import churchpresentermobile.composeapp.generated.resources.calendar_repeats
import churchpresentermobile.composeapp.generated.resources.calendar_runs
import churchpresentermobile.composeapp.generated.resources.calendar_starts
import churchpresentermobile.composeapp.generated.resources.calendar_sum_after_prev
import churchpresentermobile.composeapp.generated.resources.calendar_sum_loops
import churchpresentermobile.composeapp.generated.resources.calendar_sum_own_length
import churchpresentermobile.composeapp.generated.resources.calendar_sum_plays
import churchpresentermobile.composeapp.generated.resources.calendar_sum_runs
import churchpresentermobile.composeapp.generated.resources.calendar_sum_starts_at
import churchpresentermobile.composeapp.generated.resources.calendar_sum_starts_cued
import churchpresentermobile.composeapp.generated.resources.calendar_sum_then_blank
import churchpresentermobile.composeapp.generated.resources.calendar_sum_then_hold
import churchpresentermobile.composeapp.generated.resources.calendar_sum_then_next
import com.church.presenter.churchpresentermobile.calendar.clockText
import com.church.presenter.churchpresentermobile.calendar.formatDuration
import com.church.presenter.churchpresentermobile.calendar.minutesOfDay
import com.church.presenter.churchpresentermobile.calendar.timeFromMinutes
import com.church.presenter.churchpresentermobile.model.RowEnd
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

private const val SECONDS_PER_MINUTE = 60
private val RUN_CHOICES_MINUTES = listOf(5, 15, 30, 60)
private val START_OFFSETS_MINUTES = listOf(15, 5)
private val REPEAT_CHOICES = listOf(2, 3)

/**
 * What the timing panel edits: the row's timing plus its planned length. Kept apart from
 * [RowTiming] because the panel's chips are relative to the service start (`−15`, `On time`) and
 * the stored form is a wall-clock time.
 */
data class TimingDraft(
    /** Minutes relative to the service start to start on its own, or null to wait to be cued. */
    val startOffsetMinutes: Int? = null,
    val followsPrevious: Boolean = false,
    /** Planned length in seconds, or null for the item's own length. */
    val runSeconds: Int? = null,
    val repeats: Int = 1,
    val atEnd: String = RowEnd.HOLD,
) {
    fun toTiming(serviceStart: String): RowTiming {
        val startMinutes = minutesOfDay(serviceStart)
        val startAt = if (startOffsetMinutes != null && startMinutes != null) timeFromMinutes(startMinutes + startOffsetMinutes) else ""
        return RowTiming(
            startAt = startAt,
            followsPrevious = followsPrevious && startAt.isEmpty(),
            repeats = repeats,
            atEnd = atEnd,
        )
    }

    companion object {
        fun of(timing: RowTiming, plannedSeconds: Int?, serviceStart: String): TimingDraft {
            val startMinutes = minutesOfDay(serviceStart)
            val rowMinutes = timing.startAt.takeIf { it.isNotEmpty() }?.let(::minutesOfDay)
            return TimingDraft(
                startOffsetMinutes = if (rowMinutes != null && startMinutes != null) rowMinutes - startMinutes else null,
                followsPrevious = timing.followsPrevious,
                runSeconds = plannedSeconds,
                repeats = timing.repeats,
                atEnd = timing.atEnd,
            )
        }
    }
}

/** Starts · Runs · Repeats · At end, as chip rows, with the whole thing read back in one line. */
@Composable
internal fun TimingPanel(
    draft: TimingDraft,
    onChange: (TimingDraft) -> Unit,
    serviceStart: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TimingRow(stringResource(Res.string.calendar_starts)) {
            val cued = draft.startOffsetMinutes == null && !draft.followsPrevious
            TimingChip(stringResource(Res.string.calendar_chip_cued), cued, enabled) {
                onChange(draft.copy(startOffsetMinutes = null, followsPrevious = false))
            }
            TimingChip(stringResource(Res.string.calendar_chip_after_prev), draft.followsPrevious, enabled) {
                onChange(draft.copy(startOffsetMinutes = null, followsPrevious = true))
            }
            START_OFFSETS_MINUTES.forEach { minutes ->
                TimingChip(stringResource(Res.string.calendar_chip_minus_minutes, minutes), draft.startOffsetMinutes == -minutes, enabled) {
                    onChange(draft.copy(startOffsetMinutes = -minutes, followsPrevious = false))
                }
            }
            TimingChip(stringResource(Res.string.calendar_chip_on_time), draft.startOffsetMinutes == 0, enabled) {
                onChange(draft.copy(startOffsetMinutes = 0, followsPrevious = false))
            }
        }
        TimingRow(stringResource(Res.string.calendar_runs)) {
            TimingChip(stringResource(Res.string.calendar_chip_its_own), draft.runSeconds == null, enabled) {
                onChange(draft.copy(runSeconds = null))
            }
            RUN_CHOICES_MINUTES.forEach { minutes ->
                val label = if (minutes == SECONDS_PER_MINUTE) {
                    stringResource(Res.string.calendar_chip_hour)
                } else {
                    stringResource(Res.string.calendar_chip_minutes, minutes)
                }
                TimingChip(label, draft.runSeconds == minutes * SECONDS_PER_MINUTE, enabled) {
                    onChange(draft.copy(runSeconds = minutes * SECONDS_PER_MINUTE))
                }
            }
        }
        TimingRow(stringResource(Res.string.calendar_repeats)) {
            TimingChip(stringResource(Res.string.calendar_chip_once), draft.repeats == 1, enabled) { onChange(draft.copy(repeats = 1)) }
            TimingChip(stringResource(Res.string.calendar_chip_loop), draft.repeats == 0, enabled) { onChange(draft.copy(repeats = 0)) }
            REPEAT_CHOICES.forEach { n ->
                TimingChip(n.toString(), draft.repeats == n, enabled) { onChange(draft.copy(repeats = n)) }
            }
        }
        TimingRow(stringResource(Res.string.calendar_at_end)) {
            TimingChip(stringResource(Res.string.calendar_chip_hold), draft.atEnd == RowEnd.HOLD, enabled) {
                onChange(draft.copy(atEnd = RowEnd.HOLD))
            }
            TimingChip(stringResource(Res.string.calendar_chip_next_item), draft.atEnd == RowEnd.NEXT, enabled) {
                onChange(draft.copy(atEnd = RowEnd.NEXT))
            }
            TimingChip(stringResource(Res.string.calendar_chip_blank), draft.atEnd == RowEnd.BLANK, enabled) {
                onChange(draft.copy(atEnd = RowEnd.BLANK))
            }
        }
        Text(
            text = timingSummary(draft, serviceStart),
            color = LocalAppColors.current.muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun TimingRow(label: String, chips: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label.uppercase(),
            color = colors.muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.06.em,
            modifier = Modifier.width(56.dp),
        )
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) { chips() }
    }
}

@Composable
private fun TimingChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    CalendarChip(label = label, selected = selected, onClick = { if (enabled) onClick() }, dim = true)
}

/** `Starts when cued · uses the item’s own length · then holds`. */
@Composable
internal fun timingSummary(draft: TimingDraft, serviceStart: String): String {
    val starts = when {
        draft.startOffsetMinutes != null -> {
            val startMinutes = minutesOfDay(serviceStart)
            val at = if (startMinutes != null) clockText(timeFromMinutes(startMinutes + draft.startOffsetMinutes)) else ""
            stringResource(Res.string.calendar_sum_starts_at, at)
        }
        draft.followsPrevious -> stringResource(Res.string.calendar_sum_after_prev)
        else -> stringResource(Res.string.calendar_sum_starts_cued)
    }
    val length = when {
        draft.repeats == 0 -> stringResource(Res.string.calendar_sum_loops)
        draft.repeats > 1 -> stringResource(Res.string.calendar_sum_plays, draft.repeats)
        draft.runSeconds != null -> stringResource(Res.string.calendar_sum_runs, formatDuration(draft.runSeconds))
        else -> stringResource(Res.string.calendar_sum_own_length)
    }
    val then = stringResource(
        when (draft.atEnd) {
            RowEnd.NEXT -> Res.string.calendar_sum_then_next
            RowEnd.BLANK -> Res.string.calendar_sum_then_blank
            else -> Res.string.calendar_sum_then_hold
        },
    )
    return "$starts · $length · $then"
}
