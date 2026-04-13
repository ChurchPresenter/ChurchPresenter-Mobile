package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A single image inside a [PicturesFolder]. */
@Serializable
data class PictureImage(
    val index: Int = 0,
    @SerialName("file-name") val fileName: String? = null,
    @SerialName("thumbnail-url") val thumbnailUrl: String? = null,
)

/** Top-level response from GET /api/pictures. */
@Serializable
data class PicturesFolder(
    @SerialName("folder-id") val folderId: String? = null,
    @SerialName("folder-name") val folderName: String? = null,
    @SerialName("folder-path") val folderPath: String? = null,
    @SerialName("image-total") val imageTotal: Int? = null,
    val images: List<PictureImage>? = null,
) {
    val displayName: String get() = folderName?.takeIf { it.isNotBlank() } ?: "Pictures"
    val totalImages: Int get() = imageTotal ?: images?.size ?: 0
    val allImages: List<PictureImage> get() = images ?: emptyList()
}

/** Request body for POST /api/pictures/select. */
@Serializable
data class PictureSelectRequest(
    @SerialName("folder-id") val folderId: String,
    val index: Int,
)

/** Request body for POST /api/schedule/add when adding a picture. */
@Serializable
data class PictureScheduleAddRequest(val item: PictureSchedulePayload)

@Serializable
data class PictureSchedulePayload(
    val type: String = "image",
    @SerialName("folder-id") val folderId: String,
    @SerialName("image-index") val imageIndex: Int,
    @SerialName("displayText") val displayText: String,
)

/** Response from POST /api/pictures/upload. */
@Serializable
data class UploadPhotoResponse(
    val ok: Boolean = true,
    @SerialName("folder-id") val folderId: String,
    @SerialName("image-index") val imageIndex: Int,
)

