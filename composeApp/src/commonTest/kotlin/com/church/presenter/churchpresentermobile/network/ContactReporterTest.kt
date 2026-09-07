package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The contact endpoint's answers, and the body we put on the wire.
 *
 * The status mapping is what routes the user to retry, to the web form, or to
 * fix their input, so it is tested without a socket. The encoded body is tested
 * because the server identifies the client from a field inside it: a wrong value
 * there is a silent 403 in production, and nothing else would show it.
 */
class ContactReporterTest {

    @Test
    fun anAcceptedMessageIsASuccess() {
        assertEquals(ContactReporter.Outcome.Success, ContactReporter.classifyStatus(200))
    }

    @Test
    fun theRateLimitIsItsOwnAnswer() {
        // 429 must not be lumped in with other 4xx: it is the one the UI answers
        // by sending the user to the Turnstile-protected web form.
        assertEquals(ContactReporter.Outcome.RateLimited, ContactReporter.classifyStatus(429))
    }

    @Test
    fun otherClientErrorsDeferToTheBody() {
        // null means "read the server's reason out of the body" — see submit().
        assertNull(ContactReporter.classifyStatus(400))
        assertNull(ContactReporter.classifyStatus(403))
    }

    @Test
    fun serverErrorsAreRetryableFailures() {
        assertEquals(ContactReporter.Outcome.Failure, ContactReporter.classifyStatus(500))
        assertEquals(ContactReporter.Outcome.Failure, ContactReporter.classifyStatus(502))
    }

    @Test
    fun theServersReasonIsLiftedOutOfTheBody() {
        assertEquals("Name is required", ContactReporter.parseErrorMessage("""{"error":"Name is required"}"""))
    }

    @Test
    fun aBodyWithNoReasonGivesNothingToShow() {
        assertNull(ContactReporter.parseErrorMessage("""{"ok":false}"""))
        assertNull(ContactReporter.parseErrorMessage("not json at all"))
        assertNull(ContactReporter.parseErrorMessage(""))
    }

