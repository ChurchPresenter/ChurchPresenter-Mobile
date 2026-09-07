package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.media_add_to_schedule
import churchpresentermobile.composeapp.generated.resources.media_cd_back10
import churchpresentermobile.composeapp.generated.resources.media_cd_forward10
import churchpresentermobile.composeapp.generated.resources.media_cd_mute
import churchpresentermobile.composeapp.generated.resources.media_cd_pause
import churchpresentermobile.composeapp.generated.resources.media_cd_play
import churchpresentermobile.composeapp.generated.resources.media_cd_stop
import churchpresentermobile.composeapp.generated.resources.media_choose_file
import churchpresentermobile.composeapp.generated.resources.media_clear_screen
import churchpresentermobile.composeapp.generated.resources.media_go_live
import churchpresentermobile.composeapp.generated.resources.media_load_label
import churchpresentermobile.composeapp.generated.resources.media_local_ready
import churchpresentermobile.composeapp.generated.resources.media_no_media_loaded
import churchpresentermobile.composeapp.generated.resources.media_on_screen
import churchpresentermobile.composeapp.generated.resources.media_pick_to_upload
import churchpresentermobile.composeapp.generated.resources.media_playing
import churchpresentermobile.composeapp.generated.resources.media_playing_generic
import churchpresentermobile.composeapp.generated.resources.media_playing_on_desktop
import churchpresentermobile.composeapp.generated.resources.media_source_label
import churchpresentermobile.composeapp.generated.resources.media_source_network_url
import churchpresentermobile.composeapp.generated.resources.media_source_upload
import churchpresentermobile.composeapp.generated.resources.media_subtitle_empty
import churchpresentermobile.composeapp.generated.resources.media_subtitle_url
import churchpresentermobile.composeapp.generated.resources.media_target_loaded
import churchpresentermobile.composeapp.generated.resources.media_target_none
import churchpresentermobile.composeapp.generated.resources.media_target_uploaded
import churchpresentermobile.composeapp.generated.resources.media_target_url
import churchpresentermobile.composeapp.generated.resources.media_uploading_percent
import churchpresentermobile.composeapp.generated.resources.media_uploads_disabled
import churchpresentermobile.composeapp.generated.resources.media_uploads_disabled_hint
import churchpresentermobile.composeapp.generated.resources.media_url_placeholder
import churchpresentermobile.composeapp.generated.resources.media_will_send
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.MediaSource
import com.church.presenter.churchpresentermobile.viewmodel.MediaViewModel
import com.church.presenter.churchpresentermobile.viewmodel.mediaKindFrom
import com.church.presenter.churchpresentermobile.viewmodel.mediaTitleFrom
import com.church.presenter.churchpresentermobile.viewmodel.normalizeUrl
import org.jetbrains.compose.resources.stringResource

