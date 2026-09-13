package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The settings pages drawn on their own, with the values the sheet would
 * hand them.
 *
 * Two pages depend on a mode the sheet cannot enter on every platform — the
 * Mode page and the standalone Computer page — and two more change shape
 * with a desktop that is absent or still answering. Handing the page the
 * value directly reaches all of those on every runtime, where going through
 * the sheet would only reach them on a phone that can present.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsPageTest {

    private class Recorder {
        var modeTapped: AppMode? = null
        var theme: ThemeMode? = null
        var telemetry: Boolean? = null
        var checks = 0
        var contacts = 0
    }

    private fun ComposeUiTest.showPage(
        section: SettingsSection,
        appMode: AppMode = AppMode.REMOTE,
        status: StatusUiState? = null,
        draftUrl: String? = null,
        twoPane: Boolean = false,
        recorder: Recorder = Recorder(),
    ) = showScreen {
        SettingsPage(
            section = section,
            appSettings = storedSettings(),
            appMode = appMode,
            activeUrl = "http://192.168.1.50:8765/api",
            draft = ServerDraft("192.168.1.50", "8765", "", "", ""),
            edits = ServerDraftEdits({}, {}, {}, {}, {}, {}),
            draftUrl = draftUrl,
            themeMode = ThemeMode.SYSTEM,
            telemetryEnabled = true,
            status = status,
            onModeTapped = { recorder.modeTapped = it },
            onThemeMode = { recorder.theme = it },
            onTelemetry = { recorder.telemetry = it },
            onCheckStatus = { recorder.checks++ },
            onContact = { recorder.contacts++ },
            twoPane = twoPane,
        )
    }

    // ── Mode ─────────────────────────────────────────────────────────────

    @Test
    fun theModePageOffersBothModes() = runComposeUiTest {
        showPage(SettingsSection.MODE)

        assertTrue(exists(UiTags.SETTINGS_MODE_SECTION))
        assertTrue(exists(UiTags.settingsMode(0)))
        assertTrue(exists(UiTags.settingsMode(1)))
    }

    @Test
    fun tappingTheOtherModeReportsIt() = runComposeUiTest {
        val recorder = Recorder()
        showPage(SettingsSection.MODE, appMode = AppMode.REMOTE, recorder = recorder)

        click(UiTags.settingsMode(1))

        assertEquals(AppMode.STANDALONE, recorder.modeTapped)
    }

    @Test
    fun tappingTheCurrentModeReportsNothing() = runComposeUiTest {
        // Nothing to switch to, so nothing to confirm.
        val recorder = Recorder()
        showPage(SettingsSection.MODE, appMode = AppMode.STANDALONE, recorder = recorder)

        click(UiTags.settingsMode(1))

        assertNull(recorder.modeTapped)
    }

    @Test
    fun theModePageExplainsTheModeInForce() = runComposeUiTest {
        showPage(SettingsSection.MODE, appMode = AppMode.STANDALONE)

        assertTrue(isShowing("no computer needed"))
    }

    // ── Computer (standalone's address) ──────────────────────────────────

    @Test
    fun theComputerPageHasTheAddressAndNoStatusCheck() = runComposeUiTest {
        showPage(SettingsSection.COMPUTER, appMode = AppMode.STANDALONE)

        assertTrue(exists(UiTags.SETTINGS_COMPUTER_SECTION))
        assertFalse(exists(UiTags.SETTINGS_SERVER_SECTION))
        assertFalse(exists(UiTags.SETTINGS_CHECK_STATUS))
    }

    // ── Server ───────────────────────────────────────────────────────────

    @Test
    fun theServerPagePreviewsAChangedAddress() = runComposeUiTest {
        showPage(SettingsSection.SERVER, draftUrl = "http://10.0.0.9:8765/api")

        assertTrue(exists(UiTags.SETTINGS_DRAFT_URL))
        assertTrue(isShowing("10.0.0.9"))
    }

    @Test
    fun theServerPageShowsNoPreviewForTheSavedAddress() = runComposeUiTest {
        showPage(SettingsSection.SERVER, draftUrl = null)

        assertFalse(exists(UiTags.SETTINGS_DRAFT_URL))
    }

    @Test
    fun checkingTheStatusReachesTheSheet() = runComposeUiTest {
        val recorder = Recorder()
        showPage(SettingsSection.SERVER, recorder = recorder)

        click(UiTags.SETTINGS_CHECK_STATUS)

        assertEquals(1, recorder.checks)
    }

    @Test
    fun theTabletPairsHostAndPortAndStillOffersBoth() = runComposeUiTest {
        showPage(SettingsSection.SERVER, twoPane = true)

        assertTrue(exists(UiTags.SETTINGS_HOST))
        assertTrue(exists(UiTags.SETTINGS_PORT))
    }

    // ── Appearance ───────────────────────────────────────────────────────

    @Test
    fun pickingAThemeReportsIt() = runComposeUiTest {
        val recorder = Recorder()
        showPage(SettingsSection.APPEARANCE, recorder = recorder)

        click(UiTags.settingsTheme(2))

        assertEquals(ThemeMode.DARK, recorder.theme)
    }

    // ── Diagnostics ──────────────────────────────────────────────────────

    @Test
    fun diagnosticsWithNoDesktopShowsNoConnection() = runComposeUiTest {
        showPage(SettingsSection.DIAGNOSTICS, status = null)

        assertFalse(exists(UiTags.SETTINGS_CONNECTION))
        assertTrue(exists(UiTags.SETTINGS_TELEMETRY))
    }

    @Test
    fun diagnosticsShowsACheckStillInFlight() = runComposeUiTest {
        showPage(SettingsSection.DIAGNOSTICS, status = StatusUiState.Loading)

        assertTrue(exists(UiTags.SETTINGS_CONNECTION))
        assertTrue(isShowing("Checking"))
    }

    @Test
    fun diagnosticsShowsARejectedKey() = runComposeUiTest {
        showPage(SettingsSection.DIAGNOSTICS, status = StatusUiState.Unauthorized(401))

        assertTrue(isShowing("Rejected"))
    }

    @Test
    fun theSwitchReportsItsNewSetting() = runComposeUiTest {
        val recorder = Recorder()
        showPage(SettingsSection.DIAGNOSTICS, recorder = recorder)

        click(UiTags.SETTINGS_TELEMETRY)

        assertEquals(false, recorder.telemetry)
    }

    // ── About ────────────────────────────────────────────────────────────

    @Test
    fun aboutWithNoDesktopNamesOnlyTheApp() = runComposeUiTest {
        showPage(SettingsSection.ABOUT, status = null)

        assertTrue(exists(UiTags.SETTINGS_CONTACT))
        assertFalse(isShowing("1.4.2"))
    }

    @Test
    fun aboutWithAnUnreachableDesktopNamesNoServerVersion() = runComposeUiTest {
        showPage(SettingsSection.ABOUT, status = StatusUiState.Error("refused"))

        assertFalse(isShowing("1.4.2"))
    }

    @Test
    fun contactReachesTheSheet() = runComposeUiTest {
        val recorder = Recorder()
        showPage(SettingsSection.ABOUT, recorder = recorder)

        click(UiTags.SETTINGS_CONTACT)

        assertEquals(1, recorder.contacts)
    }

    // ── The mode-switch confirmation ─────────────────────────────────────

    @Test
    fun confirmingTheSwitchConfirms() = runComposeUiTest {
        var confirmed = 0
        var dismissed = 0
        showScreen {
            ModeSwitchDialog(target = AppMode.STANDALONE, onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        }

        click(UiTags.MODE_SWITCH_CONFIRM)

        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun cancellingTheSwitchDismisses() = runComposeUiTest {
        var confirmed = 0
        var dismissed = 0
        showScreen {
            ModeSwitchDialog(target = AppMode.REMOTE, onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        }

        click(UiTags.MODE_SWITCH_CANCEL)

        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    @Test
    fun theSwitchNamesWhereProjectionWillGo() = runComposeUiTest {
        showScreen { ModeSwitchDialog(target = AppMode.STANDALONE, onConfirm = {}, onDismiss = {}) }
        assertTrue(isShowing("your own screens"))
    }

    @Test
    fun theSwitchBackNamesTheComputer() = runComposeUiTest {
        showScreen { ModeSwitchDialog(target = AppMode.REMOTE, onConfirm = {}, onDismiss = {}) }
        assertTrue(isShowing("on your computer"))
    }
}
