package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.generateUUID
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.util.Logger

private const val TAG = "AppSettings"

private const val KEY_HOST = "server_host"
private const val KEY_PORT = "server_port"
private const val KEY_API_KEY = "api_key"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_SETTINGS_VERSION = "settings_version"
private const val KEY_DEVICE_ID = "device_id"
private const val KEY_FCM_TOKEN        = "fcm_token"
private const val KEY_APP_OPEN_COUNT   = "app_open_count"
private const val KEY_SETUP_COMPLETE   = "setup_complete"
private const val KEY_CERT_TRUSTED     = "cert_trusted"

/**
 * Increment this whenever DEFAULT_HOST or DEFAULT_PORT changes.
 * Causes stored host/port to be reset to the platform-appropriate defaults on next launch.
 */
private const val CURRENT_SETTINGS_VERSION = 4

/**
 * Returns the appropriate default host for this platform/environment.
 * On the Android emulator this is 10.0.2.2 (the host machine).
 * On real devices and other platforms this is [ApiConstants.DEFAULT_HOST].
 */
expect fun resolveDefaultHost(): String

/**
 * Persists and reads server connection settings using platform-native storage.
 * Uses [expect/actual] so each platform provides its own storage mechanism.
 */
class AppSettings {
    private val storage: SettingsStorage = createSettingsStorage()
    private val defaultHost = resolveDefaultHost()

    init {
        migrateIfNeeded()
    }

    /**
     * If the stored settings version is older than [CURRENT_SETTINGS_VERSION],
     * reset host and port to the current platform defaults. API key is preserved.
     */
    private fun migrateIfNeeded() {
        val storedVersion = storage.getInt(KEY_SETTINGS_VERSION, 0)
        if (storedVersion < CURRENT_SETTINGS_VERSION) {
            Logger.d(TAG, "Migrating settings from version $storedVersion to $CURRENT_SETTINGS_VERSION — resetting host=$defaultHost port=${ApiConstants.DEFAULT_PORT}")
            storage.putString(KEY_HOST, defaultHost)
            storage.putInt(KEY_PORT, ApiConstants.DEFAULT_PORT)
            storage.putInt(KEY_SETTINGS_VERSION, CURRENT_SETTINGS_VERSION)
            Logger.d(TAG, "Migration complete — host=$defaultHost port=${ApiConstants.DEFAULT_PORT}")
        } else {
            Logger.d(TAG, "Settings version $storedVersion is current — host=${storage.getString(KEY_HOST, defaultHost)} port=${storage.getInt(KEY_PORT, ApiConstants.DEFAULT_PORT)}")
        }
    }

    /** The server host / IP address. */
    var host: String
        get() = storage.getString(KEY_HOST, defaultHost)
        set(value) {
            // Colons belong in the port, not the host — strip any that sneak in
            val sanitized = value.trim().replace(":", ".")
            if (sanitized != value.trim()) {
                Logger.e(TAG, "Host value '$value' contained colons — sanitized to '$sanitized'")
            }
            storage.putString(KEY_HOST, sanitized)
        }

    /** The server port number. */
    var port: Int
        get() = storage.getInt(KEY_PORT, ApiConstants.DEFAULT_PORT)
        set(value) { storage.putInt(KEY_PORT, value) }

    /** The optional API key sent as the [ApiConstants.API_KEY_HEADER] header. Empty string means no key. */
    var apiKey: String
        get() = storage.getString(KEY_API_KEY, "")
        set(value) { storage.putString(KEY_API_KEY, value) }

    /**
     * A stable unique identifier for this app installation.
     * Generated once via [generateUUID] and persisted across launches.
     * Sent as [ApiConstants.DEVICE_ID_HEADER] on every POST request.
     */
    val deviceId: String
        get() {
            var id = storage.getString(KEY_DEVICE_ID, "")
            if (id.isBlank()) {
                id = generateUUID()
                storage.putString(KEY_DEVICE_ID, id)
                Logger.d(TAG, "deviceId — generated new UUID: $id")
            }
            return id
        }

    /** The user's preferred colour scheme. Defaults to [ThemeMode.SYSTEM]. */
    var themeMode: ThemeMode
        get() = storage.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            .let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM }
        set(value) { storage.putString(KEY_THEME_MODE, value.name) }

    /**
     * FCM (Firebase Cloud Messaging) registration token for this device.
     * Written by [FirebasePushService.onNewToken] on Android and the
     * MessagingDelegate in iOSApp.swift on iOS. Empty when not yet received.
     */
    var fcmToken: String
        get() = storage.getString(KEY_FCM_TOKEN, "")
        set(value) { storage.putString(KEY_FCM_TOKEN, value) }

    /**
     * Number of times the app has been opened. Incremented in MainActivity on
     * Android and AppDelegate on iOS. Used to decide when to show the in-app
     * review prompt (3rd, 10th, then every 20th launch).
     */
    var appOpenCount: Int
        get() = storage.getInt(KEY_APP_OPEN_COUNT, 0)
        set(value) { storage.putInt(KEY_APP_OPEN_COUNT, value) }

    /**
     * True once the user has dismissed the first-launch settings screen at least once
     * (either by saving or cancelling). Used so that [App] automatically opens the
     * settings screen on the very first launch to prompt for the server IP and port.
     */
    var isSetupComplete: Boolean
        get() = storage.getInt(KEY_SETUP_COMPLETE, 0) == 1
        set(value) { storage.putInt(KEY_SETUP_COMPLETE, if (value) 1 else 0) }

    /**
     * True once the user has completed (or explicitly skipped) the first-launch
     * certificate trust setup flow. When false, [App] will show [CertSetupScreen]
     * after the initial settings save so the user can install the CA certificate.
     */
    var isCertTrusted: Boolean
        get() = storage.getInt(KEY_CERT_TRUSTED, 0) == 1
        set(value) { storage.putInt(KEY_CERT_TRUSTED, if (value) 1 else 0) }

    /** Builds the full HTTPS API base URL from the current host and port. */
    val apiBaseUrl: String
        get() = "https://$host:$port/api"
}

/**
 * Platform-agnostic key/value storage interface backed by native persistence.
 */
interface SettingsStorage {
    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
}

/** Creates the platform-specific [SettingsStorage] implementation. */
expect fun createSettingsStorage(): SettingsStorage
