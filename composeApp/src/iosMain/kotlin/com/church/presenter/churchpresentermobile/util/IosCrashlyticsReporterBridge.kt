package com.church.presenter.churchpresentermobile.util

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ReportUnhandledExceptionHook
import kotlin.native.setUnhandledExceptionHook

/**
 * Interface implemented in Swift and injected from [iOSApp.swift] so that
 * Kotlin can call Firebase Crashlytics on iOS without requiring a CocoaPods
 * dependency in the KMP build. The Swift implementation (SwiftCrashlyticsReporter)
 * also forwards every call to Sentry — the interface name is kept as-is to
 * avoid a wider Kotlin+Swift rename.
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
    fun setEnabled(enabled: Boolean)
}

/**
 * Singleton holder — set once from Swift at startup, then called from
 * [CrashReporting] for every non-fatal event.
 */
object IosCrashlyticsReporterBridge {
    /**
     * Assigned from Swift before any Kotlin code runs network operations. Assigning it also
     * installs [installUnhandledExceptionHook], since this is the first Kotlin the app runs.
     */
    var reporter: IosCrashlyticsReporter? = null
        set(value) {
            field = value
            if (value != null) installUnhandledExceptionHook()
        }

    private var hookInstalled = false

    /**
     * Reports a Kotlin exception nothing caught before the runtime aborts the process.
     *
     * Without it the crash reaches Sentry only as the SIGABRT that follows, with no exception
     * type, message or Kotlin stack. The hook records it as an error first (Sentry stores the
     * event on disk before sending, so it goes out on the next launch), then hands over to any
     * hook installed before it; the runtime terminates the process either way.
     */
    @OptIn(ExperimentalNativeApi::class)
    private fun installUnhandledExceptionHook() {
        if (hookInstalled) return
        hookInstalled = true
        var previous: ReportUnhandledExceptionHook? = null
        previous = setUnhandledExceptionHook { throwable ->
            runCatching { CrashReporting.recordException(throwable) }
            previous?.invoke(throwable)
        }
    }
}

