package com.church.presenter.churchpresentermobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.church.presenter.churchpresentermobile.service.FirebasePushService
import com.church.presenter.churchpresentermobile.util.Analytics

class ChurchPresenterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Analytics.init()
        createNotificationChannel()
    }

    /**
     * Creates the notification channel as early as possible (at process start),
     * so it exists whether the app is in the foreground, background, or was just
     * cold-started by an incoming FCM message.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FirebasePushService.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                // HIGH → notifications appear as heads-up banners
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_description)
                enableLights(true)
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
