package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.DesktopAddressFields
import com.church.presenter.churchpresentermobile.ui.NOT_CHURCH_PRESENTER
import com.church.presenter.churchpresentermobile.ui.ServerStatusDialog
import com.church.presenter.churchpresentermobile.ui.SettingsScreen
import com.church.presenter.churchpresentermobile.ui.SettingsSection
import com.church.presenter.churchpresentermobile.ui.UiTags
import com.church.presenter.churchpresentermobile.ui.unreachableStatusVm
import com.church.presenter.churchpresentermobile.ui.healthyDesktop
import com.church.presenter.churchpresentermobile.ui.restrictedDesktop
import com.church.presenter.churchpresentermobile.ui.statusVm
import com.church.presenter.churchpresentermobile.viewmodel.SettingsViewModel
import kotlin.test.Test

/**
 * Settings, its status dialog, and the address fields they share.
 *
 * Settings is where a volunteer is sent when nothing works, so it is captured
 * both with a computer already saved and empty as it is on a fresh install.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsScreenshotTest {

    private fun settings(host: String = "", apiKey: String = ""): AppSettings =
        AppSettings(InMemorySettingsStorage()).apply {
            if (host.isNotEmpty()) this.host = host
            if (apiKey.isNotEmpty()) this.apiKey = apiKey
        }

    // The sheet opens on a menu of sections; each section is its own page.

    @Test
    fun settingsMenuConfigured() = screenshot(
        "settings__menu-configured",
        dialog = true,
        until = { drew(UiTags.SETTINGS_CONNECTION) },
    ) {
        sheet(settings(host = "192.168.1.10", apiKey = "abc123"))
    }

    @Test
    fun settingsMenuFresh() = screenshot("settings__menu-fresh-install", dialog = true) {
        // No desktop saved yet: the connection line reports it cannot be reached.
        sheet(settings(), reachable = false)
    }

    @Test
    fun settingsServerPage() = screenshot("settings__server", dialog = true) {
        sheet(settings(host = "192.168.1.10", apiKey = "abc123"), page = SettingsSection.SERVER)
    }

    @Test
    fun settingsDevicePage() = screenshot("settings__device", dialog = true) {
        sheet(settings(host = "192.168.1.10"), page = SettingsSection.DEVICE)
    }

    @Test
    fun settingsAppearancePage() = screenshot("settings__appearance", dialog = true) {
        sheet(settings(host = "192.168.1.10"), page = SettingsSection.APPEARANCE)
    }

    @Test
    fun settingsDiagnosticsPage() = screenshot(
        "settings__diagnostics",
        dialog = true,
        until = { drew(UiTags.SETTINGS_CONNECTION) },
    ) {
        sheet(settings(host = "192.168.1.10"), page = SettingsSection.DIAGNOSTICS)
    }

    @Test
    fun settingsAboutPage() = screenshot("settings__about", dialog = true) {
        sheet(settings(host = "192.168.1.10"), page = SettingsSection.ABOUT)
    }

    @Composable
    private fun sheet(appSettings: AppSettings, page: SettingsSection? = null, reachable: Boolean = true) {
        SettingsScreen(
            appSettings = appSettings,
            onDismiss = {},
            onSaved = {},
            onContact = {},
            providedViewModel = SettingsViewModel(appSettings),
            providedStatusViewModel = if (reachable) {
                statusVm(appSettings, body = healthyDesktop)
            } else {
                unreachableStatusVm(appSettings)
            },
            initialSection = page,
        )
    }

    private fun ComposeUiTest.drew(tag: String): Boolean =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun statusDialogHealthy() = screenshot("server-status-dialog__healthy", dialog = true) {
        val appSettings = settings(host = "192.168.1.10")
        ServerStatusDialog(
            statusViewModel = statusVm(appSettings, body = healthyDesktop),
            onDismiss = {},
        )
    }

    @Test
    fun statusDialogRestricted() = screenshot("server-status-dialog__restricted", dialog = true) {
        val appSettings = settings(host = "192.168.1.10")
        ServerStatusDialog(
            statusViewModel = statusVm(appSettings, body = restrictedDesktop),
            onDismiss = {},
        )
    }

    @Test
    fun statusDialogWrongServer() = screenshot("server-status-dialog__wrong-server", dialog = true) {
        // Something answered on the port, but it is not ChurchPresenter — the
        // dialog has to distinguish that from an unreachable computer.
        val appSettings = settings(host = "192.168.1.10")
        ServerStatusDialog(
            statusViewModel = statusVm(appSettings, body = NOT_CHURCH_PRESENTER),
            onDismiss = {},
        )
    }

    @Test
    fun addressFields() = screenshot("desktop-address__with-hint") {
        DesktopAddressFields(settings = settings(host = "192.168.1.10"))
    }

    @Test
    fun addressFieldsWithoutHint() = screenshot("desktop-address__no-hint") {
        DesktopAddressFields(settings = settings(host = "192.168.1.10"), showHint = false)
    }

    @Test
    fun addressFieldsEmpty() = screenshot("desktop-address__empty") {
        DesktopAddressFields(settings = settings())
    }
}
