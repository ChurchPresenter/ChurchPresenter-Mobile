package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ProjectSongRequest
import com.church.presenter.churchpresentermobile.model.SelectSectionRequest
import com.church.presenter.churchpresentermobile.model.SelectSongPayload
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SongItemPayload
import com.church.presenter.churchpresentermobile.model.SongVerse
import com.church.presenter.churchpresentermobile.model.SongsResponse
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

private const val TAG = "SongService"

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/** Minimal model for the `{"ok":bool,"reason":"...","error":"..."}` shape the API returns. */
@Serializable
private data class ApiResponseBody(
    val ok: Boolean? = null,
    val reason: String? = null,
    val error: String? = null,
)

/**
 * Throws [ApiException] when the response indicates failure —
 * either via a non-2xx HTTP status or an `ok:false` body.
 */
private fun checkApiResponse(statusCode: Int, rawBody: String) {
    val parsed = runCatching { json.decodeFromString<ApiResponseBody>(rawBody) }.getOrNull()
    val isOk = parsed?.ok ?: (statusCode in 200..299)
    if (!isOk) {
        throw ApiException(
            httpStatus = statusCode,
            reason     = parsed?.reason ?: parsed?.error,
        )
    }
}

/**
 * Handles all REST communication with the ChurchPresenter companion API.
 *
 * @param settings The current [AppSettings] used to build the base URL and supply the API key.
 */
class SongService(private val settings: AppSettings) {
    private val client: HttpClient = createHttpClient()
    /** WebSocket service for approval-required actions (project / add-to-schedule). */
    private val wsService: WebSocketService = WebSocketService(settings)

    init {
        Logger.d(TAG, "SongService created — host=${settings.host} port=${settings.port} baseUrl=${settings.apiBaseUrl}")
    }

