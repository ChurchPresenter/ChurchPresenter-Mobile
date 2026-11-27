package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_cancel
import churchpresentermobile.composeapp.generated.resources.library_delete
import churchpresentermobile.composeapp.generated.resources.local_media_add_video
import churchpresentermobile.composeapp.generated.resources.local_media_cd_delete
import churchpresentermobile.composeapp.generated.resources.local_media_delete_body
import churchpresentermobile.composeapp.generated.resources.local_media_delete_title
import churchpresentermobile.composeapp.generated.resources.local_media_empty_body
import churchpresentermobile.composeapp.generated.resources.local_media_empty_title
import churchpresentermobile.composeapp.generated.resources.local_media_from_files
import churchpresentermobile.composeapp.generated.resources.local_media_from_photos
import churchpresentermobile.composeapp.generated.resources.local_media_importing
import churchpresentermobile.composeapp.generated.resources.local_media_loaded
import churchpresentermobile.composeapp.generated.resources.local_media_loops
import churchpresentermobile.composeapp.generated.resources.local_media_unsupported
import churchpresentermobile.composeapp.generated.resources.local_media_videos
import churchpresentermobile.composeapp.generated.resources.local_media_videos_summary
import coil3.compose.AsyncImage
import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import com.church.presenter.churchpresentermobile.present.MAX_VIDEO_IMPORT_BYTES
import com.church.presenter.churchpresentermobile.present.VIDEO_IMPORT_EXTENSIONS
import com.church.presenter.churchpresentermobile.present.VideoLibrary
import com.church.presenter.churchpresentermobile.ui.MediaFilePicker
import com.church.presenter.churchpresentermobile.ui.PhotosVideoPicker
import com.church.presenter.churchpresentermobile.ui.formatTime
import com.church.presenter.churchpresentermobile.ui.photosLibraryIsSeparate
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LocalMediaViewModel
import org.jetbrains.compose.resources.stringResource

private const val PERCENT = 100
private const val SELECTED_FILL_ALPHA = 0.06f
private const val SELECTED_BORDER_ALPHA = 0.45f
private const val BADGE_ALPHA = 0.65f

/** Collects the list half's state and hands it to [LocalVideoListPane]. */
@Composable
internal fun ColumnScope.LocalVideoList(viewModel: LocalMediaViewModel, library: VideoLibrary) {
    val videos by viewModel.videos.collectAsState()
    val state by viewModel.transportState.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    LocalVideoListPane(
        videos = videos,
        loadedId = state.video?.id,
        loopingId = state.video?.id?.takeIf { state.endAction == VideoEndAction.LOOP },
        supported = viewModel.isSupported,
        importProgress = importProgress,
        thumbnailPath = library::thumbnailPath,
        onSelect = viewModel::select,
        onDelete = viewModel::delete,
        onPicked = viewModel::import,
        onPickError = viewModel::showMessage,
    )
}

/**
 * The videos on this phone: a header with how many and how much space they
 * take, the Add button, and one row per clip — a 16:9 still with its length,
 * its name, and format · resolution · size, so the right clip is easy to find
 * under pressure. Tapping a row loads it into the player; Live is still a
 * separate, deliberate tap.
 */
