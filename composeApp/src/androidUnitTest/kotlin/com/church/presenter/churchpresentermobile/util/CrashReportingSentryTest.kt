package com.church.presenter.churchpresentermobile.util

import com.church.presenter.churchpresentermobile.testutil.RecordingContext
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.sentry.Hint
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * How Sentry is configured, and what it is told to throw away.
 *
 * The filter is the part that matters. An unreachable companion server is this
 * client's normal state — the desktop is off six days a week — and the SDK
 * captures more than our own call sites do: an exception escaping an OkHttp
 * dispatcher thread reaches the uncaught-exception integration without passing
 * through any of our checks. That is how "No route to host" and connect
 * timeouts to a LAN address kept arriving from builds whose call-site checks
 * already excluded them.
 *
 * `SentryAndroid.init` is intercepted rather than run, so the real
 * configuration block executes against a real options object and nothing is
 * installed into this JVM.
 */
class CrashReportingSentryTest {

    @AfterTest
    fun unmock() = unmockkAll()

    /** The options the app configures Sentry with, without starting it. */
    private fun configuredOptions(): SentryAndroidOptions {
        val configuration = slot<Sentry.OptionsConfiguration<SentryAndroidOptions>>()
        mockkStatic(SentryAndroid::class)
        every { SentryAndroid.init(any(), capture(configuration)) } returns Unit

        CrashReporting.initSentry(RecordingContext())

        return SentryAndroidOptions().also { configuration.captured.configure(it) }
    }

    /** What the filter does with an event carrying [error]. */
    private fun filtered(error: Throwable?): SentryEvent? {
        val options = configuredOptions()
        val event = SentryEvent().apply { throwable = error }
        return assertNotNull(options.beforeSend).execute(event, Hint())
    }

    // ── Configuration ────────────────────────────────────────────────────

    @Test
    fun `a destination is configured`() {
        // Without a DSN the SDK silently disables itself, and nothing would ever
        // arrive — a failure with no symptom at all.
        assertTrue(configuredOptions().dsn?.isNotBlank() == true)
    }

    @Test
    fun `the release is the app version, so a report names the build it came from`() {
        assertEquals(appVersion, configuredOptions().release)
    }

    @Test
    fun `developer and store builds are reported as separate environments`() {
        assertTrue(configuredOptions().environment in setOf("development", "production"))
    }

    @Test
    fun `stack traces are attached but thread dumps are not`() {
        // The stack is the whole value of a report; every thread's stack is
        // mostly noise and inflates the payload.
        val options = configuredOptions()

        assertTrue(options.isAttachStacktrace)
        assertFalse(options.isAttachThreads)
    }

    @Test
    fun `a store build spends no quota on performance tracing`() {
        // Tracing draws on a separate quota from error events, and the
        // auto-instrumented activity/app-start transactions are not used.
        val rate = configuredOptions().tracesSampleRate

        assertEquals(if (isDebugBuild) 1.0 else 0.0, rate)
    }

    // ── What is thrown away ──────────────────────────────────────────────

    @Test
    fun `a connect timeout to the desktop is dropped`() {
        assertNull(filtered(SocketTimeoutException("Connect timeout has expired")))
    }

    @Test
    fun `a refused connection to the desktop is dropped`() {
        // What a desktop with the companion server switched off answers with.
        assertNull(filtered(ConnectException("Connection refused")))
    }

    @Test
    fun `an unreachable host is dropped`() {
        assertNull(filtered(java.net.UnknownHostException("churchpresenter.local")))
    }

    @Test
    fun `a connectivity failure wrapped in something else is still dropped`() {
        // The shape the SDK actually captures: an engine exception whose cause is
        // the real socket failure. Checking only the outer one misses it.
        val wrapped = IllegalStateException("request failed", ConnectException("No route to host"))

        assertNull(filtered(wrapped))
    }

    // ── What is kept ─────────────────────────────────────────────────────

    @Test
    fun `a real bug is reported`() {
        val event = filtered(IllegalStateException("deck index out of range"))

        assertNotNull(event)
    }

    @Test
    fun `an event with no exception at all is reported`() {
        // Messages and breadcrumbed events carry no throwable; dropping those
        // would silently discard everything logged rather than thrown.
        assertNotNull(filtered(null))
    }

    @Test
    fun `an exception that merely mentions the server is still reported`() {
        // The filter matches connectivity markers, not the word "server" — a bug
        // whose message happens to name the desktop is still a bug.
        assertNotNull(filtered(IllegalArgumentException("server returned an unknown slide kind")))
    }
}
