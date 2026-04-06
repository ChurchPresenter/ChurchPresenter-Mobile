package com.church.presenter.churchpresentermobile.util

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
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

    /**
     * Checks the Play Store for an available update and starts the update flow
     * if one is found. Must be called from an [ComponentActivity] that has
     * already registered [launcher] via [registerForActivityResult].
     */
    fun checkAndPrompt(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
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
                Logger.e(TAG, "Failed to check for app update", e)
                CrashReporting.recordException(e)
            }
    }
}

