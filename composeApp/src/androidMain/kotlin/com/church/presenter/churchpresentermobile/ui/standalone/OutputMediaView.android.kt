package com.church.presenter.churchpresentermobile.ui.standalone

import android.content.Context
import android.graphics.Color
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import com.church.presenter.churchpresentermobile.model.VideoCue
import com.church.presenter.churchpresentermobile.model.VIDEO_DRIFT_CHECK_MS
import com.church.presenter.churchpresentermobile.model.VIDEO_DRIFT_TOLERANCE_MS
import com.church.presenter.churchpresentermobile.model.VIDEO_SEEK_SETTLE_MS
import com.church.presenter.churchpresentermobile.model.VIDEO_SYNC_TOLERANCE_MS
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.expectedPositionMs
import com.church.presenter.churchpresentermobile.model.shouldSeek

@Composable
actual fun OutputWebView(url: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // Keep navigation inside the view: a link that opened the phone's
                // browser would put Chrome on the projector and take the slide off it.
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Nothing here should ever read the device's files.
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mediaPlaybackRequiresUserGesture = false
                setBackgroundColor(Color.BLACK)
                // This is a projector output surface, never an input control. Left
                // focusable, Android's focus-search cascade can enter it while it
                // sits inside the Pager/LazyColumn preview slot, and Compose's
                // beyond-bounds search transiently disposing it mid-cascade crashes
                // with a "pending composition has not been applied" runtime error
                // (CHURCH-PRESENTER-MOBILE-1Q).
                isFocusable = false
                isFocusableInTouchMode = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            }
        },
        update = { view -> if (url.isNotBlank() && view.url != url) view.loadUrl(url) },
        onRelease = { view ->
            // Left alone, a WebView keeps running scripts and playing media after
            // the slide has moved on.
            view.stopLoading()
            view.loadUrl("about:blank")
            view.destroy()
        },
    )
}

@Composable
actual fun OutputVideoView(
    url: String,
    cue: VideoCue?,
    modifier: Modifier,
    muted: Boolean,
    soundOnDevice: Boolean,
) {
    val control = remember { CuePlayer() }
    // Black over the view rather than hiding it: a VideoView that goes INVISIBLE
    // destroys its surface, and with it the player and the position.
    var endedBlack by remember { mutableStateOf(false) }
    control.onEndedToBlack = { endedBlack = true }
    LaunchedEffect(cue?.seq) { if (cue?.playing == true) endedBlack = false }
    // Between cues: a player that drifts — a slow decoder, a dropped frame run —
    // is pulled back without waiting for the operator to touch anything.
    LaunchedEffect(control) {
        while (true) {
            delay(VIDEO_DRIFT_CHECK_MS)
            control.correctDrift()
        }
    }

    Box(modifier.background(ComposeColor.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                // Centred in a frame: a VideoView keeps the clip's shape but lays itself
                // out from the top-left, which put an upright phone video against the
                // left edge of the hall screen instead of letterboxed in the middle.
                FrameLayout(context).apply {
                    isFocusable = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    val video = VideoView(context).apply {
                        // See the matching note in OutputWebView: this is output-only and
                        // must never enter Android's focus-search cascade (CHURCH-PRESENTER-MOBILE-1Q).
                        isFocusable = false
                        isFocusableInTouchMode = false
                        control.attach(this)
                    }
                    addView(
                        video,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER,
                        ),
                    )
                }
            },
            update = { frame -> control.update(frame.getChildAt(0) as VideoView, url, cue, muted, soundOnDevice) },
            onRelease = { frame ->
                control.detach()
                (frame.getChildAt(0) as? VideoView)?.stopPlayback()
            },
        )
        if (endedBlack || cue?.endedToBlack == true) {
            Box(Modifier.fillMaxSize().background(ComposeColor.Black))
        }
    }
}

