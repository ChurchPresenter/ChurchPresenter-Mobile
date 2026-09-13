package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.GearButton
import com.church.presenter.churchpresentermobile.ui.ModePickerScreen
import com.church.presenter.churchpresentermobile.ui.NOT_CHURCH_PRESENTER
import com.church.presenter.churchpresentermobile.ui.NavRail
import com.church.presenter.churchpresentermobile.ui.ScreenHeader
import com.church.presenter.churchpresentermobile.ui.ServerStatusDialog
import com.church.presenter.churchpresentermobile.ui.SettingsScreen
import com.church.presenter.churchpresentermobile.ui.SettingsSection
import com.church.presenter.churchpresentermobile.ui.SplashScreen
import com.church.presenter.churchpresentermobile.ui.StatusScreen
import com.church.presenter.churchpresentermobile.ui.UiTags
import com.church.presenter.churchpresentermobile.ui.healthyDesktop
import com.church.presenter.churchpresentermobile.ui.restrictedDesktop
import com.church.presenter.churchpresentermobile.ui.statusVm
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.SettingsViewModel
import io.ktor.http.HttpStatusCode
import kotlin.test.Test

/**
 * Everything between opening the app and being connected, on a tablet — the
 * splash, the mode picker, the startup status screen, the settings sheet and
 * its status modal — laid out across a 1366dp window rather than stacked.
 *
 * The same states [SetupScreenshotTest] and [SettingsScreenshotTest] capture
 * at phone width. A tablet arrangement that lost a state would be a golden
 * nobody noticed missing, so each state the phone has is here too.
 */
@OptIn(ExperimentalTestApi::class)
class TabletSetupScreenshotTest {

    private fun settings(host: String = "", apiKey: String = ""): AppSettings =
        AppSettings(InMemorySettingsStorage()).apply {
            if (host.isNotEmpty()) this.host = host
            if (apiKey.isNotEmpty()) this.apiKey = apiKey
        }

    // ── Splash and first-run ─────────────────────────────────────────────

    @Test
    fun splash() = tablet("splash-tablet__initial") {
        // The field behind the cross is the screen's own size on any density;
        // a glow with a radius in pixels once drew as a square here.
        SplashScreen(onComplete = {}, twoPane = true)
    }

    @Test
    fun modePickerRemote() = tablet("mode-picker-tablet__remote-selected") {
        ModePickerScreen(onModeChosen = {}, twoPane = true)
    }

    @Test
    fun modePickerStandalone() = tablet("mode-picker-tablet__standalone-selected") {
        ModePickerScreen(onModeChosen = {}, twoPane = true, initialMode = AppMode.STANDALONE)
    }

    // ── Startup status ───────────────────────────────────────────────────

    @Test
    fun statusHealthy() = tablet("status-tablet__healthy", until = { drew(UiTags.STATUS_ALL_GOOD) }) {
        status(body = healthyDesktop)
    }

    @Test
    fun statusRestricted() = tablet("status-tablet__restricted", until = { drew(UiTags.STATUS_WARNINGS) }) {
        // Five issues: the grid's odd last card spans the row.
        status(body = restrictedDesktop)
    }

    @Test
    fun statusNotChurchPresenter() = tablet(
        "status-tablet__not-churchpresenter",
        until = { drew(UiTags.STATUS_ERROR) },
    ) {
        status(body = NOT_CHURCH_PRESENTER)
    }

    @Test
    fun statusUnreachable() = tablet("status-tablet__unreachable", until = { drew(UiTags.STATUS_ERROR) }) {
        status(body = "", httpStatus = HttpStatusCode.ServiceUnavailable)
    }

    @Composable
    private fun status(body: String, httpStatus: HttpStatusCode = HttpStatusCode.OK) {
        StatusScreen(
            viewModel = statusVm(settings(), body = body, status = httpStatus),
            onContinue = {},
            onOpenSettings = {},
            twoPane = true,
        )
    }

    // ── Settings: the list beside each page ──────────────────────────────

