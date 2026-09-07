package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.present.OutputSink
import com.church.presenter.churchpresentermobile.present.PhotoSource
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
 * @param photos The operator's picked photos, which this server also serves so
 *   that both displays load an image from the same address.
 * @param onBaseUrl Called with the address the server is reachable at, and with
 *   null when it stops — what tells the photo library whether it has anywhere to
 *   serve from.
 * @param address This phone's LAN address. A seam over [localIpAddress]: whether
 *   there is an address at all decides between serving and saying so, and a
 *   build agent's answer is not the one a test wants to depend on.
 * @param loadAssets The bundled display page. A seam over [WebAssets.load] for
 *   the same reason — the bundle is not readable off a device.
 * @param serverFactory Builds the embedded server. Seamed so the attach path can
 *   be exercised without one, not so a different server can be substituted — see
 *   [DisplayServer], which exists only because the platform server is an
 *   `expect class` a test cannot stand in for.
 */
class WebPageSink(
    private val preferredPort: Int,
    private val onPortBound: (Int) -> Unit = {},
    private val photos: PhotoSource = PhotoSource.NONE,
    private val onBaseUrl: (String?) -> Unit = {},
    private val address: () -> String? = { localIpAddress() },
    private val loadAssets: suspend () -> WebAssets = { WebAssets.load() },
    private val serverFactory: (WebAssets, PhotoSource) -> DisplayServer = localWebServerFactory,
) : OutputSink {

    override val id: String = WEB_PAGE_SINK_ID

    private val _status = MutableStateFlow(
        SinkStatus(id = WEB_PAGE_SINK_ID, displayName = DISPLAY_NAME)
    )
    override val status: StateFlow<SinkStatus> = _status.asStateFlow()

    private val json = Json { encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob())

    private var server: DisplayServer? = null
    private var clientWatcher: Job? = null

    /** The last envelope, replayed by the server to any display that connects later. */
    private var lastEnvelope: SlideEnvelope? = null

    override suspend fun attach() {
        if (server != null) return
        _status.value = _status.value.copy(state = SinkState.ATTACHING, detail = null)

        val lanAddress = address()
        if (lanAddress == null) {
            // No LAN address means no device could reach us anyway. Say so
            // plainly rather than starting a server nobody can find.
            _status.value = _status.value.failed(NO_NETWORK, DISPLAY_NAME)
            Logger.e(TAG, "no local network address — not starting the server")
            return
        }

        val assets = loadAssets()
        if (assets.isEmpty) {
            _status.value = _status.value.failed(NO_ASSETS, DISPLAY_NAME)
            Logger.e(TAG, "display assets missing from the bundle — not starting the server")
            return
        }

        // Building the server is inside the guard too: everything from here to a
        // bound port is one attempt, and any part of it failing has to leave a
        // reason on the outputs row rather than escaping into the caller.
        val started = runCatching {
            val instance = serverFactory(assets, photos)
            instance to instance.start(preferredPort, ApiConstants.STANDALONE_PORT_CANDIDATES.toList())
        }

        started.onSuccess { (instance, port) ->
            server = instance
            onPortBound(port)
            val url = "http://$lanAddress:$port"
            onBaseUrl(url)
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
            Logger.d(TAG, "serving on $url")
        }.onFailure { e ->
            _status.value = _status.value.failed(e.message, DISPLAY_NAME)
            Logger.e(TAG, "failed to start the presentation server: ${e.message}", e)
        }
    }

    override suspend fun detach() {
        onBaseUrl(null)
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