private val ON_AMBER = Color(0xFF3A2A08)

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

    val loaded = playback?.isLoaded == true
    val isLive = playback?.isLive == true
    val isPlaying = playback?.isPlaying == true
    val durationMs = playback?.durationMs ?: 0L
    val positionMs = playback?.positionMs ?: 0L

    // Strings referenced inside non-composable lambdas must be resolved up front.
    val playingLabel = stringResource(Res.string.media_playing)
    val genericMediaLabel = stringResource(Res.string.media_playing_generic)

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

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            // ── Now-playing artwork (metadata only — never the video) ─────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = colors.accent.copy(alpha = if (loaded) 0.75f else 0.4f),
                    modifier = Modifier.size(60.dp),
                )
                if (isLive) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.danger.copy(alpha = 0.9f))
                            .padding(horizontal = 11.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                        Text(
                            stringResource(Res.string.media_on_screen),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.04.em,
                            modifier = Modifier.testTag(UiTags.MEDIA_ON_SCREEN),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            // ── Title + type ──────────────────────────────────────────────
            val composedUrl = normalizeUrl(url)
            val title = when {
                loaded -> playback?.title?.ifBlank { playingLabel } ?: playingLabel
                source == MediaSource.UPLOAD && uploaded != null -> uploaded!!.title
                source == MediaSource.URL && composedUrl.isNotBlank() -> mediaTitleFrom(composedUrl)
                else -> stringResource(Res.string.media_no_media_loaded)
            }
            val subtitle = when {
                loaded -> stringResource(Res.string.media_playing_on_desktop)
                source == MediaSource.UPLOAD && uploaded != null -> stringResource(Res.string.media_local_ready)
                source == MediaSource.UPLOAD -> stringResource(Res.string.media_pick_to_upload)
                source == MediaSource.URL && composedUrl.isNotBlank() -> stringResource(Res.string.media_subtitle_url, mediaKindFrom(composedUrl))
                else -> stringResource(Res.string.media_subtitle_empty)
            }
            Text(
                title,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(UiTags.MEDIA_TITLE),
            )
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = colors.muted, fontSize = 12.sp, modifier = Modifier.testTag(UiTags.MEDIA_SUBTITLE))

            Spacer(Modifier.height(12.dp))
            // ── Seek bar ──────────────────────────────────────────────────
            Slider(
                value = progress,
                modifier = Modifier.testTag(UiTags.MEDIA_SEEK),
                enabled = loaded && durationMs > 0L,
                onValueChange = { scrubbing = true; scrubValue = it },
                onValueChangeFinished = {
                    viewModel.seekTo((scrubValue * durationMs).toLong())
                    scrubbing = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.inputBg,
                    disabledThumbColor = colors.dim,
                    disabledActiveTrackColor = colors.dim,
                    disabledInactiveTrackColor = colors.inputBg,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val shownPos = if (scrubbing) (scrubValue * durationMs).toLong() else positionMs
                Text(
                    formatTime(shownPos),
                    color = colors.secondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag(UiTags.MEDIA_POSITION),
                )
                Text(
                    formatTime(durationMs),
                    color = colors.muted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag(UiTags.MEDIA_DURATION),
                )
            }

            Spacer(Modifier.height(10.dp))
            // ── Transport controls ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleControl(
                    Icons.Filled.Stop,
                    stringResource(Res.string.media_cd_stop),
                    46.dp,
                    enabled = loaded,
                    modifier = Modifier.testTag(UiTags.MEDIA_STOP),
                ) { viewModel.stopPlayback() }
                CircleControl(
                    Icons.Filled.Replay10,
                    stringResource(Res.string.media_cd_back10),
                    52.dp,
                    enabled = loaded,
                    modifier = Modifier.testTag(UiTags.MEDIA_BACK_10),
                ) { viewModel.seekBackward() }
                // Big play/pause
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(if (loaded) colors.accent else colors.surfaceElevated)
                        .testTag(UiTags.MEDIA_PLAY_PAUSE)
                        .clickable(enabled = loaded) { viewModel.playPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(Res.string.media_cd_pause) else stringResource(Res.string.media_cd_play),
                        tint = if (loaded) colors.onAccent else colors.dim,
                        modifier = Modifier.size(30.dp),
                    )
                }
                CircleControl(
                    Icons.Filled.Forward10,
                    stringResource(Res.string.media_cd_forward10),
                    52.dp,
                    enabled = loaded,
                    modifier = Modifier.testTag(UiTags.MEDIA_FORWARD_10),
                ) { viewModel.seekForward() }
                CircleControl(
                    if (playback?.muted == true) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    stringResource(Res.string.media_cd_mute), 46.dp, enabled = loaded,
                    modifier = Modifier.testTag(UiTags.MEDIA_MUTE),
                ) { viewModel.muteToggle() }
            }

            Spacer(Modifier.height(14.dp))
            // ── Volume ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
                Slider(
                    value = playback?.volume ?: 1f,
                    enabled = loaded,
                    onValueChange = { viewModel.setVolume(it) },
                    modifier = Modifier.weight(1f).testTag(UiTags.MEDIA_VOLUME),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.inputBg,
                        disabledThumbColor = colors.dim,
                        disabledActiveTrackColor = colors.dim,
                        disabledInactiveTrackColor = colors.inputBg,
                    ),
                )
            }

            Spacer(Modifier.height(18.dp))
            // ── Send media (Add to Schedule + Go Live) ────────────────────
            // Resolve what the actions will actually send, mirroring the ViewModel.
            val sendTarget: String? = when {
                source == MediaSource.UPLOAD && uploaded != null ->
                    stringResource(Res.string.media_target_uploaded, uploaded!!.title)
                source == MediaSource.URL && composedUrl.isNotBlank() ->
                    stringResource(Res.string.media_target_url)
                playback?.isLoaded == true && playback?.source?.isNotBlank() == true ->
                    stringResource(Res.string.media_target_loaded, playback?.title?.ifBlank { genericMediaLabel } ?: genericMediaLabel)
                else -> null
            }
            val willSendText = if (sendTarget != null)
                stringResource(Res.string.media_will_send, sendTarget)
            else stringResource(Res.string.media_target_none)
            Text(
                willSendText,
                color = if (sendTarget != null) colors.secondary else colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.testTag(UiTags.MEDIA_WILL_SEND),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.amber)
                        .testTag(UiTags.MEDIA_ADD_TO_SCHEDULE)
                        .clickable { viewModel.addToSchedule() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = ON_AMBER, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(7.dp))
                    Text(stringResource(Res.string.media_add_to_schedule), color = ON_AMBER, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accent)
                        .testTag(UiTags.MEDIA_GO_LIVE)
                        .clickable { viewModel.goLive() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(7.dp))
                    Text(stringResource(Res.string.media_go_live), color = colors.onAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .testTag(UiTags.MEDIA_CLEAR)
                    .clickable { viewModel.clearScreen() },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(Res.string.media_clear_screen), color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(24.dp))
            // ── Load media (explicit source: Network URL or Upload) ───────
            Text(stringResource(Res.string.media_source_label), color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em)
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = listOf(stringResource(Res.string.media_source_network_url), stringResource(Res.string.media_source_upload)),
                selectedIndex = if (source == MediaSource.URL) 0 else 1,
                onSelect = { viewModel.setSource(if (it == 0) MediaSource.URL else MediaSource.UPLOAD) },
                optionTag = { UiTags.mediaSource(it) },
            )
            Spacer(Modifier.height(12.dp))
            if (source == MediaSource.URL) {
                UrlField(
                    value = url,
                    onValueChange = viewModel::setUrl,
                    modifier = Modifier.testTag(UiTags.MEDIA_URL),
                )
            } else if (!canUploadFiles) {
                // Desktop has file uploads turned off — show a disabled state, no picker.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.inputBg)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, tint = colors.dim, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.media_uploads_disabled), color = colors.muted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.media_uploads_disabled_hint), color = colors.muted, fontSize = 12.sp)
            } else {
                MediaFilePicker(
                    onFilePicked = { file -> if (file != null) viewModel.uploadPicked(file) },
                    onError = { viewModel.showMessage(it) },
                    maxBytes = maxUploadMb.toLong() * 1024 * 1024,
                ) { launchPicker ->
                    val uploadingLabel = stringResource(Res.string.media_uploading_percent, (uploadProgress * 100).toInt())
                    val chooseLabel = stringResource(Res.string.media_choose_file)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .border(1.dp, if (uploaded != null) colors.accent else colors.border, RoundedCornerShape(12.dp))
                            .testTag(UiTags.MEDIA_UPLOAD)
                            .clickable(enabled = !uploading) { launchPicker() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            when {
                                uploading -> uploadingLabel
                                uploaded != null -> uploaded!!.title
                                else -> chooseLabel
                            },
                            color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (uploading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)),
                        color = colors.accent,
                        trackColor = colors.inputBg,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CircleControl(
    icon: ImageVector,
    label: String,
    diameter: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.border, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) colors.secondary else colors.dim, modifier = Modifier.size((diameter.value * 0.42f).dp))
    }
}

@Composable
private fun UrlField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
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
