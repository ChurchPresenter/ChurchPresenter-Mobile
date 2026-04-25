package com.church.presenter.churchpresentermobile.util

/**
 * Interface implemented in Swift and injected from [iOSApp.swift] so that
 * Kotlin can call Firebase Analytics on iOS without requiring a CocoaPods
 * dependency in the KMP build.
 */
interface IosAnalyticsReporter {
    fun logEvent(name: String, params: Map<String, String>)
}

/**
 * Singleton holder — set once from Swift at startup, then called from
 * [Analytics] for every tracked event.
 */
object IosAnalyticsBridge {
    /** Assigned from Swift before any Kotlin code logs events. */
    var reporter: IosAnalyticsReporter? = null
}

