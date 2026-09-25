package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import churchpresentermobile.composeapp.generated.resources.cd_settings
import com.church.presenter.churchpresentermobile.ui.GearButton
import com.church.presenter.churchpresentermobile.ui.UiTags
import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.calendar.longDate
import com.church.presenter.churchpresentermobile.calendar.parseStoredDate
import com.church.presenter.churchpresentermobile.calendar.storedDate
import com.church.presenter.churchpresentermobile.calendar.sync.CalendarSyncEngine
import com.church.presenter.churchpresentermobile.calendar.sync.SongCatalogStore
import com.church.presenter.churchpresentermobile.calendar.sync.CalendarSyncState
import com.church.presenter.churchpresentermobile.calendar.sync.ClientKeySource
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

internal val MonthPaneWidth = 320.dp

/**
 * The Calendar: planned services on a month grid, each with a run of show.
 *
 * On a phone the month and the open service are two screens; beside them on a tablet they are
 * two panes. Both draw their own header — the month view's carries Today, and the run of show's
 * carries the service's name and its Armed switch — so the shell's header stays out of the way.
 *
 * @param onBack What the month view's back arrow does; null hides it.
 */
@Composable
fun CalendarScreen(
    repository: CalendarRepository,
    songCatalog: SongCatalog?,
    catalogStore: SongCatalogStore? = null,
    bibleCatalog: BibleCatalog?,
    settings: AppSettings,
    twoPane: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
) {
    val viewModel: CalendarViewModel = viewModel(key = "calendar") {
        calendarViewModel(repository, songCatalog, bibleCatalog, catalogStore, settings)
    }
    val syncStatus by viewModel.syncStatus.collectAsState()
    val nextSyncAt by viewModel.nextSyncAt.collectAsState()
    val syncNow = rememberSyncClock(active = syncStatus != SyncStatus.NotEnrolled)
    val syncView = CalendarSyncView(syncStatus, nextSyncAt, syncNow)
    val enrollFlow by viewModel.enrollment.collectAsState()
    var syncSheet by remember { mutableStateOf(false) }
    val document by viewModel.document.collectAsState()
    val month by viewModel.visibleMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val openId by viewModel.openServiceId.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val songDurations by viewModel.songDurations.collectAsState()
    val books by viewModel.books.collectAsState()
    val sources = remember(songs, songDurations, books, document.presets) {
        PickerSources(songs, books, document.presets, viewModel::chapterPreview, songDurations)
    }
    val openService = openId?.let { document.serviceById(it) }
    var newServiceSheet by remember { mutableStateOf(false) }

    val runActions: (PlannedService) -> RunOfShowActions = { service ->
        runOfShowActions(viewModel, service)
    }

    val monthServices = document.services.filter { parseStoredDate(it.date)?.let(month::contains) == true }
    val dayServices = document.servicesOn(storedDate(selectedDate))
    val canCopyLast = viewModel.services.lastLike(selectedDate) != null

    // The three shapes this screen takes, each its own composable so that what the shell draws
    // reads as a choice between them rather than as one long function with three halves.
    val paneState = CalendarMonthState(month, selectedDate, monthServices, dayServices, canCopyLast)
    val headerActions = MonthHeaderActions(
        onToday = viewModel::goToToday,
        onSync = { syncSheet = true },
        sync = syncView,
        onSettings = onSettings,
    )
    when {
        twoPane -> CalendarTwoPane(
            state = paneState,
            header = headerActions,
            viewModel = viewModel,
            sources = sources,
            openService = openService,
            runActions = runActions,
            onAddService = { newServiceSheet = true },
            modifier = modifier,
            onBack = onBack,
        )
        openService != null -> {
            AppBackHandler(enabled = true) { viewModel.closeService() }
            RunOfShowScreen(
                openService,
                sources,
                viewModel.rows::newId,
                runActions(openService),
                modifier = modifier,
                onBack = viewModel::closeService,
                sync = syncView,
                onSync = { syncSheet = true },
            )
        }
        else -> CalendarMonthPane(
            state = paneState,
            header = headerActions,
            viewModel = viewModel,
            onBack = onBack,
            onAddService = { newServiceSheet = true },
            modifier = modifier,
        )
    }

    if (syncSheet) {
        CalendarSyncSheet(
            status = syncStatus,
            flow = enrollFlow,
            canReachDesktop = settings.host.isNotBlank(),
            actions = SyncActions(
                onEnroll = viewModel.pairing::start,
                onScanned = viewModel.pairing::complete,
                onReset = viewModel.pairing::reset,
                onSyncNow = viewModel.pairing::syncNow,
                onLeave = { viewModel.pairing.leave(); syncSheet = false },
            ),
            onDismiss = { syncSheet = false; viewModel.pairing.reset() },
        )
    }

    if (newServiceSheet) {
        NewServiceSheet(
            date = selectedDate,
            templates = document.templates,
            defaultName = stringResource(Res.string.calendar_default_service_name),
            onCreate = { draft ->
                viewModel.services.add(draft.name, draft.startTime, draft.kind, draft.templateId)
                newServiceSheet = false
            },
            onDismiss = { newServiceSheet = false },
        )
    }
}

