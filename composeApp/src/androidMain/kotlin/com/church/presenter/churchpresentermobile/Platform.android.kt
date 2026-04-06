package com.church.presenter.churchpresentermobile

import android.os.Build
import java.util.UUID

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun generateUUID(): String = UUID.randomUUID().toString()

