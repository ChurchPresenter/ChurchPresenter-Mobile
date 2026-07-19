package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AnnouncementItemPayload
import com.church.presenter.churchpresentermobile.model.AnnouncementRequest
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "AnnouncementService"
private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Sends composed announcements / timers to the desktop schedule over the shared
 * WebSocket (same `add_to_schedule` path used by songs, bible, dictionary, …).
 */
class AnnouncementService(
    private val wsService: ServerEventService,
) {
    /** Adds the announcement/timer to the desktop schedule (does not go live). */
    suspend fun addToSchedule(item: AnnouncementItemPayload): Result<Unit> = apiRunCatching {
        val payload = json.encodeToString(AnnouncementRequest(item))
        Logger.d(TAG, "addToSchedule ▶ WS add_to_schedule  payload=$payload")
        wsService.sendAction(WsMessageType.ADD_TO_SCHEDULE, payload).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "addToSchedule — FAILED: ${e.message}", e) }

    /** Shows the announcement/timer on the desktop screen now (requires operator approval). */
    suspend fun showOnScreen(item: AnnouncementItemPayload): Result<Unit> = apiRunCatching {
        val payload = json.encodeToString(AnnouncementRequest(item))
        Logger.d(TAG, "showOnScreen ▶ WS project  payload=$payload")
        wsService.sendAction(WsMessageType.PROJECT, payload).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "showOnScreen — FAILED: ${e.message}", e) }

    /** Clears whatever is currently on the desktop screen. */
    suspend fun clearScreen(): Result<Unit> = apiRunCatching {
        wsService.sendAction(WsMessageType.CLEAR, "", fireAndForget = true).getOrThrow()
    }.onFailure { e -> Logger.e(TAG, "clearScreen — FAILED: ${e.message}", e) }
}
