package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants

actual fun resolveDefaultHost(): String = ApiConstants.DEFAULT_HOST

/** In-memory settings — a test host has nothing to persist between runs. */
class JvmSettingsStorage : SettingsStorage {
    private val strings = mutableMapOf<String, String>()
    private val ints = mutableMapOf<String, Int>()

    override fun getString(key: String, defaultValue: String): String = strings[key] ?: defaultValue
    override fun putString(key: String, value: String) { strings[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue
    override fun putInt(key: String, value: Int) { ints[key] = value }
}

actual fun createSettingsStorage(): SettingsStorage = JvmSettingsStorage()
