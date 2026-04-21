package com.church.presenter.churchpresentermobile.util

import platform.Foundation.NSBundle

actual val isDebugBuild: Boolean = false
actual val appVersion: String =
    NSBundle.mainBundle.infoDictionary
        ?.get("CFBundleShortVersionString") as? String
        ?: "unknown"
