package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.present.LocalWebServer
import com.church.presenter.churchpresentermobile.present.OutputSink
import com.church.presenter.churchpresentermobile.present.PresentationKeepAlive
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.WebAssets
import com.church.presenter.churchpresentermobile.present.localIpAddress
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "WebPageSink"

/** Stable id of the phone-hosted web page sink within a registry. */
const val WEB_PAGE_SINK_ID = "web_page"

/**
 * Serves the projected display page from the phone itself.
 *
 * Any browser on the same network — a smart TV, a laptop wired to a projector,
 * a spare phone — opens the URL and becomes an audience screen. Several can
 * connect at once and all stay in step. Nothing leaves the local network and
 * nothing is fetched from the internet, so this works in a hall whose Wi-Fi has
 * no uplink at all.
 *
 * @param onPortBound Called with the port actually bound, so the caller can
 *   remember it and hand the operator a stable URL between services.
 */
class WebPageSink(
    private val preferredPort: Int,
    private val onPortBound: (Int) -> Unit = {},
) : OutputSink {

    override val id: String = WEB_PAGE_SINK_ID

    private val _status = MutableStateFlow(
        SinkStatus(id = WEB_PAGE_SINK_ID, displayName = DISPLAY_NAME)
    )
    override val status: StateFlow<SinkStatus> = _status.asStateFlow()

    private val json = Json { encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob())

    private var server: LocalWebServer? = null
    private var clientWatcher: Job? = null

    /** The last envelope, replayed by the server to any display that connects later. */
    private var lastEnvelope: SlideEnvelope? = null

    override suspend fun attach() {
        if (server != null) return
        _status.value = _status.value.copy(state = SinkState.ATTACHING, detail = null)

        val address = localIpAddress()
        if (address == null) {
            // No LAN address means no device could reach us anyway. Say so
            // plainly rather than starting a server nobody can find.
            _status.value = _status.value.copy(state = SinkState.ERROR, detail = NO_NETWORK)
            Logger.e(TAG, "no local network address — not starting the server")
            return
        }

        val assets = WebAssets.load()
        if (assets.isEmpty) {
            _status.value = _status.value.copy(state = SinkState.ERROR, detail = NO_ASSETS)
            Logger.e(TAG, "display assets missing from the bundle — not starting the server")
            return
        }

        val instance = LocalWebServer(assets)
        val started = runCatching {
            instance.start(preferredPort, ApiConstants.STANDALONE_PORT_CANDIDATES.toList())
        }

        started.onSuccess { port ->
            server = instance
            onPortBound(port)
            val url = "http://$address:$port"
            _status.value = _status.value.copy(
                state = SinkState.ATTACHED,
                detail = url,
                clientCount = 0,
            )
            // Hold the CPU and Wi-Fi awake for as long as we are serving, or the
            // display drops out the moment the operator's phone locks.
            PresentationKeepAlive.start(url)
            // Bring a display that connects later straight up to date.
            lastEnvelope?.let { instance.publish(json.encodeToString(it)) }
            clientWatcher = scope.launch {
                instance.clientCount.collect { count ->
                    _status.value = _status.value.copy(clientCount = count)
                }
            }
            Logger.d(TAG, "serving on http://$address:$port")
        }.onFailure { e ->
            _status.value = _status.value.copy(state = SinkState.ERROR, detail = e.message)
            Logger.e(TAG, "failed to start the presentation server: ${e.message}", e)
        }
    }

    override suspend fun detach() {
        PresentationKeepAlive.stop()
        clientWatcher?.cancel()
        clientWatcher = null
        server?.stop()
        server = null
        _status.value = SinkStatus(id = WEB_PAGE_SINK_ID, displayName = DISPLAY_NAME)
    }

    override fun render(envelope: SlideEnvelope) {
        lastEnvelope = envelope
        val running = server ?: return
        runCatching { running.publish(json.encodeToString(envelope)) }
            .onFailure { Logger.e(TAG, "publish failed: ${it.message}") }
    }

    /** Releases the sink's coroutine scope. Call when the sink is discarded for good. */
    fun dispose() {
        scope.cancel()
    }

    private companion object {
        const val DISPLAY_NAME = "Browser screen"
        const val NO_NETWORK = "Not connected to Wi-Fi"
        const val NO_ASSETS = "Display files missing from this build"
    }
}
