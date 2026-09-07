package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.viewmodel.SettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The parts of Settings that are not about the desktop: appearance, privacy,
 * getting in touch, and the debug-only developer section.
 *
 * Two of these behave differently from everything else on the sheet and that is
 * the point of testing them here. The theme applies as soon as it is picked, so
 * the operator can see it — but it is only *persisted* on Save. Telemetry is the
 * opposite: it is written through immediately, because nobody should have to tap
 * Save to stop being tracked.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsPreferencesTest {

    // ── Appearance ───────────────────────────────────────────────────────

    @Test
    fun allThreeAppearanceChoicesAreOffered() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.settingsTheme(0)))
        assertTrue(exists(UiTags.settingsTheme(1)))
        assertTrue(exists(UiTags.settingsTheme(2)))
    }

    @Test
    fun theSavedThemeIsTheOneSelected() = runComposeUiTest {
        val settings = storedSettings()
        settings.themeMode = ThemeMode.DARK
        showSettings(settings)

        tagged(UiTags.settingsTheme(2)).assertIsSelected()
    }

    @Test
    fun theOtherThemesAreNotSelected() = runComposeUiTest {
        val settings = storedSettings()
        settings.themeMode = ThemeMode.DARK
        showSettings(settings)

        tagged(UiTags.settingsTheme(0)).assertIsNotSelected()
        tagged(UiTags.settingsTheme(1)).assertIsNotSelected()
    }

    @Test
    fun systemIsSelectedByDefault() = runComposeUiTest {
        val settings = storedSettings()
        settings.themeMode = ThemeMode.SYSTEM
        showSettings(settings)

        tagged(UiTags.settingsTheme(0)).assertIsSelected()
    }

    @Test
    fun pickingLightSelectsLight() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        click(UiTags.settingsTheme(1))

        assertEquals(ThemeMode.LIGHT, vm.themeMode.value)
    }

    @Test
    fun pickingDarkSelectsDark() = runComposeUiTest {
        val settings = storedSettings()
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        click(UiTags.settingsTheme(2))

        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }

    @Test
    fun theChosenThemeIsMarkedSelected() = runComposeUiTest {
        showSettings(storedSettings())

        click(UiTags.settingsTheme(1))

        tagged(UiTags.settingsTheme(1)).assertIsSelected()
    }

    @Test
    fun choosingAThemeDeselectsTheOldOne() = runComposeUiTest {
        showSettings(storedSettings())

        click(UiTags.settingsTheme(2))

        tagged(UiTags.settingsTheme(0)).assertIsNotSelected()
    }

    @Test
    fun aThemeIsNotPersistedUntilSaved() = runComposeUiTest {
        val settings = storedSettings()
        settings.themeMode = ThemeMode.SYSTEM
        showSettings(settings)

        click(UiTags.settingsTheme(2))

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
    }

    @Test
    fun savingPersistsTheTheme() = runComposeUiTest {
        val settings = storedSettings()
        settings.themeMode = ThemeMode.SYSTEM
        showSettings(settings)

        click(UiTags.settingsTheme(2))
        click(UiTags.SETTINGS_SAVE)

        assertEquals(ThemeMode.DARK, settings.themeMode)
    }

    @Test
    fun cancellingForgetsTheTheme() = runComposeUiTest {
        val settings = storedSettings()
        settings.themeMode = ThemeMode.SYSTEM
        val vm = SettingsViewModel(settings)
        showSettings(settings, viewModel = vm)

        click(UiTags.settingsTheme(2))
        click(UiTags.SETTINGS_CANCEL)

        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)
    }

    @Test
    fun aBadHostBlocksTheThemeFromBeingSaved() = runComposeUiTest {
        // Everything on the sheet saves together; a refused address means the
        // whole save is refused, theme included.
        val settings = storedSettings()
        settings.themeMode = ThemeMode.SYSTEM
        showSettings(settings)

        click(UiTags.settingsTheme(2))
        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_SAVE)

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
    }

    // ── Privacy ──────────────────────────────────────────────────────────

    @Test
    fun theTelemetrySwitchIsOffered() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_TELEMETRY))
    }

    @Test
    fun theSwitchShowsTelemetryIsOn() = runComposeUiTest {
        val settings = storedSettings()
        settings.isTelemetryEnabled = true
        showSettings(settings)

        tagged(UiTags.SETTINGS_TELEMETRY).assertIsOn()
    }

    @Test
    fun theSwitchShowsTelemetryIsOff() = runComposeUiTest {
        val settings = storedSettings()
        settings.isTelemetryEnabled = false
        showSettings(settings)

        tagged(UiTags.SETTINGS_TELEMETRY).assertIsOff()
    }

    @Test
    fun turningTelemetryOffAppliesImmediately() = runComposeUiTest {
        // No Save tap: nobody should have to agree twice to stop being tracked.
        val settings = storedSettings()
        settings.isTelemetryEnabled = true
        showSettings(settings)

        click(UiTags.SETTINGS_TELEMETRY)

        assertFalse(settings.isTelemetryEnabled)
    }

    @Test
    fun turningTelemetryOnAppliesImmediately() = runComposeUiTest {
        val settings = storedSettings()
        settings.isTelemetryEnabled = false
        showSettings(settings)

        click(UiTags.SETTINGS_TELEMETRY)

        assertTrue(settings.isTelemetryEnabled)
    }

    @Test
    fun theSwitchFollowsTheChange() = runComposeUiTest {
        val settings = storedSettings()
        settings.isTelemetryEnabled = false
        showSettings(settings)

        click(UiTags.SETTINGS_TELEMETRY)

        tagged(UiTags.SETTINGS_TELEMETRY).assertIsOn()
    }

    @Test
    fun cancellingDoesNotUndoATelemetryChange() = runComposeUiTest {
        // It was applied immediately and deliberately; Cancel belongs to the
        // draft fields, not to a privacy decision already acted on.
        val settings = storedSettings()
        settings.isTelemetryEnabled = true
        showSettings(settings)

        click(UiTags.SETTINGS_TELEMETRY)
        click(UiTags.SETTINGS_CANCEL)

        assertFalse(settings.isTelemetryEnabled)
    }

    @Test
    fun aBadHostDoesNotBlockATelemetryChange() = runComposeUiTest {
        val settings = storedSettings()
        settings.isTelemetryEnabled = true
        showSettings(settings)

        type(UiTags.SETTINGS_HOST, "")
        click(UiTags.SETTINGS_TELEMETRY)

        assertFalse(settings.isTelemetryEnabled)
    }

    // ── Contact ──────────────────────────────────────────────────────────

    @Test
    fun contactIsOffered() = runComposeUiTest {
        // Settings is where people look for a way to reach support.
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_CONTACT))
    }

    @Test
    fun contactOpensTheForm() = runComposeUiTest {
        var contacted = 0
        showSettings(storedSettings(), onContact = { contacted++ })

        click(UiTags.SETTINGS_CONTACT)

        assertEquals(1, contacted)
    }

    @Test
    fun contactDoesNotSaveTheSheet() = runComposeUiTest {
        var saved = 0
        showSettings(storedSettings(), onSaved = { saved++ })

        click(UiTags.SETTINGS_CONTACT)

        assertEquals(0, saved)
    }

    @Test
    fun contactDoesNotCloseTheSheet() = runComposeUiTest {
        var dismissed = 0
        showSettings(storedSettings(), onDismiss = { dismissed++ })

        click(UiTags.SETTINGS_CONTACT)

        assertEquals(0, dismissed)
    }

    // ── The developer section (debug builds only) ────────────────────────

    @Test
    fun theTestErrorButtonIsOfferedInADebugBuild() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_TEST_ERROR))
    }

    @Test
    fun nothingIsSentUntilTheButtonIsPressed() = runComposeUiTest {
        showSettings(storedSettings())

        assertFalse(exists(UiTags.SETTINGS_TEST_ERROR_SENT))
    }

    @Test
    fun sendingATestErrorIsConfirmedOnScreen() = runComposeUiTest {
        showSettings(storedSettings())

        click(UiTags.SETTINGS_TEST_ERROR)

        assertTrue(exists(UiTags.SETTINGS_TEST_ERROR_SENT))
    }

    @Test
    fun sendingATestErrorDoesNotSaveTheSheet() = runComposeUiTest {
        var saved = 0
        showSettings(storedSettings(), onSaved = { saved++ })

        click(UiTags.SETTINGS_TEST_ERROR)

        assertEquals(0, saved)
    }

    @Test
    fun sendingATestErrorDoesNotCloseTheSheet() = runComposeUiTest {
        var dismissed = 0
        showSettings(storedSettings(), onDismiss = { dismissed++ })

        click(UiTags.SETTINGS_TEST_ERROR)

        assertEquals(0, dismissed)
    }

    @Test
    fun theConfirmationStaysAfterASecondSend() = runComposeUiTest {
        showSettings(storedSettings())
        click(UiTags.SETTINGS_TEST_ERROR)

        click(UiTags.SETTINGS_TEST_ERROR)

        assertTrue(exists(UiTags.SETTINGS_TEST_ERROR_SENT))
    }
}
