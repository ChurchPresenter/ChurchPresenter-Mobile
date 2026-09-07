package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the parts of the socket service that do not need a socket.
 *
 * The connect and listen loops need a real server and are exercised end to end
 * rather than here. What is covered is everything that decides *whether* to reach
 * for the network at all — the mode gate and the pause/resume switch — which is
 * where the reported problems have been.
 */
class ServerEventServiceTest {

    private fun service(mode: AppMode) =
        ServerEventService(AppSettings(InMemorySettingsStorage()), MutableStateFlow(mode))

    // ── The standalone gate ──────────────────────────────────────────────

    @Test
    fun standaloneRefusesToSendRatherThanWaitingOnASocket() = runTest {
        // In standalone every action is served locally by ProjectionRouter, so
        // nothing should arrive here. Failing immediately matters: waiting out the
        // connection timeout for a socket that is deliberately never opened looks
        // to the operator like a frozen UI.
        val result = service(AppMode.STANDALONE).sendAction(WsMessageType.PROJECT, "{}")

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("STANDALONE") == true,
            "the reason should name the mode: ${result.exceptionOrNull()?.message}",
        )
    }

    @Test
    fun everyActionKindIsRefusedInStandalone() = runTest {
        val svc = service(AppMode.STANDALONE)

        for (type in listOf(
            WsMessageType.PROJECT,
            WsMessageType.ADD_TO_SCHEDULE,
            WsMessageType.SELECT_SONG,
            WsMessageType.SELECT_BIBLE_VERSE,
            WsMessageType.CLEAR,
            WsMessageType.MEDIA_PLAY_PAUSE,
        )) {
            assertTrue(svc.sendAction(type, "{}").isFailure, type)
        }
    }

    @Test
    fun aFireAndForgetActionIsRefusedTheSameWay() = runTest {
        // Fire-and-forget still has to report the refusal, or the caller records a
        // success that never happened.
        val result = service(AppMode.STANDALONE)
            .sendAction(WsMessageType.CLEAR, "", fireAndForget = true)

        assertTrue(result.isFailure)
    }

    // ── Connection state ─────────────────────────────────────────────────

    @Test
    fun nothingIsConnectedBeforeListenIsCalled() {
        assertFalse(service(AppMode.REMOTE).connected.value)
    }

    @Test
    fun noPushEventHasArrivedYet() {
        val svc = service(AppMode.REMOTE)

        assertEquals(null, svc.mediaState.value)
    }

    // ── Pause and resume ─────────────────────────────────────────────────

    @Test
    fun pausingDropsTheConnectionFlag() {
        // Called when the app is backgrounded: the retry loop otherwise blocks on
        // socket connect and keeps the process busy, which gets reported as a
        // background ANR.
        val svc = service(AppMode.REMOTE)

        svc.pause()

        assertFalse(svc.connected.value)
    }

    @Test
    fun pausingTwiceIsHarmless() {
        val svc = service(AppMode.REMOTE)

        svc.pause()
        svc.pause()

        assertFalse(svc.connected.value)
    }

    @Test
    fun resumingAfterAPauseIsSafe() {
        val svc = service(AppMode.REMOTE)
        svc.pause()

        svc.resume()
        svc.resume()

        assertFalse(svc.connected.value, "resuming arms the loop; it does not connect by itself")
    }

    @Test
    fun resumingWithoutAPauseIsHarmless() {
        val svc = service(AppMode.REMOTE)

        svc.resume()

        assertFalse(svc.connected.value)
    }

    // ── Reconnect and teardown ───────────────────────────────────────────

    @Test
    fun reconnectingWithNoSessionOpenDoesNothing() {
        val svc = service(AppMode.REMOTE)

        svc.reconnect()

        assertFalse(svc.connected.value)
    }

    @Test
    fun closingReleasesTheClientAndClearsTheFlag() {
        val svc = service(AppMode.REMOTE)

        svc.closeClient()

        assertFalse(svc.connected.value)
    }

    @Test
    fun closingTwiceIsHarmless() {
        // onCleared can run more than once across a config change.
        val svc = service(AppMode.REMOTE)

        svc.closeClient()
        svc.closeClient()

        assertFalse(svc.connected.value)
    }

    // ── Sending with no desktop on the other end ─────────────────────────
    //
    // The state the app is in whenever the desktop is off, asleep, or on another
    // network — which is most of the week. Nothing here opens a socket: the send
    // path waits for a connection that never becomes ready, so what is under test
    // is how long it waits, how often it tries, and what it tells the caller.
    //
    // Run on virtual time, so the ten-second waits cost nothing.

    @Test
    fun `an action sent with no connection fails rather than hanging for ever`() = runTest {
        val result = service(AppMode.REMOTE).sendAction(WsMessageType.PROJECT, "{}")

        assertTrue(result.isFailure)
    }

    @Test
    fun `the failure says the connection timed out`() = runTest {
        // The operator sees this text; "Timed out waiting for WebSocket connection"
        // points at the desktop, which is where the problem actually is.
        val result = service(AppMode.REMOTE).sendAction(WsMessageType.PROJECT, "{}")

        assertTrue(
            result.exceptionOrNull()?.message?.contains("Timed out") == true,
            "unexpected reason: ${result.exceptionOrNull()?.message}",
        )
    }

    @Test
    fun `the failure names the address it could not reach`() = runTest {
        val settings = AppSettings(InMemorySettingsStorage())
        val svc = ServerEventService(settings, MutableStateFlow(AppMode.REMOTE))

        val result = svc.sendAction(WsMessageType.PROJECT, "{}")

        assertTrue(
            result.exceptionOrNull()?.message?.contains(settings.wsBaseUrl) == true,
            "the address should be in the message: ${result.exceptionOrNull()?.message}",
        )
    }

    @Test
    fun `it tries three times, ten seconds apart, then gives up`() = runTest {
        // Pinned because both halves matter and pull opposite ways: too few or too
        // short and a desktop that is merely slow to answer is declared unreachable
        // mid-service; too many or too long and the button stays dead in the
        // operator's hand with no explanation.
        val started = currentTime

        service(AppMode.REMOTE).sendAction(WsMessageType.PROJECT, "{}")

        assertEquals(30_000L, currentTime - started)
    }

    @Test
    fun `a fire-and-forget action with no connection fails the same way`() = runTest {
        // It waits for the connection like any other; only the response is skipped.
        val result = service(AppMode.REMOTE)
            .sendAction(WsMessageType.CLEAR, "", fireAndForget = true)

        assertTrue(result.isFailure)
    }

    @Test
    fun `an instant action with no connection fails the same way`() = runTest {
        val result = service(AppMode.REMOTE).sendAction(WsMessageType.SELECT_SONG, "{}")

        assertTrue(result.isFailure)
    }

    @Test
    fun `a failed send leaves the service disconnected, not half-open`() = runTest {
        // A stale "connected" flag would make the next action skip the wait and
        // fail instantly on a session that is not there.
        val svc = service(AppMode.REMOTE)

        svc.sendAction(WsMessageType.PROJECT, "{}")

        assertFalse(svc.connected.value)
    }

    @Test
    fun `a second action after a failure waits again rather than failing instantly`() = runTest {
        // The desktop coming back is the normal recovery; the send path must not
        // remember the earlier failure.
        val svc = service(AppMode.REMOTE)
        svc.sendAction(WsMessageType.PROJECT, "{}")
        val started = currentTime

        svc.sendAction(WsMessageType.PROJECT, "{}")

        assertEquals(30_000L, currentTime - started)
    }

    @Test
    fun `pausing during a wait stops the next action from waiting at all`() = runTest {
        // Backgrounding the app while an action is queued: the mode gate is what
        // stops the retry loop, so a paused service still reports the timeout
        // rather than parking the caller for ever.
        val svc = service(AppMode.REMOTE)
        svc.pause()

        val result = svc.sendAction(WsMessageType.PROJECT, "{}")

        assertTrue(result.isFailure)
    }

    @Test
    fun `closing the client after a failed send is harmless`() = runTest {
        val svc = service(AppMode.REMOTE)
        svc.sendAction(WsMessageType.PROJECT, "{}")

        svc.closeClient()

        assertFalse(svc.connected.value)
    }
}
