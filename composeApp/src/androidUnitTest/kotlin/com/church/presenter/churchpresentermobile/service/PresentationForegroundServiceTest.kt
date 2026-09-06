package com.church.presenter.churchpresentermobile.service

import android.app.Service
import android.content.Intent
import com.church.presenter.churchpresentermobile.testutil.RecordingContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the ways [PresentationForegroundService] is asked to run and the ways
 * that request can be denied.
 *
 * All of it matters for the same reason: this service exists to keep a phone
 * driving a screen mid-service, and the failure modes it guards against —
 * a system restart with no work to do, and Android 12+ refusing a
 * foreground-service start from the background — used to crash the operator's
 * phone rather than merely stand the feature down.
 *
 * Android-only, and no emulator: the service is constructed directly and its
 * lifecycle methods called. Everything it inherits is an `android.jar` stub
 * (`unitTests.isReturnDefaultValues`), which is exactly what makes the refusal
 * path reachable — building a notification against stub classes fails, which
 * is the shape of a real refusal.
 */
class PresentationForegroundServiceTest {

    private fun service() = PresentationForegroundService()

    @Test
    fun `nothing binds to this service`() {
        // It is started and stopped, never bound; returning a binder would invite
        // a caller to hold it open past the point the service should have gone.
        assertNull(service().onBind(Intent()))
    }

    @Test
    fun `a system restart with no intent stands down instead of starting`() {
        // The crash this replaced: Android restarts the process, delivers a null
        // intent with the app in the background, and startForeground() is refused
        // outright. There is nothing to restart into anyway.
        val result = service().onStartCommand(null, 0, START_ID)

        assertEquals(Service.START_NOT_STICKY, result)
    }

    @Test
    fun `a refused foreground start is survived rather than thrown`() {
        // Losing the keep-alive degrades the feature; crashing the phone during a
        // service is far worse.
        val result = service().onStartCommand(Intent(), 0, START_ID)

        assertEquals(Service.START_NOT_STICKY, result)
    }

    @Test
    fun `the service never asks the system to restart it`() {
        // Deliberately not sticky — a restart arrives with no intent and no server,
        // so being brought back only re-runs the branch above.
        val service = service()

        assertTrue(
            listOf(
                service.onStartCommand(null, 0, START_ID),
                service.onStartCommand(Intent(), 0, START_ID),
            ).all { it == Service.START_NOT_STICKY }
        )
    }

    @Test
    fun `stopping with no locks held is not an error`() {
        // onDestroy runs whether or not the start ever got as far as taking them.
        service().onDestroy()
    }

    @Test
    fun `starting asks the system to start the service`() {
        val context = RecordingContext()

        PresentationForegroundService.start(context, "http://192.168.1.5:8080")

        assertEquals(1, context.startedServices.size)
    }

    @Test
    fun `starting without a url still starts the service`() {
        // The URL is only the notification's subtitle; the locks are the point.
        val context = RecordingContext()

        PresentationForegroundService.start(context, null)

        assertEquals(1, context.startedServices.size)
    }

    @Test
    fun `a refused start is swallowed rather than crashing the caller`() {
        // start() is called from the Browser screen coming into view. A refusal
        // there must not take the app down with it.
        PresentationForegroundService.start(RecordingContext(failStarts = true), "http://x")
    }

    @Test
    fun `stopping asks the system to stop the service`() {
        val context = RecordingContext()

        PresentationForegroundService.stop(context)

        assertEquals(1, context.stopCount)
    }

    @Test
    fun `a refused stop is swallowed too`() {
        // Nothing useful can be done about it, and the process is usually on its
        // way out by the time this runs.
        PresentationForegroundService.stop(RecordingContext(failStarts = true))
    }

    private companion object {
        const val START_ID = 7
    }
}
