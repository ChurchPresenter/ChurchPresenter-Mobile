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

/** Returns the stored application [Context], or null if [initSettingsContext] hasn't been called yet. */
fun getAppContext(): Context? = appContext

/**
 * Returns true when the app is running inside an Android emulator.
 * The emulator cannot reach the host machine via the LAN IP — it must use 10.0.2.2.
 */
fun isRunningOnEmulator(): Boolean = looksLikeEmulator(
    fingerprint = Build.FINGERPRINT,
    model = Build.MODEL,
    manufacturer = Build.MANUFACTURER,
    brand = Build.BRAND,
    device = Build.DEVICE,
    product = Build.PRODUCT,
    hardware = Build.HARDWARE,
)

/**
 * The emulator test itself, over the build identifiers rather than over
 * [Build] — `internal` so it can be exercised without a device, since
 * `android.os.Build`'s fields are static finals a unit test cannot set.
 *
 * Every parameter is nullable because they are all null on a plain JVM, and a
 * missing identifier is evidence of nothing rather than evidence of an
 * emulator. Getting a false positive here points the app at 10.0.2.2, which no
 * real phone can route to — the whole Songs tab then times out.
 */
@Suppress("LongParameterList", "CyclomaticComplexMethod")
internal fun looksLikeEmulator(
    fingerprint: String?,
    model: String?,
    manufacturer: String?,
    brand: String?,
    device: String?,
    product: String?,
    hardware: String?,
): Boolean =
    fingerprint?.startsWith("generic") == true ||
    fingerprint?.startsWith("unknown") == true ||
    model?.contains("google_sdk") == true ||
    model?.contains("Emulator") == true ||
    model?.contains("Android SDK built for") == true ||
    manufacturer?.contains("Genymotion") == true ||
    brand?.startsWith("generic") == true ||
    device?.startsWith("generic") == true ||
    product?.contains("sdk_gphone") == true ||
    product?.contains("vbox86p") == true ||
    hardware == "goldfish" ||
    hardware == "ranchu"

/**
 * On Android: returns 10.0.2.2 when running in the emulator (routes to host machine),
 * otherwise returns the real-device LAN IP [ApiConstants.DEFAULT_HOST].
 */
actual fun resolveDefaultHost(): String {
    // Guard against unit-test / non-device JVMs where android.os.Build fields are null.
    val onEmulator = try { isRunningOnEmulator() } catch (_: Throwable) { false }
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
