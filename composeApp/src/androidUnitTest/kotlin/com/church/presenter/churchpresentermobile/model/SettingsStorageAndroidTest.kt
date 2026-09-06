package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.testutil.RecordingContext
import kotlin.test.Test
import kotlin.test.assertEquals
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
        // android.os.Build's fields are null on a plain JVM, so the emulator probe
        // throws; the guard around it is what keeps this from taking the app with
        // it, and the answer a real phone would give is the LAN address.
        assertEquals(ApiConstants.DEFAULT_HOST, resolveDefaultHost())
    }

    @Test
    fun `resolving the host twice gives the same answer`() {
        assertEquals(resolveDefaultHost(), resolveDefaultHost())
    }
}
