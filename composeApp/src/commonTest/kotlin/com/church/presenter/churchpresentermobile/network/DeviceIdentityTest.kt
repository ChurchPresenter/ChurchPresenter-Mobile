package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What the desktop is told about this device.
 *
 * Approving `3f7c1a9e-…` mid-service is guesswork; the name is what makes the
 * prompt answerable. See ChurchPresenter#381.
 */
class DeviceIdentityTest {

    private var seen: Headers = Headers.Empty

    private fun clientFor(settings: AppSettings): HttpClient {
        val engine = MockEngine { request ->
            seen = request.headers
            respond("{}", HttpStatusCode.OK)
        }
        return HttpClient(engine)
    }

    private suspend fun send(settings: AppSettings) {
        clientFor(settings).get("http://desktop/api/status") { identifyDevice(settings) }
    }

    @Test
    fun theCustomNameIsWhatTheDesktopIsTold() = runTest {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.displayName = "Sound desk"

        send(settings)

        assertEquals("Sound desk", seen[ApiConstants.DEVICE_NAME_HEADER])
        assertEquals(settings.deviceId, seen[ApiConstants.DEVICE_ID_HEADER])
    }

    @Test
    fun aCustomNameIsTrimmedNotSentWithItsWhitespace() = runTest {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.displayName = "  Stage left  "

        send(settings)

        assertEquals("Stage left", seen[ApiConstants.DEVICE_NAME_HEADER])
    }

    @Test
    fun aNameThatIsOnlyWhitespaceCountsAsNoName() = runTest {
        // On a platform with no OS name this must omit the header rather than
        // send an empty one — a blank label reads worse than the UUID it replaced.
        val settings = AppSettings(InMemorySettingsStorage())
        settings.displayName = "   "

        send(settings)

        val reported = settings.reportedDeviceName
        if (reported.isBlank()) {
            assertNull(seen[ApiConstants.DEVICE_NAME_HEADER])
        } else {
            assertEquals(reported, seen[ApiConstants.DEVICE_NAME_HEADER])
        }
    }

    @Test
    fun theIdIsAlwaysSentEvenWithNoName() = runTest {
        val settings = AppSettings(InMemorySettingsStorage())

        send(settings)

        assertEquals(settings.deviceId, seen[ApiConstants.DEVICE_ID_HEADER])
    }

    @Test
    fun theHeaderNameMatchesWhatTheDesktopReads() {
        // The desktop reads this exact header, then a query parameter of the same
        // name on the WebSocket handshake (WebSocketRoute.kt).
        assertEquals("X-Device-Name", ApiConstants.DEVICE_NAME_HEADER)
        assertEquals("X-Device-Id", ApiConstants.DEVICE_ID_HEADER)
    }
}
