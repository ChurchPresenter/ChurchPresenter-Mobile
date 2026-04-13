package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/** JS stub — photo picking from the browser file system is not supported in this release. */
@Composable
actual fun PhotoPickerLauncher(
    onPhotoPicked: OnPhotoPickedCallback,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    content { /* no-op on web */ }
}

