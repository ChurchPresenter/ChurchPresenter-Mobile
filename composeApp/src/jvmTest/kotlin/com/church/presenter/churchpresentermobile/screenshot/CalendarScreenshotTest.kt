package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.calendar.PickerBook
import com.church.presenter.churchpresentermobile.calendar.YearMonthRef
import com.church.presenter.churchpresentermobile.calendar.sync.SyncStatus
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.SectionPalette
import com.church.presenter.churchpresentermobile.model.ServiceKind
import com.church.presenter.churchpresentermobile.ui.calendar.CalendarSyncView
import com.church.presenter.churchpresentermobile.ui.calendar.DayServices
import com.church.presenter.churchpresentermobile.ui.calendar.MonthHeader
import com.church.presenter.churchpresentermobile.ui.calendar.MonthGrid
import com.church.presenter.churchpresentermobile.ui.calendar.PickerSources
import com.church.presenter.churchpresentermobile.ui.calendar.RowListActions
import com.church.presenter.churchpresentermobile.ui.calendar.RunOfShowActions
import com.church.presenter.churchpresentermobile.ui.calendar.RunOfShowScreen
import com.church.presenter.churchpresentermobile.ui.calendar.CalendarSyncSheetContent
import com.church.presenter.churchpresentermobile.ui.calendar.ServiceTypesLegend
import com.church.presenter.churchpresentermobile.ui.calendar.SyncActions
import com.church.presenter.churchpresentermobile.viewmodel.EnrollFlow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The calendar: the month a service is planned in, the run of show inside it, and what the phone
 * says while it is enrolling with the church computer.
 *
 * Every one of these takes plain data rather than a ViewModel, so a golden here is the composable's
 * own doing and nothing else's — the dates are fixed for the same reason.
 */
class CalendarScreenshotTest {

    private companion object {
        val SEPTEMBER = YearMonthRef(2026, 9)
        val SUNDAY = LocalDate(2026, 9, 20)
        val TODAY = LocalDate(2026, 9, 17)
        /** The sync line's clock, pinned so the countdown in a golden never moves. */
        val SYNC_NOW: Instant = Instant.parse("2026-09-20T10:00:00Z")
    }

    private val services = listOf(
        PlannedService(id = "s1", date = "2026-09-20", name = "Sunday Morning", startTime = "10:00"),
        PlannedService(
            id = "s2",
            date = "2026-09-23",
            name = "Midweek Prayer",
            startTime = "19:00",
            kind = ServiceKind.MIDWEEK.id,
        ),
        PlannedService(
            id = "s3",
            date = "2026-09-27",
            name = "Baptism Service",
            startTime = "10:00",
            kind = ServiceKind.SPECIAL.id,
        ),
    )

    /** A service with one of every row shape: a heading, songs, a passage, a ministry item, a cue. */
    private val fullService = PlannedService(
        id = "s1",
        date = "2026-09-20",
        name = "Sunday Morning",
        startTime = "10:00",
        rows = listOf(
            PlanRow.Section(id = "r1", title = "Worship", color = SectionPalette.VIOLET),
            PlanRow.Song(id = "r2", title = "Amazing Grace", songbook = "Hymnal", number = "42"),
            PlanRow.Song(id = "r3", title = "How Great Thou Art", songbook = "Hymnal", number = "108"),
            PlanRow.Section(id = "r4", title = "Word", color = SectionPalette.AMBER),
            PlanRow.Bible(id = "r5", title = "John 3:16-17"),
            PlanRow.Ministry(id = "r6", title = "Welcome & notices", detail = "Anna"),
            PlanRow.Ref(id = "r7", title = "Countdown", kind = RowKind.CUE, subtitle = "Blank the screens"),
        ),
        plannedSeconds = mapOf("r2" to 270, "r3" to 300, "r5" to 120, "r6" to 300),
        timing = mapOf("r2" to RowTiming.DEFAULT),
    )

    private val sources = PickerSources(
        songs = emptyList(),
        books = emptyList<PickerBook>(),
        presets = emptyList(),
        chapterPreview = { _, _ -> null },
    )

    private val runActions = RunOfShowActions(
        rows = RowListActions(onAdd = { _, _, _ -> }, onUpdate = {}, onRemove = {}, onMove = { _, _ -> }),
        onArmed = {},
        onCopy = {},
        onUpdateService = {},
        onDelete = {},
    )

    // ── The month ────────────────────────────────────────────────────────

    @Test
    fun monthWithServices() = screenshot("calendar__month") {
        // Three kinds in one month, each with its own dot, and a chosen day that is not today.
        Month(services)
    }

    @Test
    fun monthEmpty() = screenshot("calendar__month-empty") {
        // A new install in a month nobody has planned yet: the grid still has to read as a month.
        Month(emptyList())
    }

    @Test
    fun monthHeaderSynced() = screenshot("calendar__month-header-synced") {
        // The line sits over the buttons: when the last round was, and when the next one runs.
        MonthHeaderWith(SyncStatus.Synced("2026-09-20T09:58:00Z", 0, 0), nextAt = SYNC_NOW + 3.minutes)
    }