@Composable
internal fun MonthHeader(
    serviceCount: Int,
    monthName: String,
    onToday: () -> Unit,
    onBack: (() -> Unit)?,
    compact: Boolean,
    onSync: () -> Unit,
    sync: CalendarSyncView,
    onSettings: (() -> Unit)?,
) {
    val colors = LocalAppColors.current
    SyncLineAbove(
        sync = sync,
        onSync = onSync,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (compact) Modifier else Modifier.statusBarsPadding())
            .padding(horizontal = PagePadding, vertical = if (compact) 12.dp else 14.dp),
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
            Text(
                stringResource(Res.string.calendar_title),
                color = colors.text,
                fontSize = if (compact) 18.sp else 22.sp,
                fontWeight = FontWeight.Bold,
            )
            MutedText(stringResource(Res.string.calendar_month_summary, serviceCountText(serviceCount), monthName))
        }
        MonthHeaderButtons(onToday, onSync, sync.status, onSettings)
    }
}

@Composable
private fun MonthHeaderButtons(onToday: () -> Unit, onSync: () -> Unit, status: SyncStatus, onSettings: (() -> Unit)?) {
    val synced = status is SyncStatus.Synced || status is SyncStatus.Syncing
    Row(verticalAlignment = Alignment.CenterVertically) {
        CalendarSecondaryButton(stringResource(Res.string.calendar_today), onClick = onToday)
        Spacer(Modifier.width(8.dp))
        CalendarSecondaryButton(
            label = null,
            icon = if (synced) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
            contentDescription = stringResource(Res.string.calendar_sync_cd),
            onClick = onSync,
        )
        if (onSettings != null) {
            Spacer(Modifier.width(8.dp))
            CalendarSecondaryButton(
                label = null,
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(Res.string.cd_settings),
                onClick = onSettings,
            )
        }
    }
}

@Composable
internal fun DayHeader(
    date: LocalDate,
    services: List<PlannedService>,
    onAdd: () -> Unit,
    onSettings: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(longDate(date), color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            MutedText(serviceCountText(services.size))
        }
        // An empty day offers Add service in its body; a second one up here is noise.
        if (services.isNotEmpty()) {
            CalendarPrimaryButton(
                stringResource(Res.string.calendar_add_service),
                icon = Icons.Filled.Add,
                onClick = onAdd,
            )
        }
        // Beside the pane that reaches the window's right edge, the gear sits in the top-right
        // corner as it does on every other tab, rather than among the month's buttons.
        if (onSettings != null) {
            Spacer(Modifier.width(8.dp))
            GearButton(onClick = onSettings, modifier = Modifier.testTag(UiTags.HEADER_SETTINGS))
        }
    }
}

/**
 * The ViewModel this screen owns, with everything it talks to wired in.
 *
 * Out of the composable because it is a page of construction and none of it is layout: the screen
 * reads as what it draws, and what it is built from sits underneath.
 */
private fun calendarViewModel(
    repository: CalendarRepository,
    songCatalog: SongCatalog?,
    bibleCatalog: BibleCatalog?,
    catalogStore: SongCatalogStore?,
    settings: AppSettings,
): CalendarViewModel {
    val syncState = { CalendarSyncState.fromJson(settings.calendarSyncJson) }
    val saveSync: (CalendarSyncState) -> Unit = { settings.calendarSyncJson = it.toJson() }
    return CalendarViewModel(
        repository = repository,
        songCatalog = songCatalog,
        bibleCatalog = bibleCatalog,
        sync = CalendarSyncEngine(
            repository = repository,
            state = syncState,
            saveState = saveSync,
            clientKeys = ClientKeySource(settings),
            pushToken = { settings.fcmToken },
            deviceName = { settings.reportedDeviceName },
            catalogStore = catalogStore,
        ),
        enrollService = EnrollService(settings),
        deviceName = { settings.reportedDeviceName },
        saveEnrollment = saveSync,
    )
}

/** Everything one service's run of show can ask for, pointed at that service. */
private fun runOfShowActions(
    viewModel: CalendarViewModel,
    service: PlannedService,
): RunOfShowActions = RunOfShowActions(
    rows = RowListActions(
        onAdd = { row, seconds, timing -> viewModel.rows.add(service.id, row, seconds, timing) },
        onUpdate = { viewModel.rows.update(service.id, it.row, it.seconds, it.timing) },
        onRemove = { viewModel.rows.remove(service.id, it) },
        onMove = { from, to -> viewModel.rows.move(service.id, from, to) },
    ),
    onArmed = { viewModel.services.setArmed(service.id, it) },
    onCopy = { viewModel.services.copy(service.id, it.rule, it.count, it.includeRows, it.includeCues) },
    onUpdateService = { draft ->
        viewModel.services.update(service.copy(name = draft.name, startTime = draft.startTime, kind = draft.kind))
    },
    onDelete = { viewModel.services.delete(service.id) },
)
