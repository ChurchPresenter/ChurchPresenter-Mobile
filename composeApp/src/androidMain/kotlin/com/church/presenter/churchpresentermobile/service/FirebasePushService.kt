package com.church.presenter.churchpresentermobile.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.content.edit
import androidx.core.app.NotificationCompat
import com.church.presenter.churchpresentermobile.MainActivity
import com.church.presenter.churchpresentermobile.R
import com.church.presenter.churchpresentermobile.util.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM tokens and pushed notices.
 *
 * `open` only so a test can supply an application context: an unattached
 * service has none, and the token-persisting path is worth covering without an
 * emulator. Nothing in the app subclasses it.
 */
open class FirebasePushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Logger.d(TAG, "FCM token refreshed: $token")
        // Persist the token using the same SharedPreferences file as AppSettings
        applicationContext
            .getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit { putString(KEY_FCM_TOKEN, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Logger.d(TAG, "FCM message from ${message.from}")

        showNotification(
            title = notificationTitle(
                fromNotification = message.notification?.title,
                fromData = message.data[KEY_DATA_TITLE],
                default = getString(R.string.notification_default_title),
            ),
            body = notificationBody(
                fromNotification = message.notification?.body,
                fromData = message.data[KEY_DATA_BODY],
            ),
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun showNotification(title: String, body: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_cross)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)   // heads-up banner
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {

        /**
         * The heading to show for a pushed message.
         *
         * A push arrives one of two ways and the two are not interchangeable: a
         * *notification* message carries its own title, while a *data* message —
         * the kind the server sends so the app can act on it — carries the title
         * inside the payload. Taking only the first would leave every data push
         * showing the generic app name.
         *
         * `internal` rather than private so this can be tested: `RemoteMessage`
         * reads its fields out of an `android.os.Bundle`, which the unit-test
         * `android.jar` stubs into nothing, so the surrounding method cannot run
         * off a device. The choice it makes can.
         */
        internal fun notificationTitle(
            fromNotification: String?,
            fromData: String?,
            default: String,
        ): String = fromNotification ?: fromData ?: default

        /**
         * The body to show for a pushed message. Empty rather than a placeholder:
         * a title-only notice reads as intended, "null" does not.
         */
        internal fun notificationBody(fromNotification: String?, fromData: String?): String =
            fromNotification ?: fromData ?: ""

        const val CHANNEL_ID     = "church_presenter_channel"
        const val PREFS_NAME     = "church_presenter_prefs"
        const val KEY_FCM_TOKEN  = "fcm_token"
        private const val KEY_DATA_TITLE = "title"
        private const val KEY_DATA_BODY  = "body"
        private const val TAG    = "FirebasePushService"
    }
}
