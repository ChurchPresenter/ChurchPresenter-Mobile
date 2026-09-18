package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

@Composable
actual fun TextDocumentPicker(
    onPicked: (PickedTextFile?) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    content { onPicked(null) }
}

@Composable
actual fun TextDocumentExporter(
    onError: (String) -> Unit,
    content: @Composable (share: (text: String, suggestedName: String) -> Unit) -> Unit,
) {
    content { _, _ -> }
}

@Composable
actual fun BinaryDocumentExporter(
    onError: (String) -> Unit,
    content: @Composable (share: (bytes: ByteArray, suggestedName: String, mimeType: String) -> Unit) -> Unit,
) {
    content { _, _, _ -> }
}
