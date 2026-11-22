package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.SettingsSection
import kotlin.test.Test

/**
 * Settings on a tablet, the section list beside each page (a tablet always has one open, so
 * there is no menu of its own): the menu in each mode, every page, and every state of the
 * connection the Server page explains.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsTabletScreenshotTest {

    private fun tablet(name: String, content: @Composable () -> Unit) = screenshot(
        name = name,
        width = null,
        dialog = true,
        surface = Screenshots.TABLET_SURFACE,
        content = content,
    )

    // ── Mode ────────────────────────────────────────────────────────────

    @Test
    fun modeRemote() = tablet("settings-tablet__mode-remote") {
        SettingsShot(AppMode.REMOTE, SettingsSection.MODE, twoPane = true)
    }

    @Test
    fun modeStandalone() = tablet("settings-tablet__mode-standalone") {
        SettingsShot(AppMode.STANDALONE, SettingsSection.MODE, twoPane = true)
    }

    @Test
    fun modeCalendar() = tablet("settings-tablet__mode-calendar") {
        SettingsShot(AppMode.CALENDAR, SettingsSection.MODE, twoPane = true)
    }

    // ── Server, every connection state ──────────────────────────────────

    @Test
    fun serverConnected() = tablet("settings-tablet__server-connected") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = true)
    }

    @Test
    fun serverLongLibraryNames() = tablet("settings-tablet__server-long-names") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = true, desktop = Desktop.LONG_NAMES)
    }

    @Test
    fun serverLimited() = tablet("settings-tablet__server-limited") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = true, desktop = Desktop.LIMITED)
    }

    @Test
    fun serverChecking() = tablet("settings-tablet__server-checking") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = true, desktop = Desktop.CHECKING)
    }

    @Test
    fun serverUnreachable() = tablet("settings-tablet__server-unreachable") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = true, desktop = Desktop.UNREACHABLE)
    }

    @Test
    fun serverRejected() = tablet("settings-tablet__server-key-rejected") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = true, desktop = Desktop.REJECTED)
    }

    @Test
    fun serverWrong() = tablet("settings-tablet__server-not-churchpresenter") {
        SettingsShot(AppMode.REMOTE, SettingsSection.SERVER, twoPane = true, desktop = Desktop.WRONG_SERVER)
    }

    // ── The other pages ─────────────────────────────────────────────────

    @Test
    fun device() = tablet("settings-tablet__device") {
        SettingsShot(AppMode.REMOTE, SettingsSection.DEVICE, twoPane = true)
    }

    @Test
    fun appearance() = tablet("settings-tablet__appearance") {
        SettingsShot(AppMode.REMOTE, SettingsSection.APPEARANCE, twoPane = true, theme = ThemeMode.SYSTEM)
    }

    @Test
    fun diagnostics() = tablet("settings-tablet__diagnostics") {
        SettingsShot(AppMode.REMOTE, SettingsSection.DIAGNOSTICS, twoPane = true)
    }

    @Test
    fun diagnosticsStandalone() = tablet("settings-tablet__diagnostics-standalone") {
        SettingsShot(AppMode.STANDALONE, SettingsSection.DIAGNOSTICS, twoPane = true)
    }

    @Test
    fun about() = tablet("settings-tablet__about") {
        SettingsShot(AppMode.REMOTE, SettingsSection.ABOUT, twoPane = true)
    }

    @Test
    fun computer() = tablet("settings-tablet__computer") {
        SettingsShot(AppMode.STANDALONE, SettingsSection.COMPUTER, twoPane = true)
    }

    @Test
    fun display() = tablet("settings-tablet__display") {
        SettingsShot(AppMode.STANDALONE, SettingsSection.DISPLAY, twoPane = true)
    }
}
