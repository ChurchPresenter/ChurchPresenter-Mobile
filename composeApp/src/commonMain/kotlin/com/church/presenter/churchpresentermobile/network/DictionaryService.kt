package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DictionaryItemPayload
import com.church.presenter.churchpresentermobile.model.DictionaryVersesResponse
import com.church.presenter.churchpresentermobile.model.ProjectDictionaryRequest
import com.church.presenter.churchpresentermobile.model.StrongsEntry
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "DictionaryService"
private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/** Language/testament filter for dictionary search. */
enum class DictionaryFilter(val apiValue: String) {
    ALL("all"), HEBREW("hebrew"), GREEK("greek")
}

/**
 * REST + WebSocket client for the Strong's dictionary feature.
 *
 * Reads (`GET /api/dictionary`, `GET /api/dictionary/{number}`) go over HTTP with
 * the `X-Api-Key` header; project / add-to-schedule go over the shared WebSocket,
 * matching [SongService] / [BibleService].
 */
class DictionaryService(
    private val settings: AppSettings,
    private val wsService: WsSender,
    private val client: HttpClient = createHttpClient(),
) {
    private fun HttpRequestBuilder.applyApiKey() {
        val key = settings.apiKey
        if (key.isNotBlank()) header(ApiConstants.API_KEY_HEADER, key)
        header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
    }

    /**
     * Searches the dictionary. Empty [query] returns the first [limit] entries.
     *
     * When [book] (1-based book number) is supplied, results are restricted to the
     * Strong's numbers that occur within that Bible reference. [chapter] and [verse]
     * narrow the range progressively: book only → whole book, book+chapter → whole
     * chapter, book+chapter+verse → a single verse. They combine with [query]/[filter].
     */
    suspend fun search(
        query: String,
        filter: DictionaryFilter = DictionaryFilter.ALL,
        lang: String = "en",
        limit: Int = 100,
        book: Int? = null,
        chapter: Int? = null,
        verse: Int? = null,
    ): Result<List<StrongsEntry>> = apiRunCatching {
        val url = "${settings.apiBaseUrl}/${ApiConstants.DICTIONARY_ENDPOINT}"
        val response = client.get(url) {
            applyApiKey()
            parameter("q", query)
            parameter("filter", filter.apiValue)
            parameter("lang", lang)
            parameter("limit", limit)
            if (book != null) parameter("book", book)
            if (chapter != null) parameter("chapter", chapter)
            if (verse != null) parameter("verse", verse)
        }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw Exception("HTTP ${response.status.value}")
        json.decodeFromString<List<StrongsEntry>>(raw)
    }.onFailure { e -> Logger.e(TAG, "search — FAILED: ${e.message}", e) }

    /**
     * Fetches the verses in which [number] appears (the entry sheet's "Appears in"
     * list), capped at [limit]. When [book] (1-based) — optionally with [chapter]
     * and [verse] — is supplied, references in that scope are ordered first so the
     * verse currently being filtered leads the list.
     */
    suspend fun getVerses(
        number: String,
        limit: Int = 25,
        book: Int? = null,
        chapter: Int? = null,
        verse: Int? = null,
        lang: String = "en",
    ): Result<DictionaryVersesResponse> = apiRunCatching {
        val url = "${settings.apiBaseUrl}/${ApiConstants.DICTIONARY_ENDPOINT}/${number.encodeURLPathPart()}/verses"
        val response = client.get(url) {
            applyApiKey()
            parameter("limit", limit)
            parameter("lang", lang)
            if (book != null) parameter("book", book)
            if (chapter != null) parameter("chapter", chapter)
            if (verse != null) parameter("verse", verse)
        }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw Exception("HTTP ${response.status.value}")
        json.decodeFromString<DictionaryVersesResponse>(raw)
    }.onFailure { e -> Logger.e(TAG, "getVerses — FAILED: ${e.message}", e) }

    /** Looks up a single entry by Strong's number (e.g. "H430"). */
    suspend fun lookup(number: String, lang: String = "en"): Result<StrongsEntry> = apiRunCatching {
        val url = "${settings.apiBaseUrl}/${ApiConstants.DICTIONARY_ENDPOINT}/${number.encodeURLPathPart()}"
        val response = client.get(url) {
            applyApiKey()
            parameter("lang", lang)
        }
        val raw = response.bodyAsText()
        if (response.status.value !in 200..299) throw Exception("HTTP ${response.status.value}")
        json.decodeFromString<StrongsEntry>(raw)
    }.onFailure { e -> Logger.e(TAG, "lookup — FAILED: ${e.message}", e) }

    /** Sends the entry to the presenter window immediately (requires desktop approval). */
    suspend fun projectEntry(entry: StrongsEntry): Result<Unit> = apiRunCatching {
        val payload = json.encodeToString(entry.toProjectRequest())
        Logger.d(TAG, "projectEntry ▶ WS project  payload=$payload")
        wsService.sendAction(WsMessageType.PROJECT, payload).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "projectEntry — FAILED: ${e.message}", e) }

    /** Adds the entry to the schedule list (does not go live). */
    suspend fun addEntryToSchedule(entry: StrongsEntry): Result<Unit> = apiRunCatching {
        val payload = json.encodeToString(entry.toProjectRequest())
        Logger.d(TAG, "addEntryToSchedule ▶ WS add_to_schedule  payload=$payload")
        wsService.sendAction(WsMessageType.ADD_TO_SCHEDULE, payload).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "addEntryToSchedule — FAILED: ${e.message}", e) }

    private fun StrongsEntry.toProjectRequest() = ProjectDictionaryRequest(
        item = DictionaryItemPayload(
            id              = number,
            strongsNumber   = number,
            title           = word,
            transliteration = transliteration,
            definition      = definition,
            displayText     = "$word ($number)"
        )
    )

    fun closeClient() = client.close()
}
