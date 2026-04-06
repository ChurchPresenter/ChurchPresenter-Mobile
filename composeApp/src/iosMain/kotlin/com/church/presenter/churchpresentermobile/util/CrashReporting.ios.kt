package com.church.presenter.churchpresentermobile.util

/**
 * iOS stub — Firebase Crashlytics is initialised automatically when
 * FirebaseApp.configure() is called from iOSApp.swift.
 * Non-fatal recording and user-id are bridged via Swift helpers if needed.
 */
actual object CrashReporting {
    actual fun init() {
        // Crashlytics auto-starts after FirebaseApp.configure() in iOSApp.swift
    }

    actual fun log(message: String) {
        println("[Crashlytics] $message")
    }

    actual fun recordException(throwable: Throwable) {
        println("[Crashlytics] Non-fatal: ${throwable.message}")
        throwable.printStackTrace()
    }

    actual fun setUserId(userId: String) {
        // Bridge to Swift: Crashlytics.crashlytics().setUserID(userId)
        println("[Crashlytics] userId=$userId")
    }
}

