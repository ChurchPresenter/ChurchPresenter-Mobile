package com.church.presenter.churchpresentermobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.church.presenter.churchpresentermobile.model.getAppContext
import java.util.UUID

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val os: String = "android"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun generateUUID(): String = UUID.randomUUID().toString()

/**
 * The name the user gave the phone in Settings, falling back to the model.
 *
 * Settings.Global.DEVICE_NAME is what "Pixel 7 Pro" or "Sam's phone" lives in;
 * it is null on devices that never had one set, where the model is still far
 * better than a UUID.
 */
actual fun deviceName(): String {
    val fromSettings = getAppContext()?.let { context ->
        runCatching { Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) }
            .getOrNull()
    }
    return fromSettings?.takeIf { it.isNotBlank() } ?: "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}

actual fun openUrl(url: String) {
    val ctx = getAppContext() ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