@Composable
internal fun ColumnScope.LocalVideoListPane(
    videos: List<LocalVideo>,
    loadedId: String?,
    loopingId: String?,
    supported: Boolean,
    importProgress: Float?,
    thumbnailPath: (LocalVideo) -> String?,
    onSelect: (LocalVideo) -> Unit,
    onDelete: (LocalVideo) -> Unit,
    onPicked: (PickedMediaFile) -> Unit,
    onPickError: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    var pendingDelete by remember { mutableStateOf<LocalVideo?>(null) }
    pendingDelete?.let { video ->
        DeleteVideoDialog(
            video,
            onConfirm = {
                onDelete(video)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.local_media_videos).uppercase(),
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.08.em,
            modifier = Modifier.weight(1f),
        )
        if (videos.isNotEmpty()) {
            Text(
                stringResource(
                    Res.string.local_media_videos_summary,
                    videos.size,
                    formatBytes(videos.sumOf { it.sizeBytes }),
                ),
                color = colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    if (!supported) {
        Text(stringResource(Res.string.local_media_unsupported), color = colors.muted, fontSize = 13.sp)
        return
    }
    AddVideoButton(importProgress, onPicked, onPickError)
    Spacer(Modifier.height(12.dp))
    if (videos.isEmpty()) {
        EmptyVideos()
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        videos.forEach { video ->
            VideoRow(
                video = video,
                thumbnailPath = thumbnailPath(video),
                loaded = video.id == loadedId,
                looping = video.id == loopingId,
                onClick = { onSelect(video) },
                onDelete = { pendingDelete = video },
            )
        }
    }
}

/**
 * Add video. Where the platform keeps the camera roll out of the file picker's
 * reach (iOS), a tap asks which: the photo library or Files. Elsewhere it opens
 * the file picker straight away, which already lists the gallery.
 */
@Composable
private fun AddVideoButton(
    importProgress: Float?,
    onPicked: (PickedMediaFile) -> Unit,
    onPickError: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val onFile: (PickedMediaFile?) -> Unit = { file -> if (file != null) onPicked(file) }
    MediaFilePicker(
        onFilePicked = onFile,
        onError = onPickError,
        maxBytes = MAX_VIDEO_IMPORT_BYTES,
        extensions = VIDEO_IMPORT_EXTENSIONS,
    ) { openFiles ->
        PhotosVideoPicker(
            onFilePicked = onFile,
            onError = onPickError,
            maxBytes = MAX_VIDEO_IMPORT_BYTES,
        ) { openPhotos ->
            var choosing by remember { mutableStateOf(false) }
            Box {
                AddVideoRow(importProgress) {
                    if (photosLibraryIsSeparate) choosing = true else openFiles()
                }
                DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.local_media_from_photos)) },
                        leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                        onClick = {
                            choosing = false
                            openPhotos()
                        },
                        modifier = Modifier.testTag(VideoTags.ADD_FROM_PHOTOS),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.local_media_from_files)) },
                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        onClick = {
                            choosing = false
                            openFiles()
                        },
                        modifier = Modifier.testTag(VideoTags.ADD_FROM_FILES),
                    )
                }
            }
        }
    }
    if (importProgress != null) {
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { importProgress },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)),
            color = colors.accent,
            trackColor = colors.inputBg,
        )
    }
}

@Composable
private fun AddVideoRow(importProgress: Float?, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .testTag(VideoTags.ADD)
            .clickable(enabled = importProgress == null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            if (importProgress != null) {
                stringResource(Res.string.local_media_importing, (importProgress * PERCENT).toInt())
            } else {
                stringResource(Res.string.local_media_add_video)
            },
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyVideos() {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag(VideoTags.EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = colors.dim, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(Res.string.local_media_empty_title),
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(Res.string.local_media_empty_body), color = colors.muted, fontSize = 12.sp)
    }
}

@Composable
private fun VideoRow(
    video: LocalVideo,
    thumbnailPath: String?,
    loaded: Boolean,
    looping: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (loaded) colors.accent.copy(alpha = SELECTED_FILL_ALPHA) else colors.surface)
            .border(1.dp, if (loaded) colors.accent.copy(alpha = SELECTED_BORDER_ALPHA) else colors.border, shape)
            .testTag(VideoTags.row(video.id))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VideoThumb(thumbnailPath, if (looping) "∞ ${formatTime(video.durationMs)}" else formatTime(video.durationMs))
        Column(Modifier.weight(1f)) {
            Text(
                video.title,
                color = colors.text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                // Two lines: the tablet's list pane is narrow, and "Countdown - 5 …"
                // does not tell one countdown from another.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                videoDetails(video, looping, stringResource(Res.string.local_media_loops)),
                color = colors.muted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (loaded) {
                Text(
                    "● " + stringResource(Res.string.local_media_loaded),
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Icon(
            Icons.Outlined.DeleteOutline,
            contentDescription = stringResource(Res.string.local_media_cd_delete),
            tint = colors.muted,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .testTag(VideoTags.delete(video.id))
                .clickable(onClick = onDelete)
                .padding(8.dp),
        )
    }
}

@Composable
private fun VideoThumb(path: String?, duration: String) {
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black),
    ) {
        if (path != null) {
            AsyncImage(
                model = "file://$path",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            duration,
            color = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = BADGE_ALPHA))
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun DeleteVideoDialog(video: LocalVideo, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.local_media_delete_title)) },
        text = { Text(stringResource(Res.string.local_media_delete_body, video.title)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(VideoTags.DELETE_CONFIRM)) {
                Text(stringResource(Res.string.library_delete), color = LocalAppColors.current.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(VideoTags.DELETE_CANCEL)) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