    @Test
    fun monthHeaderSyncFailed() = screenshot("calendar__month-header-sync-failed") {
        MonthHeaderWith(SyncStatus.Failed("offline"), nextAt = SYNC_NOW + 5.minutes)
    }

    @Composable
    private fun MonthHeaderWith(status: SyncStatus, nextAt: Instant) {
        MonthHeader(
            serviceCount = services.size,
            monthName = "September 2026",
            onToday = {},
            onBack = null,
            compact = false,
            onSync = {},
            sync = CalendarSyncView(status, nextAt, SYNC_NOW),
            onSettings = {},
        )
    }

    @Composable
    private fun Month(shown: List<PlannedService>) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            MonthGrid(SEPTEMBER, SUNDAY, TODAY, shown, {}, {}, {})
            DayServices(
                date = SUNDAY,
                services = shown.filter { it.date == "2026-09-20" },
                selectedId = null,
                onOpen = {},
                onAdd = {},
                onCopyLast = {},
            )
            ServiceTypesLegend(shown)
        }
    }

    @Test
    fun dayWithNothingPlanned() = screenshot("calendar__day-empty") {
        // Offers both ways out of an empty day: start one, or copy the last like it.
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            DayServices(
                date = SUNDAY,
                services = emptyList(),
                selectedId = null,
                onOpen = {},
                onAdd = {},
                onCopyLast = {},
            )
        }
    }

    @Test
    fun dayWithNoServiceToCopy() = screenshot("calendar__day-empty-nothing-to-copy") {
        // A first-ever service: there is no last week to copy, so only Add is offered.
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            DayServices(
                date = SUNDAY,
                services = emptyList(),
                selectedId = null,
                onOpen = {},
                onAdd = {},
                onCopyLast = null,
            )
        }
    }

    // ── The run of show ──────────────────────────────────────────────────

    @Test
    fun runOfShow() = screenshot("calendar__run-of-show") {
        RunOfShowScreen(fullService, sources, { "new" }, runActions, onBack = {})
    }

    @Test
    fun runOfShowSynced() = screenshot("calendar__run-of-show-synced") {
        // On a phone the sync line takes the room above Armed, since the month header is not on screen.
        RunOfShowScreen(
            fullService,
            sources,
            { "new" },
            runActions,
            onBack = {},
            sync = CalendarSyncView(SyncStatus.Synced("2026-09-20T09:58:00Z", 0, 0), SYNC_NOW + 3.minutes, SYNC_NOW),
        )
    }

    @Test
    fun runOfShowEmpty() = screenshot("calendar__run-of-show-empty") {
        RunOfShowScreen(
            fullService.copy(rows = emptyList(), plannedSeconds = emptyMap(), timing = emptyMap()),
            sources,
            { "new" },
            runActions,
            onBack = {},
        )
    }

    @Test
    fun runOfShowInline() = screenshot("calendar__run-of-show-inline", width = Screenshots.TABLET_WIDTH) {
        // Beside the month on a tablet: no back arrow, and each row carries its own move and
        // remove controls because there is no room for a row editor sheet over the top.
        RunOfShowScreen(fullService, sources, { "new" }, runActions, inline = true)
    }

    // ── Enrolling with the church computer ───────────────────────────────

    @Test
    fun syncIdle() = syncSheet("calendar-sync__idle", SyncStatus.NotEnrolled, EnrollFlow.Idle)

    @Test
    fun syncWaiting() = syncSheet(
        "calendar-sync__waiting",
        SyncStatus.NotEnrolled,
        EnrollFlow.WaitingForApproval("482913"),
    )

    @Test
    fun syncScanQr() = syncSheet("calendar-sync__scan-qr", SyncStatus.NotEnrolled, EnrollFlow.ScanQr)

    @Test
    fun syncDesktopOff() = syncSheet("calendar-sync__desktop-off", SyncStatus.NotEnrolled, EnrollFlow.SyncOff)

    @Test
    fun syncDenied() = syncSheet("calendar-sync__denied", SyncStatus.NotEnrolled, EnrollFlow.Denied)

    @Test
    fun syncFailed() = syncSheet(
        "calendar-sync__failed",
        SyncStatus.NotEnrolled,
        EnrollFlow.Failed("connection refused"),
    )

    @Test
    fun syncEnrolled() = syncSheet(
        "calendar-sync__enrolled",
        SyncStatus.Synced("2026-09-20T10:30:00Z", pulled = 2, pushed = 1),
        EnrollFlow.Idle,
    )

    @Test
    fun syncUnauthorized() = syncSheet("calendar-sync__unauthorized", SyncStatus.Unauthorized, EnrollFlow.Idle)

    private fun syncSheet(name: String, status: SyncStatus, flow: EnrollFlow) = screenshot(name) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            CalendarSyncSheetContent(
                status = status,
                flow = flow,
                canReachDesktop = true,
                actions = syncActions,
                onDismiss = {},
            )
        }
    }

    private val syncActions = SyncActions(
        onEnroll = {},
        onScanned = {},
        onReset = {},
        onSyncNow = {},
        onLeave = {},
    )
}
