package com.church.presenter.churchpresentermobile.util

import kotlin.test.Test

/**
 * Covers the one promise [CrashReporting] makes: reporting a problem never
 * becomes a second problem.
 *
 * It fans out to two backends — Firebase Crashlytics and Sentry — and neither
 * is available on a bare JVM, in a build without `google-services.json`, or
 * before the app has finished starting. Every call site treats these as
 * fire-and-forget, so a throw from any of them would take down whatever was
 * already going wrong. Each is individually wrapped for that reason; these
 * tests run with both backends absent, which is the state that wrapping is for.
 */
class CrashReportingTest {

    @Test
    fun `initialising with no backend present is silent`() {
        CrashReporting.init()
    }

    @Test
    fun `enabling collection with no backend present is silent`() {
        // The privacy switch in Settings; re-initialises Sentry when turned on.
        CrashReporting.setEnabled(true)
    }

    @Test
    fun `disabling collection with no backend present is silent`() {
        CrashReporting.setEnabled(false)
    }

    @Test
    fun `a breadcrumb with no backend present is silent`() {
        CrashReporting.log("loading songs from 192.168.1.5")
    }

    @Test
    fun `recording a non-fatal with no backend present is silent`() {
        CrashReporting.recordException(IllegalStateException("boom"))
    }

    @Test
    fun `recording a throwable with no message is silent`() {
        // The path that matters most: this is called from the foreground service's
        // refusal branch, where the exception is whatever Android threw.
        CrashReporting.recordException(RuntimeException())
    }

    @Test
    fun `setting the user id with no backend present is silent`() {
        CrashReporting.setUserId("device-1234")
    }

    @Test
    fun `setting a custom key with no backend present is silent`() {
        CrashReporting.setCustomKey("server", "192.168.1.5:8765")
    }

    @Test
    fun `an empty custom key value is accepted`() {
        CrashReporting.setCustomKey("server", "")
    }
}
