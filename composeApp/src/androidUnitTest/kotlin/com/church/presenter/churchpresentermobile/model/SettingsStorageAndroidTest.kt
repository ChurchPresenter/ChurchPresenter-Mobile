package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.testutil.RecordingContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertSame

/**
 * Covers the Android side of settings storage: which context is kept, which
 * preference file is written, and what host the app falls back to.
 */
class SettingsStorageAndroidTest {

    private fun storageOver(context: RecordingContext): AndroidSettingsStorage {
        initSettingsContext(context)
        return AndroidSettingsStorage()
    }

    @Test
    fun `the application context is kept, not the one that was passed`() {
        // An Activity handed in here and held in a process-wide field would be
        // leaked for the life of the app, and survive every rotation.
        val application = RecordingContext()
        val activity = RecordingContext(appContext = application)

        initSettingsContext(activity)

        assertSame(application, getAppContext())
    }

    @Test
    fun `a string survives a round trip`() {
        val context = RecordingContext()
        val storage = storageOver(context)

        storage.putString("server_host", "192.168.1.5")

        assertEquals("192.168.1.5", storage.getString("server_host", "fallback"))
    }

    @Test
    fun `an unwritten string comes back as the default`() {
        val storage = storageOver(RecordingContext())

        assertEquals("fallback", storage.getString("never_written", "fallback"))
    }

    @Test
    fun `an int survives a round trip`() {
        val storage = storageOver(RecordingContext())

        storage.putInt("server_port", 8765)

        assertEquals(8765, storage.getInt("server_port", 0))
    }

    @Test
    fun `an unwritten int comes back as the default`() {
        val storage = storageOver(RecordingContext())

        assertEquals(8765, storage.getInt("never_written", 8765))
    }

    @Test
    fun `writing twice keeps the second value`() {
        val storage = storageOver(RecordingContext())

        storage.putString("server_host", "first")
        storage.putString("server_host", "second")

        assertEquals("second", storage.getString("server_host", ""))
    }

    @Test
    fun `everything lands in the one shared preference file`() {
        // The same file FirebasePushService writes the FCM token into; a second
        // file here would split the app's settings across two stores.
        val context = RecordingContext()
        val storage = storageOver(context)

        storage.putString("a", "1")
        storage.putInt("b", 2)

        assertEquals(setOf("church_presenter_prefs"), context.preferenceFiles.keys)
    }

    @Test
    fun `the settings factory produces the Android storage`() {
        initSettingsContext(RecordingContext())

        val settings = AppSettings(createSettingsStorage())
        settings.fcmToken = "token-1"

        assertEquals("token-1", settings.fcmToken)
    }

    @Test
    fun `off a device the default host is the LAN one`() {
        // android.os.Build's fields are null on a plain JVM, so nothing matches
        // the emulator test and the answer is the one a real phone would give.
        assertEquals(ApiConstants.DEFAULT_HOST, resolveDefaultHost())
    }

    @Test
    fun `resolving the host twice gives the same answer`() {
        assertEquals(resolveDefaultHost(), resolveDefaultHost())
    }

    // ── Which host a fresh install points at ─────────────────────────────
    //
    // An emulator cannot reach the developer's machine on its LAN address; it
    // has to use 10.0.2.2. A real phone is the other way round. Guess wrong in
    // either direction and every tab times out against an address that does not
    // exist, with nothing on screen to say why — so the test is over the build
    // identifiers rather than over `Build`, whose fields a unit test cannot set.

    /** Build identifiers as a real phone reports them. */
    private fun phone(
        fingerprint: String? = "samsung/dm3q/dm3q:14/UP1A/S918BXXU:user/release-keys",
        model: String? = "SM-S918B",
        manufacturer: String? = "samsung",
        brand: String? = "samsung",
        device: String? = "dm3q",
        product: String? = "dm3qxxx",
        hardware: String? = "qcom",
    ) = looksLikeEmulator(fingerprint, model, manufacturer, brand, device, product, hardware)

    @Test
    fun `a real phone is not taken for an emulator`() {
        assertFalse(phone())
    }

    @Test
    fun `a generic fingerprint is an emulator`() {
        assertTrue(phone(fingerprint = "generic/sdk_gphone64/emu64x:14/UE1A:userdebug/test-keys"))
    }

    @Test
    fun `an unknown fingerprint is an emulator`() {
        assertTrue(phone(fingerprint = "unknown"))
    }

    @Test
    fun `the SDK models are emulators`() {
        assertTrue(phone(model = "google_sdk"))
        assertTrue(phone(model = "Android SDK built for x86"))
        assertTrue(phone(model = "Android Emulator"))
    }

    @Test
    fun `a Genymotion image is an emulator`() {
        assertTrue(phone(manufacturer = "Genymotion"))
    }

    @Test
    fun `a generic brand or device is an emulator`() {
        assertTrue(phone(brand = "generic_x86"))
        assertTrue(phone(device = "generic_x86_arm"))
    }

    @Test
    fun `the emulator product names are recognised`() {
        assertTrue(phone(product = "sdk_gphone64_arm64"))
        assertTrue(phone(product = "vbox86p"))
    }

    @Test
    fun `the two emulator hardware names are recognised`() {
        // goldfish is the classic AVD kernel, ranchu the current one.
        assertTrue(phone(hardware = "goldfish"))
        assertTrue(phone(hardware = "ranchu"))
    }

    @Test
    fun `hardware is matched exactly, not as a substring`() {
        // "goldfish_opengl" appears on some real devices' driver strings; a
        // substring match there would send a phone to 10.0.2.2.
        assertFalse(phone(hardware = "goldfish_opengl"))
        assertFalse(phone(hardware = "ranchu_extra"))
    }

    @Test
    fun `a build that reports nothing about itself is not an emulator`() {
        // Every identifier is null on a plain JVM. Absence of evidence is not
        // evidence, and guessing "emulator" here would point the app at an
        // address no real device can route to.
        assertFalse(looksLikeEmulator(null, null, null, null, null, null, null))
    }

    @Test
    fun `a device whose model merely mentions a brand is not an emulator`() {
        // The model check is a substring match, so it is the one most likely to
        // catch a real phone by accident.
        assertFalse(phone(model = "Pixel 8 Pro"))
        assertFalse(phone(model = "SDK"))
    }
}
