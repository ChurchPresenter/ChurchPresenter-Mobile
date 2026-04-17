package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleBooksResponse
import com.church.presenter.churchpresentermobile.model.BibleChapterResponse
import com.church.presenter.churchpresentermobile.model.BibleItemPayload
import com.church.presenter.churchpresentermobile.model.BibleSelectRequest
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.ProjectBibleRequest
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "BibleService"

/** Lenient JSON parser — ignores unknown keys and is tolerant of malformed input. */
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/** Minimal model for the `{"ok":bool,"reason":"...","error":"..."}` shape the API returns. */
@Serializable
private data class BibleApiResponseBody(
    val ok: Boolean? = null,
    val reason: String? = null,
    val error: String? = null,
)

/**
 * Throws [ApiException] when the response indicates failure —
 * either via a non-2xx HTTP status or an `ok:false` body.
 */
private fun checkApiResponse(statusCode: Int, rawBody: String) {
    val parsed = runCatching { json.decodeFromString<BibleApiResponseBody>(rawBody) }.getOrNull()
    val isOk = parsed?.ok ?: (statusCode in 200..299)
    if (!isOk) {
        throw ApiException(
            httpStatus = statusCode,
            reason     = parsed?.reason ?: parsed?.error,
        )
    }
}

/**
 * Handles all REST communication with the Bible endpoints of the ChurchPresenter API.
 *
 * @param settings The current [AppSettings] used to build the base URL and supply the API key.
 */
class BibleService(private val settings: AppSettings) {
    private val client: HttpClient = createHttpClient()
    /** WebSocket service for approval-required actions (project / add-to-schedule). */
    private val wsService: WebSocketService = WebSocketService(settings)

    init {
        Logger.d(TAG, "BibleService created — host=${settings.host} port=${settings.port} baseUrl=${settings.apiBaseUrl}")
    }

