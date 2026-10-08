package com.church.presenter.churchpresentermobile.util

import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.Lifecycle
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
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
                val type = when (info.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE ->
                        if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
                            AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
                    // Resume an immediate update that was interrupted
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> AppUpdateType.IMMEDIATE
                    else -> {
                        Logger.d(TAG, "No update available")
                        return@addOnSuccessListener
                    }
                }
                startFlow(activity, manager, info, launcher, type)
            }
            .addOnFailureListener { e ->
                // Expected on sideloaded / non-Play installs (emulators, alternative
                // stores, direct APK) — there is no Play Store to query, so treat it
                // as noise rather than reporting it to crash reporting.
                if (e is InstallException && e.errorCode in EXPECTED_NO_PLAY_ERRORS) {
                    Logger.d(TAG, "Skipping update check — no Play Store (errorCode=${e.errorCode})")
                } else if (e.isPlayStoreGone()) {
                    Logger.d(TAG, "Skipping update check — the Play Store service went away: ${e.message}")
                } else {
                    Logger.e(TAG, "Failed to check for app update", e)
                    CrashReporting.recordException(e)
                }
            }
    }

    /**
     * Starts the update flow, unless the activity that asked is gone.
     *
     * Play answers on its own time. By then the activity may have been destroyed —
     * rotated, or closed — and its [launcher] unregistered with it, and launching
     * an unregistered launcher throws (CHURCH-PRESENTER-MOBILE-23). The activity
     * that replaces it runs its own check, so nothing is lost by skipping this one.
     */
    private fun startFlow(
        activity: ComponentActivity,
        manager: AppUpdateManager,
        info: AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        type: Int,
    ) {
        if (activity.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            Logger.d(TAG, "Update available, but the activity that asked is gone — not prompting")
            return
        }
        Logger.d(TAG, "Update available — launching type=$type")
        try {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        } catch (e: IllegalStateException) {
            // The launcher went with its activity between the check above and here.
            Logger.e(TAG, "Update flow not started: ${e.message}", e)
        }
    }

    /**
     * True when the Play Store's update service died while answering — the Play
     * Store app updating itself, or being killed for memory on a small device
     * (CHURCH-PRESENTER-MOBILE-22, "AppUpdateService : Binder has died"). Not a
     * defect here; the next launch asks again.
     */
    private fun Throwable.isPlayStoreGone(): Boolean =
        generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).any { it is RemoteException }

    private const val MAX_CAUSE_DEPTH = 8
}

