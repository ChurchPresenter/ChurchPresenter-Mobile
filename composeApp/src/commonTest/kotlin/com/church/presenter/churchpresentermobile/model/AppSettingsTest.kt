package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [AppSettings] logic via the injected in-memory storage seam: first-run
 * migration, host sanitization, URL builders, and stable deviceId.
 */
class AppSettingsTest {

    private fun settings(storage: InMemorySettingsStorage = InMemorySettingsStorage()) = AppSettings(storage)

    @Test
    fun freshInstallMigratesToDefaultHostAndPort() {
        val s = settings()
        assertEquals(ApiConstants.DEFAULT_HOST, s.host)
        assertEquals(ApiConstants.DEFAULT_PORT, s.port)
    }

    @Test
    fun hostSetterStripsColonsToDots() {
        val s = settings()
        s.host = "192.168.1.50:8765"
        assertEquals("192.168.1.50.8765", s.host)
    }

    @Test
    fun hostSetterTrimsWhitespace() {
        val s = settings()
        s.host = "  10.0.0.5  "
        assertEquals("10.0.0.5", s.host)
    }

    @Test
    fun apiAndWsUrlsAreBuiltFromHostAndPort() {
        val s = settings()
        s.host = "10.0.0.9"
        s.port = 9000
        assertEquals("http://10.0.0.9:9000/api", s.apiBaseUrl)
        assertEquals("ws://10.0.0.9:9000/ws", s.wsBaseUrl)
    }

    @Test
    fun apiKeyDefaultsEmptyAndPersists() {
        val s = settings()
        assertEquals("", s.apiKey)
        s.apiKey = "secret"
        assertEquals("secret", s.apiKey)
    }

    @Test
    fun themeModeDefaultsToSystemAndRejectsUnknownStored() {
        assertEquals(ThemeMode.SYSTEM, settings().themeMode)
        val storage = InMemorySettingsStorage(initialStrings = mapOf("theme_mode" to "NONSENSE"))
        // stored version is 0 → migration runs but theme is untouched; unknown value → SYSTEM
        assertEquals(ThemeMode.SYSTEM, AppSettings(storage).themeMode)
    }

    @Test
    fun themeModeRoundTrips() {
        val s = settings()
        s.themeMode = ThemeMode.DARK
        assertEquals(ThemeMode.DARK, s.themeMode)
    }

    @Test
    fun deviceIdIsGeneratedOnceAndStable() {
        val storage = InMemorySettingsStorage()
        val s = AppSettings(storage)
        val first = s.deviceId
        assertTrue(first.isNotBlank())
        // Same instance and a fresh instance over the same storage return the same id.
        assertEquals(first, s.deviceId)
        assertEquals(first, AppSettings(storage).deviceId)
    }

    @Test
    fun telemetryDefaultsEnabled() {
        assertTrue(settings().isTelemetryEnabled)
        val s = settings()
        s.isTelemetryEnabled = false
        assertTrue(!s.isTelemetryEnabled)
    }
}
