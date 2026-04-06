package com.church.presenter.churchpresentermobile.util

/**
 * Multiplatform Remote Config abstraction.
 *
 * Android  → Firebase Remote Config
 * iOS      → Firebase Remote Config (initialised from Swift via FirebaseApp.configure())
 * JS/WASM  → in-memory no-op stubs
 *
 * Typical usage:
 *   RemoteConfig.init()                         // call once at startup
 *   RemoteConfig.fetchAndActivate { activated ->
 *       val featureEnabled = RemoteConfig.getBoolean("feature_x")
 *   }
 */
expect object RemoteConfig {
    /**
     * Set default values and configure the minimum fetch interval.
     * Call once at app startup before any [getString]/[getBoolean]/[getLong] calls.
     *
     * @param defaults  Map of parameter key → default value (String, Boolean, or Long).
     * @param fetchIntervalSeconds  Minimum time between fetches in seconds.
     *                              Use 0 during development, 3600 (1 h) for production.
     */
    fun init(defaults: Map<String, Any> = emptyMap(), fetchIntervalSeconds: Long = 3600)

    /**
     * Fetch the latest values from the server and immediately activate them.
     * [onComplete] is called on the main thread with `true` if new values were activated.
     */
    fun fetchAndActivate(onComplete: (activated: Boolean) -> Unit)

    /** Returns the remote String value for [key], or [default] if not set. */
    fun getString(key: String, default: String = ""): String

    /** Returns the remote Boolean value for [key], or [default] if not set. */
    fun getBoolean(key: String, default: Boolean = false): Boolean

    /** Returns the remote Long value for [key], or [default] if not set. */
    fun getLong(key: String, default: Long = 0L): Long
}

