package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.DesktopAddressFields
import com.church.presenter.churchpresentermobile.ui.NOT_CHURCH_PRESENTER
import com.church.presenter.churchpresentermobile.ui.ServerStatusDialog
import com.church.presenter.churchpresentermobile.ui.SettingsScreen
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
class SettingsScreenshotTest {

    private fun settings(host: String = "", apiKey: String = ""): AppSettings =
        AppSettings(InMemorySettingsStorage()).apply {
            if (host.isNotEmpty()) this.host = host
            if (apiKey.isNotEmpty()) this.apiKey = apiKey
        }

    @Test
    fun settingsConfigured() = screenshot("settings__configured", dialog = true) {
        val appSettings = settings(host = "192.168.1.10", apiKey = "abc123")
        SettingsScreen(
            appSettings = appSettings,
            onDismiss = {},
            onSaved = {},
            onContact = {},
            providedViewModel = SettingsViewModel(appSettings),
            providedStatusViewModel = statusVm(appSettings, body = healthyDesktop),
        )
    }

    @Test
    fun settingsFresh() = screenshot("settings__fresh-install", dialog = true) {
        val appSettings = settings()
        SettingsScreen(
            appSettings = appSettings,
            onDismiss = {},
            onSaved = {},
            onContact = {},
            providedViewModel = SettingsViewModel(appSettings),
            providedStatusViewModel = statusVm(appSettings, body = healthyDesktop),
        )
    }

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
