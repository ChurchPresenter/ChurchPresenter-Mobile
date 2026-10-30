package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.church.presenter.churchpresentermobile.tv.PhoneSource
import com.church.presenter.churchpresentermobile.tv.phoneLiveStates
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneOutputScreen
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** How long a dropped phone is given before the TV says it is reconnecting. */
private const val QUIET_RECONNECT_MS = 3_000L

/**
 * A phone's live slide, full screen — drawn by [StandaloneOutputScreen], the
 * same renderer as the phone's preview, Cast and HDMI, so the TV matches them.
 */
@Composable
internal fun TvLiveScreen(source: PhoneSource) {
    KeepScreenOn()
    val client = remember { createWebSocketClient() }
    DisposableEffect(client) { onDispose { client.close() } }
    val states = remember(source, client) { phoneLiveStates(source, client) }
    val state by states.collectAsState(initial = LiveState.Connecting)
    TvLiveContent(state, source.name)
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
            is LiveState.Lost -> {
                // The last slide stays up: a few seconds stale beats a black room.
                state.lastSlide?.let { StandaloneOutputScreen(it) }
                    ?: TvMessage(stringResource(Res.string.tv_connecting, sourceName))
                ReconnectingBadge(sourceName, Modifier.align(Alignment.BottomEnd))
            }
            LiveState.Ended -> TvMessage(stringResource(Res.string.tv_ended))
            LiveState.TooNew -> TvMessage(stringResource(Res.string.tv_too_new))
        }
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
