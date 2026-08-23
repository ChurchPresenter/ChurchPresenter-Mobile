package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The address as edited in the sync sheet, which has no Save button.
 *
 * Writing through as it is typed is the whole point: the next tap on Copy builds its URL from
 * `settings.apiBaseUrl`, so anything still sitting in a draft would be ignored.
 */
class DesktopAddressViewModelTest {

    private fun vm(): Pair<DesktopAddressViewModel, AppSettings> {
        val settings = AppSettings(InMemorySettingsStorage())
        return DesktopAddressViewModel(settings) to settings
    }

    @Test
    fun aTypedHostReachesSettingsWithoutASaveButton() = runVmTest {
        val (viewModel, settings) = vm()

        viewModel.setHost("10.0.0.5")

        assertEquals("10.0.0.5", settings.host)
    }

    @Test
    fun aHalfTypedPortIsNotWrittenButIsStillShown() = runVmTest {
        // "87" on the way to "8765" must not briefly point the app at port 87.
        val (viewModel, settings) = vm()
        val original = settings.port

        viewModel.setPort("70000")

        assertEquals("70000", viewModel.port.value)
        assertEquals(original, settings.port)
        assertTrue(viewModel.portError.value)
    }

    @Test
    fun aUsablePortIsWrittenImmediately() = runVmTest {
        val (viewModel, settings) = vm()

        viewModel.setPort("9000")

        assertEquals(9000, settings.port)
        assertFalse(viewModel.portError.value)
    }

    @Test
    fun theKeyFieldStaysHiddenUntilSomethingAsksForOne() = runVmTest {
        // Most desktops have no key set, and a field asking for a secret reads as a requirement.
        val (viewModel, _) = vm()
        assertFalse(viewModel.keyRequired.value)

        viewModel.revealKeyField()

        assertTrue(viewModel.keyRequired.value)
    }

    @Test
    fun aKeyAlreadySavedIsShownWithoutBeingAskedFor() = runVmTest {
        val settings = AppSettings(InMemorySettingsStorage()).apply { apiKey = "secret" }

        assertTrue(DesktopAddressViewModel(settings).keyRequired.value)
    }

    @Test
    fun aScannedCodeFillsAllThreeAtOnce() = runVmTest {
        val (viewModel, settings) = vm()

        viewModel.applyScannedUrl("churchpresenter://connect?host=10.1.1.9&port=9100&apikey=zzz")

        assertEquals("10.1.1.9", settings.host)
        assertEquals(9100, settings.port)
        assertEquals("zzz", settings.apiKey)
        // And the fields on screen follow, rather than showing what was there before.
        assertEquals("10.1.1.9", viewModel.host.value)
        assertEquals("9100", viewModel.port.value)
        assertTrue(viewModel.keyRequired.value)
    }

    @Test
    fun somethingThatIsNotAConnectCodeChangesNothing() = runVmTest {
        val (viewModel, settings) = vm()
        val original = settings.host

        viewModel.applyScannedUrl("https://example.com")

        assertEquals(original, settings.host)
    }

    @Test
    fun anAddressChangedElsewhereIsPickedUp() = runVmTest {
        // The Settings sheet and this one write the same fields; a stale draft would send the
        // next copy to the old computer.
        val (viewModel, settings) = vm()
        settings.host = "10.9.9.9"

        viewModel.reloadFromStorage()

        assertEquals("10.9.9.9", viewModel.host.value)
    }
}
