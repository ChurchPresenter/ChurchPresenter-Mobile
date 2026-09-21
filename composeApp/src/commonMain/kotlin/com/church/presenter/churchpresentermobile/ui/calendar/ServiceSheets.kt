package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_add_service
import churchpresentermobile.composeapp.generated.resources.calendar_blank_service
import churchpresentermobile.composeapp.generated.resources.calendar_blank_service_hint
import churchpresentermobile.composeapp.generated.resources.calendar_cancel
import churchpresentermobile.composeapp.generated.resources.calendar_delete_service
import churchpresentermobile.composeapp.generated.resources.calendar_edit_service
import churchpresentermobile.composeapp.generated.resources.calendar_item_count_one
import churchpresentermobile.composeapp.generated.resources.calendar_item_count_other
import churchpresentermobile.composeapp.generated.resources.calendar_kind_midweek_short
import churchpresentermobile.composeapp.generated.resources.calendar_kind_special_short
import churchpresentermobile.composeapp.generated.resources.calendar_kind_sunday_short
import churchpresentermobile.composeapp.generated.resources.calendar_name
import churchpresentermobile.composeapp.generated.resources.calendar_new_service
import churchpresentermobile.composeapp.generated.resources.calendar_save
import churchpresentermobile.composeapp.generated.resources.calendar_start_from
import churchpresentermobile.composeapp.generated.resources.calendar_start_time
import churchpresentermobile.composeapp.generated.resources.calendar_template_hint
import churchpresentermobile.composeapp.generated.resources.calendar_type
import com.church.presenter.churchpresentermobile.calendar.clockText
import com.church.presenter.churchpresentermobile.calendar.longDate
import com.church.presenter.churchpresentermobile.calendar.parseClockText
import com.church.presenter.churchpresentermobile.calendar.storedTime
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.SavedTemplate
import com.church.presenter.churchpresentermobile.model.ServiceKind
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private const val DEFAULT_START = "10:00"

/** What the new-service form produces. */
internal class ServiceDraft(val name: String, val startTime: String, val kind: String, val templateId: String?)

/** "New service" on a date: name, start time, type, and what to start from. */
@Composable
internal fun NewServiceSheet(
    date: LocalDate,
    templates: List<SavedTemplate>,
    defaultName: String,
    onCreate: (ServiceDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    CalendarSheet(onDismiss) {
        ServiceForm(
            title = stringResource(Res.string.calendar_new_service),
            subtitle = longDate(date),
            initialName = defaultName,
            initialStart = DEFAULT_START,
            initialKind = ServiceKind.SUNDAY.id,
            templates = templates,
            confirmLabel = stringResource(Res.string.calendar_add_service),
            onConfirm = onCreate,
            onDismiss = onDismiss,
        )
    }
}

/** The same form over an existing service, plus Delete. */
@Composable
internal fun EditServiceSheet(
    service: PlannedService,
    onSave: (ServiceDraft) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    CalendarSheet(onDismiss) {
        ServiceForm(
            title = stringResource(Res.string.calendar_edit_service),
            subtitle = service.date.let { d -> runCatching { longDate(LocalDate.parse(d)) }.getOrDefault(d) },
            initialName = service.name,
            initialStart = service.startTime,
            initialKind = service.kind,
            templates = emptyList(),
            confirmLabel = stringResource(Res.string.calendar_save),
            onConfirm = onSave,
            onDismiss = onDismiss,
            onDelete = onDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalAppColors.current.sheetBackground,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = PagePadding)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun ServiceForm(
    title: String,
    subtitle: String,
    initialName: String,
    initialStart: String,
    initialKind: String,
    templates: List<SavedTemplate>,
    confirmLabel: String,
    onConfirm: (ServiceDraft) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var startText by remember { mutableStateOf(clockText(initialStart)) }
    var kindIndex by remember { mutableStateOf(ServiceKind.entries.indexOf(ServiceKind.byId(initialKind))) }
    var templateId by remember { mutableStateOf<String?>(null) }
    val parsedStart = parseClockText(startText)
    val valid = name.isNotBlank() && parsedStart != null

    SheetTitle(title, onClose = onDismiss, subtitle = subtitle)
    Spacer(Modifier.height(14.dp))
    CompactField(stringResource(Res.string.calendar_name), name, { name = it }, highlighted = true)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CompactField(
            label = stringResource(Res.string.calendar_start_time),
            value = startText,
            onValueChange = { startText = it },
            modifier = Modifier.weight(1f),
            highlighted = parsedStart == null,
        )
        Column(modifier = Modifier.weight(1.4f)) {
            CalendarOverline(stringResource(Res.string.calendar_type))
            Spacer(Modifier.height(6.dp))
            SegmentRow(
                options = listOf(
                    stringResource(Res.string.calendar_kind_sunday_short),
                    stringResource(Res.string.calendar_kind_midweek_short),
                    stringResource(Res.string.calendar_kind_special_short),
                ),
                selected = kindIndex,
                onSelect = { kindIndex = it },
            )
        }
    }
    if (templates.isNotEmpty() || onDelete == null) {
        Spacer(Modifier.height(14.dp))
        CalendarOverline(stringResource(Res.string.calendar_start_from))
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceCard(
                title = stringResource(Res.string.calendar_blank_service),
                subtitle = stringResource(Res.string.calendar_blank_service_hint),
                selected = templateId == null,
                onClick = { templateId = null },
            )
            templates.forEach { template ->
                ChoiceCard(
                    title = template.name,
                    subtitle = stringResource(Res.string.calendar_template_hint, clockText(template.startTime), itemCountText(template.rows)),
                    selected = templateId == template.id,
                    onClick = { templateId = template.id },
                )
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onDelete != null) {
            CalendarSecondaryButton(
                label = null,
                icon = Icons.Outlined.Delete,
                contentDescription = stringResource(Res.string.calendar_delete_service),
                onClick = onDelete,
            )
        }
        CalendarSecondaryButton(stringResource(Res.string.calendar_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
        CalendarPrimaryButton(
            label = confirmLabel,
            enabled = valid,
            onClick = {
                val start = parsedStart ?: return@CalendarPrimaryButton
                onConfirm(ServiceDraft(name.trim(), storedTime(start), ServiceKind.entries[kindIndex].id, templateId))
            },
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
internal fun itemCountText(rows: List<PlanRow>): String {
    val count = rows.count { it !is PlanRow.Section }
    return if (count == 1) stringResource(Res.string.calendar_item_count_one) else stringResource(Res.string.calendar_item_count_other, count)
}
