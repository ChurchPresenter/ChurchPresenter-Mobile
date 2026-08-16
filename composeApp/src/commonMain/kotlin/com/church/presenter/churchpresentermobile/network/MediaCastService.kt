package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.MediaItemPayload
import com.church.presenter.churchpresentermobile.model.MediaRequest
import com.church.presenter.churchpresentermobile.model.MediaUploadResponse
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.encodeURLQueryComponent
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "MediaCastService"
// encodeDefaults = true is REQUIRED: mediaType defaults to "url", and without this kotlinx omits
// any field equal to its default — so "mediaType":"url" would never reach the desktop, which then
// falls back to "local" and tries to open the URL as a file path (nothing plays).
private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

/**
 * Sends media to the desktop: a network URL over the shared WebSocket (same
 * `project` / `add_to_schedule` path used by songs, bible, web, …), or an uploaded
 * file over HTTP. Named *Cast* to avoid colliding with the desktop's local
 * media-playback concepts.
 */
class MediaCastService(
    private val settings: AppSettings,
    private val wsService: WsSender,
) {
    /** Separate client with no request/socket timeout — required for large media uploads. */
    private val uploadClient: HttpClient = createActionHttpClient()
    /** Adds the media to the desktop schedule (does not go live). */
    suspend fun addToSchedule(item: MediaItemPayload): Result<Unit> = apiRunCatching {
        val payload = json.encodeToString(MediaRequest(item))
        Logger.d(TAG, "addToSchedule ▶ WS add_to_schedule  payload=$payload")
        wsService.sendAction(WsMessageType.ADD_TO_SCHEDULE, payload).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "addToSchedule — FAILED: ${e.message}", e) }

    /** Projects the media on the desktop screen now (requires operator approval). */
    suspend fun goLive(item: MediaItemPayload): Result<Unit> = apiRunCatching {
        val payload = json.encodeToString(MediaRequest(item))
        Logger.d(TAG, "goLive ▶ WS project  payload=$payload")
        wsService.sendAction(WsMessageType.PROJECT, payload).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "goLive — FAILED: ${e.message}", e) }

    /** Clears whatever is currently on the desktop screen. */
    suspend fun clearScreen(): Result<Unit> = apiRunCatching {
        wsService.sendAction(WsMessageType.CLEAR, "", fireAndForget = true).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "clearScreen — FAILED: ${e.message}", e) }

    // ── Transport controls (fire-and-forget — the desktop echoes new state back) ──

    private suspend fun sendControl(type: String, payload: String = ""): Result<Unit> = apiRunCatching {
        wsService.sendAction(type, payload, fireAndForget = true).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "$type — FAILED: ${e.message}", e) }

    suspend fun playPause() = sendControl(WsMessageType.MEDIA_PLAY_PAUSE)
    suspend fun stop() = sendControl(WsMessageType.MEDIA_STOP)
    suspend fun seekForward() = sendControl(WsMessageType.MEDIA_SEEK_FORWARD)
    suspend fun seekBackward() = sendControl(WsMessageType.MEDIA_SEEK_BACKWARD)
    suspend fun seekTo(positionMs: Long) = sendControl(WsMessageType.MEDIA_SEEK_TO, positionMs.toString())
    suspend fun setVolume(volume: Float) = sendControl(WsMessageType.MEDIA_SET_VOLUME, volume.toString())
    suspend fun muteToggle() = sendControl(WsMessageType.MEDIA_MUTE_TOGGLE)

    /**
     * Streams a media file to the desktop (`POST /api/media/upload?name=…`) as raw bytes so the
     * whole file never sits in memory. The desktop saves it and returns the absolute local path
     * to project as a local [MediaItemPayload]. [onProgress] reports 0f‑1f as bytes are sent.
     */
    suspend fun uploadMedia(picked: PickedMediaFile, onProgress: (Float) -> Unit): Result<MediaUploadResponse> {
        val encodedName = picked.fileName.encodeURLQueryComponent()
        val url = "${settings.apiBaseUrl}/${ApiConstants.MEDIA_UPLOAD_ENDPOINT}?name=$encodedName"
        val total = picked.sizeBytes
        Logger.d(TAG, "uploadMedia ▶ POST $url  name=${picked.fileName}  size=$total")
        return apiRunCatching {
            val response = uploadClient.post(url) {
                val key = settings.apiKey
                if (key.isNotBlank()) header(ApiConstants.API_KEY_HEADER, key)
                header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
                setBody(object : OutgoingContent.WriteChannelContent() {
                    override val contentType = ContentType.Application.OctetStream
                    override val contentLength = total.takeIf { it > 0 }
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        picked.streamTo(channel) { sent ->
                            if (total > 0) onProgress((sent.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                })
            }
            val raw = response.bodyAsText()
            Logger.d(TAG, "uploadMedia ◀ status=${response.status}  body=${raw.take(200)}")
            response.ensureSuccess(raw)
            json.decodeFromString<MediaUploadResponse>(raw)
        }.onFailure { e -> Logger.e(TAG, "uploadMedia — FAILED: ${e.message}", e) }
    }
}
