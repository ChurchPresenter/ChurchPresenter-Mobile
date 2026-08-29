package com.church.presenter.churchpresentermobile.network

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
}
