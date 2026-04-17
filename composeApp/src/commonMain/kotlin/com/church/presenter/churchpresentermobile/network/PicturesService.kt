package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.PictureScheduleAddRequest
import com.church.presenter.churchpresentermobile.model.PictureSchedulePayload
import com.church.presenter.churchpresentermobile.model.PictureSelectRequest
import com.church.presenter.churchpresentermobile.model.PicturesFolder
import com.church.presenter.churchpresentermobile.model.UploadPhotoResponse
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

private const val TAG = "PicturesService"

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Handles REST communication with the pictures endpoint.
 *
 * @param settings The shared [AppSettings] used to build the base URL.
 */
class PicturesService(private val settings: AppSettings) {
    private val client: HttpClient = createHttpClient()
    /** WebSocket service for approval-required actions (add-to-schedule). */
    private val wsService: WebSocketService = WebSocketService(settings)

    init {
        Logger.d(TAG, "PicturesService created — baseUrl=${settings.apiBaseUrl}")
    }

    /**
     * Fetches a pictures folder from the API.
     *
     * - No folderId → `GET /api/pictures`              (current/default folder)
     * - With folderId → `GET /api/pictures/{folderId}` (specific folder from schedule)
     *
     * Resolves all thumbnail URLs to absolute using scheme+host+port.
     */
    suspend fun getPictures(folderId: String? = null): Result<PicturesFolder> {
        val url = if (!folderId.isNullOrBlank())
            "${settings.apiBaseUrl}/${ApiConstants.PICTURES_ENDPOINT}/$folderId"
        else
            "${settings.apiBaseUrl}/${ApiConstants.PICTURES_ENDPOINT}"
        Logger.d(TAG, "getPictures — requesting URL: $url")
        return apiRunCatching {
            val httpResponse = client.get(url) { applyApiKey() }
            val statusCode = httpResponse.status.value
            Logger.d(TAG, "getPictures — HTTP status: ${httpResponse.status}")
            val raw = httpResponse.bodyAsText()
            Logger.d(TAG, "getPictures — raw body (first 300 chars): ${raw.take(300)}")
            if (!httpResponse.status.isSuccess()) {
                throw Exception("HTTP $statusCode — ${raw.take(200)}")
            }
            val folder = json.decodeFromString<PicturesFolder>(raw)
            // Thumbnail URLs are root-relative — prefix with scheme+host+port only (not /api)
            val imageBase = "http://${settings.host}:${settings.port}"
            val resolved = folder.copy(
                images = folder.images?.map { image ->
                    image.copy(thumbnailUrl = image.thumbnailUrl?.let { "$imageBase$it" })
                }
            )
            Logger.d(TAG, "getPictures — parsed ${resolved.totalImages} images in '${resolved.displayName}'")
            resolved
        }.onFailure { e ->
            Logger.e(TAG, "getPictures — FAILED for URL $url: ${e.message}", e)
        }
    }

    /**
     * Sends POST /api/pictures/select to display the given image on screen.
     *
     * [fileName] is the primary identifier — the desktop server resolves the file by name,
     * making selection immune to any index-ordering difference between clients.
     * [indexFallback] is included in the request body **only** when [fileName] is null, as a
     * last-resort for servers that do not yet support name-based resolution.
     *
     * @param folderId      The folder ID from the current [PicturesFolder].
     * @param fileName      The exact filename; used as the primary identifier when non-null.
     * @param indexFallback Server image index; sent only when [fileName] is null.
     */
    suspend fun selectPicture(folderId: String, fileName: String?, indexFallback: Int? = null): Result<Unit> {
        return apiRunCatching {
            val payload = json.encodeToString(PictureSelectRequest(folderId = folderId, index = indexFallback))
            Logger.d(TAG, "selectPicture ▶ WS select_picture  folderId=$folderId  index=$indexFallback  payload=$payload")
            wsService.sendAction(WsMessageType.SELECT_PICTURE, payload, fireAndForget = true).getOrThrow()
        }.onFailure { e ->
            Logger.e(TAG, "selectPicture — FAILED: ${e.message}", e)
        }
    }

