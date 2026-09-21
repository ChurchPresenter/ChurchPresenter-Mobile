package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_add_service
import churchpresentermobile.composeapp.generated.resources.calendar_default_service_name
import churchpresentermobile.composeapp.generated.resources.calendar_month_summary
import churchpresentermobile.composeapp.generated.resources.calendar_sync_cd
import churchpresentermobile.composeapp.generated.resources.calendar_title
import churchpresentermobile.composeapp.generated.resources.calendar_today
import churchpresentermobile.composeapp.generated.resources.cd_back
import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.calendar.longDate
import com.church.presenter.churchpresentermobile.calendar.monthTitle
import com.church.presenter.churchpresentermobile.calendar.parseStoredDate
import com.church.presenter.churchpresentermobile.calendar.storedDate
import com.church.presenter.churchpresentermobile.calendar.today
import com.church.presenter.churchpresentermobile.calendar.sync.CalendarSyncEngine
import com.church.presenter.churchpresentermobile.calendar.sync.CalendarSyncState
import com.church.presenter.churchpresentermobile.calendar.sync.EnrollService
import com.church.presenter.churchpresentermobile.calendar.sync.SyncStatus
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.network.BibleCatalog
import com.church.presenter.churchpresentermobile.network.SongCatalog
import com.church.presenter.churchpresentermobile.ui.AppBackHandler
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.CalendarViewModel
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private val MonthPaneWidth = 320.dp

/**
 * The Calendar: planned services on a month grid, each with a run of show.
 *
 * On a phone the month and the open service are two screens; beside them on a tablet they are
 * two panes. Both draw their own header — the month view's carries Today, and the run of show's
 * carries the service's name and its Armed switch — so the shell's header stays out of the way.
 *
 * @param onBack What the month view's back arrow does on a phone; null hides it.
 * @param onLoadIntoSchedule Loads a run of show into the desktop's schedule; null while there is no desktop to load into.
 */
