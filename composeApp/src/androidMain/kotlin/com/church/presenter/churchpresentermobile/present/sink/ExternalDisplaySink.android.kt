package com.church.presenter.churchpresentermobile.present.sink

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.present.OutputSink
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.SlideBus
import com.church.presenter.churchpresentermobile.ui.PresentationOwners
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneOutputScreen
import com.church.presenter.churchpresentermobile.util.ActivityHolder
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ExternalDisplaySink"

actual fun createExternalDisplaySink(): OutputSink? = AndroidExternalDisplaySink()

/**
 * Projects onto any secondary display Android knows about.
 *
 * `DISPLAY_CATEGORY_PRESENTATION` covers wired HDMI/USB-C-DisplayPort, Miracast,
 * and — usefully — a Chromecast session started from the system's "Cast screen"
 * quick setting. In every case the phone keeps showing the controller while the
 * TV shows the slide, which is the whole point: the phone drives the screen, it
 * never mirrors it.
 *
 * Content comes from [SlideBus] rather than from [render]. The presentation
 * window is created by platform code outside the Compose tree, so it cannot be
 * handed state through a normal composition — it collects the bus instead, and
 * that also means a display plugged in mid-service immediately shows whatever is
 * already projected rather than waiting for the next slide change.
 */
private class AndroidExternalDisplaySink : OutputSink {

    override val id: String = EXTERNAL_DISPLAY_SINK_ID

    private val _status = MutableStateFlow(
        SinkStatus(id = EXTERNAL_DISPLAY_SINK_ID, displayName = DEFAULT_NAME)
    )
    override val status: StateFlow<SinkStatus> = _status.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var presentation: SlidePresentation? = null
    private var displayListener: DisplayManager.DisplayListener? = null
    private var wantAttached = false

    override suspend fun attach() {
        if (wantAttached) return
        wantAttached = true
        _status.value = _status.value.copy(state = SinkState.ATTACHING)
        runOnMain {
            registerDisplayListener()
            showOnBestDisplay()
        }
    }

    override suspend fun detach() {
        wantAttached = false
        runOnMain {
            unregisterDisplayListener()
            dismissPresentation()
            _status.value = SinkStatus(id = EXTERNAL_DISPLAY_SINK_ID, displayName = DEFAULT_NAME)
        }
    }

    /**
     * No-op by design — the presentation window collects [SlideBus] directly.
     * Kept so the sink still satisfies the interface and so a future variant
     * that needs push delivery has an obvious place to put it.
     */
    override fun render(envelope: SlideEnvelope) = Unit

    // ── Display plumbing ─────────────────────────────────────────────────

    private fun displayManager(): DisplayManager? =
        ActivityHolder.current?.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private fun registerDisplayListener() {
        if (displayListener != null) return
        val manager = displayManager() ?: return
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = showOnBestDisplay()
            override fun onDisplayChanged(displayId: Int) = showOnBestDisplay()
            override fun onDisplayRemoved(displayId: Int) {
                // The TV was unplugged or the cast session ended. Drop the window
                // but stay "attached" so re-plugging mid-service just works.
                if (presentation?.display?.displayId == displayId) {
                    dismissPresentation()
                    _status.value = _status.value.searching(DEFAULT_NAME)
                }
            }
        }
        manager.registerDisplayListener(listener, mainHandler)
        displayListener = listener
    }

    private fun unregisterDisplayListener() {
        displayListener?.let { displayManager()?.unregisterDisplayListener(it) }
        displayListener = null
    }

    /** Picks the first presentation display and shows the slide window on it. */
    private fun showOnBestDisplay() {
        if (!wantAttached) return
        val activity = ActivityHolder.current ?: return
        val display = displayManager()
            ?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            ?.firstOrNull()

        if (display == null) {
            dismissPresentation()
            _status.value = _status.value.searching(DEFAULT_NAME)
            return
        }
        if (presentation?.display?.displayId == display.displayId && presentation?.isShowing == true) return

        dismissPresentation()
        runCatching {
            SlidePresentation(activity, display).also { window ->
                window.show()
                presentation = window
            }
        }.onSuccess {
            _status.value = _status.value.copy(
                state = SinkState.ATTACHED,
                displayName = display.name?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME,
                detail = "${display.mode?.physicalWidth ?: 0}×${display.mode?.physicalHeight ?: 0}",
                clientCount = 1,
            )
            Logger.d(TAG, "attached to display ${display.displayId} (${display.name})")
        }.onFailure { e ->
            // Most likely a BadTokenException from a stale Activity. Report it
            // rather than crashing a live service.
            _status.value = _status.value.copy(state = SinkState.ERROR, detail = e.message)
            Logger.e(TAG, "failed to show presentation: ${e.message}", e)
        }
    }

    private fun dismissPresentation() {
        presentation?.let { runCatching { it.dismiss() } }
        presentation = null
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private companion object {
        const val DEFAULT_NAME = "External display"
    }
}

/** The window shown on the secondary display. Hosts Compose, driven by [SlideBus]. */
private class SlidePresentation(
    activity: Activity,
    display: Display,
) : Presentation(activity, display) {

    private val owners = PresentationOwners()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A ComposeView needs the three ViewTree owners, which a Presentation's
        // decor view does not have. Install them before setting any content.
        window?.decorView?.let(owners::attachTo)
        owners.onShow()

        setContentView(
            ComposeView(context).apply {
                setContent {
                    val envelope by SlideBus.current.collectAsState()
                    StandaloneOutputScreen(envelope.slide ?: Slide.BLANK)
                }
            }
        )
    }

    override fun onStop() {
        owners.onDismiss()
        super.onStop()
    }
}
