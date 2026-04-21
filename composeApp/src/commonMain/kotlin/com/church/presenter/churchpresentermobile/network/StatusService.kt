package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ServerStatus
import com.church.presenter.churchpresentermobile.util.Logger
import com.church.presenter.churchpresentermobile.util.appVersion
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

private const val TAG = "StatusService"

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Fetches server capability / status information from GET /api/status.
 *
 * The endpoint is expected to be provided by the ChurchPresenter desktop app.
 * If the endpoint does not exist (HTTP 404) the call still succeeds with an
 * empty [ServerStatus] so the caller can surface an appropriate warning.
 */
class StatusService(private val settings: AppSettings) {
    private val client: HttpClient = createHttpClient()

    suspend fun fetchStatus(): Result<ServerStatus> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.STATUS_ENDPOINT}"
        Logger.d(TAG, "fetchStatus — GET $url")
        return apiRunCatching {
            val response = client.get(url) {
                val key = settings.apiKey
                if (key.isNotBlank()) header(ApiConstants.API_KEY_HEADER, key)
                header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
                header(ApiConstants.APP_VERSION_HEADER, appVersion)
            }
            val raw = response.bodyAsText()
            Logger.d(TAG, "fetchStatus — status=${response.status}  body=${raw.take(300)}")
            if (!response.status.isSuccess()) {
                Logger.d(TAG, "fetchStatus — endpoint missing (${response.status}), suppressing content warnings")
                return@apiRunCatching ServerStatus(endpointAvailable = false)
            }
            // Prefer the X-Server-Version response header — it carries the real
            // BuildConfig.APP_VERSION from the desktop, whereas the JSON body's
            // appVersion field may still be the stale Constants.SERVER_VERSION.
            val headerVersion = response.headers[ApiConstants.SERVER_VERSION_HEADER]
            json.decodeFromString<ServerStatus>(raw)
                .let { if (headerVersion != null) it.copy(appVersion = headerVersion) else it }
                .also {
                    Logger.d(TAG, "fetchStatus — parsed: version=${it.appVersion} (header=$headerVersion)  bibles=${it.bibles.size}  songbooks=${it.songbooks.size}  features=${it.features}")
                }
        }.onFailure { e ->
            Logger.e(TAG, "fetchStatus — FAILED: ${e.message}", e)
        }
    }

    fun closeClient() {
        client.close()
    }
}

