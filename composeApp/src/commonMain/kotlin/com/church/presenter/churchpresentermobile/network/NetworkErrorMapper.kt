package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.util.CrashReporting

/**
 * Records this exception as a non-fatal event in Crashlytics **and** returns a
 * short, user-readable message for the UI.
 *
 * Every failure is logged as a breadcrumb; only those that [shouldReportAsNonFatal]
 * considers genuine defects are reported. See that function for the rules.
 *
 * Custom keys attached to every non-fatal report:
 *  - `network_tag`        — the class/ViewModel that originated the call
 *  - `network_operation`  — the specific method that failed (e.g. "loadSongs")
 *  - `network_error_type` — Kotlin class name of the exception
 *  - `network_error_msg`  — first 200 chars of the exception message
 *
 * @param tag       ViewModel/class tag (shown as a breadcrumb key in Crashlytics).
 * @param operation Human-readable operation name, e.g. `"loadSongs"`.
 */
fun Throwable.recordNetworkError(tag: String, operation: String): String {
    val errorMsg = message?.take(200) ?: "unknown"
    // Breadcrumb log — visible in the Crashlytics log tab. Always logged, even
    // for expected failures, so the breadcrumb trail is intact if a *real*
    // crash follows.
    CrashReporting.log("[$tag] $operation FAILED (${this::class.simpleName}): $errorMsg")

    if (shouldReportAsNonFatal()) {
        // Custom keys — pinned to the session for every subsequent report
        CrashReporting.setCustomKey("network_tag",        tag)
        CrashReporting.setCustomKey("network_operation",  operation)
        CrashReporting.setCustomKey("network_error_type", this::class.simpleName ?: "Throwable")
        CrashReporting.setCustomKey("network_error_msg",  errorMsg)
        CrashReporting.recordException(this)
    }
    return toFriendlyNetworkMessage()
}

/**
 * Whether this failure is a genuine defect worth a non-fatal crash report, as
 * opposed to a condition this client should simply expect to meet in the field.
 *
 * Not reported:
 *  - Transient connectivity failures (timeouts, connection refused, offline —
 *    see [isExpectedConnectivityError]). An unreachable LAN server is the normal
 *    state for this client, not a defect.
 *  - [ApiException]s outside [REPORTABLE_SERVER_FAULT_STATUSES] — the desktop
 *    answered, and what it said is a business-logic response. That covers 4xx
 *    rejections (denied, blocked, missing) and 503, which the desktop uses to
 *    mean "nothing loaded yet" rather than "I am broken".
 *
 * Reported: everything else, including 500/502/504 — those indicate the desktop
 * itself failed, and silencing them would hide real server bugs.
 */
fun Throwable.shouldReportAsNonFatal(): Boolean {
    if (isExpectedConnectivityError()) return false
    val status = (this as? ApiException)?.httpStatus ?: return true
    return status in REPORTABLE_SERVER_FAULT_STATUSES
}

/**
 * True when the desktop answered "I'm not ready for that yet" rather than failing.
 *
 * The companion server uses 503 for ordinary not-loaded states — most visibly
 * `GET /api/pictures` before the operator has opened a picture folder. Screens
 * should render their empty state for this, not an error banner: nothing has
 * gone wrong, and there is nothing the phone can retry into existence.
 */
fun Throwable.isServerNotReady(): Boolean =
    this is ApiException && httpStatus == 503

/**
 * Server-fault statuses that still warrant a non-fatal report. Deliberately
 * excludes 503, which the desktop returns for ordinary "not loaded" states.
 */
private val REPORTABLE_SERVER_FAULT_STATUSES = setOf(
    500,   // Internal Server Error
    502,   // Bad Gateway
    504,   // Gateway Timeout
)

/**
 * True for transient connectivity failures that are expected whenever the
 * companion server is unreachable — timeouts, connection refused, unresolved
 * host, device offline. These are handled by the UI (error state) and must not
 * be reported to crash reporting as non-fatals.
 *
 * Detection is by exception class name and message pattern rather than by type,
 * because the underlying exceptions differ per platform (Ktor Darwin vs OkHttp)
 * and some aren't referenceable from commonMain.
 */
fun Throwable.isExpectedConnectivityError(): Boolean {
    // Walk the cause chain: the connectivity failure is often wrapped (e.g. a
    // Ktor/engine exception whose cause is the real SocketTimeoutException), and
    // recordException reports the whole chain, so checking only `this` misses it.
    var current: Throwable? = this
    var depth = 0
    while (current != null && depth < 8) {
        val name = current::class.simpleName ?: ""
        if (name in EXPECTED_CONNECTIVITY_EXCEPTIONS) return true

        val raw = current.message
        if (raw != null && CONNECTIVITY_MESSAGE_MARKERS.any { raw.contains(it, ignoreCase = true) }) {
            return true
        }
        current = current.cause
        depth++
    }
    return false
}

