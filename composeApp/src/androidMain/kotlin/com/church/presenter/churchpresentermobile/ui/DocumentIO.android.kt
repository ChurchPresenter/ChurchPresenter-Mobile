package com.church.presenter.churchpresentermobile.ui

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * `.cpset` and ChordPro files are plain text, but Android reports them
 * inconsistently — a file emailed as an attachment often arrives as
 * `application/octet-stream`. Accepting the broad types and validating the
 * content is more reliable than trusting the MIME type.
 */
private val TEXT_DOCUMENT_MIME_TYPES = arrayOf(
    "application/json",
    "text/plain",
    "application/octet-stream",
    "*/*",
)

/** A library export is text; anything this size is not one. */
private const val MAX_DOCUMENT_BYTES = 25L * 1024 * 1024

@Composable
actual fun TextDocumentPicker(
    onPicked: (PickedTextFile?) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) { onPicked(null); return@rememberLauncherForActivityResult }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val (name, size) = context.contentResolver
                        .query(uri, null, null, null, null)
                        ?.use { cursor ->
                            if (!cursor.moveToFirst()) return@use null
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            val fileName = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                            val bytes = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                            fileName to bytes
                        } ?: (null to 0L)

                    if (size > MAX_DOCUMENT_BYTES) {
                        error("That file is too large to be a song library")
                    }

                    val text = context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().decodeToString() }
                        ?: error("Could not read that file")

                    PickedTextFile(text = text, fileName = name ?: "import")
                }
            }

            result
                .onSuccess(onPicked)
                .onFailure { onPicked(null); onError(it.message ?: "Could not read that file") }
        }
    }

    content { launcher.launch(TEXT_DOCUMENT_MIME_TYPES) }
}

@Composable
actual fun TextDocumentExporter(
    onError: (String) -> Unit,
    content: @Composable (share: (text: String, suggestedName: String) -> Unit) -> Unit,
) {
    val context = LocalContext.current

    content { text, suggestedName ->
        runCatching {
            // Written to the app's cache and shared through the existing
            // FileProvider, so no storage permission is involved.
            val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(outDir, suggestedName).apply { writeText(text) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, suggestedName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, suggestedName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { onError(it.message ?: "Could not share that file") }
    }
}
