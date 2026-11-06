package com.church.presenter.churchpresentermobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import com.church.presenter.churchpresentermobile.R
import com.church.presenter.churchpresentermobile.present.NowShowing
import com.church.presenter.churchpresentermobile.util.ActivityHolder
import com.church.presenter.churchpresentermobile.util.CrashReporting
import com.church.presenter.churchpresentermobile.util.Logger

private const val TAG = "PresentationService"

/**
 * Keeps the standalone presentation server alive and reachable while the app is
 * not in front.
 *
 * Two separate problems, two locks. A partial [PowerManager.WakeLock] stops the
 * CPU sleeping, and a high-performance [WifiManager.WifiLock] stops Wi-Fi
 * power-saving — without the latter, a locked phone drops LAN sockets after a
 * minute or two and the TV goes to the standby screen mid-song, which is the
 * single most damaging way this feature could fail.
 *
 * The notification is not decoration: a foreground service is the only way
 * Android permits this, and it also gives the operator a visible, tappable
 * reminder that the phone is currently driving a screen. It is a media player,
 * with the slide as its album art and Close as its button, and follows every
 * slide change ([show]).
 */
class PresentationForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A null intent means the system restarted us on its own after killing
        // the process. Calling startForeground() here is what crashed the app:
        // the restart arrives with the app in the background, where Android 12+
        // refuses foreground-service starts outright, and the refusal surfaces
        // as an uncaught ForegroundServiceStartNotAllowedException on the main
        // thread. There is nothing useful to restart into anyway — the server is
        // gone with the process, and WebPageSink re-attaches (re-supplying the
        // URL) when the app comes back. So stand down quietly.
        if (intent == null) {
            Logger.d(TAG, "system restart with no intent — standing down")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (intent.action == ACTION_CLOSE_APP) {
            closeApp()
            return START_NOT_STICKY
        }

        // Even a legitimate start can be denied: startForegroundService() only
        // buys a few seconds of grace, and if the app is backgrounded or the
        // device dozes before this runs, the allowance is gone. Losing the
        // keep-alive degrades the feature; crashing the operator's phone
        // mid-service is far worse. Caught as Exception rather than by type so
        // no API 31+ class is referenced on older devices.
        url = intent.getStringExtra(EXTRA_URL)
        val started = runCatching {
            startForeground(NOTIFICATION_ID, buildNotification(url))
        }
        if (started.isFailure) {
            val e = started.exceptionOrNull()
            Logger.e(TAG, "foreground start refused: ${e?.message}", e)
            e?.let { CrashReporting.recordException(it) }
            stopSelf(startId)
            return START_NOT_STICKY
        }

        running = true
        acquireLocks()
        Logger.d(TAG, "presentation service started")
        // Deliberately not sticky: see the null-intent branch above.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        releaseLocks()
        releaseMedia()
        Logger.d(TAG, "presentation service stopped")
        super.onDestroy()
    }

    /**
     * The notification's Close button: stops presenting and ends the app. The
     * display server lives in this process, so ending the process is what takes
     * it down; finishing the screen alone would leave it serving.
     */
    private fun closeApp() {
        Logger.d(TAG, "closing the app from the notification")
        ActivityHolder.current?.finishAndRemoveTask()
        releaseMedia()
        stopSelf()
        Handler(Looper.getMainLooper()).post { Process.killProcess(Process.myPid()) }
    }

    private fun acquireLocks() {
        if (wakeLock != null) return
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = power?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)?.apply {
            setReferenceCounted(false)
            acquire(MAX_HOLD_MS)
        }

        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wifi?.createWifiLock(lockMode, WIFI_LOCK_TAG)?.apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseLocks() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
        wifiLock = null
    }

    // A seam rather than an inline call: PresentationLocksTest stubs it, since a
    // working Notification.Builder is the one thing the unit-test android.jar lacks.
    private fun buildNotification(url: String?): Notification {
        createChannel()
        val session = media ?: PresentationMediaSession(this, ::closeApp).also { media = it }
        return buildPresentationNotification(this, url, nowShowing, session)
    }

    private fun releaseMedia() {
        media?.release()
        media = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(PRESENTATION_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                PRESENTATION_CHANNEL_ID,
                getString(R.string.presentation_service_channel),
                // LOW: persistent, silent, no heads-up. It is a status indicator,
                // not an alert — an operator must never be buzzed mid-service.
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 4711
        private const val WAKE_LOCK_TAG = "ChurchPresenter::presentation"
        private const val WIFI_LOCK_TAG = "ChurchPresenter::presentation-wifi"
        private const val EXTRA_URL = "url"
        private const val ACTION_CLOSE_APP = "com.church.presenter.churchpresentermobile.CLOSE_APP"

        /** Longest a single service run holds the CPU awake — a very long service. */
        private const val MAX_HOLD_MS = 6 * 60 * 60 * 1000L

        // What the notification pictures. Held here rather than on the instance
        // because the slide changes while the service runs, from code that has
        // a Context but no handle on the service.
        @Volatile private var url: String? = null
        @Volatile private var nowShowing: NowShowing? = null
        @Volatile private var running = false
        @Volatile private var media: PresentationMediaSession? = null

        /**
         * Redraws the notification for what the screens now show. Remembered
         * when the service is not running, so it starts already showing it.
         */
        fun show(context: Context, nowShowing: NowShowing?) {
            this.nowShowing = nowShowing
            if (!running) return
            runCatching {
                context.getSystemService(NotificationManager::class.java)
                    ?.notify(NOTIFICATION_ID, buildPresentationNotification(context, url, nowShowing, media))
            }.onFailure { Logger.e(TAG, "could not update the notification: ${it.message}") }
        }

        /** What the notification's Close button sends this service. */
        internal fun closeAppIntent(context: Context): PendingIntent = PendingIntent.getService(
            context,
            0,
            Intent(context, PresentationForegroundService::class.java).setAction(ACTION_CLOSE_APP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun start(context: Context, url: String?) {
            val intent = Intent(context, PresentationForegroundService::class.java)
            intent.putExtra(EXTRA_URL, url)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.onFailure { Logger.e(TAG, "could not start presentation service: ${it.message}") }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, PresentationForegroundService::class.java))
            }
        }
    }
}
