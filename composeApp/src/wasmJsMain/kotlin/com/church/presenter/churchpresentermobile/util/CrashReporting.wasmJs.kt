package com.church.presenter.churchpresentermobile.util

/**
 * The browser build has no Crashlytics: logs and exceptions go to the console,
 * and the calls that only shape a crash report (user id, custom keys) have
 * nothing to attach to, so they do nothing.
 */
actual object CrashReporting {
    actual fun init() = Unit
    actual fun setEnabled(enabled: Boolean) = Unit
    actual fun log(message: String) { println("[CrashReporting] $message") }
    actual fun recordException(throwable: Throwable) { throwable.printStackTrace() }
    actual fun setUserId(userId: String) = Unit
    actual fun setCustomKey(key: String, value: String) = Unit
}
