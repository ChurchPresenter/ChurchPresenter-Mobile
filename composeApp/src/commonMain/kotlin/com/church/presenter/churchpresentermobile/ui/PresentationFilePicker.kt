package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/**
 * A single picked presentation file: raw bytes + the original file name.
 * Accepted types: PowerPoint (.pptx / .ppt), PDF (.pdf), Keynote (.key).
 */
data class PickedFile(val bytes: ByteArray, val fileName: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedFile) return false
        return fileName == other.fileName && bytes.contentEquals(other.bytes)
    }
    override fun hashCode(): Int = 31 * bytes.contentHashCode() + fileName.hashCode()
}

/**
 * Callback invoked with the picked file when the user confirms selection,
 * or `null` when the user cancels.
 */
typealias OnFilePickedCallback = (file: PickedFile?) -> Unit

/**
 * Platform-specific composable that wraps the native document-picker UI.
 * Accepts PowerPoint (.pptx / .ppt), PDF (.pdf), and Keynote (.key) files only.
 *
 * Usage:
 * ```
 * PresentationFilePicker(onFilePicked = { file -> … }) { launch ->
 *     Button(onClick = launch) { Text("Upload Presentation") }
 * }
 * ```
 *
 * @param onFilePicked Called on the UI thread when picking completes.
 * @param content      Receives a `launch: () -> Unit` lambda to trigger the picker.
 */
@Composable
expect fun PresentationFilePicker(
    onFilePicked: OnFilePickedCallback,
    content: @Composable (launch: () -> Unit) -> Unit,
)

