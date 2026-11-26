package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.media_cd_play
import churchpresentermobile.composeapp.generated.resources.local_media_cd_loop
import com.church.presenter.churchpresentermobile.model.VideoCue
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.VideoSound
import com.church.presenter.churchpresentermobile.present.LocalVideoFiles
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.VideoLibrary
import com.church.presenter.churchpresentermobile.present.VideoTransport
import com.church.presenter.churchpresentermobile.present.VideoTransportState
import com.church.presenter.churchpresentermobile.ui.CircleControl
import com.church.presenter.churchpresentermobile.ui.MediaArtwork
import com.church.presenter.churchpresentermobile.ui.MediaSeekBar
import com.church.presenter.churchpresentermobile.ui.MediaTransportRow
import com.church.presenter.churchpresentermobile.ui.MediaTwoPane
import com.church.presenter.churchpresentermobile.ui.MediaVolume
import com.church.presenter.churchpresentermobile.ui.ToastHost
import com.church.presenter.churchpresentermobile.ui.formatTime
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LocalMediaViewModel
import com.church.presenter.churchpresentermobile.viewmodel.LocalVideoStatus
import com.church.presenter.churchpresentermobile.viewmodel.VideoAction
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource

/** How strongly the paused preview is dimmed under its play button. */
private const val PAUSED_SCRIM_ALPHA = 0.25f
private const val PLAY_OVERLAY_ALPHA = 0.92f
private const val BADGE_ALPHA = 0.6f

/**
 * The standalone Media tab: videos stored on this phone, and a player that puts
 * one on the hall screen.
 *
 * Laid out as the remote Media tab is, so an operator who knows one knows the
 * other: the player on top — the same artwork, scrubber, transport and volume —
 * and what to play next below it (beside it on a tablet). Where remote sends a
 * URL or an upload to the desktop, this lists the videos copied onto the device.
 *
 * Owns its own ViewModel per the project rule. The library, transport and
 * engine are plain collaborators that outlive the tab, so a clip keeps playing
 * when the operator goes to the Bible.
 */
@Composable
fun LocalMediaScreen(
    library: VideoLibrary,
    transport: VideoTransport,
    engine: StandaloneEngine,
    modifier: Modifier = Modifier,
    twoPane: Boolean = false,
    onMenu: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    /** Supplied by tests only; the screen owns its own otherwise. */
    providedViewModel: LocalMediaViewModel? = null,
) {
    val viewModel: LocalMediaViewModel = providedViewModel
        ?: viewModel(key = "local-media") { LocalMediaViewModel(library, transport, engine) }
    val colors = LocalAppColors.current
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    val player: @Composable ColumnScope.() -> Unit = { LocalPlayer(viewModel, library) }
    val list: @Composable ColumnScope.() -> Unit = { LocalVideoList(viewModel, library) }

    if (twoPane) {
        MediaTwoPane(
            player = player,
            send = list,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
            onMenu = onMenu,
            onSettings = onSettings,
        )
        return
    }
    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            player()
            Spacer(Modifier.height(22.dp))
            list()
            Spacer(Modifier.height(24.dp))
        }
        ToastHost(snackbarHostState)
    }
}

/** Collects what the player half needs and hands it to [LocalPlayerPane] as plain values. */
@Composable
private fun ColumnScope.LocalPlayer(viewModel: LocalMediaViewModel, library: VideoLibrary) {
    val state by viewModel.transportState.collectAsState()
    val status by viewModel.status.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val previewCue by viewModel.previewCue.collectAsState()
    val onScreen by viewModel.isOnScreen.collectAsState()
    val blank by viewModel.isBlank.collectAsState()
    LocalPlayerPane(
        state = state,
        status = status,
        positionMs = positionMs,
        previewCue = previewCue,
        thumbnailPath = state.video?.let(library::thumbnailPath),
        onScreen = onScreen,
        blank = blank,
        onAction = viewModel::onAction,
        cueNow = viewModel::currentCue,
    )
}

/**
 * The player half, from plain values — `internal` so a test can frame every
 * state without a running transport.
 */
