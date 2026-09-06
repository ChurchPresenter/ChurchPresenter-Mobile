package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.FakeDesktop
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The persistent socket to the desktop, driven over a real connection.
 *
 * Everything that matters about this class only happens once a session exists:
 * the handshake that identifies the phone, the routing of a pushed frame onto
 * the right flow, and the half of `sendAction` that waits for the operator on
 * the desktop to press Allow. None of it can be reached with a mock engine —
 * Ktor's has no WebSocket support — so a small CIO server stands in for the
 * desktop on a loopback port.
 *
 * `runBlocking`, not `runTest`: the reconnect backoff and the socket itself run
 * on real time, and a virtual clock would fire the timeouts before the
 * handshake completed.
 */
class ServerEventServiceLiveTest {

    private val desktop = FakeDesktop()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var service: ServerEventService? = null
    private var listening: Job? = null

    /** The settings the connected service was built over, for asserting on the handshake. */
    private lateinit var settings: AppSettings

    /**
     * Runs [block] against the fake desktop under a hard time budget.
     *
     * Everything here waits on real sockets and real reconnect backoff, so a
     * test that stops making progress would otherwise block the whole suite
     * rather than failing. Twenty seconds is well past the slowest of these
     * (a two-second reconnect plus a handshake) and well short of noticing.
     */
    private fun liveTest(block: suspend CoroutineScope.() -> Unit) =
        runBlocking { withTimeout(TEST_BUDGET_MS) { block() } }

    @AfterTest
    fun cleanUp() = runBlocking {
        listening?.cancel()
        service?.closeClient()
        scope.cancel()
        desktop.stop()
    }

    /**
     * A service connected to the fake desktop, with [configure] applied to its
     * settings before the socket is opened.
     */
    private suspend fun connected(
        configure: AppSettings.() -> Unit = {},
    ): ServerEventService {
        val port = desktop.start()
        settings = AppSettings(InMemorySettingsStorage()).apply {
            host = "127.0.0.1"
            this.port = port
            configure()
        }
        val svc = ServerEventService(settings, MutableStateFlow(AppMode.REMOTE))
        service = svc
        listening = scope.launch { svc.listen() }
        withTimeout(TIMEOUT_MS) { svc.connected.first { it } }
        return svc
    }

    private fun headerOf(name: String): String? =
        desktop.handshakes.value.last().entries
            .firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()

    private fun queryOf(name: String): String? =
        desktop.queries.value.last()[name]?.firstOrNull()

    /** Pushes [type] with [payload] and waits for the socket to be carrying it. */
    private suspend fun push(type: String, payload: String = "") {
        desktop.pushes.emit("""{"type":"$type","payload":${quote(payload)}}""")
    }

    private fun quote(raw: String) = "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    // ── Connecting ───────────────────────────────────────────────────────

    @Test
    fun `the phone connects and reports itself connected`() = liveTest {
        val svc = connected()

        assertTrue(svc.connected.value)
        assertEquals(1, withTimeout(TIMEOUT_MS) { desktop.awaitConnections(1) })
    }

    @Test
    fun `the handshake identifies the device`() = liveTest {
        // The desktop lists connected phones by this; without it every phone in
        // the room looks like the same one.
        connected()

        assertEquals(settings.deviceId, headerOf(ApiConstants.DEVICE_ID_HEADER))
        assertFalse(settings.deviceId.isBlank())
    }

    @Test
    fun `the device id is also sent as a query parameter`() = liveTest {
        // The app's own browser build cannot set headers on a WebSocket
        // handshake, so the desktop reads a parameter of the same name instead.
        connected()

        assertEquals(headerOf(ApiConstants.DEVICE_ID_HEADER), queryOf(ApiConstants.DEVICE_ID_HEADER))
    }

    @Test
    fun `an API key is sent when one is configured`() = liveTest {
        connected { apiKey = "secret-key" }

        assertEquals("secret-key", headerOf(ApiConstants.API_KEY_HEADER))
    }

    @Test
    fun `no API key header is sent when none is configured`() = liveTest {
        connected { apiKey = "" }

        assertNull(headerOf(ApiConstants.API_KEY_HEADER))
    }

    @Test
    fun `a device name is sent, encoded`() = liveTest {
        // A non-ASCII name would throw before the socket opened, leaving the app
        // in its reconnect loop for ever.
        connected { customDeviceName = "Звукова рубка" }

        val sent = headerOf(ApiConstants.DEVICE_NAME_HEADER)
        assertNotNull(sent)
        assertTrue(sent.all { it.code < 128 }, "the name was not encoded: $sent")
    }

    @Test
    fun `the device name is also sent as a query parameter`() = liveTest {
        connected { customDeviceName = "Sound desk" }

        assertEquals("Sound desk", queryOf(ApiConstants.DEVICE_NAME_HEADER))
        assertEquals(headerOf(ApiConstants.DEVICE_NAME_HEADER), queryOf(ApiConstants.DEVICE_NAME_HEADER))
    }

    // ── Pushed events ────────────────────────────────────────────────────

