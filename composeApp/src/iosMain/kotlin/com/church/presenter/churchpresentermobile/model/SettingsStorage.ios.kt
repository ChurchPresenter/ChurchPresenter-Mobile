package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants
import platform.Foundation.NSUserDefaults

/** iOS always connects to the real device LAN IP — no emulator routing needed. */
actual fun resolveDefaultHost(): String = ApiConstants.DEFAULT_HOST

/**
 * iOS implementation of [SettingsStorage] backed by [NSUserDefaults].
 */
class IosSettingsStorage : SettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String, defaultValue: String): String =
        defaults.stringForKey(key) ?: defaultValue

    override fun putString(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return if (defaults.objectForKey(key) != null) {
            defaults.integerForKey(key).toInt()
        } else {
            defaultValue
        }
    }

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), key)
    }
}

actual fun createSettingsStorage(): SettingsStorage = IosSettingsStorage()
