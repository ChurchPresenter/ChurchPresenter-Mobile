package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSItemProvider
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.time
import kotlin.coroutines.resume

/**
 * iOS actual — presents [PHPickerViewController] from the topmost view controller.
 * Supports multi-selection with no hard limit.
 * Requires NSPhotoLibraryUsageDescription in Info.plist.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PhotoPickerLauncher(
    onPhotoPicked: OnPhotoPickedCallback,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    // Hold the delegate in composition memory so it isn't garbage-collected while the
    // PHPickerViewController is presented (delegate property is weak in ObjC).
    val delegate = remember { IosPhotoPickerDelegate(onPhotoPicked) }

    content {
        val config = PHPickerConfiguration()
        config.selectionLimit = 0L          // 0 = unlimited multi-select
        config.filter = PHPickerFilter.imagesFilter
        val picker = PHPickerViewController(configuration = config)
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

/** Walks the presented-VC chain to find the topmost visible controller. */
private fun topViewController(): UIViewController? {
    var vc: UIViewController? =
        UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    while (vc?.presentedViewController != null) {
        vc = vc.presentedViewController
    }
    return vc
}

@OptIn(ExperimentalForeignApi::class)
private class IosPhotoPickerDelegate(
    private val onPhotoPicked: OnPhotoPickedCallback,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)

        @Suppress("UNCHECKED_CAST")
        val results = didFinishPicking as List<PHPickerResult>

        if (results.isEmpty()) {
            onPhotoPicked(emptyList())
            return
        }

        // Only the names are fixed here; each photo is loaded and re-encoded when the
        // caller reads it. Loading every original up front held them all in memory
        // at once, and a large multi-select ran the app out of memory.
        val stamp = time(null)
        onPhotoPicked(
            results.mapIndexed { index, result ->
                val provider = result.itemProvider
                PickedPhoto("device_photo_${stamp}_$index.jpg") { loadJpeg(provider) }
            },
        )
    }
}

/**
 * One photo as JPEG bytes, or null when it cannot be loaded. The provider's
 * completion block runs on a background thread, so this never blocks the caller.
 */
@OptIn(ExperimentalForeignApi::class)
private suspend fun loadJpeg(provider: NSItemProvider): ByteArray? = suspendCancellableCoroutine { done ->
    provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
        val bytes = data?.let { nsData ->
            // Decode to UIImage first — this normalises HEIC, PNG, BMP, WebP, etc.
            // Then re-encode as JPEG (quality 0.85) so the uploaded bytes always
            // match the ".jpg" file extension and the server can serve them directly.
            val jpegData = UIImageJPEGRepresentation(UIImage(data = nsData), 0.85) ?: nsData
            jpegData.bytes?.readBytes(jpegData.length.toInt())
        }
        done.resume(bytes)
    }
}
