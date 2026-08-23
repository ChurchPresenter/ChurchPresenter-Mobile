package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

/**
 * Says who this device is on a request to the desktop.
 *
 * The id is what the desktop keys approval and blocking on; the name is what it
 * shows the operator while they decide. Both travel together because either one
 * alone is a worse prompt: an id nobody recognises, or a name that cannot be
 * remembered between services.
 *
 * One helper rather than a `header(...)` line per service, so every service
 * identifies the device the same way and a new one cannot quietly forget to.
 */
fun HttpRequestBuilder.identifyDevice(settings: AppSettings) {
    header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
    // Nothing rather than an empty header: the desktop falls back to the id, and
    // a blank name would be a worse label than the UUID it replaced.
    settings.reportedDeviceName.takeIf { it.isNotBlank() }?.let { name ->
        header(ApiConstants.DEVICE_NAME_HEADER, name)
    }
}
