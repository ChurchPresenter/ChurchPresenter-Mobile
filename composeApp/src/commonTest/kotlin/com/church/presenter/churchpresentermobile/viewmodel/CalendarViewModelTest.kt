package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.calendar.RepeatRule
import com.church.presenter.churchpresentermobile.calendar.YearMonthRef
import com.church.presenter.churchpresentermobile.calendar.sync.CalendarSyncEngine
import com.church.presenter.churchpresentermobile.calendar.sync.CalendarSyncState
import com.church.presenter.churchpresentermobile.calendar.sync.ClientKeySource
import com.church.presenter.churchpresentermobile.calendar.sync.EnrollService
import com.church.presenter.churchpresentermobile.calendar.sync.RelayClient
import com.church.presenter.churchpresentermobile.calendar.sync.SyncStatus
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import com.church.presenter.churchpresentermobile.testutil.tearDown
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarViewModelTest {

    private val storage = InMemoryFileStorage()
    private val repository = CalendarRepository(storage, now = { "2026-09-20T10:00:00Z" })
    private var counter = 0
    private val newId: () -> String = { "id-${++counter}" }

    private fun viewModel(
        enrollService: EnrollService? = null,
        sync: CalendarSyncEngine? = null,
        saveEnrollment: (CalendarSyncState) -> Unit = {},
    ) = CalendarViewModel(
        repository,
        sync = sync,
        enrollService = enrollService,
        deviceName = { "Anna's iPhone" },
        saveEnrollment = saveEnrollment,
        newId = newId,
    )

    private val sunday = LocalDate(2026, 9, 27)

    @Test
    fun theMonthFollowsTheSelectionAndStepsOnItsOwn() = runVmTest {
        val vm = viewModel()
        try {
            vm.showMonth(YearMonthRef(2026, 9))
            vm.showNextMonth()
            assertEquals(YearMonthRef(2026, 10), vm.visibleMonth.value)
            vm.showPreviousMonth()
            assertEquals(YearMonthRef(2026, 9), vm.visibleMonth.value)
            vm.select(LocalDate(2026, 12, 25))
            assertEquals(LocalDate(2026, 12, 25), vm.selectedDate.value)
            assertEquals(YearMonthRef(2026, 12), vm.visibleMonth.value)
            vm.goToToday()
            assertTrue(vm.visibleMonth.value.contains(vm.selectedDate.value))
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aNewServiceLandsOnTheSelectedDateAndOpens() = runVmTest {
        val vm = viewModel()
        try {
            vm.select(sunday)
            val id = vm.addService("Sunday Service", "10:00", "sunday", templateId = null)
            assertEquals(id, vm.openServiceId.value)
            val service = vm.servicesOn(sunday).single()
            assertEquals("Sunday Service", service.name)
            assertEquals("2026-09-27", service.date)
            assertEquals("2026-09-20T10:00:00Z", service.updatedAt)
            assertEquals(setOf(id), vm.document.value.pendingPush)
            vm.closeService()
            assertNull(vm.openServiceId.value)
            vm.openService(id)
            assertEquals(id, vm.openServiceId.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun rowsAreAddedReplacedMovedAndRemoved() = runVmTest {
        val vm = viewModel()
        try {
            vm.select(sunday)
            val id = vm.addService("Sunday", "10:00", "sunday", null)
            vm.addRow(id, PlanRow.Section("s1", "Worship"), null, RowTiming.DEFAULT)
            vm.addRow(id, PlanRow.Song("r1", "Opening"), 270, RowTiming.DEFAULT)
            vm.addRow(id, PlanRow.Ministry("r2", "Sermon"), 1800, RowTiming(startAt = "10:30"))
            var service = repository.service(id)!!
            assertEquals(listOf("s1", "r1", "r2"), service.rows.map { it.id })
            assertEquals(270, service.plannedSecondsFor("r1"))
            assertEquals("10:30", service.timingFor("r2").startAt)

            vm.updateRow(id, PlanRow.Song("r1", "Opening, renamed"), 300, RowTiming(repeats = 2))
            service = repository.service(id)!!
            assertEquals(3, service.rows.size)
            assertEquals("Opening, renamed", service.rows[1].title)
            assertEquals(300, service.plannedSecondsFor("r1"))

            vm.moveRow(id, 2, 1)
            assertEquals(listOf("s1", "r2", "r1"), repository.service(id)!!.rows.map { it.id })

            vm.removeRow(id, "r1")
            service = repository.service(id)!!
            assertEquals(listOf("s1", "r2"), service.rows.map { it.id })
            assertNull(service.plannedSecondsFor("r1"))
            assertTrue("r1" !in service.timing)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun copiesRepeatIntoTheFutureUnderOneSeries() = runVmTest {
        val vm = viewModel()
        try {
            vm.select(sunday)
            val id = vm.addService("Sunday", "10:00", "sunday", null)
            vm.addRow(id, PlanRow.Song("r1", "Opening"), 270, RowTiming.DEFAULT)
            assertEquals(2, vm.copyService(id, RepeatRule.WEEKLY, 2, includeRows = true, includeCues = true))
            val doc = vm.document.value
            assertEquals(3, doc.services.size)
            val series = doc.serviceById(id)!!.seriesId
            assertTrue(series.isNotEmpty())
            assertTrue(doc.services.all { it.seriesId == series })
            assertEquals(listOf("2026-09-27", "2026-10-04", "2026-10-11"), doc.services.map { it.date }.sorted())
            assertTrue(doc.services.all { it.rows.single().title == "Opening" })
            assertEquals(0, vm.copyService("missing", RepeatRule.WEEKLY, 2, includeRows = true, includeCues = true))
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun copyLastFindsTheLatestEarlierServiceOnTheSameWeekday() = runVmTest {
        val vm = viewModel()
        try {
            vm.select(LocalDate(2026, 9, 13))
            vm.addService("Two Sundays ago", "10:00", "sunday", null)
            vm.select(LocalDate(2026, 9, 20))
            val last = vm.addService("Last Sunday", "10:00", "sunday", null)
            vm.addRow(last, PlanRow.Song("r1", "Opening"), 270, RowTiming.DEFAULT)
            vm.select(LocalDate(2026, 9, 23))
            vm.addService("Midweek", "19:00", "midweek", null)

            assertEquals("Last Sunday", vm.lastServiceLike(sunday)!!.name)
            assertNull(vm.lastServiceLike(LocalDate(2026, 9, 26)))

            vm.copyLastInto(sunday)
            val copy = vm.servicesOn(sunday).single()
            assertEquals(copy.id, vm.openServiceId.value)
            assertEquals("Last Sunday", copy.name)
            assertEquals("Opening", copy.rows.single().title)
            assertTrue(copy.rows.single().id != "r1")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun deletingArmingAndTemplates() = runVmTest {
        val vm = viewModel()
        try {
            vm.select(sunday)
            val id = vm.addService("Sunday", "10:00", "sunday", null)
            vm.setArmed(id, false)
            assertTrue(!repository.service(id)!!.armed)

            vm.addRow(id, PlanRow.Song("r1", "Opening"), 270, RowTiming.DEFAULT)
            vm.saveAsTemplate(id, "Standard")
            val template = vm.document.value.templates.single()
            assertEquals("Standard", template.name)
            vm.select(LocalDate(2026, 10, 4))
            val fromTemplate = vm.addService("Next", "10:00", "sunday", template.id)
            assertEquals("Opening", repository.service(fromTemplate)!!.rows.single().title)

            vm.deleteService(fromTemplate)
            assertNull(vm.openServiceId.value)
            assertNull(repository.service(fromTemplate))
            assertEquals(setOf(fromTemplate), vm.document.value.pendingDeletes)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun enrollmentShowsACodeThenAsksForTheQr() = runVmTestUnconfined {
        val desktop = HttpClient(
            MockEngine {
                respond("""{"relayUrl":"https://sync.example.org","instanceId":"inst-1"}""", HttpStatusCode.OK)
            },
        )
        val vm = viewModel(enrollService = EnrollService(AppSettings(InMemorySettingsStorage()), desktop))
        try {
            vm.startEnrollment()
            val waiting = vm.enrollment.value
            if (waiting is EnrollFlow.WaitingForApproval) assertEquals(6, waiting.code.length)
            assertEquals(EnrollFlow.ScanQr, vm.enrollment.first { it !is EnrollFlow.WaitingForApproval })
            vm.resetEnrollment()
            assertEquals(EnrollFlow.Idle, vm.enrollment.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aRefusedEnrollmentIsDeniedASwitchedOffDesktopSaysSoAndAFailedOneSaysWhy() = runVmTestUnconfined {
        val refusing = HttpClient(MockEngine { respond("", HttpStatusCode.Forbidden) })
        val denied = viewModel(enrollService = EnrollService(AppSettings(InMemorySettingsStorage()), refusing))
        val switchedOff = HttpClient(MockEngine { respond("""{"error":"sync_off"}""", HttpStatusCode.Conflict) })
        val off = viewModel(enrollService = EnrollService(AppSettings(InMemorySettingsStorage()), switchedOff))
        val broken = HttpClient(MockEngine { respond("", HttpStatusCode.InternalServerError) })
        val failed = viewModel(enrollService = EnrollService(AppSettings(InMemorySettingsStorage()), broken))
        try {
            denied.startEnrollment()
            assertEquals(EnrollFlow.Denied, denied.enrollment.first { it !is EnrollFlow.WaitingForApproval })
            off.startEnrollment()
            assertEquals(EnrollFlow.SyncOff, off.enrollment.first { it !is EnrollFlow.WaitingForApproval })
            failed.startEnrollment()
            assertIs<EnrollFlow.Failed>(failed.enrollment.first { it !is EnrollFlow.WaitingForApproval })
        } finally {
            tearDown(denied, off, failed)
        }
    }

    @Test
    fun theQrCompletesEnrollmentOrIsRefused() = runVmTest {
        var saved: CalendarSyncState? = null
        val vm = viewModel(saveEnrollment = { saved = it })
        try {
            vm.completeEnrollment("https://example.org/not-a-qr")
            assertEquals(EnrollFlow.Failed(""), vm.enrollment.value)
            assertNull(saved)
            vm.completeEnrollment(
                "churchpresenter://calendar-enroll?relay=https://sync.example.org&instance=inst-1" +
                    "&token=tokentokentokentoken&key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8",
            )
            assertEquals(EnrollFlow.Done, vm.enrollment.value)
            assertEquals("inst-1", assertNotNull(saved).instanceId)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun anEnrolledPhoneSyncsOnOpenAndPushesAnEditOnceItSettles() = runVmTestUnconfined {
        val calls = ArrayList<String>()
        var rev = 10L
        val relayHttp = HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath.substringAfter("/i/inst-1/")
                calls += "${request.method.value} $path"
                when {
                    path == "changes" -> respond("""{"rev":$rev,"records":[],"tombstones":[]}""", HttpStatusCode.OK)
                    request.method == HttpMethod.Put -> respond("""{"rev":${++rev}}""", HttpStatusCode.OK)
                    else -> respond("{}", HttpStatusCode.NotFound)
                }
            },
        )
        val websiteHttp = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })
        val settings = AppSettings(InMemorySettingsStorage()).apply {
            relayClientKey = "clientkeyclientkey01"
            relayClientKeyFetchedAt = 5_000_000L
        }
        var state = CalendarSyncState(
            relayUrl = "https://sync.example.org",
            instanceId = "inst-1",
            deviceToken = "devicetokendevicetoken",
            instanceKey = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8",
        )
        val engine = CalendarSyncEngine(
            repository,
            state = { state },
            saveState = { state = it },
            clientKeys = ClientKeySource(settings, websiteHttp, now = { 5_000_000L }),
            clientFor = { s, k -> RelayClient(s, k, relayHttp) },
        )
        val vm = viewModel(sync = engine)
        try {
            // The engine starts as Synced("") when enrolled; the first real round stamps a time.
            vm.syncStatus.first { it is SyncStatus.Synced && it.at.isNotEmpty() }
            assertEquals(listOf("GET changes"), calls)

            vm.select(sunday)
            vm.addService("Sunday", "10:00", "sunday", null)
            vm.syncStatus.first { it is SyncStatus.Synced && it.pushed == 1 }
            assertTrue("PUT records/id-1" in calls)
            assertTrue(vm.document.value.pendingPush.isEmpty())
            assertEquals(rev, state.cursor)

            vm.leaveSync()
            assertEquals(SyncStatus.NotEnrolled, vm.syncStatus.value)
            assertTrue(!state.isEnrolled)
        } finally {
            tearDown(vm)
        }
    }
}
