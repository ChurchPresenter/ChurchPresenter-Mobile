package com.church.presenter.churchpresentermobile.util

import com.church.presenter.churchpresentermobile.model.initSettingsContext
import com.church.presenter.churchpresentermobile.testutil.RecordingContext
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * That a report reaches both backends, and that neither can stop the other.
 *
 * Crash reporting fans out to Firebase Crashlytics and Sentry — Crashlytics
 * because the existing dashboards read from it, Sentry because the desktop app
 * already uses it and the two are meant to converge. Every call site is
 * fire-and-forget with no result to check, so a backend that silently stopped
 * receiving would look exactly like a period with no crashes.
 *
 * Each call is wrapped separately in the production code; these tests are what
 * says why. Both backends are mocked at their static entry points, so nothing
 * is initialised in this JVM and no report leaves the machine.
 */
class CrashReportingBackendsTest {

    private val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    @BeforeTest
    fun installBackends() {
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics
        mockkStatic(Sentry::class)
        mockkStatic(SentryAndroid::class)
        every { SentryAndroid.init(any(), any<Sentry.OptionsConfiguration<SentryAndroidOptions>>()) } returns Unit
    }

    @AfterTest
    fun unmock() = unmockkAll()

    // ── Both backends are told ───────────────────────────────────────────

    @Test
    fun `a breadcrumb reaches both backends`() {
        // Breadcrumbs are what make a crash report readable — the last few
        // things the operator did before it. Losing one backend's trail halves
        // the report on that dashboard.
        CrashReporting.log("loading songs from 192.168.1.5")

        verify { crashlytics.log("loading songs from 192.168.1.5") }
        verify { Sentry.addBreadcrumb("loading songs from 192.168.1.5") }
    }

    @Test
    fun `a non-fatal reaches both backends`() {
        val error = IllegalStateException("deck index out of range")

        CrashReporting.recordException(error)

        verify { crashlytics.recordException(error) }
        verify { Sentry.captureException(error) }
    }

    @Test
    fun `the device identifier reaches both backends`() {
        // Without it a report cannot be tied to the church that filed it, which
        // is the first thing asked when one arrives.
        CrashReporting.setUserId("device-1234")

        verify { crashlytics.setUserId("device-1234") }
        verify { Sentry.setUser(any()) }
    }

    @Test
    fun `a custom key reaches both backends`() {
        CrashReporting.setCustomKey("ws_server_url", "192.168.1.5:8765")

        verify { crashlytics.setCustomKey("ws_server_url", "192.168.1.5:8765") }
        verify { Sentry.setTag("ws_server_url", "192.168.1.5:8765") }
    }

    // ── The privacy switch ───────────────────────────────────────────────

    @Test
    fun `initialising turns Crashlytics collection on`() {
        CrashReporting.init()

        verify { crashlytics.setCrashlyticsCollectionEnabled(true) }
    }

    @Test
    fun `turning reporting off stops Crashlytics collecting`() {
        CrashReporting.setEnabled(false)

        verify { crashlytics.setCrashlyticsCollectionEnabled(false) }
    }

    @Test
    fun `turning reporting off shuts Sentry down`() {
        // Sentry has no runtime collection flag, so the only way to honour the
        // setting is to close the client.
        CrashReporting.setEnabled(false)

        verify { Sentry.close() }
    }

    @Test
    fun `turning reporting back on restarts Sentry`() {
        // Closed is closed: without re-initialising, a user who toggled the
        // setting off and on again would silently never report anything more.
        initSettingsContext(RecordingContext())

        CrashReporting.setEnabled(true)

        verify { crashlytics.setCrashlyticsCollectionEnabled(true) }
        verify { SentryAndroid.init(any(), any<Sentry.OptionsConfiguration<SentryAndroidOptions>>()) }
    }

    // ── Neither backend can take the other down ──────────────────────────

