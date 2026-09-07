package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.DeepLinkHandler
import com.church.presenter.churchpresentermobile.viewmodel.SettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The settings sheet as a whole.
 *
 * Two things only exist at this level. One is a QR code scanned while the sheet
 * is open: it writes host, port and key straight into storage behind the
 * ViewModel's back, and the fields have to catch up or the next Save would put
 * the old address back. The other is that the sheet saves as a unit — every
 * field at once, or none of them.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsSheetTest {

    private fun connectLink(host: String, port: Int, key: String? = null) =
        "churchpresenter://connect?host=$host&port=$port" + (key?.let { "&apikey=$it" } ?: "")

    // ── A QR code scanned while the sheet is open ────────────────────────

    @Test
    fun aScannedAddressReachesTheHostField() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.50")
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        DeepLinkHandler.handle(connectLink("10.0.0.5", 9000), settings)

        awaitThat { vm.host.value == "10.0.0.5" }
    }

    @Test
    fun aScannedAddressReachesThePortField() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        DeepLinkHandler.handle(connectLink("10.0.0.5", 9000), settings)

        awaitThat { vm.port.value == "9000" }
    }

    @Test
    fun aScannedKeyReachesTheKeyField() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        DeepLinkHandler.handle(connectLink("10.0.0.5", 9000, key = "s3cret"), settings)

        awaitThat { vm.apiKey.value == "s3cret" }
    }

    @Test
    fun aScannedAddressIsShownOnScreen() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        DeepLinkHandler.handle(connectLink("10.0.0.5", 9000), settings)

        awaitThat { isShowing("10.0.0.5") }
    }

    @Test
    fun aScannedAddressReplacesWhatWasBeingTyped() = runComposeUiTest {
        // The operator scanned the code *instead of* finishing typing; the
        // half-typed address must not survive to the next Save.
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)
        type(UiTags.SETTINGS_HOST, "192.168.1.9")

        DeepLinkHandler.handle(connectLink("10.0.0.5", 9000), settings)

        awaitThat { vm.host.value == "10.0.0.5" }
    }

    @Test
    fun aScannedAddressClearsAnyFieldError() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)
        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        DeepLinkHandler.handle(connectLink("10.0.0.5", 9000), settings)

        awaitThat { vm.hostError.value == null }
    }

    @Test
    fun anInvalidLinkChangesNothing() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.50")
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        DeepLinkHandler.handle("churchpresenter://connect?host=&port=9000", settings)

        assertEquals("192.168.1.50", vm.host.value)
    }

    @Test
    fun anUnrelatedLinkChangesNothing() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.50")
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        DeepLinkHandler.handle("https://example.org/connect?host=10.0.0.5&port=9000", settings)

        assertEquals("192.168.1.50", vm.host.value)
    }

    @Test
    fun aScannedAddressIsAlreadySavedSoTheSheetNeedsNoSave() = runComposeUiTest {
        // The deep link writes to storage itself — the fields are only catching up.
        val settings = storedSettings()
        showSettings(settings)

        DeepLinkHandler.handle(connectLink("10.0.0.5", 9000), settings)

        awaitThat { settings.host == "10.0.0.5" }
    }

    @Test
    fun theSheetStaysOpenAfterAScan() = runComposeUiTest {
        var dismissed = 0
        val settings = storedSettings()
        showSettings(settings, onDismiss = { dismissed++ })

        DeepLinkHandler.handle(connectLink("10.0.0.5", 9000), settings)

        awaitThat { isShowing("10.0.0.5") }
        assertEquals(0, dismissed)
    }

    // ── The sheet saves as a unit ────────────────────────────────────────

    @Test
    fun everyFieldIsSavedTogether() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        type(UiTags.SETTINGS_PORT, "9100")
        type(UiTags.SETTINGS_API_KEY, "s3cret")
        type(UiTags.SETTINGS_DEVICE_NAME, "Sound desk")
        type(UiTags.SETTINGS_DISPLAY_NAME, "Sam")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("10.0.0.9", settings.host)
        assertEquals(9100, settings.port)
        assertEquals("s3cret", settings.apiKey)
        assertEquals("Sound desk", settings.customDeviceName)
        assertEquals("Sam", settings.displayName)
    }

    @Test
    fun savingTwiceKeepsTheSecondAnswer() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        click(UiTags.SETTINGS_SAVE)
        type(UiTags.SETTINGS_HOST, "10.0.0.11")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("10.0.0.11", settings.host)
    }

    @Test
    fun savingTwiceReportsBoth() = runComposeUiTest {
        var saved = 0
        showSettings(storedSettings(), onSaved = { saved++ })

        click(UiTags.SETTINGS_SAVE)
        click(UiTags.SETTINGS_SAVE)

        assertEquals(2, saved)
    }

    @Test
    fun changingOneFieldLeavesTheOthersAlone() = runComposeUiTest {
        val settings = storedSettings(apiKey = "existing-key", displayName = "Sam")
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("existing-key", settings.apiKey)
        assertEquals("Sam", settings.displayName)
    }

    @Test
    fun clearingTheKeyRemovesIt() = runComposeUiTest {
        // A desktop that stopped requiring a key needs the phone to stop sending
        // one, and an empty field is how that is said.
        val settings = storedSettings(apiKey = "existing-key")
        showSettings(settings)

        type(UiTags.SETTINGS_API_KEY, "")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("", settings.apiKey)
    }

    @Test
    fun clearingTheDeviceNameGoesBackToTheOsName() = runComposeUiTest {
        val settings = storedSettings(customDeviceName = "Sound desk")
        showSettings(settings)

        type(UiTags.SETTINGS_DEVICE_NAME, "")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("", settings.customDeviceName)
    }

    @Test
    fun clearingTheQaNameRemovesIt() = runComposeUiTest {
        val settings = storedSettings(displayName = "Sam")
        showSettings(settings)

        type(UiTags.SETTINGS_DISPLAY_NAME, "")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("", settings.displayName)
    }

    // ── What the sheet offers ────────────────────────────────────────────

    @Test
    fun theSheetOffersSave() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_SAVE))
    }

    @Test
    fun theSheetOffersCancel() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_CANCEL))
    }

    @Test
    fun theSheetShowsNoStandaloneComputerSectionWhileConnectedToADesktop() = runComposeUiTest {
        // The address means something different in each mode; showing both
        // would be two answers to one question.
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_SERVER_SECTION))
        assertFalse(exists(UiTags.SETTINGS_COMPUTER_SECTION))
    }

    @Test
    fun theSheetOffersAppearance() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.settingsTheme(0)))
    }

    @Test
    fun theSheetOffersPrivacy() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_TELEMETRY))
    }

    @Test
    fun theSheetOffersTheStatusCheck() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_CHECK_STATUS))
    }

    @Test
    fun noModeSwitchDialogIsOpenToBeginWith() = runComposeUiTest {
        showSettings(storedSettings())

        assertFalse(exists(UiTags.MODE_SWITCH_CONFIRM))
    }

    @Test
    fun theSheetSurvivesEverySectionBeingTouched() = runComposeUiTest {
        // A smoke pass over the whole sheet in one composition: the sections do
        // not fight each other for state.
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        click(UiTags.settingsTheme(1))
        click(UiTags.SETTINGS_TEST_ERROR)
        type(UiTags.SETTINGS_DISPLAY_NAME, "Sam")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("10.0.0.9", settings.host)
        assertEquals("Sam", settings.displayName)
    }
}
