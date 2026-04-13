package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSItemProvider
import platform.Foundation.NSOperationQueue
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

        // Process photos one at a time, dispatching back to the main queue between each
        // to keep collection in-order and thread-safe without explicit locking.
        collectNext(results, 0, mutableListOf())
    }

    private fun collectNext(
        results: List<PHPickerResult>,
        index: Int,
        accumulated: MutableList<PickedPhoto>,
    ) {
        if (index >= results.size) {
            onPhotoPicked(accumulated)
            return
        }

        val itemProvider: NSItemProvider = results[index].itemProvider
        // Request the raw image data. The completion block runs on a background thread.
        itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            val photo = data?.let { nsData ->
                // Decode to UIImage first — this normalises HEIC, PNG, BMP, WebP, etc.
                // Then re-encode as JPEG (quality 0.85) so the uploaded bytes always
                // match the ".jpg" file extension and the server can serve them directly.
                val uiImage = UIImage(data = nsData)
                // Re-encode as JPEG (handles HEIC, PNG, BMP, WebP → always .jpg bytes)
                val jpegData = UIImageJPEGRepresentation(uiImage, 0.85) ?: nsData
                val bytes = jpegData.bytes?.readBytes(jpegData.length.toInt()) ?: ByteArray(0)
                PickedPhoto(bytes, "device_photo_${time(null)}_$index.jpg")
            }
            // Return to main thread before appending + recursing
            NSOperationQueue.mainQueue.addOperationWithBlock {
                if (photo != null) accumulated.add(photo)
                collectNext(results, index + 1, accumulated)
            }
        }
    }
}
