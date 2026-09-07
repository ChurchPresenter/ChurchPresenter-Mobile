package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.viewmodel.SettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The desktop's address, as typed into Settings.
 *
 * This is the one screen that can disconnect the phone from the computer, and
 * every field here is judged by what reaches storage rather than by what the
 * field shows. The failures worth catching are the silent ones: a save that
 * keeps a host the HTTP client can never resolve, a Cancel that persists
 * anyway, and a port typed as "80o" quietly becoming something else.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsServerTest {

    // ── The fields that are offered ──────────────────────────────────────

    @Test
    fun theServerSectionIsShown() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_SERVER_SECTION))
    }

    @Test
    fun theHostFieldIsOffered() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_HOST))
    }

    @Test
    fun thePortFieldIsOffered() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_PORT))
    }

    @Test
    fun theApiKeyFieldIsOffered() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_API_KEY))
    }

    @Test
    fun theDeviceNameFieldIsOffered() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_DEVICE_NAME))
    }

    @Test
    fun theQaNameFieldIsOffered() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_DISPLAY_NAME))
    }

    // ── What the sheet opens with ────────────────────────────────────────

    @Test
    fun theSavedHostIsShown() = runComposeUiTest {
        showSettings(storedSettings(host = "10.0.0.7"))

        assertTrue(isShowing("10.0.0.7"))
    }

    @Test
    fun theSavedPortIsShown() = runComposeUiTest {
        showSettings(storedSettings(port = 9100))

        assertTrue(isShowing("9100"))
    }

    @Test
    fun theServerInUseIsNamed() = runComposeUiTest {
        // Not a live connection check — just what the app is pointed at.
        showSettings(storedSettings(host = "10.0.0.7", port = 9100))

        assertTrue(exists(UiTags.SETTINGS_ACTIVE_URL))
        assertTrue(isShowing("http://10.0.0.7:9100/api"))
    }

    @Test
    fun theSavedApiKeyIsLoadedIntoTheField() = runComposeUiTest {
        // Whether it is *masked* is a visual transformation, which the
        // semantics tree cannot see — the field still reports its real value.
        // What is assertable, and what matters, is that the saved key is the one
        // the sheet would save back.
        val settings = storedSettings(apiKey = "s3cret-key")
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        assertEquals("s3cret-key", vm.apiKey.value)
    }

    // ── Saving ───────────────────────────────────────────────────────────

    @Test
    fun savingPersistsTheHost() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("10.0.0.9", settings.host)
    }

    @Test
    fun savingPersistsThePort() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_PORT, "9000")
        click(UiTags.SETTINGS_SAVE)

        assertEquals(9000, settings.port)
    }

    @Test
    fun savingPersistsTheApiKey() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_API_KEY, "s3cret")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("s3cret", settings.apiKey)
    }

    @Test
    fun savingPersistsTheDeviceName() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_DEVICE_NAME, "Sound desk")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("Sound desk", settings.customDeviceName)
    }

    @Test
    fun savingPersistsTheQaName() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_DISPLAY_NAME, "Sam")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("Sam", settings.displayName)
    }

    @Test
    fun savingTrimsTheHost() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "  10.0.0.9  ")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("10.0.0.9", settings.host)
    }

    @Test
    fun savingTrimsTheApiKey() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_API_KEY, "  s3cret  ")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("s3cret", settings.apiKey)
    }

    @Test
    fun savingTrimsTheDeviceName() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_DEVICE_NAME, "  Sound desk  ")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("Sound desk", settings.customDeviceName)
    }

    @Test
    fun savingReportsSuccess() = runComposeUiTest {
        var saved = 0
        showSettings(storedSettings(), onSaved = { saved++ })

        click(UiTags.SETTINGS_SAVE)

        assertEquals(1, saved)
    }

    @Test
    fun savingClosesTheSheet() = runComposeUiTest {
        var dismissed = 0
        showSettings(storedSettings(), onDismiss = { dismissed++ })

        click(UiTags.SETTINGS_SAVE)

        assertEquals(1, dismissed)
    }

    @Test
    fun savingUpdatesTheServerInUse() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        click(UiTags.SETTINGS_SAVE)

        assertTrue(isShowing("http://10.0.0.9:8765/api"))
    }

    // ── A host that cannot work ──────────────────────────────────────────

    @Test
    fun anEmptyHostIsRefused() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        assertNotNull(vm.hostError.value)
    }

    @Test
    fun anEmptyHostIsNotPersisted() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.50")
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("192.168.1.50", settings.host)
    }

    @Test
    fun anEmptyHostDoesNotCloseTheSheet() = runComposeUiTest {
        // The operator has to be able to see and fix the field.
        var dismissed = 0
        showSettings(storedSettings(), onDismiss = { dismissed++ })

        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        assertEquals(0, dismissed)
    }

    @Test
    fun anEmptyHostDoesNotReportSuccess() = runComposeUiTest {
        var saved = 0
        showSettings(storedSettings(), onSaved = { saved++ })

        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        assertEquals(0, saved)
    }

    @Test
    fun aHostOfOnlySpacesIsRefused() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        type(UiTags.SETTINGS_HOST, "    ")
        click(UiTags.SETTINGS_SAVE)

        assertNotNull(vm.hostError.value)
    }

    @Test
    fun aHostThatCannotBeAHostIsRefused() = runComposeUiTest {
        // Not merely unreachable — unusable. Left to the HTTP client it becomes
        // an exception on every call with nothing on screen to explain it.
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        type(UiTags.SETTINGS_HOST, "not a host!")
        click(UiTags.SETTINGS_SAVE)

        assertNotNull(vm.hostError.value)
    }

    @Test
    fun anUnusableHostIsNotPersisted() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.50")
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "not a host!")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("192.168.1.50", settings.host)
    }

    @Test
    fun typingClearsTheHostError() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)
        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")

        assertNull(vm.hostError.value)
    }

    @Test
    fun aFixedHostCanBeSaved() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)
        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("10.0.0.9", settings.host)
    }

    // ── A port that cannot work ──────────────────────────────────────────

    @Test
    fun aNonNumericPortIsRefused() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        type(UiTags.SETTINGS_PORT, "80o")
        click(UiTags.SETTINGS_SAVE)

        assertNotNull(vm.portError.value)
    }

    @Test
    fun aNonNumericPortIsNotPersisted() = runComposeUiTest {
        val settings = storedSettings(port = 8765)
        showSettings(settings)

        type(UiTags.SETTINGS_PORT, "80o")
        click(UiTags.SETTINGS_SAVE)

        assertEquals(8765, settings.port)
    }

    @Test
    fun anEmptyPortIsRefused() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        type(UiTags.SETTINGS_PORT, "")
        click(UiTags.SETTINGS_SAVE)

        assertNotNull(vm.portError.value)
    }

    @Test
    fun portZeroIsRefused() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        type(UiTags.SETTINGS_PORT, "0")
        click(UiTags.SETTINGS_SAVE)

        assertNotNull(vm.portError.value)
    }

    @Test
    fun aPortAboveTheRangeIsRefused() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        type(UiTags.SETTINGS_PORT, "65536")
        click(UiTags.SETTINGS_SAVE)

        assertNotNull(vm.portError.value)
    }

    @Test
    fun theHighestValidPortIsAccepted() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_PORT, "65535")
        click(UiTags.SETTINGS_SAVE)

        assertEquals(65535, settings.port)
    }

    @Test
    fun theLowestValidPortIsAccepted() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_PORT, "1")
        click(UiTags.SETTINGS_SAVE)

        assertEquals(1, settings.port)
    }

    @Test
    fun typingClearsThePortError() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)
        type(UiTags.SETTINGS_PORT, "")
        click(UiTags.SETTINGS_SAVE)

        type(UiTags.SETTINGS_PORT, "8765")

        assertNull(vm.portError.value)
    }

    @Test
    fun aBadPortDoesNotPersistTheHostEither() = runComposeUiTest {
        // The address is one thing; half of it saved is a server nobody can reach.
        val settings = storedSettings(host = "192.168.1.50")
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        type(UiTags.SETTINGS_PORT, "nope")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("192.168.1.50", settings.host)
    }

    @Test
    fun aBadPortStillKeepsTheNames() = runComposeUiTest {
        // Neither name is a reachability field, so a typo in the port must not
        // discard the name the operator just set.
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_DEVICE_NAME, "Sound desk")
        type(UiTags.SETTINGS_PORT, "nope")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("Sound desk", settings.customDeviceName)
    }

    // ── Cancel ───────────────────────────────────────────────────────────

    @Test
    fun cancellingClosesTheSheet() = runComposeUiTest {
        var dismissed = 0
        showSettings(storedSettings(), onDismiss = { dismissed++ })

        click(UiTags.SETTINGS_CANCEL)

        assertEquals(1, dismissed)
    }

    @Test
    fun cancellingSavesNothing() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.50")
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        click(UiTags.SETTINGS_CANCEL)

        assertEquals("192.168.1.50", settings.host)
    }

    @Test
    fun cancellingReportsNoSave() = runComposeUiTest {
        var saved = 0
        showSettings(storedSettings(), onSaved = { saved++ })

        click(UiTags.SETTINGS_CANCEL)

        assertEquals(0, saved)
    }

    @Test
    fun cancellingPutsTheFieldsBack() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.50")
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        type(UiTags.SETTINGS_HOST, "10.0.0.9")
        click(UiTags.SETTINGS_CANCEL)

        assertEquals("192.168.1.50", vm.host.value)
    }

    @Test
    fun cancellingClearsAnyError() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)
        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        click(UiTags.SETTINGS_CANCEL)

        assertNull(vm.hostError.value)
    }

    // ── Reset to defaults ────────────────────────────────────────────────

    @Test
    fun resettingIsOffered() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_RESET))
    }

    @Test
    fun resettingPutsTheDefaultHostInTheField() = runComposeUiTest {
        val settings = storedSettings(host = "10.0.0.9")
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        click(UiTags.SETTINGS_RESET)

        assertEquals(ApiConstants.DEFAULT_HOST, vm.host.value)
    }

    @Test
    fun resettingPutsTheDefaultPortInTheField() = runComposeUiTest {
        val settings = storedSettings(port = 9100)
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        click(UiTags.SETTINGS_RESET)

        assertEquals(ApiConstants.DEFAULT_PORT.toString(), vm.port.value)
    }

    @Test
    fun resettingDoesNotSaveOnItsOwn() = runComposeUiTest {
        // It fills the fields; the operator still has to agree by tapping Save.
        val settings = storedSettings(host = "10.0.0.9")
        showSettings(settings)

        click(UiTags.SETTINGS_RESET)

        assertEquals("10.0.0.9", settings.host)
    }

    @Test
    fun resettingThenSavingPersistsTheDefaults() = runComposeUiTest {
        val settings = storedSettings(host = "10.0.0.9")
        showSettings(settings)

        click(UiTags.SETTINGS_RESET)
        click(UiTags.SETTINGS_SAVE)

        assertEquals(ApiConstants.DEFAULT_HOST, settings.host)
    }

    @Test
    fun resettingClearsAnError() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)
        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        click(UiTags.SETTINGS_RESET)

        assertNull(vm.hostError.value)
    }

    // ── The draft-address preview ────────────────────────────────────────

    @Test
    fun noPreviewIsShownUntilTheAddressChanges() = runComposeUiTest {
        showSettings(storedSettings())

        assertFalse(exists(UiTags.SETTINGS_DRAFT_URL))
    }

    @Test
    fun changingTheHostPreviewsTheNewAddress() = runComposeUiTest {
        showSettings(storedSettings())

        type(UiTags.SETTINGS_HOST, "10.0.0.9")

        assertTrue(exists(UiTags.SETTINGS_DRAFT_URL))
    }

    @Test
    fun thePreviewShowsWhereTheAppWouldConnect() = runComposeUiTest {
        showSettings(storedSettings())

        type(UiTags.SETTINGS_HOST, "10.0.0.9")

        assertTrue(isShowing("http://10.0.0.9:8765/api"))
    }

    @Test
    fun changingThePortPreviewsTheNewAddress() = runComposeUiTest {
        showSettings(storedSettings())

        type(UiTags.SETTINGS_PORT, "9100")

        assertTrue(isShowing("http://192.168.1.50:9100/api"))
    }

    @Test
    fun thePreviewGoesOnceTheAddressIsSaved() = runComposeUiTest {
        showSettings(storedSettings())
        type(UiTags.SETTINGS_HOST, "10.0.0.9")

        click(UiTags.SETTINGS_SAVE)

        assertFalse(exists(UiTags.SETTINGS_DRAFT_URL))
    }

    @Test
    fun typingTheSameAddressBackShowsNoPreview() = runComposeUiTest {
        showSettings(storedSettings(host = "192.168.1.50"))
        type(UiTags.SETTINGS_HOST, "10.0.0.9")

        type(UiTags.SETTINGS_HOST, "192.168.1.50")

        assertFalse(exists(UiTags.SETTINGS_DRAFT_URL))
    }

    @Test
    fun changingOnlyTheApiKeyShowsNoPreview() = runComposeUiTest {
        // The key is not part of the address, so nothing about where the app
        // connects has changed.
        showSettings(storedSettings())

        type(UiTags.SETTINGS_API_KEY, "s3cret")

        assertFalse(exists(UiTags.SETTINGS_DRAFT_URL))
    }

    // ── Addresses typed the way people actually type them ────────────────

    @Test
    fun aPastedUrlKeepsOnlyTheHost() = runComposeUiTest {
        // People paste what the desktop shows them, scheme and all.
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "http://10.0.0.9")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("10.0.0.9", settings.host)
    }

    @Test
    fun aPastedUrlWithAPathKeepsOnlyTheHost() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "http://10.0.0.9/api")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("10.0.0.9", settings.host)
    }

    @Test
    fun aWebSocketUrlKeepsOnlyTheHost() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "ws://10.0.0.9")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("10.0.0.9", settings.host)
    }

    @Test
    fun aHostnameIsAcceptedAsWellAsAnAddress() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "church-desktop.local")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("church-desktop.local", settings.host)
    }

    @Test
    fun aPortTypedWithSpacesIsStillAPort() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings)

        type(UiTags.SETTINGS_PORT, "  9100  ")
        click(UiTags.SETTINGS_SAVE)

        assertEquals(9100, settings.port)
    }

    @Test
    fun aDictatedAddressThatIsJustWordsIsRefused() = runComposeUiTest {
        // The real one: "high dynamic range" arrived from voice input, was
        // accepted, and then failed every request with nothing on screen to say why.
        val settings = storedSettings(host = "192.168.1.50")
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "high dynamic range")
        click(UiTags.SETTINGS_SAVE)

        assertEquals("192.168.1.50", settings.host)
    }
}
