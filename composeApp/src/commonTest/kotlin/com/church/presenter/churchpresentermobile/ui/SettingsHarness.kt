package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.StatusService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.viewmodel.SettingsViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * Setup for the settings sheet.
 *
 * Both of its ViewModels are supplied rather than created: the sheet is a
 * `Dialog`, which has no ViewModelStoreOwner of its own in a test, and a test
 * that cannot hold the ViewModel cannot assert on what was actually persisted —
 * which is the only thing that matters here. Settings are judged by what reaches
 * [AppSettings], not by what a field is showing.
 */
internal fun storedSettings(
    host: String = "192.168.1.50",
    port: Int = 8765,
    apiKey: String = "",
    displayName: String = "",
    customDeviceName: String = "",
): AppSettings {
    val settings = AppSettings(InMemorySettingsStorage())
    settings.host = host
    settings.port = port
    settings.apiKey = apiKey
    settings.displayName = displayName
    settings.customDeviceName = customDeviceName
    return settings
}

/** A status endpoint that answers with [body], or fails with [status]. */
internal fun statusVm(
    settings: AppSettings,
    body: String = healthyDesktop,
    status: HttpStatusCode = HttpStatusCode.OK,
): StatusViewModel = StatusViewModel(settings) {
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

/**
 * A desktop that never answers.
 *
 * Not a 500: an HTTP error is a server *saying* something, and the app probes
 * `/songs` after one and concludes it is talking to something that is not
 * ChurchPresenter. Genuinely unreachable means the request itself fails.
 */
internal fun unreachableStatusVm(settings: AppSettings): StatusViewModel =
    StatusViewModel(settings) {
        StatusService(
            baseUrl = it.apiBaseUrl,
            apiKey = it.apiKey,
            deviceId = it.deviceId,
            client = mockClient { throw IllegalStateException("Connection refused") },
        )
    }

/** A desktop with content and nothing to warn about. */
internal val healthyDesktop = """
    {"appVersion":"1.4.2","endpoints":["songs","bible","schedule"],
     "bibles":["KJV","ESV"],"songbooks":["Hymns"],
     "permissions":{"canPresent":true,"canAddToSchedule":true,"canUploadFiles":true}}
""".trimIndent()

/** Reachable, but this device may not present or upload. */
internal val restrictedDesktop = """
    {"appVersion":"1.4.2","endpoints":["songs"],
     "bibles":[],"songbooks":[],
     "permissions":{"canPresent":false,"canAddToSchedule":false,"canUploadFiles":false}}
""".trimIndent()

/** Something answered, but it is not ChurchPresenter. */
internal val notChurchPresenter = """{"message":"hello from nginx"}"""

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showSettings(
    settings: AppSettings,
    viewModel: SettingsViewModel = SettingsViewModel(settings),
    status: StatusViewModel = statusVm(settings),
    onDismiss: () -> Unit = {},
    onSaved: () -> Unit = {},
    onContact: () -> Unit = {},
) = showScreen {
    SettingsScreen(
        appSettings = settings,
        onDismiss = onDismiss,
        onSaved = onSaved,
        onContact = onContact,
        providedViewModel = viewModel,
        providedStatusViewModel = status,
    )
}