    @Test
    fun settingsServer() = tablet("settings-tablet__server", dialog = true) { settingsSheet(SettingsSection.SERVER) }

    @Test
    fun settingsDevice() = tablet("settings-tablet__device", dialog = true) { settingsSheet(SettingsSection.DEVICE) }

    @Test
    fun settingsAppearance() = tablet("settings-tablet__appearance", dialog = true) {
        settingsSheet(SettingsSection.APPEARANCE)
    }

    @Test
    fun settingsDiagnostics() = tablet(
        "settings-tablet__diagnostics",
        dialog = true,
        until = { drew(UiTags.SETTINGS_CONNECTION) },
    ) {
        settingsSheet(SettingsSection.DIAGNOSTICS)
    }

    @Test
    fun settingsAbout() = tablet("settings-tablet__about", dialog = true) { settingsSheet(SettingsSection.ABOUT) }

    @Composable
    private fun settingsSheet(section: SettingsSection) {
        val appSettings = settings(host = "192.168.1.10", apiKey = "abc123")
        SettingsScreen(
            appSettings = appSettings,
            onDismiss = {},
            onSaved = {},
            onContact = {},
            providedViewModel = SettingsViewModel(appSettings),
            providedStatusViewModel = statusVm(appSettings, body = healthyDesktop),
            twoPane = true,
            initialSection = section,
        )
    }

    // ── Check Server Status modal ────────────────────────────────────────

    @Test
    fun statusDialogHealthy() = tablet(
        "server-status-dialog-tablet__healthy",
        dialog = true,
        until = { drew(UiTags.STATUS_DIALOG_CONNECTED) },
    ) {
        statusDialog(body = healthyDesktop)
    }

    @Test
    fun statusDialogRestricted() = tablet(
        "server-status-dialog-tablet__restricted",
        dialog = true,
        until = { drew(UiTags.STATUS_DIALOG_WARNINGS) },
    ) {
        statusDialog(body = restrictedDesktop)
    }

    @Test
    fun statusDialogWrongServer() = tablet(
        "server-status-dialog-tablet__wrong-server",
        dialog = true,
        until = { drew(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER) },
    ) {
        statusDialog(body = NOT_CHURCH_PRESENTER)
    }

    @Composable
    private fun statusDialog(body: String) {
        val appSettings = settings(host = "192.168.1.10")
        ServerStatusDialog(
            statusViewModel = statusVm(appSettings, body = body),
            onDismiss = {},
            twoPane = true,
            address = appSettings.apiBaseUrl,
        )
    }

    // ── The shell's corners ──────────────────────────────────────────────

    @Test
    fun shellCorners() = tablet("shell-tablet__corners") {
        // Where the hamburger and the gear live beside a rail: the rail's
        // top-left and the window's top-right, not each pane's header.
        val colors = LocalAppColors.current
        Row(Modifier.fillMaxSize()) {
            NavRail(selectedTab = AppTab.SONGS, onTabSelected = {}, onMenu = {})
            Box(Modifier.weight(1f).fillMaxSize()) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.width(380.dp).fillMaxHeight()) { ScreenHeader(title = "Songs") }
                    VerticalDivider(color = colors.borderSubtle)
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        ScreenHeader(title = "Amazing Grace", subtitle = "Hymns", largeTitle = false)
                    }
                }
                GearButton(
                    onClick = {},
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 20.dp),
                )
            }
        }
    }

    // ── Frame ────────────────────────────────────────────────────────────

    private fun ComposeUiTest.drew(tag: String): Boolean =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    /** A tablet-wide subject on a tablet-sized window, so the second pane is not clipped off. */
    private fun tablet(
        name: String,
        dialog: Boolean = false,
        until: (ComposeUiTest.() -> Boolean)? = null,
        content: @Composable () -> Unit,
    ) = screenshot(
        name = name,
        width = null,
        dialog = dialog,
        surface = Screenshots.TABLET_SURFACE,
        until = until,
        content = content,
    )
}
