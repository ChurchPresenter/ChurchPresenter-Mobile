@file:OptIn(ExperimentalWasmJsInterop::class)

package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants

actual fun resolveDefaultHost(): String = ApiConstants.DEFAULT_HOST

/**
 * WasmJS implementation of [SettingsStorage] backed by the browser's localStorage
 * via JS interop.
 */
class WasmJsSettingsStorage : SettingsStorage {
    override fun getString(key: String, defaultValue: String): String =
        getLocalStorageItem(key) ?: defaultValue

    override fun putString(key: String, value: String) {
        setLocalStorageItem(key, value)
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        getLocalStorageItem(key)?.toIntOrNull() ?: defaultValue

    override fun putInt(key: String, value: Int) {
        setLocalStorageItem(key, value.toString())
    }
}

@JsFun("(key) => localStorage.getItem(key)")
private external fun getLocalStorageItem(key: String): String?

@JsFun("(key, value) => localStorage.setItem(key, value)")
private external fun setLocalStorageItem(key: String, value: String)

actual fun createSettingsStorage(): SettingsStorage = WasmJsSettingsStorage()
