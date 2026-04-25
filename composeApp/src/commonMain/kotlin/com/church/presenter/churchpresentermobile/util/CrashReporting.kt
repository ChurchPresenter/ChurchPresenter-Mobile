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

    /**
     * Attach a key-value pair to all subsequent Crashlytics reports in this session.
     * Useful for pinning the server URL, last operation, or error type so every
     * non-fatal report carries that context automatically.
     */
    fun setCustomKey(key: String, value: String)
}

