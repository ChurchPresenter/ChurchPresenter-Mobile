package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

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
    onError: (String) -> Unit = {},
    content: @Composable (launch: () -> Unit) -> Unit,
)

