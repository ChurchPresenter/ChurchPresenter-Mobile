package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

@Composable
actual fun MediaFilePicker(
    onFilePicked: OnMediaPickedCallback,
    onError: (String) -> Unit,
    maxBytes: Long,
    extensions: Set<String>,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    content { }
}
