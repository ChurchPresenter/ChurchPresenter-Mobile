package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    // ── App mode ─────────────────────────────────────────────────────────

    @Test
    fun appModeDefaultsToRemote() {
        assertEquals(AppMode.REMOTE, settings().appMode)
    }

    @Test
    fun modeIsNotChosenOnAFreshInstall() {
        assertFalse(settings().isModeChosen)
        val s = settings()
        s.isModeChosen = true
        assertTrue(s.isModeChosen)
    }

    /**
     * Existing installs carry a v5 blob with no app_mode key. They must keep
     * behaving exactly as before, which means reading back as REMOTE.
     */
    @Test
    fun existingSettingsWithoutAModeKeyReadAsRemote() {
        val storage = InMemorySettingsStorage(
            initialStrings = mapOf("server_host" to "10.0.0.5", "api_key" to "secret"),
            initialInts = mapOf("settings_version" to 5, "server_port" to 9000),
        )
        val s = AppSettings(storage)
        assertEquals(AppMode.REMOTE, s.appMode)
        assertFalse(s.isModeChosen)
        // Adding the mode keys must not have disturbed the existing connection.
        assertEquals("10.0.0.5", s.host)
        assertEquals(9000, s.port)
        assertEquals("secret", s.apiKey)
    }

    /**
     * Standalone needs an output sink, so it is coerced away on platforms that
     * have none (the js/wasmJs web build). On Android and iOS it round-trips.
     */
    @Test
    fun standaloneIsCoercedOnPlatformsThatCannotPresent() {
        val storage = InMemorySettingsStorage()
        val s = AppSettings(storage)
        s.appMode = AppMode.STANDALONE

        val expected = if (supportsStandalone) AppMode.STANDALONE else AppMode.REMOTE
        assertEquals(expected, s.appMode)
        // And the stored value agrees with what the app is actually doing.
        assertEquals(expected, AppSettings(storage).appMode)
    }

    /**
     * A settings blob written on a phone and restored onto the web build must
     * not leave it in a mode it has no sink for.
     */
    @Test
    fun aStoredStandaloneValueIsCoercedOnRead() {
        val s = AppSettings(InMemorySettingsStorage(initialStrings = mapOf("app_mode" to "STANDALONE")))
        val expected = if (supportsStandalone) AppMode.STANDALONE else AppMode.REMOTE
        assertEquals(expected, s.appMode)
    }

    @Test
    fun anUnrecognisedStoredModeFallsBackToRemote() {
        val s = AppSettings(InMemorySettingsStorage(initialStrings = mapOf("app_mode" to "TELEPATHY")))
        assertEquals(AppMode.REMOTE, s.appMode)
    }

    @Test
    fun standalonePortDefaultsAndPersists() {
        val storage = InMemorySettingsStorage()
        val s = AppSettings(storage)
        assertEquals(ApiConstants.STANDALONE_HTTP_PORT_DEFAULT, s.standalonePort)
        s.standalonePort = 8771
        assertEquals(8771, AppSettings(storage).standalonePort)
    }

    @Test
    fun standalonePortDoesNotCollideWithTheDesktopPort() {
        assertTrue(ApiConstants.STANDALONE_HTTP_PORT_DEFAULT != ApiConstants.DEFAULT_PORT)
        assertTrue(ApiConstants.STANDALONE_HTTP_PORT_DEFAULT in ApiConstants.STANDALONE_PORT_CANDIDATES)
    }

    // ── Persisted blobs and counters ─────────────────────────────────────

    @Test
    fun `json blobs default to an empty document of the right shape`() {
        val settings = AppSettings(InMemorySettingsStorage())

        // Object for a single record, array for a list — so the first decode after
        // a fresh install parses instead of throwing.
        assertEquals("{}", settings.librarySyncStateJson)
        assertEquals("[]", settings.savedAnnouncementsJson)
        assertEquals("[]", settings.savedBookmarksJson)
    }

    @Test
    fun `json blobs round-trip through storage`() {
        val settings = AppSettings(InMemorySettingsStorage())

        settings.librarySyncStateJson = """{"songCount":240}"""
        settings.savedAnnouncementsJson = """[{"id":"a1"}]"""
        settings.savedBookmarksJson = """[{"ref":"John:3:16"}]"""

        assertEquals("""{"songCount":240}""", settings.librarySyncStateJson)
        assertEquals("""[{"id":"a1"}]""", settings.savedAnnouncementsJson)
        assertEquals("""[{"ref":"John:3:16"}]""", settings.savedBookmarksJson)
    }

    @Test
    fun `blobs are kept apart from each other`() {
        // Same storage, different keys — writing one must not disturb another.
        val settings = AppSettings(InMemorySettingsStorage())

        settings.savedAnnouncementsJson = """[{"id":"a1"}]"""

        assertEquals("[]", settings.savedBookmarksJson)
        assertEquals("{}", settings.librarySyncStateJson)
    }

    @Test
    fun `the push token starts empty and round-trips`() {
        val settings = AppSettings(InMemorySettingsStorage())

        assertEquals("", settings.fcmToken)

        settings.fcmToken = "tok-123"
        assertEquals("tok-123", settings.fcmToken)
    }

    @Test
    fun `the launch counter starts at zero and counts up`() {
        // Drives the review prompt on the 3rd, 10th and every 20th launch.
        val settings = AppSettings(InMemorySettingsStorage())

        assertEquals(0, settings.appOpenCount)

        settings.appOpenCount = settings.appOpenCount + 1
        settings.appOpenCount = settings.appOpenCount + 1
        assertEquals(2, settings.appOpenCount)
    }

    @Test
    fun `setup is incomplete on a fresh install, so the settings screen opens first`() {
        val settings = AppSettings(InMemorySettingsStorage())

        assertFalse(settings.isSetupComplete)
        assertFalse(settings.isConnectSetupDone)
    }

    @Test
    fun `the setup flags survive as booleans through an int-backed store`() {
        // Stored as 0/1, so the round-trip is worth pinning in both directions.
        val settings = AppSettings(InMemorySettingsStorage())

        settings.isSetupComplete = true
        settings.isConnectSetupDone = true
        assertTrue(settings.isSetupComplete)
        assertTrue(settings.isConnectSetupDone)

        settings.isSetupComplete = false
        assertFalse(settings.isSetupComplete)
        assertTrue(settings.isConnectSetupDone, "the two flags are independent")
    }

    @Test
    fun `settings read back from a second instance over the same storage`() {
        // The screen builds its own AppSettings; what one writes another must see.
        val storage = InMemorySettingsStorage()
        AppSettings(storage).apply {
            isSetupComplete = true
            appOpenCount = 7
            fcmToken = "tok"
        }

        val reopened = AppSettings(storage)

        assertTrue(reopened.isSetupComplete)
        assertEquals(7, reopened.appOpenCount)
        assertEquals("tok", reopened.fcmToken)
    }
}
