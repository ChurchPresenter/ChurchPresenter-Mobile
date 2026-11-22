package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.DesktopAddressFields
import com.church.presenter.churchpresentermobile.ui.SettingsSection
import kotlin.test.Test

/**
 * Settings on a phone: the menu in each mode, every page, and every state of the
 * connection the Server page explains — plus the address fields the sync sheet shares.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsScreenshotTest {

    private fun phone(name: String, content: @Composable () -> Unit) = screenshot(
        name = name,
        width = null,
        dialog = true,
        surface = PHONE_SURFACE,
        content = content,
    )

    // ── Menu, one per mode ──────────────────────────────────────────────

    @Test
    fun menuRemote() = phone("settings__menu-remote") { SettingsShot(AppMode.REMOTE, null, twoPane = false) }

    @Test
    fun menuRemoteUnreachable() = phone("settings__menu-remote-unreachable") {
        SettingsShot(AppMode.REMOTE, null, twoPane = false, desktop = Desktop.UNREACHABLE)
    }

    @Test
    fun menuRemoteLimited() = phone("settings__menu-remote-limited") {
        SettingsShot(AppMode.REMOTE, null, twoPane = false, desktop = Desktop.LIMITED)
    }

    @Test
    fun menuStandalone() = phone("settings__menu-standalone") {
        SettingsShot(AppMode.STANDALONE, null, twoPane = false)
    }

    @Test
    fun menuCalendar() = phone("settings__menu-calendar") { SettingsShot(AppMode.CALENDAR, null, twoPane = false) }

    // ── Mode ────────────────────────────────────────────────────────────

    @Test
    fun modeRemote() = phone("settings__mode-remote") {
        SettingsShot(AppMode.REMOTE, SettingsSection.MODE, twoPane = false)
    }

    @Test
    fun modeStandalone() = phone("settings__mode-standalone") {
        SettingsShot(AppMode.STANDALONE, SettingsSection.MODE, twoPane = false)
    }

    @Test
    fun modeCalendar() = phone("settings__mode-calendar") {
        SettingsShot(AppMode.CALENDAR, SettingsSection.MODE, twoPane = false)
    }

    // ── Server, every connection state ──────────────────────────────────

    @Test
    fun serverConnected() = phone("settings__server-connected") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = false)
    }

    @Test
    fun serverLongLibraryNames() = phone("settings__server-long-names") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = false, desktop = Desktop.LONG_NAMES)
    }

    @Test
    fun serverLimited() = phone("settings__server-limited") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = false, desktop = Desktop.LIMITED)
    }

    @Test
    fun serverChecking() = phone("settings__server-checking") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = false, desktop = Desktop.CHECKING)
    }

    @Test
    fun serverUnreachable() = phone("settings__server-unreachable") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = false, desktop = Desktop.UNREACHABLE)
    }

    @Test
    fun serverRejected() = phone("settings__server-key-rejected") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = false, desktop = Desktop.REJECTED)
    }

    @Test
    fun serverWrong() = phone("settings__server-not-churchpresenter") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = false, desktop = Desktop.WRONG_SERVER)
    }

    // ── The other pages ─────────────────────────────────────────────────

    @Test
    fun device() = phone("settings__device") { SettingsShot(AppMode.REMOTE, SettingsSection.DEVICE, twoPane = false) }

    @Test
    fun appearance() = phone("settings__appearance") {
        SettingsShot(AppMode.REMOTE, SettingsSection.APPEARANCE, twoPane = false, theme = ThemeMode.SYSTEM)
    }

    @Test
    fun diagnostics() = phone("settings__diagnostics") {
        SettingsShot(AppMode.REMOTE, SettingsSection.DIAGNOSTICS, twoPane = false)
    }

    @Test
    fun diagnosticsStandalone() = phone("settings__diagnostics-standalone") {
        SettingsShot(AppMode.STANDALONE, SettingsSection.DIAGNOSTICS, twoPane = false)
    }

    @Test
    fun about() = phone("settings__about") { SettingsShot(AppMode.REMOTE, SettingsSection.ABOUT, twoPane = false) }

    @Test
    fun computer() = phone("settings__computer") {
        SettingsShot(AppMode.STANDALONE, SettingsSection.COMPUTER, twoPane = false)
    }

    @Test
    fun display() = phone("settings__display") {
        SettingsShot(AppMode.STANDALONE, SettingsSection.DISPLAY, twoPane = false)
    }

    // ── The address fields the sync sheet shares ────────────────────────

    private fun settings(host: String = ""): AppSettings =
        AppSettings(InMemorySettingsStorage()).apply { if (host.isNotEmpty()) this.host = host }

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
