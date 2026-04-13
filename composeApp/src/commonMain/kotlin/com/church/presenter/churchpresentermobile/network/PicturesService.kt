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
    /** Separate client with no request/socket timeout for approval-required POSTs. */
    private val actionClient: HttpClient = createActionHttpClient()

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
            val imageBase = "https://${settings.host}:${settings.port}"
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
     * Sends POST /api/pictures/select to display the given image index on screen.
     *
     * @param folderId The folder ID from the current [PicturesFolder].
     * @param index    The zero-based image index to display.
     */
    suspend fun selectPicture(folderId: String, index: Int): Result<Unit> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.PICTURES_SELECT_ENDPOINT}"
        val body = json.encodeToString(PictureSelectRequest(folderId = folderId, index = index))
        Logger.d(TAG, "selectPicture ▶ POST $url  payload=$body")
        return apiRunCatching {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
                applyApiKey()
            }
            val responseBody = response.bodyAsText()
            Logger.d(TAG, "selectPicture ◀ status=${response.status}  body=$responseBody")
        }.onFailure { e ->
            Logger.e(TAG, "selectPicture — FAILED: ${e.message}", e)
        }
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
     * Adds a picture to the schedule via POST /api/schedule/add.
     *
     * @param folderId    The folder containing the image.
     * @param imageIndex  Zero-based index of the image.
     * @param displayText Human-readable label shown in the schedule.
     */
    suspend fun addToSchedule(folderId: String, imageIndex: Int, displayText: String): Result<Unit> {
        val url = "${settings.apiBaseUrl}/${ApiConstants.SCHEDULE_ADD_ENDPOINT}"
        return apiRunCatching {
            val body = json.encodeToString(
                PictureScheduleAddRequest(PictureSchedulePayload(folderId = folderId, imageIndex = imageIndex, displayText = displayText))
            )
            Logger.d(TAG, "addToSchedule ▶ POST $url  payload=$body")
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
    suspend fun uploadPhoto(imageBytes: ByteArray, fileName: String): Result<UploadPhotoResponse> {
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
        actionClient.close()
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

