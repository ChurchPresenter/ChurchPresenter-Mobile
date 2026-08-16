package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.application.serverConfig
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
        // CIO reports a failed bind by failing its internal accept job, and that
        // job is a root coroutine — with no handler installed, the BindException
        // goes straight to the platform's uncaught-exception handler and kills
        // the app. It never reaches the caller's runCatching, so the port ladder
        // below never got its chance. Routing the failure into `failed` turns a
        // crash into an ordinary failed attempt.
        val failed = CompletableDeferred<Throwable>()
        val handler = CoroutineExceptionHandler { _, e ->
            if (!failed.complete(e)) Logger.e(TAG, "presentation server failed: ${e.message}", e)
        }

        val config = serverConfig {
            parentCoroutineContext = handler
            module { module() }
        }
        val engine = embeddedServer(CIO, config) {
            connector { this.port = port }
            // Sockets accepted by the previous run linger in TIME_WAIT for about
            // a minute after the process dies. Without SO_REUSEADDR, an operator
            // who restarts the app inside that window cannot rebind the port —
            // and it is the port printed on the URL the room was just given.
            reuseAddress = true
        }
        engine.start(wait = false)

        // Whichever comes first: connectors resolve (reporting the real port,
        // which matters when we asked for 0) or the bind fails. resolvedConnectors()
        // simply never completes on a failed bind, so it cannot be awaited alone.
        val outcome = coroutineScope {
            val race = CompletableDeferred<Result<Int>>()
            val onFailure = launch { race.complete(Result.failure(failed.await())) }
            val onBound = launch {
                race.complete(
                    runCatching {
                        engine.engine.resolvedConnectors().firstOrNull()?.port
                            ?: error("no connector resolved on port $port")
                    }
                )
            }
            val result = withTimeoutOrNull(BIND_TIMEOUT_MS) { race.await() }
            onFailure.cancel()
            onBound.cancel()
            result ?: Result.failure(IllegalStateException("timed out binding port $port"))
        }

        return outcome
            .onFailure { runCatching { engine.stop(gracePeriodMillis = 0, timeoutMillis = 0) } }
            .getOrThrow()
            .also { server = engine }
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

        /** Longest a single bind attempt may take before the next port is tried. */
        const val BIND_TIMEOUT_MS = 5_000L
        const val STOP_GRACE_MS = 300L
        const val STOP_TIMEOUT_MS = 1_000L
        const val PING_PERIOD_MS = 20_000L
        const val PING_TIMEOUT_MS = 30_000L
    }
}
