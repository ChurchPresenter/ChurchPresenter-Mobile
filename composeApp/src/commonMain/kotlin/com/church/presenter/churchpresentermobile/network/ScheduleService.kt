package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.model.ScheduleResponse
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

private const val TAG = "ScheduleService"

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Fetches the service schedule from GET /api/schedule.
 *
 * @param settings The current [AppSettings] used to build the base URL and API key.
 */
class ScheduleService(private val settings: AppSettings) {
    private val client: HttpClient = createHttpClient()

    init {
        Logger.d(TAG, "ScheduleService created — baseUrl=${settings.apiBaseUrl}")
    }

    /**
     * Fetches the schedule items from the server.
     *
     * Handles both array responses (bare JSON array) and object responses
     * (wrapped in `{ "items": [...] }` or `{ "schedule": [...] }`).
     */
    suspend fun getSchedule(): Result<List<ScheduleItem>> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.SCHEDULE_ENDPOINT}"
        Logger.d(TAG, "getSchedule — requesting URL: $url")
        return apiRunCatching {
            val httpResponse = client.get(url) { applyApiKey() }
            Logger.d(TAG, "getSchedule — HTTP status: ${httpResponse.status}")
            val raw = httpResponse.bodyAsText()
            Logger.d(TAG, "getSchedule — raw body (first 500 chars): ${raw.take(500)}")

            // Parse flexibly: the endpoint may return a bare array or a wrapper object
            val element = lenientJson.parseToJsonElement(raw)
            val items: List<ScheduleItem> = if (element is JsonArray) {
                lenientJson.decodeFromJsonElement<List<ScheduleItem>>(element)
            } else {
                val response = lenientJson.decodeFromJsonElement<ScheduleResponse>(element)
                response.allItems
            }
            Logger.d(TAG, "getSchedule — parsed ${items.size} items")
            items
        }.onFailure { e ->
            Logger.e(TAG, "getSchedule — FAILED for URL $url: ${e.message}", e)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyApiKey() {
        val key = settings.apiKey
        if (key.isNotBlank()) {
            header(ApiConstants.API_KEY_HEADER, key)
        }
        header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
    }

    /** Releases the underlying HTTP client. Call when the owning ViewModel is cleared. */
    fun closeClient() {
        Logger.d(TAG, "closeClient — closing HTTP client")
        client.close()
    }
}

