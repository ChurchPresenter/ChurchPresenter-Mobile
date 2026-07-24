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
import kotlin.test.Test
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
    fun notChurchPresenterMapsToNotChurchPresenter() = runVmTestUnconfined {
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
}
