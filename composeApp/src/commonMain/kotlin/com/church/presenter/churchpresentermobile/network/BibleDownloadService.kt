package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

private const val TAG = "BibleDownloadService"

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Copies whole Bible modules off the desktop.
 *
 * Separate from [BibleService], which talks to the chapter API for a desktop that is presenting.
 * This one downloads the `.spb` files behind that API so the phone can read them with no desktop
 * at all — one 4.6 MB body instead of 1,189 chapter requests.
 *
 * Uses [createActionHttpClient] rather than [createHttpClient]: its 12-second request timeout
 * would kill a multi-megabyte download on ordinary church Wi-Fi. The action client already has
 * exactly the shape wanted here — no request or socket timeout, a bounded connect timeout so an
 * unreachable address still fails fast — even though it was built for approval endpoints.
 */
class BibleDownloadService(
    private val settings: AppSettings,
    private val client: HttpClient = createActionHttpClient(),
) {

    /**
     * The modules the desktop has configured, in its own presentation order.
     *
     * The names are the pick list *and* the identity: [downloadTranslation] takes a position in
     * this same list, so the two calls must be read together.
     */
    suspend fun listTranslations(): Result<List<String>> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.BIBLE_TRANSLATIONS_ENDPOINT}"
        return apiRunCatching {
            val response = client.get(url) { applyApiKey() }
            val raw = response.bodyAsText()
            response.ensureSuccess(raw)
            json.decodeFromString<List<String>>(raw)
        }.onFailure { Logger.e(TAG, "listTranslations — FAILED for $url: ${it.message}", it) }
            .onSuccess { Logger.d(TAG, "listTranslations — ${it.size} modules offered") }
    }

    /**
     * One module's raw `.spb` text, by its position in the manifest.
     *
     * The desktop indexes into a list it rebuilds from its own settings, so a position is only
     * good for as long as that list is unchanged — callers must re-read the manifest immediately
     * before downloading and match on the file name, or an operator changing translations on the
     * desktop mid-sync gets a different Bible than the one they picked.
     */
    suspend fun downloadTranslation(index: Int): Result<String> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.BIBLE_TRANSLATION_ENDPOINT}/$index"
        return apiRunCatching {
            val response = client.get(url) { applyApiKey() }
            val raw = response.bodyAsText()
            response.ensureSuccess(raw)
            raw
        }.onFailure { Logger.e(TAG, "downloadTranslation($index) — FAILED: ${it.message}", it) }
            .onSuccess { Logger.d(TAG, "downloadTranslation($index) — ${it.length} chars") }
    }

    private fun HttpRequestBuilder.applyApiKey() {
        val key = settings.apiKey
        if (key.isNotBlank()) header(ApiConstants.API_KEY_HEADER, key)
        identifyDevice(settings)
    }

    /** Releases the underlying HTTP client. Call when the owning ViewModel is cleared. */
    fun closeClient() {
        client.close()
    }
}
