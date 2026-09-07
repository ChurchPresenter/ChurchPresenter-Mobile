package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.flow.MutableStateFlow
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
        try {
            vm.setHost("10.0.0.5")
            vm.setPort("9000")
            vm.setApiKey(" k ")
            var ok = false
            vm.save(onSuccess = { ok = true }, emptyHostError = "EMPTY", invalidPortError = "PORT", invalidHostError = "HOST")

            assertTrue(ok)
            assertEquals("10.0.0.5", settings.host)
            assertEquals(9000, settings.port)
            assertEquals("k", settings.apiKey) // trimmed
            assertNull(vm.hostError.value)
            assertNull(vm.portError.value)
            assertEquals("http://10.0.0.5:9000/api", vm.activeUrl.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun bothNamesSurviveASaveInStandalone() = runVmTest {
        // Neither name is a server field, so neither may be lost with them. This
        // used to be written only from the server-field path, which standalone
        // never reaches — the name was typed, saved, and silently discarded.
        val settings = AppSettings(InMemorySettingsStorage())
        val vm = SettingsViewModel(settings, MutableStateFlow(AppMode.STANDALONE))
        try {
            vm.setCustomDeviceName(" Sound desk ")
            vm.setDisplayName(" Ada ")
            vm.save(onSuccess = {}, emptyHostError = "EMPTY", invalidPortError = "PORT", invalidHostError = "HOST")

            assertEquals("Sound desk", settings.customDeviceName)
            assertEquals("Ada", settings.displayName)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aBadPortDoesNotDiscardANameTypedBesideIt() = runVmTest {
        val (vm, settings) = vmWith()
        try {
            vm.setHost("10.0.0.5")
            vm.setPort("nonsense")
            vm.setCustomDeviceName("Sound desk")
            var ok = false
            vm.save(onSuccess = { ok = true }, emptyHostError = "EMPTY", invalidPortError = "PORT", invalidHostError = "HOST")

            assertFalse(ok) // the port is still rejected
            assertEquals("PORT", vm.portError.value)
            assertEquals("Sound desk", settings.customDeviceName)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun saveBlankHostSetsErrorAndDoesNotPersist() = runVmTest {
        val (vm, settings) = vmWith()
        try {
            vm.setHost("   ")
            vm.setPort("8765")
            var ok = false
            vm.save(onSuccess = { ok = true }, emptyHostError = "EMPTY", invalidPortError = "PORT", invalidHostError = "HOST")

            assertFalse(ok)
            assertEquals("EMPTY", vm.hostError.value)
            assertEquals(ApiConstants.DEFAULT_HOST, settings.host) // unchanged
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun standaloneSavesWithoutAServerAddress() = runVmTest {
        // Standalone hides the server fields, so a host left blank before the
        // switch must not block saving the settings it does show — the operator
        // would have no field on screen to fix it with.
        val settings = AppSettings(InMemorySettingsStorage())
        val vm = SettingsViewModel(settings, MutableStateFlow(AppMode.STANDALONE))
        try {
            vm.setHost("   ")
            vm.setPort("nonsense")
            vm.setThemeMode(ThemeMode.DARK)
            var ok = false
            vm.save(onSuccess = { ok = true }, emptyHostError = "EMPTY", invalidPortError = "PORT", invalidHostError = "HOST")

            assertTrue(ok)
            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertNull(vm.hostError.value)
            assertNull(vm.portError.value)
            // Fields that aren't on screen aren't written either.
            assertEquals(ApiConstants.DEFAULT_HOST, settings.host)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun saveRejectsOutOfRangeAndNonNumericPort() = runVmTest {
        for (bad in listOf("0", "70000", "abc", "")) {
            val (vm, _) = vmWith()
            try {
                vm.setHost("h")
                vm.setPort(bad)
                var ok = false
                vm.save(
                    onSuccess = { ok = true },
                    emptyHostError = "EMPTY",
                    invalidPortError = "PORT",
                    invalidHostError = "HOST",
                )
                assertFalse(ok, "port '$bad' should be invalid")
                assertEquals("PORT", vm.portError.value)
            } finally {
                tearDown(vm)
            }
        }
    }

    @Test
    fun resetToDefaultsSetsDraftFields() = runVmTest {
        val (vm, _) = vmWith()
        try {
            vm.setHost("1.2.3.4")
            vm.setPort("1")
            vm.resetToDefaults()
            assertEquals(ApiConstants.DEFAULT_HOST, vm.host.value)
            assertEquals(ApiConstants.DEFAULT_PORT.toString(), vm.port.value)
            assertNull(vm.hostError.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun cancelRestoresFromStorage() = runVmTest {
        val (vm, _) = vmWith()
        try {
            vm.setHost("scratch")
            vm.cancel()
            assertEquals(ApiConstants.DEFAULT_HOST, vm.host.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun setTelemetryEnabledPersistsImmediately() = runVmTest {
        val (vm, settings) = vmWith()
        try {
            vm.setTelemetryEnabled(false)
            assertFalse(vm.telemetryEnabled.value)
            assertFalse(settings.isTelemetryEnabled)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun setThemeModeUpdatesState() = runVmTest {
        val (vm, _) = vmWith()
        try {
            vm.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, vm.themeMode.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun draftBaseUrlTracksHostAndPort() = runVmTest {
        val (vm, _) = vmWith()
        try {
            vm.setHost("1.2.3.4")
            vm.setPort("42")
            advanceUntilIdle()
            assertEquals("http://1.2.3.4:42/api", vm.draftBaseUrl.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun urlChangedIsFalseInitiallyAndTrueAfterEdit() = runVmTest {
        val (vm, _) = vmWith()
        try {
            advanceUntilIdle()
            assertFalse(vm.urlChanged.value)
            vm.setHost("9.9.9.9")
            advanceUntilIdle()
            assertTrue(vm.urlChanged.value)
        } finally {
            tearDown(vm)
        }
    }
}
