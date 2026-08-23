package com.church.presenter.churchpresentermobile.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the computer is, and the one place that decides whether an address is usable.
 *
 * Two surfaces now write it — Settings, and the sync sheet in the Library tab — and they must
 * not drift on what counts as a valid port. Both go through [save].
 */
object DesktopAddress {

    /** Result of trying to save an address. */
    sealed interface Outcome {
        data object Saved : Outcome
        data class Invalid(val hostBlank: Boolean, val portInvalid: Boolean) : Outcome
    }

    private val _changeCount = MutableStateFlow(0)

    /**
     * Incremented on every successful save, so the other surface reloads rather than showing a
     * stale draft. The twin of [com.church.presenter.churchpresentermobile.DeepLinkHandler.appliedCount],
     * which does the same job for a scanned QR code.
     */
    val changeCount: StateFlow<Int> = _changeCount.asStateFlow()

    /** Writes the address when it is usable, and reports precisely what was wrong when not. */
    fun save(settings: AppSettings, host: String, port: String, apiKey: String): Outcome {
        val trimmedHost = host.trim()
        val portNumber = port.trim().toIntOrNull()
        val hostBlank = trimmedHost.isBlank()
        val portInvalid = portNumber == null || portNumber !in VALID_PORTS

        if (hostBlank || portInvalid) return Outcome.Invalid(hostBlank, portInvalid)

        settings.host = trimmedHost
        settings.port = portNumber!!
        settings.apiKey = apiKey.trim()
        _changeCount.value += 1
        return Outcome.Saved
    }

    private val VALID_PORTS = 1..65535
}
