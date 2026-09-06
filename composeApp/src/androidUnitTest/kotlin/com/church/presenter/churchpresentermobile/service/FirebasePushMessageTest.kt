package com.church.presenter.churchpresentermobile.service

import android.content.Context
import com.church.presenter.churchpresentermobile.testutil.RecordingContext
import com.google.firebase.messaging.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What a pushed message is shown as.
 *
 * A push arrives one of two ways. A *notification* message carries its own
 * title and body, which Android would draw by itself; a *data* message — the
 * kind the server sends when the app has to act on it — carries the same two
 * fields inside the payload and is drawn by this service. Reading only the
 * first would leave every data push showing the generic app name and no text.
 *
 * `RemoteMessage` reads its fields out of an `android.os.Bundle`, which the
 * unit-test `android.jar` stubs into nothing, so it is mocked here. The service
 * itself is real; only the final `showNotification` call, which needs a working
 * `Notification.Builder`, is stubbed out.
 */
class FirebasePushMessageTest {

    private class TestableService(private val context: Context) : FirebasePushService() {
        override fun getApplicationContext(): Context = context
    }

    /** The service with the notification-drawing step stubbed, plus the title/body it was given. */
    private class Harness {
        val service = spyk(TestableService(RecordingContext()), recordPrivateCalls = true)
        var title: String? = null
        var body: String? = null

        init {
            every { service.getString(any()) } returns DEFAULT_TITLE
            every { service["showNotification"](any<String>(), any<String>()) } answers {
                title = firstArg()
                body = secondArg()
            }
        }

        fun deliver(message: RemoteMessage) = service.onMessageReceived(message)
    }

    /** A push whose title and body arrive in whichever half of the message is given. */
    private fun message(
        notificationTitle: String? = null,
        notificationBody: String? = null,
        data: Map<String, String> = emptyMap(),
    ): RemoteMessage = mockk<RemoteMessage>(relaxed = true) {
        every { from } returns "/topics/all"
        every { getData() } returns data.toMutableMap()
        every { notification } returns
            if (notificationTitle == null && notificationBody == null) null
            else mockk(relaxed = true) {
                every { title } returns notificationTitle
                every { body } returns notificationBody
            }
    }

    @Test
    fun `a notification message is shown as the server wrote it`() {
        val harness = Harness()

        harness.deliver(message(notificationTitle = "Service starts at 10", notificationBody = "Doors 9:30"))

        assertEquals("Service starts at 10", harness.title)
        assertEquals("Doors 9:30", harness.body)
    }

    @Test
    fun `a data message is shown from its payload`() {
        // The kind the desktop sends. Without the payload fallback this would
        // show the app name and nothing else.
        val harness = Harness()

        harness.deliver(message(data = mapOf("title" to "New song added", "body" to "Amazing Grace")))

        assertEquals("New song added", harness.title)
        assertEquals("Amazing Grace", harness.body)
    }

    @Test
    fun `a message with neither falls back to the app's own title`() {
        val harness = Harness()

        harness.deliver(message())

        assertEquals(DEFAULT_TITLE, harness.title)
        assertEquals("", harness.body)
    }

    @Test
    fun `an empty body is shown as empty rather than as the word null`() {
        val harness = Harness()

        harness.deliver(message(notificationTitle = "Reminder"))

        assertEquals("Reminder", harness.title)
        assertEquals("", harness.body)
    }

    @Test
    fun `the notification half wins when a message carries both`() {
        // Android draws a notification message itself when the app is in the
        // background, so following the same field here keeps the two consistent.
        val harness = Harness()

        harness.deliver(
            message(
                notificationTitle = "From the notification",
                notificationBody = "Notification body",
                data = mapOf("title" to "From the data", "body" to "Data body"),
            )
        )

        assertEquals("From the notification", harness.title)
        assertEquals("Notification body", harness.body)
    }

    @Test
    fun `every message reaches the notification drawer`() {
        val harness = Harness()

        harness.deliver(message(data = mapOf("title" to "One")))
        harness.deliver(message(data = mapOf("title" to "Two")))

        verify(exactly = 2) { harness.service["showNotification"](any<String>(), any<String>()) }
    }

    // ── The choice itself ────────────────────────────────────────────────

    @Test
    fun `the title prefers the notification, then the data, then the default`() {
        assertEquals("n", FirebasePushService.notificationTitle("n", "d", "fallback"))
        assertEquals("d", FirebasePushService.notificationTitle(null, "d", "fallback"))
        assertEquals("fallback", FirebasePushService.notificationTitle(null, null, "fallback"))
    }

    @Test
    fun `the body prefers the notification, then the data, then nothing`() {
        assertEquals("n", FirebasePushService.notificationBody("n", "d"))
        assertEquals("d", FirebasePushService.notificationBody(null, "d"))
        assertEquals("", FirebasePushService.notificationBody(null, null))
    }

    private companion object {
        const val DEFAULT_TITLE = "Church Presenter"
    }
}
