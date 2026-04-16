package com.church.presenter.churchpresentermobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/** Returns a new random UUID string (e.g. "550e8400-e29b-41d4-a716-446655440000"). */
expect fun generateUUID(): String

/**
 * Opens [url] in the platform's default browser / Safari / Chrome.
 * Used by [CertSetupScreen] to open the CA certificate download URL.
 */
expect fun openUrl(url: String)

