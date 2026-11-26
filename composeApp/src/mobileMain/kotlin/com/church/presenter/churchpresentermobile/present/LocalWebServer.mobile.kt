package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.install
import io.ktor.server.application.serverConfig
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
actual class LocalWebServer actual constructor(
    private val assets: WebAssets,
    private val photos: PhotoSource,
    private val videos: VideoSource,
) {

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
                // A CancellationException here is ambiguous: CIO reports a failed
                // bind by cancelling its own engine job, so the failure arrives as
                // "LazyStandaloneCoroutine is cancelling" with the BindException as
                // its cause. Rethrowing on that (as a naive cancellation check does)
                // abandons the ladder on the first taken port — observed on device,
                // where an occupied 8766 left the sink in ERROR instead of moving to
                // 8767. Only a cancellation of *this* coroutine may propagate.
                if (e is CancellationException && !currentCoroutineContext().isActive) throw e
                Logger.d(TAG, "port $port unavailable: ${(e.cause ?: e).message}")
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
            // Declared before the catch-all so the operator's photos are not
            // answered with the bundled page's 404. Ktor prefers the more
            // specific route, but the order also says which is which.
            get("/$PHOTO_ROUTE/{id}") {
                val photo = call.parameters["id"]?.let(photos::photo)
                if (photo == null) {
                    call.respondText(
                        text = "Not found",
                        contentType = ContentType.Text.Plain,
                        status = HttpStatusCode.NotFound,
                    )
                } else {
                    // A photo's URL carries a UUID minted when it was picked, so the
                    // bytes behind it can never change: the answer is safely
                    // immutable. Without saying so a TV browser is free to
                    // revalidate — or refetch — on every reconnect mid-service.
                    call.response.header(HttpHeaders.CacheControl, PHOTO_CACHE_CONTROL)
                    call.respondBytes(
                        bytes = photo.bytes,
                        contentType = ContentType.parse(photo.contentType),
                    )
                }
            }

            // Videos stream a range at a time: a TV's player asks for the part it is
            // about to play, and AVPlayer will not play at all from a server that
            // ignores the Range header. Nothing is ever read whole into memory.
            get("/$VIDEO_ROUTE/{name}") {
                val video = call.parameters["name"]?.let(videos::video)
                if (video == null) {
                    call.respondText(
                        text = "Not found",
                        contentType = ContentType.Text.Plain,
                        status = HttpStatusCode.NotFound,
                    )
                    return@get
                }
                call.response.header(HttpHeaders.AcceptRanges, "bytes")
                call.response.header(HttpHeaders.CacheControl, PHOTO_CACHE_CONTROL)
                when (val range = parseByteRange(call.request.headers[HttpHeaders.Range], video.size)) {
                    RangeRequest.Unsatisfiable -> {
                        call.response.header(HttpHeaders.ContentRange, "bytes */${video.size}")
                        call.respondText("", status = HttpStatusCode.RequestedRangeNotSatisfiable)
                    }
                    RangeRequest.Whole -> call.respond(VideoContent(video, 0L, video.size - 1, HttpStatusCode.OK))
                    is RangeRequest.Part -> {
                        val contentRange = "bytes ${range.start}-${range.end}/${video.size}"
                        call.response.header(HttpHeaders.ContentRange, contentRange)
                        call.respond(VideoContent(video, range.start, range.end, HttpStatusCode.PartialContent))
                    }
                }
            }

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

    /**
     * Bytes [start]..[end] of [video], written a chunk at a time off the caller's
     * thread — a 1 GB file must never be one allocation.
     */
    private class VideoContent(
        private val video: ServedVideo,
        private val start: Long,
        private val end: Long,
        override val status: HttpStatusCode,
    ) : OutgoingContent.WriteChannelContent() {
        override val contentType: ContentType = ContentType.parse(video.contentType)
        override val contentLength: Long = end - start + 1

        override suspend fun writeTo(channel: ByteWriteChannel) {
            var position = start
            while (position <= end) {
                val length = minOf(VIDEO_CHUNK_BYTES.toLong(), end - position + 1).toInt()
                val bytes = withContext(Dispatchers.Default) { video.read(position, length) }
                if (bytes == null || bytes.isEmpty()) break
                channel.writeFully(bytes)
                position += bytes.size
            }
            channel.flush()
        }
    }

    private companion object {
        /** A year, and immutable: the id in the URL is unique to these bytes. */
        const val PHOTO_CACHE_CONTROL = "public, max-age=31536000, immutable"

        const val EPHEMERAL_PORT = 0

        /** How much of a video is read per write — enough to keep a TV fed, small enough to never matter. */
        const val VIDEO_CHUNK_BYTES = 256 * 1024

        /** Longest a single bind attempt may take before the next port is tried. */
        const val BIND_TIMEOUT_MS = 5_000L
        const val STOP_GRACE_MS = 300L
        const val STOP_TIMEOUT_MS = 1_000L
        const val PING_PERIOD_MS = 20_000L
        const val PING_TIMEOUT_MS = 30_000L
    }
}
