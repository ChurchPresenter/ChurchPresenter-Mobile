package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.StatusProbeResult
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests the [StatusService] classification of a server probe into a
 * [StatusProbeResult], using a [MockEngine]-backed client injected via the
 * internal test constructor. This is the core of "does 'Connected' mean we
 * really reached a ChurchPresenter server."
 */
class StatusServiceTest {

    private val baseUrl = "http://test.local/api"

    /** Builds a StatusService whose HTTP client is driven by [respondTo], keyed on request path. */
    private fun service(
        respondTo: MockRequestHandleScope.(path: String) -> HttpResponseData,
    ): StatusService {
        val engine = MockEngine { request -> respondTo(request.url.encodedPath) }
        return StatusService(baseUrl, apiKey = "", deviceId = "test-device", client = HttpClient(engine))
    }

    private fun MockRequestHandleScope.isStatus(path: String) = path.endsWith("/status")

    // ── Verified: /status returns a ChurchPresenter-shaped body ──────────────

    @Test
    fun verifiedViaAppVersion() = runTest {
        val svc = service { respond("""{"appVersion":"1.4.2"}""", HttpStatusCode.OK) }
        val v = assertIs<StatusProbeResult.Verified>(svc.fetchStatus().getOrThrow())
        assertEquals("1.4.2", v.status.appVersion)
    }

    @Test
    fun verifiedViaServerVersionHeader() = runTest {
        // Empty body but the X-Server-Version header identifies a ChurchPresenter server;
        // the header value must override the (absent) body appVersion.
        val svc = service {
            respond("{}", HttpStatusCode.OK, headersOf(ApiConstants.SERVER_VERSION_HEADER, "2.0.0"))
        }
        val v = assertIs<StatusProbeResult.Verified>(svc.fetchStatus().getOrThrow())
        assertEquals("2.0.0", v.status.appVersion)
    }

    @Test
    fun verifiedViaEndpoints() = runTest {
        val svc = service { respond("""{"endpoints":["songs","bible"]}""", HttpStatusCode.OK) }
        assertIs<StatusProbeResult.Verified>(svc.fetchStatus().getOrThrow())
    }

    @Test
    fun verifiedViaFeatures() = runTest {
        val svc = service { respond("""{"features":["media"]}""", HttpStatusCode.OK) }
        assertIs<StatusProbeResult.Verified>(svc.fetchStatus().getOrThrow())
    }

    // ── Empty/defaulted 2xx body is NOT verified — falls back to /songs ──────

    @Test
    fun emptyStatusBodyProbesSongsAndConfirmsChurchPresenter() = runTest {
        val svc = service { path ->
            if (isStatus(path)) respond("{}", HttpStatusCode.OK)
            else respond("""{"song-book":[]}""", HttpStatusCode.OK)
        }
        assertIs<StatusProbeResult.ReachableNoStatusEndpoint>(svc.fetchStatus().getOrThrow())
    }

    @Test
    fun emptyStatusBodyWithNonChurchPresenterSongsIsNotChurchPresenter() = runTest {
        val svc = service { path ->
            if (isStatus(path)) respond("{}", HttpStatusCode.OK)
            else respond("""{"tracks":[]}""", HttpStatusCode.OK)
        }
        assertIs<StatusProbeResult.NotChurchPresenter>(svc.fetchStatus().getOrThrow())
    }

    // ── /status missing (404) — identity decided by /songs probe ────────────

    @Test
    fun statusMissingButSongsConfirmsChurchPresenter() = runTest {
        val svc = service { path ->
            if (isStatus(path)) respond("Not Found", HttpStatusCode.NotFound)
            else respond("""{"song-book":[{"book-name":"Hymns","song-total":0,"songs":[]}]}""", HttpStatusCode.OK)
        }
        assertIs<StatusProbeResult.ReachableNoStatusEndpoint>(svc.fetchStatus().getOrThrow())
    }

    @Test
    fun statusMissingAndSongsUnrecognizedIsNotChurchPresenter() = runTest {
        val svc = service { path ->
            if (isStatus(path)) respond("Not Found", HttpStatusCode.NotFound)
            else respond("<html>router admin</html>", HttpStatusCode.OK)
        }
        val r = assertIs<StatusProbeResult.NotChurchPresenter>(svc.fetchStatus().getOrThrow())
        assertTrue(r.detail.isNotBlank())
    }

