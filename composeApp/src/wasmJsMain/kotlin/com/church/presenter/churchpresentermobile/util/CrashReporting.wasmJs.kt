package com.church.presenter.churchpresentermobile.util

actual object CrashReporting {
    actual fun init() {}
    actual fun log(message: String) { println("[CrashReporting] $message") }
    actual fun recordException(throwable: Throwable) { throwable.printStackTrace() }
    actual fun setUserId(userId: String) {}
    actual fun setCustomKey(key: String, value: String) {}
}

