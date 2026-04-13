package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/** WasmJS stub — photo picking from the browser is not supported in this release. */
@Composable
actual fun PhotoPickerLauncher(
    onPhotoPicked: OnPhotoPickedCallback,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    content { /* no-op on web */ }
}

