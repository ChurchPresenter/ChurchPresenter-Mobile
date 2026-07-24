package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests [SettingsViewModel] — validation, persistence, and the reactive draft/urlChanged flows. */
class SettingsViewModelTest {

    private fun vmWith(): Pair<SettingsViewModel, AppSettings> {
        val settings = AppSettings(InMemorySettingsStorage())
        return SettingsViewModel(settings) to settings
    }

    @Test
    fun saveValidPersistsAndInvokesSuccess() = runVmTest {
        val (vm, settings) = vmWith()
        vm.setHost("10.0.0.5")
        vm.setPort("9000")
        vm.setApiKey(" k ")
        var ok = false
        vm.save(onSuccess = { ok = true }, emptyHostError = "EMPTY", invalidPortError = "PORT")

        assertTrue(ok)
        assertEquals("10.0.0.5", settings.host)
        assertEquals(9000, settings.port)
        assertEquals("k", settings.apiKey) // trimmed
        assertNull(vm.hostError.value)
        assertNull(vm.portError.value)
        assertEquals("http://10.0.0.5:9000/api", vm.activeUrl.value)
    }

    @Test
    fun saveBlankHostSetsErrorAndDoesNotPersist() = runVmTest {
        val (vm, settings) = vmWith()
        vm.setHost("   ")
        vm.setPort("8765")
        var ok = false
        vm.save(onSuccess = { ok = true }, emptyHostError = "EMPTY", invalidPortError = "PORT")

        assertFalse(ok)
        assertEquals("EMPTY", vm.hostError.value)
        assertEquals(ApiConstants.DEFAULT_HOST, settings.host) // unchanged
    }

    @Test
    fun saveRejectsOutOfRangeAndNonNumericPort() = runVmTest {
        for (bad in listOf("0", "70000", "abc", "")) {
            val (vm, _) = vmWith()
            vm.setHost("h")
            vm.setPort(bad)
            var ok = false
            vm.save(onSuccess = { ok = true }, emptyHostError = "EMPTY", invalidPortError = "PORT")
            assertFalse(ok, "port '$bad' should be invalid")
            assertEquals("PORT", vm.portError.value)
        }
    }

    @Test
    fun resetToDefaultsSetsDraftFields() = runVmTest {
        val (vm, _) = vmWith()
        vm.setHost("1.2.3.4")
        vm.setPort("1")
        vm.resetToDefaults()
        assertEquals(ApiConstants.DEFAULT_HOST, vm.host.value)
        assertEquals(ApiConstants.DEFAULT_PORT.toString(), vm.port.value)
        assertNull(vm.hostError.value)
    }

    @Test
    fun cancelRestoresFromStorage() = runVmTest {
        val (vm, _) = vmWith()
        vm.setHost("scratch")
        vm.cancel()
        assertEquals(ApiConstants.DEFAULT_HOST, vm.host.value)
    }

    @Test
    fun setTelemetryEnabledPersistsImmediately() = runVmTest {
        val (vm, settings) = vmWith()
        vm.setTelemetryEnabled(false)
        assertFalse(vm.telemetryEnabled.value)
        assertFalse(settings.isTelemetryEnabled)
    }

    @Test
    fun setThemeModeUpdatesState() = runVmTest {
        val (vm, _) = vmWith()
        vm.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }

    @Test
    fun draftBaseUrlTracksHostAndPort() = runVmTest {
        val (vm, _) = vmWith()
        vm.setHost("1.2.3.4")
        vm.setPort("42")
        advanceUntilIdle()
        assertEquals("http://1.2.3.4:42/api", vm.draftBaseUrl.value)
    }

    @Test
    fun urlChangedIsFalseInitiallyAndTrueAfterEdit() = runVmTest {
        val (vm, _) = vmWith()
        advanceUntilIdle()
        assertFalse(vm.urlChanged.value)
        vm.setHost("9.9.9.9")
        advanceUntilIdle()
        assertTrue(vm.urlChanged.value)
    }
}
