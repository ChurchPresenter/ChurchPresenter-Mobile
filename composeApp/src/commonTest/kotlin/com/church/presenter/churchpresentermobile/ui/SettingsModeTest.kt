package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppModeHolder
import com.church.presenter.churchpresentermobile.model.supportsStandalone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Mode page, and what the rest of the sheet looks like once the phone is
 * the presenter.
 *
 * Only a platform that can present standalone has any of this: elsewhere
 * [AppModeHolder] coerces every request back to remote and the page is not in
 * the menu, so each test steps aside there rather than asserting on a control
 * the build cannot offer. On Android and iOS — the builds a church actually
 * runs — every one of them runs.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsModeTest {

    @AfterTest
    fun leaveRemote() = AppModeHolder.resetForTest()

    // ── Switching ────────────────────────────────────────────────────────

    @Test
    fun theModePageOffersBothModes() = runComposeUiTest {
        if (!supportsStandalone) return@runComposeUiTest
        showSettings(storedSettings(), section = SettingsSection.MODE)

        assertTrue(exists(UiTags.settingsMode(0)))
        assertTrue(exists(UiTags.settingsMode(1)))
    }

    @Test
    fun tappingTheOtherModeAsksFirst() = runComposeUiTest {
        // Switching redirects where everything projects; that is not a thing
        // to do on a mis-tap.
        if (!supportsStandalone) return@runComposeUiTest
        showSettings(storedSettings(), section = SettingsSection.MODE)

        click(UiTags.settingsMode(1))

        assertTrue(exists(UiTags.MODE_SWITCH_CONFIRM))
        assertEquals(AppMode.REMOTE, AppModeHolder.mode.value)
    }

    @Test
    fun tappingTheCurrentModeAsksNothing() = runComposeUiTest {
        if (!supportsStandalone) return@runComposeUiTest
        showSettings(storedSettings(), section = SettingsSection.MODE)

        click(UiTags.settingsMode(0))

        assertFalse(exists(UiTags.MODE_SWITCH_CONFIRM))
    }

    @Test
    fun confirmingSwitchesTheMode() = runComposeUiTest {
        if (!supportsStandalone) return@runComposeUiTest
        val settings = storedSettings()
        showSettings(settings, section = SettingsSection.MODE)

        click(UiTags.settingsMode(1))
        click(UiTags.MODE_SWITCH_CONFIRM)

        assertEquals(AppMode.STANDALONE, AppModeHolder.mode.value)
        assertEquals(AppMode.STANDALONE, settings.appMode)
        assertFalse(exists(UiTags.MODE_SWITCH_CONFIRM))
    }

    @Test
    fun cancellingKeepsTheMode() = runComposeUiTest {
        if (!supportsStandalone) return@runComposeUiTest
        val settings = storedSettings()
        showSettings(settings, section = SettingsSection.MODE)

        click(UiTags.settingsMode(1))
        click(UiTags.MODE_SWITCH_CANCEL)

        assertEquals(AppMode.REMOTE, AppModeHolder.mode.value)
        assertFalse(exists(UiTags.MODE_SWITCH_CONFIRM))
    }

    @Test
    fun switchingBackAsksAgain() = runComposeUiTest {
        if (!supportsStandalone) return@runComposeUiTest
        val settings = storedSettings()
        AppModeHolder.set(settings, AppMode.STANDALONE)
        showSettings(settings, section = SettingsSection.MODE)

        click(UiTags.settingsMode(0))

        assertTrue(exists(UiTags.MODE_SWITCH_CONFIRM))
    }

    // ── The sheet once the phone is the presenter ────────────────────────

    @Test
    fun standaloneOffersTheComputerNotTheServer() = runComposeUiTest {
        // The address means something different in each mode: a server to
        // drive, or a computer to copy songs from. Never both.
        if (!supportsStandalone) return@runComposeUiTest
        val settings = storedSettings()
        AppModeHolder.set(settings, AppMode.STANDALONE)
        showSettings(settings, section = null)

        assertTrue(exists(UiTags.settingsSection(SettingsSection.COMPUTER)))
        assertFalse(exists(UiTags.settingsSection(SettingsSection.SERVER)))
        assertFalse(exists(UiTags.settingsSection(SettingsSection.DEVICE)))
    }

    @Test
    fun standaloneHasNoConnectionToReport() = runComposeUiTest {
        // There is no desktop, so "unreachable" would be a false alarm.
        if (!supportsStandalone) return@runComposeUiTest
        val settings = storedSettings()
        AppModeHolder.set(settings, AppMode.STANDALONE)
        showSettings(settings, section = null)

        assertFalse(exists(UiTags.SETTINGS_CONNECTION))
    }

    @Test
    fun standaloneComputerPageHasTheAddress() = runComposeUiTest {
        if (!supportsStandalone) return@runComposeUiTest
        val settings = storedSettings()
        AppModeHolder.set(settings, AppMode.STANDALONE)
        showSettings(settings, section = SettingsSection.COMPUTER)

        assertTrue(exists(UiTags.SETTINGS_COMPUTER_SECTION))
        assertFalse(exists(UiTags.SETTINGS_SERVER_SECTION))
        assertFalse(exists(UiTags.SETTINGS_CHECK_STATUS))
    }

    @Test
    fun standaloneDiagnosticsKeepsTheSwitchAndDropsTheConnection() = runComposeUiTest {
        if (!supportsStandalone) return@runComposeUiTest
        val settings = storedSettings()
        AppModeHolder.set(settings, AppMode.STANDALONE)
        showSettings(settings, section = SettingsSection.DIAGNOSTICS)

        assertTrue(exists(UiTags.SETTINGS_TELEMETRY))
        assertFalse(exists(UiTags.SETTINGS_CONNECTION))
    }

    @Test
    fun standaloneAboutNamesNoServerVersion() = runComposeUiTest {
        if (!supportsStandalone) return@runComposeUiTest
        val settings = storedSettings()
        AppModeHolder.set(settings, AppMode.STANDALONE)
        showSettings(settings, section = SettingsSection.ABOUT)

        assertTrue(exists(UiTags.SETTINGS_CONTACT))
        assertFalse(isShowing("1.4.2"))
    }
}
