package com.church.presenter.churchpresentermobile.present.sink

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How many screens are actually watching.
 *
 * The count is the operator's only confirmation that the TV at the back really
 * did open the page — the phone cannot see the screen it is serving. It arrives
 * asynchronously from the server rather than at attach time, which is why these
 * few run on real time with a hard budget rather than on the test clock: the
 * sink collects the count on its own scope, and a virtual clock would expire
 * the wait before that collection ever ran.
 *
 * In `jvmTest` rather than `commonTest` because `runBlocking` is a JVM idea —
 * Kotlin/JS has no thread to block, so a common-source version does not compile
 * for the browser targets. This is the run JaCoCo measures either way.
 */
class WebPageSinkClientsTest {

    /** Real time, because the sink's watcher runs on a real dispatcher. */
    private fun clientTest(block: suspend CoroutineScope.() -> Unit) =
        runBlocking { withTimeout(TEST_BUDGET_MS) { block() } }

    private suspend fun WebPageSink.awaitClients(count: Int) =
        status.first { it.clientCount == count }

    @Test
    fun `a display connecting is counted`() = clientTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        server.setClients(1)

        assertEquals(1, sink.awaitClients(1).clientCount)
    }

    @Test
    fun `several displays are all counted`() = clientTest {
        // A TV, a laptop on the projector and a spare phone is an ordinary
        // Sunday, not an unusual one.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        server.setClients(3)

        assertEquals(3, sink.awaitClients(3).clientCount)
    }

    @Test
    fun `a display leaving is counted down again`() = clientTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        server.setClients(2)
        sink.awaitClients(2)

        server.setClients(1)

        assertEquals(1, sink.awaitClients(1).clientCount)
    }

    @Test
    fun `every display leaving takes the count to nothing`() = clientTest {
        // The row has to admit that nobody is watching, or an operator whose TV
        // dropped out has no way to tell.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        server.setClients(1)
        sink.awaitClients(1)

        server.setClients(0)

        assertEquals(0, sink.awaitClients(0).clientCount)
    }

    @Test
    fun `the count does not disturb the address`() = clientTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        server.setClients(2)
        sink.awaitClients(2)

        assertEquals("http://192.168.1.50:8080", sink.status.value.detail)
    }

    @Test
    fun `the count does not disturb the attached state`() = clientTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        server.setClients(2)
        sink.awaitClients(2)

        assertTrue(sink.status.value.isAttached)
    }

    @Test
    fun `a count that arrives before anything connects is nothing`() = clientTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)

        sink.attach()

        assertEquals(0, sink.status.value.clientCount)
    }

    @Test
    fun `the count from a previous serving does not carry over`() = clientTest {
        // Detaching drops the watcher, so a stale figure cannot outlive it.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        server.setClients(2)
        sink.awaitClients(2)

        sink.detach()

        assertEquals(0, sink.status.value.clientCount)
    }

    @Test
    fun `a detached sink stops counting`() = clientTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        server.setClients(2)
        sink.awaitClients(2)
        sink.detach()

        server.setClients(5)

        assertEquals(0, sink.status.value.clientCount)
    }

    @Test
    fun `re-attaching starts counting again`() = clientTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.detach()
        sink.attach()

        server.setClients(1)

        assertEquals(1, sink.awaitClients(1).clientCount)
    }

    @Test
    fun `a disposed sink stops counting`() = clientTest {
        // Disposal is the end of the sink's life; nothing should still be
        // writing to its status.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.dispose()

        server.setClients(4)

        assertEquals(0, sink.status.value.clientCount)
    }

    @Test
    fun `a count arriving while a frame is published is not lost`() = clientTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope("Amazing grace"))
        server.setClients(2)

        assertEquals(2, sink.awaitClients(2).clientCount)
        assertTrue(server.published.isNotEmpty())
    }

    @Test
    fun `a failed attach counts nobody`() = clientTest {
        val server = FakeDisplayServer(failToStart = IllegalStateException("no port free"))
        val sink = webSink(server = server)

        sink.attach()
        server.setClients(3)

        assertEquals(0, sink.status.value.clientCount)
    }

    private companion object {
        /** A stall fails fast rather than blocking the suite. */
        const val TEST_BUDGET_MS = 20_000L
    }
}
