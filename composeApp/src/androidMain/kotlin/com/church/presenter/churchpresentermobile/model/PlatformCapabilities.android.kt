package com.church.presenter.churchpresentermobile.model

import android.content.Intent
import android.provider.Settings

actual val supportsStandalone: Boolean = true

actual val supportsEmbeddedServer: Boolean = true

actual val supportsExternalDisplay: Boolean = true

actual val isForegroundOnlyServer: Boolean = false

actual val mirroringRoute: MirroringRoute = MirroringRoute.CAST

actual val canOpenMirroringSettings: Boolean = true

/**
 * Opens the system cast picker.
 *
 * Wrapped in [runCatching] because [Settings.ACTION_CAST_SETTINGS] is a public
 * intent that some OEM builds still ship without a matching activity, and a
 * missing settings screen must not crash the operator mid-service.
 */
actual fun openMirroringSettings() {
    runCatching {
        val intent = Intent(Settings.ACTION_CAST_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getAppContext()?.startActivity(intent)
    }
}
