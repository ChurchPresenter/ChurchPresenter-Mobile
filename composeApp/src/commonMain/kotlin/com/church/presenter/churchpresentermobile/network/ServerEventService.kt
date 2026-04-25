package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.util.CrashReporting
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "ServerEventService"
private const val RECONNECT_DELAY_MS = 3_000L

/** Server-push event envelope — `{ "type": "...", "payload": "..." }`. */
@Serializable
private data class ServerEvent(val type: String, val payload: String = "")

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Maintains a persistent WebSocket connection to the server and exposes
 * server-push events as [SharedFlow]s.
 *
 * On connect the server immediately pushes the current state (songs, schedule,
 * bible, presentations, pictures). Subsequent pushes arrive whenever the
 * desktop app changes state — e.g. when a schedule item is approved/added.
 *
 * Call [listen] inside a coroutine scope (e.g. `viewModelScope`); it loops
 * forever, reconnecting after any disconnection, until the scope is cancelled.
 *
 * @param settings Shared [AppSettings] supplying host, port, API key, and device ID.
 */
class ServerEventService(private val settings: AppSettings) {
    private val client: HttpClient = createWebSocketClient()

    private val _scheduleUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits [Unit] each time the server pushes a `schedule_updated` event. */
    val scheduleUpdated: SharedFlow<Unit> = _scheduleUpdated.asSharedFlow()

    private val _songsUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits [Unit] each time the server pushes a `songs_updated` event. */
    val songsUpdated: SharedFlow<Unit> = _songsUpdated.asSharedFlow()

    /**
     * Connects to `ws://<host>:<port>/ws` and listens for server-push events.
     * Automatically reconnects after any error or disconnection.
     * Suspends until the calling coroutine is cancelled.
     */
    suspend fun listen() {
        // Track whether the very first connection to the current server URL has ever
        // succeeded. Once connected successfully we stop sending "could not connect"
        // non-fatals so that normal reconnections (e.g. brief network blip) don't
        // flood Crashlytics.
        var everConnected = false
        while (true) {
            try {
                Logger.d(TAG, "listen — connecting to ${settings.wsBaseUrl}")
                client.webSocket(
                    urlString = settings.wsBaseUrl,
                    request = {
                        val key = settings.apiKey
                        if (key.isNotBlank()) headers.append(ApiConstants.API_KEY_HEADER, key)
                        headers.append(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
                    }
                ) {
                    Logger.d(TAG, "listen — connected")
                    everConnected = true
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()
                        val event = runCatching {
                            json.decodeFromString<ServerEvent>(text)
                        }.getOrNull() ?: continue

                        Logger.d(TAG, "listen — event type=${event.type}")
                        when (event.type) {
                            "schedule_updated"     -> _scheduleUpdated.tryEmit(Unit)
                            "songs_updated"        -> _songsUpdated.tryEmit(Unit)
                        }
                    }
                    Logger.d(TAG, "listen — session closed normally")
                }
            } catch (e: CancellationException) {
                Logger.d(TAG, "listen — cancelled")
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, "listen — connection lost: ${e.message} — retrying in ${RECONNECT_DELAY_MS}ms", e)
                // Record to Crashlytics:
                //  • Always on the first failure (server unreachable on initial connect).
                //  • Also if we were previously connected and then lost the session
                //    unexpectedly (not a clean close).
                val errorMsg = e.message?.take(200) ?: "unknown"
                if (!everConnected) {
                    CrashReporting.log("[$TAG] listen — initial connect FAILED (${e::class.simpleName}): $errorMsg url=${settings.wsBaseUrl}")
                    CrashReporting.setCustomKey("network_tag",        TAG)
                    CrashReporting.setCustomKey("network_operation",  "listen/connect")
                    CrashReporting.setCustomKey("network_error_type", e::class.simpleName ?: "Exception")
                    CrashReporting.setCustomKey("network_error_msg",  errorMsg)
                    CrashReporting.setCustomKey("ws_server_url",      settings.wsBaseUrl)
                    CrashReporting.recordException(e)
                } else {
                    // After a successful connection, only breadcrumb-log drops; avoid
                    // spamming non-fatals on every temporary network blip.
                    CrashReporting.log("[$TAG] listen — session dropped (${e::class.simpleName}): $errorMsg")
                }
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    /** Releases the underlying WebSocket client. Call when the owning ViewModel is cleared. */
    fun closeClient() {
        Logger.d(TAG, "closeClient")
        client.close()
    }
}