    @Test
    fun `a schedule update reaches the schedule flow`() = liveTest {
        val svc = connected()

        withTimeout<Unit>(TIMEOUT_MS) {
            val seen = scope.launch { svc.scheduleUpdated.first() }
            while (seen.isActive) push("schedule_updated")
            seen.join()
        }
    }

    @Test
    fun `each list the desktop can change has its own signal`() = liveTest {
        val svc = connected()

        for ((type, flow) in listOf(
            "songs_updated" to svc.songsUpdated,
            "bible_updated" to svc.bibleUpdated,
            "presentation_updated" to svc.presentationUpdated,
            "pictures_updated" to svc.picturesUpdated,
            "questions_updated" to svc.questionsUpdated,
            "display_cleared" to svc.displayCleared,
        )) {
            withTimeout<Unit>(TIMEOUT_MS) {
                val seen = scope.launch { flow.first() }
                while (seen.isActive) push(type)
                seen.join()
            }
        }
    }

    @Test
    fun `a section the operator picked on the desktop carries its index`() = liveTest {
        // How the phone's verse list follows along when someone drives from the
        // desktop instead.
        val svc = connected()

        var index = -1
        withTimeout<Unit>(TIMEOUT_MS) {
            val seen = scope.launch { index = svc.songSectionSelected.first() }
            while (seen.isActive) push("song_section_selected", "2")
            seen.join()
        }

        assertEquals(2, index)
    }

    @Test
    fun `the desktop's media state reaches the media screen`() = liveTest {
        val svc = connected()

        desktop.pushes.emit(
            """{"type":"media_state_changed","payload":"{\"isLoaded\":true,\"title\":\"Sermon clip\"}"}"""
        )
        val state = withTimeout(TIMEOUT_MS) { svc.mediaState.first { it != null } }

        assertEquals("Sermon clip", assertNotNull(state).title)
    }

    @Test
    fun `a media state that cannot be read leaves the last one alone`() = liveTest {
        // A newer desktop sending a shape this build cannot parse must not blank
        // the transport controls mid-playback.
        val svc = connected()
        desktop.pushes.emit(
            """{"type":"media_state_changed","payload":"{\"isLoaded\":true,\"title\":\"Sermon clip\"}"}"""
        )
        withTimeout(TIMEOUT_MS) { svc.mediaState.first { it != null } }

        desktop.pushes.emit("""{"type":"media_state_changed","payload":"not json"}""")
        push("schedule_updated")

        assertEquals("Sermon clip", assertNotNull(svc.mediaState.value).title)
    }

    @Test
    fun `an event this build does not know about is ignored`() = liveTest {
        // A newer desktop adds event types; an older phone has to stay connected
        // through them rather than treating one as a protocol error.
        val svc = connected()

        push("something_new_entirely")
        push("schedule_updated")

        assertTrue(svc.connected.value)
    }

    @Test
    fun `a frame that is not JSON does not drop the connection`() = liveTest {
        val svc = connected()

        desktop.pushes.emit("this is not json at all")
        push("schedule_updated")

        assertTrue(svc.connected.value)
    }

    // ── Sending actions ──────────────────────────────────────────────────

    @Test
    fun `an approved action succeeds`() = liveTest {
        val svc = connected()

        val result = withTimeout(TIMEOUT_MS) { svc.sendAction(WsMessageType.PROJECT, """{"id":"1"}""") }

        assertTrue(result.isSuccess)
    }

    @Test
    fun `the frame carries the type and the payload as a string`() = liveTest {
        // The desktop expects the payload double-serialised — a nested object
        // there is rejected outright.
        val svc = connected()

        withTimeout<Unit>(TIMEOUT_MS) { svc.sendAction(WsMessageType.PROJECT, """{"id":"1"}""") }
        val frame = withTimeout(TIMEOUT_MS) { desktop.nextFrame() }

        assertTrue(""""type":"project"""" in frame, frame)
        assertTrue("""\"id\":\"1\"""" in frame, frame)
    }

    @Test
    fun `an action the operator denies fails with the reason`() = liveTest {
        desktop.reply = { """{"ok":false,"reason":"denied"}""" }
        val svc = connected()

        val result = withTimeout(TIMEOUT_MS) { svc.sendAction(WsMessageType.PROJECT, "{}") }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("denied") == true)
    }

    @Test
    fun `a refusal with no reason still fails`() = liveTest {
        desktop.reply = { """{"ok":false}""" }
        val svc = connected()

        assertTrue(withTimeout(TIMEOUT_MS) { svc.sendAction(WsMessageType.PROJECT, "{}") }.isFailure)
    }

    @Test
    fun `a refusal carrying an error instead of a reason still fails`() = liveTest {
        desktop.reply = { """{"ok":false,"error":"session blocked"}""" }
        val svc = connected()

        val result = withTimeout(TIMEOUT_MS) { svc.sendAction(WsMessageType.PROJECT, "{}") }

        assertTrue(result.exceptionOrNull()?.message?.contains("session blocked") == true)
    }