    @Test
    fun `a breadcrumb still reaches Sentry when Crashlytics throws`() {
        // This is why each call is wrapped on its own rather than in one block.
        every { crashlytics.log(any()) } throws IllegalStateException("Firebase not initialised")

        CrashReporting.log("still worth recording")

        verify { Sentry.addBreadcrumb("still worth recording") }
    }

    @Test
    fun `a non-fatal still reaches Sentry when Crashlytics throws`() {
        val error = IllegalStateException("deck index out of range")
        every { crashlytics.recordException(any()) } throws IllegalStateException("Firebase not initialised")

        CrashReporting.recordException(error)

        verify { Sentry.captureException(error) }
    }

    @Test
    fun `a non-fatal still reaches Crashlytics when Sentry throws`() {
        val error = IllegalStateException("deck index out of range")
        every { Sentry.captureException(any<Throwable>()) } throws IllegalStateException("Sentry not started")

        CrashReporting.recordException(error)

        verify { crashlytics.recordException(error) }
    }

    @Test
    fun `a backend that throws never reaches the caller`() {
        // Reporting a problem must not become a second problem: recordException
        // is called from catch blocks that are already handling something.
        every { crashlytics.recordException(any()) } throws IllegalStateException("boom")
        every { Sentry.captureException(any<Throwable>()) } throws IllegalStateException("boom")

        CrashReporting.recordException(RuntimeException("original"))
    }

    @Test
    fun `a failing identity call does not stop the other backend`() {
        every { crashlytics.setUserId(any()) } throws IllegalStateException("Firebase not initialised")

        CrashReporting.setUserId("device-1234")

        verify { Sentry.setUser(any()) }
    }

    @Test
    fun `a failing custom key does not stop the other backend`() {
        every { crashlytics.setCustomKey(any(), any<String>()) } throws IllegalStateException("boom")

        CrashReporting.setCustomKey("ws_server_url", "192.168.1.5:8765")

        verify { Sentry.setTag("ws_server_url", "192.168.1.5:8765") }
    }

    @Test
    fun `a breadcrumb still reaches Crashlytics when Sentry throws`() {
        every { Sentry.addBreadcrumb(any<String>()) } throws IllegalStateException("Sentry not started")

        CrashReporting.log("still worth recording")

        verify { crashlytics.log("still worth recording") }
    }

    @Test
    fun `an identity call still reaches Crashlytics when Sentry throws`() {
        every { Sentry.setUser(any()) } throws IllegalStateException("Sentry not started")

        CrashReporting.setUserId("device-1234")

        verify { crashlytics.setUserId("device-1234") }
    }

    @Test
    fun `a custom key still reaches Crashlytics when Sentry throws`() {
        every { Sentry.setTag(any(), any()) } throws IllegalStateException("Sentry not started")

        CrashReporting.setCustomKey("ws_server_url", "192.168.1.5:8765")

        verify { crashlytics.setCustomKey("ws_server_url", "192.168.1.5:8765") }
    }

    @Test
    fun `turning reporting off still stops Crashlytics when Sentry will not close`() {
        // The privacy setting has to take effect on whichever backend can hear
        // it; a half-honoured opt-out is the worst possible outcome.
        every { Sentry.close() } throws IllegalStateException("Sentry not started")

        CrashReporting.setEnabled(false)

        verify { crashlytics.setCrashlyticsCollectionEnabled(false) }
    }

    @Test
    fun `turning reporting on survives a Sentry that will not start`() {
        initSettingsContext(RecordingContext())
        every {
            SentryAndroid.init(any(), any<Sentry.OptionsConfiguration<SentryAndroidOptions>>())
        } throws IllegalStateException("no context")

        CrashReporting.setEnabled(true)

        verify { crashlytics.setCrashlyticsCollectionEnabled(true) }
    }

    @Test
    fun `initialising survives a Crashlytics that is not there`() {
        every { FirebaseCrashlytics.getInstance() } throws IllegalStateException("Firebase not initialised")

        CrashReporting.init()
        CrashReporting.log("still fine")
        CrashReporting.recordException(RuntimeException("still fine"))
    }
}
