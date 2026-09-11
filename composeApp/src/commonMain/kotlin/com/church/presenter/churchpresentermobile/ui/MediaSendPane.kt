package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.media_add_to_schedule
import churchpresentermobile.composeapp.generated.resources.media_choose_file
import churchpresentermobile.composeapp.generated.resources.media_clear_screen
import churchpresentermobile.composeapp.generated.resources.media_go_live
import churchpresentermobile.composeapp.generated.resources.media_playing_generic
import churchpresentermobile.composeapp.generated.resources.media_source_label
import churchpresentermobile.composeapp.generated.resources.media_source_network_url
import churchpresentermobile.composeapp.generated.resources.media_source_upload
import churchpresentermobile.composeapp.generated.resources.media_target_loaded
import churchpresentermobile.composeapp.generated.resources.media_target_none
import churchpresentermobile.composeapp.generated.resources.media_target_uploaded
import churchpresentermobile.composeapp.generated.resources.media_target_url
import churchpresentermobile.composeapp.generated.resources.media_uploading_percent
import churchpresentermobile.composeapp.generated.resources.media_uploads_disabled
import churchpresentermobile.composeapp.generated.resources.media_uploads_disabled_hint
import churchpresentermobile.composeapp.generated.resources.media_will_send
import com.church.presenter.churchpresentermobile.model.MediaPlaybackState
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.MediaSource
import com.church.presenter.churchpresentermobile.viewmodel.UploadedMedia
import org.jetbrains.compose.resources.stringResource

/** Bytes in a megabyte, for turning the server's limit into one the picker checks. */
private const val BYTES_PER_MB = 1024L * 1024L

/** Percent, for the upload label. */
private const val PERCENT = 100

/**
 * Everything that decides what plays next and where it goes: a line saying what
 * the buttons will actually send, the three send buttons, and the source the
 * media is loaded from.
 *
 * The "will send" line exists because the three buttons act on whichever of
 * three things is current — an upload, a typed URL, or whatever the desktop
 * already has loaded — and an operator pressing Go Live deserves to know which
 * before the room finds out.
 */
@Composable
internal fun ColumnScope.MediaSendPane(
    playback: MediaPlaybackState?,
    source: MediaSource,
    url: String,
    composedUrl: String,
    uploaded: UploadedMedia?,
    uploading: Boolean,
    uploadProgress: Float,
    canUploadFiles: Boolean,
    maxUploadMb: Int,
    onAddToSchedule: () -> Unit,
    onGoLive: () -> Unit,
    onClearScreen: () -> Unit,
    onSourceChange: (MediaSource) -> Unit,
    onUrlChange: (String) -> Unit,
    onFilePicked: (PickedMediaFile) -> Unit,
    onPickError: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val genericMediaLabel = stringResource(Res.string.media_playing_generic)

    // Mirrors the ViewModel's own choice of what to send. Kept in step by being
    // the same three conditions in the same order.
    val sendTarget: String? = when {
        source == MediaSource.UPLOAD && uploaded != null ->
            stringResource(Res.string.media_target_uploaded, uploaded.title)
        source == MediaSource.URL && composedUrl.isNotBlank() ->
            stringResource(Res.string.media_target_url)
        playback?.isLoaded == true && playback.source?.isNotBlank() == true ->
            stringResource(
                Res.string.media_target_loaded,
                playback.title?.ifBlank { genericMediaLabel } ?: genericMediaLabel,
            )
        else -> null
    }
    Text(
        text = if (sendTarget != null) {
            stringResource(Res.string.media_will_send, sendTarget)
        } else {
            stringResource(Res.string.media_target_none)
        },
        color = if (sendTarget != null) colors.secondary else colors.muted,
        fontSize = 12.sp,
        modifier = Modifier.testTag(UiTags.MEDIA_WILL_SEND),
    )

    Spacer(Modifier.height(8.dp))
    MediaSendActions(
        onAddToSchedule = onAddToSchedule,
        onGoLive = onGoLive,
        onClearScreen = onClearScreen,
    )

    Spacer(Modifier.height(24.dp))
    MediaSourcePicker(
        source = source,
        url = url,
        uploaded = uploaded,
        uploading = uploading,
        uploadProgress = uploadProgress,
        canUploadFiles = canUploadFiles,
        maxUploadMb = maxUploadMb,
        onSourceChange = onSourceChange,
        onUrlChange = onUrlChange,
        onFilePicked = onFilePicked,
        onPickError = onPickError,
    )
    Spacer(Modifier.height(24.dp))
}

