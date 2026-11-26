package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.church.presenter.churchpresentermobile.model.VideoCue
import com.church.presenter.churchpresentermobile.model.VIDEO_DRIFT_CHECK_MS
import com.church.presenter.churchpresentermobile.model.VIDEO_DRIFT_TOLERANCE_MS
import com.church.presenter.churchpresentermobile.model.VIDEO_SEEK_SETTLE_MS
import com.church.presenter.churchpresentermobile.model.VIDEO_SYNC_TOLERANCE_MS
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.expectedPositionMs
import com.church.presenter.churchpresentermobile.model.shouldSeek
import platform.AVFoundation.AVPlayerActionAtItemEndPause
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.currentItem
import platform.AVFoundation.status
import platform.AVFoundation.actionAtItemEnd
import platform.AVFoundation.currentTime
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.AVFoundation.rate
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.CoreGraphics.CGRectZero
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.play
import platform.AVFoundation.pause
import platform.AVFoundation.setMuted
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun OutputWebView(url: String, modifier: Modifier) {
    val view = remember {
        WKWebView(frame = CGRectZero.readValue(), configuration = WKWebViewConfiguration()).apply {
            opaque = false
            backgroundColor = UIColor.blackColor
        }
    }
    UIKitView(
        factory = { view },
        modifier = modifier,
        update = { webView ->
            val target = NSURL.URLWithString(url)
            if (url.isNotBlank() && target != null && webView.URL?.absoluteString != url) {
                webView.loadRequest(NSURLRequest.requestWithURL(target))
            }
        },
        onRelease = { webView -> webView.stopLoading() },
    )
}

/** How often the player is looked at — enough to catch the clip opening promptly. */
private const val READY_POLL_MS = 250L

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun OutputVideoView(
    url: String,
    cue: VideoCue?,
    modifier: Modifier,
    muted: Boolean,
    soundOnDevice: Boolean,
) {
    // soundOnDevice is not honoured: iOS gives an app one audio route, and with a
    // screen on the HDMI adapter or AirPlay that route is the TV. The Media tab
    // says so rather than pretend.
    val control = remember { CuePlayer() }
    var endedBlack by remember { mutableStateOf(false) }
    control.onEndedToBlack = { endedBlack = true }
    LaunchedEffect(cue?.seq) { if (cue?.playing == true) endedBlack = false }
    LaunchedEffect(control) {
        while (true) {
            delay(READY_POLL_MS)
            control.tick()
        }
    }
    DisposableEffect(control) { onDispose { control.release() } }

    Box(modifier.background(Color.Black)) {
        UIKitView(
            factory = { control.container },
            modifier = Modifier.fillMaxSize(),
            update = { host ->
                control.layer.setFrame(host.bounds)
                control.update(url, cue, muted)
            },
            onRelease = { control.player.pause() },
        )
        if (endedBlack || cue?.endedToBlack == true) {
            Box(Modifier.fillMaxSize().background(Color.Black))
        }
    }
}

/**
 * Applies [VideoCue]s to an [AVPlayer].
 *
 * Swaps the item only when the address changes — replacing it on every cue
 * would restart the clip each time the volume moved — and watches the item's
 * end itself, because AVPlayer has no looping flag.
 */
@OptIn(ExperimentalForeignApi::class)
private class CuePlayer {
    val player = AVPlayer()
    val container = UIView(frame = CGRectZero.readValue()).apply { backgroundColor = UIColor.blackColor }
    val layer = AVPlayerLayer().also {
        it.player = player
        it.videoGravity = AVLayerVideoGravityResizeAspect
        container.layer.addSublayer(it)
    }
    var onEndedToBlack: () -> Unit = {}
    private var url: String? = null
    private var cue: VideoCue? = null
    private var cueReceivedAt = 0.0
    private var lastSeekAt = 0.0
    private var lastDriftCheck = 0.0
    private var syncedSinceLoad = false
    private var endObserver: Any? = null