    /**
     * Fetches the full song catalog from the server and flattens it into a single list.
     */
    suspend fun getSongs(): Result<List<Song>> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.SONGS_ENDPOINT}"
        Logger.d(TAG, "getSongs — requesting URL: $url")
        return apiRunCatching {
            val httpResponse = client.get(url) { applyApiKey() }
            Logger.d(TAG, "getSongs — HTTP status: ${httpResponse.status}")
            val raw = httpResponse.bodyAsText()
            Logger.d(TAG, "getSongs — raw body (first 500 chars): ${raw.take(500)}")
            val response = json.decodeFromString<SongsResponse>(raw)
            val songs = response.songBook.flatMap { book ->
                book.songs.map { song -> song.copy(bookName = book.bookName) }
            }
            Logger.d(TAG, "getSongs — parsed ${songs.size} songs across ${response.songBook.size} books")
            songs
        }.onFailure { e ->
            Logger.e(TAG, "getSongs — FAILED for URL $url: ${e.message}", e)
        }
    }

    /**
     * Fetches full song details (including lyrics/verses) for a single song.
     * Uses a two-pass approach: standard JSON decode first, then a flexible
     * JsonObject scan as a safety net for any unrecognised field names.
     */
    suspend fun getSongDetail(number: String, bookName: String?): Result<SongDetail> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.SONGS_ENDPOINT}/$number"
        Logger.d(TAG, "getSongDetail — requesting URL: $url  bookName=$bookName")
        return apiRunCatching {
            val httpResponse = client.get(url) {
                applyApiKey()
                if (!bookName.isNullOrBlank()) parameter("songbook", bookName)
            }
            Logger.d(TAG, "getSongDetail — HTTP status: ${httpResponse.status}")
            val raw = httpResponse.bodyAsText()
            Logger.d(TAG, "getSongDetail — FULL raw body:\n$raw")

            // Pass 1: standard decode (handles all @SerialName variants in the model)
            val base = json.decodeFromString<SongDetail>(raw)
            Logger.d(TAG, "getSongDetail — pass1: verses=${base.allVerses.size}  plainText=${base.plainText?.take(80)}")

            if (base.hasLyrics) return@apiRunCatching base

            // Pass 2: flexible scan — walk every key in the JsonObject
            Logger.d(TAG, "getSongDetail — pass1 found no lyrics, running flexible scan")
            val obj = json.parseToJsonElement(raw).jsonObject
            flexibleSongDetail(base, obj)
        }.onFailure { e ->
            Logger.e(TAG, "getSongDetail — FAILED for URL $url: ${e.message}", e)
        }
    }

    /**
     * Scans [obj] for any array or string value that looks like verse/lyrics data
     * and merges it into [base].
     */
    private fun flexibleSongDetail(base: SongDetail, obj: JsonObject): SongDetail {
        // --- Try any JsonArray whose objects contain at least one string value ---
        val versesFromScan: List<SongVerse>? = obj.entries
            .filter { (_, v) -> v is JsonArray }
            .firstNotNullOfOrNull { (key, v) ->
                val arr = v as JsonArray
                Logger.d(TAG, "flexibleScan — checking array key='$key' size=${arr.size}")
                runCatching { json.decodeFromJsonElement<List<SongVerse>>(arr) }
                    .getOrNull()
                    ?.filter { it.displayText.isNotBlank() }
                    ?.takeIf { it.isNotEmpty() }
                    ?.also { Logger.d(TAG, "flexibleScan — found ${it.size} verses under key='$key'") }
            }

        // --- Try any string value that looks like multi-line lyrics ---
        val plainFromScan: String? = if (versesFromScan == null) {
            obj.entries
                .filter { (_, v) -> v is JsonPrimitive && v.isString }
                .map { (k, v) -> k to (v as JsonPrimitive).contentOrNull.orEmpty() }
                .filter { (_, text) -> text.contains('\n') && text.length > 30 }
                .maxByOrNull { (_, text) -> text.length }
                ?.also { (key, text) -> Logger.d(TAG, "flexibleScan — found plain lyrics under key='$key' (${text.length} chars)") }
                ?.second
        } else null

        return if (versesFromScan != null || plainFromScan != null) {
            base.copy(
                verses = versesFromScan ?: base.verses,
                text   = plainFromScan  ?: base.text
            )
        } else {
            Logger.d(TAG, "flexibleScan — no lyrics found in any field")
            base
        }
    }

    /**
     * Notifies the server to navigate to the given song via WebSocket select_song.
     * Payload: { id, songNumber, title, songbook }
     */
    suspend fun selectSong(song: Song): Result<Unit> {
        return apiRunCatching {
            val payload = json.encodeToString(
                SelectSongPayload(
                    id         = song.number,
                    songNumber = song.number.toIntOrNull() ?: 0,
                    title      = song.title,
                    songbook   = song.bookName
                )
            )
            Logger.d(TAG, "selectSong ▶ WS select_song  payload=$payload")
            wsService.sendAction(WsMessageType.SELECT_SONG, payload, fireAndForget = true).getOrThrow()
        }.onFailure { e ->
            Logger.e(TAG, "selectSong — FAILED: ${e.message}", e)
        }
    }

    /**
     * Navigates the live presenter to a specific section (verse) of the currently projected song.
     * Sends WS select_song_section with { number, section } payload.
     *
     * @param songNumber  The song number string.
     * @param verseIndex  0-based section index.
     */
    suspend fun selectVerse(songNumber: String, bookName: String?, verseIndex: Int): Result<Unit> {
        return apiRunCatching {
            val payload = json.encodeToString(SelectSectionRequest(number = songNumber, section = verseIndex))
            Logger.d(TAG, "selectVerse ▶ WS select_song_section  section=$verseIndex  payload=$payload")
            wsService.sendAction(WsMessageType.SELECT_SONG_SECTION, payload, fireAndForget = true).getOrThrow()
        }.onFailure { e ->
            Logger.e(TAG, "selectVerse — FAILED: ${e.message}", e)
        }
    }

    /**
     * Tells the presenter to clear the display (show nothing) via WebSocket clear.
     */
    suspend fun clearDisplay(): Result<Unit> {
        Logger.d(TAG, "clearDisplay ▶ WS clear")
        return apiRunCatching {
            wsService.sendAction(WsMessageType.CLEAR, "", fireAndForget = true).getOrThrow()
        }.onFailure { e -> Logger.e(TAG, "clearDisplay — FAILED: ${e.message}", e) }
    }

    /**
     * Sends the song to the presenter window immediately via WebSocket project message.
     */
    suspend fun projectSong(song: Song): Result<Unit> {
        return apiRunCatching {
            val payload = json.encodeToString(song.toProjectRequest())
            Logger.d(TAG, "projectSong ▶ WS project  payload: $payload")
            wsService.sendAction(WsMessageType.PROJECT, payload).getOrThrow()
        }.onFailure { e -> Logger.e(TAG, "projectSong — FAILED: ${e.message}", e) }
    }

    /**
     * Adds the song to the schedule list via WebSocket add_to_schedule message.
     */
    suspend fun addSongToSchedule(song: Song): Result<Unit> {
        return apiRunCatching {
            val payload = json.encodeToString(song.toProjectRequest())
            Logger.d(TAG, "addSongToSchedule ▶ WS add_to_schedule  payload: $payload")
            wsService.sendAction(WsMessageType.ADD_TO_SCHEDULE, payload).getOrThrow()
        }.onFailure { e -> Logger.e(TAG, "addSongToSchedule — FAILED: ${e.message}", e) }
    }

    private fun Song.toProjectRequest() = ProjectSongRequest(
        item = SongItemPayload(
            id          = number,
            songNumber  = number.toIntOrNull() ?: 0,
            title       = title,
            songbook    = bookName,
            displayText = if (number.isNotBlank()) "$number - $title" else title
        )
    )

    private fun HttpRequestBuilder.applyApiKey() {
        val key = settings.apiKey
        if (key.isNotBlank()) header(ApiConstants.API_KEY_HEADER, key)
        header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
    }

    /** Releases the underlying HTTP and WebSocket clients. Call when the owning ViewModel is cleared. */
    fun closeClient() {
        Logger.d(TAG, "closeClient — closing HTTP and WebSocket clients")
        client.close()
        wsService.closeClient()
    }
}

