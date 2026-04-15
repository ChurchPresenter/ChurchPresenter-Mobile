package com.church.presenter.churchpresentermobile.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val PRESENTATION_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/x-iwork-keynote-sffkey",
    // Fallback for devices that serve Keynote/PPT as octet-stream
    "application/octet-stream",
)

/** Android actual — uses the system file picker restricted to presentation file types. */
@Composable
actual fun PresentationFilePicker(
    onFilePicked: OnFilePickedCallback,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) { onFilePicked(null); return@rememberLauncherForActivityResult }

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) { onFilePicked(null); return@rememberLauncherForActivityResult }

        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (col >= 0 && cursor.moveToFirst()) cursor.getString(col) else null
        } ?: "presentation_${System.currentTimeMillis()}.pptx"

        // Only forward files with accepted extensions
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext !in setOf("pdf", "ppt", "pptx", "key")) {
            onFilePicked(null)
            return@rememberLauncherForActivityResult
        }

        onFilePicked(PickedFile(bytes, name))
    }

    content {
        launcher.launch(PRESENTATION_MIME_TYPES)
    }
}

