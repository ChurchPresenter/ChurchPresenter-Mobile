package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.rememberLazyListState
import com.church.presenter.churchpresentermobile.ui.verticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_add_item
import churchpresentermobile.composeapp.generated.resources.calendar_armed
import churchpresentermobile.composeapp.generated.resources.calendar_auto_starts
import churchpresentermobile.composeapp.generated.resources.calendar_copy_service
import churchpresentermobile.composeapp.generated.resources.calendar_run_empty_body
import churchpresentermobile.composeapp.generated.resources.calendar_run_empty_title
import churchpresentermobile.composeapp.generated.resources.cd_back
import com.church.presenter.churchpresentermobile.calendar.autoStartCount
import com.church.presenter.churchpresentermobile.calendar.clockText
import com.church.presenter.churchpresentermobile.calendar.clockedRows
import com.church.presenter.churchpresentermobile.calendar.endMinutes
import com.church.presenter.churchpresentermobile.calendar.minutesText
import com.church.presenter.churchpresentermobile.calendar.timeFromMinutes
import com.church.presenter.churchpresentermobile.calendar.totalSeconds
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.ui.EmptyState
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

private enum class OpenSheet { NONE, ADD, COPY, EDIT_SERVICE }

/**
 * One service's run of show: colored sections, each row with its projected clock time and how it
 * runs, and the bar that adds, copies and loads. A tapped row opens its editor.
 *
 * @param onBack Drawn as a back arrow on a phone; null beside a month pane, where there is nowhere to go back to.
 * @param inline True when the rows sit beside the calendar and can be moved with buttons rather than a sheet.
 */
@Composable
internal fun RunOfShowScreen(
    service: PlannedService,
    sources: PickerSources,
    newRowId: () -> String,
    actions: RunOfShowActions,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    inline: Boolean = false,
) {
    val colors = LocalAppColors.current
    var sheet by remember { mutableStateOf(OpenSheet.NONE) }
    var editingRowId by remember { mutableStateOf<String?>(null) }
    val rows = clockedRows(service)

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        RunHeader(service, onBack = onBack, onEdit = { sheet = OpenSheet.EDIT_SERVICE }, onArmed = actions.onArmed)
        HorizontalDivider(color = colors.borderSubtle)
        if (service.rows.isEmpty()) {
            EmptyState(
                title = stringResource(Res.string.calendar_run_empty_title),
                body = stringResource(Res.string.calendar_run_empty_body),
                actionLabel = stringResource(Res.string.calendar_add_item),
                actionIcon = Icons.Filled.Add,
                onAction = { sheet = OpenSheet.ADD },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            val listState = rememberLazyListState()
            val drag = remember(listState) { RowDragState(listState) { from, to -> actions.rows.onMove(from, to) } }
            val currentIds by rememberUpdatedState(service.rows.map { it.id })
            // While a row is dragged the list follows the order being dragged, not the saved one.
            val shown = drag.order.takeIf { drag.draggingId != null }
                ?.let { order -> rows.sortedBy { order.indexOf(it.row.id) } }
                ?: rows
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScrollbar(listState),
                contentPadding = PaddingValues(horizontal = PagePadding, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.row.id }) { clocked ->
                    val index = service.rows.indexOfFirst { it.id == clocked.row.id }
                    val rowActions = RowActions(
                        onOpen = { editingRowId = clocked.row.id },
                        handle = Modifier.dragHandle(drag, clocked.row.id) { currentIds },
                        onMoveUp = if (inline && index > 0) {
                            ({ holdScroll(listState); actions.rows.onMove(index, index - 1) })
                        } else {
                            null
                        },
                        onMoveDown = if (inline && index < service.rows.lastIndex) {
                            ({ holdScroll(listState); actions.rows.onMove(index, index + 1) })
                        } else {
                            null
                        },
                        onRemove = if (inline) ({ actions.rows.onRemove(clocked.row.id) }) else null,
                    )
                    val rowModifier = Modifier.testTag(CalendarTags.row(clocked.row.id))
                        .draggableRow(drag, clocked.row.id) { currentIds }
                    when (val row = clocked.row) {
                        is PlanRow.Section -> SectionRow(row, rowActions, rowModifier)
                        else -> ItemRow(clocked, service.startTime, rowActions, rowModifier)
                    }
                }
            }
        }
        HorizontalDivider(color = colors.borderSubtle)
        RunBottomBar(
            onAdd = { sheet = OpenSheet.ADD },
            onCopy = { sheet = OpenSheet.COPY },
        )
    }

    when (sheet) {
        OpenSheet.ADD -> AddToServiceSheet(
            serviceName = service.name,
            serviceStart = service.startTime,
            sources = sources,
            newRowId = newRowId,
            onAdd = actions.rows.onAdd,
            onDismiss = { sheet = OpenSheet.NONE },
        )
        OpenSheet.COPY -> CopyServiceSheet(
            service = service,
            onCopy = { actions.onCopy(it); sheet = OpenSheet.NONE },
            onDismiss = { sheet = OpenSheet.NONE },
        )
        OpenSheet.EDIT_SERVICE -> EditServiceSheet(
            service = service,
            onSave = { actions.onUpdateService(it); sheet = OpenSheet.NONE },
            onDelete = { sheet = OpenSheet.NONE; actions.onDelete() },
            onDismiss = { sheet = OpenSheet.NONE },
        )
        OpenSheet.NONE -> Unit
    }
    val editing = editingRowId?.let { id -> service.rows.firstOrNull { it.id == id } }
    if (editing != null) {
        val index = service.rows.indexOf(editing)
        RowEditorSheet(
            service = service,
            row = editing,
            onSave = { actions.rows.onUpdate(it); editingRowId = null },
            onMove = { delta -> actions.rows.onMove(index, index + delta) },
            onRemove = { actions.rows.onRemove(editing.id); editingRowId = null },
            onDismiss = { editingRowId = null },
        )
    }
}

