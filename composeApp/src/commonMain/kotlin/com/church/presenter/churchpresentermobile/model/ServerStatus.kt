package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Per-device permission flags returned by GET /api/status.
 *
 * The server resolves these based on the `X-Device-Id` header sent with the request.
 * If the header is absent, or the device has no explicit entry, the server returns
 * the global defaults configured in ChurchPresenter settings.
 *
 * All fields default to `true` so that older server versions that omit the object
 * are treated as fully permissive (no regression for existing deployments).
 */
@Serializable
data class DevicePermissions(
    /** Whether this device is allowed to control the presentation (project/clear/select). */
    @SerialName("canPresent")         val canPresent: Boolean        = true,
    /** Whether this device is allowed to add items to the schedule. */
    @SerialName("canAddToSchedule")   val canAddToSchedule: Boolean  = true,
    /** Whether this device is allowed to upload files (presentations, pictures). */
    @SerialName("canUploadFiles")     val canUploadFiles: Boolean    = true,
)

/**
 * Response model for GET /api/status.
 *
 * All fields are optional / have defaults so that partial responses from older
 * server versions are parsed gracefully.
 */
@Serializable
data class ServerStatus(
    /** Version string of the desktop app, e.g. "1.4.2". */
    @SerialName("appVersion")      val appVersion: String?          = null,
    /** List of API endpoint paths the server exposes, e.g. ["songs","bible","schedule"]. */
    @SerialName("endpoints")       val endpoints: List<String>      = emptyList(),
    /** API-key policy: "none" means no key is configured on the server. */
    @SerialName("apiKeyStatus")    val apiKeyStatus: String?        = null,
    /** Bible translation names available on the server. */
    @SerialName("bibles")          val bibles: List<String>         = emptyList(),
    /** Song-book names available on the server. */
    @SerialName("songbooks")       val songbooks: List<String>      = emptyList(),
    /** Feature flags / capability strings the server advertises. */
    @SerialName("features")        val features: List<String>       = emptyList(),
    /**
     * Per-device permission flags for the device that sent the request.
     * Defaults to fully permissive so older servers that omit this field
     * do not incorrectly restrict functionality.
     */
    @SerialName("permissions")     val permissions: DevicePermissions = DevicePermissions(),
    /**
     * NOT part of the wire format — set by [StatusService] to `false` when the
     * server returned a non-2xx response (e.g. 404 because the endpoint is not
     * implemented yet). When `false`, content-availability warnings (no bibles,
     * no songbooks) are suppressed because the server simply didn't tell us —
     * it doesn't mean the content is missing.
     */
    val endpointAvailable: Boolean = true,
)

/** A human-readable warning derived from a [ServerStatus] response. */
sealed class StatusWarning {
    /** The server has no API key configured — certain protective endpoints will reject requests. */
    data object NoApiKey : StatusWarning()
    /** No Bible translations are available on the server. */
    data object NoBibles : StatusWarning()
    /** No song books are available on the server. */
    data object NoSongbooks : StatusWarning()
    /** This device is not permitted to control the presentation. */
    data object PresentBlocked : StatusWarning()
    /** This device is not permitted to add items to the schedule. */
    data object ScheduleBlocked : StatusWarning()
    /** This device is not permitted to upload files. */
    data object UploadBlocked : StatusWarning()
    /** The server is reachable but returned an unexpected / very old version string. */
    data class UnknownVersion(val version: String?) : StatusWarning()
    /** A specific expected API endpoint is missing from the server's reported list. */
    data class MissingEndpoint(val endpoint: String) : StatusWarning()
}

/** Derives a list of [StatusWarning] items from a [ServerStatus] payload. */
fun ServerStatus.deriveWarnings(): List<StatusWarning> {
    val warnings = mutableListOf<StatusWarning>()

    // API key is an optional preference — not having one configured is not a warning.

    // Content-availability warnings: only fire when the status endpoint replied
    // successfully. A 404 fallback returns empty lists, but that just means the
    // endpoint isn't implemented yet — NOT that the content is absent.
    if (endpointAvailable) {
        if (bibles.isEmpty()) {
            warnings += StatusWarning.NoBibles
        }
        if (songbooks.isEmpty()) {
            warnings += StatusWarning.NoSongbooks
        }

        // Device-level permission restrictions
        if (!permissions.canPresent) {
            warnings += StatusWarning.PresentBlocked
        }
        if (!permissions.canAddToSchedule) {
            warnings += StatusWarning.ScheduleBlocked
        }
        if (!permissions.canUploadFiles) {
            warnings += StatusWarning.UploadBlocked
        }
    }

    return warnings
}

