package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun OutputVideoView(url: String, modifier: Modifier) {
    val player = remember { AVPlayer() }
    val container = remember { UIView(frame = CGRectZero.readValue()) }
    val layer = remember {
        AVPlayerLayer().also {
            it.player = player
            it.videoGravity = AVLayerVideoGravityResizeAspect
            container.backgroundColor = UIColor.blackColor
            container.layer.addSublayer(it)
        }
    }
    UIKitView(
        factory = { container },
        modifier = modifier,
        update = { host ->
            layer.setFrame(host.bounds)
            val target = NSURL.URLWithString(url)
            if (url.isNotBlank() && target != null) {
                // The desk owns the sound; an unmuted projector surprises the room.
                player.setMuted(true)
                player.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = target))
                player.play()
            }
        },
        onRelease = { player.pause() },
    )
}
