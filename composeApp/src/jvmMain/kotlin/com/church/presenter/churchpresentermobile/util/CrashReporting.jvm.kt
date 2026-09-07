package com.church.presenter.churchpresentermobile.util

actual object CrashReporting {
    actual fun init() = Unit
    actual fun setEnabled(enabled: Boolean) = Unit
    actual fun log(message: String) = Unit
    actual fun recordException(throwable: Throwable) = Unit
    actual fun setUserId(userId: String) = Unit
    actual fun setCustomKey(key: String, value: String) = Unit
}
