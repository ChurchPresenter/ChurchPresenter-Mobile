package com.church.presenter.churchpresentermobile.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Remote Config on a target with no Firebase behind it.
 *
 * Every screen reads its flags through this, so what the stub does *is* what the
 * app does on those targets: hand back the defaults it was given at startup and
 * never pretend a fetch happened. The failure worth catching is a lookup that
 * returns something other than the caller's default when the value is missing or
 * is not the type asked for — a feature flag reading `false` by accident turns a
 * tab off for everybody.
 *
 * In `jvmTest` rather than `commonTest` on purpose: the Android and iOS actuals
 * are Firebase itself, and asserting the in-memory store's rules against those
 * would be asserting Firebase. This is the run JaCoCo measures, so the stub's
 * lines are covered where they live.
 *
 * The stub is an object with one store for the whole process, so every test here
 * uses keys of its own rather than assuming an empty one.
 */
class RemoteConfigStubTest {

    private fun key(name: String) = "stub_test_$name"

    // ── Values that were never set ───────────────────────────────────────

    @Test
    fun `a string that was never set falls back to the caller's default`() {
        assertEquals("fallback", RemoteConfig.getString(key("absent_string"), "fallback"))
    }

    @Test
    fun `a boolean that was never set falls back to the caller's default`() {
        // A feature flag defaulting the wrong way turns a tab off for everyone.
        assertTrue(RemoteConfig.getBoolean(key("absent_true"), default = true))
    }

    @Test
    fun `a boolean default of false is respected too`() {
        assertFalse(RemoteConfig.getBoolean(key("absent_false"), default = false))
    }

    @Test
    fun `a long that was never set falls back to the caller's default`() {
        assertEquals(42L, RemoteConfig.getLong(key("absent_long"), 42L))
    }

    @Test
    fun `an empty string is a perfectly good default`() {
        assertEquals("", RemoteConfig.getString(key("absent_blank"), ""))
    }

    @Test
    fun `zero is a perfectly good default`() {
        assertEquals(0L, RemoteConfig.getLong(key("absent_zero"), 0L))
    }

    // ── Values supplied as defaults at startup ───────────────────────────

    @Test
    fun `a string default supplied at startup is what comes back`() {
        RemoteConfig.init(mapOf(key("banner") to "Service at 10"))

        assertEquals("Service at 10", RemoteConfig.getString(key("banner"), "unused"))
    }

    @Test
    fun `a boolean default supplied at startup is what comes back`() {
        RemoteConfig.init(mapOf(key("maintenance") to true))

        assertTrue(RemoteConfig.getBoolean(key("maintenance"), default = false))
    }

    @Test
    fun `a long default supplied at startup is what comes back`() {
        RemoteConfig.init(mapOf(key("min_version") to 7L))

        assertEquals(7L, RemoteConfig.getLong(key("min_version"), 0L))
    }

    @Test
    fun `a supplied value wins over the value asked for at the call site`() {
        RemoteConfig.init(mapOf(key("wins") to "from startup"))

        assertEquals("from startup", RemoteConfig.getString(key("wins"), "from the call"))
    }

    @Test
    fun `several defaults are all kept`() {
        RemoteConfig.init(
            mapOf(
                key("multi_a") to "one",
                key("multi_b") to "two",
            )
        )

        assertEquals("one", RemoteConfig.getString(key("multi_a"), ""))
        assertEquals("two", RemoteConfig.getString(key("multi_b"), ""))
    }

    @Test
    fun `a second init adds to what is there rather than replacing it`() {
        // Startup calls it once, but a test host or a re-entry must not wipe
        // flags another caller already relied on.
        RemoteConfig.init(mapOf(key("kept") to "first"))

        RemoteConfig.init(mapOf(key("added") to "second"))

        assertEquals("first", RemoteConfig.getString(key("kept"), ""))
        assertEquals("second", RemoteConfig.getString(key("added"), ""))
    }

    @Test
    fun `a second init can change a value it already held`() {
        RemoteConfig.init(mapOf(key("changed") to "before"))

        RemoteConfig.init(mapOf(key("changed") to "after"))

        assertEquals("after", RemoteConfig.getString(key("changed"), ""))
    }

    @Test
    fun `an empty set of defaults is harmless`() {
        RemoteConfig.init(mapOf(key("survives") to "yes"))

        RemoteConfig.init(emptyMap())

        assertEquals("yes", RemoteConfig.getString(key("survives"), ""))
    }

    @Test
    fun `init with no arguments at all is harmless`() {
        RemoteConfig.init()

        assertEquals("default", RemoteConfig.getString(key("still_absent"), "default"))
    }

    // ── Values of the wrong type ─────────────────────────────────────────

    @Test
    fun `a string asked for as a boolean falls back`() {
        // Remote Config values arrive as strings from a console where anyone can
        // type anything; reading one as a flag must not invent a true.
        RemoteConfig.init(mapOf(key("mistyped_bool") to "true"))

        assertFalse(RemoteConfig.getBoolean(key("mistyped_bool"), default = false))
    }

    @Test
    fun `a string asked for as a long falls back`() {
        RemoteConfig.init(mapOf(key("mistyped_long") to "7"))

        assertEquals(99L, RemoteConfig.getLong(key("mistyped_long"), 99L))
    }

