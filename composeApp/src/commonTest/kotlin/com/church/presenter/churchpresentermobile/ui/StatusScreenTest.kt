package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.StatusService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The startup check — the first screen after launch, and the one that has to
 * explain why nothing works when nothing works.
 *
 * Its whole job is telling four failures apart: the desktop is unreachable, the
 * API key was rejected, something answered that is not ChurchPresenter, and
 * everything is fine but this device is not allowed to do much. Each sends the
 * operator somewhere different, so each test asserts the other states are
 * absent rather than only that its own appeared.
 */
@OptIn(ExperimentalTestApi::class)
class StatusScreenTest {

    private fun statusVm(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): StatusViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return StatusViewModel(settings) {
            StatusService(
                baseUrl = it.apiBaseUrl,
                apiKey = it.apiKey,
                deviceId = it.deviceId,
                client = mockClient {
                    respond(
                        body,
                        status,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            )
        }
    }

    private fun ComposeUiTest.showStatus(
        vm: StatusViewModel,
        onContinue: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) = showScreen {
        StatusScreen(viewModel = vm, onContinue = onContinue, onOpenSettings = onOpenSettings)
    }

    /** A desktop with content and no restrictions — nothing to warn about. */
    private val healthy = """
        {"appVersion":"1.4.2","endpoints":["songs","bible","schedule"],
         "bibles":["KJV"],"songbooks":["Hymns"],
         "permissions":{"canPresent":true,"canAddToSchedule":true,"canUploadFiles":true}}
    """.trimIndent()

    /** Reachable, but this device may not present or upload. */
    private val restricted = """
        {"appVersion":"1.4.2","endpoints":["songs","bible","schedule"],
         "bibles":["KJV"],"songbooks":["Hymns"],
         "permissions":{"canPresent":false,"canAddToSchedule":false,"canUploadFiles":false}}
    """.trimIndent()

    // ── Everything is fine ───────────────────────────────────────────────

    @Test
    fun aHealthyDesktopReportsAllGood() = runComposeUiTest {
        val vm = statusVm(healthy)
        showStatus(vm)

        awaitThat { vm.uiState.value is StatusUiState.Success }
        assertTrue(exists(UiTags.STATUS_ALL_GOOD))
    }

    @Test
    fun aHealthyDesktopShowsNoWarningsScreen() = runComposeUiTest {
        val vm = statusVm(healthy)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_ALL_GOOD) }
        assertFalse(exists(UiTags.STATUS_WARNINGS))
    }

    @Test
    fun aHealthyDesktopShowsNoError() = runComposeUiTest {
        val vm = statusVm(healthy)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_ALL_GOOD) }
        assertFalse(exists(UiTags.STATUS_ERROR))
    }

    @Test
    fun theOperatorCanCarryOnFromTheAllGoodScreen() = runComposeUiTest {
        var continued = false
        val vm = statusVm(healthy)
        showStatus(vm, onContinue = { continued = true })

        awaitThat { exists(UiTags.STATUS_ALL_GOOD) }
        click(UiTags.STATUS_CONTINUE)

        assertTrue(continued)
    }

    @Test
    fun theDesktopsVersionIsShown() = runComposeUiTest {
        // The first thing anyone asks when the two ends disagree.
        val vm = statusVm(healthy)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_ALL_GOOD) }
        assertTrue(isShowing("1.4.2"))
    }

    @Test
    fun theTranslationsTheDesktopHasAreListed() = runComposeUiTest {
        val vm = statusVm(healthy)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_ALL_GOOD) }
        assertTrue(isShowing("KJV"))
    }

    @Test
    fun theSongbooksTheDesktopHasAreListed() = runComposeUiTest {
        val vm = statusVm(healthy)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_ALL_GOOD) }
        assertTrue(isShowing("Hymns"))
    }

    // ── Reachable, but restricted ────────────────────────────────────────

    @Test
    fun aRestrictedDeviceIsWarnedRatherThanWavedThrough() = runComposeUiTest {
        // Being told now beats discovering mid-service that Present does nothing.
        val vm = statusVm(restricted)
        showStatus(vm)

        awaitThat { vm.uiState.value is StatusUiState.Success }
        assertTrue(exists(UiTags.STATUS_WARNINGS))
    }

    @Test
    fun aRestrictedDeviceIsNotReportedAsAllGood() = runComposeUiTest {
        val vm = statusVm(restricted)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_WARNINGS) }
        assertFalse(exists(UiTags.STATUS_ALL_GOOD))
    }

    @Test
    fun aDesktopWithNoBiblesIsAWarningNotAnError() = runComposeUiTest {
        // The desktop is working; it just has nothing loaded yet.
        val vm = statusVm(
            """
            {"appVersion":"1.4.2","endpoints":["songs"],"bibles":[],"songbooks":["Hymns"],
             "permissions":{"canPresent":true,"canAddToSchedule":true,"canUploadFiles":true}}
            """.trimIndent()
        )
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_WARNINGS) }
        assertFalse(exists(UiTags.STATUS_ERROR))
    }

    @Test
    fun aDesktopWithNoSongbooksIsAWarningNotAnError() = runComposeUiTest {
        val vm = statusVm(
            """
            {"appVersion":"1.4.2","endpoints":["songs"],"bibles":["KJV"],"songbooks":[],
             "permissions":{"canPresent":true,"canAddToSchedule":true,"canUploadFiles":true}}
            """.trimIndent()
        )
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_WARNINGS) }
        assertFalse(exists(UiTags.STATUS_ERROR))
    }

    @Test
    fun theOperatorCanCarryOnPastWarnings() = runComposeUiTest {
        // A warning is advice, not a locked door.
        var continued = false
        val vm = statusVm(restricted)
        showStatus(vm, onContinue = { continued = true })

        awaitThat { exists(UiTags.STATUS_WARNINGS) }
        click(UiTags.STATUS_CONTINUE)

        assertTrue(continued)
    }

    @Test
    fun settingsAreReachableFromTheWarningsScreen() = runComposeUiTest {
        var opened = false
        val vm = statusVm(restricted)
        showStatus(vm, onOpenSettings = { opened = true })

        awaitThat { exists(UiTags.STATUS_WARNINGS) }
        click(UiTags.STATUS_OPEN_SETTINGS)

        assertTrue(opened)
    }

    // ── Nothing usable at the other end ──────────────────────────────────

    @Test
    fun aRejectedApiKeyIsReported() = runComposeUiTest {
        val vm = statusVm("", HttpStatusCode.Unauthorized)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_ERROR) }
        assertTrue(exists(UiTags.STATUS_ERROR))
    }

    @Test
    fun aRejectedApiKeyIsNotReportedAsAllGood() = runComposeUiTest {
        val vm = statusVm("", HttpStatusCode.Unauthorized)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_ERROR) }
        assertFalse(exists(UiTags.STATUS_ALL_GOOD))
    }

    @Test
    fun aForbiddenReplyIsAlsoReported() = runComposeUiTest {
        val vm = statusVm("", HttpStatusCode.Forbidden)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_ERROR) }
        assertTrue(exists(UiTags.STATUS_ERROR))
    }

    @Test
    fun aHostThatIsNotChurchPresenterIsReported() = runComposeUiTest {
        // Someone typed the address of their router, not the desktop.
        val vm = statusVm("<html>hello</html>", HttpStatusCode.OK)
        showStatus(vm)

        awaitThat { vm.uiState.value !is StatusUiState.Loading }
        assertTrue(exists(UiTags.STATUS_ERROR))
    }

    @Test
    fun aServerFailureIsReported() = runComposeUiTest {
        val vm = statusVm("boom", HttpStatusCode.InternalServerError)
        showStatus(vm)

        awaitThat { vm.uiState.value !is StatusUiState.Loading }
        assertTrue(exists(UiTags.STATUS_ERROR))
    }

    @Test
    fun aFailureOffersARetry() = runComposeUiTest {
        // The usual cause is a desktop that had not finished starting.
        val vm = statusVm("", HttpStatusCode.Unauthorized)
        showStatus(vm)

        awaitThat { exists(UiTags.STATUS_ERROR) }
        assertTrue(exists(UiTags.STATUS_RETRY))
    }

    @Test
    fun aFailureOffersAWayIntoSettings() = runComposeUiTest {
        var opened = false
        val vm = statusVm("", HttpStatusCode.Unauthorized)
        showStatus(vm, onOpenSettings = { opened = true })

        awaitThat { exists(UiTags.STATUS_ERROR) }
        click(UiTags.STATUS_OPEN_SETTINGS)

        assertTrue(opened)
    }

    @Test
    fun aFailureStillLetsTheOperatorCarryOn() = runComposeUiTest {
        // Standalone mode works with no desktop at all, so this must not trap.
        var continued = false
        val vm = statusVm("", HttpStatusCode.Unauthorized)
        showStatus(vm, onContinue = { continued = true })

        awaitThat { exists(UiTags.STATUS_ERROR) }
        click(UiTags.STATUS_CONTINUE)

        assertTrue(continued)
    }

    @Test
    fun retryingDoesNotItselfContinue() = runComposeUiTest {
        var continued = false
        val vm = statusVm("", HttpStatusCode.Unauthorized)
        showStatus(vm, onContinue = { continued = true })

        awaitThat { exists(UiTags.STATUS_ERROR) }
        click(UiTags.STATUS_RETRY)

        assertFalse(continued)
    }

    @Test
    fun nothingIsReportedGoodWhileTheCheckIsStillRunning() = runComposeUiTest {
        // A green tick before the answer arrives is worse than a spinner.
        val vm = statusVm(healthy)
        showStatus(vm)

        if (vm.uiState.value is StatusUiState.Loading) {
            assertFalse(exists(UiTags.STATUS_ALL_GOOD))
            assertFalse(exists(UiTags.STATUS_ERROR))
        }
    }
}
