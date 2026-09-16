package com.church.presenter.churchpresentermobile.util

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Wraps the Google Play In-App Update API.
 *
 * Prefers a [AppUpdateType.FLEXIBLE] update (background download, non-blocking)
 * and falls back to [AppUpdateType.IMMEDIATE] (full-screen) when flexible is not
 * allowed by the Play policy.
 *
 * Also resumes any [UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS]
 * that was interrupted (e.g. app was killed mid-download).
 */
object AppUpdate {

    private const val TAG = "AppUpdate"

    /** Package name of the Play Store app, as reported by `PackageManager.getInstallerPackageName`. */
    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    /**
     * Install error codes that simply mean "this build can't talk to the Play
     * Store" — normal for sideloaded/emulator/non-Play installs. Not real bugs,
     * so we log them but don't send them to crash reporting.
     */
    private val EXPECTED_NO_PLAY_ERRORS = setOf(
        InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND,
        InstallErrorCode.ERROR_APP_NOT_OWNED,
        // A transient device state (low battery, low disk space) refusing the
        // background install — the operator's phone, not our code. See
        // CHURCH-PRESENTER-MOBILE-1K.
        InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED,
    )

    /**
     * Checks the Play Store for an available update and starts the update flow
     * if one is found. Must be called from an [ComponentActivity] that has
     * already registered [launcher] via [registerForActivityResult].
     */
    fun checkAndPrompt(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        // Play Core binds to a Play Store service on its own HandlerThread as soon
        // as appUpdateInfo() is called. On a device with no real Play Store to bind
        // to — every sideloaded APK from the release workflow (see TESTING.md) and
        // every emulator — that bind can fail as an uncaught exception on that
        // thread ("Failed to bind to the service"), which crashes the app before
        // any Task success/failure callback ever runs, so EXPECTED_NO_PLAY_ERRORS
        // above never gets a chance to see it. See CHURCH-PRESENTER-MOBILE-1D/1J.
        val installer = activity.packageManager.getInstallerPackageName(activity.packageName)
        if (installer != PLAY_STORE_PACKAGE) {
            Logger.d(TAG, "Skipping update check — not installed via Play Store (installer=$installer)")
            return
        }

        val manager = AppUpdateManagerFactory.create(activity)
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                when (info.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE -> {
                        val type = if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
                            AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
                        Logger.d(TAG, "Update available — launching type=$type")
                        manager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(type).build()
                        )
                    }
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        // Resume an immediate update that was interrupted
                        Logger.d(TAG, "Resuming interrupted immediate update")
                        manager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                    }
                    else -> Logger.d(TAG, "No update available")
                }
            }
            .addOnFailureListener { e ->
                // Expected on sideloaded / non-Play installs (emulators, alternative
                // stores, direct APK) — there is no Play Store to query, so treat it
                // as noise rather than reporting it to crash reporting.
                if (e is InstallException && e.errorCode in EXPECTED_NO_PLAY_ERRORS) {
                    Logger.d(TAG, "Skipping update check — no Play Store (errorCode=${e.errorCode})")
                } else {
                    Logger.e(TAG, "Failed to check for app update", e)
                    CrashReporting.recordException(e)
                }
            }
    }
}

