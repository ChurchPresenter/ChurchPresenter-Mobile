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
        data class Invalid(
            val hostBlank: Boolean,
            val portInvalid: Boolean,
            /** Present but unusable as a URL host — see [isUsableHost]. */
            val hostInvalid: Boolean = false,
        ) : Outcome
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
        val cleanedHost = normalizeHost(host)
        val portNumber = port.trim().toIntOrNull()
        val hostBlank = cleanedHost.isBlank()
        val hostInvalid = !hostBlank && !isUsableHost(cleanedHost)
        val portInvalid = portNumber == null || portNumber !in VALID_PORTS

        if (hostBlank || hostInvalid || portInvalid) {
            return Outcome.Invalid(hostBlank, portInvalid, hostInvalid)
        }

        settings.host = cleanedHost
        settings.port = portNumber!!
        settings.apiKey = apiKey.trim()
        _changeCount.value += 1
        return Outcome.Saved
    }

    /**
     * Tidies what was typed or pasted into the bare host the URL needs.
     *
     * Pasting the whole address off a desktop's screen — `http://192.168.1.5:8765/api` — is the
     * ordinary way an operator gets this wrong, and every part of it except the host is either
     * already known or set in the field beside it. Stripping is better than rejecting: the
     * paste carried the right answer.
     *
     * A paste that still carries a port — `192.168.1.5:8765` — is left unusable on purpose, so
     * [save] refuses it and the field says why. The alternatives are both worse: dropping the
     * port connects somewhere the operator can see they did not ask for, and applying it
     * overwrites the port field beside it without saying so.
     */
    fun normalizeHost(raw: String): String {
        var host = raw.trim()
        SCHEMES.firstOrNull { host.startsWith(it, ignoreCase = true) }?.let {
            host = host.substring(it.length)
        }
        host = host.substringBefore('/').substringBefore('?').substringBefore('#')
        return host.trim()
    }

    /**
     * Whether [host] can actually be used as the host of a URL.
     *
     * Blank was the only thing checked before, so anything else typed into the field reached the
     * HTTP client as-is. A dictated address that arrived as "high dynamic range" was accepted,
     * saved, and then thrown out by the URL builder on every request and every WebSocket
     * reconnect — several hundred failures an hour, none of which the operator could see a
     * reason for. Rejecting it at the field is the only place the answer is knowable.
     *
     * Deliberately not a full RFC 1123 parser. It rejects what cannot be a host — whitespace,
     * separators, anything a URL would have to escape — and accepts everything else, so an
     * unusual but legitimate name is never refused. A name that is well-formed but wrong
     * (`"printer"`) still fails later, where it is correctly reported as unreachable.
     */
    fun isUsableHost(host: String): Boolean {
        val candidate = host.trim()
        if (candidate.isEmpty()) return false
        // Bracketed IPv6 literal — the one legitimate use of colons in a host.
        if (candidate.startsWith("[")) return candidate.endsWith("]") && IPV6_BODY.matches(candidate.drop(1).dropLast(1))
        return HOSTNAME.matches(candidate)
    }

    private val SCHEMES = listOf("http://", "https://", "ws://", "wss://")

    /** Letters, digits, dots, hyphens and underscores, starting on something alphanumeric. */
    private val HOSTNAME = Regex("^[A-Za-z0-9][A-Za-z0-9._-]*$")

    private val IPV6_BODY = Regex("^[0-9A-Fa-f:.]+$")

    private val VALID_PORTS = 1..65535
}
