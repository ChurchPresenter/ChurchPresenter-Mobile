package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "WebSocketService"

/** Message type constants matching the server's WebSocket protocol. */
object WsMessageType {
    const val PROJECT          = "project"
    const val ADD_TO_SCHEDULE  = "add_to_schedule"
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
     * Sends an action message over a WebSocket connection and waits for the server's
     * acknowledgment frame.
     *
     * @param type        Message type (e.g. [WsMessageType.PROJECT]).
     * @param payloadJson The JSON-serialised payload body (will be double-encoded as a string).
     */
    suspend fun sendAction(type: String, payloadJson: String): Result<Unit> {
        val url = settings.wsBaseUrl
        Logger.d(TAG, "sendAction ▶ type=$type  url=$url")
        return apiRunCatching {
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

                // Wait for server acknowledgment
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val responseText = frame.readText()
                        Logger.d(TAG, "sendAction ◀ received: $responseText")
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
                }
                close()
            }
            Logger.d(TAG, "sendAction ◀ completed  type=$type")
        }.onFailure { e ->
            Logger.e(TAG, "sendAction — FAILED type=$type: ${e.message}", e)
        }
    }

    /** Releases the underlying WebSocket HTTP client. Call when the owning service is closed. */
    fun closeClient() {
        Logger.d(TAG, "closeClient — closing WebSocket client")
        client.close()
    }
}

