package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/**
 * Flat payload for the web/website remote endpoints. The desktop's
 * `RemoteItemDto.toScheduleItem()` infers a `WebsiteItem` from a non-null [url].
 */
@Serializable
data class WebsiteItemPayload(
    val type: String = "website",
    val id: String = "", // server assigns a UUID when blank
    val url: String,
    val websiteTitle: String = "",
    val displayText: String = "",
)

/** Wrapper request body for the project / schedule-add endpoints. */
@Serializable
data class WebsiteRequest(val item: WebsiteItemPayload)
