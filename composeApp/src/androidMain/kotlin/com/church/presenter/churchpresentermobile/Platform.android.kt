package com.church.presenter.churchpresentermobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.church.presenter.churchpresentermobile.model.getAppContext
import java.util.UUID

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun generateUUID(): String = UUID.randomUUID().toString()

actual fun openUrl(url: String) {
    val ctx = getAppContext() ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

