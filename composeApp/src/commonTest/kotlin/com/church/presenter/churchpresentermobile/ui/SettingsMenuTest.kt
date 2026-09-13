package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The settings sheet as a menu of pages.
 *
 * A phone opens on the menu and pushes into a page; a tablet keeps the menu
 * beside the page. Either way every control still lives on exactly one page,
 * one draft is shared across all of them, and Save acts on the whole draft
 * from any page — which is what these tests hold the sheet to.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsMenuTest {

    // ── The menu on a phone ──────────────────────────────────────────────

    @Test
    fun thePhoneOpensOnTheMenu() = runComposeUiTest {
        showSettings(storedSettings(), section = null)

        assertTrue(exists(UiTags.settingsSection(SettingsSection.SERVER)))
        assertTrue(exists(UiTags.settingsSection(SettingsSection.ABOUT)))
        assertFalse(exists(UiTags.SETTINGS_HOST))
    }

    @Test
    fun theMenuHasNothingToSave() = runComposeUiTest {
        // Every edit lives on a page, so the menu's header carries no Save.
        showSettings(storedSettings(), section = null)

        assertTrue(exists(UiTags.SETTINGS_CANCEL))
        assertFalse(exists(UiTags.SETTINGS_SAVE))
    }

    @Test
    fun theMenuShowsTheConnection() = runComposeUiTest {
        showSettings(storedSettings(), section = null)

        awaitThat { exists(UiTags.SETTINGS_CONNECTION) }
        assertTrue(isShowing("192.168.1.50:8765"))
    }

    @Test
    fun theMenuCallsARestrictedDesktopLimited() = runComposeUiTest {
        showSettings(storedSettings(), status = statusVm(storedSettings(), body = restrictedDesktop), section = null)

        awaitThat { isShowing("Limited") }
    }

    @Test
    fun theMenuCallsAnAbsentDesktopUnreachable() = runComposeUiTest {
        showSettings(storedSettings(), status = unreachableStatusVm(storedSettings()), section = null)

        awaitThat { isShowing("Unreachable") }
    }

    @Test
    fun theMenuCallsAStrangerNotChurchPresenter() = runComposeUiTest {
        showSettings(storedSettings(), status = statusVm(storedSettings(), body = NOT_CHURCH_PRESENTER), section = null)

        awaitThat { isShowing("Not ChurchPresenter") }
    }

    @Test
    fun tappingARowOpensThatPage() = runComposeUiTest {
        showSettings(storedSettings(), section = null)

        openSection(SettingsSection.DEVICE)

        assertTrue(exists(UiTags.SETTINGS_DEVICE_NAME))
        assertFalse(exists(UiTags.SETTINGS_HOST))
        assertFalse(exists(UiTags.settingsSection(SettingsSection.SERVER)))
    }

    @Test
    fun aPageOffersTheWayBackAndSave() = runComposeUiTest {
        showSettings(storedSettings(), section = null)

        openSection(SettingsSection.SERVER)

        assertTrue(exists(UiTags.SETTINGS_BACK))
        assertTrue(exists(UiTags.SETTINGS_SAVE))
        assertFalse(exists(UiTags.SETTINGS_CANCEL))
    }

    @Test
    fun backReturnsToTheMenu() = runComposeUiTest {
        showSettings(storedSettings(), section = null)
        openSection(SettingsSection.APPEARANCE)

        backToMenu()

        assertTrue(exists(UiTags.settingsSection(SettingsSection.APPEARANCE)))
        assertFalse(exists(UiTags.settingsTheme(0)))
    }

    @Test
    fun backDoesNotCloseTheSheet() = runComposeUiTest {
        var dismissed = 0
        showSettings(storedSettings(), section = null, onDismiss = { dismissed++ })
        openSection(SettingsSection.APPEARANCE)

        backToMenu()

        assertEquals(0, dismissed)
    }

    @Test
    fun theMenuDoesNotOfferTheStandaloneComputerWhileRemote() = runComposeUiTest {
        // The address means something different in each mode; showing both
        // would be two answers to one question.
        showSettings(storedSettings(), section = null)

        assertTrue(exists(UiTags.settingsSection(SettingsSection.SERVER)))
        assertFalse(exists(UiTags.settingsSection(SettingsSection.COMPUTER)))
    }

    // ── One draft across the pages ───────────────────────────────────────

    @Test
    fun anEditSurvivesLeavingThePage() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        switchTo(SettingsSection.DEVICE)
        switchTo(SettingsSection.SERVER)

        assertTrue(isShowing("10.0.0.9"))
    }

    @Test
    fun saveFromAnotherPagePersistsAnEditMadeElsewhere() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        switchTo(SettingsSection.ABOUT)
        click(UiTags.SETTINGS_SAVE)

        awaitThat { settings.host == "10.0.0.9" }
    }

    @Test
    fun cancelFromTheMenuDiscardsAnEditMadeOnAPage() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.50")
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        cancelSheet()

        assertEquals("192.168.1.50", settings.host)
    }

    // ── Each page owns only its own controls ─────────────────────────────

    @Test
    fun theServerPageHasTheAddressAndTheStatusCheck() = runComposeUiTest {
        showSettings(storedSettings(), section = SettingsSection.SERVER)

        assertTrue(exists(UiTags.SETTINGS_HOST))
        assertTrue(exists(UiTags.SETTINGS_PORT))
        assertTrue(exists(UiTags.SETTINGS_API_KEY))
        assertTrue(exists(UiTags.SETTINGS_CHECK_STATUS))
        assertFalse(exists(UiTags.SETTINGS_DEVICE_NAME))
        assertFalse(exists(UiTags.SETTINGS_TELEMETRY))
    }

    @Test
    fun theDevicePageHasTheTwoNames() = runComposeUiTest {
        showSettings(storedSettings(), section = SettingsSection.DEVICE)

        assertTrue(exists(UiTags.SETTINGS_DEVICE_NAME))
        assertTrue(exists(UiTags.SETTINGS_DISPLAY_NAME))
        assertFalse(exists(UiTags.SETTINGS_HOST))
    }

    @Test
    fun theDiagnosticsPageHasTheConnectionAndTheSwitch() = runComposeUiTest {
        showSettings(storedSettings(), section = SettingsSection.DIAGNOSTICS)

        awaitThat { exists(UiTags.SETTINGS_CONNECTION) }
        assertTrue(exists(UiTags.SETTINGS_TELEMETRY))
        assertFalse(exists(UiTags.SETTINGS_HOST))
    }

    @Test
    fun theAboutPageHasContactAndTheVersions() = runComposeUiTest {
        showSettings(storedSettings(), section = SettingsSection.ABOUT)

        assertTrue(exists(UiTags.SETTINGS_CONTACT))
        awaitThat { isShowing("1.4.2") }
    }

    // ── The tablet ───────────────────────────────────────────────────────

    @Test
    fun theTabletOpensOnTheFirstPageWithTheListBesideIt() = runComposeUiTest {
        showSettings(storedSettings(), section = null, twoPane = true)

        assertTrue(exists(UiTags.settingsSection(SettingsSection.SERVER)))
        assertTrue(exists(UiTags.SETTINGS_HOST))
        assertTrue(exists(UiTags.SETTINGS_SAVE))
        assertTrue(exists(UiTags.SETTINGS_CANCEL))
        assertFalse(exists(UiTags.SETTINGS_BACK))
    }

    @Test
    fun theTabletListSwitchesThePage() = runComposeUiTest {
        showSettings(storedSettings(), section = null, twoPane = true)

        openSection(SettingsSection.APPEARANCE)

        assertTrue(exists(UiTags.settingsTheme(0)))
        assertFalse(exists(UiTags.SETTINGS_HOST))
        // The list stays beside the page.
        assertTrue(exists(UiTags.settingsSection(SettingsSection.SERVER)))
    }

    @Test
    fun theTabletPreviewsAChangedAddressUnderThePairedFields() = runComposeUiTest {
        showSettings(storedSettings(host = "192.168.1.50"), twoPane = true)

        assertFalse(exists(UiTags.SETTINGS_DRAFT_URL))
        type(UiTags.SETTINGS_HOST, "10.0.0.9")

        assertTrue(exists(UiTags.SETTINGS_DRAFT_URL))
    }

    @Test
    fun theTabletKeepsTheDraftAcrossPages() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, twoPane = true)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        openSection(SettingsSection.DEVICE)
        click(UiTags.SETTINGS_SAVE)

        awaitThat { settings.host == "10.0.0.9" }
    }
}
