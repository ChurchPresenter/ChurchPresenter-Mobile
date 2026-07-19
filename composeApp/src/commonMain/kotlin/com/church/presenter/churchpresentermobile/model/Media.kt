package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/**
 * Flat payload for the media remote endpoints. The desktop's
 * `RemoteItemDto.toScheduleItem()` infers a `MediaItem` from a non-null [mediaUrl].
 * [mediaType] is one of the desktop `Constants.MEDIA_TYPE_*` values ("url", "local", "audio").
 */
@Serializable
data class MediaItemPayload(
    val type: String = "media",
    val id: String = "", // server assigns a UUID when blank
    val mediaUrl: String,
    val mediaTitle: String = "",
    val mediaType: String = "url",
    val displayText: String = "",
)

/** Wrapper request body for the project / schedule-add endpoints. */
@Serializable
data class MediaRequest(val item: MediaItemPayload)

/** Response from `POST /api/media/upload`. [path] is the absolute file path on the desktop. */
@Serializable
data class MediaUploadResponse(
    val ok: Boolean = false,
    val path: String = "",
    val name: String = "",
    val mediaType: String = "local",
)

/**
 * Snapshot of the desktop media player's playback state, pushed over the WebSocket
 * (`media_state_changed`). Metadata only — the app never renders the video itself.
 */
@Serializable
data class MediaPlaybackState(
    val isLive: Boolean = false,
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val mediaType: String = "",
    /** The loaded media's source — a network URL or the desktop's local file path. */
    val source: String = "",
)
