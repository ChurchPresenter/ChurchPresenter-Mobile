package com.church.presenter.churchpresentermobile.ui

import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

/** The largest Bible XML in any of the archives is 21 MB; this leaves room and refuses a video. */
private const val MAX_BINARY_DOCUMENT_BYTES = 64L * 1024 * 1024

@Composable
actual fun BinaryDocumentPicker(
    onPicked: (PickedBinaryFile?) -> Unit,
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

                    if (size > MAX_BINARY_DOCUMENT_BYTES) error("That file is too large to be a Bible")

                    val bytes = context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?: error("Could not read that file")

                    PickedBinaryFile(bytes = bytes, fileName = name ?: "import.xml")
                }
            }

            result
                .onSuccess(onPicked)
                .onFailure { onPicked(null); onError(it.message ?: "Could not read that file") }
        }
    }

    // Bible XML arrives as text/xml, application/xml or — from a download — octet-stream.
    content { launcher.launch(arrayOf("text/xml", "application/xml", "application/octet-stream", "*/*")) }
}

@Composable
actual fun ImageFilePicker(
    onPicked: (PickedBinaryFile?) -> Unit,
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
                    val bytes = context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?: error("Could not read that picture")
                    if (bytes.size > MAX_BINARY_DOCUMENT_BYTES) error("That picture is too large")
                    PickedBinaryFile(bytes = bytes, fileName = uri.lastPathSegment ?: "picture")
                }
            }

            result
                .onSuccess(onPicked)
                .onFailure { onPicked(null); onError(it.message ?: "Could not read that picture") }
        }
    }

    content { launcher.launch(arrayOf("image/*")) }
}

@Composable
actual fun TextDocumentExporter(
    onError: (String) -> Unit,
    content: @Composable (share: (writer: TextDocumentWriter, suggestedName: String) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Both presses would write the same file, and each would raise its own share sheet.
    var writing by remember { mutableStateOf(false) }

    content { writer, suggestedName ->
        if (!writing) {
            writing = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { writeExportFile(context, suggestedName, writer) }
                        .mapCatching { startShare(context, it, suggestedName, "application/json") }
                        .onFailure { onError(it.message ?: "Could not share that file") }
                } finally {
                    writing = false
                }
            }
        }
    }
}

@Composable
actual fun BinaryDocumentExporter(
    onError: (String) -> Unit,
    content: @Composable (share: (bytes: ByteArray, suggestedName: String, mimeType: String) -> Unit) -> Unit,
) {
    val context = LocalContext.current

    content { bytes, suggestedName, mimeType -> shareFile(context, bytes, suggestedName, mimeType, onError) }
}

/**
 * Writes [bytes] to the app's cache and raises the share sheet for it.
 *
 * Shared through the existing FileProvider, so no storage permission is involved.
 */
private fun shareFile(
    context: Context,
    bytes: ByteArray,
    suggestedName: String,
    mimeType: String,
    onError: (String) -> Unit,
) {
    runCatching {
        val file = exportFile(context, suggestedName).apply { writeBytes(bytes) }
        startShare(context, file, suggestedName, mimeType)
    }.onFailure { onError(it.message ?: "Could not share that file") }
}

/** Streams [writer]'s pieces to the export file, so the document is never whole in memory. */
private fun writeExportFile(context: Context, name: String, writer: TextDocumentWriter): Result<File> =
    runCatching {
        exportFile(context, name).also { file ->
            file.bufferedWriter().use { out -> writer { out.write(it) } }
        }
    }

/** Where an export is written: the app's cache, which the FileProvider below can share from. */
private fun exportFile(context: Context, name: String): File =
    File(File(context.cacheDir, "exports").apply { mkdirs() }, name)

private fun startShare(context: Context, file: File, suggestedName: String, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, suggestedName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(intent, suggestedName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
