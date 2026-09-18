package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.editor_cancel
import churchpresentermobile.composeapp.generated.resources.report_all_bibles
import churchpresentermobile.composeapp.generated.resources.report_all_songbooks
import churchpresentermobile.composeapp.generated.resources.report_clear
import churchpresentermobile.composeapp.generated.resources.report_clear_body
import churchpresentermobile.composeapp.generated.resources.report_clear_confirm
import churchpresentermobile.composeapp.generated.resources.report_clear_title
import churchpresentermobile.composeapp.generated.resources.report_export_csv
import churchpresentermobile.composeapp.generated.resources.report_export_xls
import churchpresentermobile.composeapp.generated.resources.report_from
import churchpresentermobile.composeapp.generated.resources.report_no_bible
import churchpresentermobile.composeapp.generated.resources.report_no_songbook
import churchpresentermobile.composeapp.generated.resources.report_pick_ok
import churchpresentermobile.composeapp.generated.resources.report_range_12m
import churchpresentermobile.composeapp.generated.resources.report_range_3m
import churchpresentermobile.composeapp.generated.resources.report_range_6m
import churchpresentermobile.composeapp.generated.resources.report_range_all
import churchpresentermobile.composeapp.generated.resources.report_range_all_time
import churchpresentermobile.composeapp.generated.resources.report_range_last_12_months
import churchpresentermobile.composeapp.generated.resources.report_range_last_3_months
import churchpresentermobile.composeapp.generated.resources.report_range_last_6_months
import churchpresentermobile.composeapp.generated.resources.report_range_year
import churchpresentermobile.composeapp.generated.resources.report_standalone_only
import churchpresentermobile.composeapp.generated.resources.report_tab_activity
import churchpresentermobile.composeapp.generated.resources.report_tab_bible
import churchpresentermobile.composeapp.generated.resources.report_tab_count
import churchpresentermobile.composeapp.generated.resources.report_tab_songs
import churchpresentermobile.composeapp.generated.resources.report_to
import com.church.presenter.churchpresentermobile.model.ReportDates
import com.church.presenter.churchpresentermobile.model.ReportPreset
import com.church.presenter.churchpresentermobile.ui.OutlineActionButton
import com.church.presenter.churchpresentermobile.ui.SegmentedControl
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.CcliReportViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ReportData
import com.church.presenter.churchpresentermobile.viewmodel.ReportTab
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime

/** The quick ranges in chip order. Year is a menu, not a chip, so it is not here. */
private val QUICK_PRESETS = listOf(
    ReportPreset.LAST_3_MONTHS,
    ReportPreset.LAST_6_MONTHS,
    ReportPreset.LAST_12_MONTHS,
    ReportPreset.ALL_TIME,
)

/**
 * Everything above the report: the standalone badge, the range, the view and
 * the content filter. Reads and drives the ViewModel directly so the screen
 * body stays about layout.
 *
 * @param wide On a tablet the chips carry their long labels ("Last 3 Months").
 */
@Composable
internal fun ReportFilters(vm: CcliReportViewModel, report: ReportData, wide: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val filters by vm.filters.collectAsState()
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.report_standalone_only).uppercase(),
            color = colors.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.em,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            QUICK_PRESETS.forEachIndexed { index, preset ->
                PresetChip(
                    label = presetLabel(preset, wide),
                    selected = filters.preset == preset,
                    onClick = { vm.setPreset(preset) },
                    modifier = Modifier.testTag(ReportTags.preset(index)),
                )
            }
            YearMenu(
                years = report.years,
                selected = filters.preset == ReportPreset.YEAR,
                onPick = vm::setYear,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val from = stringResource(Res.string.report_from)
            val to = stringResource(Res.string.report_to)
            DateButton(from, filters.range.from, vm::setFrom, ReportTags.FROM, Modifier.weight(1f))
            DateButton(to, filters.range.to, vm::setTo, ReportTags.TO, Modifier.weight(1f))
        }
        val songsTab = stringResource(Res.string.report_tab_songs)
        val bibleTab = stringResource(Res.string.report_tab_bible)
        SegmentedControl(
            options = listOf(
                stringResource(Res.string.report_tab_count, songsTab, report.songs.size),
                stringResource(Res.string.report_tab_count, bibleTab, report.verses.size),
                stringResource(Res.string.report_tab_activity),
            ),
            selectedIndex = ReportTab.entries.indexOf(filters.tab),
            onSelect = { vm.setTab(ReportTab.entries[it]) },
            optionTag = ReportTags::tab,
        )
        when (filters.tab) {
            ReportTab.SONGS -> ContentFilterMenu(
                allLabel = stringResource(Res.string.report_all_songbooks),
                blankLabel = stringResource(Res.string.report_no_songbook),
                options = report.songbooks,
                selected = filters.songbook,
                onPick = vm::setSongbook,
            )
            ReportTab.BIBLE -> ContentFilterMenu(
                allLabel = stringResource(Res.string.report_all_bibles),
                blankLabel = stringResource(Res.string.report_no_bible),
                options = report.bibles,
                selected = filters.bible,
                onPick = vm::setBible,
            )
            ReportTab.ACTIVITY -> Unit
        }
    }
}

