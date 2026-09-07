package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.StatusService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Tests [StatusViewModel] probe-result -> [StatusUiState] mapping and refreshQuietly. */
class StatusViewModelTest {

    private fun vm(handler: MockRequestHandleScope.(path: String) -> HttpResponseData): StatusViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return StatusViewModel(settings) { StatusService("http://t/api", "", "d", mockClient(handler)) }
    }

    private fun MockRequestHandleScope.isStatus(path: String) = path.endsWith("/status")

    /** Awaits the first non-Loading state (the init fetch completes via a real microtask). */
    private suspend fun StatusViewModel.settled(): StatusUiState =
        uiState.first { it !is StatusUiState.Loading }

    @Test
    fun verifiedFullyProvisionedIsSuccessWithoutWarnings() = runVmTestUnconfined {
        val vm = vm { respond("""{"appVersion":"1.0","bibles":["KJV"],"songbooks":["Hymns"]}""", HttpStatusCode.OK) }
        assertTrue(assertIs<StatusUiState.Success>(vm.settled()).warnings.isEmpty())
    }

    @Test
    fun verifiedWithMissingContentIsSuccessWithWarnings() = runVmTestUnconfined {
        val vm = vm { respond("""{"appVersion":"1.0"}""", HttpStatusCode.OK) }
        assertTrue(assertIs<StatusUiState.Success>(vm.settled()).warnings.isNotEmpty())
    }

    @Test
    fun reachableNoStatusEndpointIsConnectedSuccess() = runVmTestUnconfined {
        val vm = vm { path ->
            if (isStatus(path)) respond("nope", HttpStatusCode.NotFound)
            else respond("""{"song-book":[]}""", HttpStatusCode.OK)
        }
        val s = assertIs<StatusUiState.Success>(vm.settled())
        assertTrue(s.warnings.isEmpty())
        assertTrue(!s.status.endpointAvailable)
    }

    @Test
    fun unauthorizedMapsToUnauthorized() = runVmTestUnconfined {
        val vm = vm { respond("", HttpStatusCode.Unauthorized) }
        assertIs<StatusUiState.Unauthorized>(vm.settled())
    }

    @Test
    fun NOT_CHURCH_PRESENTERMapsToNotChurchPresenter() = runVmTestUnconfined {
        val vm = vm { path ->
            if (isStatus(path)) respond("nope", HttpStatusCode.NotFound)
            else respond("<html>router</html>", HttpStatusCode.OK)
        }
        assertIs<StatusUiState.NotChurchPresenter>(vm.settled())
    }

    @Test
    fun connectivityFailureMapsToError() = runVmTestUnconfined {
        val vm = vm { throw RuntimeException("Connect timeout has expired") }
        assertIs<StatusUiState.Error>(vm.settled())
    }

    @Test
    fun refreshQuietlyUpdatesOnVerifiedResult() = runVmTestUnconfined {
        var mode = "ok"
        val settings = AppSettings(InMemorySettingsStorage())
        val vm = StatusViewModel(settings) {
            StatusService("http://t/api", "", "d", mockClient {
                if (mode == "ok") respond("""{"appVersion":"1.0","bibles":["KJV"],"songbooks":["H"]}""", HttpStatusCode.OK)
                else respond("""{"appVersion":"2.0"}""", HttpStatusCode.OK) // verified but content-limited
            })
        }
        assertTrue(assertIs<StatusUiState.Success>(vm.settled()).warnings.isEmpty())

        mode = "limited"
        vm.refreshQuietly()
        val updated = vm.uiState.first { it is StatusUiState.Success && it.warnings.isNotEmpty() }
        assertTrue(assertIs<StatusUiState.Success>(updated).warnings.isNotEmpty())
    }

    @Test
    fun `rechecking rebuilds the client and probes again`() = runVmTestUnconfined {
        // The button exists for the case where the server has just been started or
        // the address just corrected, so it must not reuse the old client.
        var built = 0
        val settings = AppSettings(InMemorySettingsStorage())
        val asked = MutableStateFlow(0)
        val vm = StatusViewModel(settings) {
            built++
            StatusService("http://t/api", "", "d", mockClient { asked.value += 1; respond("{}") })
        }
        try {
            asked.first { it > 0 }
            assertEquals(1, built)
            val before = asked.value

            vm.recheck()

            asked.first { it > before }
            assertEquals(2, built, "recheck should build a fresh client")
        } finally {
            tearDown(vm)
        }
    }

    // ── refreshQuietly ───────────────────────────────────────────────────
    //
    // The pull-to-refresh on a screen the operator is already reading. Unlike the
    // initial load it must never blank what is on show: a transient blip mid-
    // service would replace a working status with an error the operator then has
    // to re-check by hand.

    /** Answers differently on the second probe, so a refresh can change the result. */
    private fun refreshingVm(
        first: MockRequestHandleScope.(String) -> HttpResponseData,
        second: MockRequestHandleScope.(String) -> HttpResponseData,
    ): StatusViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        var probes = 0
        return StatusViewModel(settings) {
            StatusService("http://t/api", "", "d", mockClient { path ->
                if (path.endsWith("/status")) probes += 1
                if (probes <= 1) first(path) else second(path)
            })
        }
    }

    @Test
    fun aQuietRefreshPicksUpANewStatus() = runVmTestUnconfined {
        val vm = refreshingVm(
            first = { respond("""{"appVersion":"1.0"}""", HttpStatusCode.OK) },
            second = { respond("""{"appVersion":"1.0","bibles":["KJV"],"songbooks":["Hymns"]}""", HttpStatusCode.OK) },
        )
        try {
            assertTrue(assertIs<StatusUiState.Success>(vm.settled()).warnings.isNotEmpty())

            vm.refreshQuietly()
            val settled = vm.uiState.first { it is StatusUiState.Success && it.warnings.isEmpty() }

            assertTrue(assertIs<StatusUiState.Success>(settled).warnings.isEmpty())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aQuietRefreshThatFindsNoStatusEndpointStillReportsConnected() = runVmTestUnconfined {
        val vm = refreshingVm(
            first = { respond("""{"appVersion":"1.0"}""", HttpStatusCode.OK) },
            second = { path ->
                if (path.endsWith("/status")) respond("nope", HttpStatusCode.NotFound)
                else respond("""{"song-book":[]}""", HttpStatusCode.OK)
            },
        )
        try {
            vm.settled()

            vm.refreshQuietly()
            val settled = vm.uiState.first { it is StatusUiState.Success && !it.status.endpointAvailable }

            assertTrue(!assertIs<StatusUiState.Success>(settled).status.endpointAvailable)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aQuietRefreshThatIsRefusedKeepsWhatIsOnScreen() = runVmTestUnconfined {
        // The key case: an API key revoked mid-service would otherwise replace a
        // perfectly good status screen with an auth error on a background poll.
        val vm = refreshingVm(
            first = { respond("""{"appVersion":"1.0","bibles":["KJV"],"songbooks":["Hymns"]}""", HttpStatusCode.OK) },
            second = { respond("", HttpStatusCode.Unauthorized) },
        )
        try {
            val before = assertIs<StatusUiState.Success>(vm.settled())

            vm.refreshQuietly()

            assertEquals(before, vm.uiState.value, "a quiet refresh must not blank the screen")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aQuietRefreshThatFindsSomethingElseKeepsWhatIsOnScreen() = runVmTestUnconfined {
        // A captive portal answering for the LAN address; the previous good status
        // is still the most accurate thing to show.
        val vm = refreshingVm(
            first = { respond("""{"appVersion":"1.0","bibles":["KJV"],"songbooks":["Hymns"]}""", HttpStatusCode.OK) },
            second = { path ->
                if (path.endsWith("/status")) respond("nope", HttpStatusCode.NotFound)
                else respond("<html>router</html>", HttpStatusCode.OK)
            },
        )
        try {
            val before = assertIs<StatusUiState.Success>(vm.settled())

            vm.refreshQuietly()

            assertEquals(before, vm.uiState.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aQuietRefreshThatCannotReachTheDesktopKeepsWhatIsOnScreen() = runVmTestUnconfined {
        val settings = AppSettings(InMemorySettingsStorage())
        var probes = 0
        val vm = StatusViewModel(settings) {
            StatusService("http://t/api", "", "d", mockClient {
                probes += 1
                if (probes <= 1) respond("""{"appVersion":"1.0","bibles":["KJV"],"songbooks":["Hymns"]}""", HttpStatusCode.OK)
                else error("Connect timeout has expired")
            })
        }
        try {
            val before = assertIs<StatusUiState.Success>(vm.settled())

            vm.refreshQuietly()

            assertEquals(before, vm.uiState.value, "a blip must not blank the screen")
        } finally {
            tearDown(vm)
        }
    }
}
