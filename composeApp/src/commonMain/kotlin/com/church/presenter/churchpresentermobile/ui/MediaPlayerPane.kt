package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.media_cd_back10
import churchpresentermobile.composeapp.generated.resources.media_cd_forward10
import churchpresentermobile.composeapp.generated.resources.media_cd_mute
import churchpresentermobile.composeapp.generated.resources.media_cd_pause
import churchpresentermobile.composeapp.generated.resources.media_cd_play
import churchpresentermobile.composeapp.generated.resources.media_cd_stop
import churchpresentermobile.composeapp.generated.resources.media_local_ready
import churchpresentermobile.composeapp.generated.resources.media_no_media_loaded
import churchpresentermobile.composeapp.generated.resources.media_on_screen
import churchpresentermobile.composeapp.generated.resources.media_pick_to_upload
import churchpresentermobile.composeapp.generated.resources.media_playing
import churchpresentermobile.composeapp.generated.resources.media_playing_on_desktop
import churchpresentermobile.composeapp.generated.resources.media_subtitle_empty
import churchpresentermobile.composeapp.generated.resources.media_subtitle_url
import com.church.presenter.churchpresentermobile.model.MediaPlaybackState
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.MediaSource
import com.church.presenter.churchpresentermobile.viewmodel.UploadedMedia
import com.church.presenter.churchpresentermobile.viewmodel.mediaKindFrom
import com.church.presenter.churchpresentermobile.viewmodel.mediaTitleFrom
import org.jetbrains.compose.resources.stringResource

/** The artwork box's shape. Metadata only — the video itself never comes here. */
private const val ARTWORK_ASPECT = 16f / 9f

/** How visible the play glyph is on the artwork, loaded and not. */
private const val GLYPH_ALPHA_LOADED = 0.75f
private const val GLYPH_ALPHA_IDLE = 0.4f

/** A transport glyph, as a fraction of the button it sits in. */
private const val GLYPH_TO_BUTTON = 0.42f

/**
 * Everything that acts on media the desktop has already loaded: the artwork,
 * what it is called, the seek bar, the transport row and the volume.
 *
 * Takes the [MediaPlaybackState] whole rather than a dozen unpacked flags. Every
 * value below comes off it, and passing it as one keeps "what the desktop says
 * is playing" a single thing that is either there or not.
 */
@Composable
internal fun ColumnScope.MediaPlayerPane(
    playback: MediaPlaybackState?,
    source: MediaSource,
    composedUrl: String,
    uploaded: UploadedMedia?,
    progress: Float,
    scrubbing: Boolean,
    scrubValue: Float,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onStop: () -> Unit,
    onBack10: () -> Unit,
    onPlayPause: () -> Unit,
    onForward10: () -> Unit,
    onMute: () -> Unit,
    onVolume: (Float) -> Unit,
) {
    val loaded = playback?.isLoaded == true

    MediaArtwork(loaded = loaded, isLive = playback?.isLive == true)

    Spacer(Modifier.height(14.dp))
    MediaNowPlaying(
        playback = playback,
        source = source,
        composedUrl = composedUrl,
        uploaded = uploaded,
    )

    Spacer(Modifier.height(12.dp))
    MediaSeekBar(
        progress = progress,
        positionMs = playback?.positionMs ?: 0L,
        durationMs = playback?.durationMs ?: 0L,
        enabled = loaded,
        scrubbing = scrubbing,
        scrubValue = scrubValue,
        onScrub = onScrub,
        onScrubFinished = onScrubFinished,
    )

    Spacer(Modifier.height(10.dp))
    MediaTransport(
        loaded = loaded,
        isPlaying = playback?.isPlaying == true,
        muted = playback?.muted == true,
        onStop = onStop,
        onBack10 = onBack10,
        onPlayPause = onPlayPause,
        onForward10 = onForward10,
        onMute = onMute,
    )

    Spacer(Modifier.height(14.dp))
    MediaVolume(volume = playback?.volume ?: 1f, enabled = loaded, onVolume = onVolume)
}

/** Now-playing artwork — metadata only, never the video. */
@Composable
private fun MediaArtwork(loaded: Boolean, isLive: Boolean) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ARTWORK_ASPECT)
            .clip(shape)
            .background(Color.Black)
            .border(1.dp, colors.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = colors.accent.copy(
                alpha = if (loaded) GLYPH_ALPHA_LOADED else GLYPH_ALPHA_IDLE,
            ),
            modifier = Modifier.size(60.dp),
        )
        if (isLive) OnScreenPill(Modifier.align(Alignment.TopStart))
    }
}

