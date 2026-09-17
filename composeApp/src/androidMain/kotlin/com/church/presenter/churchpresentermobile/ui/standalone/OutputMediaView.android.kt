package com.church.presenter.churchpresentermobile.ui.standalone

import android.graphics.Color
import android.media.MediaPlayer
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri

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
actual fun OutputVideoView(url: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                setBackgroundColor(Color.BLACK)
                // See the matching note in OutputWebView: this is output-only and
                // must never enter Android's focus-search cascade (CHURCH-PRESENTER-MOBILE-1Q).
                isFocusable = false
                isFocusableInTouchMode = false
                setOnPreparedListener { player: MediaPlayer ->
                    player.isLooping = true
                    // The desk owns the sound; an unmuted projector is a surprise
                    // nobody in the room asked for.
                    player.setVolume(0f, 0f)
                    start()
                }
            }
        },
        update = { view ->
            if (url.isNotBlank()) {
                view.setVideoURI(url.toUri())
            }
        },
        onRelease = { view -> view.stopPlayback() },
    )
}
