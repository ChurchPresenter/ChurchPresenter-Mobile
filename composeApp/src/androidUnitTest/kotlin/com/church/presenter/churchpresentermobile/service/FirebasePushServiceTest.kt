package com.church.presenter.churchpresentermobile.service

import android.content.Context
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AndroidSettingsStorage
import com.church.presenter.churchpresentermobile.model.initSettingsContext
import com.church.presenter.churchpresentermobile.testutil.RecordingContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers where [FirebasePushService] puts the FCM registration token.
 *
 * The token is the only thing this service persists, and it is written straight
 * to `SharedPreferences` rather than through [AppSettings] — so the file name
 * and key are duplicated between the two. Get either wrong and the token is
 * saved somewhere nothing reads: push registration then looks fine on the
 * device and the server never learns the address. These tests read it back the
 * way the app does, so the two cannot drift apart unnoticed.
 *
 * `onMessageReceived` is not covered here: it needs a real `RemoteMessage`,
 * whose data map is read out of an `android.os.Bundle` that the unit-test
 * `android.jar` stubs into nothing.
 */
class FirebasePushServiceTest {

    /**
     * The service with a context that answers for preferences.
     *
     * `onNewToken` reaches for `applicationContext`, which is null on an
     * unattached service — overriding it is all that is needed to run the real
     * method against a real (in-memory) preference store.
     */
    private class TestableService(private val context: Context) : FirebasePushService() {
        override fun getApplicationContext(): Context = context
    }

    @Test
    fun `a refreshed token is persisted`() {
        val context = RecordingContext()

        TestableService(context).onNewToken("token-abc")

        val prefs = context.preferenceFiles.getValue(FirebasePushService.PREFS_NAME)
        assertEquals("token-abc", prefs.getString(FirebasePushService.KEY_FCM_TOKEN, null))
    }

    @Test
    fun `the token lands where the app settings read it`() {
        // The real coupling: AppSettings.fcmToken reads its own key out of its own
        // preference file, and this service writes without going through it.
        val context = RecordingContext()
        initSettingsContext(context)

        TestableService(context).onNewToken("token-xyz")

        assertEquals("token-xyz", AppSettings(AndroidSettingsStorage()).fcmToken)
    }

    @Test
    fun `a re-issued token replaces the one before it`() {
        // FCM rotates tokens; keeping the stale one would send to a dead address.
        val context = RecordingContext()
        val service = TestableService(context)

        service.onNewToken("first")
        service.onNewToken("second")

        val prefs = context.preferenceFiles.getValue(FirebasePushService.PREFS_NAME)
        assertEquals("second", prefs.getString(FirebasePushService.KEY_FCM_TOKEN, null))
    }

    @Test
    fun `only the token file is touched`() {
        // A second preference file would be a sign the name had been mistyped.
        val context = RecordingContext()

        TestableService(context).onNewToken("token-abc")

        assertEquals(setOf(FirebasePushService.PREFS_NAME), context.preferenceFiles.keys)
    }

    @Test
    fun `the notification channel is the one the app creates`() {
        // Notifications posted to a channel that was never created are dropped
        // silently by Android; this id has to match the one MainActivity registers.
        assertTrue(FirebasePushService.CHANNEL_ID.isNotBlank())
        assertEquals("church_presenter_channel", FirebasePushService.CHANNEL_ID)
    }
}
