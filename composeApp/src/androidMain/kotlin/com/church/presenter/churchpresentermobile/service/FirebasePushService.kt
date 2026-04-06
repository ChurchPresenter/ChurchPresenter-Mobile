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

class FirebasePushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Logger.d(TAG, "FCM token refreshed: $token")
        // Persist the token using the same SharedPreferences file as AppSettings
        applicationContext
            .getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit { putString(KEY_FCM_TOKEN, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Logger.d(TAG, "FCM message from ${message.from}")

        val title = message.notification?.title
            ?: message.data[KEY_DATA_TITLE]
            ?: getString(R.string.notification_default_title)
        val body = message.notification?.body
            ?: message.data[KEY_DATA_BODY]
            ?: ""

        showNotification(title, body)
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
        const val CHANNEL_ID     = "church_presenter_channel"
        const val PREFS_NAME     = "church_presenter_prefs"
        const val KEY_FCM_TOKEN  = "fcm_token"
        private const val KEY_DATA_TITLE = "title"
        private const val KEY_DATA_BODY  = "body"
        private const val TAG    = "FirebasePushService"
    }
}