    @Test
    fun theBodyNamesAClientTheServerAccepts() {
        // resolveSource() in the website's api/contact-app.ts keys off this, and
        // answers an unrecognised value with 403 rather than a validation error.
        val encoded = ContactReporter.encodeBody(
            ContactReporter.ContactRequest(
                type = "bugReport",
                name = "Ada",
                message = "It stopped",
                client = ContactReporter.CLIENT_NATIVE,
            )
        )
        assertTrue(""""client":"mobile"""" in encoded, "the client field must reach the server: $encoded")
    }

    @Test
    fun theHoneypotIsSentAndEmpty() {
        // Omitting it entirely would also pass server-side, but the field has to
        // survive serialization for the shape to match the desktop's.
        val encoded = ContactReporter.encodeBody(
            ContactReporter.ContactRequest(type = "feedback", name = "Ada", message = "Hello")
        )
        assertTrue(""""company":""""" in encoded, "honeypot must be present and empty: $encoded")
    }

    @Test
    fun theWebBuildNamesItselfSeparately() {
        // The server labels the two differently ("Mobile app" vs "Mobile app (web)").
        assertTrue(ContactReporter.CLIENT_WEB != ContactReporter.CLIENT_NATIVE)
        assertEquals("mobile-web", ContactReporter.CLIENT_WEB)
    }

    // ── The whole submission ─────────────────────────────────────────────
    //
    // Over a mock engine, so the mapping from what the server answers to what the
    // user is told is checked end to end without touching churchpresenter.org.

    private fun request() = ContactReporter.ContactRequest(
        type = "bugReport",
        name = "Ada",
        message = "The songs list is empty",
        email = "ada@example.org",
    )

    /** A client answering every request with [status] and [body]. */
    private fun answering(status: HttpStatusCode, body: String = "") = HttpClient(MockEngine {
        respond(
            content = body,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    })

    @Test
    fun `a message the server accepts is reported as sent`() = runTest {
        val outcome = ContactReporter.submit(request(), answering(HttpStatusCode.OK, """{"ok":true}"""))

        assertEquals(ContactReporter.Outcome.Success, outcome)
    }

    @Test
    fun `hitting the rate limit sends the user to the web form`() = runTest {
        // The escalation exists because the web form is Turnstile-protected and
        // this endpoint is not; a bare "try again later" would strand the user.
        val outcome = ContactReporter.submit(
            request(),
            answering(HttpStatusCode.TooManyRequests, """{"error":"slow down"}"""),
        )

        assertEquals(ContactReporter.Outcome.RateLimited, outcome)
    }

    @Test
    fun `a rejected message carries the server's own reason`() = runTest {
        val outcome = ContactReporter.submit(
            request(),
            answering(HttpStatusCode.BadRequest, """{"error":"Message is too short"}"""),
        )

        assertEquals(ContactReporter.Outcome.Invalid("Message is too short"), outcome)
    }

    @Test
    fun `a rejection with no readable reason still reports as invalid`() = runTest {
        // A proxy or an older server can answer 400 with HTML; the user still has
        // to be told their input was refused rather than that the network failed.
        val outcome = ContactReporter.submit(
            request(),
            answering(HttpStatusCode.BadRequest, "<html>Bad Request</html>"),
        )

        assertEquals(ContactReporter.Outcome.Invalid(null), outcome)
    }

    @Test
    fun `a forbidden client is reported as invalid, not as an outage`() = runTest {
        // What an unrecognised `client` value gets: 403. Calling it a failure
        // would have the UI offer a retry that can never succeed.
        val outcome = ContactReporter.submit(
            request(),
            answering(HttpStatusCode.Forbidden, """{"error":"unknown source"}"""),
        )

        assertEquals(ContactReporter.Outcome.Invalid("unknown source"), outcome)
    }

    @Test
    fun `a server error is a transient failure`() = runTest {
        val outcome = ContactReporter.submit(request(), HttpClient(MockEngine {
            respondError(HttpStatusCode.InternalServerError)
        }))

        assertEquals(ContactReporter.Outcome.Failure, outcome)
    }

    @Test
    fun `an unreachable server is reported as a network error`() = runTest {
        // The one outcome that tells the user to check their connection rather
        // than their message.
        val outcome = ContactReporter.submit(request(), HttpClient(MockEngine {
            error("Connect timeout has expired")
        }))

        assertEquals(ContactReporter.Outcome.NetworkError, outcome)
    }

    @Test
    fun `the message is posted as JSON to the contact endpoint`() = runTest {
        val seen = MutableStateFlow<String?>(null)
        val client = HttpClient(MockEngine { req ->
            seen.value = req.url.toString()
            respond("""{"ok":true}""", HttpStatusCode.OK)
        })

        ContactReporter.submit(request(), client)

        assertEquals("https://www.churchpresenter.org/api/contact-app", seen.value)
    }

    @Test
    fun `the request identifies the app in its User-Agent`() = runTest {
        // The server's fallback identification, for parity with the desktop.
        val seen = MutableStateFlow<String?>(null)
        val client = HttpClient(MockEngine { req ->
            seen.value = req.headers[HttpHeaders.UserAgent]
            respond("""{"ok":true}""", HttpStatusCode.OK)
        })

        ContactReporter.submit(request(), client)

        assertTrue(
            seen.value?.startsWith("ChurchPresenterMobile/") == true,
            "unexpected User-Agent: ${seen.value}",
        )
    }

    @Test
    fun `the diagnostics line names the app and the platform`() {
        // Appended to the message body so a bug report says what it came from.
        val context = ContactReporter.defaultContext()

        assertTrue(context.startsWith("Church Presenter Mobile "), context)
        assertTrue("·" in context, context)
    }

    @Test
    fun `the client id is one the server recognises`() {
        assertTrue(
            ContactReporter.clientId() in setOf(ContactReporter.CLIENT_NATIVE, ContactReporter.CLIENT_WEB),
            ContactReporter.clientId(),
        )
    }
}
