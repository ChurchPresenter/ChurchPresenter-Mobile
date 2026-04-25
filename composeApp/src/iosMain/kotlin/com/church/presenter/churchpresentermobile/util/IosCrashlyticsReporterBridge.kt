package com.church.presenter.churchpresentermobile.util

/**
 * Interface implemented in Swift and injected from [iOSApp.swift] so that
 * Kotlin can call Firebase Crashlytics on iOS without requiring a CocoaPods
 * dependency in the KMP build.
 *
 * Swift usage (in AppDelegate.application(_:didFinishLaunchingWithOptions:)):
 * ```swift
 * IosCrashlyticsReporterBridge.shared.reporter = SwiftCrashlyticsReporter()
 * ```
 */
interface IosCrashlyticsReporter {
    fun log(message: String)
    fun recordError(message: String, type: String, stackTrace: String)
    fun setUserId(userId: String)
    fun setCustomKey(key: String, value: String)
}

/**
 * Singleton holder — set once from Swift at startup, then called from
 * [CrashReporting] for every non-fatal event.
 */
object IosCrashlyticsReporterBridge {
    /** Assigned from Swift before any Kotlin code runs network operations. */
    var reporter: IosCrashlyticsReporter? = null
}

