package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

// Standalone mode — and therefore the library these documents describe — is off
// on web, so nothing reaches these. They exist so the shared code compiles.

@Composable
actual fun TextDocumentPicker(
    onPicked: (PickedTextFile?) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    content { onError("Importing files is not available in the browser") }
}

@Composable
actual fun TextDocumentExporter(
    onError: (String) -> Unit,
    content: @Composable (share: (text: String, suggestedName: String) -> Unit) -> Unit,
) {
    content { _, _ -> onError("Exporting files is not available in the browser") }
}
