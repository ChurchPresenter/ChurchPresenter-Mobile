package com.church.presenter.churchpresentermobile.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PRESENTATION_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/x-iwork-keynote-sffkey",
    // Fallback for devices that serve Keynote/PPT as octet-stream
    "application/octet-stream",
)

/** Maximum accepted file size in bytes (75 MB). Base64 encoding adds ~33 % overhead,
 *  so a 75 MB file produces ~100 MB on the wire — safe for most Android heap limits. */
private const val MAX_FILE_BYTES = 75L * 1024 * 1024

/** Android actual — uses the system file picker restricted to presentation file types. */
@Composable
actual fun PresentationFilePicker(
    onFilePicked: OnFilePickedCallback,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) { onFilePicked(null); return@rememberLauncherForActivityResult }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                // Query display name and size together
                val (name, sizeBytes) = context.contentResolver
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

                val fileName = name ?: "presentation_${System.currentTimeMillis()}.pptx"

                // Reject unsupported extensions early (before reading the whole file)
                val ext = fileName.substringAfterLast('.', "").lowercase()
                if (ext !in setOf("pdf", "ppt", "pptx", "key")) return@withContext null

                // Reject files that are too large to fit in memory
                if (sizeBytes > MAX_FILE_BYTES) {
                    val sizeMb = sizeBytes / (1024 * 1024)
                    val limitMb = MAX_FILE_BYTES / (1024 * 1024)
                    return@withContext "File is too large (${sizeMb} MB). Maximum supported size is ${limitMb} MB."
                }

                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext null

                PickedFile(bytes, fileName)
            }

            when (result) {
                is PickedFile -> onFilePicked(result)
                is String     -> { onFilePicked(null); onError(result) }
                else          -> onFilePicked(null)
            }
        }
    }

    content {
        launcher.launch(PRESENTATION_MIME_TYPES)
    }
}
