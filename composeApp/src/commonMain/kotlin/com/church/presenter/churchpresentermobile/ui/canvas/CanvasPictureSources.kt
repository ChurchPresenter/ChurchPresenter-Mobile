package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.church.presenter.churchpresentermobile.model.ImageSource
import com.church.presenter.churchpresentermobile.ui.ImageFilePicker
import com.church.presenter.churchpresentermobile.ui.PhotoPickerLauncher
import com.church.presenter.churchpresentermobile.viewmodel.ImagePick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

/*
 * Real pictures on the canvas: the platform pickers they come in through, and the
 * bytes they are drawn from.
 */

/**
 * A placed picture's bytes by key, or null for one that is not kept. Provided by
 * the canvas screen from its picture store; nothing anywhere else, so a preview or
 * a test draws every picture as its placeholder.
 */
internal val LocalCanvasPictures = staticCompositionLocalOf<(String) -> ByteArray?> { { null } }

/** The picture kept as [key], decoded off the main thread — null until it is, or if it will not decode. */
@Composable
internal fun rememberPicture(key: String?): State<ImageBitmap?> {
    val pictures = LocalCanvasPictures.current
    return produceState<ImageBitmap?>(initialValue = null, key, pictures) {
        value = key?.let {
            withContext(Dispatchers.Default) {
                pictures(it)?.let { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
            }
        }
    }
}

/** The pickers' launch actions, caught as each picker composes. */
private class PickerLaunchers {
    var photos: (() -> Unit)? = null
    var files: (() -> Unit)? = null
}

/**
 * The photo library and file pickers, opened whenever [picking] asks for one.
 *
 * @param onPicked The picked picture's bytes, or null when the picker was cancelled.
 * @param onUnreadable The file picker could not read what was picked.
 */
@Composable
internal fun PicturePickers(
    picking: ImagePick?,
    onPicked: (ByteArray?) -> Unit,
    onUnreadable: () -> Unit,
) {
    val launchers = remember { PickerLaunchers() }
    PhotoPickerLauncher(onPhotoPicked = { photos -> onPicked(photos.firstOrNull()?.bytes) }) { launch ->
        launchers.photos = launch
    }
    ImageFilePicker(onPicked = { file -> onPicked(file?.bytes) }, onError = { onUnreadable() }) { launch ->
        launchers.files = launch
    }
    LaunchedEffect(picking) {
        when (picking?.source) {
            ImageSource.PHOTOS -> launchers.photos?.invoke()
            ImageSource.FILES -> launchers.files?.invoke()
            else -> Unit
        }
    }
}