/** The red "on screen" badge — the one thing here that says the room can see it. */
@Composable
private fun OnScreenPill(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
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

/**
 * What is loaded, and where it came from.
 *
 * Four cases in each line, and they are not the same four: the title falls back
 * to the URL's own file name, while the subtitle is the one that says whether
 * this is playing on the desktop already or only picked here.
 */
@Composable
private fun MediaNowPlaying(
    playback: MediaPlaybackState?,
    source: MediaSource,
    composedUrl: String,
    uploaded: UploadedMedia?,
) {
    val colors = LocalAppColors.current
    val loaded = playback?.isLoaded == true
    val playingLabel = stringResource(Res.string.media_playing)
    val hasUpload = source == MediaSource.UPLOAD && uploaded != null
    val hasUrl = source == MediaSource.URL && composedUrl.isNotBlank()

    val title = when {
        loaded -> playback?.title?.ifBlank { playingLabel } ?: playingLabel
        hasUpload -> uploaded!!.title
        hasUrl -> mediaTitleFrom(composedUrl)
        else -> stringResource(Res.string.media_no_media_loaded)
    }
    val subtitle = when {
        loaded -> stringResource(Res.string.media_playing_on_desktop)
        hasUpload -> stringResource(Res.string.media_local_ready)
        source == MediaSource.UPLOAD -> stringResource(Res.string.media_pick_to_upload)
        hasUrl -> stringResource(Res.string.media_subtitle_url, mediaKindFrom(composedUrl))
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
    Text(
        subtitle,
        color = colors.muted,
        fontSize = 12.sp,
        modifier = Modifier.testTag(UiTags.MEDIA_SUBTITLE),
    )
}

/**
 * The scrub bar and the two times under it.
 *
 * While [scrubbing] the position shown is the thumb's, not the desktop's: the
 * desktop keeps reporting where it actually is, and letting that through would
 * drag the thumb back out from under the finger.
 */
@Composable
private fun MediaSeekBar(
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    scrubbing: Boolean,
    scrubValue: Float,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
) {
    val colors = LocalAppColors.current
    Slider(
        value = progress,
        modifier = Modifier.testTag(UiTags.MEDIA_SEEK),
        enabled = enabled && durationMs > 0L,
        onValueChange = onScrub,
        onValueChangeFinished = onScrubFinished,
        colors = mediaSliderColors(),
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
}

/** Stop, back ten, play/pause, forward ten, mute. */
@Composable
private fun MediaTransport(
    loaded: Boolean,
    isPlaying: Boolean,
    muted: Boolean,
    onStop: () -> Unit,
    onBack10: () -> Unit,
    onPlayPause: () -> Unit,
    onForward10: () -> Unit,
    onMute: () -> Unit,
) {
    val colors = LocalAppColors.current
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
            onClick = onStop,
        )
        CircleControl(
            Icons.Filled.Replay10,
            stringResource(Res.string.media_cd_back10),
            52.dp,
            enabled = loaded,
            modifier = Modifier.testTag(UiTags.MEDIA_BACK_10),
            onClick = onBack10,
        )
        // Big play/pause — the one control reached for without looking.
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(if (loaded) colors.accent else colors.surfaceElevated)
                .testTag(UiTags.MEDIA_PLAY_PAUSE)
                .clickable(enabled = loaded, onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) Res.string.media_cd_pause else Res.string.media_cd_play,
                ),
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
            onClick = onForward10,
        )
        CircleControl(
            if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            stringResource(Res.string.media_cd_mute),
            46.dp,
            enabled = loaded,
            modifier = Modifier.testTag(UiTags.MEDIA_MUTE),
            onClick = onMute,
        )
    }
}

@Composable
private fun MediaVolume(volume: Float, enabled: Boolean, onVolume: (Float) -> Unit) {
    val colors = LocalAppColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(18.dp),
        )
        Slider(
            value = volume,
            enabled = enabled,
            onValueChange = onVolume,
            modifier = Modifier.weight(1f).testTag(UiTags.MEDIA_VOLUME),
            colors = mediaSliderColors(),
        )
    }
}

/** One palette for both sliders, so seek and volume cannot drift apart. */
@Composable
private fun mediaSliderColors(): SliderColors {
    val colors = LocalAppColors.current
    return SliderDefaults.colors(
        thumbColor = colors.accent,
        activeTrackColor = colors.accent,
        inactiveTrackColor = colors.inputBg,
        disabledThumbColor = colors.dim,
        disabledActiveTrackColor = colors.dim,
        disabledInactiveTrackColor = colors.inputBg,
    )
}

/** A round icon button, dimmed and inert when there is nothing loaded to act on. */
@Composable
private fun CircleControl(
    icon: ImageVector,
    label: String,
    diameter: Dp,
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
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) colors.secondary else colors.dim,
            modifier = Modifier.size(diameter * GLYPH_TO_BUTTON),
        )
    }
}
