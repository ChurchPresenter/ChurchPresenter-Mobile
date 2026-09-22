package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_duration
import churchpresentermobile.composeapp.generated.resources.calendar_move_down
import churchpresentermobile.composeapp.generated.resources.calendar_move_up
import churchpresentermobile.composeapp.generated.resources.calendar_name
import churchpresentermobile.composeapp.generated.resources.calendar_remove_row
import churchpresentermobile.composeapp.generated.resources.calendar_save_row
import churchpresentermobile.composeapp.generated.resources.calendar_section_hint
import churchpresentermobile.composeapp.generated.resources.calendar_who_or_note
import com.church.presenter.churchpresentermobile.calendar.formatDuration
import com.church.presenter.churchpresentermobile.calendar.parseDuration
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.SectionPalette
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * A tapped row: its name where it can be renamed, its length, the timing panel, and the row's
 * place in the order. Desktop-authored rows keep their content read-only; only where and how
 * they run is this device's to change.
 */
@Composable
internal fun RowEditorSheet(
    service: PlannedService,
    row: PlanRow,
    onSave: (RowEdit) -> Unit,
    onMove: (delta: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    CalendarSheet(onDismiss) {
        RowEditorContent(service, row, onSave, onMove, onRemove, onDismiss)
    }
}

/** The editor without the sheet around it, so each row kind's form can be driven in a test. */
@Composable
internal fun RowEditorContent(
    service: PlannedService,
    row: PlanRow,
    onSave: (RowEdit) -> Unit,
    onMove: (delta: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var title by remember { mutableStateOf(row.title) }
    var detail by remember { mutableStateOf((row as? PlanRow.Ministry)?.detail.orEmpty()) }
    var color by remember { mutableStateOf((row as? PlanRow.Section)?.color ?: SectionPalette.DEFAULT) }
    var durationText by remember { mutableStateOf(service.plannedSecondsFor(row.id)?.let(::formatDuration).orEmpty()) }
    var timing by remember {
        mutableStateOf(
            TimingDraft.of(service.timingFor(row.id), service.plannedSecondsFor(row.id), service.startTime),
        )
    }
    val index = service.rows.indexOfFirst { it.id == row.id }
    val editable = row is PlanRow.Section || row is PlanRow.Ministry || row is PlanRow.Bible

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KindBadge(row.kindKey, size = 30.dp)
        SheetTitle(
            row.title,
            onClose = onDismiss,
            subtitle = rowSubtitle(row).ifEmpty { null },
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(14.dp))
    if (editable) {
        CompactField(
            stringResource(Res.string.calendar_name),
            title,
            { title = it },
            highlighted = true,
            modifier = Modifier.testTag(CalendarTags.ROW_NAME),
        )
        Spacer(Modifier.height(12.dp))
    }
    if (row is PlanRow.Ministry) {
        CompactField(
            stringResource(Res.string.calendar_who_or_note),
            detail,
            { detail = it },
            modifier = Modifier.testTag(CalendarTags.ROW_DETAIL),
        )
        Spacer(Modifier.height(12.dp))
    }
    if (row is PlanRow.Section) {
        // A heading is a divider, so there is nothing to time -- only a color to give it.
        SwatchRow(color = color, onColor = { color = it })
        Spacer(Modifier.height(8.dp))
        Text(stringResource(Res.string.calendar_section_hint), color = colors.muted, fontSize = 12.sp)
    } else {
        RowTiming(
            durationText = durationText,
            onDuration = { durationText = it },
            timing = timing,
            onTiming = { draft ->
                timing = draft
                durationText = draft.runSeconds?.let(::formatDuration).orEmpty()
            },
            serviceStart = service.startTime,
        )
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RowPlaceButtons(
            canMoveUp = index > 0,
            canMoveDown = index in 0 until service.rows.lastIndex,
            onMove = onMove,
            onRemove = onRemove,
        )
        CalendarPrimaryButton(
            label = stringResource(Res.string.calendar_save_row),
            onClick = {
                val edited = when (row) {
                    is PlanRow.Section -> row.copy(title = title.trim().ifEmpty { row.title }, color = color)
                    is PlanRow.Ministry -> row.copy(
                        title = title.trim().ifEmpty { row.title },
                        detail = detail.trim(),
                    )
                    is PlanRow.Bible -> row.copy(title = title.trim().ifEmpty { row.title })
                    else -> row
                }
                val seconds = if (row is PlanRow.Section) null else parseDuration(durationText)
                val rowTiming =
                    if (row is PlanRow.Section) RowTiming.DEFAULT else timing.toTiming(service.startTime)
                onSave(RowEdit(edited, seconds, rowTiming))
            },
            modifier = Modifier.weight(1f).testTag(CalendarTags.ROW_SAVE),
        )
    }

}

/** How long a row runs, and when it fires: the two the desktop's automation reads. */
@Composable
private fun RowTiming(
    durationText: String,
    onDuration: (String) -> Unit,
    timing: TimingDraft,
    onTiming: (TimingDraft) -> Unit,
    serviceStart: String,
) {
    CompactField(
        stringResource(Res.string.calendar_duration),
        durationText,
        onDuration,
        placeholder = "4:30",
        modifier = Modifier.testTag(CalendarTags.ROW_DURATION),
    )
    Spacer(Modifier.height(12.dp))
    TimingPanel(
        draft = timing.copy(runSeconds = parseDuration(durationText)),
        onChange = onTiming,
        serviceStart = serviceStart,
    )
}

/** Where the row sits, and whether it stays: up, down and remove, in that order. */
@Composable
private fun RowPlaceButtons(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (delta: Int) -> Unit,
    onRemove: () -> Unit,
) {
    CalendarSecondaryButton(
        label = null,
        icon = Icons.Filled.KeyboardArrowUp,
        contentDescription = stringResource(Res.string.calendar_move_up),
        onClick = { if (canMoveUp) onMove(-1) },
        modifier = Modifier.testTag(CalendarTags.ROW_UP),
    )
    CalendarSecondaryButton(
        label = null,
        icon = Icons.Filled.KeyboardArrowDown,
        contentDescription = stringResource(Res.string.calendar_move_down),
        onClick = { if (canMoveDown) onMove(1) },
        modifier = Modifier.testTag(CalendarTags.ROW_DOWN),
    )
    CalendarSecondaryButton(
        label = null,
        icon = Icons.Outlined.Delete,
        contentDescription = stringResource(Res.string.calendar_remove_row),
        onClick = onRemove,
        modifier = Modifier.testTag(CalendarTags.ROW_REMOVE),
    )
}
