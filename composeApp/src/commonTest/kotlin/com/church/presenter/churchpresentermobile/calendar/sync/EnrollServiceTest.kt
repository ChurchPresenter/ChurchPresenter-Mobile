package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EnrollServiceTest {

    private val settings = AppSettings(InMemorySettingsStorage()).apply { apiKey = "api-secret" }
    private var seen: HttpRequestData? = null

    private fun service(status: HttpStatusCode, body: String) = EnrollService(
        settings,
        HttpClient(
            MockEngine { request ->
                seen = request
                respond(body, status)
            },
        ),
    )

    @Test
    fun theDesktopIsAskedWithTheNameTheCodeAndThisDevicesIdentity() = runTest {
        val reply = service(HttpStatusCode.OK, """{"relayUrl":"https://sync.example.org/","instanceId":"inst-1"}""")
            .enroll("Anna's iPhone", "123456").getOrThrow()
        assertEquals(EnrollReply("https://sync.example.org", "inst-1"), reply)
        val request = assertNotNull(seen)
        assertTrue(request.url.toString().endsWith("/api/calendar/enroll"))
        assertEquals("""{"deviceName":"Anna's iPhone","code":"123456"}""", (request.body as TextContent).text)
        assertEquals("api-secret", request.headers[ApiConstants.API_KEY_HEADER])
        assertNotNull(request.headers[ApiConstants.DEVICE_ID_HEADER])
    }

    @Test
    fun aRefusalIsDeniedNotAnError() = runTest {
        val result = service(HttpStatusCode.Forbidden, "").enroll("Phone", "123456")
        assertTrue(result.exceptionOrNull() is EnrollDenied)
    }

    @Test
    fun aReplyPointingAtAnUnsafeRelayIsRefused() = runTest {
        val plain = service(HttpStatusCode.OK, """{"relayUrl":"http://evil.example","instanceId":"inst-1"}""")
        assertTrue(plain.enroll("Phone", "123456").exceptionOrNull() is RelayFailure.Rejected)
        val badId = service(HttpStatusCode.OK, """{"relayUrl":"https://sync.example.org","instanceId":"../x"}""")
        assertTrue(badId.enroll("Phone", "123456").exceptionOrNull() is RelayFailure.Rejected)
        val junk = service(HttpStatusCode.OK, "<html>")
        assertTrue(junk.enroll("Phone", "123456").isFailure)
        val down = service(HttpStatusCode.ServiceUnavailable, "")
        assertTrue(down.enroll("Phone", "123456").exceptionOrNull() is RelayFailure.Rejected)
    }
}
