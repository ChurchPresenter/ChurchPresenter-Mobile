package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.WebsiteItemPayload
import com.church.presenter.churchpresentermobile.model.WebsiteRequest
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "WebService"
// encodeDefaults = true so payload fields equal to their default value still reach the desktop.
private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

/**
 * Sends web pages to the desktop over the shared WebSocket (same `project` /
 * `add_to_schedule` path used by songs, bible, announcements, …).
 */
class WebService(
    private val wsService: WsSender,
) {
    /** Adds the web page to the desktop schedule (does not go live). */
    suspend fun addToSchedule(item: WebsiteItemPayload): Result<Unit> = apiRunCatching {
        val payload = json.encodeToString(WebsiteRequest(item))
        Logger.d(TAG, "addToSchedule ▶ WS add_to_schedule  payload=$payload")
        wsService.sendAction(WsMessageType.ADD_TO_SCHEDULE, payload).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "addToSchedule — FAILED: ${e.message}", e) }

    /** Projects the web page on the desktop screen now (requires operator approval). */
    suspend fun projectPage(item: WebsiteItemPayload): Result<Unit> = apiRunCatching {
        val payload = json.encodeToString(WebsiteRequest(item))
        Logger.d(TAG, "projectPage ▶ WS project  payload=$payload")
        wsService.sendAction(WsMessageType.PROJECT, payload).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "projectPage — FAILED: ${e.message}", e) }

    /** Clears whatever is currently on the desktop screen. */
    suspend fun clearScreen(): Result<Unit> = apiRunCatching {
        wsService.sendAction(WsMessageType.CLEAR, "", fireAndForget = true).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "clearScreen — FAILED: ${e.message}", e) }
}
