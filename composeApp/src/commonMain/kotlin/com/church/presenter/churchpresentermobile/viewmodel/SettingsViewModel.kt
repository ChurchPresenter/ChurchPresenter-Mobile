package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.util.Analytics
import com.church.presenter.churchpresentermobile.util.CrashReporting
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val TAG = "SettingsViewModel"

/**
 * Manages the settings UI state and persists changes to [AppSettings].
 *
 * @param appSettings The shared [AppSettings] instance.
 */
class SettingsViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val _host = MutableStateFlow(appSettings.host)
    /** Current host / IP address field value. */
    val host = _host.asStateFlow()

    private val _port = MutableStateFlow(appSettings.port.toString())
    /** Current port field value (as a string so the text field can hold partial input). */
    val port = _port.asStateFlow()

    private val _apiKey = MutableStateFlow(appSettings.apiKey)
    /** Current API key field value. */
    val apiKey = _apiKey.asStateFlow()

    private val _displayName = MutableStateFlow(appSettings.displayName)
    /** Current display name field value. */
    val displayName = _displayName.asStateFlow()

    private val _activeUrl = MutableStateFlow(appSettings.apiBaseUrl)
    /** The URL currently saved in storage — what the app is actually using right now. */
    val activeUrl = _activeUrl.asStateFlow()

    /**
     * Draft API base URL built reactively from the current [host] and [port] fields.
     * Mirrors the formula in [AppSettings.apiBaseUrl] but uses the unsaved draft values.
     */
    val draftBaseUrl: StateFlow<String> = combine(_host, _port) { h, p ->
        "http://${h.trim()}:${p.trim()}/api"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "http://${appSettings.host}:${appSettings.port}/api"
    )

    /**
     * True when the draft server address ([draftBaseUrl]) differs from the currently
     * active URL — i.e. saving would change which server the app connects to.
     */
    val urlChanged: StateFlow<Boolean> = combine(draftBaseUrl, _activeUrl) { draft, active ->
        draft != active
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    private val _themeMode = MutableStateFlow(appSettings.themeMode)
    /** The currently selected [ThemeMode] option. */
    val themeMode = _themeMode.asStateFlow()

    private val _telemetryEnabled = MutableStateFlow(appSettings.isTelemetryEnabled)
    /** Whether usage analytics and crash reports are currently being shared. */
    val telemetryEnabled = _telemetryEnabled.asStateFlow()

    init {
        Logger.d(TAG, "SettingsViewModel init — loaded from storage: host=${appSettings.host} port=${appSettings.port} url=${appSettings.apiBaseUrl}")
    }

    private val _hostError = MutableStateFlow<String?>(null)
    /** Validation error for the host field, or null when valid. */
    val hostError = _hostError.asStateFlow()

    private val _portError = MutableStateFlow<String?>(null)
    /** Validation error for the port field, or null when valid. */
    val portError = _portError.asStateFlow()

    /**
     * Updates the host field value.
     *
     * @param value The new host string.
     */
    fun setHost(value: String) {
        _host.value = value
        _hostError.value = null
    }

    /**
     * Updates the port field value.
     *
     * @param value The new port string (may be partial/invalid during typing).
     */
    fun setPort(value: String) {
        _port.value = value
        _portError.value = null
    }

    /**
     * Updates the API key field value.
     *
     * @param value The new API key string. Empty string means no key.
     */
    fun setApiKey(value: String) {
        _apiKey.value = value
    }

    fun setDisplayName(value: String) {
        _displayName.value = value
    }

    /**
     * Updates the theme mode selection.
     *
     * @param value The new [ThemeMode].
     */
    fun setThemeMode(value: ThemeMode) {
        _themeMode.value = value
    }

    /**
     * Updates the telemetry (analytics + crash reporting) privacy preference.
     * Unlike the other fields, this applies and persists immediately rather
     * than waiting for [save] — there's no reason to require a Save tap for
     * turning tracking on/off.
     *
     * @param enabled True to share usage analytics and crash reports.
     */
    fun setTelemetryEnabled(enabled: Boolean) {
        _telemetryEnabled.value = enabled
        appSettings.isTelemetryEnabled = enabled
        CrashReporting.setEnabled(enabled)
        Analytics.setEnabled(enabled)
        Logger.d(TAG, "setTelemetryEnabled — $enabled")
    }

    /**
     * Validates all fields and, if valid, persists the settings.
     *
     * @param onSuccess Called when settings are saved successfully.
     * @param emptyHostError Localised error string for an empty host.
     * @param invalidPortError Localised error string for an invalid port.
     */
    fun save(
        onSuccess: () -> Unit,
        emptyHostError: String,
        invalidPortError: String
    ) {
        var valid = true

        if (_host.value.isBlank()) {
            _hostError.value = emptyHostError
            valid = false
        }

        val portInt = _port.value.trim().toIntOrNull()
        if (portInt == null || portInt !in 1..65535) {
            _portError.value = invalidPortError
            valid = false
        }

        if (!valid) return

        appSettings.host = _host.value.trim()
        appSettings.port = portInt!!
        appSettings.apiKey = _apiKey.value.trim()
        appSettings.displayName = _displayName.value.trim()
        appSettings.themeMode = _themeMode.value
        _activeUrl.value = appSettings.apiBaseUrl
        Logger.d(TAG, "save — persisted host=${appSettings.host} port=${appSettings.port} url=${appSettings.apiBaseUrl} themeMode=${appSettings.themeMode}")
        onSuccess()
    }

    /**
     * Resets the draft fields to the built-in default values from [ApiConstants].
     * Does not save — the user still needs to tap Save.
     */
    fun resetToDefaults() {
        _host.value = ApiConstants.DEFAULT_HOST
        _port.value = ApiConstants.DEFAULT_PORT.toString()
        _hostError.value = null
        _portError.value = null
    }

    /** Resets the draft fields back to the last saved values. */
    fun cancel() {
        _host.value = appSettings.host
        _port.value = appSettings.port.toString()
        _apiKey.value = appSettings.apiKey
        _displayName.value = appSettings.displayName
        _themeMode.value = appSettings.themeMode
        _hostError.value = null
        _portError.value = null
    }

    /**
     * Reloads all draft fields from persistent storage.
     * Called when an external event (e.g. a QR-code deep link) writes new settings
     * to [appSettings] behind the ViewModel's back, so the UI stays in sync.
     */
    fun reloadFromStorage() = cancel()
}