private val EXPECTED_CONNECTIVITY_EXCEPTIONS = setOf(
    "SocketTimeoutException",
    "HttpRequestTimeoutException",
    "ConnectTimeoutException",
    "ConnectException",
    "UnresolvedAddressException",
    "UnknownHostException",
    // WebSocket upgrade rejected — always a proxy/captive-portal/wrong-endpoint
    // condition (e.g. a gateway answering the /ws upgrade with 504), never a bug.
    "ProtocolException",
)

private val CONNECTIVITY_MESSAGE_MARKERS = listOf(
    "timeout",
    "timed out",
    // OkHttp socket-connect timeout message is "failed to connect to <ip> ... after
    // Nms" — it contains no "timeout" substring, so match the connect phrasing too.
    "failed to connect",
    "Connection refused",
    "ECONNREFUSED",
    "Unable to resolve host",
    "Could not connect",
    "Code=-1001",   // NSURLError timed out
    "Code=-1004",   // NSURLError could not connect to host
    "Code=-1009",   // NSURLError not connected to internet
    "offline",
    // Internal sentinel exceptions thrown by ServerEventService when the
    // server is unreachable — expected connectivity conditions, not bugs.
    "WebSocket not connected",
    "WebSocket connection",
    // WebSocket upgrade answered by a plain HTTP response (proxy / captive portal
    // / server not speaking WS) — e.g. "Expected HTTP 101 response but was 504".
    "Expected HTTP 101",
)

/**
 * Converts a raw network [Throwable] into a short, user-readable message.
 *
 * Both platforms wrap underlying OS errors in Ktor exceptions whose [Throwable.message]
 * can be extremely verbose (e.g. on iOS the full NSError dictionary is included).
 * This extension collapses the most common patterns into friendly one-liners.
 */
fun Throwable.toFriendlyNetworkMessage(): String {
    // ── Server answered with a non-2xx ───────────────────────────────────────
    // The desktop's own `reason` is written for humans ("No picture folder
    // loaded"), so prefer it over anything synthesised here. Only when it says
    // nothing useful does the status code get translated into words — a bare
    // "HTTP 403" tells the operator nothing they can act on.
    if (this is ApiException) {
        reason?.let { return it }
        return when (httpStatus) {
            401, 403 -> "Server refused the request. Check the API key in Settings."
            404      -> "Not found on the server."
            408      -> "The server took too long to respond."
            503      -> "The desktop isn't ready yet."
            in 500..599 -> "The desktop app reported an error."
            else     -> "Server error ($httpStatus)."
        }
    }

    val raw = message ?: return "Connection error"

    // ── iOS: Ktor Darwin engine wraps NSURLError like
    //    "Exception in http request: Error Domain=NSURLErrorDomain Code=-1200 ..."
    if (raw.startsWith("Exception in http request:")) {
        val inner = raw.removePrefix("Exception in http request:").trim()
        return when {
            inner.contains("Code=-1200") ||
            inner.contains("TLS", ignoreCase = true) ||
            inner.contains("SSL", ignoreCase = true) ||
            inner.contains("secure connection", ignoreCase = true) ->
                "SSL error: could not establish a secure connection. Check server settings."
            inner.contains("Code=-1009") ||
            inner.contains("offline", ignoreCase = true) ->
                "No network connection."
            inner.contains("Code=-1004") ||
            inner.contains("Could not connect", ignoreCase = true) ->
                "Server not reachable. Check the IP address and port."
            inner.contains("Code=-1001") ||
            inner.contains("timed out", ignoreCase = true) ->
                "Connection timed out. Make sure the server is running."
            inner.contains("Code=-1003") ||
            inner.contains("hostname", ignoreCase = true) ->
                "Invalid server address. Check the IP address."
            // Fallback: first line only, capped at 120 chars
            else -> inner.lines().firstOrNull()?.take(120) ?: "Connection error"
        }
    }

    // ── Android / OkHttp patterns ────────────────────────────────────────────
    return when {
        raw.contains("Handshake failed", ignoreCase = true) ||
        raw.contains("PKIX path", ignoreCase = true) ||
        raw.contains("Trust anchor", ignoreCase = true) ||
        raw.contains("CertPathValidatorException", ignoreCase = true) ->
            "SSL error: could not establish a secure connection. Check server settings."

        raw.contains("Connection refused", ignoreCase = true) ||
        raw.contains("ECONNREFUSED") ->
            "Server not reachable. Check the IP address and port."

        raw.contains("Unable to resolve host", ignoreCase = true) ||
        raw.contains("UnresolvedAddressException") ->
            "Invalid server address. Check the IP address."

        raw.contains("SocketTimeoutException") ||
        raw.contains("timeout", ignoreCase = true) ->
            "Connection timed out. Make sure the server is running."

        raw.contains("NetworkOnMainThreadException") ->
            "Network error (internal). Please restart the app."

        else -> raw.take(120)
    }
}
