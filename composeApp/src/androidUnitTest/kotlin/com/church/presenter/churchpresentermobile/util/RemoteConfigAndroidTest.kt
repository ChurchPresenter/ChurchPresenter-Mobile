package com.church.presenter.churchpresentermobile.util

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Remote Config bridge, against a mocked Firebase.
 *
 * Everything here decides how the app behaves when the server says something
 * unexpected or says nothing at all — the maintenance flag, the feature
 * switches, the minimum version. A fetch failure is the normal state on a hall
 * with no uplink, and it has to leave the app running on its cached or default
 * values rather than reporting an error the operator cannot act on.
 *
 * `FirebaseRemoteConfig` is a final class with a static factory, so it is
 * mocked. The object holds its handle in a `lazy`, which is why the static mock
 * is installed once and the same instance is re-stubbed per test.
 */
class RemoteConfigAndroidTest {

    @BeforeTest
    fun installFirebase() {
        // One shared instance across the class (see below), so the calls the
        // previous test recorded have to go before this one counts any.
        clearMocks(firebase)
        mockkStatic(FirebaseRemoteConfig::class)
        every { FirebaseRemoteConfig.getInstance() } returns firebase
        every { firebase.setConfigSettingsAsync(any()) } returns mockk(relaxed = true)
        every { firebase.setDefaultsAsync(any<Map<String, Any>>()) } returns mockk(relaxed = true)
        every { firebase.getString(any()) } returns ""
        every { firebase.getBoolean(any()) } returns false
        every { firebase.getLong(any()) } returns 0L
    }

