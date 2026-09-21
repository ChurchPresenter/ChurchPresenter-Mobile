package com.church.presenter.churchpresentermobile.calendar.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RelayClientTest {

    private val state = CalendarSyncState(
        relayUrl = "https://sync.example.org/",
        instanceId = "inst-1",
        deviceToken = "devicetokendevicetoken",
        instanceKey = "k".repeat(43),
        cursor = 5,
    )
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "{}",
        clientKey: String = "ck",
    ) = RelayClient(
        state,
        clientKey = { clientKey },
        client = HttpClient(
            MockEngine { request ->
                requests += request
                respond(body, status)
            },
        ),
    )

    private fun bodyOf(request: HttpRequestData): String = (request.body as TextContent).text

    @Test
    fun everyCallCarriesTheDeviceTokenAndClientKeyUnderTheInstancePath() = runTest {
        client(body = """{"rev":9,"records":[]}""").changes(since = 5)
        val request = requests.single()
        assertEquals("https://sync.example.org/i/inst-1/changes?since=5", request.url.toString())
        assertEquals("Bearer devicetokendevicetoken", request.headers["Authorization"])
        assertEquals("ck", request.headers["X-Client-Key"])
        assertEquals("application/json", request.headers["Accept"])
    }

    @Test
    fun aBlankClientKeyIsNotSentAsAnEmptyHeader() = runTest {
        client(body = """{"rev":9}""", clientKey = "").changes(since = 0)
        assertNull(requests.single().headers["X-Client-Key"])
    }

    @Test
    fun putRecordSendsTheRevisionItWasBuiltFromAndReturnsTheNewOne() = runTest {
        val rev = client(body = """{"rev":12}""").putRecord(SealedRecord("svc-1", "2026-12-26", "Ym94"), ifRev = 7)
        assertEquals(12L, rev)
        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("https://sync.example.org/i/inst-1/records/svc-1", request.url.toString())
        assertEquals("7", request.headers["If-Match"])
        val expectedBody =
            """{"id":"svc-1","keepUntil":"2026-12-26","box":"Ym94","updatedAt":"","updatedBy":"","rev":0}"""
        assertEquals(expectedBody, bodyOf(request))
    }

    @Test
    fun deleteAndPushTokenHitTheirOwnPaths() = runTest {
        assertEquals(3L, client(body = """{"rev":3}""").deleteRecord("svc-1"))
        assertEquals(HttpMethod.Delete, requests.single().method)
        assertEquals("https://sync.example.org/i/inst-1/records/svc-1", requests.single().url.toString())
        requests.clear()
        client().registerPushToken("fcm-token")
        assertEquals("https://sync.example.org/i/inst-1/devices/me/push", requests.single().url.toString())
        assertEquals("""{"pushToken":"fcm-token"}""", bodyOf(requests.single()))
    }

    @Test
    fun theChangesPageIsDecodedWithUnknownFieldsIgnored() = runTest {
        val body = """{"rev":20,"records":[{"id":"a","keepUntil":"2026-12-01","box":"Ym94","rev":19,"extra":1}],
            "tombstones":[{"id":"b","deletedAt":"2026-09-20T00:00:00Z"}],
            "presetsBox":"cHJl","lastDesktopInstall":"x"}"""
        val changes = client(body = body).changes(since = 5)
        assertEquals(20L, changes.rev)
        assertEquals(listOf("a"), changes.records.map { it.id })
        assertEquals(19L, changes.records[0].rev)
        assertEquals(listOf("b"), changes.tombstones.map { it.id })
        assertEquals("cHJl", changes.presetsBox)
    }

    @Test
    fun eachRefusalIsToldApart() = runTest {
        assertFailsWith<RelayFailure.ClientKey> {
            client(HttpStatusCode.Unauthorized, """{"error":"client_key"}""").changes(0)
        }
        assertFailsWith<RelayFailure.Unauthorized> {
            client(HttpStatusCode.Unauthorized, """{"error":"unauthorized"}""").changes(0)
        }
        assertFailsWith<RelayFailure.Unauthorized> { client(HttpStatusCode.Forbidden).changes(0) }
        assertFailsWith<RelayFailure.Conflict> { client(HttpStatusCode.Conflict).deleteRecord("a") }
        assertFailsWith<RelayFailure.Conflict> {
            client(HttpStatusCode.PreconditionFailed).putRecord(SealedRecord("a", "2026-12-01", "Ym94"), 1)
        }
        val rejected = assertFailsWith<RelayFailure.Rejected> {
            client(HttpStatusCode.PayloadTooLarge, "x".repeat(1_000)).changes(0)
        }
        assertTrue(rejected.message!!.contains("413"))
        assertTrue(rejected.message!!.length < 300)
    }
}
