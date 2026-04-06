package com.church.presenter.churchpresentermobile.util

/**
 * iOS stub — Firebase Remote Config is available on iOS after FirebaseApp.configure()
 * is called in iOSApp.swift. Wire up a Swift helper if you need Kotlin to read
 * Remote Config values directly; for now values fall back to [default].
 */
actual object RemoteConfig {

    private val store = mutableMapOf<String, Any>()

    actual fun init(defaults: Map<String, Any>, fetchIntervalSeconds: Long) {
        store.putAll(defaults)
    }

    actual fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        // Remote Config is managed by the Swift layer on iOS.
        onComplete(false)
    }

    actual fun getString(key: String, default: String): String =
        (store[key] as? String) ?: default

    actual fun getBoolean(key: String, default: Boolean): Boolean =
        (store[key] as? Boolean) ?: default

    actual fun getLong(key: String, default: Long): Long =
        (store[key] as? Long) ?: default
}

