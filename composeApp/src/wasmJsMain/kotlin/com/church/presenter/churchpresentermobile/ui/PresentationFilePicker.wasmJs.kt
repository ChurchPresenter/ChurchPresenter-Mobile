package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/** WasmJS stub — file picking from the browser is not supported in this release. */
@Composable
actual fun PresentationFilePicker(
    onFilePicked: OnFilePickedCallback,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    content { /* no-op on web */ }
}

