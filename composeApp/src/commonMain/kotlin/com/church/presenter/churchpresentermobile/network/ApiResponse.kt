package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/**
 * Throws [ApiException] when this response is not 2xx, carrying the status code
 * and the server's own explanation.
 *
 * Every service must reject non-success responses through this helper rather than
 * with a bare `Exception("HTTP …")`. The distinction matters beyond tidiness:
 * [recordNetworkError] decides what reaches crash reporting by exception *type*,
 * so a plain exception is treated as a bug and reported as a non-fatal, while an
 * [ApiException] is understood to be the server answering — a business-logic
 * response, not a defect.
 *
 * The concrete symptom that prompted this: the desktop answers `GET /api/pictures`
 * with `503 — No picture folder loaded` whenever the operator hasn't opened a
 * folder yet, which is a perfectly normal state. Thrown as a plain exception it
 * bypassed the suppression and filed a Sentry error on essentially every launch.
 *
 * @param body The already-read response body, used as the failure reason.
 *             Callers read the body themselves (once) for logging, so it is
 *             passed in rather than re-read here.
 */
fun HttpResponse.ensureSuccess(body: String? = null) {
    if (status.isSuccess()) return
    throw ApiException(
        httpStatus = status.value,
        reason = body?.trim()?.take(200)?.takeIf { it.isNotEmpty() },
    )
}
