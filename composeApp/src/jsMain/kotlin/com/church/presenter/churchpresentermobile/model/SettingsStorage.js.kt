package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants
import kotlinx.browser.localStorage

actual fun resolveDefaultHost(): String = ApiConstants.DEFAULT_HOST

/**
 * JS implementation of [SettingsStorage] backed by the browser's [localStorage].
 */
class JsSettingsStorage : SettingsStorage {
    override fun getString(key: String, defaultValue: String): String =
        localStorage.getItem(key) ?: defaultValue

    override fun putString(key: String, value: String) {
        localStorage.setItem(key, value)
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        localStorage.getItem(key)?.toIntOrNull() ?: defaultValue

    override fun putInt(key: String, value: Int) {
        localStorage.setItem(key, value.toString())
    }
}

actual fun createSettingsStorage(): SettingsStorage = JsSettingsStorage()
