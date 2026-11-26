package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/** The file picker already reaches this platform's gallery, so there is nothing separate to offer. */
actual val photosLibraryIsSeparate: Boolean = false

@Composable
actual fun PhotosVideoPicker(
    onFilePicked: OnMediaPickedCallback,
    onError: (String) -> Unit,
    maxBytes: Long,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    content { }
}
