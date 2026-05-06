package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.util.CrashReporting
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

private const val TAG = "ServerEventService"
private const val INITIAL_RECONNECT_DELAY_MS = 2_000L
private const val MAX_RECONNECT_DELAY_MS = 30_000L

/** Maximum number of attempts for a single [ServerEventService.sendAction] call. */
private const val WS_MAX_ATTEMPTS = 3
/** Delay between retry attempts in milliseconds. */
private const val WS_RETRY_DELAY_MS = 500L
/** Timeout for waiting for an approval response (2 minutes — approval dialog may stay open). */
private const val ACTION_RESPONSE_TIMEOUT_MS = 120_000L
/** Maximum time sendAction() will wait for the WebSocket connection to become ready. */
private const val WS_CONNECTION_TIMEOUT_MS = 10_000L

/** Server-push event envelope — `{ "type": "...", "payload": "..." }`. */
@Serializable
private data class ServerEvent(val type: String, val payload: String = "")

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
    const val BIBLE_HOLD          = "bible_hold"
}

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Maintains a persistent WebSocket connection to the server and exposes
 * server-push events as [SharedFlow]s. Also routes outbound action messages
 * over the same persistent connection, avoiding per-action handshake overhead.
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

    /** The currently active WebSocket session, or null when disconnected. */
    private var activeSession: DefaultWebSocketSession? = null

    /** Signals that [activeSession] is set and the connection is usable. */
    private val connectionReady = MutableStateFlow(false)

    /** Serialises approval-gated actions so only one waits for a response at a time. */
    private val actionMutex = Mutex()

    /** Completed by the listen loop when it receives a response frame (has "ok" key). */
    private var pendingResponse: CompletableDeferred<WsResponse>? = null

    private val _scheduleUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits [Unit] each time the server pushes a `schedule_updated` event. */
    val scheduleUpdated: SharedFlow<Unit> = _scheduleUpdated.asSharedFlow()

    private val _songsUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits [Unit] each time the server pushes a `songs_updated` event. */
    val songsUpdated: SharedFlow<Unit> = _songsUpdated.asSharedFlow()

    private val _displayCleared = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits [Unit] each time the server pushes a `display_cleared` event. */
    val displayCleared: SharedFlow<Unit> = _displayCleared.asSharedFlow()

    private val _bibleUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits [Unit] each time the server pushes a `bible_updated` event (e.g. translation switched). */
    val bibleUpdated: SharedFlow<Unit> = _bibleUpdated.asSharedFlow()

    private val _presentationUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits [Unit] each time the server pushes a `presentation_updated` event. */
    val presentationUpdated: SharedFlow<Unit> = _presentationUpdated.asSharedFlow()

    private val _picturesUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits [Unit] each time the server pushes a `pictures_updated` event. */
    val picturesUpdated: SharedFlow<Unit> = _picturesUpdated.asSharedFlow()

    private val _songSectionSelected = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    /** Emits the section index each time the server pushes a `song_section_selected` event. */
    val songSectionSelected: SharedFlow<Int> = _songSectionSelected.asSharedFlow()

    private val _questionsUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits [Unit] each time the server pushes a `questions_updated` event (Q&A question added/changed). */
    val questionsUpdated: SharedFlow<Unit> = _questionsUpdated.asSharedFlow()

    /**
     * Sends an action message over the persistent WebSocket connection.
     *
     * @param type           Message type (e.g. [WsMessageType.PROJECT]).
     * @param payloadJson    The JSON-serialised payload body (will be double-encoded as a string).
     * @param fireAndForget  When `true`, sends the frame without waiting for a server response.
     *                       When `false` (default), waits for an `{"ok":…}` response frame —
     *                       required for approval-gated commands.
     */
    suspend fun sendAction(type: String, payloadJson: String, fireAndForget: Boolean = false): Result<Unit> {
        Logger.d(TAG, "sendAction ▶ type=$type  url=${settings.wsBaseUrl}")

        var lastException: Throwable? = null
        repeat(WS_MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                Logger.d(TAG, "sendAction — retry attempt ${attempt + 1}/$WS_MAX_ATTEMPTS  type=$type")
            }
            // Wait for the connection to be ready before each attempt (handles first-launch,
            // reconnect races, and connection drops that occur while queued behind actionMutex).
            if (!connectionReady.value) {
                Logger.d(TAG, "sendAction — waiting for WebSocket connection (attempt ${attempt + 1})…")
                try {
                    withTimeout(WS_CONNECTION_TIMEOUT_MS) {
                        connectionReady.first { it }
                    }
                } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                    val err = Exception("Timed out waiting for WebSocket connection to ${settings.wsBaseUrl}")
                    Logger.e(TAG, "sendAction — connection timeout on attempt ${attempt + 1}", err)
                    lastException = err
                    return@repeat
                }
            } else if (attempt > 0) {
                delay(WS_RETRY_DELAY_MS * attempt)
            }
            val result = apiRunCatching {
                val session = activeSession
                    ?: throw Exception("WebSocket not connected to ${settings.wsBaseUrl}")

                val envelope = json.encodeToString(
                    WsOutboundMessage(type = type, payload = payloadJson)
                )
                Logger.d(TAG, "sendAction ▶ sending frame: ${envelope.take(300)}")

                if (fireAndForget) {
                    session.send(Frame.Text(envelope))
                    Logger.d(TAG, "sendAction ◀ fire-and-forget completed  type=$type")
                } else {
                    // Approval-gated: serialise so only one action waits at a time
                    actionMutex.withLock {
                        val deferred = CompletableDeferred<WsResponse>()
                        pendingResponse = deferred
                        try {
                            session.send(Frame.Text(envelope))
                            val response = try {
                                withTimeout(ACTION_RESPONSE_TIMEOUT_MS) {
                                    deferred.await()
                                }
                            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                                // Convert to a non-cancelling exception so apiRunCatching wraps
                                // it in Result.failure instead of silently cancelling the caller.
                                throw ApiException(httpStatus = 408, reason = "No response from server — approval timed out")
                            }
                            if (response.ok != true) {
                                throw ApiException(
                                    httpStatus = 400,
                                    reason = response.reason ?: response.error,
                                )
                            }
                            Logger.d(TAG, "sendAction ◀ approved  type=$type")
                        } finally {
                            pendingResponse = null
                        }
                    }
                }
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
        // All attempts exhausted — report the final failure to Crashlytics
        val finalError = lastException ?: Exception("sendAction failed after $WS_MAX_ATTEMPTS attempts")
        if (finalError !is ApiException) {
            CrashReporting.log("WS sendAction FAILED after $WS_MAX_ATTEMPTS attempts  type=$type  url=${settings.wsBaseUrl}")
            CrashReporting.setCustomKey("ws_action_type",    type)
            CrashReporting.setCustomKey("ws_server_url",     settings.wsBaseUrl)
            CrashReporting.setCustomKey("network_error_type", finalError::class.simpleName ?: "Throwable")
            CrashReporting.setCustomKey("network_error_msg",  finalError.message?.take(200) ?: "unknown")
            CrashReporting.recordException(finalError)
        }
        return Result.failure(finalError)
    }

    /**
     * Connects to `ws://<host>:<port>/ws` and listens for server-push events.
     * Automatically reconnects after any error or disconnection.
     * Suspends until the calling coroutine is cancelled.
     */
    suspend fun listen() {
        var everConnected = false
        var reconnectDelay = INITIAL_RECONNECT_DELAY_MS
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
                    activeSession = this
                    connectionReady.value = true
                    everConnected = true
                    reconnectDelay = INITIAL_RECONNECT_DELAY_MS
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()

                        // Route frame: response (has "ok") vs event (has "type")
                        val jsonObj = runCatching {
                            json.parseToJsonElement(text).jsonObject
                        }.getOrNull()

                        if (jsonObj != null && (jsonObj.containsKey("ok") || (!jsonObj.containsKey("type")))) {
                            // Action response — complete the pending deferred
                            val response = runCatching {
                                json.decodeFromString<WsResponse>(text)
                            }.getOrNull() ?: WsResponse(ok = true)
                            pendingResponse?.complete(response)
                            continue
                        }

                        val event = runCatching {
                            json.decodeFromString<ServerEvent>(text)
                        }.getOrNull() ?: continue

                        Logger.d(TAG, "listen — event type=${event.type}")
                        when (event.type) {
                            "schedule_updated"     -> _scheduleUpdated.tryEmit(Unit)
                            "songs_updated"        -> _songsUpdated.tryEmit(Unit)
                            "bible_updated"        -> _bibleUpdated.tryEmit(Unit)
                            "display_cleared"      -> _displayCleared.tryEmit(Unit)
                            "presentation_updated" -> _presentationUpdated.tryEmit(Unit)
                            "pictures_updated"     -> _picturesUpdated.tryEmit(Unit)
                            "song_section_selected" -> {
                                val index = event.payload?.toIntOrNull()
                                if (index != null) _songSectionSelected.tryEmit(index)
                            }
                            "questions_updated" -> _questionsUpdated.tryEmit(Unit)
                        }
                    }
                    Logger.d(TAG, "listen — session closed normally")
                }
            } catch (e: CancellationException) {
                Logger.d(TAG, "listen — cancelled")
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, "listen — connection lost: ${e.message} — retrying in ${reconnectDelay}ms", e)
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
                    CrashReporting.log("[$TAG] listen — session dropped (${e::class.simpleName}): $errorMsg")
                }
            } finally {
                // Complete any pending action — covers both exception and normal close.
                // If the catch block already called completeExceptionally, this is a no-op.
                pendingResponse?.completeExceptionally(Exception("WebSocket session ended"))
                pendingResponse = null
                connectionReady.value = false
                activeSession = null
            }
            delay(reconnectDelay)
            reconnectDelay = (reconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
        }
    }

    /**
     * Closes the current WebSocket session (if any), causing the [listen] loop to
     * reconnect immediately with the latest [AppSettings] values (e.g. after the
     * user changes the server host/port).
     */
    fun reconnect() {
        Logger.d(TAG, "reconnect — closing current session to force reconnect")
        val session = activeSession ?: return
        connectionReady.value = false
        activeSession = null
        pendingResponse?.completeExceptionally(Exception("Reconnecting"))
        pendingResponse = null
        // Cancel the session's coroutine scope, which terminates the incoming
        // frame loop and causes listen() to reconnect with updated settings.
        session.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    /** Releases the underlying WebSocket client. Call when the owning ViewModel is cleared. */
    fun closeClient() {
        Logger.d(TAG, "closeClient")
        connectionReady.value = false
        activeSession = null
        pendingResponse?.cancel()
        pendingResponse = null
        client.close()
    }
}
