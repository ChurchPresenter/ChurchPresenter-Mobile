package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.media_title
import churchpresentermobile.composeapp.generated.resources.media_load_label
import churchpresentermobile.composeapp.generated.resources.media_url_placeholder
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.MediaViewModel
import com.church.presenter.churchpresentermobile.viewmodel.normalizeUrl
import org.jetbrains.compose.resources.stringResource

internal val ON_AMBER = Color(0xFF3A2A08)

/**
 * A position or duration as `m:ss`.
 *
 * `internal` so the clock the operator reads can be tested without a desktop
 * playing anything: a negative or absent position is reported by the desktop as
 * 0 or -1, and both have to read as the start rather than as "-1:-1".
 */
internal fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/**
 * Media tab — a *control surface* for media playing on the desktop (no video is
 * rendered on the phone). Shows the live now-playing state and transport controls,
 * and lets the user send a network URL / uploaded file live or to the schedule.
 */
@Composable
fun MediaScreen(
    viewModel: MediaViewModel,
    canUploadFiles: Boolean,
    maxUploadMb: Int,
    modifier: Modifier = Modifier,
    /** Put the player and the send/load controls side by side. */
    twoPane: Boolean = false,
    /** Opens the schedule drawer. Only used in [twoPane], which owns its header. */
    onMenu: (() -> Unit)? = null,
    /** Opens settings. Only used in [twoPane], which owns its header. */
    onSettings: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val url by viewModel.url.collectAsState()
    val message by viewModel.message.collectAsState()
    val playback by viewModel.playback.collectAsState()
    val uploaded by viewModel.uploaded.collectAsState()
    val uploading by viewModel.uploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val source by viewModel.source.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isLive = playback?.isLive == true
    val isPlaying = playback?.isPlaying == true
    val durationMs = playback?.durationMs ?: 0L
    val positionMs = playback?.positionMs ?: 0L


    // Local scrub state so the seek bar doesn't jump while the user is dragging it.
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    val progress = when {
        scrubbing -> scrubValue
        durationMs > 0L -> (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> 0f
    }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message!!, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    // Read by both halves — the send actions name the URL the player composed,
    // so it cannot live inside either one.
    val composedUrl = normalizeUrl(url)

    // ── The two halves, each named once ──────────────────────────────────
    // A phone scrolls through both in one column; a tablet puts the player on
    // the left and everything that loads or sends media on the right.
    val playerPane: @Composable ColumnScope.() -> Unit = {
        MediaPlayerPane(
            playback = playback,
            source = source,
            composedUrl = composedUrl,
            uploaded = uploaded,
            progress = progress,
            scrubbing = scrubbing,
            scrubValue = scrubValue,
            onScrub = { scrubbing = true; scrubValue = it },
            onScrubFinished = {
                viewModel.seekTo((scrubValue * durationMs).toLong())
                scrubbing = false
            },
            onStop = viewModel::stopPlayback,
            onBack10 = viewModel::seekBackward,
            onPlayPause = viewModel::playPause,
            onForward10 = viewModel::seekForward,
            onMute = viewModel::muteToggle,
            onVolume = viewModel::setVolume,
        )
    }
    val sendPane: @Composable ColumnScope.() -> Unit = {
        MediaSendPane(
            playback = playback,
            source = source,
            url = url,
            composedUrl = composedUrl,
            uploaded = uploaded,
            uploading = uploading,
            uploadProgress = uploadProgress,
            canUploadFiles = canUploadFiles,
            maxUploadMb = maxUploadMb,
            onAddToSchedule = viewModel::addToSchedule,
            onGoLive = viewModel::goLive,
            onClearScreen = viewModel::clearScreen,
            onSourceChange = viewModel::setSource,
            onUrlChange = viewModel::setUrl,
            onFilePicked = viewModel::uploadPicked,
            onPickError = viewModel::showMessage,
        )
    }

    if (twoPane) {
        MediaTwoPane(
            onMenu = onMenu,
            onSettings = onSettings,
            player = playerPane,
            send = sendPane,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
        )
        return
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            playerPane()
            Spacer(Modifier.height(18.dp))
            sendPane()
            Spacer(Modifier.height(24.dp))
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
internal fun UrlField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.inputBg)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Link, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(stringResource(Res.string.media_url_placeholder), color = colors.muted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The tablet's arrangement for the Media tab: the player on the left, and
 * everything that loads or sends media on the right.
 *
 * The split is by what the control *does*, not by hierarchy: the left half acts
 * on media that is already playing — scrub, pause, volume — and the right half
 * decides what plays next and where it goes. On a phone the second half sits
 * below the first, a scroll away from the transport an operator is watching.
 *
 * Only the right half scrolls. The player's own height is fixed by its 16:9
 * artwork, and a transport row that could scroll out of reach is exactly what the
 * phone layout already goes out of its way to prevent.
 */
@Composable
private fun MediaTwoPane(
    player: @Composable ColumnScope.() -> Unit,
    send: @Composable ColumnScope.() -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ScreenHeader(
                    title = stringResource(Res.string.media_title),
                    onMenu = onMenu,
                    onSettings = onSettings,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    player()
                }
            }

            VerticalDivider(color = colors.borderSubtle)

            Column(
                modifier = Modifier
                    .width(MediaSendPaneWidth)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                send()
                Spacer(Modifier.height(24.dp))
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
