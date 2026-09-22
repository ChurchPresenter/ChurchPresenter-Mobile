package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.PresetSummary
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.content.TextContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarSyncEngineTest {

    private val key = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8"
    private val enrolled = CalendarSyncState(
        relayUrl = "https://sync.example.org",
        instanceId = "inst-1",
        deviceToken = "devicetokendevicetoken",
        instanceKey = key,
    )
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** The relay, as far as one phone can tell: records by id, a revision counter, the keys it expects. */
    private inner class Relay {
        var rev = 10L
        val records = LinkedHashMap<String, SealedRecord>()
        val tombstones = ArrayList<RemoteTombstone>()
        var presetsBox = ""
        /** Records per `changes` page; a smaller number than the records held makes the relay answer `more`. */
        var pageSize = Int.MAX_VALUE
        var clientKey = "clientkeyclientkey01"
        var deviceToken = enrolled.deviceToken
        var pushTokens = ArrayList<String>()
        val calls = ArrayList<String>()

        fun handle(scope: MockRequestHandleScope, request: HttpRequestData): HttpResponseData = with(scope) {
            val path = request.url.encodedPath.removePrefix("/i/inst-1/")
            calls += "${request.method.value} $path"
            if (request.headers["X-Client-Key"] != clientKey) {
                return respond("""{"error":"client_key"}""", HttpStatusCode.Unauthorized)
            }
            if (request.headers["Authorization"] != "Bearer $deviceToken") {
                return respond("""{"error":"unauthorized"}""", HttpStatusCode.Unauthorized)
            }
            return when {
                path == "changes" -> {
                    val since = request.url.parameters["since"]!!.toLong()
                    val newer = records.values.filter { it.rev > since }.sortedBy { it.rev }
                    val more = newer.size > pageSize
                    val page = ChangesResponse(
                        rev = if (more) newer[pageSize - 1].rev else rev,
                        records = newer.take(pageSize),
                        tombstones = tombstones,
                        presetsBox = presetsBox,
                        more = more,
                    )
                    respond(json.encodeToString(ChangesResponse.serializer(), page), HttpStatusCode.OK)
                }
                path.startsWith("records/") && request.method == HttpMethod.Put -> {
                    val id = path.removePrefix("records/")
                    val current = records[id]?.rev ?: 0L
                    if (request.headers["If-Match"]!!.toLong() != current) {
                        return respond("{}", HttpStatusCode.PreconditionFailed)
                    }
                    val record = json.decodeFromString(SealedRecord.serializer(), (request.body as TextContent).text)
                    rev += 1
                    records[id] = record.copy(updatedAt = "2026-09-20T12:00:00Z", updatedBy = "phone", rev = rev)
                    respond("""{"rev":$rev}""", HttpStatusCode.OK)
                }
                path.startsWith("records/") && request.method == HttpMethod.Delete -> {
                    records.remove(path.removePrefix("records/"))
                    rev += 1
                    tombstones += RemoteTombstone(path.removePrefix("records/"), "2026-09-20T12:00:00Z")
                    respond("""{"rev":$rev}""", HttpStatusCode.OK)
                }
                path == "devices/me/push" -> {
                    val body = (request.body as TextContent).text
                    pushTokens += json.decodeFromString(PushTokenBody.serializer(), body).pushToken
                    respond("{}", HttpStatusCode.OK)
                }
                else -> respond("""{"error":"not_found"}""", HttpStatusCode.NotFound)
            }
        }

        /** Something the desktop put there, sealed as it would be. */
        suspend fun desktopWrote(service: PlannedService) {
            rev += 1
            val sealed = sealing().seal(service)
            records[service.id] = sealed.copy(updatedAt = "2026-09-20T09:00:00Z", updatedBy = "desktop", rev = rev)
        }
    }

    private val relay = Relay()
    private val storage = InMemoryFileStorage()
    private val repository = CalendarRepository(storage, now = { "2026-09-20T10:00:00Z" })
    private val settings = AppSettings(InMemorySettingsStorage()).apply {
        relayClientKey = "clientkeyclientkey01"
        relayClientKeyFetchedAt = 5_000_000L
    }
    private var websiteKey = "clientkeyclientkey01"
    private var state = enrolled
    private var pushToken = ""

    private suspend fun sealing() = Sealing.fromEncodedKey(key, "inst-1")!!

    private fun engine(): CalendarSyncEngine {
        val relayHttp = HttpClient(MockEngine { request -> relay.handle(this, request) })
        val websiteHttp = HttpClient(MockEngine { respond("""{"clientKey":"$websiteKey"}""", HttpStatusCode.OK) })
        return CalendarSyncEngine(
            repository,
            state = { state },
            saveState = { state = it },
            clientKeys = ClientKeySource(settings, websiteHttp, now = { 5_000_000L }),
            pushToken = { pushToken },
            clientFor = { s, k -> RelayClient(s, k, relayHttp) },
        )
    }

    private fun service(id: String, name: String, rev: Long = 0) =
        PlannedService(id, "2026-09-27", name, "10:00", rows = listOf(PlanRow.Section("r1", "Worship")), rev = rev)

    @Test
    fun anUnenrolledPhoneDoesNotTouchTheNetwork() = runTest {
        state = CalendarSyncState()
        val engine = engine()
        assertEquals(SyncStatus.NotEnrolled, engine.status.value)
        assertFalse(engine.sync())
        assertTrue(relay.calls.isEmpty())
    }

    @Test
    fun aKeyThatDoesNotDecodeMeansThisPhoneIsOut() = runTest {
        state = enrolled.copy(instanceKey = "notakeynotakeynotakey")
        val engine = engine()
        assertFalse(engine.sync())
        assertEquals(SyncStatus.Unauthorized, engine.status.value)
        assertTrue(relay.calls.isEmpty())
    }

    @Test
    fun whatTheDesktopPlannedArrivesSanitizedWithPresetsAndTheCursorMovesOn() = runTest {
        relay.desktopWrote(service("svc-1", "Sunday\u0000 Service"))
        relay.desktopWrote(service("svc-2", "Midweek"))
        relay.records["junk"] = SealedRecord("junk", "2026-12-01", "bm90IGEgYm94", rev = 99)
        val presetIndex = """{"presets":[{"id":"p1","name":"Countdown","kind":"timer"}]}"""
        relay.presetsBox = sealing().sealText(presetIndex, PRESETS_RECORD)
        val engine = engine()
        assertTrue(engine.sync())
        val doc = repository.document.value
        assertEquals(setOf("svc-1", "svc-2"), doc.services.map { it.id }.toSet())
        assertEquals("Sunday Service", doc.serviceById("svc-1")!!.name)
        assertEquals(11L, doc.serviceById("svc-1")!!.rev)
        assertEquals(listOf(PresetSummary("p1", "Countdown", kind = "timer")), doc.presets)
        assertEquals(relay.rev, state.cursor)
        assertTrue(state.lastSyncAt.isNotEmpty())
        val status = assertIs<SyncStatus.Synced>(engine.status.value)
        assertEquals(2, status.pulled)
        assertEquals(0, status.pushed)
        assertEquals(listOf("GET changes"), relay.calls)
    }

    @Test
    fun theDesktopsSongbooksArriveThroughTheSamePullAndLandInTheCatalog() = runTest {
        val catalogStore = SongCatalogStore(InMemoryFileStorage())
        val websiteHttp = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })
        val relayHttp = HttpClient(MockEngine { request -> relay.handle(this, request) })
        val book = CatalogRecord("Hymnal", songs = listOf(CatalogSong("42", "Here I Am to Worship", 270)))
        val json = Json { encodeDefaults = true; explicitNulls = false }
        val box = sealing().sealText(json.encodeToString(CatalogRecord.serializer(), book), "catalog:Hymnal-cf49adf4")
        relay.rev += 1
        val hymnalId = "catalog:Hymnal-cf49adf4"
        relay.records[hymnalId] = SealedRecord(hymnalId, "2028-12-20", box, rev = relay.rev)
        relay.desktopWrote(service("svc-1", "Sunday"))
        val engine = CalendarSyncEngine(
            repository,
            state = { state },
            saveState = { state = it },
            clientKeys = ClientKeySource(settings, websiteHttp, now = { 5_000_000L }),
            clientFor = { s, k -> RelayClient(s, k, relayHttp) },
            catalogStore = catalogStore,
        )

        assertTrue(engine.sync())

        assertEquals(listOf("svc-1"), repository.document.value.services.map { it.id })
        assertEquals(setOf("catalog:Hymnal-cf49adf4"), catalogStore.records.value.keys)
        assertEquals(270, catalogStore.durations().secondsFor(catalogStore.songs().single()))
        assertEquals(1, assertIs<SyncStatus.Synced>(engine.status.value).pulled)

        relay.rev += 1
        relay.tombstones += RemoteTombstone("catalog:Hymnal-cf49adf4", "2026-09-21T00:00:00Z")
        assertTrue(engine.sync())
        assertTrue(catalogStore.isEmpty)
        assertEquals(listOf("svc-1"), repository.document.value.services.map { it.id })
    }

    @Test
    fun localEditsArePushedWithTheirRevisionAndComeBackStamped() = runTest {
        relay.desktopWrote(service("svc-1", "Sunday"))
        val engine = engine()
        engine.sync()
        repository.saveService(repository.service("svc-1")!!.copy(name = "Sunday, renamed here"))
        repository.saveService(service("svc-new", "Added here"))
        assertEquals(setOf("svc-1", "svc-new"), repository.document.value.pendingPush)

        assertTrue(engine.sync())
        val doc = repository.document.value
        assertTrue(doc.pendingPush.isEmpty())
        assertEquals("Sunday, renamed here", doc.serviceById("svc-1")!!.name)
        assertEquals("2026-09-20T12:00:00Z", doc.serviceById("svc-1")!!.updatedAt)
        assertEquals(relay.records.getValue("svc-new").rev, doc.serviceById("svc-new")!!.rev)
        assertEquals("Sunday, renamed here", sealing().open(relay.records.getValue("svc-1"))!!.name)
        assertEquals(2, assertIs<SyncStatus.Synced>(engine.status.value).pushed)
    }

    @Test
    fun aStaleLocalEditGivesWayToTheDesktopsCopy() = runTest {
        relay.desktopWrote(service("svc-1", "Sunday"))
        val engine = engine()
        engine.sync()
        repository.saveService(repository.service("svc-1")!!.copy(name = "Phone edit"))
        relay.desktopWrote(service("svc-1", "Desktop edit"))

        assertTrue(engine.sync())
        val doc = repository.document.value
        assertEquals("Desktop edit", doc.serviceById("svc-1")!!.name)
        assertTrue(doc.pendingPush.isEmpty())
        assertEquals("Desktop edit", sealing().open(relay.records.getValue("svc-1"))!!.name)
    }

    @Test
    fun aLocalDeleteReachesTheRelayAndATombstoneRemovesHere() = runTest {
        relay.desktopWrote(service("svc-1", "Sunday"))
        relay.desktopWrote(service("svc-2", "Midweek"))
        val engine = engine()
        engine.sync()
        repository.deleteService("svc-1")
        relay.records.remove("svc-2")
        relay.rev += 1
        relay.tombstones += RemoteTombstone("svc-2", "2026-09-20T11:00:00Z")

        assertTrue(engine.sync())
        val doc = repository.document.value
        assertTrue(doc.services.isEmpty())
        assertTrue(doc.pendingDeletes.isEmpty())
        assertTrue(doc.deletedServices.isEmpty())
        assertNull(relay.records["svc-1"])
        assertTrue(relay.calls.contains("DELETE records/svc-1"))
    }

    @Test
    fun aRevokedPhoneStopsAndSaysSo() = runTest {
        relay.deviceToken = "someone-else"
        val engine = engine()
        assertFalse(engine.sync())
        assertEquals(SyncStatus.Unauthorized, engine.status.value)
    }

    @Test
    fun aFullPageIsFollowedUntilTheRelayHasNothingMore() = runTest {
        relay.desktopWrote(service("svc-1", "One"))
        relay.desktopWrote(service("svc-2", "Two"))
        relay.desktopWrote(service("svc-3", "Three"))
        relay.pageSize = 2
        val engine = engine()
        assertTrue(engine.sync())
        assertEquals(listOf("GET changes", "GET changes"), relay.calls)
        assertEquals("One", repository.service("svc-1")!!.name)
        assertEquals("Three", repository.service("svc-3")!!.name)
        assertEquals(relay.rev, state.cursor, "the cursor ends on the relay's revision, not short of it")
    }

    @Test
    fun aRotatedClientKeyIsFetchedFromTheWebsiteAndTheRoundRepeated() = runTest {
        relay.desktopWrote(service("svc-1", "Sunday"))
        relay.clientKey = "rotatedkeyrotatedkey"
        websiteKey = "rotatedkeyrotatedkey"
        val engine = engine()
        assertTrue(engine.sync())
        assertEquals("rotatedkeyrotatedkey", settings.relayClientKey)
        assertEquals(listOf("GET changes", "GET changes"), relay.calls)
        assertEquals("Sunday", repository.service("svc-1")!!.name)
    }

    @Test
    fun whenTheWebsiteStillHandsOutTheOldKeyTheRoundFails() = runTest {
        relay.clientKey = "rotatedkeyrotatedkey"
        val engine = engine()
        assertFalse(engine.sync())
        assertIs<SyncStatus.Failed>(engine.status.value)
    }

    @Test
    fun thePushTokenIsRegisteredOnceAndAgainOnlyWhenItChanges() = runTest {
        pushToken = "fcm-1"
        val engine = engine()
        engine.sync()
        engine.sync()
        assertEquals(listOf("fcm-1"), relay.pushTokens)
        assertEquals("fcm-1", state.registeredPushToken)
        pushToken = "fcm-2"
        engine.sync()
        assertEquals(listOf("fcm-1", "fcm-2"), relay.pushTokens)
    }

    @Test
    fun aRelayWhoseRevisionWentBackwardsIsNotTrusted() = runTest {
        state = enrolled.copy(cursor = 500)
        val engine = engine()
        assertFalse(engine.sync())
        assertIs<SyncStatus.Failed>(engine.status.value)
        assertEquals(500L, state.cursor)
    }

    @Test
    fun leavingForgetsTheEnrollment() = runTest {
        val engine = engine()
        engine.leave()
        assertFalse(state.isEnrolled)
        assertEquals(SyncStatus.NotEnrolled, engine.status.value)
        assertFalse(engine.isEnrolled)
    }
}
