package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "LocalWebServer"

/**
 * Ktor CIO implementation shared by Android and iOS.
 *
 * The broadcast channel is a [MutableStateFlow] rather than a queue, and each
 * connected session simply collects it. That gives three things for free: a
 * newly connected display is sent the current slide immediately, a slow client
 * coalesces rather than backing up, and there is no per-session bookkeeping to
 * leak when a TV is switched off mid-service.
 */
actual class LocalWebServer actual constructor(private val assets: WebAssets) {

    private val _clientCount = MutableStateFlow(0)
    actual val clientCount: StateFlow<Int> = _clientCount.asStateFlow()

    /** The latest envelope. Replayed to every new connection. */
    private val current = MutableStateFlow<String?>(null)

    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    actual suspend fun start(preferredPort: Int, candidates: List<Int>): Int {
        if (server != null) return boundPort

        // Ports are tried by actually binding them: a pre-flight "is it free?"
        // probe would be a race, and CIO gives no cheaper way to ask.
        val ordered = buildList {
            add(preferredPort)
            candidates.forEach { if (it != preferredPort) add(it) }
            // 0 = let the OS pick. An odd port still works; failing to start does not.
            add(EPHEMERAL_PORT)
        }

        for (port in ordered) {
            val attempt = runCatching { startOn(port) }
            attempt.onSuccess { actualPort ->
                boundPort = actualPort
                Logger.d(TAG, "listening on port $actualPort")
                return actualPort
            }
            attempt.onFailure { e ->
                if (e is CancellationException) throw e
                Logger.d(TAG, "port $port unavailable: ${e.message}")
            }
        }
        throw IllegalStateException("Could not bind any port for the presentation server")
    }

    private suspend fun startOn(port: Int): Int {
        val engine = embeddedServer(CIO, port = port) { module() }
        engine.start(wait = false)
        // resolvedConnectors() both reports the real port (needed when we asked
        // for 0) and surfaces a bind failure that start(wait = false) swallowed.
        val resolved = engine.engine.resolvedConnectors().firstOrNull()
            ?: run {
                engine.stop(gracePeriodMillis = 0, timeoutMillis = 0)
                error("no connector resolved on port $port")
            }
        server = engine
        return resolved.port
    }

    actual suspend fun stop() {
        val running = server ?: return
        server = null
        _clientCount.value = 0
        runCatching {
            running.stop(gracePeriodMillis = STOP_GRACE_MS, timeoutMillis = STOP_TIMEOUT_MS)
        }.onFailure { Logger.e(TAG, "stop failed: ${it.message}") }
        Logger.d(TAG, "stopped")
    }

    actual fun publish(json: String) {
        current.value = json
    }

    private fun io.ktor.server.application.Application.module() {
        // A hall network drops idle sockets silently; pings keep the display
        // connection alive through the long gaps between slide changes.
        install(WebSockets) {
            pingPeriodMillis = PING_PERIOD_MS
            timeoutMillis = PING_TIMEOUT_MS
        }
        routing {
            get("/{path...}") {
                val requested = call.request.local.uri
                val asset = assets.forPath(requested)
                if (asset == null) {
                    call.respondText(
                        text = "Not found",
                        contentType = ContentType.Text.Plain,
                        status = HttpStatusCode.NotFound,
                    )
                } else {
                    call.respondBytes(
                        bytes = asset.bytes,
                        contentType = ContentType.parse(asset.contentType),
                    )
                }
            }

            webSocket("/live") {
                _clientCount.value += 1
                Logger.d(TAG, "display connected (${_clientCount.value} total)")
                try {
                    // collectLatest replays the current frame on connect and
                    // drops superseded frames for a slow display.
                    current.collectLatest { frame ->
                        if (frame != null) send(Frame.Text(frame))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.d(TAG, "display disconnected: ${e.message}")
                } finally {
                    _clientCount.value = (_clientCount.value - 1).coerceAtLeast(0)
                }
            }
        }
    }

    private var boundPort: Int = 0

    private companion object {
        const val EPHEMERAL_PORT = 0
        const val STOP_GRACE_MS = 300L
        const val STOP_TIMEOUT_MS = 1_000L
        const val PING_PERIOD_MS = 20_000L
        const val PING_TIMEOUT_MS = 30_000L
    }
}
