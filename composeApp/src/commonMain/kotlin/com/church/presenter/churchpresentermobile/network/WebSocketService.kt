package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

private const val TAG = "WebSocketService"

/** Maximum number of attempts for a single [WebSocketService.sendAction] call. */
private const val WS_MAX_ATTEMPTS = 3
/** Delay between retry attempts in milliseconds. */
private const val WS_RETRY_DELAY_MS = 500L

/** Message type constants matching the server's WebSocket protocol. */
object WsMessageType {
    // Approval-required actions
    const val PROJECT             = "project"
    const val ADD_TO_SCHEDULE     = "add_to_schedule"
    const val ADD_BATCH_TO_SCHEDULE = "add_batch_to_schedule"
    // Instant actions (no approval dialog)
    const val SELECT_SONG         = "select_song"
    const val SELECT_SONG_SECTION = "select_song_section"
    const val SELECT_BIBLE_VERSE  = "select_bible_verse"
    const val SELECT_PICTURE      = "select_picture"
    const val SELECT_SLIDE        = "select_slide"
    const val CLEAR               = "clear"
}

/** Outbound WebSocket message envelope. */
@Serializable
private data class WsOutboundMessage(
    val type: String,
    /** Double-serialised payload — the server expects a JSON *string*, not a nested object. */
    val payload: String,
)

/** Inbound WebSocket response from the server. */
@Serializable
private data class WsResponse(
    val ok: Boolean? = null,
    val reason: String? = null,
    val error: String? = null,
)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Manages WebSocket action messaging with the ChurchPresenter server.
 *
 * Opens a fresh WebSocket session for each action and closes it after
 * receiving an acknowledgment (or on timeout / error).
 *
 * The server WebSocket endpoint is `ws://<host>:<port>/ws`.
 * Messages use the envelope:
 * ```json
 * { "type": "project", "payload": "<json-encoded string>" }
 * ```
 * where `payload` is the same body that would be sent via the corresponding REST endpoint,
 * serialised as a JSON *string* value (double-encoded).
 *
 * @param settings The current [AppSettings] supplying host, port, API key, and device ID.
 */
class WebSocketService(private val settings: AppSettings) {
    private val client: HttpClient = createWebSocketClient()

    init {
        Logger.d(TAG, "WebSocketService created — wsUrl=${settings.wsBaseUrl}")
    }

    /**
     * Sends an action message over a WebSocket connection.
     *
     * @param type           Message type (e.g. [WsMessageType.PROJECT]).
     * @param payloadJson    The JSON-serialised payload body (will be double-encoded as a string).
     * @param fireAndForget  When `true`, sends the frame and closes immediately without waiting
     *                       for a server response. Use for instant commands that need no approval
     *                       ([WsMessageType.SELECT_SONG], [WsMessageType.CLEAR], etc.).
     *                       When `false` (default), waits for an `{"ok":…}` response frame —
     *                       required for approval-gated commands ([WsMessageType.PROJECT],
     *                       [WsMessageType.ADD_TO_SCHEDULE], etc.).
     */
    suspend fun sendAction(type: String, payloadJson: String, fireAndForget: Boolean = false): Result<Unit> {
        val url = settings.wsBaseUrl
        Logger.d(TAG, "sendAction ▶ type=$type  url=$url")
        var lastException: Throwable? = null
        repeat(WS_MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                Logger.d(TAG, "sendAction — retry attempt ${attempt + 1}/$WS_MAX_ATTEMPTS  type=$type")
                delay(WS_RETRY_DELAY_MS * attempt)
            }
            val result = apiRunCatching {
                client.webSocket(
                    urlString = url,
                    request = {
                        val key = settings.apiKey
                        if (key.isNotBlank()) {
                            headers.append(ApiConstants.API_KEY_HEADER, key)
                        }
                        headers.append(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
                    }
                ) {
                    val envelope = json.encodeToString(
                        WsOutboundMessage(type = type, payload = payloadJson)
                    )
                    Logger.d(TAG, "sendAction ▶ sending frame: ${envelope.take(300)}")
                    send(Frame.Text(envelope))

                    if (fireAndForget) {
                        // Instant command — no response expected, close immediately.
                        close()
                    } else {
                        // Approval-gated command — wait for {"ok": true/false} response,
                        // skipping any server-push event frames sent on connect.
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val responseText = frame.readText()
                            Logger.d(TAG, "sendAction ◀ received: ${responseText.take(200)}")
                            val jsonObj = runCatching {
                                json.parseToJsonElement(responseText).jsonObject
                            }.getOrNull() ?: continue

                            // Skip server-push events (have "type" but no "ok")
                            if (jsonObj.containsKey("type") && !jsonObj.containsKey("ok")) {
                                Logger.d(TAG, "sendAction — skipping server-push event: ${responseText.take(80)}")
                                continue
                            }

                            val response = runCatching {
                                json.decodeFromString<WsResponse>(responseText)
                            }.getOrNull()
                            val isOk = response?.ok ?: true
                            if (!isOk) {
                                throw ApiException(
                                    httpStatus = 400,
                                    reason = response?.reason ?: response?.error,
                                )
                            }
                            break
                        }
                        close()
                    }
                }
                Logger.d(TAG, "sendAction ◀ completed  type=$type")
            }
            if (result.isSuccess) return result
            lastException = result.exceptionOrNull()
            // Don't retry on application-level rejections (server said ok=false)
            if (lastException is ApiException) {
                Logger.e(TAG, "sendAction — server rejection, not retrying: ${lastException?.message}")
                return result
            }
            Logger.e(TAG, "sendAction — attempt ${attempt + 1} FAILED type=$type: ${lastException?.message}", lastException)
        }
        return Result.failure(lastException ?: Exception("sendAction failed after $WS_MAX_ATTEMPTS attempts"))
    }

    /** Releases the underlying WebSocket HTTP client. Call when the owning service is closed. */
    fun closeClient() {
        Logger.d(TAG, "closeClient — closing WebSocket client")
        client.close()
    }
}

