package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideMessageType
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeOutputSink
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectionRouterTest {

    private class Fixture(mode: AppMode) {
        val modeFlow = MutableStateFlow(mode)
        val remote = FakeWsSender()
        val registry = SinkRegistry()
        val sink = FakeOutputSink()
        val engine: StandaloneEngine
        val router: ProjectionRouter

        init {
            registry.register(sink)
            engine = StandaloneEngine(modeFlow, registry) { }
            router = ProjectionRouter(modeFlow, remote, engine)
        }
    }

    // ── Remote mode: verbatim delegation ─────────────────────────────────

    @Test
    fun `remote mode forwards the action to the desktop unchanged`() = runTest {
        val f = Fixture(AppMode.REMOTE)

        val result = f.router.sendAction(WsMessageType.PROJECT, """{"item":{"id":"1"}}""", false)

        assertTrue(result.isSuccess)
        assertEquals(1, f.remote.calls.size)
        assertEquals(WsMessageType.PROJECT, f.remote.lastType)
        assertEquals("""{"item":{"id":"1"}}""", f.remote.lastPayload)
    }

    @Test
    fun `remote mode preserves the fireAndForget flag`() = runTest {
        val f = Fixture(AppMode.REMOTE)

        f.router.sendAction(WsMessageType.SELECT_SONG, "{}", fireAndForget = true)
        assertEquals(true, f.remote.calls.last().third)

        f.router.sendAction(WsMessageType.PROJECT, "{}", fireAndForget = false)
        assertEquals(false, f.remote.calls.last().third)
    }

    @Test
    fun `remote mode propagates a transport failure to the caller`() = runTest {
        val f = Fixture(AppMode.REMOTE)
        f.remote.failWith(IllegalStateException("socket closed"))

        val result = f.router.sendAction(WsMessageType.PROJECT, "{}", false)

        assertTrue(result.isFailure)
        assertEquals("socket closed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `remote mode never touches the standalone engine`() = runTest {
        val f = Fixture(AppMode.REMOTE)

        f.router.sendAction(WsMessageType.CLEAR, "{}", true)

        assertTrue(f.sink.rendered.isEmpty())
    }

    // ── Standalone mode: handled locally ─────────────────────────────────

    @Test
    fun `standalone mode never reaches the desktop`() = runTest {
        val f = Fixture(AppMode.STANDALONE)

        listOf(
            WsMessageType.PROJECT,
            WsMessageType.SELECT_SONG,
            WsMessageType.CLEAR,
            WsMessageType.ADD_TO_SCHEDULE,
        ).forEach { f.router.sendAction(it, "{}", false) }

        assertTrue(f.remote.calls.isEmpty(), "standalone mode must not send to the desktop")
    }

    @Test
    fun `standalone mode handles clear locally`() = runTest {
        val f = Fixture(AppMode.STANDALONE)

        val result = f.router.sendAction(WsMessageType.CLEAR, "{}", true)

        assertTrue(result.isSuccess)
        assertEquals(SlideMessageType.CLEAR, f.sink.last.type)
    }

    @Test
    fun `standalone mode reports success for desktop-only actions`() = runTest {
        val f = Fixture(AppMode.STANDALONE)

        val result = f.router.sendAction(WsMessageType.ADD_TO_SCHEDULE, """{"item":{}}""", false)

        assertTrue(result.isSuccess, "a desktop-only action must not surface as an error to the user")
        assertTrue(f.sink.rendered.isEmpty())
    }

    // ── Switching ────────────────────────────────────────────────────────

    @Test
    fun `the routing decision is re-read on every call`() = runTest {
        val f = Fixture(AppMode.REMOTE)

        f.router.sendAction(WsMessageType.CLEAR, "{}", true)
        assertEquals(1, f.remote.calls.size)

        f.modeFlow.value = AppMode.STANDALONE
        f.router.sendAction(WsMessageType.CLEAR, "{}", true)
        assertEquals(1, f.remote.calls.size, "no further calls should reach the desktop")
        assertEquals(1, f.sink.rendered.size)

        f.modeFlow.value = AppMode.REMOTE
        f.router.sendAction(WsMessageType.CLEAR, "{}", true)
        assertEquals(2, f.remote.calls.size)
    }
}
