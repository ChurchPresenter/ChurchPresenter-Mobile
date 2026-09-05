package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/** A text document the user chose, plus its file name. */
data class PickedTextFile(val text: String, val fileName: String)

/**
 * Opens a text document — a `.cpset` library/set, or a ChordPro song file.
 *
 * Follows the same shape as [PresentationFilePicker]: the platform owns the
 * picker UI and hands back content, and the caller supplies the button.
 *
 * @param onPicked Called with the file, or `null` when the user cancels.
 */
@Composable
expect fun TextDocumentPicker(
    onPicked: (PickedTextFile?) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
)

/**
 * Saves [text] as a file the user can keep or send on.
 *
 * On Android and iOS this raises the system share sheet, which covers both
 * "save to Files" and "send to the other operator" without the app having to
 * care which they meant.
 *
 * @param suggestedName File name including extension, e.g. `Sunday.cpset`.
 */
@Composable
expect fun TextDocumentExporter(
    onError: (String) -> Unit,
    content: @Composable (share: (text: String, suggestedName: String) -> Unit) -> Unit,
)
