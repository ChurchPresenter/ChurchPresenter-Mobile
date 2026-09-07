package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.PhotoSource
import com.church.presenter.churchpresentermobile.present.WebAsset
import com.church.presenter.churchpresentermobile.present.WebAssets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared setup for the output sinks' tests.
 *
 * The real display server is an `expect class` whose JVM actual refuses to
 * start — a desktop host does not serve the page — so every attach would fail
 * and the serving path could not be reached at all. [FakeDisplayServer] stands
 * in for it through the [DisplayServer] seam, and records what the sink asked it
 * to do.
 */
internal class FakeDisplayServer(
    /** The port [start] reports having bound, as a real server picks one. */
    var boundPort: Int = 8080,
    /** When set, [start] fails with this instead of binding. */
    var failToStart: Throwable? = null,
    /** When set, [publish] throws — a socket that died between frames. */
    var failToPublish: Throwable? = null,
) : DisplayServer {

    private val _clientCount = MutableStateFlow(0)
    override val clientCount: StateFlow<Int> = _clientCount.asStateFlow()

    /** Every frame the sink has pushed, in order, as raw JSON. */
    val published = mutableListOf<String>()

    var startCount = 0
        private set

    var stopCount = 0
        private set

    /** The arguments of the last [start], for the port-preference rules. */
    var lastPreferredPort: Int? = null
        private set

    var lastCandidates: List<Int>? = null
        private set

    /** A display connecting or leaving, as the real server reports it. */
    fun setClients(count: Int) {
        _clientCount.value = count
    }

    override suspend fun start(preferredPort: Int, candidates: List<Int>): Int {
        startCount++
        lastPreferredPort = preferredPort
        lastCandidates = candidates
        failToStart?.let { throw it }
        return boundPort
    }

    override suspend fun stop() {
        stopCount++
    }

    override fun publish(json: String) {
        failToPublish?.let { throw it }
        published += json
    }
}

/** The bundled display page, as a sink that is going to serve it sees it. */
internal fun bundledAssets() = WebAssets(
    mapOf("index.html" to WebAsset("<html></html>".encodeToByteArray(), "text/html; charset=utf-8"))
)

/** A build whose display page did not make it into the bundle. */
internal fun missingAssets() = WebAssets(emptyMap())

/**
 * A sink wired to [server], on a phone that has [address] and assets that load.
 *
 * Every argument has the shape the real thing has when everything is working, so
 * a test only names what it is changing.
 */
internal fun webSink(
    server: FakeDisplayServer = FakeDisplayServer(),
    preferredPort: Int = 8080,
    address: String? = "192.168.1.50",
    assets: WebAssets = bundledAssets(),
    onPortBound: (Int) -> Unit = {},
    onBaseUrl: (String?) -> Unit = {},
    photos: PhotoSource = PhotoSource.NONE,
    loadAssets: (suspend () -> WebAssets)? = null,
) = WebPageSink(
    preferredPort = preferredPort,
    onPortBound = onPortBound,
    photos = photos,
    onBaseUrl = onBaseUrl,
    address = { address },
    loadAssets = loadAssets ?: { assets },
    serverFactory = { _, _ -> server },
)

internal fun envelope(body: String = "Amazing grace", rev: Long = 1L) =
    SlideEnvelope(rev = rev, slide = Slide(kind = SlideKind.SONG, body = body))
