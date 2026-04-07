package com.church.presenter.churchpresentermobile.util

/**
 * iOS implementation of the Remote Config bridge.
 *
 * Firebase Remote Config is owned by the Swift layer (FirebaseApp.configure() +
 * fetchAndActivate in iOSApp.swift).  After a successful fetch Swift calls
 * [applyValues] with all key→string pairs, which updates the in-memory store and
 * fires the pending [fetchAndActivate] callback so Kotlin reacts just like Android.
 *
 * The Kotlin object is exposed to Swift/ObjC as "KotlinRemoteConfig" to avoid a
 * name clash with Firebase's own RemoteConfig class.
 */
@OptIn(kotlin.experimental.ExperimentalObjCName::class)
@ObjCName("KotlinRemoteConfig", swiftName = "KotlinRemoteConfig")
actual object RemoteConfig {

    private val store = mutableMapOf<String, Any>()

    /** Set when [fetchAndActivate] is called; fired once [applyValues] arrives. */
    private var pendingCallback: ((Boolean) -> Unit)? = null

    /**
     * True once [applyValues] has been called at least once.
     * Guards the race where Swift completes the fetch before Kotlin calls
     * [fetchAndActivate] (e.g. very fast network on first launch).
     */
    private var valuesApplied = false

    actual fun init(defaults: Map<String, Any>, fetchIntervalSeconds: Long) {
        store.putAll(defaults)
    }

    /**
     * Stores the callback until Swift delivers the fetched values via [applyValues].
     * If Swift already delivered values before this was called, fires immediately.
     */
    actual fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        if (valuesApplied) {
            onComplete(true)
        } else {
            pendingCallback = onComplete
        }
    }

    /**
     * Called from Swift (iOSApp.swift) on the main thread after Firebase Remote Config
     * fetchAndActivate completes.  All values arrive as strings — Firebase iOS SDK's
     * native representation — and are stored verbatim; [getBoolean] / [getLong] parse
     * them on read.
     *
     * Swift usage:
     *   KotlinRemoteConfig.shared.applyValues(values: ["key": "value", ...])
     */
    fun applyValues(values: Map<String, String>) {
        store.putAll(values)
        valuesApplied = true
        val cb = pendingCallback
        pendingCallback = null
        cb?.invoke(true)
        Logger.d("RemoteConfig", "iOS values applied — ${values.keys}")
    }

    actual fun getString(key: String, default: String): String =
        (store[key] as? String)?.ifEmpty { default } ?: default

    /** Parses both native Boolean and Firebase string values ("true"/"false"/"1"/"0"). */
    actual fun getBoolean(key: String, default: Boolean): Boolean {
        val v = store[key] ?: return default
        return when (v) {
            is Boolean -> v
            is String  -> v.lowercase() == "true" || v == "1"
            else       -> default
        }
    }

    /** Parses both native Long and Firebase string values ("3600", etc.). */
    actual fun getLong(key: String, default: Long): Long {
        val v = store[key] ?: return default
        return when (v) {
            is Long   -> v
            is String -> v.toLongOrNull() ?: default
            else      -> default
        }
    }
}