    /** A fetch that succeeds, reporting whether new values were activated. */
    private fun fetchSucceeds(activated: Boolean) {
        val task = mockk<Task<Boolean>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<Boolean>>()) } answers {
            firstArg<OnSuccessListener<Boolean>>().onSuccess(activated)
            task
        }
        every { task.addOnFailureListener(any()) } returns task
        every { firebase.fetchAndActivate() } returns task
    }

    /** A fetch that fails the way a throttled or offline one does. */
    private fun fetchFails(error: Exception) {
        val task = mockk<Task<Boolean>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<Boolean>>()) } returns task
        every { task.addOnFailureListener(any()) } answers {
            firstArg<OnFailureListener>().onFailure(error)
            task
        }
        every { firebase.fetchAndActivate() } returns task
    }

    // ── Initialisation ───────────────────────────────────────────────────

    @Test
    fun `the fetch interval given is the one Firebase is configured with`() {
        // Short in development so a flag can be flipped and seen; long in
        // production so the app is not asking on every launch.
        val settings = slot<FirebaseRemoteConfigSettings>()
        every { firebase.setConfigSettingsAsync(capture(settings)) } returns mockk(relaxed = true)

        RemoteConfig.init(emptyMap(), fetchIntervalSeconds = 3_600L)

        assertEquals(3_600L, settings.captured.minimumFetchIntervalInSeconds)
    }

    @Test
    fun `defaults are registered so the first launch has values before any fetch`() {
        val defaults = slot<Map<String, Any>>()
        every { firebase.setDefaultsAsync(capture(defaults)) } returns mockk(relaxed = true)

        RemoteConfig.init(mapOf(RemoteConfigKeys.MAINTENANCE_MODE to false), fetchIntervalSeconds = 60L)

        assertEquals(false, defaults.captured[RemoteConfigKeys.MAINTENANCE_MODE])
    }

    @Test
    fun `initialising with no defaults does not register an empty set`() {
        RemoteConfig.init(emptyMap(), fetchIntervalSeconds = 60L)

        verify(exactly = 0) { firebase.setDefaultsAsync(any<Map<String, Any>>()) }
    }

    // ── Fetching ─────────────────────────────────────────────────────────

    @Test
    fun `a fetch that activates new values says so`() {
        fetchSucceeds(activated = true)
        var activated: Boolean? = null

        RemoteConfig.fetchAndActivate { activated = it }

        assertEquals(true, activated)
    }

    @Test
    fun `a fetch that finds nothing new says so`() {
        // Normal on a second launch inside the fetch interval.
        fetchSucceeds(activated = false)
        var activated: Boolean? = null

        RemoteConfig.fetchAndActivate { activated = it }

        assertEquals(false, activated)
    }

    @Test
    fun `a failed fetch completes rather than leaving the caller waiting`() {
        // The splash screen waits on this callback; never calling it would leave
        // the app on the splash screen for ever when the hall has no uplink.
        fetchFails(RuntimeException("Connect timeout has expired"))
        var called = false

        RemoteConfig.fetchAndActivate { called = true }

        assertTrue(called)
    }

    @Test
    fun `a failed fetch reports no activation so cached values keep being used`() {
        fetchFails(RuntimeException("throttled"))
        var activated: Boolean? = null

        RemoteConfig.fetchAndActivate { activated = it }

        assertEquals(false, activated)
    }

    @Test
    fun `a failure with no message is still survivable`() {
        fetchFails(RuntimeException())
        var called = false

        RemoteConfig.fetchAndActivate { called = true }

        assertTrue(called)
    }

    // ── Reading values ───────────────────────────────────────────────────

    @Test
    fun `a string set on the server is what is read`() {
        every { firebase.getString(RemoteConfigKeys.ANNOUNCEMENT_BANNER) } returns "Server maintenance Sunday"

        assertEquals(
            "Server maintenance Sunday",
            RemoteConfig.getString(RemoteConfigKeys.ANNOUNCEMENT_BANNER, default = "none"),
        )
    }

    @Test
    fun `an unset string falls back to the default rather than showing blank`() {
        // Firebase answers "" for a key it has never been given, which would
        // otherwise render as an empty banner rather than no banner.
        every { firebase.getString(any()) } returns ""

        assertEquals("none", RemoteConfig.getString(RemoteConfigKeys.ANNOUNCEMENT_BANNER, default = "none"))
    }

    @Test
    fun `a flag set on the server is what is read`() {
        every { firebase.getBoolean(RemoteConfigKeys.MAINTENANCE_MODE) } returns true

        assertTrue(RemoteConfig.getBoolean(RemoteConfigKeys.MAINTENANCE_MODE, default = false))
    }

    @Test
    fun `an unset flag comes back false, which is what Firebase's own default gives`() {
        // Note the `default` argument is not consulted on Android: the value
        // registered through init() is already Firebase's fallback, so asking
        // twice would let the two disagree.
        every { firebase.getBoolean(any()) } returns false

        assertFalse(RemoteConfig.getBoolean(RemoteConfigKeys.MAINTENANCE_MODE, default = true))
    }

    @Test
    fun `a number set on the server is what is read`() {
        every { firebase.getLong(RemoteConfigKeys.MIN_APP_VERSION) } returns 42L

        assertEquals(42L, RemoteConfig.getLong(RemoteConfigKeys.MIN_APP_VERSION, default = 0L))
    }

    private companion object {
        /**
         * One instance for the whole class: [RemoteConfig] resolves its handle
         * through a `lazy`, so the first test to touch it fixes the object every
         * later one sees.
         */
        val firebase: FirebaseRemoteConfig = mockk(relaxed = true)
    }

    // ── The shapes the app actually calls ────────────────────────────────
    //
    // Every parameter here has a default, and the app leans on them: startup
    // calls init() bare, and a feature switch is read as getBoolean(key). The
    // defaults are part of the contract, so they are exercised as written
    // rather than only through their fully-spelled-out forms.

    @Test
    fun `initialising with nothing given uses the production fetch interval`() {
        // An hour. Development overrides it to 0 so a flag can be flipped and
        // seen; shipping that would ask on every launch.
        val settings = slot<FirebaseRemoteConfigSettings>()
        every { firebase.setConfigSettingsAsync(capture(settings)) } returns mockk(relaxed = true)

        RemoteConfig.init()

        assertEquals(3_600L, settings.captured.minimumFetchIntervalInSeconds)
    }

    @Test
    fun `initialising with defaults alone still uses the production interval`() {
        val settings = slot<FirebaseRemoteConfigSettings>()
        every { firebase.setConfigSettingsAsync(capture(settings)) } returns mockk(relaxed = true)

        RemoteConfig.init(mapOf(RemoteConfigKeys.MAINTENANCE_MODE to false))

        assertEquals(3_600L, settings.captured.minimumFetchIntervalInSeconds)
    }

    @Test
    fun `a string read with no default falls back to empty rather than to null`() {
        // The banner is rendered straight from this; a null would need a check
        // at every call site.
        every { firebase.getString(any()) } returns ""

        assertEquals("", RemoteConfig.getString(RemoteConfigKeys.ANNOUNCEMENT_BANNER))
    }

    @Test
    fun `a string read with no default still returns what the server set`() {
        every { firebase.getString(RemoteConfigKeys.ANNOUNCEMENT_BANNER) } returns "Maintenance Sunday"

        assertEquals("Maintenance Sunday", RemoteConfig.getString(RemoteConfigKeys.ANNOUNCEMENT_BANNER))
    }

    @Test
    fun `a flag read with no default is off`() {
        // Every feature switch is read this way, so "unset" has to mean the safe
        // side — maintenance mode off, not maintenance mode on.
        every { firebase.getBoolean(any()) } returns false

        assertFalse(RemoteConfig.getBoolean(RemoteConfigKeys.MAINTENANCE_MODE))
    }

    @Test
    fun `a flag read with no default still returns what the server set`() {
        every { firebase.getBoolean(RemoteConfigKeys.FEATURE_BIBLE_ENABLED) } returns true

        assertTrue(RemoteConfig.getBoolean(RemoteConfigKeys.FEATURE_BIBLE_ENABLED))
    }

    @Test
    fun `a number read with no default is zero`() {
        // The minimum-version gate compares against this; anything but zero
        // would lock users out of a build the server never asked to block.
        every { firebase.getLong(any()) } returns 0L

        assertEquals(0L, RemoteConfig.getLong(RemoteConfigKeys.MIN_APP_VERSION))
    }

    @Test
    fun `a number read with no default still returns what the server set`() {
        every { firebase.getLong(RemoteConfigKeys.MIN_APP_VERSION) } returns 19L

        assertEquals(19L, RemoteConfig.getLong(RemoteConfigKeys.MIN_APP_VERSION))
    }
}
