package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import com.church.presenter.churchpresentermobile.DeepLinkHandler
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DesktopAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The address of the computer this phone copies content from.
 *
 * Used where there is no Save button — the sync sheet — so it writes through as it is edited
 * rather than holding a draft. Both [com.church.presenter.churchpresentermobile.network.SongService]
 * and [com.church.presenter.churchpresentermobile.network.BibleDownloadService] build their URL
 * from `settings.apiBaseUrl` per call, so the very next tap on Copy uses whatever is on screen.
 *
 * The port is written only once it parses, so half-typed "87" does not briefly point the app at
 * port 87 — but the field keeps showing what was typed either way.
 */
class DesktopAddressViewModel(
    private val settings: AppSettings,
) : ViewModel() {

    private val _host = MutableStateFlow(settings.host)
    val host = _host.asStateFlow()

    private val _port = MutableStateFlow(settings.port.toString())
    val port = _port.asStateFlow()

    private val _apiKey = MutableStateFlow(settings.apiKey)
    val apiKey = _apiKey.asStateFlow()

    /**
     * True once something has answered with "who are you?" — the sheet reveals the key field on
     * this rather than showing it to everyone, since most desktops have no key set.
     */
    private val _keyRequired = MutableStateFlow(settings.apiKey.isNotBlank())
    val keyRequired = _keyRequired.asStateFlow()

    private val _portError = MutableStateFlow(false)
    val portError = _portError.asStateFlow()

    private val _hostError = MutableStateFlow(false)

    /** True when what is typed cannot be a host — the twin of [portError]. */
    val hostError = _hostError.asStateFlow()

    fun setHost(value: String) {
        _host.value = value
        // Written through only when it could actually be a host. This surface has no Save
        // button, so without the check every keystroke of an unusable address reached the
        // HTTP client and the WebSocket's reconnect loop, which retried it forever.
        val cleaned = DesktopAddress.normalizeHost(value)
        val usable = DesktopAddress.isUsableHost(cleaned)
        _hostError.value = cleaned.isNotBlank() && !usable
        if (usable) settings.host = cleaned
    }

    fun setPort(value: String) {
        _port.value = value
        val parsed = value.trim().toIntOrNull()
        val valid = parsed != null && parsed in 1..65535
        _portError.value = value.isNotBlank() && !valid
        if (valid) settings.port = parsed!!
    }

    fun setApiKey(value: String) {
        _apiKey.value = value
        settings.apiKey = value.trim()
    }

    /** Shows the key field, after something answered 401. */
    fun revealKeyField() {
        _keyRequired.value = true
    }

    /** Applies a scanned `churchpresenter://connect` URL, which carries all three at once. */
    fun applyScannedUrl(url: String) {
        if (DeepLinkHandler.handle(url, settings)) reloadFromStorage()
    }

    /** Picks up an address changed elsewhere — the Settings sheet, or a scanned code. */
    fun reloadFromStorage() {
        _host.value = settings.host
        _port.value = settings.port.toString()
        _apiKey.value = settings.apiKey
        _portError.value = false
        _hostError.value = false
        if (settings.apiKey.isNotBlank()) _keyRequired.value = true
    }

    /** Saves through the shared validator, for surfaces that do have a Save button. */
    fun save(): DesktopAddress.Outcome =
        DesktopAddress.save(settings, _host.value, _port.value, _apiKey.value)
}
