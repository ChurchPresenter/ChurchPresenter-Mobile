package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlinx.coroutines.flow.MutableStateFlow
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
}
