package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.ExternalDisplayViewController
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.present.OutputSink
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.SlideBus
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIScreen
import platform.UIKit.UIScreenDidConnectNotification
import platform.UIKit.UIScreenDidDisconnectNotification
import platform.UIKit.UIWindow
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val TAG = "ExternalDisplaySink"

actual fun createExternalDisplaySink(): OutputSink? = IosExternalDisplaySink()

/**
 * Projects onto a connected external screen — AirPlay or a wired adapter.
 *
 * AirPlay mirroring is the important case: once the user mirrors to an Apple TV,
 * iOS reports the TV as a second `UIScreen`, and an app that puts its own window
 * on that screen shows *different* content there instead of a mirror. That makes
 * this the only route to an Apple TV (which has no browser, so the phone-hosted
 * web page cannot reach it) and it costs nothing.
 *
 * Uses `UIScreen` connect/disconnect notifications rather than adopting a scene
 * manifest. The scene route is the modern API, but adopting `UIApplicationSceneManifest`
 * rewires the app's existing SwiftUI launch path — including the splash, the
 * `churchpresenter://` deep links and the quick actions — which is not a trade
 * worth making for a second display. This API is soft-deprecated on iOS 16+ but
 * still functional, and it is contained entirely in Kotlin: no Swift changes.
 *
 * Content comes from [SlideBus], not [render] — the window lives outside any
 * composition, and reading a StateFlow means a screen connected mid-service
 * immediately catches up to whatever is already projected.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosExternalDisplaySink : OutputSink {

    override val id: String = EXTERNAL_DISPLAY_SINK_ID

    private val _status = MutableStateFlow(
        SinkStatus(id = EXTERNAL_DISPLAY_SINK_ID, displayName = DEFAULT_NAME)
    )
    override val status: StateFlow<SinkStatus> = _status.asStateFlow()

    private var externalWindow: UIWindow? = null
    private var connectObserver: NSObjectProtocol? = null
    private var disconnectObserver: NSObjectProtocol? = null
    private var wantAttached = false

    override suspend fun attach() {
        if (wantAttached) return
        wantAttached = true
        _status.value = _status.value.copy(state = SinkState.ATTACHING)
        onMain {
            registerObservers()
            showOnBestScreen()
        }
    }

    override suspend fun detach() {
        wantAttached = false
        onMain {
            unregisterObservers()
            dismissWindow()
            _status.value = SinkStatus(id = EXTERNAL_DISPLAY_SINK_ID, displayName = DEFAULT_NAME)
        }
    }

    /** No-op by design — the external window collects [SlideBus] directly. */
    override fun render(envelope: SlideEnvelope) = Unit

    // ── Screen plumbing ──────────────────────────────────────────────────

    private fun registerObservers() {
        if (connectObserver != null) return
        val center = NSNotificationCenter.defaultCenter
        val queue = NSOperationQueue.mainQueue
        connectObserver = center.addObserverForName(
            name = UIScreenDidConnectNotification,
            `object` = null,
            queue = queue,
        ) { _: NSNotification? -> showOnBestScreen() }
        disconnectObserver = center.addObserverForName(
            name = UIScreenDidDisconnectNotification,
            `object` = null,
            queue = queue,
        ) { _: NSNotification? ->
            // The screen went away — drop the window but stay attached so
            // reconnecting mid-service just works.
            dismissWindow()
            if (wantAttached) {
                _status.value = _status.value.searching(DEFAULT_NAME)
            }
        }
    }

    private fun unregisterObservers() {
        val center = NSNotificationCenter.defaultCenter
        connectObserver?.let(center::removeObserver)
        disconnectObserver?.let(center::removeObserver)
        connectObserver = null
        disconnectObserver = null
    }

    /** The first screen that is not the device's own. */
    private fun externalScreen(): UIScreen? =
        UIScreen.screens.filterIsInstance<UIScreen>().firstOrNull { it != UIScreen.mainScreen }

    private fun showOnBestScreen() {
        if (!wantAttached) return
        val screen = externalScreen()
        if (screen == null) {
            dismissWindow()
            _status.value = _status.value.searching(DEFAULT_NAME)
            return
        }
        if (externalWindow?.screen == screen) return

        dismissWindow()
        runCatching {
            UIWindow(frame = screen.bounds).apply {
                this.screen = screen
                rootViewController = ExternalDisplayViewController()
                hidden = false
            }
        }.onSuccess { window ->
            externalWindow = window
            val resolution = screen.describeResolution()
            _status.value = _status.value.copy(
                state = SinkState.ATTACHED,
                detail = resolution,
                clientCount = 1,
            )
            Logger.d(TAG, "attached to external screen $resolution")
        }.onFailure { e ->
            _status.value = _status.value.copy(state = SinkState.ERROR, detail = e.message)
            Logger.e(TAG, "failed to open external window: ${e.message}", e)
        }
    }

    private fun dismissWindow() {
        externalWindow?.let {
            it.hidden = true
            it.rootViewController = null
        }
        externalWindow = null
    }

    /** "1920×1080" in points, for the outputs list. */
    private fun UIScreen.describeResolution(): String = bounds.useContents {
        "${size.width.toInt()}×${size.height.toInt()}"
    }

    private fun onMain(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) { block() }
    }

    private companion object {
        const val DEFAULT_NAME = "External display"
    }
}