    /**
     * Fetches the list of available Bible books from the server.
     */
    suspend fun getBooks(): Result<List<BibleBook>> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.BIBLE_ENDPOINT}"
        Logger.d(TAG, "getBooks — requesting URL: $url")
        return apiRunCatching {
            val httpResponse = client.get(url) { applyApiKey() }
            val statusCode = httpResponse.status.value
            Logger.d(TAG, "getBooks — HTTP status: ${httpResponse.status}")
            // Read body once as text — avoids double-read and works regardless of Content-Type
            val raw = httpResponse.bodyAsText()
            Logger.d(TAG, "getBooks — raw body (first 500 chars): ${raw.take(500)}")
            if (!httpResponse.status.isSuccess()) {
                throw Exception("HTTP $statusCode — ${raw.take(200)}")
            }
            val response = json.decodeFromString<BibleBooksResponse>(raw)
            val books = response.allBooks
            Logger.d(TAG, "getBooks — parsed ${books.size} books")
            books
        }.onFailure { e ->
            Logger.e(TAG, "getBooks — FAILED for URL $url: ${e.message}", e)
        }
    }

    /**
     * Fetches all verses for a book/chapter using query parameters.
     *
     * Calls GET /api/bible?book=[bookNumber]&chapter=[chapter]
     *
     * @param bookNumber The 1-based book index in the books list.
     * @param chapter The 1-based chapter number.
     */
    suspend fun getChapter(bookNumber: Int, chapter: Int): Result<List<BibleVerse>> {
        val baseUrl = "${settings.apiBaseUrl}/${ApiConstants.BIBLE_ENDPOINT}"
        val url = "$baseUrl?book=$bookNumber&chapter=$chapter"
        Logger.d(TAG, "getChapter — requesting URL: $url")
        return apiRunCatching {
            val httpResponse = client.get(baseUrl) {
                parameter("book", bookNumber)
                parameter("chapter", chapter)
                applyApiKey()
            }
            val statusCode = httpResponse.status.value
            Logger.d(TAG, "getChapter — HTTP status: ${httpResponse.status}")
            val raw = httpResponse.bodyAsText()
            Logger.d(TAG, "getChapter — raw body (first 500 chars): ${raw.take(500)}")
            if (!httpResponse.status.isSuccess()) {
                throw Exception("HTTP $statusCode for $url — ${raw.take(200)}")
            }
            val response = json.decodeFromString<BibleChapterResponse>(raw)
            val verses = response.allVerses
            Logger.d(TAG, "getChapter — parsed ${verses.size} verses")
            verses
        }.onFailure { e ->
            Logger.e(TAG, "getChapter — FAILED for URL $url: ${e.message}", e)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyApiKey() {
        val key = settings.apiKey
        if (key.isNotBlank()) {
            Logger.d(TAG, "applyApiKey — adding ${ApiConstants.API_KEY_HEADER} header")
            header(ApiConstants.API_KEY_HEADER, key)
        }
        header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
    }

    /**
     * Sends a single Bible verse directly to the projection output via POST /api/bible/select.
     * Fires immediately — no approval dialog is shown on the desktop.
     *
     * @param bookName    Display name of the book (e.g. "Genesis").
     * @param chapter     1-based chapter number.
     * @param verseNumber 1-based verse number.
     * @param verseText   Text to display on screen.
     * @param verseRange  Optional range string for multi-verse display (e.g. "1-3").
     */
    suspend fun selectBibleVerse(
        bookName: String,
        chapter: Int,
        verseNumber: Int,
        verseText: String,
        verseRange: String? = null
    ): Result<Unit> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.BIBLE_SELECT_ENDPOINT}"
        return apiRunCatching {
            val payload = BibleSelectRequest(
                bookName    = bookName,
                chapter     = chapter,
                verseNumber = verseNumber,
                verseText   = verseText,
                verseRange  = verseRange
            )
            val body = json.encodeToString(payload)
            Logger.d(TAG, "selectBibleVerse ▶ POST $url  payload=$body")
            val response = client.post(url) {
                applyApiKey()
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val responseBody = response.bodyAsText()
            Logger.d(TAG, "selectBibleVerse ◀ status=${response.status}  body=$responseBody")
            checkApiResponse(response.status.value, responseBody)
            Unit
        }.onFailure { e -> Logger.e(TAG, "selectBibleVerse — FAILED: ${e.message}", e) }
    }

    /**
     * Tells the presenter to clear the display (show nothing).
     * Called when the user taps "Stop Projecting".
     */
    suspend fun clearDisplay(): Result<Unit> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.CLEAR_ENDPOINT}"
        Logger.d(TAG, "clearDisplay ▶ POST $url")
        return apiRunCatching {
            val response = client.post(url) { applyApiKey() }
            val responseBody = response.bodyAsText()
            Logger.d(TAG, "clearDisplay ◀ status=${response.status}  body=$responseBody")
            checkApiResponse(response.status.value, responseBody)
            Unit
        }.onFailure { e -> Logger.e(TAG, "clearDisplay — FAILED: ${e.message}", e) }
    }

    /**
     * Sends a single Bible verse to the presenter window immediately via POST /api/project.
     * Requires desktop approval before the verse appears on screen.
     *
     * @param bookName    Display name of the book (e.g. "Genesis").
     * @param chapter     1-based chapter number.
     * @param verseNumber 1-based verse number.
     * @param verseText   Text to display on screen.
     */
    suspend fun projectBibleVerse(
        bookName: String,
        chapter: Int,
        verseNumber: Int,
        verseText: String
    ): Result<Unit> {
        return apiRunCatching {
            val payload = json.encodeToString(
                ProjectBibleRequest(
                    item = BibleItemPayload(
                        bookName    = bookName,
                        chapter     = chapter,
                        verseNumber = verseNumber,
                        verseText   = verseText
                    )
                )
            )
            Logger.d(TAG, "projectBibleVerse ▶ WS project  payload=$payload")
            wsService.sendAction(WsMessageType.PROJECT, payload).getOrThrow()
        }.onFailure { e -> Logger.e(TAG, "projectBibleVerse — FAILED: ${e.message}", e) }
    }

    /**
     * Adds selected Bible verses to the schedule as a single range item (does not go live).
     *
     * Single verse  → POST /api/schedule/add  { item: { bookName, chapter, verseNumber, verseText } }
     * Multiple verses → POST /api/schedule/add  { item: { …, verseRange: "1-3" } }
     *   verseRange is "start-end" for contiguous selections (e.g. "1-3")
     *   or comma-separated for sparse selections (e.g. "1,3,5").
     *
     * @param bookName Display name of the book (e.g. "Genesis").
     * @param chapter  1-based chapter number.
     * @param verses   The [BibleVerse] objects to add, in order.
     */
    suspend fun addBibleToSchedule(
        bookName: String,
        chapter: Int,
        verses: List<BibleVerse>
    ): Result<Unit> {
        if (verses.isEmpty()) return Result.success(Unit)

        val sorted = verses.sortedBy { it.number }
        val numbers = sorted.map { it.number }

        val verseRange: String? = if (sorted.size == 1) {
            null
        } else {
            val isContiguous = numbers.zipWithNext().all { (a, b) -> b == a + 1 }
            if (isContiguous) "${numbers.first()}-${numbers.last()}"
            else numbers.joinToString(",")
        }

        val combinedText = sorted.joinToString("\n") { it.displayText }

        return apiRunCatching {
            val payload = json.encodeToString(
                ProjectBibleRequest(
                    item = BibleItemPayload(
                        bookName    = bookName,
                        chapter     = chapter,
                        verseNumber = numbers.first(),
                        verseText   = combinedText,
                        verseRange  = verseRange
                    )
                )
            )
            Logger.d(TAG, "addBibleToSchedule ▶ WS add_to_schedule  verseRange=$verseRange  payload=$payload")
            wsService.sendAction(WsMessageType.ADD_TO_SCHEDULE, payload).getOrThrow()
        }.onFailure { e -> Logger.e(TAG, "addBibleToSchedule — FAILED: ${e.message}", e) }
    }

    /** Releases the underlying HTTP client. Call when the owning ViewModel is cleared. */
    fun closeClient() {
        Logger.d(TAG, "closeClient — closing HTTP and WebSocket clients")
        client.close()
        wsService.closeClient()
    }
}
