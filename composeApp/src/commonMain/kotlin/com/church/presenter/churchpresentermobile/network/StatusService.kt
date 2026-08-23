package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ServerStatus
import com.church.presenter.churchpresentermobile.model.StatusProbeResult
import com.church.presenter.churchpresentermobile.util.Logger
import com.church.presenter.churchpresentermobile.util.appVersion
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

private const val TAG = "StatusService"

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Probes the configured server and classifies the outcome as a [StatusProbeResult].
 *
 * "Connected" must mean we genuinely reached a ChurchPresenter server — not merely
 * that *some* host answered HTTP at the configured address. A 404 from `/api/status`
 * is therefore NOT treated as connected: we fall back to probing the guaranteed
 * `/api/songs` endpoint to positively identify a ChurchPresenter server before
 * reporting success. The desktop app is expected to provide `GET /api/status`;
 * older versions that omit it still verify via `/api/songs`.
 */
class StatusService internal constructor(
    private val baseUrl: String,
    private val apiKey: String,
    private val deviceId: String,
    private val client: HttpClient,
    /** What the desktop should call this device; blank sends no name. */
    private val deviceName: String = "",
) {
    /**
     * Production constructor — reads connection details from [AppSettings] and
     * builds the real platform HTTP client. Tests use the [internal] constructor
     * above to inject a [MockEngine]-backed client.
     */
    constructor(settings: AppSettings) : this(
        baseUrl = settings.apiBaseUrl,
        apiKey = settings.apiKey,
        deviceId = settings.deviceId,
        client = createHttpClient(),
        deviceName = settings.reportedDeviceName,
    )

    /**
     * Fetches and classifies server status.
     *
     * Connectivity failures (timeout / refused / unresolved host) surface as
     * `Result.failure`. Any HTTP-level outcome is described precisely via
     * [StatusProbeResult].
     */
    suspend fun fetchStatus(): Result<StatusProbeResult> {
        val statusUrl = "$baseUrl/${ApiConstants.STATUS_ENDPOINT}"
        Logger.d(TAG, "fetchStatus — GET $statusUrl")
        return apiRunCatching {
            val response = client.get(statusUrl) { applyHeaders() }
            val raw = response.bodyAsText()
            Logger.d(TAG, "fetchStatus — status=${response.status}  body=${raw.take(300)}")

            when {
                isUnauthorized(response) -> {
                    Logger.d(TAG, "fetchStatus — unauthorized (${response.status.value})")
                    StatusProbeResult.Unauthorized(response.status.value)
                }

                response.status.isSuccess() -> {
                    // Prefer the X-Server-Version response header — it carries the real
                    // BuildConfig.APP_VERSION from the desktop, whereas the JSON body's
                    // appVersion field may still be the stale Constants.SERVER_VERSION.
                    val headerVersion = response.headers[ApiConstants.SERVER_VERSION_HEADER]
                    val status = json.decodeFromString<ServerStatus>(raw)
                        .let { if (headerVersion != null) it.copy(appVersion = headerVersion) else it }
                    Logger.d(TAG, "fetchStatus — parsed: version=${status.appVersion} (header=$headerVersion)  bibles=${status.bibles.size}  songbooks=${status.songbooks.size}  features=${status.features}")

                    // Identity check: the lenient decoder happily turns `{}` or an
                    // unrelated body into an all-defaults ServerStatus, so require a
                    // POSITIVE signal — never "did it decode".
                    val isChurchPresenter = headerVersion != null ||
                        status.appVersion != null ||
                        status.endpoints.isNotEmpty() ||
                        status.features.isNotEmpty()

                    if (isChurchPresenter) {
                        StatusProbeResult.Verified(status)
                    } else {
                        Logger.d(TAG, "fetchStatus — 2xx but no ChurchPresenter identity signal, probing /songs")
                        probeSongs()
                    }
                }

                else -> {
                    // 404/500/… — endpoint missing or odd. Confirm identity via /songs.
                    Logger.d(TAG, "fetchStatus — /status returned ${response.status.value}, probing /songs")
                    probeSongs()
                }
            }
        }.onFailure { e ->
            Logger.e(TAG, "fetchStatus — FAILED: ${e.message}", e)
        }
    }

    /**
     * Probes `GET /api/songs` — a documented, always-present ChurchPresenter endpoint —
     * to positively identify the server when `/api/status` is missing or unrecognised.
     */
    private suspend fun probeSongs(): StatusProbeResult {
        val songsUrl = "$baseUrl/${ApiConstants.SONGS_ENDPOINT}"
        Logger.d(TAG, "probeSongs — GET $songsUrl")
        val response = client.get(songsUrl) { applyHeaders() }
        val raw = response.bodyAsText()
        Logger.d(TAG, "probeSongs — status=${response.status}  body=${raw.take(200)}")

        if (isUnauthorized(response)) {
            return StatusProbeResult.Unauthorized(response.status.value)
        }
        if (!response.status.isSuccess()) {
            return StatusProbeResult.NotChurchPresenter("HTTP ${response.status.value} from /songs")
        }

        // A ChurchPresenter songs response is a JSON object with a "song-book" key
        // (@SerialName on SongsResponse.songBook). Anything else is not our server.
        val hasSongBook = runCatching {
            json.parseToJsonElement(raw).jsonObject.containsKey("song-book")
        }.getOrDefault(false)

        return if (hasSongBook) {
            StatusProbeResult.ReachableNoStatusEndpoint
        } else {
            StatusProbeResult.NotChurchPresenter("Response did not identify as ChurchPresenter")
        }
    }

    private fun isUnauthorized(response: HttpResponse): Boolean =
        response.status.value == 401 || response.status.value == 403

    private fun HttpRequestBuilder.applyHeaders() {
        if (apiKey.isNotBlank()) header(ApiConstants.API_KEY_HEADER, apiKey)
        header(ApiConstants.DEVICE_ID_HEADER, deviceId)
        if (deviceName.isNotBlank()) header(ApiConstants.DEVICE_NAME_HEADER, deviceName)
        header(ApiConstants.APP_VERSION_HEADER, appVersion)
    }

    fun closeClient() {
        client.close()
    }
}
