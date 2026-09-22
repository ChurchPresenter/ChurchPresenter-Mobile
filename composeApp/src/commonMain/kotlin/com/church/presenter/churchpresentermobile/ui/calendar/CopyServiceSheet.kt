package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_cancel
import churchpresentermobile.composeapp.generated.resources.calendar_copy_title
import churchpresentermobile.composeapp.generated.resources.calendar_create_n
import churchpresentermobile.composeapp.generated.resources.calendar_creates_one
import churchpresentermobile.composeapp.generated.resources.calendar_creates_other
import churchpresentermobile.composeapp.generated.resources.calendar_how_many_times
import churchpresentermobile.composeapp.generated.resources.calendar_include
import churchpresentermobile.composeapp.generated.resources.calendar_include_cues
import churchpresentermobile.composeapp.generated.resources.calendar_include_cues_hint
import churchpresentermobile.composeapp.generated.resources.calendar_include_rows
import churchpresentermobile.composeapp.generated.resources.calendar_include_rows_hint
import churchpresentermobile.composeapp.generated.resources.calendar_repeat
import churchpresentermobile.composeapp.generated.resources.calendar_repeat_fortnightly
import churchpresentermobile.composeapp.generated.resources.calendar_repeat_monthly
import churchpresentermobile.composeapp.generated.resources.calendar_repeat_once
import churchpresentermobile.composeapp.generated.resources.calendar_repeat_weekly
import com.church.presenter.churchpresentermobile.calendar.RepeatRule
import com.church.presenter.churchpresentermobile.calendar.longDate
import com.church.presenter.churchpresentermobile.calendar.repeatDates
import com.church.presenter.churchpresentermobile.calendar.shortDate
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private const val DEFAULT_COUNT = 4
private const val MAX_COUNT = 52

/** What the copy sheet decided. */
internal class CopyChoice(val rule: RepeatRule, val count: Int, val includeRows: Boolean, val includeCues: Boolean)

/** "Copy Sunday Morning": into next week, or repeating weekly, fortnightly or monthly. */
@Composable
internal fun CopyServiceSheet(service: PlannedService, onCopy: (CopyChoice) -> Unit, onDismiss: () -> Unit) {
    CalendarSheet(onDismiss) { CopyServiceContent(service, onCopy, onDismiss) }
}

/** The copy form without the sheet around it, so its repeat rules can be driven in a test. */
@Composable
internal fun CopyServiceContent(service: PlannedService, onCopy: (CopyChoice) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    var rule by remember { mutableStateOf(RepeatRule.WEEKLY) }
    var countText by remember { mutableStateOf(DEFAULT_COUNT.toString()) }
    var includeRows by remember { mutableStateOf(true) }
    var includeCues by remember { mutableStateOf(true) }
    val count = countText.toIntOrNull()?.coerceIn(1, MAX_COUNT) ?: 1
    val from = runCatching { LocalDate.parse(service.date) }.getOrNull()
    val dates = from?.let { repeatDates(it, rule, count) }.orEmpty()
    val cueCount = service.rows.count { it is PlanRow.Ref && it.kind == RowKind.CUE }

    SheetTitle(
        title = stringResource(Res.string.calendar_copy_title, service.name),
        subtitle = "${from?.let(::shortDate) ?: service.date} · ${itemCountText(service.rows)}",
        onClose = onDismiss,
    )
    Spacer(Modifier.height(14.dp))
    CalendarOverline(stringResource(Res.string.calendar_repeat))
    Spacer(Modifier.height(6.dp))
    SegmentRow(
        options = listOf(
            stringResource(Res.string.calendar_repeat_once),
            stringResource(Res.string.calendar_repeat_weekly),
            stringResource(Res.string.calendar_repeat_fortnightly),
            stringResource(Res.string.calendar_repeat_monthly),
        ),
        selected = RepeatRule.entries.indexOf(rule),
        onSelect = { rule = RepeatRule.entries[it] },
        tagFor = CalendarTags::repeatRule,
    )
    if (rule != RepeatRule.ONCE) {
        Spacer(Modifier.height(12.dp))
        HowManyTimesRow(countText, onCount = { countText = it })
    }
    Spacer(Modifier.height(12.dp))
    DatesPreview(dates)
    Spacer(Modifier.height(14.dp))
    CalendarOverline(stringResource(Res.string.calendar_include))
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CheckCard(
            title = stringResource(Res.string.calendar_include_rows),
            subtitle = stringResource(Res.string.calendar_include_rows_hint, service.rows.size),
            checked = includeRows,
            onToggle = { includeRows = !includeRows },
            modifier = Modifier.testTag(CalendarTags.COPY_INCLUDE_ROWS),
        )
        // Only where there is automation to carry: a service with no cues has nothing to ask.
        if (cueCount > 0) {
            CheckCard(
                title = stringResource(Res.string.calendar_include_cues),
                subtitle = stringResource(Res.string.calendar_include_cues_hint, cueCount),
                checked = includeCues,
                onToggle = { includeCues = !includeCues },
                modifier = Modifier.testTag(CalendarTags.COPY_INCLUDE_CUES),
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CalendarSecondaryButton(
            stringResource(Res.string.calendar_cancel),
            onClick = onDismiss,
            modifier = Modifier.weight(1f).testTag(CalendarTags.COPY_CANCEL),
        )
        CalendarPrimaryButton(
            label = stringResource(Res.string.calendar_create_n, dates.size),
            onClick = { onCopy(CopyChoice(rule, count, includeRows, includeCues)) },
            modifier = Modifier.weight(CONFIRM_WIDTH).testTag(CalendarTags.COPY_CONFIRM),
        )
    }

}

/** How many copies, asked only when the repeat makes more than one. */
@Composable
private fun HowManyTimesRow(countText: String, onCount: (String) -> Unit) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.calendar_how_many_times),
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        CompactField(
            label = "",
            value = countText,
            onValueChange = { onCount(it.filter { c -> c.isDigit() }.take(COUNT_DIGITS)) },
            keyboardType = KeyboardType.Number,
            modifier = Modifier.width(COUNT_FIELD_WIDTH).testTag(CalendarTags.COPY_COUNT),
        )
    }
}

/** The dates this copy would create, numbered, so nobody presses Create to find out. */
@Composable
private fun DatesPreview(dates: List<LocalDate>) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CalendarOverline(
            if (dates.size == 1) {
                stringResource(Res.string.calendar_creates_one)
            } else {
                stringResource(Res.string.calendar_creates_other, dates.size)
            },
        )
        dates.forEachIndexed { index, date ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}", color = colors.muted, fontSize = 12.sp, modifier = Modifier.width(ORDINAL_WIDTH))
                Text(longDate(date), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Create sits wider than Cancel beside it. */
private const val CONFIRM_WIDTH = 1.4f
private const val COUNT_DIGITS = 2
private val COUNT_FIELD_WIDTH = 72.dp
private val ORDINAL_WIDTH = 16.dp
