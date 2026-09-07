package com.church.presenter.churchpresentermobile.testutil

import android.content.SharedPreferences

/**
 * An in-memory [SharedPreferences], so code that persists through Android's
 * preference store can be exercised on the unit-test JVM.
 *
 * The stub `android.jar` unit tests compile against has no working
 * implementation — every method is a no-op returning a default — so a fake is
 * the only way to see what was written without an emulator or Robolectric
 * (which this project does not use; see AGENT.md).
 *
 * Writes land immediately on both `apply()` and `commit()`. Nothing here is
 * asynchronous, so a test can read straight back after the call it is checking.
 */
class FakePreferences : SharedPreferences {

    /** What has been written so far, for a test to assert against. */
    val values: MutableMap<String, Any?> = mutableMapOf()

    override fun getAll(): MutableMap<String, *> = values

    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST")
        (values[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var cleared = false

        override fun putString(key: String?, value: String?) = put(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values)
        override fun putInt(key: String?, value: Int) = put(key, value)
        override fun putLong(key: String?, value: Long) = put(key, value)
        override fun putFloat(key: String?, value: Float) = put(key, value)
        override fun putBoolean(key: String?, value: Boolean) = put(key, value)

        override fun remove(key: String?): SharedPreferences.Editor {
            key?.let { removed += it }
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            cleared = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (cleared) values.clear()
            removed.forEach { values.remove(it) }
            values.putAll(pending)
        }

        private fun put(key: String?, value: Any?): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }
    }
}