@Composable
private fun presetLabel(preset: ReportPreset, wide: Boolean): String = when (preset) {
    ReportPreset.LAST_3_MONTHS ->
        stringResource(if (wide) Res.string.report_range_last_3_months else Res.string.report_range_3m)
    ReportPreset.LAST_6_MONTHS ->
        stringResource(if (wide) Res.string.report_range_last_6_months else Res.string.report_range_6m)
    ReportPreset.LAST_12_MONTHS ->
        stringResource(if (wide) Res.string.report_range_last_12_months else Res.string.report_range_12m)
    ReportPreset.ALL_TIME -> stringResource(if (wide) Res.string.report_range_all_time else Res.string.report_range_all)
    ReportPreset.YEAR -> stringResource(Res.string.report_range_year)
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(9.dp)
    Text(
        text = label,
        color = if (selected) colors.onAccent else colors.secondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .semantics { this.selected = selected }
            .clip(shape)
            .background(if (selected) colors.accent else colors.inputBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** "Year ▾" — a menu of the years with plays in them. Highlighted while one is the range. */
@Composable
private fun YearMenu(years: List<Int>, selected: Boolean, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .testTag(ReportTags.YEAR)
                .clip(RoundedCornerShape(9.dp))
                .background(if (selected) colors.accent else colors.inputBg)
                .clickable(enabled = years.isNotEmpty()) { expanded = true }
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.report_range_year),
                color = if (selected) colors.onAccent else colors.secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = if (selected) colors.onAccent else colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = { onPick(year); expanded = false },
                    modifier = Modifier.testTag(ReportTags.year(year)),
                )
            }
        }
    }
}

/** "From · Jan 1, 2026" — opens a date picker. */
@Composable
private fun DateButton(
    label: String,
    day: LocalDate,
    onPick: (LocalDate) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var picking by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .testTag(tag)
            .clip(RoundedCornerShape(11.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(11.dp))
            .clickable { picking = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), color = colors.muted, fontSize = 10.sp, letterSpacing = 0.05.em)
        Text(ReportDates.dateLabel(day), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    if (picking) {
        ReportDatePicker(initial = day, onPick = { onPick(it); picking = false }, onDismiss = { picking = false })
    }
}

/**
 * The Material date picker, speaking [LocalDate].
 *
 * The picker works in UTC-midnight millis whatever the device's zone, so the
 * conversion both ways goes through UTC — going through the local zone would
 * shift the chosen day by one east of Greenwich.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun ReportDatePicker(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = state.selectedDateMillis
                    if (picked == null) onDismiss() else onPick(ReportDates.dayOf(picked, TimeZone.UTC))
                },
            ) { Text(stringResource(Res.string.report_pick_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.editor_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

/** "All songbooks ▾" / "All bibles ▾" — narrows the open view to one book or translation. */
@Composable
private fun ContentFilterMenu(
    allLabel: String,
    blankLabel: String,
    options: List<String>,
    selected: String?,
    onPick: (String?) -> Unit,
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    fun nameOf(option: String) = option.ifBlank { blankLabel }
    Box {
        Row(
            modifier = Modifier
                .testTag(ReportTags.FILTER)
                .clip(RoundedCornerShape(9.dp))
                .clickable(enabled = options.isNotEmpty()) { expanded = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected?.let(::nameOf) ?: allLabel,
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel, fontWeight = if (selected == null) FontWeight.Bold else FontWeight.Normal) },
                onClick = { onPick(null); expanded = false },
                modifier = Modifier.testTag(ReportTags.filterOption(null)),
            )
            options.forEach { option ->
                val weight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                DropdownMenuItem(
                    text = { Text(nameOf(option), fontWeight = weight) },
                    onClick = { onPick(option); expanded = false },
                    modifier = Modifier.testTag(ReportTags.filterOption(option)),
                )
            }
        }
    }
}

/** The exports and the clear, pinned under the report. */
@Composable
internal fun ReportActionBar(wide: Boolean, onExportCsv: () -> Unit, onExportXls: () -> Unit, onClear: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(color = colors.borderSubtle)
        val buttons: @Composable (Modifier) -> Unit = { each ->
            OutlineActionButton(
                label = stringResource(Res.string.report_export_csv),
                icon = Icons.Outlined.Description,
                onClick = onExportCsv,
                modifier = each.testTag(ReportTags.EXPORT_CSV),
            )
            OutlineActionButton(
                label = stringResource(Res.string.report_export_xls),
                icon = Icons.Outlined.TableChart,
                onClick = onExportXls,
                modifier = each.testTag(ReportTags.EXPORT_XLS),
            )
            ClearButton(onClear, each.testTag(ReportTags.CLEAR))
        }
        if (wide) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) { buttons(Modifier.weight(1f)) }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { buttons(Modifier.fillMaxWidth()) }
        }
    }
}

/** Red-tinted, because it deletes history. */
@Composable
private fun ClearButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.danger, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.danger, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(Res.string.report_clear),
            color = colors.danger,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 9.dp),
        )
    }
}

/** Asks before the play history is deleted. There is no undo behind this. */
@Composable
internal fun ClearStatisticsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.report_clear_title)) },
        text = { Text(stringResource(Res.string.report_clear_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(ReportTags.CLEAR_CONFIRM)) {
                Text(stringResource(Res.string.report_clear_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(ReportTags.CLEAR_CANCEL)) {
                Text(stringResource(Res.string.editor_cancel))
            }
        },
    )
}
