package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/** Web stub — media file picking from the browser is not supported in this release. */
@Composable
actual fun MediaFilePicker(
    onFilePicked: OnMediaPickedCallback,
    onError: (String) -> Unit,
    maxBytes: Long,
    extensions: Set<String>,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    content { /* no-op on web */ }
}
