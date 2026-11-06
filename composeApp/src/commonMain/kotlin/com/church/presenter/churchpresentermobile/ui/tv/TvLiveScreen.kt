package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.tv_connecting
import churchpresentermobile.composeapp.generated.resources.tv_ended
import churchpresentermobile.composeapp.generated.resources.tv_reconnecting
import churchpresentermobile.composeapp.generated.resources.tv_too_new
import com.church.presenter.churchpresentermobile.network.createWebSocketClient
import com.church.presenter.churchpresentermobile.tv.KeepScreenOn
import com.church.presenter.churchpresentermobile.tv.LiveState
import com.church.presenter.churchpresentermobile.tv.TvSource
import com.church.presenter.churchpresentermobile.tv.SourceKind
import com.church.presenter.churchpresentermobile.tv.desktopLiveStates
import com.church.presenter.churchpresentermobile.tv.phoneLiveStates
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneOutputScreen
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** How long a dropped phone is given before the TV says it is reconnecting. */
private const val QUIET_RECONNECT_MS = 3_000L

/** How long the info card stays up after OK is held. */
private const val INFO_SHOWN_MS = 8_000L

/**
 * What a source is showing, full screen. A phone's slide is drawn by
 * [StandaloneOutputScreen], the same renderer as its preview, Cast and HDMI; a
 * desktop's output arrives as finished pictures, so the TV matches its projector.
 */
@Composable
internal fun TvLiveScreen(source: TvSource) {
    KeepScreenOn()
    val client = remember { createWebSocketClient() }
    DisposableEffect(client) { onDispose { client.close() } }
    val states = remember(source, client) {
        when (source.kind) {
            SourceKind.PHONE -> phoneLiveStates(source, client)
            SourceKind.DESKTOP -> desktopLiveStates(source, client)
        }
    }
    val state by states.collectAsState(initial = LiveState.Connecting)
    var infoShown by remember { mutableStateOf(false) }
    val okKey = remember { HeldOkKey() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(infoShown) {
        if (infoShown) {
            delay(INFO_SHOWN_MS)
            infoShown = false
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .onPreviewKeyEvent { event ->
                if (okKey.update(event.key in OK_KEYS, event.type == KeyEventType.KeyDown)) infoShown = true
                false
            }
            .focusable(),
    ) {
        TvLiveContent(state, source.name)
        if (infoShown) TvInfoOverlay(source, state, Modifier.align(Alignment.TopEnd))
    }
}

/**
 * What the TV draws for [state]. Separate from the connection so it can be
 * tested and photographed without a phone.
 */
@Composable
internal fun TvLiveContent(state: LiveState, sourceName: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).testTag(TvTags.LIVE)) {
        when (state) {
            LiveState.Connecting -> TvMessage(stringResource(Res.string.tv_connecting, sourceName))
            is LiveState.Showing -> StandaloneOutputScreen(state.slide)
            is LiveState.Picture -> DesktopPicture(state.image, state.version)
            is LiveState.Lost -> {
                // The last slide stays up: a few seconds stale beats a black room.
                when {
                    state.lastPicture != null -> DesktopPicture(state.lastPicture, 0)
                    state.lastSlide != null -> StandaloneOutputScreen(state.lastSlide)
                    else -> TvMessage(stringResource(Res.string.tv_connecting, sourceName))
                }
                ReconnectingBadge(sourceName, Modifier.align(Alignment.BottomEnd))
            }
            LiveState.Ended -> TvMessage(stringResource(Res.string.tv_ended))
            LiveState.TooNew -> TvMessage(stringResource(Res.string.tv_too_new))
        }
    }
}

/**
 * A desktop's output. Keyed on [version] because the picture is painted over in
 * place: to Compose it is the same object, and nothing would redraw it.
 */
@Composable
private fun DesktopPicture(image: ImageBitmap, version: Int) {
    key(version) {
        Image(
            painter = BitmapPainter(image),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * A small, late notice in the corner. Late, because a phone that blinks off
 * Wi-Fi for a second should not put a message over the sermon slide.
 */
@Composable
private fun ReconnectingBadge(sourceName: String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(QUIET_RECONNECT_MS)
        visible = true
    }
    if (!visible) return
    Text(
        text = stringResource(Res.string.tv_reconnecting, sourceName),
        color = Color.White,
        fontSize = 16.sp,
        modifier = modifier
            .padding(24.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(TvTags.STATUS),
    )
}
