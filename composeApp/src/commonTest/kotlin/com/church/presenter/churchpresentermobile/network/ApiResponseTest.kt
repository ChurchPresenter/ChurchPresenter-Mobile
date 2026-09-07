package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Tests [ensureSuccess], the one place a non-2xx response becomes an exception.
 *
 * The type it throws is what keeps a routine server refusal (the desktop's
 * `503 — No picture folder loaded`) out of crash reporting, so the tests care
 * about the exception's class as much as its message.
 */
class ApiResponseTest {

    private suspend fun response(status: HttpStatusCode): HttpResponse =
        mockClient { respond("", status) }.get("http://x/api/test")

    @Test
    fun `a 2xx response passes`() = runTest {
        response(HttpStatusCode.OK).ensureSuccess("ignored")
        response(HttpStatusCode.Created).ensureSuccess()
        response(HttpStatusCode.NoContent).ensureSuccess()
    }

    @Test
    fun `a failure carries the status code`() = runTest {
        val e = assertFailsWith<ApiException> {
            response(HttpStatusCode.ServiceUnavailable).ensureSuccess("No picture folder loaded")
        }

        assertEquals(503, e.httpStatus)
        assertEquals("No picture folder loaded", e.reason)
        assertEquals("HTTP 503: No picture folder loaded", e.message)
    }

    @Test
    fun `the body is trimmed`() = runTest {
        val e = assertFailsWith<ApiException> {
            response(HttpStatusCode.Forbidden).ensureSuccess("  session blocked \n")
        }

        assertEquals("session blocked", e.reason)
    }

    @Test
    fun `a long body is capped so an HTML error page cannot become the message`() = runTest {
        val e = assertFailsWith<ApiException> {
            response(HttpStatusCode.InternalServerError).ensureSuccess("x".repeat(500))
        }

        assertEquals(200, e.reason?.length)
    }

    @Test
    fun `a missing or blank body leaves no reason rather than an empty one`() = runTest {
        assertNull(assertFailsWith<ApiException> { response(HttpStatusCode.NotFound).ensureSuccess() }.reason)
        assertNull(assertFailsWith<ApiException> { response(HttpStatusCode.NotFound).ensureSuccess("") }.reason)
        assertNull(assertFailsWith<ApiException> { response(HttpStatusCode.NotFound).ensureSuccess("   ") }.reason)

        // With no reason the message stops at the status, with no dangling colon.
        val e = assertFailsWith<ApiException> { response(HttpStatusCode.NotFound).ensureSuccess("  ") }
        assertEquals("HTTP 404", e.message)
    }

    @Test
    fun `only 2xx is success — a 3xx the client did not follow still fails`() = runTest {
        // Redirects are off here so ensureSuccess sees the 302 itself; isSuccess() is 2xx only.
        val redirect = HttpClient(MockEngine { respond("", HttpStatusCode.Found) }) { followRedirects = false }
            .get("http://x/api/test")

        val e = assertFailsWith<ApiException> { redirect.ensureSuccess() }

        assertEquals(302, e.httpStatus)
    }
}
