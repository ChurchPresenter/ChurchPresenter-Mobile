package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/**
 * Whether this platform keeps the camera roll's videos out of [MediaFilePicker]'s
 * reach. True on iOS: the Files picker never shows the Photos library, so an
 * iPad with dozens of clips in Photos answered "Add video" with nothing to pick.
 * False on Android, whose document picker already lists Gallery and Google Photos.
 */
expect val photosLibraryIsSeparate: Boolean

/**
 * Picks one video from the device's photo library, as a [MediaFilePicker] does
 * from its files: the callback gets a file that streams on demand, or null when
 * cancelled. Draws [content] and does nothing on a platform where
 * [photosLibraryIsSeparate] is false.
 */
@Composable
expect fun PhotosVideoPicker(
    onFilePicked: OnMediaPickedCallback,
    onError: (String) -> Unit,
    maxBytes: Long,
    content: @Composable (launch: () -> Unit) -> Unit,
)
