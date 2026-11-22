package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DevicePermissions
import com.church.presenter.churchpresentermobile.model.ServerStatus
import com.church.presenter.churchpresentermobile.model.StatusWarning
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.ServerDraft
import com.church.presenter.churchpresentermobile.ui.ServerDraftEdits
import com.church.presenter.churchpresentermobile.ui.SettingsPage
import com.church.presenter.churchpresentermobile.ui.SettingsSection
import com.church.presenter.churchpresentermobile.ui.SettingsSheet
import com.church.presenter.churchpresentermobile.ui.settingsSections
import com.church.presenter.churchpresentermobile.ui.standalone.DisplaySettingsPage
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneFixture
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState

/** An iPhone's screen in points, for the phone settings captures. */
internal val PHONE_SURFACE = DpSize(393.dp, 852.dp)

/** The connection states the Server page explains. */
internal enum class Desktop(val state: StatusUiState?) {
    CONNECTED(
        StatusUiState.Success(
            ServerStatus(appVersion = "1.4.2", bibles = listOf("KJV", "ESV"), songbooks = listOf("Hymns")),
            emptyList(),
        ),
    ),
    LONG_NAMES(
        StatusUiState.Success(
            ServerStatus(
                appVersion = "1.4.2",
                bibles = listOf("King James Version (1769) with Strong's numbers", "English Standard Version"),
                songbooks = listOf("Hymns of the Christian Church and Mission, second edition"),
            ),
            emptyList(),
        ),
    ),
    LIMITED(
        StatusUiState.Success(
            ServerStatus(
                appVersion = "1.4.2",
                permissions = DevicePermissions(canPresent = false, canAddToSchedule = false, canUploadFiles = false),
            ),
            listOf(
                StatusWarning.PresentBlocked,
                StatusWarning.ScheduleBlocked,
                StatusWarning.UploadBlocked,
                StatusWarning.NoBibles,
                StatusWarning.NoSongbooks,
            ),
        ),
    ),
    CHECKING(StatusUiState.Loading),
    UNREACHABLE(StatusUiState.Error("Connection refused")),
    REJECTED(StatusUiState.Unauthorized(401)),
    WRONG_SERVER(StatusUiState.NotChurchPresenter("hello from nginx")),
    NONE(null),
}

internal fun shotSettings(): AppSettings = AppSettings(InMemorySettingsStorage()).apply {
    host = "192.168.1.10"
    apiKey = "abc123"
    customDeviceName = "Worship iPhone"
}

/**
 * The settings sheet as a phone or tablet shows it in [mode], open on [page]
 * (null for the menu), with the desktop answering [desktop].
 *
 * Built from the sheet and its pages rather than through `SettingsScreen`, because the JVM
 * cannot leave remote mode and this target has no Mode section — both of which every phone
 * and tablet has.
 */
@Composable
internal fun SettingsShot(
    mode: AppMode,
    page: SettingsSection?,
    twoPane: Boolean,
    desktop: Desktop = if (mode == AppMode.REMOTE) Desktop.CONNECTED else Desktop.NONE,
    theme: ThemeMode = ThemeMode.SYSTEM,
) {
    val settings = remember { shotSettings() }
    val status = desktop.state.takeIf { mode == AppMode.REMOTE }
    val sections = settingsSections(
        hasDesktop = mode == AppMode.REMOTE,
        supportsStandalone = true,
        canLeaveMode = mode != AppMode.REMOTE,
        usesComputer = mode != AppMode.CALENDAR,
        hasDisplay = mode == AppMode.STANDALONE,
    )
    SettingsSheet(
        sections = sections,
        status = status,
        address = "${settings.host}:${settings.port}",
        appMode = mode,
        twoPane = twoPane,
        initialSection = page,
        onDismiss = {},
        onSave = {},
        page = { section, openServer ->
            SettingsPage(
                section = section,
                appSettings = settings,
                appMode = mode,
                activeUrl = settings.apiBaseUrl,
                draft = ServerDraft(
                    host = settings.host,
                    port = settings.port.toString(),
                    apiKey = settings.apiKey,
                    customDeviceName = settings.customDeviceName,
                    displayName = "",
                ),
                edits = ServerDraftEdits({}, {}, {}, {}, {}, {}),
                draftUrl = null,
                themeMode = theme,
                telemetryEnabled = true,
                status = status,
                onModeTapped = {},
                onThemeMode = {},
                onTelemetry = {},
                onCheckStatus = {},
                onContact = {},
                twoPane = twoPane,
                displayPage = { ShotDisplayPage() },
                onOpenServer = openServer,
            )
        },
    )
}

@Composable
private fun ShotDisplayPage() {
    val fixture = remember { StandaloneFixture() }
    val settings = remember { AppSettings(InMemorySettingsStorage()) }
    val viewModel = remember { StandaloneViewModel(fixture.engine, fixture.registry, settings, null) }
    DisplaySettingsPage(
        engine = fixture.engine,
        registry = fixture.registry,
        settings = settings,
        photos = null,
        providedViewModel = viewModel,
    )
}
