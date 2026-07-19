package com.church.presenter.churchpresentermobile.ui

import coil3.network.NetworkHeaders
import com.church.presenter.churchpresentermobile.network.ApiConstants

/**
 * Headers for Coil thumbnail requests. The `/api/pictures/.../images` and
 * `/api/presentations/.../slides` endpoints are behind `checkApiKey`, so image
 * requests must carry the API key + device id or they 401 (blank thumbnails).
 */
fun apiImageHeaders(apiKey: String, deviceId: String): NetworkHeaders =
    NetworkHeaders.Builder().apply {
        if (apiKey.isNotBlank()) set(ApiConstants.API_KEY_HEADER, apiKey)
        if (deviceId.isNotBlank()) set(ApiConstants.DEVICE_ID_HEADER, deviceId)
    }.build()
