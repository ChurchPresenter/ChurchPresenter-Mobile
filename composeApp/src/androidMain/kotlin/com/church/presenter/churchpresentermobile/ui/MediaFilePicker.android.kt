package com.church.presenter.churchpresentermobile.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val MEDIA_MIME_TYPES = arrayOf(
    "video/*",
    "audio/*",
    // Fallback for providers that serve media as octet-stream
    "application/octet-stream",
)

private val VIDEO_MIME_TYPES = arrayOf("video/*", "application/octet-stream")

private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "aiff", "opus")

/** Android actual — streams the picked file straight from its content URI (never buffered whole). */
@Composable
actual fun MediaFilePicker(
    onFilePicked: OnMediaPickedCallback,
    onError: (String) -> Unit,
    maxBytes: Long,
    extensions: Set<String>,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) { onFilePicked(null); return@rememberLauncherForActivityResult }

        scope.launch {
            val meta = withContext(Dispatchers.IO) {
                appContext.contentResolver
                    .query(uri, null, null, null, null)
                    ?.use { cursor ->
                        val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            val n = if (nameCol >= 0) cursor.getString(nameCol) else null
                            val s = if (sizeCol >= 0) cursor.getLong(sizeCol) else -1L
                            Pair(n, s)
                        } else Pair(null, -1L)
                    } ?: Pair(null, -1L)
            }
            val fileName = meta.first ?: "media_${System.currentTimeMillis()}.mp4"
            val sizeBytes = meta.second

            val ext = fileName.substringAfterLast('.', "").lowercase()
            if (ext !in extensions) {
                onFilePicked(null); onError("Unsupported file type: .$ext"); return@launch
            }
            if (sizeBytes > maxBytes) {
                val mb = sizeBytes / (1024 * 1024)
                onFilePicked(null)
                onError("File is too large ($mb MB). Maximum is ${maxBytes / (1024 * 1024)} MB.")
                return@launch
            }

            val picked = PickedMediaFile(fileName, sizeBytes.coerceAtLeast(0L)) { channel, onProgress ->
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        val buffer = ByteArray(256 * 1024)
                        var sent = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            channel.writeFully(buffer, 0, read)
                            sent += read
                            onProgress(sent)
                        }
                        channel.flush()
                    } ?: throw IllegalStateException("Could not open the selected file")
                }
            }
            onFilePicked(picked)
        }
    }

    content {
        // Only videos asked for: don't offer the audio a video library cannot hold.
        val videoOnly = extensions.none { it in AUDIO_EXTENSIONS }
        launcher.launch(if (videoOnly) VIDEO_MIME_TYPES else MEDIA_MIME_TYPES)
    }
}
