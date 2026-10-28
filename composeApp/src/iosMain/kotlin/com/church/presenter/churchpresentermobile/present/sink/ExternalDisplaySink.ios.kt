package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.externalDisplayViewController
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.present.OutputSink
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.UIKit.UIScreen
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

private const val TAG = "ExternalDisplaySink"

actual fun createExternalDisplaySink(): OutputSink? = IosExternalDisplaySink()

/**
 * Projects onto a connected external screen — AirPlay or a wired adapter.
 *
 * AirPlay mirroring is the important case: once the user mirrors to an Apple TV,
 * iOS offers the TV to the app as a second scene, and an app that puts its own
 * window there shows *different* content instead of a mirror. That makes this
 * the only route to an Apple TV (which has no browser, so the phone-hosted web
 * page cannot reach it) and it costs nothing.
 *
 * The app is scene-based (SwiftUI `App` lifecycle), so the window must be
 * created on the external `UIWindowScene` — a window placed with
 * `UIWindow.screen` and the `UIScreen` connect notifications never appears in a
 * scene-based app, which is why AirPlay used to show only the mirror. Swift
 * claims the scene in `AppDelegate.application(_:configurationForConnecting:options:)`
 * and hands it over through [ExternalDisplayScenes]. With no window on the
 * scene, iOS falls back to mirroring, so detaching restores the mirror.
 *
 * Content comes from [SlideBus][com.church.presenter.churchpresentermobile.present.SlideBus],
 * not [render] — the window lives outside any composition, and reading a
 * StateFlow means a screen connected mid-service immediately catches up to
 * whatever is already projected.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosExternalDisplaySink : OutputSink {

    override val id: String = EXTERNAL_DISPLAY_SINK_ID

    private val _status = MutableStateFlow(
        SinkStatus(id = EXTERNAL_DISPLAY_SINK_ID, displayName = DEFAULT_NAME)
    )
    override val status: StateFlow<SinkStatus> = _status.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var externalWindow: UIWindow? = null
    private var sceneWatcher: Job? = null

    override suspend fun attach() {
        if (sceneWatcher != null) return
        _status.value = _status.value.copy(state = SinkState.ATTACHING)
        // Follows connects and disconnects while attached, so reconnecting
        // mid-service just works.
        sceneWatcher = scope.launch {
            ExternalDisplayScenes.scene.collect { showOn(it) }
        }
    }

    override suspend fun detach() {
        sceneWatcher?.cancel()
        sceneWatcher = null
        withContext(Dispatchers.Main) {
            dismissWindow()
            _status.value = SinkStatus(id = EXTERNAL_DISPLAY_SINK_ID, displayName = DEFAULT_NAME)
        }
    }

    /** No-op by design — the external window collects SlideBus directly. */
    override fun render(envelope: SlideEnvelope) = Unit

    // ── Scene plumbing ───────────────────────────────────────────────────

    private fun showOn(scene: UIWindowScene?) {
        if (scene == null) {
            dismissWindow()
            _status.value = _status.value.searching(DEFAULT_NAME)
            return
        }
        if (externalWindow?.windowScene == scene) return

        dismissWindow()
        runCatching {
            UIWindow(windowScene = scene).apply {
                rootViewController = externalDisplayViewController()
                hidden = false
            }
        }.onSuccess { window ->
            externalWindow = window
            val resolution = scene.screen.describeResolution()
            _status.value = _status.value.copy(
                state = SinkState.ATTACHED,
                detail = resolution,
                clientCount = 1,
            )
            Logger.d(TAG, "attached to external scene $resolution")
        }.onFailure { e ->
            _status.value = _status.value.failed(e.message, DEFAULT_NAME)
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

    private companion object {
        const val DEFAULT_NAME = "External display"
    }
}
