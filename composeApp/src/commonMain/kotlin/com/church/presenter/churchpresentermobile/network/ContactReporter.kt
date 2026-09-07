package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.getPlatform
import com.church.presenter.churchpresentermobile.util.appVersion
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val HTTP_OK = 200
private const val HTTP_TOO_MANY_REQUESTS = 429

/**
 * Sends a contact / feedback message to the ChurchPresenter server, which relays
 * it by email. Mirrors the desktop app's ContactReporter so both clients feed the
 * same inbox and the same triage.
 *
 * No credentials ever live in the app: this only POSTs the message the user just
 * typed to a public HTTPS endpoint — the same shape of call [PingReporter]
 * already makes. Bot abuse is bounded server-side by a per-IP rate limit; when it
 * trips the server returns HTTP 429 and we send the user to [WEB_CONTACT_URL],
 * where the web form is protected by Turnstile.
 */
object ContactReporter {

    /**
     * Which client this is, in the server's vocabulary.
     *
     * The server reads this to label the email ("Mobile app" / "Mobile app
     * (web)") and, since the User-Agent gate was dropped, to decide the request
     * is genuine at all — an unrecognised value is a 403, not a validation
     * error. The spellings must match `CLIENT_SOURCES` in the website's
     * `api/contact-app.ts`.
     */
    internal const val CLIENT_NATIVE = "mobile"
    internal const val CLIENT_WEB = "mobile-web"

    @Serializable
    data class ContactRequest(
        val type: String,
        val name: String,
        val message: String,
        val email: String = "",
        val context: String = "",
        /** See [CLIENT_NATIVE]. The browser build cannot set a User-Agent, so this is the identification. */
        val client: String = CLIENT_NATIVE,
        // Honeypot — always empty from a real client; the server rejects non-empty.
        val company: String = "",
    )

    sealed interface Outcome {
        /** Delivered (or accepted in local dev). */
        data object Success : Outcome
        /** Rejected by validation; [error] is the server's reason if available. */
        data class Invalid(val error: String?) : Outcome
        /** Per-IP rate limit hit — escalate the user to [WEB_CONTACT_URL]. */
        data object RateLimited : Outcome
        /** Couldn't reach the server (no connection / timeout) — retryable; hint to check the network. */
        data object NetworkError : Outcome
        /** Server-side error (5xx / unexpected status) — transient, safe to retry. */
        data object Failure : Outcome
    }

    /** Public web contact form (guarded by Turnstile) — the rate-limit escalation target. */
    const val WEB_CONTACT_URL = "https://www.churchpresenter.org/contact"
    private const val ENDPOINT = "https://www.churchpresenter.org/api/contact-app"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val http by lazy { createHttpClient() }

    @Serializable
    private data class ErrorBody(val error: String? = null)

    /** The client id for the platform this build is running on. */
    internal fun clientId(): String =
        if (getPlatform().os == "web") CLIENT_WEB else CLIENT_NATIVE

    /** Non-sensitive diagnostics appended to the message body to aid bug-report triage. */
    fun defaultContext(): String = "Church Presenter Mobile $appVersion · ${getPlatform().name}"

    suspend fun submit(request: ContactRequest): Outcome = submit(request, http)

    /**
     * The same submission over a supplied [client].
     *
     * A narrow seam, like [WsSender] elsewhere: everything that decides what the
     * user is told — which status means retry, which means "use the web form",
     * and where the server's own reason comes from — lives between the POST and
     * the [Outcome], and none of it should need a live endpoint to check.
     */
    internal suspend fun submit(request: ContactRequest, client: HttpClient): Outcome {
        val result = apiRunCatching {
            client.post(ENDPOINT) {
                // The server's fallback identification, for parity with the desktop.
                // Browsers refuse to let a script set this, which is exactly why
                // ContactRequest.client carries the same fact in the body.
                header(HttpHeaders.UserAgent, "ChurchPresenterMobile/$appVersion")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(ContactRequest.serializer(), request))
            }
        }
        val response = result.getOrElse {
            // Connection refused / no route / DNS / timeout — surfaced as a network
            // error so the UI can tell the user to check their connection.
            return Outcome.NetworkError
        }
        // A 4xx is the only status that needs the body read for its reason, so it is
        // handled here (the one place with the response in hand) rather than in
        // [classifyStatus].
        return classifyStatus(response.status.value)
            ?: Outcome.Invalid(parseErrorMessage(apiRunCatching { response.bodyAsText() }.getOrDefault("")))
    }

    /**
     * Maps an HTTP status to its terminal [Outcome], or null for a 4xx whose reason still has to be
     * read out of the response body. This is the decision that routes the user to retry, to the web
     * form, or to fix their input — split out from [submit] so it is testable without a socket.
     */
    internal fun classifyStatus(status: Int): Outcome? = when (status) {
        HTTP_OK -> Outcome.Success
        HTTP_TOO_MANY_REQUESTS -> Outcome.RateLimited
        in 400..499 -> null
        else -> Outcome.Failure
    }

    /** Lifts the server's human-readable reason out of a 4xx JSON body, or null if it isn't there. */
    internal fun parseErrorMessage(body: String): String? = runCatching {
        json.decodeFromString(ErrorBody.serializer(), body).error
    }.getOrNull()

    /** The JSON actually put on the wire — the only place a wrong [client] or honeypot would show. */
    internal fun encodeBody(request: ContactRequest): String =
        json.encodeToString(ContactRequest.serializer(), request)
}
