package com.church.presenter.churchpresentermobile.model

/**
 * Thrown by the network layer when the server returns a non-success response.
 *
 * @param httpStatus  The HTTP status code (e.g. 403).
 * @param reason      The `reason` or `error` field from the response body, if present.
 */
class ApiException(
    val httpStatus: Int,
    val reason: String? = null,
) : Exception("HTTP $httpStatus${reason?.let { ": $it" } ?: ""}")

