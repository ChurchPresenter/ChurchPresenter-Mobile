package com.church.presenter.churchpresentermobile.util

/**
 * Multiplatform crash-reporting abstraction.
 *
 * Android  → Firebase Crashlytics
 * iOS      → Firebase Crashlytics (initialised from Swift via FirebaseApp.configure())
 * JS/WASM  → console no-op stubs
 */
expect object CrashReporting {
    /** Enable collection and mark Crashlytics as ready. */
    fun init()

    /** Send a breadcrumb log message that appears alongside any subsequent crash report. */
    fun log(message: String)

    /** Record a non-fatal exception so it appears in the Crashlytics dashboard. */
    fun recordException(throwable: Throwable)

    /** Associate all future reports with this user / device identifier. */
    fun setUserId(userId: String)
}