    @Test
    fun statusMissingAndSongsErrorIsNotChurchPresenter() = runTest {
        val svc = service { path ->
            if (isStatus(path)) respond("Not Found", HttpStatusCode.NotFound)
            else respond("boom", HttpStatusCode.InternalServerError)
        }
        val r = assertIs<StatusProbeResult.NotChurchPresenter>(svc.fetchStatus().getOrThrow())
        assertTrue(r.detail.contains("500"))
    }

    // ── Unauthorized (API key rejected) ─────────────────────────────────────

    @Test
    fun status401IsUnauthorized() = runTest {
        val svc = service { respond("", HttpStatusCode.Unauthorized) }
        val u = assertIs<StatusProbeResult.Unauthorized>(svc.fetchStatus().getOrThrow())
        assertEquals(401, u.httpStatus)
    }

    @Test
    fun status403IsUnauthorized() = runTest {
        val svc = service { respond("", HttpStatusCode.Forbidden) }
        val u = assertIs<StatusProbeResult.Unauthorized>(svc.fetchStatus().getOrThrow())
        assertEquals(403, u.httpStatus)
    }

    @Test
    fun statusMissingAndSongsUnauthorizedIsUnauthorized() = runTest {
        val svc = service { path ->
            if (isStatus(path)) respond("Not Found", HttpStatusCode.NotFound)
            else respond("", HttpStatusCode.Unauthorized)
        }
        assertIs<StatusProbeResult.Unauthorized>(svc.fetchStatus().getOrThrow())
    }

    // ── Connectivity failure surfaces as Result.failure (→ Error in UI) ─────

    @Test
    fun connectivityFailureIsResultFailure() = runTest {
        val svc = service { throw RuntimeException("Connect timeout has expired") }
        assertTrue(svc.fetchStatus().isFailure)
    }

    @Test
    fun theProductionConstructorWiresItselfFromSettings() {
        // The only path the app itself uses. It reads five separate fields off
        // AppSettings and builds the platform client; a mis-wired field there is a
        // runtime failure on the first probe rather than a compile error. Building
        // it opens no connection, so this is safe to do in a unit test.
        val settings = AppSettings(InMemorySettingsStorage()).apply {
            host = "10.0.0.5"
            port = 8765
            apiKey = "s3cret"
        }

        val service = StatusService(settings)

        assertNotNull(service)
        service.closeClient()
    }

    // ── What the probe tells the desktop about this phone ────────────────
    //
    // The Status screen is usually the first thing to reach a newly configured
    // desktop, so its headers are what decide whether the phone shows up in the
    // desktop's connected-devices list at all.

    /** The headers a probe sends, with [apiKey] and [deviceName] configured. */
    private suspend fun probeHeaders(apiKey: String, deviceName: String): io.ktor.http.Headers {
        var seen = io.ktor.http.Headers.Empty
        val engine = MockEngine { request ->
            seen = request.headers
            respond("""{"appVersion":"1.4.2"}""", HttpStatusCode.OK)
        }
        StatusService(baseUrl, apiKey, deviceId = "test-device", client = HttpClient(engine), deviceName = deviceName)
            .fetchStatus()
        return seen
    }

    @Test
    fun `the probe sends the API key when one is configured`() = runTest {
        assertEquals("secret-key", probeHeaders("secret-key", "")[ApiConstants.API_KEY_HEADER])
    }

    @Test
    fun `the probe sends no API key header when none is configured`() = runTest {
        assertNull(probeHeaders("", "")[ApiConstants.API_KEY_HEADER])
    }

    @Test
    fun `the probe always identifies the device`() = runTest {
        assertEquals("test-device", probeHeaders("", "")[ApiConstants.DEVICE_ID_HEADER])
    }

    @Test
    fun `the probe sends a device name when one is set`() = runTest {
        assertEquals("Sound desk", probeHeaders("", "Sound desk")[ApiConstants.DEVICE_NAME_HEADER])
    }

    @Test
    fun `the probe sends no name header rather than an empty one`() = runTest {
        // A blank name is a worse label than the id it would replace, so the
        // desktop is left to fall back rather than shown nothing.
        assertNull(probeHeaders("", "")[ApiConstants.DEVICE_NAME_HEADER])
    }

    @Test
    fun `a name outside ASCII is encoded before it is sent`() = runTest {
        val name = probeHeaders("", "Звукова рубка")[ApiConstants.DEVICE_NAME_HEADER]

        assertNotNull(name)
        assertTrue(name.all { it.code < 128 }, "sent raw: $name")
    }
}