@Composable
internal fun ColumnScope.LocalPlayerPane(
    state: VideoTransportState,
    status: LocalVideoStatus,
    positionMs: Long,
    previewCue: VideoCue?,
    thumbnailPath: String?,
    onScreen: Boolean,
    blank: Boolean,
    onAction: (VideoAction) -> Unit,
    cueNow: () -> VideoCue? = { previewCue },
) {
    val loaded = state.isLoaded
    val duration = state.durationMs
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    val progress = when {
        scrubbing -> scrubValue
        duration > 0L -> (positionMs.toFloat() / duration).coerceIn(0f, 1f)
        else -> 0f
    }

    MediaArtwork(
        loaded = loaded,
        isLive = onScreen && !blank,
        modifier = Modifier.testTag(VideoTags.PREVIEW),
    ) {
        // Fresh at the moment the preview is built or the transport changes, never
        // the position from whenever a button was last pressed.
        val cue = remember(previewCue) { cueNow() }
        VideoPreview(state, positionMs, cue, thumbnailPath, onPlay = { onAction(VideoAction.TogglePlay) })
    }
    Spacer(Modifier.height(14.dp))
    VideoNowPlaying(title = state.video?.title, status = status)
    Spacer(Modifier.height(12.dp))
    MediaSeekBar(
        progress = progress,
        positionMs = positionMs,
        durationMs = duration,
        enabled = loaded,
        scrubbing = scrubbing,
        scrubValue = scrubValue,
        onScrub = { scrubbing = true; scrubValue = it },
        onScrubFinished = {
            onAction(VideoAction.SeekTo((scrubValue * duration).toLong()))
            scrubbing = false
        },
    )
    Spacer(Modifier.height(10.dp))
    MediaTransportRow(
        loaded = loaded,
        isPlaying = state.playing,
        onStop = { onAction(VideoAction.Stop) },
        onBack10 = { onAction(VideoAction.Back10) },
        onPlayPause = { onAction(VideoAction.TogglePlay) },
        onForward10 = { onAction(VideoAction.Forward10) },
    ) {
        CircleControl(
            Icons.Filled.Repeat,
            stringResource(Res.string.local_media_cd_loop),
            46.dp,
            enabled = loaded,
            active = state.endAction == VideoEndAction.LOOP,
            modifier = Modifier.testTag(VideoTags.LOOP),
            onClick = { onAction(VideoAction.ToggleLoop) },
        )
    }
    Spacer(Modifier.height(14.dp))
    MediaVolume(
        volume = state.volume,
        enabled = loaded,
        onVolume = { onAction(VideoAction.SetVolume(it)) },
        showPercent = true,
    )
    Spacer(Modifier.height(16.dp))
    VideoScreenActions(loaded = loaded, onScreen = onScreen, blank = blank, onAction = onAction)
    Spacer(Modifier.height(16.dp))
    VideoOptionsCard(
        endAction = state.endAction,
        sound = state.sound,
        enabled = loaded,
        onEndAction = { onAction(VideoAction.SetEndAction(it)) },
        onSound = { onAction(VideoAction.SetSound(it)) },
    )
}

/**
 * Inside the 16:9 box: the clip itself once it has started, its still before
 * then, a play button over a paused picture, and the clock in the corner.
 *
 * The preview is the real file, muted unless the sound is meant to come from
 * the phone — never a mock-up of what the room sees.
 */
@Composable
private fun BoxScope.VideoPreview(
    state: VideoTransportState,
    positionMs: Long,
    cue: VideoCue?,
    thumbnailPath: String?,
    onPlay: () -> Unit,
) {
    val url = state.url
    val started = state.playing || positionMs > 0L
    if (url != null && started) {
        OutputVideoView(
            url = LocalVideoFiles.playable(url),
            cue = cue,
            modifier = Modifier.fillMaxSize(),
            muted = state.sound != VideoSound.PHONE,
            // "This phone" means the phone, even with a TV on its HDMI port or a Cast
            // screen attached — both of which take every app's sound by default.
            soundOnDevice = true,
        )
    } else if (thumbnailPath != null) {
        AsyncImage(
            model = "file://$thumbnailPath",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
    if (!state.isLoaded) return
    if (!state.playing) PlayOverlay(onPlay)
    TimeBadge(
        text = "${formatTime(positionMs)} / ${formatTime(state.durationMs)}",
        modifier = Modifier.align(Alignment.BottomEnd),
    )
}

@Composable
private fun BoxScope.PlayOverlay(onPlay: () -> Unit) {
    val colors = LocalAppColors.current
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = PAUSED_SCRIM_ALPHA)))
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = PLAY_OVERLAY_ALPHA))
            .clickable(onClick = onPlay),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = stringResource(Res.string.media_cd_play),
            tint = colors.onAccent,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun TimeBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
            .padding(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = BADGE_ALPHA))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag(VideoTags.TIME),
    )
}