    init {
        player.actionAtItemEnd = AVPlayerActionAtItemEndPause
    }

    fun update(newUrl: String, newCue: VideoCue?, muted: Boolean) {
        if (newCue != cue) cueReceivedAt = NSProcessInfo.processInfo.systemUptime
        cue = newCue
        if (newUrl.isNotBlank() && newUrl != url) {
            // Covers both the server's http address and a file:// one for a screen
            // plugged into this phone while the network server is down.
            val target = NSURL.URLWithString(newUrl) ?: return
            url = newUrl
            val item = AVPlayerItem(uRL = target)
            player.replaceCurrentItemWithPlayerItem(item)
            watchEnd(item)
            // A seek before the clip has opened is silently dropped, so the first
            // one waits for it — see tick().
            syncedSinceLoad = false
        }
        val current = newCue
        if (current == null) {
            player.setMuted(true)
            player.play()
            return
        }
        player.setMuted(muted)
        player.volume = current.volume.coerceIn(0f, 1f)
        seekIfDrifted(current)
        if (current.playing) player.play() else player.pause()
    }

    /** Seeks back into step if playing has wandered from the cue. */
    /**
     * Called a few times a second. Until the clip has opened, does nothing; the
     * moment it has, seeks it to where the cue says — un-blanking or going live
     * mid-clip used to start the TV from 0:00, because the seek sent while the
     * clip was still opening was dropped. After that, checks drift every
     * [VIDEO_DRIFT_CHECK_MS].
     */
    fun tick() {
        val current = cue ?: return
        if (player.currentItem?.status != AVPlayerItemStatusReadyToPlay) return
        if (!syncedSinceLoad) {
            syncedSinceLoad = true
            seekIfDrifted(current)
            if (current.playing) player.play() else player.pause()
            return
        }
        val now = NSProcessInfo.processInfo.systemUptime
        if ((now - lastDriftCheck) * MS_PER_SECOND < VIDEO_DRIFT_CHECK_MS) return
        lastDriftCheck = now
        correctDrift()
    }

    private fun correctDrift() {
        val current = cue ?: return
        if (!current.playing || player.rate == 0f) return
        val sinceSeekMs = (NSProcessInfo.processInfo.systemUptime - lastSeekAt) * MS_PER_SECOND
        if (sinceSeekMs < VIDEO_SEEK_SETTLE_MS) return
        seekIfDrifted(current, VIDEO_DRIFT_TOLERANCE_MS)
    }

    /**
     * AVPlayer opens a clip asynchronously, so the time since the cue arrived is
     * added on: the clip starts where the phone is, not where it was.
     */
    private fun seekIfDrifted(current: VideoCue, toleranceMs: Long = VIDEO_SYNC_TOLERANCE_MS) {
        val elapsedMs = ((NSProcessInfo.processInfo.systemUptime - cueReceivedAt) * MS_PER_SECOND).toLong()
        val expected = expectedPositionMs(current, elapsedMs)
        val nowMs = (CMTimeGetSeconds(player.currentTime()) * MS_PER_SECOND).toLong()
        if (shouldSeek(nowMs, expected, current, toleranceMs)) {
            player.seekToTime(CMTimeMakeWithSeconds(expected / MS_PER_SECOND, SEEK_TIMESCALE))
            lastSeekAt = NSProcessInfo.processInfo.systemUptime
        }
    }

    private fun watchEnd(item: AVPlayerItem) {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = item,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            when (cue?.endAction ?: VideoEndAction.LOOP) {
                VideoEndAction.LOOP -> {
                    player.seekToTime(CMTimeMakeWithSeconds(0.0, SEEK_TIMESCALE))
                    player.play()
                }
                VideoEndAction.BLACK -> onEndedToBlack()
                VideoEndAction.HOLD -> Unit
            }
        }
    }

    fun release() {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }

    private companion object {
        const val MS_PER_SECOND = 1000.0
        const val SEEK_TIMESCALE = 600

    }
}
