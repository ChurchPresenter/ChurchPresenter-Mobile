package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.deviceName
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
private const val KEY_DISPLAY_NAME     = "display_name"
private const val KEY_DEVICE_NAME      = "device_name"
private const val KEY_SAVED_ANNOUNCEMENTS = "saved_announcements"
private const val KEY_SAVED_BOOKMARKS = "saved_bookmarks"
private const val KEY_FCM_TOKEN        = "fcm_token"
private const val KEY_APP_OPEN_COUNT   = "app_open_count"
private const val KEY_SETUP_COMPLETE   = "setup_complete"
private const val KEY_CONNECT_SETUP    = "connect_setup_done"
private const val KEY_TELEMETRY_ENABLED = "telemetry_enabled"
private const val KEY_APP_MODE         = "app_mode"
private const val KEY_MODE_CHOSEN      = "mode_chosen"
private const val KEY_STANDALONE_PORT  = "standalone_port"
private const val KEY_LIBRARY_SYNC      = "library_sync_state"

/**
 * Increment this whenever DEFAULT_HOST or DEFAULT_PORT changes.
 * Causes stored host/port to be reset to the platform-appropriate defaults on next launch.
 */
private const val CURRENT_SETTINGS_VERSION = 5

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
class AppSettings(
    private val storage: SettingsStorage = createSettingsStorage(),
) {
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

    /** JSON array of the user's saved announcements (composer presets). Defaults to "[]". */
    var savedAnnouncementsJson: String
        get() = storage.getString(KEY_SAVED_ANNOUNCEMENTS, "[]")
        set(value) { storage.putString(KEY_SAVED_ANNOUNCEMENTS, value) }

    var savedBookmarksJson: String
        get() = storage.getString(KEY_SAVED_BOOKMARKS, "[]")
        set(value) { storage.putString(KEY_SAVED_BOOKMARKS, value) }

    /** The user's preferred colour scheme. Defaults to [ThemeMode.SYSTEM]. */
    var themeMode: ThemeMode
        get() = storage.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            .let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM }
        set(value) { storage.putString(KEY_THEME_MODE, value.name) }

    /**
     * The person's name, sent as the author when submitting Q&A questions.
     *
     * Not the same thing as [customDeviceName]: the desktop shows a question's
     * author and the device it came from on separate lines, so "Sound desk" is
     * the right answer to one and the wrong answer to the other. Blank until
     * asked for; the caller then falls back down [reportedDeviceName] to
     * [deviceId] so a question is never unattributed.
     */
    var displayName: String
        get() = storage.getString(KEY_DISPLAY_NAME, "")
        set(value) { storage.putString(KEY_DISPLAY_NAME, value) }

    /**
     * The operator's own name for this device, overriding the one the OS gives.
     *
     * Typed precisely because the OS name was blank, unhelpful ("iPhone"), or
     * the wrong thing to call this device in this building.
     */
    var customDeviceName: String
        get() = storage.getString(KEY_DEVICE_NAME, "")
        set(value) { storage.putString(KEY_DEVICE_NAME, value) }

    /**
     * The name this device reports to a desktop, sent as
     * [ApiConstants.DEVICE_NAME_HEADER] so an operator approving a connection
     * reads "Pixel 7 Pro" or "Sound desk" rather than a UUID.
     *
     * [customDeviceName] wins when set. Blank when neither exists — a browser
     * with no custom name — and the caller then sends nothing at all rather
     * than an empty header.
     */
    val reportedDeviceName: String
        get() = customDeviceName.trim().ifBlank { deviceName().trim() }

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
     * True once the user has completed (or skipped) the first-launch
     * connection QR-scan setup flow.
     */
    var isConnectSetupDone: Boolean
        get() = storage.getInt(KEY_CONNECT_SETUP, 0) == 1
        set(value) { storage.putInt(KEY_CONNECT_SETUP, if (value) 1 else 0) }

    /**
     * True when the user allows sending usage analytics and crash reports
     * (Firebase Analytics, Firebase Crashlytics, Sentry). Defaults to enabled;
     * the "telemetry_enabled" key is also read directly from native storage by
     * the iOS Swift startup code so Sentry can be gated before Kotlin runs —
     * keep the key name in sync with [iOSApp.swift]'s `AppConstants.telemetryEnabledKey`.
     */
    var isTelemetryEnabled: Boolean
        get() = storage.getInt(KEY_TELEMETRY_ENABLED, 1) == 1
        set(value) { storage.putInt(KEY_TELEMETRY_ENABLED, if (value) 1 else 0) }

    /**
     * How the app projects — remote control of a desktop, or standalone presenter.
     *
     * Reads coerce to [AppMode.REMOTE] on platforms that cannot present
     * ([supportsStandalone] is false), so a settings blob written on a phone can
     * never leave the web build in a mode it has no sink for. Writes are
     * coerced the same way, so the stored value never disagrees with what the
     * app is actually doing.
     */
    var appMode: AppMode
        get() {
            if (!supportsStandalone) return AppMode.REMOTE
            val stored = storage.getString(KEY_APP_MODE, AppMode.REMOTE.name)
            return AppMode.entries.firstOrNull { it.name == stored } ?: AppMode.REMOTE
        }
        set(value) {
            val effective = if (supportsStandalone) value else AppMode.REMOTE
            storage.putString(KEY_APP_MODE, effective.name)
        }

    /**
     * True once the user has picked a mode on the setup screen. Existing
     * installs default to false and are shown the picker on next launch —
     * harmless, since [appMode] already defaults to their current behaviour.
     */
    var isModeChosen: Boolean
        get() = storage.getInt(KEY_MODE_CHOSEN, 0) == 1
        set(value) { storage.putInt(KEY_MODE_CHOSEN, if (value) 1 else 0) }

    /**
     * Port the standalone presentation web server last bound successfully.
     * Remembered so the URL handed to a TV stays stable between services.
     */
    var standalonePort: Int
        get() = storage.getInt(KEY_STANDALONE_PORT, ApiConstants.STANDALONE_HTTP_PORT_DEFAULT)
        set(value) { storage.putInt(KEY_STANDALONE_PORT, value) }

    /**
     * JSON blob describing the last desktop→library sync, so the Library screen
     * can answer "is this current?" without touching the network. Same
     * persistence pattern as [savedAnnouncementsJson]. Defaults to "{}".
     */
    var librarySyncStateJson: String
        get() = storage.getString(KEY_LIBRARY_SYNC, "{}")
        set(value) { storage.putString(KEY_LIBRARY_SYNC, value) }

    /** Builds the full HTTP API base URL from the current host and port. */
    val apiBaseUrl: String
        get() = "http://$host:$port/api"

    /** Builds the plain WebSocket URL for the server push connection. */
    val wsBaseUrl: String
        get() = "ws://$host:$port/ws"
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