@Composable
fun CalendarScreen(
    repository: CalendarRepository,
    songCatalog: SongCatalog?,
    bibleCatalog: BibleCatalog?,
    settings: AppSettings,
    twoPane: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onLoadIntoSchedule: ((PlannedService) -> Unit)? = null,
) {
    val viewModel: CalendarViewModel = viewModel(key = "calendar") {
        val syncState = { CalendarSyncState.fromJson(settings.calendarSyncJson) }
        val saveSync: (CalendarSyncState) -> Unit = { settings.calendarSyncJson = it.toJson() }
        CalendarViewModel(
            repository = repository,
            songCatalog = songCatalog,
            bibleCatalog = bibleCatalog,
            sync = CalendarSyncEngine(repository, syncState, saveSync),
            enrollService = EnrollService(settings),
            deviceName = { settings.reportedDeviceName },
            saveEnrollment = saveSync,
        )
    }
    val syncStatus by viewModel.syncStatus.collectAsState()
    val enrollFlow by viewModel.enrollment.collectAsState()
    var syncSheet by remember { mutableStateOf(false) }
    val document by viewModel.document.collectAsState()
    val month by viewModel.visibleMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val openId by viewModel.openServiceId.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val books by viewModel.books.collectAsState()
    val sources = remember(songs, books, document.presets) {
        PickerSources(songs, books, document.presets, viewModel::chapterPreview)
    }
    val openService = openId?.let { document.serviceById(it) }
    var newServiceSheet by remember { mutableStateOf(false) }

    val runActions: (PlannedService) -> RunOfShowActions = { service ->
        RunOfShowActions(
            onArmed = { viewModel.setArmed(service.id, it) },
            onAddRow = { row, seconds, timing -> viewModel.addRow(service.id, row, seconds, timing) },
            onUpdateRow = { viewModel.updateRow(service.id, it.row, it.seconds, it.timing) },
            onRemoveRow = { viewModel.removeRow(service.id, it) },
            onMoveRow = { from, to -> viewModel.moveRow(service.id, from, to) },
            onCopy = { viewModel.copyService(service.id, it.rule, it.count, it.includeRows, it.includeCues) },
            onUpdateService = { draft ->
                viewModel.updateService(service.copy(name = draft.name, startTime = draft.startTime, kind = draft.kind))
            },
            onDelete = { viewModel.deleteService(service.id) },
            onLoadIntoSchedule = onLoadIntoSchedule?.let { load -> { load(service) } },
        )
    }

    val monthServices = document.services.filter { parseStoredDate(it.date)?.let(month::contains) == true }
    val dayServices = document.servicesOn(storedDate(selectedDate))
    val canCopyLast = viewModel.lastServiceLike(selectedDate) != null

    if (twoPane) {
        Row(modifier = modifier.fillMaxSize().background(LocalAppColors.current.background)) {
            Column(modifier = Modifier.width(MonthPaneWidth).fillMaxHeight().verticalScroll(rememberScrollState())) {
                MonthHeader(
                    serviceCount = monthServices.size,
                    monthName = monthTitle(month),
                    onToday = viewModel::goToToday,
                    onBack = null,
                    compact = true,
                    onSync = { syncSheet = true },
                    synced = syncStatus is SyncStatus.Synced || syncStatus is SyncStatus.Syncing,
                )
                Column(modifier = Modifier.padding(horizontal = PagePadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    MonthGrid(month, selectedDate, today(), monthServices, viewModel::select, viewModel::showPreviousMonth, viewModel::showNextMonth)
                    ServiceTypesLegend(monthServices)
                    Spacer(Modifier.height(8.dp))
                }
            }
            VerticalDivider(color = LocalAppColors.current.borderSubtle)
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                DayHeader(selectedDate, dayServices, onAdd = { newServiceSheet = true })
                val shown = openService?.takeIf { it.date == storedDate(selectedDate) } ?: dayServices.firstOrNull()
                if (dayServices.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = PagePadding).padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        dayServices.forEach { service ->
                            ServiceCard(
                                service = service,
                                selected = service.id == shown?.id,
                                onClick = { viewModel.openService(service.id) },
                                modifier = Modifier.width(220.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = LocalAppColors.current.borderSubtle)
                if (shown != null) {
                    RunOfShowScreen(shown, sources, viewModel::newRowId, runActions(shown), modifier = Modifier.weight(1f), inline = true)
                } else {
                    Box(modifier = Modifier.weight(1f).padding(PagePadding)) {
                        DayServices(
                            date = selectedDate,
                            services = emptyList(),
                            selectedId = null,
                            onOpen = {},
                            onAdd = { newServiceSheet = true },
                            onCopyLast = if (canCopyLast) ({ viewModel.copyLastInto(selectedDate) }) else null,
                        )
                    }
                }
            }
        }
    } else if (openService != null) {
        AppBackHandler(enabled = true) { viewModel.closeService() }
        RunOfShowScreen(openService, sources, viewModel::newRowId, runActions(openService), modifier = modifier, onBack = viewModel::closeService)
    } else {
        Column(modifier = modifier.fillMaxSize().background(LocalAppColors.current.background)) {
            MonthHeader(
                serviceCount = monthServices.size,
                monthName = monthTitle(month),
                onToday = viewModel::goToToday,
                onBack = onBack,
                compact = false,
                onSync = { syncSheet = true },
                synced = syncStatus is SyncStatus.Synced || syncStatus is SyncStatus.Syncing,
            )
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = PagePadding),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                MonthGrid(month, selectedDate, today(), monthServices, viewModel::select, viewModel::showPreviousMonth, viewModel::showNextMonth)
                DayServices(
                    date = selectedDate,
                    services = dayServices,
                    selectedId = null,
                    onOpen = { viewModel.openService(it.id) },
                    onAdd = { newServiceSheet = true },
                    onCopyLast = if (canCopyLast) ({ viewModel.copyLastInto(selectedDate) }) else null,
                )
                ServiceTypesLegend(monthServices)
                Spacer(Modifier.height(8.dp))
            }
            // An empty day already offers Add service in its body; a second one under it is noise.
            if (dayServices.isNotEmpty()) {
                HorizontalDivider(color = LocalAppColors.current.borderSubtle)
                AddServiceBar(onAdd = { newServiceSheet = true }, modifier = Modifier.padding(PagePadding).navigationBarsPadding())
            }
        }
    }

    if (syncSheet) {
        CalendarSyncSheet(
            status = syncStatus,
            flow = enrollFlow,
            canReachDesktop = settings.host.isNotBlank(),
            actions = SyncActions(
                onEnroll = viewModel::startEnrollment,
                onScanned = viewModel::completeEnrollment,
                onReset = viewModel::resetEnrollment,
                onSyncNow = viewModel::syncNow,
                onLeave = { viewModel.leaveSync(); syncSheet = false },
            ),
            onDismiss = { syncSheet = false; viewModel.resetEnrollment() },
        )
    }

    if (newServiceSheet) {
        NewServiceSheet(
            date = selectedDate,
            templates = document.templates,
            defaultName = stringResource(Res.string.calendar_default_service_name),
            onCreate = { draft ->
                viewModel.addService(draft.name, draft.startTime, draft.kind, draft.templateId)
                newServiceSheet = false
            },
            onDismiss = { newServiceSheet = false },
        )
    }
}

@Composable
private fun MonthHeader(
    serviceCount: Int,
    monthName: String,
    onToday: () -> Unit,
    onBack: (() -> Unit)?,
    compact: Boolean,
    onSync: () -> Unit,
    synced: Boolean,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (compact) Modifier else Modifier.statusBarsPadding())
            .padding(horizontal = PagePadding, vertical = if (compact) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.cd_back),
                tint = colors.accent,
                modifier = Modifier.size(22.dp).clickable(onClick = onBack),
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.calendar_title), color = colors.text, fontSize = if (compact) 18.sp else 22.sp, fontWeight = FontWeight.Bold)
            MutedText(stringResource(Res.string.calendar_month_summary, serviceCountText(serviceCount), monthName))
        }
        CalendarSecondaryButton(stringResource(Res.string.calendar_today), onClick = onToday)
        Spacer(Modifier.width(8.dp))
        CalendarSecondaryButton(
            label = null,
            icon = if (synced) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
            contentDescription = stringResource(Res.string.calendar_sync_cd),
            onClick = onSync,
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, services: List<PlannedService>, onAdd: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(longDate(date), color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            MutedText(serviceCountText(services.size))
        }
        CalendarPrimaryButton(stringResource(Res.string.calendar_add_service), icon = Icons.Filled.Add, onClick = onAdd)
    }
}
