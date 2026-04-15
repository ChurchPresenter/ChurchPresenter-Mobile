package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.model.PresentationScheduleAddRequest
import com.church.presenter.churchpresentermobile.model.PresentationSchedulePayload
import com.church.presenter.churchpresentermobile.model.PresentationsResponse
import com.church.presenter.churchpresentermobile.model.SelectSlideRequest
import com.church.presenter.churchpresentermobile.model.UploadPresentationResponse
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "PresentationService"

/** Lenient JSON parser — ignores unknown keys and is tolerant of malformed input. */
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true  // ensure fields with default values (e.g. type="presentation") are always included
}

/**
 * Handles all REST communication with the presentations endpoint.
 *
 * @param settings The current [AppSettings] used to build the base URL and supply the API key.
 */
class PresentationService(private val settings: AppSettings) {
    private val client: HttpClient = createHttpClient()
    /** Separate client with no request/socket timeout for approval-required POSTs. */
    private val actionClient: HttpClient = createActionHttpClient()

    init {
        Logger.d(TAG, "PresentationService created — baseUrl=${settings.apiBaseUrl}")
    }

    /**
     * Fetches a single presentation by its ID from GET /api/presentations/{id}.
     * Used when navigating from a schedule item.
     *
     * @param id The presentation ID (the schedule item `id` field).
     */
    suspend fun getPresentationById(id: String): Result<Presentation> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.PRESENTATIONS_ENDPOINT}/$id"
        Logger.d(TAG, "getPresentationById — requesting URL: $url")
        return apiRunCatching {
            val httpResponse = client.get(url) { applyApiKey() }
            val statusCode = httpResponse.status.value
            Logger.d(TAG, "getPresentationById — HTTP status: ${httpResponse.status}")
            val raw = httpResponse.bodyAsText()
            Logger.d(TAG, "getPresentationById — raw body (first 500 chars): ${raw.take(500)}")
            if (!httpResponse.status.isSuccess()) {
                throw Exception("HTTP $statusCode — ${raw.take(200)}")
            }
            val imageBase = "https://${settings.host}:${settings.port}"
            val presentation = json.decodeFromString<Presentation>(raw)
            presentation.copy(
                slides = presentation.slides?.map { slide ->
                    slide.copy(thumbnailUrl = slide.thumbnailUrl?.let { "$imageBase$it" })
                }
            ).also {
                Logger.d(TAG, "getPresentationById — parsed '${it.displayName}' with ${it.totalSlides} slides")
            }
        }.onFailure { e ->
            Logger.e(TAG, "getPresentationById — FAILED for URL $url: ${e.message}", e)
        }
    }

    /**
     * Fetches the list of presentations from GET /api/presentations.
     * Handles both a wrapped `{"presentations": [...]}` response and a bare `[...]` array.
     */
    suspend fun getPresentations(): Result<List<Presentation>> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.PRESENTATIONS_ENDPOINT}"
        Logger.d(TAG, "getPresentations — requesting URL: $url")
        return apiRunCatching {
            val httpResponse = client.get(url) { applyApiKey() }
            val statusCode = httpResponse.status.value
            Logger.d(TAG, "getPresentations — HTTP status: ${httpResponse.status}")
            val raw = httpResponse.bodyAsText()
            Logger.d(TAG, "getPresentations — raw body (first 500 chars): ${raw.take(500)}")
            if (!httpResponse.status.isSuccess()) {
                throw Exception("HTTP $statusCode — ${raw.take(200)}")
            }
            // Thumbnail URLs are root-relative (e.g. /api/presentations/…/slides/0),
            // so prefix with scheme+host+port only — NOT apiBaseUrl which already appends /api.
            val imageBase = "https://${settings.host}:${settings.port}"
            val presentations = json.decodeFromString<PresentationsResponse>(raw)
                .allPresentations
                .map { presentation ->
                    presentation.copy(
                        slides = presentation.slides?.map { slide ->
                            slide.copy(thumbnailUrl = slide.thumbnailUrl?.let { "$imageBase$it" })
                        }
                    )
                }
            Logger.d(TAG, "getPresentations — parsed ${presentations.size} presentations")
            presentations
        }.onFailure { e ->
            Logger.e(TAG, "getPresentations — FAILED for URL $url: ${e.message}", e)
        }
    }

    /**
     * Sends a select request for the given presentation, navigating to a specific slide.
     * Calls POST /api/presentations/{id}/select with {"index": slideIndex} body.
     *
     * @param id         The presentation ID to select.
     * @param slideIndex The zero-based slide index to navigate to.
     */
    suspend fun selectPresentation(id: String, slideIndex: Int): Result<String> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.PRESENTATIONS_ENDPOINT}/$id/${ApiConstants.PRESENTATION_SELECT_ENDPOINT}"
        return apiRunCatching {
            val body = json.encodeToString(SelectSlideRequest(index = slideIndex))
            Logger.d(TAG, "selectPresentation ▶ POST $url  slideIndex=$slideIndex  payload=$body")
            val response = client.post(url) {
                applyApiKey()
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val result = response.bodyAsText()
            Logger.d(TAG, "selectPresentation ◀ status=${response.status}  body=$result")
            result
        }.onFailure { e ->
            Logger.e(TAG, "selectPresentation — FAILED for URL $url: ${e.message}", e)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyApiKey() {
        val key = settings.apiKey
        if (key.isNotBlank()) {
            header(ApiConstants.API_KEY_HEADER, key)
        }
        header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
    }

    /** Tells the presenter to clear the display (show nothing). POST /api/clear. */
    suspend fun clearDisplay(): Result<Unit> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.CLEAR_ENDPOINT}"
        Logger.d(TAG, "clearDisplay ▶ POST $url")
        return apiRunCatching {
            val response = client.post(url) { applyApiKey() }
            val responseBody = response.bodyAsText()
            Logger.d(TAG, "clearDisplay ◀ status=${response.status}  body=$responseBody")
        }.onFailure { e -> Logger.e(TAG, "clearDisplay — FAILED: ${e.message}", e) }
    }

    /**
     * Adds a presentation to the schedule via POST /api/schedule/add.
     */
    suspend fun addToSchedule(presentation: Presentation): Result<Unit> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.SCHEDULE_ADD_ENDPOINT}"
        return apiRunCatching {
            val body = json.encodeToString(
                PresentationScheduleAddRequest(
                    PresentationSchedulePayload(
                        id          = presentation.displayId,
                        title       = presentation.displayName,
                        displayText = presentation.displayName,
                    )
                )
            )
            Logger.d(TAG, "addToSchedule ▶ POST $url  id=${presentation.displayId}  payload=$body")
            val response = actionClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
                applyApiKey()
            }
            val responseBody = response.bodyAsText()
            Logger.d(TAG, "addToSchedule ◀ status=${response.status}  body=$responseBody")
        }.onFailure { e -> Logger.e(TAG, "addToSchedule — FAILED: ${e.message}", e) }
    }

    /**
     * Uploads a presentation file to the server.
     *
     * Encodes [fileBytes] as a base64 data-URI with the MIME type derived from
     * [fileName]'s extension (.pdf, .pptx, .ppt, .key) and POSTs
     * `{ "name": "<fileName>", "data": "data:<mime>;base64,…" }` to
     * `POST /api/presentations/upload`.
     *
     * Uses [actionClient] (no request/socket timeout) because presentation files are
     * typically much larger than photos — a PDF can be 5–20 MB which, after base64
     * encoding (~33 % overhead), easily exceeds the 15 s timeout on [client].
     *
     * Response parsing is lenient: a minimal `{"ok":true}` is accepted, and
     * missing `id`/`name` fields fall back to null.
     *
     * @param fileBytes Raw bytes of the presentation file.
     * @param fileName  Original file name (used for MIME detection and display).
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun uploadPresentation(fileBytes: ByteArray, fileName: String): Result<UploadPresentationResponse> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.PRESENTATIONS_UPLOAD_ENDPOINT}"
        val encoded = Base64.encode(fileBytes)
        val mimeType = mimeTypeForExtension(fileName.substringAfterLast('.', "pptx").lowercase())
        val dataUri = "data:$mimeType;base64,$encoded"
        val body = """{"name":${json.encodeToString(fileName)},"data":${json.encodeToString(dataUri)}}"""
        Logger.d(TAG, "uploadPresentation ▶ POST $url  name=$fileName  mime=$mimeType  bytes=${fileBytes.size}  encodedBytes=${encoded.length}")
        return apiRunCatching {
            // actionClient has no request/socket timeout — required for large files
            val response = actionClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
                applyApiKey()
            }
            val raw = response.bodyAsText()
            Logger.d(TAG, "uploadPresentation ◀ status=${response.status}  body=${raw.take(300)}")
            if (!response.status.isSuccess()) {
                throw ApiException(
                    httpStatus = response.status.value,
                    reason     = raw.take(200),
                )
            }
            // Parse leniently: accept {"ok":true} with no id/name (both are nullable)
            runCatching { json.decodeFromString<UploadPresentationResponse>(raw) }
                .getOrDefault(UploadPresentationResponse(ok = true))
        }.onFailure { e -> Logger.e(TAG, "uploadPresentation — FAILED: ${e.message}", e) }
    }

    /** Releases the underlying HTTP client. Call when the owning ViewModel is cleared. */
    fun closeClient() {
        Logger.d(TAG, "closeClient — closing HTTP clients")
        client.close()
        actionClient.close()
    }
}

/** Maps a lowercase file extension to the appropriate presentation MIME type. */
private fun mimeTypeForExtension(ext: String): String = when (ext) {
    "pdf"  -> "application/pdf"
    "ppt"  -> "application/vnd.ms-powerpoint"
    "key"  -> "application/x-iwork-keynote-sffkey"
    else   -> "application/vnd.openxmlformats-officedocument.presentationml.presentation" // pptx default
}

