package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable

/**
 * A single picked photo: raw JPEG/PNG bytes + a suggested file name.
 */
data class PickedPhoto(val bytes: ByteArray, val fileName: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedPhoto) return false
        return fileName == other.fileName && bytes.contentEquals(other.bytes)
    }
    override fun hashCode(): Int = 31 * bytes.contentHashCode() + fileName.hashCode()
}

/**
 * Callback invoked with the list of picked photos when the user confirms selection.
 * The list is empty when the user cancels or no photos are available.
 */
typealias OnPhotoPickedCallback = (photos: List<PickedPhoto>) -> Unit

/**
 * Platform-specific composable that wraps the native photo-picker UI.
 * Supports multi-selection on Android (system Photo Picker) and iOS (PHPickerViewController).
 *
 * Usage:
 * ```
 * PhotoPickerLauncher(onPhotoPicked = { photos -> … }) { launch ->
 *     Button(onClick = launch) { Text("Pick photos") }
 * }
 * ```
 *
 * @param onPhotoPicked Called on the UI thread when picking completes.
 * @param content       Receives a `launch: () -> Unit` lambda to trigger the picker.
 */
@Composable
expect fun PhotoPickerLauncher(
    onPhotoPicked: OnPhotoPickedCallback,
    content: @Composable (launch: () -> Unit) -> Unit,
)

