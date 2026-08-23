package com.church.presenter.churchpresentermobile

interface Platform {
    val name: String
    /** One of "android", "ios", or "web" — used to tag analytics pings by OS. */
    val os: String
}

expect fun getPlatform(): Platform

/** Returns a new random UUID string (e.g. "550e8400-e29b-41d4-a716-446655440000"). */
expect fun generateUUID(): String

/**
 * What this device calls itself — the name its owner sees in the OS, or the
 * model where the OS will not say.
 *
 * Reported to the desktop so an operator approving a connection reads
 * "Pixel 7 Pro" rather than a UUID. Blank when the platform offers nothing
 * usable, which is a legitimate answer: the caller then sends no name and the
 * desktop falls back to the id, exactly as it does for clients that predate
 * this.
 */
expect fun deviceName(): String

/**
 * Opens [url] in the platform's default browser / Safari / Chrome.
 * Used by [CertSetupScreen] to open the CA certificate download URL.
 */
expect fun openUrl(url: String)