@Composable
private fun RunHeader(service: PlannedService, onBack: (() -> Unit)?, onEdit: () -> Unit, onArmed: (Boolean) -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onBack != null) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = PagePadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.cd_back),
                tint = colors.accent,
                modifier = Modifier.size(22.dp).testTag(CalendarTags.RUN_BACK).clickable(onClick = onBack),
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
            Text(
                service.name,
                color = colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MutedText(runSubtitle(service))
        }
        Text(
            stringResource(Res.string.calendar_armed),
            color = colors.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = service.armed,
            onCheckedChange = onArmed,
            modifier = Modifier.testTag(CalendarTags.RUN_ARMED),
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent),
        )
    }
}

/** `10:00–11:09 AM · 69 min · 3 auto starts`. */
@Composable
internal fun runSubtitle(service: PlannedService): String {
    val start = clockText(service.startTime)
    val end = endMinutes(service)?.let { clockText(timeFromMinutes(it)) }
    val parts = mutableListOf(if (end != null && totalSeconds(service) > 0) "$start–$end" else start)
    if (totalSeconds(service) > 0) parts += minutesText(totalSeconds(service))
    val auto = autoStartCount(service)
    if (auto > 0) parts += stringResource(Res.string.calendar_auto_starts, auto)
    return parts.joinToString(" · ")
}

@Composable
private fun RunBottomBar(onAdd: () -> Unit, onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CalendarSecondaryButton(
            label = null,
            icon = Icons.Filled.Add,
            contentDescription = stringResource(Res.string.calendar_add_item),
            onClick = onAdd,
            modifier = Modifier.testTag(CalendarTags.RUN_ADD),
        )
        CalendarSecondaryButton(
            label = null,
            icon = Icons.Outlined.ContentCopy,
            contentDescription = stringResource(Res.string.calendar_copy_service),
            onClick = onCopy,
            modifier = Modifier.testTag(CalendarTags.RUN_COPY),
        )
    }
}
