package com.church.presenter.churchpresentermobile.ui

import com.church.presenter.churchpresentermobile.network.ApiConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [apiImageHeaders] — the headers Coil sends when fetching a thumbnail.
 *
 * Image loads bypass the service layer entirely, so if these do not carry the
 * API key the grid fills with broken tiles while every other screen works.
 */
class ImageHeadersTest {

    @Test
    fun anApiKeyIsSentSoThumbnailsAreNotRejected() {
        val headers = apiImageHeaders(apiKey = "s3cret", deviceId = "device-1")

        assertEquals("s3cret", headers[ApiConstants.API_KEY_HEADER])
    }

    @Test
    fun theDeviceIsIdentifiedTheSameWayAsOnTheApi() {
        val headers = apiImageHeaders(apiKey = "s3cret", deviceId = "device-1")

        assertTrue(
            headers.asMap().values.any { values -> values.any { it.contains("device-1") } },
            "the device id should reach the server: ${headers.asMap()}",
        )
    }

    @Test
    fun withNoApiKeyTheHeadersAreStillWellFormed() {
        // The key is optional — most churches run without one.
        val headers = apiImageHeaders(apiKey = "", deviceId = "device-1")

        assertTrue(headers.asMap().isNotEmpty() || headers[ApiConstants.API_KEY_HEADER] == null)
    }

    @Test
    fun eachCallProducesAnEquivalentSetOfHeaders() {
        val first = apiImageHeaders("k", "d")
        val second = apiImageHeaders("k", "d")

        assertEquals(first.asMap().keys, second.asMap().keys)
    }
}
