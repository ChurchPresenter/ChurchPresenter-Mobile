package com.church.presenter.churchpresentermobile.util

/**
 * iOS actual — delegates to [IosCrashlyticsReporterBridge.reporter] which is
 * set from Swift (AppDelegate) using the native FirebaseCrashlytics SDK.
 *
 * When the bridge is not yet set (e.g. during unit tests) all calls fall back
 * to println so nothing crashes.
 *
 * Firebase Crashlytics is auto-started by FirebaseApp.configure() in iOSApp.swift,
 * so no explicit init() call is needed on this side.
 */
actual object CrashReporting {

    actual fun init() {
        // Crashlytics auto-starts after FirebaseApp.configure() in iOSApp.swift
    }

    actual fun log(message: String) {
        IosCrashlyticsReporterBridge.reporter?.log(message)
            ?: println("[Crashlytics] $message")
    }

    actual fun recordException(throwable: Throwable) {
        val bridge = IosCrashlyticsReporterBridge.reporter
        if (bridge != null) {
            bridge.recordError(
                message    = throwable.message ?: "Unknown error",
                type       = throwable::class.simpleName ?: "Throwable",
                stackTrace = throwable.stackTraceToString().take(4_000),
            )
        } else {
            println("[Crashlytics] Non-fatal: ${throwable.message}")
            throwable.printStackTrace()
        }
    }

    actual fun setUserId(userId: String) {
        IosCrashlyticsReporterBridge.reporter?.setUserId(userId)
            ?: println("[Crashlytics] userId=$userId")
    }

    actual fun setCustomKey(key: String, value: String) {
        IosCrashlyticsReporterBridge.reporter?.setCustomKey(key, value)
    }
}
