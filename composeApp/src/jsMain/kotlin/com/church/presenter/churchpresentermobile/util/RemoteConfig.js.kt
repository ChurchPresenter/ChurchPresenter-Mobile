package com.church.presenter.churchpresentermobile.util

actual object RemoteConfig {
    private val store = mutableMapOf<String, Any>()
    actual fun init(defaults: Map<String, Any>, fetchIntervalSeconds: Long) { store.putAll(defaults) }
    actual fun fetchAndActivate(onComplete: (Boolean) -> Unit) { onComplete(false) }
    actual fun getString(key: String, default: String): String = (store[key] as? String) ?: default
    actual fun getBoolean(key: String, default: Boolean): Boolean = (store[key] as? Boolean) ?: default
    actual fun getLong(key: String, default: Long): Long = (store[key] as? Long) ?: default
}