    /** Tells the presenter to clear the display (show nothing) via WebSocket clear. */
    suspend fun clearDisplay(): Result<Unit> {
        Logger.d(TAG, "clearDisplay ▶ WS clear")
        return apiRunCatching {
            wsService.sendAction(WsMessageType.CLEAR, "", fireAndForget = true).getOrThrow()
        }.onFailure { e -> Logger.e(TAG, "clearDisplay — FAILED: ${e.message}", e) }
    }

    /**
     * Adds a picture to the schedule via POST /api/schedule/add.
     *
     * @param folderId    The folder containing the image.
     * @param imageIndex  Zero-based index of the image.
     * @param displayText Human-readable label shown in the schedule.
     */
    suspend fun addToSchedule(folderId: String, imageIndex: Int, displayText: String): Result<Unit> {
        return apiRunCatching {
            val payload = json.encodeToString(
                PictureScheduleAddRequest(PictureSchedulePayload(folderId = folderId, imageIndex = imageIndex, displayText = displayText))
            )
            Logger.d(TAG, "addToSchedule ▶ WS add_to_schedule  payload=$payload")
            wsService.sendAction(WsMessageType.ADD_TO_SCHEDULE, payload).getOrThrow()
        }.onFailure { e -> Logger.e(TAG, "addToSchedule — FAILED: ${e.message}", e) }
    }

    /**
     * Uploads a device photo to the server.
     *
     * Encodes [imageBytes] as a base64 data-URI with the correct MIME type derived from
     * [fileName]'s extension (jpg/jpeg/png/gif/bmp/webp/heic/heif are all recognised) and
     * POSTs `{ "name": "<fileName>", "data": "data:<mime>;base64,…" }` to
     * `POST /api/pictures/upload`.
     *
     * The server saves the file, accumulates it in the shared "Device Photos" folder, and
     * returns `{ "ok": true, "folder-id": "device_uploads", "image-index": <N> }`.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun uploadPhoto(
        imageBytes: ByteArray,
        fileName: String,
    ): Result<UploadPhotoResponse> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.PICTURES_UPLOAD_ENDPOINT}"
        val encoded = Base64.encode(imageBytes)
        val mimeType = mimeTypeForExtension(fileName.substringAfterLast('.', "jpg").lowercase())
        val dataUri = "data:$mimeType;base64,$encoded"
        val body = """{"name":${json.encodeToString(fileName)},"data":${json.encodeToString(dataUri)}}"""
        Logger.d(TAG, "uploadPhoto ▶ POST $url  name=$fileName  mime=$mimeType  bytes=${imageBytes.size}")
        return apiRunCatching {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
                applyApiKey()
            }
            val raw = response.bodyAsText()
            Logger.d(TAG, "uploadPhoto ◀ status=${response.status}  body=${raw.take(200)}")
            if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value} — ${raw.take(200)}")
            json.decodeFromString<UploadPhotoResponse>(raw)
        }.onFailure { e -> Logger.e(TAG, "uploadPhoto — FAILED: ${e.message}", e) }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyApiKey() {
        val key = settings.apiKey
        if (key.isNotBlank()) header(ApiConstants.API_KEY_HEADER, key)
        header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
    }

    fun closeClient() {
        Logger.d(TAG, "closeClient")
        client.close()
        wsService.closeClient()
    }
}

/** Maps a lowercase file extension to the appropriate image MIME type. */
private fun mimeTypeForExtension(ext: String): String = when (ext) {
    "png"        -> "image/png"
    "gif"        -> "image/gif"
    "bmp"        -> "image/bmp"
    "webp"       -> "image/webp"
    "heic"       -> "image/heic"
    "heif"       -> "image/heif"
    else         -> "image/jpeg"   // jpg, jpeg, unknown → treat as JPEG
}
