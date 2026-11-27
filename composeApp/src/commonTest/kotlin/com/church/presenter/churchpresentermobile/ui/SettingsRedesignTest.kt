package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsRedesignTest {

    @Test
    fun theServerPageSaysTheDesktopIsConnected() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        awaitThat { isShowing("Connected to the computer") }
        assertTrue(isShowing("192.168.1.10:8765 · server 1.4.2"))
    }

    @Test
    fun theServerPageListsWhatTheDeviceMayDo() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        awaitThat { isShowing("This device can".uppercase()) }
        assertTrue(isShowing("Allowed"))
        assertTrue(isShowing("KJV"))
        assertTrue(isShowing("Hymns"))
    }

    @Test
    fun aRestrictedDesktopExplainsWhatToChangeOnTheComputer() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, status = statusVm(settings, body = restrictedDesktop))

        awaitThat { isShowing("Connected, with limits") }
        assertTrue(isShowing("On the computer, allow this device to present, add to the schedule and upload files."))
        assertTrue(isShowing("Add a Bible and a songbook on the computer — none were found."))
        assertTrue(isShowing("Not allowed"))
        assertTrue(isShowing("None found"))
    }

    @Test
    fun anUnreachableDesktopPointsAtTheHostField() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, status = unreachableStatusVm(settings))

        awaitThat { isShowing("Can’t reach the computer") }
        assertTrue(isShowing("Nothing answered at this address."))
        assertTrue(isShowing("Try again"))
    }

    @Test
    fun aRefusedKeyPointsAtTheKeyField() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10", apiKey = "wrong")
        showSettings(settings, status = statusVm(settings, body = "{}", status = HttpStatusCode.Unauthorized))

        awaitThat { isShowing("API key not accepted") }
        assertTrue(isShowing("The computer didn’t accept this key."))
    }

    @Test
    fun aForbiddenReplyIsTreatedAsARefusedKey() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10", apiKey = "wrong")
        showSettings(settings, status = statusVm(settings, body = "{}", status = HttpStatusCode.Forbidden))

        awaitThat { isShowing("API key not accepted") }
    }

    @Test
    fun aStrangerAtTheAddressIsNamed() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, status = statusVm(settings, body = NOT_CHURCH_PRESENTER))

        awaitThat { isShowing("That isn’t ChurchPresenter") }
    }

    @Test
    fun recheckAsksTheDesktopAgain() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, status = unreachableStatusVm(settings))
        awaitThat { isShowing("Try again") }

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { isShowing("Can’t reach the computer") }
    }

    @Test
    fun theConnectionLineOpensTheServerPage() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, section = null)
        awaitThat { exists(UiTags.SETTINGS_CONNECTION) }

        click(UiTags.SETTINGS_CONNECTION)

        awaitThat { exists(UiTags.SETTINGS_HOST) }
    }

    @Test
    fun theBackLinkReturnsToTheMenu() = runComposeUiTest {
        val settings = storedSettings(host = "192.168.1.10")
        showSettings(settings, section = SettingsSection.DEVICE)

        backToMenu()

        awaitThat { exists(UiTags.settingsSection(SettingsSection.SERVER)) }
        assertFalse(exists(UiTags.SETTINGS_BACK))
    }

    @Test
    fun doneClosesTheSheetFromTheMenu() = runComposeUiTest {
        var dismissed = 0
        showSettings(storedSettings(), section = null, onDismiss = { dismissed++ })

        click(UiTags.SETTINGS_CANCEL)

        assertEquals(1, dismissed)
    }

    @Test
    fun everyPageCanBeSaved() = runComposeUiTest {
        showSettings(storedSettings(host = "192.168.1.10"), section = SettingsSection.SERVER)
        listOf(
            SettingsSection.SERVER,
            SettingsSection.DEVICE,
            SettingsSection.APPEARANCE,
            SettingsSection.DIAGNOSTICS,
            SettingsSection.ABOUT,
        ).forEach { section ->
            switchTo(section)
            assertTrue(exists(UiTags.SETTINGS_SAVE), "$section has no Save")
        }
    }

    @Test
    fun aThemeTileAppliesAtOnce() = runComposeUiTest {
        val settings = storedSettings()
        var applied: ThemeMode? = null
        showSettings(settings, section = SettingsSection.APPEARANCE, onThemeChange = { applied = it })

        click(UiTags.settingsTheme(2))

        assertEquals(ThemeMode.DARK, applied)
        assertEquals(ThemeMode.DARK, settings.themeMode)
    }

    @Test
    fun theDevicePagePreviewsTheNameTheComputerWillSee() = runComposeUiTest {
        showSettings(storedSettings(customDeviceName = "Sound desk"), section = SettingsSection.DEVICE)

        awaitThat { isShowing("“Sound desk” wants to connect") }
    }

    @Test
    fun theTabletListsEveryPageBesideTheOneOpen() = runComposeUiTest {
        showSettings(storedSettings(host = "192.168.1.10"), section = SettingsSection.ABOUT, twoPane = true)

        assertTrue(exists(UiTags.settingsSection(SettingsSection.SERVER)))
        assertTrue(exists(UiTags.settingsSection(SettingsSection.DIAGNOSTICS)))
        assertFalse(exists(UiTags.SETTINGS_BACK))
    }
}
