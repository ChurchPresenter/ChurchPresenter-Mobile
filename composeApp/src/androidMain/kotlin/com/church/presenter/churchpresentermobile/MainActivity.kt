package com.church.presenter.churchpresentermobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.initSettingsContext
import com.church.presenter.churchpresentermobile.util.ActivityHolder
import com.church.presenter.churchpresentermobile.util.AppReview
import com.church.presenter.churchpresentermobile.util.AppUpdate
import com.church.presenter.churchpresentermobile.util.CrashReporting
import com.church.presenter.churchpresentermobile.util.RemoteConfig
import com.church.presenter.churchpresentermobile.util.RemoteConfigDefaults
import com.church.presenter.churchpresentermobile.util.RemoteConfigKeys

class MainActivity : ComponentActivity() {

    private lateinit var updateLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Register update launcher before super.onCreate()
        updateLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != RESULT_OK) {
                CrashReporting.log("Update flow not completed — resultCode=${result.resultCode}")
            }
        }

        enableEdgeToEdge()
        initSettingsContext(this)
        // A Presentation window on a second display is a Dialog and needs a real
        // Activity context, which the output sink has no other way to reach.
        ActivityHolder.attach(this)
        super.onCreate(savedInstanceState)

        val settings = AppSettings()

        // Crash reporting — associate reports with this device's stable ID
        CrashReporting.init()
        CrashReporting.setUserId(settings.deviceId)

        // App update — check Play Store and prompt if a new version is available
        AppUpdate.checkAndPrompt(this, updateLauncher)

        // Track launches and show the Play in-app review prompt at milestones
        val openCount = settings.appOpenCount + 1
        settings.appOpenCount = openCount
        AppReview.maybeRequest(this, openCount)

        // Remote Config — set defaults and fetch latest values from Firebase
        RemoteConfig.init(
            defaults = mapOf(
                RemoteConfigKeys.MAINTENANCE_MODE              to RemoteConfigDefaults.MAINTENANCE_MODE,
                RemoteConfigKeys.MIN_APP_VERSION               to RemoteConfigDefaults.MIN_APP_VERSION,
                RemoteConfigKeys.ANNOUNCEMENT_BANNER           to RemoteConfigDefaults.ANNOUNCEMENT_BANNER,
                RemoteConfigKeys.FEATURE_BIBLE_ENABLED         to RemoteConfigDefaults.FEATURE_BIBLE_ENABLED,
                RemoteConfigKeys.FEATURE_SONGS_ENABLED         to RemoteConfigDefaults.FEATURE_SONGS_ENABLED,
                RemoteConfigKeys.FEATURE_PICTURES_ENABLED      to RemoteConfigDefaults.FEATURE_PICTURES_ENABLED,
                RemoteConfigKeys.FEATURE_PRESENTATION_ENABLED  to RemoteConfigDefaults.FEATURE_PRESENTATION_ENABLED,
                RemoteConfigKeys.IS_DEMO_MODE                  to RemoteConfigDefaults.IS_DEMO_MODE,
            ),
            fetchIntervalSeconds = if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
                                      RemoteConfigDefaults.FETCH_INTERVAL_DEBUG
                                  else RemoteConfigDefaults.FETCH_INTERVAL_PRODUCTION
        )
        RemoteConfig.fetchAndActivate { activated ->
            CrashReporting.log("RemoteConfig fetchAndActivate — activated=$activated")
        }

        // Push notifications — request runtime permission (Android 13+)
        requestNotificationPermission()

        setContent { App() }

        // Deep link from cold-start is picked up in onResume (see below).
    }

    // Called when a churchpresenter:// URI arrives while the app is already running.
    // We store the new intent so onResume (which always fires when the app comes
    // to foreground) can process it once Compose is definitely active.
    override fun onDestroy() {
        ActivityHolder.detach(this)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // Process any pending deep link here rather than in onCreate / onNewIntent.
    // onResume is the earliest point guaranteed to run AFTER Compose is active,
    // which matters when the app was in the background (paused / stopped) when
    // the QR code was scanned and the camera app delivered the intent.
    override fun onResume() {
        super.onResume()
        processStoredDeepLink()
        processShortcutIntent()
    }

    // Deduplicate so the same URL isn't handled twice (e.g. after a config change).
    private var lastHandledDeepLinkUrl: String? = null

    private fun processStoredDeepLink() {
        val url = deepLinkToHandle(intent?.dataString, lastHandledDeepLinkUrl) ?: return
        lastHandledDeepLinkUrl = url
        DeepLinkHandler.handle(url, AppSettings())
    }

    // ── App shortcut handling ─────────────────────────────────────────────
    // Shortcuts fire via the intent action set in shortcuts.xml.
    // We deduplicate using the same action string to avoid re-triggering on
    // subsequent resumes (e.g. after the user rotates the screen).
    private var lastHandledShortcutAction: String? = null

    private fun processShortcutIntent() {
        val action = intent?.action ?: return
        if (action == lastHandledShortcutAction) return
        lastHandledShortcutAction = action
        shortcutTab(action)?.let { TabNavigationHandler.navigateTo(it) }
    }


    // ── Runtime notification permission (required on API 33+) ────────────────
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    RC_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    companion object {
        private const val RC_NOTIFICATION_PERMISSION = 1001
    }
}

/** The scheme a QR code / deep link must use for this app to act on it. */
internal const val DEEP_LINK_SCHEME = "churchpresenter://"

internal const val SHORTCUT_OPEN_SONGS = "com.church.presenter.churchpresentermobile.OPEN_SONGS"
internal const val SHORTCUT_OPEN_BIBLE = "com.church.presenter.churchpresentermobile.OPEN_BIBLE"

/**
 * The deep link to act on, or null when there is nothing new to do.
 *
 * Pulled out of [MainActivity] because it is the only part of the deep-link path
 * that can be wrong, and the Activity around it needs an emulator to run.
 *
 * The dedupe against [lastHandled] is the point: this is reached from `onResume`,
 * which fires again on every foreground and every configuration change, and the
 * intent is still attached. Without it, one scanned QR code re-applies its
 * host/port/key every time the screen rotates.
 *
 * @param url the intent's data string, which is absent for an ordinary launch.
 * @param lastHandled the last URL acted on, so the same one is ignored.
 */
internal fun deepLinkToHandle(url: String?, lastHandled: String?): String? {
    if (url == null) return null
    if (!url.startsWith(DEEP_LINK_SCHEME)) return null
    if (url == lastHandled) return null
    return url
}

/**
 * The tab a home-screen shortcut opens, or null when the action is not one.
 *
 * Separate from the Activity for the same reason, and worth pinning because the
 * mapping is by string: a typo here sends someone who long-pressed "Songs" to
 * the Bible, and nothing else would notice.
 */
internal fun shortcutTab(action: String?): AppTab? = when (action) {
    SHORTCUT_OPEN_SONGS -> AppTab.SONGS
    SHORTCUT_OPEN_BIBLE -> AppTab.BIBLE
    else -> null
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}