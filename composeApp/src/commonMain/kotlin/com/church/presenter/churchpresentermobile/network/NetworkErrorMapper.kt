package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.util.CrashReporting

/**
 * Records this exception as a non-fatal event in Crashlytics **and** returns a
 * short, user-readable message for the UI.
 *
 * [ApiException]s (expected server rejections — denied, blocked, HTTP 4xx) are
 * intentionally not forwarded to Crashlytics because they are business-logic
 * responses, not bugs.
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
    if (this !is ApiException) {
        val errorMsg = message?.take(200) ?: "unknown"
        // Breadcrumb log — visible in the Crashlytics log tab
        CrashReporting.log("[$tag] $operation FAILED (${this::class.simpleName}): $errorMsg")
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
 * Converts a raw network [Throwable] into a short, user-readable message.
 *
 * Both platforms wrap underlying OS errors in Ktor exceptions whose [Throwable.message]
 * can be extremely verbose (e.g. on iOS the full NSError dictionary is included).
 * This extension collapses the most common patterns into friendly one-liners.
 */
fun Throwable.toFriendlyNetworkMessage(): String {
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
