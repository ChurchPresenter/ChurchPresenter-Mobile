package com.church.presenter.churchpresentermobile.model

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.util.Logger

private const val TAG = "SettingsStorage"
private var appContext: Context? = null

/**
 * Must be called once from [MainActivity.onCreate] before any [AppSettings] is created.
 *
 * @param context The application context.
 */
fun initSettingsContext(context: Context) {
    appContext = context.applicationContext
}

/**
 * Returns true when the app is running inside an Android emulator.
 * The emulator cannot reach the host machine via the LAN IP — it must use 10.0.2.2.
 */
fun isRunningOnEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
    Build.FINGERPRINT.startsWith("unknown") ||
    Build.MODEL.contains("google_sdk") ||
    Build.MODEL.contains("Emulator") ||
    Build.MODEL.contains("Android SDK built for") ||
    Build.MANUFACTURER.contains("Genymotion") ||
    Build.BRAND.startsWith("generic") ||
    Build.DEVICE.startsWith("generic") ||
    Build.PRODUCT.contains("sdk_gphone") ||
    Build.PRODUCT.contains("vbox86p") ||
    Build.HARDWARE == "goldfish" ||
    Build.HARDWARE == "ranchu"

/**
 * On Android: returns 10.0.2.2 when running in the emulator (routes to host machine),
 * otherwise returns the real-device LAN IP [ApiConstants.DEFAULT_HOST].
 */
actual fun resolveDefaultHost(): String {
    val onEmulator = isRunningOnEmulator()
    val host = if (onEmulator) ApiConstants.EMULATOR_HOST else ApiConstants.DEFAULT_HOST
    Logger.d(TAG, "resolveDefaultHost — emulator=$onEmulator → host=$host")
    return host
}

/**
 * Android implementation of [SettingsStorage] backed by [SharedPreferences].
 */
class AndroidSettingsStorage : SettingsStorage {
    private val prefs: SharedPreferences =
        requireNotNull(appContext) {
            "initSettingsContext() must be called from MainActivity before using AppSettings"
        }.getSharedPreferences("church_presenter_prefs", Context.MODE_PRIVATE)

    override fun getString(key: String, defaultValue: String): String =
        prefs.getString(key, defaultValue) ?: defaultValue

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        prefs.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }
}

actual fun createSettingsStorage(): SettingsStorage = AndroidSettingsStorage()
