package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppModeHolder
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import com.church.presenter.churchpresentermobile.network.ScheduleService
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests the schedule drawer's mode gating.
 *
 * Scope note: [ScheduleViewModel] builds its own [com.church.presenter.churchpresentermobile.network.ScheduleService]
 * with no seam to pass a mock client through, so the live-API path is not
 * reachable from a unit test — that decode is covered by `ScheduleServiceTest`
 * instead. What is tested here is everything that decides *whether* the network
 * is touched at all, which is the part that has actually gone wrong: a build
 * that fired schedule requests at an absent computer in standalone mode.
 */
class ScheduleViewModelTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    /** Never connects: [ServerEventService.listen] is only called outside demo mode. */
    private fun events(settings: AppSettings) = ServerEventService(settings)

    @BeforeTest fun setUp() = AppModeHolder.resetForTest()

    @AfterTest fun tearDownMode() = AppModeHolder.resetForTest()

    // ── Demo mode ────────────────────────────────────────────────────────

    @Test
    fun `demo mode serves canned content`() = runVmTest {
        val settings = settings()
        val vm = ScheduleViewModel(settings, events(settings), isDemoMode = true)
        try {
            advanceUntilIdle()

            assertEquals(DemoData.scheduleItems, vm.items.value)
            assertTrue(vm.items.value.isNotEmpty(), "demo data should not be empty")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `demo mode never reports loading or failure`() = runVmTest {
        // The canned list is assigned synchronously, so no spinner should ever
        // appear and no request can fail behind it.
        val settings = settings()
        val vm = ScheduleViewModel(settings, events(settings), isDemoMode = true)
        try {
            advanceUntilIdle()

            assertFalse(vm.isLoading.value)
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `reloading in demo mode is idempotent`() = runVmTest {
        val settings = settings()
        val vm = ScheduleViewModel(settings, events(settings), isDemoMode = true)
        try {
            vm.loadSchedule()
            vm.loadSchedule()
            advanceUntilIdle()

            assertEquals(DemoData.scheduleItems, vm.items.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `saving settings in demo mode does nothing`() = runVmTest {
        // Demo mode has no server to reconnect to; rebuilding the socket here
        // would start a listener the mode is defined by not having.
        val settings = settings()
        val vm = ScheduleViewModel(settings, events(settings), isDemoMode = true)
        try {
            vm.onSettingsSaved()
            advanceUntilIdle()

            assertEquals(DemoData.scheduleItems, vm.items.value)
            assertFalse(vm.isLoading.value)
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Event flows ──────────────────────────────────────────────────────

    @Test
    fun `the push-event flows are the shared service's own`() = runVmTest {
        // These are re-exposed rather than re-broadcast, so every screen that
        // collects one is watching the same connection.
        val settings = settings()
        val eventService = events(settings)
        val vm = ScheduleViewModel(settings, eventService, isDemoMode = true)
        try {
            assertSame(eventService.displayCleared, vm.displayCleared)
            assertSame(eventService.bibleUpdated, vm.bibleUpdated)
            assertSame(eventService.songsUpdated, vm.songsUpdated)
            assertSame(eventService.presentationUpdated, vm.presentationUpdated)
            assertSame(eventService.picturesUpdated, vm.picturesUpdated)
        } finally {
            tearDown(vm)
        }
    }

    // ── The live path ────────────────────────────────────────────────────
    //
    // The demo tests above never reach the network. These drive the real load
    // through a mocked ScheduleService.

    private fun liveVm(
        handler: MockRequestHandleScope.(path: String) -> HttpResponseData,
    ): ScheduleViewModel {
        val settings = settings()
        return ScheduleViewModel(settings, events(settings), isDemoMode = false) {
            ScheduleService(it, mockClient(handler))
        }
    }

    private val scheduleJson = """
        [{"id":"a","type":"song","displayText":"1 - Amazing Grace"},
         {"id":"b","type":"bible","displayText":"John 3:16"}]
    """.trimIndent()

    @Test
    fun `the schedule loads from the desktop`() = runVmTestUnconfined {
        val vm = liveVm { respond(scheduleJson) }
        try {
            val items = vm.items.first { it.isNotEmpty() }

            assertEquals(2, items.size)
            assertEquals("1 - Amazing Grace", items.first().displayTitle)
            assertFalse(vm.isLoading.value)
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `an empty schedule is not an error`() = runVmTestUnconfined {
        // A service with nothing scheduled yet is the ordinary Sunday-morning
        // state, not a failure to report.
        val vm = liveVm { respond("[]") }
        try {
            vm.isLoading.first { !it }

            assertTrue(vm.items.value.isEmpty())
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed load is reported and stops the spinner`() = runVmTestUnconfined {
        val vm = liveVm { respond("boom", HttpStatusCode.InternalServerError) }
        try {
            val error = vm.error.first { it != null }

            assertNotNull(error)
            assertFalse(vm.isLoading.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `an explicit reload asks again`() = runVmTestUnconfined {
        val asked = MutableStateFlow(0)
        val vm = liveVm { asked.value += 1; respond(scheduleJson) }
        try {
            vm.items.first { it.isNotEmpty() }
            val before = asked.value

            vm.loadSchedule()

            asked.first { it > before }
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a reload clears a previous error before asking`() = runVmTestUnconfined {
        // Otherwise the banner from a failed attempt outlives the retry that fixed it.
        var fail = true
        val vm = liveVm { if (fail) respond("boom", HttpStatusCode.InternalServerError) else respond(scheduleJson) }
        try {
            vm.error.first { it != null }

            fail = false
            vm.loadSchedule()
            vm.items.first { it.isNotEmpty() }

            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `saving settings rebuilds the service through the same factory`() = runVmTestUnconfined {
        var built = 0
        val settings = settings()
        val vm = ScheduleViewModel(settings, events(settings), isDemoMode = false) {
            built++
            ScheduleService(it, mockClient { respond(scheduleJson) })
        }
        try {
            vm.items.first { it.isNotEmpty() }
            assertEquals(1, built)

            vm.onSettingsSaved()

            assertEquals(2, built, "a new server needs a new client")
        } finally {
            tearDown(vm)
        }
    }
}