/**
 * Applies [VideoCue]s to a [VideoView].
 *
 * The MediaPlayer only exists once the video is prepared, and is replaced every
 * time the source changes, so the last cue is kept here and re-applied in
 * `onPrepared` — a cue that arrives while the clip is still opening is not lost,
 * and the time spent opening it is added on, so the clip starts where the phone
 * is rather than where it was when the cue was sent.
 *
 * `internal`, with the clock and SDK level injected, so the decisions — volume,
 * looping, when to seek and to where, which speaker — can be tested against a
 * mocked VideoView without an emulator.
 */
internal class CuePlayer(
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
    private val sdk: Int = Build.VERSION.SDK_INT,
) {
    private var view: VideoView? = null
    private var player: MediaPlayer? = null
    private var url: String? = null
    private var cue: VideoCue? = null
    private var cueReceivedAt = 0L
    private var lastSeekAt = 0L
    private var muted: Boolean = true
    private var soundOnDevice: Boolean = false
    var onEndedToBlack: () -> Unit = {}

    fun attach(target: VideoView) {
        view = target
        target.setOnPreparedListener { prepared ->
            player = prepared
            routeSound(target, prepared)
            apply()
        }
        target.setOnCompletionListener {
            if (cue?.endAction == VideoEndAction.BLACK) onEndedToBlack()
        }
    }

    fun detach() {
        player = null
        view = null
    }

    fun update(target: VideoView, newUrl: String, newCue: VideoCue?, newMuted: Boolean, newSoundOnDevice: Boolean) {
        if (newCue != cue) cueReceivedAt = clock()
        cue = newCue
        muted = newMuted
        soundOnDevice = newSoundOnDevice
        if (newUrl.isNotBlank() && newUrl != url) {
            url = newUrl
            player = null
            target.setVideoURI(newUrl.toUri())
            return
        }
        player?.let { routeSound(target, it) }
        apply()
    }

    /** Seeks back into step if playing has wandered from the cue. */
    fun correctDrift() {
        val target = view ?: return
        val current = cue ?: return
        if (player == null || !current.playing || !target.isPlaying) return
        if (clock() - lastSeekAt < VIDEO_SEEK_SETTLE_MS) return
        seekIfDrifted(target, current, VIDEO_DRIFT_TOLERANCE_MS)
    }

    private fun apply() {
        val target = view ?: return
        val active = player ?: return
        val current = cue
        if (current == null) {
            // A video slide that did not come from the transport: the old behaviour.
            active.isLooping = true
            active.setVolume(0f, 0f)
            if (!target.isPlaying) target.start()
            return
        }
        active.isLooping = current.endAction == VideoEndAction.LOOP
        val volume = if (muted) 0f else current.volume.coerceIn(0f, 1f)
        active.setVolume(volume, volume)
        seekIfDrifted(target, current)
        when {
            current.playing && !target.isPlaying -> target.start()
            !current.playing && target.isPlaying -> target.pause()
        }
    }

    private fun seekIfDrifted(
        target: VideoView,
        current: VideoCue,
        toleranceMs: Long = VIDEO_SYNC_TOLERANCE_MS,
    ) {
        val expected = expectedPositionMs(current, clock() - cueReceivedAt)
        if (shouldSeek(target.currentPosition.toLong(), expected, current, toleranceMs)) {
            target.seekTo(expected.toInt())
            lastSeekAt = clock()
        }
    }

    /**
     * Sends the sound to the phone's own speaker or headphones when asked to.
     * Android otherwise follows the picture: with a TV on the HDMI port or a
     * Cast screen attached, every app's audio goes to the TV.
     */
    private fun routeSound(target: VideoView, active: MediaPlayer) {
        if (sdk < Build.VERSION_CODES.P) return
        val audio = target.context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val device = if (soundOnDevice) phoneOutput(audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) else null
        runCatching { active.setPreferredDevice(device) }
    }
}

/** Headphones if any are plugged in or paired, otherwise the phone's speaker. */
internal fun phoneOutput(devices: Array<AudioDeviceInfo>): AudioDeviceInfo? =
    devices.firstOrNull { it.type in HEADPHONE_TYPES }
        ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

private val HEADPHONE_TYPES = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
)
