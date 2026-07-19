package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.network.PickedMediaFile

/** Callback with the picked media file (streams on demand), or `null` when cancelled. */
typealias OnMediaPickedCallback = (PickedMediaFile?) -> Unit

/**
 * Platform-specific composable wrapping the native document/media picker, filtered to
 * video (mp4/mov/avi/mkv/wmv/flv/webm/m4v) and audio (mp3/wav/flac/aac/ogg/wma/m4a/aiff/opus).
 *
 * The returned [PickedMediaFile] streams its bytes on demand — the whole file is never held
 * in memory, so large videos don't OOM the device.
 *
 * Usage:
 * ```
 * MediaFilePicker(onFilePicked = { file -> … }) { launch ->
 *     Button(onClick = launch) { Text("Upload from phone") }
 * }
 * ```
 */
@Composable
expect fun MediaFilePicker(
    onFilePicked: OnMediaPickedCallback,
    onError: (String) -> Unit = {},
    maxBytes: Long = MAX_MEDIA_UPLOAD_BYTES,
    content: @Composable (launch: () -> Unit) -> Unit,
)

/**
 * Accepted media file extensions — kept in sync with what the desktop player (VLC) can play
 * (desktop `Constants.VIDEO_EXTENSIONS` + `Constants.AUDIO_EXTENSIONS`).
 */
internal val MEDIA_FILE_EXTENSIONS = setOf(
    // video
    "mp4", "mov", "avi", "mkv", "wmv", "flv", "webm", "m4v",
    // audio
    "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "aiff", "opus",
)

/** Default max upload size (700 MB) — covers a 5-min phone HD clip, blocks full movies.
 *  The desktop is authoritative and may override this; see [MediaFilePicker] callers. */
internal const val MAX_MEDIA_UPLOAD_BYTES = 700L * 1024 * 1024
