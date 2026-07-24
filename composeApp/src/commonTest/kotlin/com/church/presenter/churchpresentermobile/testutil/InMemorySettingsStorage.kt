package com.church.presenter.churchpresentermobile.testutil

import com.church.presenter.churchpresentermobile.model.SettingsStorage

/**
 * In-memory [SettingsStorage] for tests — no platform persistence, fully isolated
 * per instance, so [com.church.presenter.churchpresentermobile.model.AppSettings]
 * (and everything that depends on it) can be exercised headlessly and deterministically.
 */
class InMemorySettingsStorage(
    initialStrings: Map<String, String> = emptyMap(),
    initialInts: Map<String, Int> = emptyMap(),
) : SettingsStorage {
    private val strings = initialStrings.toMutableMap()
    private val ints = initialInts.toMutableMap()

    override fun getString(key: String, defaultValue: String): String = strings[key] ?: defaultValue
    override fun putString(key: String, value: String) { strings[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = ints[key] ?: defaultValue
    override fun putInt(key: String, value: Int) { ints[key] = value }
}
