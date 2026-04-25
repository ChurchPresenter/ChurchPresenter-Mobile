package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSData
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject
import platform.posix.time

/**
 * iOS actual — presents a [UIDocumentPickerViewController] (iOS 14+) filtered to
 * PowerPoint (.pptx / .ppt), PDF (.pdf), and Keynote (.key) file types.
 * Uses [UTType.typeWithFilenameExtension] so no hard-coded UTI strings are needed.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PresentationFilePicker(
    onFilePicked: OnFilePickedCallback,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val delegate = remember { IosDocumentPickerDelegate(onFilePicked) }

    content {
        // Build UTType list from file extensions — returns null for unknown types, so we filter
        val utTypes: List<UTType> = listOfNotNull(
            UTType.typeWithFilenameExtension("pdf"),
            UTType.typeWithFilenameExtension("pptx"),
            UTType.typeWithFilenameExtension("ppt"),
            UTType.typeWithFilenameExtension("key"),
        )

        // asCopy = true is equivalent to the old UIDocumentPickerModeImport:
        // the system copies the file into a sandbox-accessible temp location.
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = utTypes,
            asCopy = true,
        )
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
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
private class IosDocumentPickerDelegate(
    private val onFilePicked: OnFilePickedCallback,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)

        @Suppress("UNCHECKED_CAST")
        val urls = didPickDocumentsAtURLs as List<NSURL>
        val url = urls.firstOrNull() ?: run { onFilePicked(null); return }

        // Read on a background thread, then dispatch result to main queue
        NSOperationQueue().addOperationWithBlock {
            url.startAccessingSecurityScopedResource()
            val data: NSData? = NSData.dataWithContentsOfURL(url)
            url.stopAccessingSecurityScopedResource()

            val bytes = data?.let { it.bytes?.readBytes(it.length.toInt()) } ?: run {
                NSOperationQueue.mainQueue.addOperationWithBlock { onFilePicked(null) }
                return@addOperationWithBlock
            }

            val rawName = url.lastPathComponent ?: "presentation_${time(null)}.pptx"
            val ext = rawName.substringAfterLast('.', "").lowercase()
            if (ext !in setOf("pdf", "ppt", "pptx", "key")) {
                NSOperationQueue.mainQueue.addOperationWithBlock { onFilePicked(null) }
                return@addOperationWithBlock
            }

            val picked = PickedFile(bytes, rawName)
            NSOperationQueue.mainQueue.addOperationWithBlock { onFilePicked(picked) }
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        controller.dismissViewControllerAnimated(true, completion = null)
        onFilePicked(null)
    }
}
