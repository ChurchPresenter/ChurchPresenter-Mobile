package com.church.presenter.churchpresentermobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.initSettingsContext
import com.church.presenter.churchpresentermobile.service.FirebasePushService
import com.church.presenter.churchpresentermobile.util.Analytics
import com.church.presenter.churchpresentermobile.util.CrashReporting

class ChurchPresenterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Needed before any AppSettings() use below — also covers entry points
        // other than MainActivity (e.g. FirebasePushService) that run first.
        initSettingsContext(this)
        // Start Sentry as early as possible so it can catch crashes during
        // the rest of app startup too.
        CrashReporting.initSentry(this)
        Analytics.init()
        // Re-apply the user's persisted privacy preference — Sentry doesn't
        // remember being closed across process restarts.
        val telemetryEnabled = AppSettings().isTelemetryEnabled
        CrashReporting.setEnabled(telemetryEnabled)
        Analytics.setEnabled(telemetryEnabled)
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