    @Test
    fun `a fire-and-forget action does not wait for a reply`() = liveTest {
        // The transport controls send these many times a second; waiting for an
        // acknowledgement would make the slider lag behind the finger.
        desktop.reply = { null }
        val svc = connected()

        val result = withTimeout(TIMEOUT_MS) {
            svc.sendAction(WsMessageType.MEDIA_SEEK_TO, "1500", fireAndForget = true)
        }

        assertTrue(result.isSuccess)
        assertTrue("""media_seek_to""" in withTimeout(TIMEOUT_MS) { desktop.nextFrame() })
    }

    @Test
    fun `an instant action is acknowledged like any other`() = liveTest {
        val svc = connected()

        assertTrue(withTimeout(TIMEOUT_MS) { svc.sendAction(WsMessageType.SELECT_SONG, "{}") }.isSuccess)
    }

    @Test
    fun `several actions in a row all reach the desktop`() = liveTest {
        // Approval-gated actions are serialised through a mutex; a lost one
        // would leave the operator pressing a button that does nothing.
        val svc = connected()

        withTimeout<Unit>(TIMEOUT_MS) {
            svc.sendAction(WsMessageType.PROJECT, """{"n":1}""")
            svc.sendAction(WsMessageType.PROJECT, """{"n":2}""")
            svc.sendAction(WsMessageType.PROJECT, """{"n":3}""")
        }

        val frames = withTimeout(TIMEOUT_MS) { List(3) { desktop.nextFrame() } }
        assertEquals(listOf(1, 2, 3), frames.map { it.substringAfter("n\\\":").first().digitToInt() })
    }

    // ── Losing and regaining the connection ──────────────────────────────

    @Test
    fun `reconnecting drops the session and opens a new one`() = liveTest {
        // What a settings change does: the new host or port only takes effect on
        // a fresh connection.
        val svc = connected()

        svc.reconnect()

        withTimeout<Int>(RECONNECT_TIMEOUT_MS) { desktop.awaitConnections(2) }
        assertTrue(withTimeout(RECONNECT_TIMEOUT_MS) { svc.connected.first { it } })
    }

    @Test
    fun `pausing drops the connection and resuming brings it back`() = liveTest {
        // Backgrounding the app: the retry loop otherwise keeps the process busy
        // and gets reported as a background ANR.
        val svc = connected()

        svc.pause()
        assertFalse(svc.connected.value)

        svc.resume()

        withTimeout<Int>(RECONNECT_TIMEOUT_MS) { desktop.awaitConnections(2) }
        assertTrue(withTimeout(RECONNECT_TIMEOUT_MS) { svc.connected.first { it } })
    }

    @Test
    fun `closing the client leaves nothing connected`() = liveTest {
        val svc = connected()

        svc.closeClient()

        assertFalse(svc.connected.value)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L

        /** Long enough for the two-second reconnect backoff plus a handshake. */
        const val RECONNECT_TIMEOUT_MS = 20_000L

        /** A hard ceiling per test, so a stall fails rather than blocking the suite. */
        const val TEST_BUDGET_MS = 20_000L

        /** Long enough for the loop to fail one connect and start backing off. */
        const val RETRY_OBSERVATION_MS = 1_200L
    }

    // ── A desktop that is not there, or goes away ────────────────────────

    @Test
    fun `a desktop that hangs up is reconnected to`() = liveTest {
        // Every close is treated the same: the desktop quitting, the laptop
        // sleeping, the Wi-Fi dropping. The loop keeps trying, because the
        // operator's fix is usually to wake the laptop, not to restart the app.
        desktop.closeImmediately = true
        val port = desktop.start()
        val settings = AppSettings(InMemorySettingsStorage()).apply {
            host = "127.0.0.1"
            this.port = port
        }
        val svc = ServerEventService(settings, MutableStateFlow(AppMode.REMOTE))
        service = svc
        listening = scope.launch { svc.listen() }
        withTimeout<Int>(RECONNECT_TIMEOUT_MS) { desktop.awaitConnections(1) }

        desktop.closeImmediately = false

        assertTrue(withTimeout(RECONNECT_TIMEOUT_MS) { svc.connected.first { it } })
    }

    @Test
    fun `a desktop that is not running leaves the phone disconnected, not stuck`() = liveTest {
        // The state the app is in most of the week. It must keep retrying
        // quietly rather than erroring out or spinning the CPU.
        val deadPort = deadLoopbackPort()
        val settings = AppSettings(InMemorySettingsStorage()).apply {
            host = "127.0.0.1"
            this.port = deadPort
        }
        val svc = ServerEventService(settings, MutableStateFlow(AppMode.REMOTE))
        service = svc
        listening = scope.launch { svc.listen() }

        delay(RETRY_OBSERVATION_MS)

        assertFalse(svc.connected.value)
        assertTrue(listening!!.isActive, "the listen loop gave up")
    }

    /** A loopback port nothing is listening on. */
    private fun deadLoopbackPort(): Int =
        java.net.ServerSocket(0).use { it.localPort }
}
