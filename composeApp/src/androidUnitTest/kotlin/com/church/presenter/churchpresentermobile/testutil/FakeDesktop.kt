package com.church.presenter.churchpresentermobile.testutil

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A stand-in for the ChurchPresenter desktop's companion WebSocket.
 *
 * Speaks the same `/ws` protocol: it accepts a connection, records the
 * handshake, hands back a reply for each action frame, and can push events at
 * the phone. That is enough to drive [ServerEventService] over a real socket,
 * which is the only way to reach its connect loop, its frame routing, and the
 * half of `sendAction` that runs once a session exists.
 *
 * Bound on an ephemeral loopback port; nothing leaves the machine.
 */
class FakeDesktop {

    /** Frames the phone has sent, in order. */
    val received: Channel<String> = Channel(Channel.UNLIMITED)

    /** Handshake headers of each connection, newest last. */
    val handshakes: MutableStateFlow<List<Map<String, List<String>>>> = MutableStateFlow(emptyList())

    /** Query parameters of each connection, newest last. */
    val queries: MutableStateFlow<List<Map<String, List<String>>>> = MutableStateFlow(emptyList())

    /** How many sessions have been opened. */
    val connections: MutableStateFlow<Int> = MutableStateFlow(0)

    /** Frames to push at the phone; every connected session sends each one. */
    val pushes: MutableSharedFlow<String> = MutableSharedFlow(extraBufferCapacity = 64)

    /**
     * What to answer an inbound action frame with. Null sends nothing, which is
     * what a desktop showing an approval dialog looks like.
     */
    var reply: (String) -> String? = { """{"ok":true}""" }

    /** Set to close the session as soon as it opens — a server that hangs up. */
    var closeImmediately: Boolean = false

    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null

    /** Starts on an ephemeral port and returns it. */
    suspend fun start(): Int {
        val engine = embeddedServer(CIO, port = 0) {
            install(WebSockets)
            routing {
                webSocket("/ws") {
                    connections.value += 1
                    handshakes.value = handshakes.value + call.request.headers.entries()
                        .associate { it.key to it.value }
                    queries.value = queries.value + call.request.queryParameters.entries()
                        .associate { it.key to it.value }
                    if (closeImmediately) return@webSocket

                    val pusher = launch { pushes.collect { send(Frame.Text(it)) } }
                    try {
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val text = frame.readText()
                            received.send(text)
                            reply(text)?.let { send(Frame.Text(it)) }
                        }
                    } finally {
                        pusher.cancel()
                    }
                }
            }
        }
        engine.start(wait = false)
        server = engine
        return engine.engine.resolvedConnectors().first().port
    }

    suspend fun stop() {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 500)
        server = null
    }

    /** Waits until at least [count] sessions have been opened. */
    suspend fun awaitConnections(count: Int): Int = connections.first { it >= count }

    /** The next frame the phone sends. */
    suspend fun nextFrame(): String = received.receive()
}
