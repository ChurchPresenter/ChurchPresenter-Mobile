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

/**
 * A desktop too old to have the status endpoint, identified through `/songs`.
 *
 * The one connected-and-fine answer that carries no content lists: the app
 * knows the desktop is ChurchPresenter and nothing else, so the screen has
 * permissions to show and no Bibles or song books to list beside them.
 */
internal fun olderDesktopStatusVm(settings: AppSettings): StatusViewModel =
    StatusViewModel(settings) {
        StatusService(
            baseUrl = it.apiBaseUrl,
            apiKey = it.apiKey,
            deviceId = it.deviceId,
            client = mockClient { path ->
                if (path.endsWith("/status")) respond("nope", HttpStatusCode.NotFound)
                else respond("""{"song-book":[]}""", HttpStatusCode.OK)
            },
        )
    }

/** Content and features to report, but this device may not present — warnings *and* information. */
internal val restrictedWithContent = """
    {"appVersion":"1.4.2","endpoints":["songs","bible","schedule"],
     "bibles":["KJV"],"songbooks":["Hymns"],"features":["qa"],
     "permissions":{"canPresent":false,"canAddToSchedule":true,"canUploadFiles":true}}
""".trimIndent()

/** Something answered, but it is not ChurchPresenter. */
internal const val NOT_CHURCH_PRESENTER = """{"message":"hello from nginx"}"""

/**
 * Opens the sheet on [section]'s page — the server page unless told otherwise,
 * since that is where most of these tests type. Pass null to start on the
 * menu, as the app does on a phone; [openSection] and [backToMenu] then move
 * between pages the way an operator would.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showSettings(
    settings: AppSettings,
    viewModel: SettingsViewModel = SettingsViewModel(settings),
    status: StatusViewModel = statusVm(settings),
    onDismiss: () -> Unit = {},
    onSaved: () -> Unit = {},
    onContact: () -> Unit = {},
    section: SettingsSection? = SettingsSection.SERVER,
    twoPane: Boolean = false,
) = showScreen {
    SettingsScreen(
        appSettings = settings,
        onDismiss = onDismiss,
        onSaved = onSaved,
        onContact = onContact,
        providedViewModel = viewModel,
        providedStatusViewModel = status,
        twoPane = twoPane,
        initialSection = section,
    )
}

/** Taps [section] in the menu (or, on a tablet, in the list beside the page). */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.openSection(section: SettingsSection) = click(UiTags.settingsSection(section))

/** Taps the back arrow on a phone's page, returning to the menu. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.backToMenu() = click(UiTags.SETTINGS_BACK)

/** Leaves the page showing for the menu and opens [section] instead. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.switchTo(section: SettingsSection) {
    backToMenu()
    openSection(section)
}

/**
 * Cancels the sheet from wherever it is. Cancel is on the menu's header; a
 * phone's page has the way back to the menu instead, so from a page this is
 * two taps — the same two the operator makes.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.cancelSheet() {
    if (exists(UiTags.SETTINGS_BACK)) backToMenu()
    click(UiTags.SETTINGS_CANCEL)
}