    @Test
    fun `a boolean asked for as a string falls back`() {
        RemoteConfig.init(mapOf(key("bool_as_string") to true))

        assertEquals("fallback", RemoteConfig.getString(key("bool_as_string"), "fallback"))
    }

    @Test
    fun `a long asked for as a string falls back`() {
        RemoteConfig.init(mapOf(key("long_as_string") to 5L))

        assertEquals("fallback", RemoteConfig.getString(key("long_as_string"), "fallback"))
    }

    @Test
    fun `an int asked for as a long falls back rather than widening`() {
        // The contract is Long; a silent widening here would make the same
        // config behave differently from the real backend.
        RemoteConfig.init(mapOf(key("int_value") to 5))

        assertEquals(99L, RemoteConfig.getLong(key("int_value"), 99L))
    }

    @Test
    fun `a long asked for as a boolean falls back`() {
        RemoteConfig.init(mapOf(key("long_as_bool") to 1L))

        assertTrue(RemoteConfig.getBoolean(key("long_as_bool"), default = true))
    }

    @Test
    fun `a wrong type does not overwrite the caller's default for other keys`() {
        RemoteConfig.init(mapOf(key("bad_type") to 1L, key("good_type") to "fine"))

        assertEquals("fallback", RemoteConfig.getString(key("bad_type"), "fallback"))
        assertEquals("fine", RemoteConfig.getString(key("good_type"), "fallback"))
    }

    // ── Fetching ─────────────────────────────────────────────────────────

    @Test
    fun `a fetch on a target with no backend reports nothing activated`() {
        // Saying "activated" over values that never moved would have callers
        // re-reading flags that cannot have changed.
        var activated: Boolean? = null

        RemoteConfig.fetchAndActivate { activated = it }

        assertEquals(false, activated)
    }

    @Test
    fun `a fetch calls back exactly once`() {
        // Callers re-read every flag in this callback.
        var calls = 0

        RemoteConfig.fetchAndActivate { calls++ }

        assertEquals(1, calls)
    }

    @Test
    fun `a fetch leaves the defaults alone`() {
        RemoteConfig.init(mapOf(key("kept_over_fetch") to "value"))

        RemoteConfig.fetchAndActivate { }

        assertEquals("value", RemoteConfig.getString(key("kept_over_fetch"), ""))
    }

    @Test
    fun `fetching twice is harmless`() {
        var calls = 0

        RemoteConfig.fetchAndActivate { calls++ }
        RemoteConfig.fetchAndActivate { calls++ }

        assertEquals(2, calls)
    }

    @Test
    fun `a fetch before init is harmless`() {
        var activated: Boolean? = null

        RemoteConfig.fetchAndActivate { activated = it }

        assertEquals(false, activated)
    }

    // ── The keys the console actually holds ──────────────────────────────

    @Test
    fun `the app's own flags read their shipped defaults after init`() {
        // This is what startup does, and what every screen then reads.
        RemoteConfig.init(
            mapOf(
                RemoteConfigKeys.FEATURE_SONGS_ENABLED to RemoteConfigDefaults.FEATURE_SONGS_ENABLED,
                RemoteConfigKeys.IS_DEMO_MODE to RemoteConfigDefaults.IS_DEMO_MODE,
            )
        )

        assertTrue(RemoteConfig.getBoolean(RemoteConfigKeys.FEATURE_SONGS_ENABLED, default = false))
        assertFalse(RemoteConfig.getBoolean(RemoteConfigKeys.IS_DEMO_MODE, default = true))
    }

    @Test
    fun `the maintenance flag defaults to the app being usable`() {
        // A wrong default here locks every user out of the app.
        RemoteConfig.init(mapOf(RemoteConfigKeys.MAINTENANCE_MODE to RemoteConfigDefaults.MAINTENANCE_MODE))

        assertFalse(RemoteConfig.getBoolean(RemoteConfigKeys.MAINTENANCE_MODE, default = true))
    }

    @Test
    fun `the announcement banner defaults to nothing to say`() {
        RemoteConfig.init(mapOf(RemoteConfigKeys.ANNOUNCEMENT_BANNER to RemoteConfigDefaults.ANNOUNCEMENT_BANNER))

        assertEquals("", RemoteConfig.getString(RemoteConfigKeys.ANNOUNCEMENT_BANNER, "something"))
    }

    @Test
    fun `the minimum version default does not lock out this build`() {
        RemoteConfig.init(mapOf(RemoteConfigKeys.MIN_APP_VERSION to RemoteConfigDefaults.MIN_APP_VERSION))

        assertEquals(1L, RemoteConfig.getLong(RemoteConfigKeys.MIN_APP_VERSION, 0L))
    }

    @Test
    fun `every feature flag ships switched on`() {
        // Shipping a flag off means a feature nobody can reach until someone
        // remembers to turn it on in the console.
        assertTrue(RemoteConfigDefaults.FEATURE_BIBLE_ENABLED)
        assertTrue(RemoteConfigDefaults.FEATURE_SONGS_ENABLED)
        assertTrue(RemoteConfigDefaults.FEATURE_PICTURES_ENABLED)
        assertTrue(RemoteConfigDefaults.FEATURE_PRESENTATION_ENABLED)
    }

    @Test
    fun `a key of an empty string still reads back its default`() {
        assertEquals("fallback", RemoteConfig.getString("", "fallback"))
    }
}