/** Add to Schedule, Go Live, Clear screen. */
@Composable
private fun ColumnScope.MediaSendActions(
    onAddToSchedule: () -> Unit,
    onGoLive: () -> Unit,
    onClearScreen: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SendButton(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            label = stringResource(Res.string.media_add_to_schedule),
            background = colors.amber,
            content = ON_AMBER,
            iconSize = 18.dp,
            tag = UiTags.MEDIA_ADD_TO_SCHEDULE,
            onClick = onAddToSchedule,
        )
        SendButton(
            icon = Icons.Filled.PlayArrow,
            label = stringResource(Res.string.media_go_live),
            background = colors.accent,
            content = colors.onAccent,
            iconSize = 20.dp,
            tag = UiTags.MEDIA_GO_LIVE,
            onClick = onGoLive,
        )
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
            .clickable(onClick = onClearScreen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(Res.string.media_clear_screen),
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** One of the two filled send buttons; they differ only in colour and glyph. */
@Composable
private fun RowScope.SendButton(
    icon: ImageVector,
    label: String,
    background: Color,
    content: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    tag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .testTag(tag)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
        Spacer(Modifier.size(7.dp))
        Text(label, color = content, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Where the media comes from: a network URL, or a file off this device.
 *
 * The upload half is gated on the desktop's live permission rather than hidden,
 * so an operator who cannot upload is told why instead of finding the option
 * missing.
 */
@Composable
private fun ColumnScope.MediaSourcePicker(
    source: MediaSource,
    url: String,
    uploaded: UploadedMedia?,
    uploading: Boolean,
    uploadProgress: Float,
    canUploadFiles: Boolean,
    maxUploadMb: Int,
    onSourceChange: (MediaSource) -> Unit,
    onUrlChange: (String) -> Unit,
    onFilePicked: (PickedMediaFile) -> Unit,
    onPickError: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    Text(
        stringResource(Res.string.media_source_label),
        color = colors.muted,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.05.em,
    )
    Spacer(Modifier.height(8.dp))
    SegmentedControl(
        options = listOf(
            stringResource(Res.string.media_source_network_url),
            stringResource(Res.string.media_source_upload),
        ),
        selectedIndex = if (source == MediaSource.URL) 0 else 1,
        onSelect = { onSourceChange(if (it == 0) MediaSource.URL else MediaSource.UPLOAD) },
        optionTag = { UiTags.mediaSource(it) },
    )
    Spacer(Modifier.height(12.dp))
    when {
        source == MediaSource.URL ->
            UrlField(value = url, onValueChange = onUrlChange, modifier = Modifier.testTag(UiTags.MEDIA_URL))
        !canUploadFiles -> UploadsDisabled()
        else -> UploadPicker(
            uploaded = uploaded,
            uploading = uploading,
            uploadProgress = uploadProgress,
            maxUploadMb = maxUploadMb,
            onFilePicked = onFilePicked,
            onPickError = onPickError,
        )
    }
}

/** The desktop has file uploads turned off — say so rather than open a picker. */
@Composable
private fun ColumnScope.UploadsDisabled() {
    val colors = LocalAppColors.current
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
        Icon(
            Icons.Filled.UploadFile,
            contentDescription = null,
            tint = colors.dim,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(stringResource(Res.string.media_uploads_disabled), color = colors.muted, fontSize = 13.sp)
    }
    Spacer(Modifier.height(8.dp))
    Text(stringResource(Res.string.media_uploads_disabled_hint), color = colors.muted, fontSize = 12.sp)
}

/** Choose a file, then watch it go up. */
@Composable
private fun ColumnScope.UploadPicker(
    uploaded: UploadedMedia?,
    uploading: Boolean,
    uploadProgress: Float,
    maxUploadMb: Int,
    onFilePicked: (PickedMediaFile) -> Unit,
    onPickError: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    MediaFilePicker(
        onFilePicked = { file -> if (file != null) onFilePicked(file) },
        onError = onPickError,
        maxBytes = maxUploadMb.toLong() * BYTES_PER_MB,
    ) { launchPicker ->
        val uploadingLabel =
            stringResource(Res.string.media_uploading_percent, (uploadProgress * PERCENT).toInt())
        val chooseLabel = stringResource(Res.string.media_choose_file)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(
                    width = 1.dp,
                    color = if (uploaded != null) colors.accent else colors.border,
                    shape = RoundedCornerShape(12.dp),
                )
                .testTag(UiTags.MEDIA_UPLOAD)
                .clickable(enabled = !uploading) { launchPicker() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.UploadFile,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = when {
                    uploading -> uploadingLabel
                    uploaded != null -> uploaded.title
                    else -> chooseLabel
                },
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
