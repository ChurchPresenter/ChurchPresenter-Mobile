package com.church.presenter.churchpresentermobile.util

import kotlin.test.Test

/**
 * Covers the one promise [Analytics] makes: it never crashes the caller.
 *
 * Firebase is absent on a bare JVM and, on a device, is absent until
 * `FirebaseApp` has initialised — which is later than the first screen. Every
 * method is called from ordinary UI code with no result to check, so a throw
 * here would surface as an app that dies on launch for anyone whose build has
 * no `google-services.json` (which this repo's own default build does not; see
 * the placeholder written by `build.gradle.kts`).
 *
 * These run in exactly that state: no Firebase, so the internal handle is null
 * and every guard is exercised.
 */
class AnalyticsTest {

    @Test
    fun `initialising without Firebase is silent`() {
        Analytics.init()
    }

    @Test
    fun `turning collection on without Firebase is silent`() {
        Analytics.setEnabled(true)
    }

    @Test
    fun `turning collection off without Firebase is silent`() {
        // The privacy switch in Settings calls this; it must work in every build.
        Analytics.setEnabled(false)
    }

    @Test
    fun `an event with parameters is logged without Firebase`() {
        Analytics.logEvent("song_projected", mapOf("book" to "Hymns", "number" to "42"))
    }

    @Test
    fun `an event with no parameters is logged without Firebase`() {
        Analytics.logEvent("app_open", emptyMap())
    }

    @Test
    fun `an over-long parameter value is accepted`() {
        // Firebase rejects parameter values past 100 characters, so they are cut
        // before being handed over rather than being dropped by the SDK.
        Analytics.logEvent("search", mapOf("query" to "a".repeat(500)))
    }

    @Test
    fun `a screen view is logged without Firebase`() {
        Analytics.logScreenView("SongsList")
    }

    @Test
    fun `a blank screen name does not throw`() {
        Analytics.logScreenView("")
    }
}
