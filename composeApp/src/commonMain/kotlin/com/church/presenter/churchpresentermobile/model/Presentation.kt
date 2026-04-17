package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single slide within a [Presentation], as returned by GET /api/presentations.
 */
@Serializable
data class PresentationSlide(
    @SerialName("slide-index") val slideIndex: Int = 0,
    @SerialName("thumbnail-url") val thumbnailUrl: String? = null
)

/**
 * A single presentation item returned by GET /api/presentations.
 */
@Serializable
data class Presentation(
    val id: String? = null,
    @SerialName("file-name") val fileName: String? = null,
    @SerialName("file-type") val fileType: String? = null,
    @SerialName("slide-total") val slideTotal: Int? = null,
    val slides: List<PresentationSlide>? = null,
) {
    /** Display name — falls back to id if file-name is absent. */
    val displayName: String
        get() = fileName?.takeIf { it.isNotBlank() } ?: id ?: "Unknown"

    /** Identifier used for API select calls. */
    val displayId: String
        get() = id ?: ""

    /** Total number of slides. */
    val totalSlides: Int
        get() = slideTotal ?: slides?.size ?: 0
}

/**
 * Top-level response from GET /api/presentations.
 */
@Serializable
data class PresentationsResponse(
    val presentations: List<Presentation>? = null,
    val total: Int? = null,
) {
    val allPresentations: List<Presentation>
        get() = presentations ?: emptyList()
}

/** Request body for POST /api/schedule/add when adding a presentation. */
@Serializable
data class PresentationScheduleAddRequest(val item: PresentationSchedulePayload)

@Serializable
data class PresentationSchedulePayload(
    val type: String = "presentation",
    val id: String,
    val title: String,
    @SerialName("displayText") val displayText: String,
)

/** Request body for POST /api/presentations/{id}/select — navigates to a specific slide. */
@Serializable
data class SelectSlideRequest(val index: Int)

/** Payload for the WebSocket `select_slide` command — { id, index }. */
@Serializable
data class SelectSlideWsPayload(val id: String, val index: Int)

/** Response from POST /api/presentations/upload. */
@Serializable
data class UploadPresentationResponse(
    val ok: Boolean = true,
    val id: String? = null,
    val name: String? = null,
)

