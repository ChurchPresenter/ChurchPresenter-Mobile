package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import io.ktor.utils.io.writeFully
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
 * iOS actual — presents a document picker filtered to video and audio. iOS has no tight
 * per-app heap cap, so the file is read via NSData and streamed to the channel in chunks.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MediaFilePicker(
    onFilePicked: OnMediaPickedCallback,
    onError: (String) -> Unit,
    maxBytes: Long,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val delegate = remember { IosMediaPickerDelegate(onFilePicked, onError, maxBytes) }

    content {
        val utTypes: List<UTType> = MEDIA_FILE_EXTENSIONS.mapNotNull { ext ->
            UTType.typeWithFilenameExtension(ext)
        }
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = utTypes,
            asCopy = true,
        )
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        topMediaViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

private fun topMediaViewController(): UIViewController? {
    var vc: UIViewController? =
        UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    while (vc?.presentedViewController != null) {
        vc = vc.presentedViewController
    }
    return vc
}

@OptIn(ExperimentalForeignApi::class)
private class IosMediaPickerDelegate(
    private val onFilePicked: OnMediaPickedCallback,
    private val onError: (String) -> Unit,
    private val maxBytes: Long,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)

        @Suppress("UNCHECKED_CAST")
        val urls = didPickDocumentsAtURLs as List<NSURL>
        val url = urls.firstOrNull() ?: run { onFilePicked(null); return }

        NSOperationQueue().addOperationWithBlock {
            val rawName = url.lastPathComponent ?: "media_${time(null)}.mp4"
            val ext = rawName.substringAfterLast('.', "").lowercase()
            if (ext !in MEDIA_FILE_EXTENSIONS) {
                NSOperationQueue.mainQueue.addOperationWithBlock { onFilePicked(null); onError("Unsupported file type: .$ext") }
                return@addOperationWithBlock
            }

            url.startAccessingSecurityScopedResource()
            val data: NSData? = NSData.dataWithContentsOfURL(url)
            url.stopAccessingSecurityScopedResource()

            val bytes = data?.let { it.bytes?.readBytes(it.length.toInt()) } ?: run {
                NSOperationQueue.mainQueue.addOperationWithBlock { onFilePicked(null); onError("Could not read the selected file") }
                return@addOperationWithBlock
            }
            if (bytes.size.toLong() > maxBytes) {
                val mb = bytes.size / (1024 * 1024)
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    onFilePicked(null)
                    onError("File is too large ($mb MB). Maximum is ${maxBytes / (1024 * 1024)} MB.")
                }
                return@addOperationWithBlock
            }

            val picked = PickedMediaFile(rawName, bytes.size.toLong()) { channel, onProgress ->
                // Write in chunks so upload progress advances smoothly.
                val chunk = 256 * 1024
                var offset = 0
                while (offset < bytes.size) {
                    val end = minOf(offset + chunk, bytes.size)
                    channel.writeFully(bytes, offset, end - offset)
                    offset = end
                    onProgress(offset.toLong())
                }
                channel.flush()
            }
            NSOperationQueue.mainQueue.addOperationWithBlock { onFilePicked(picked) }
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        controller.dismissViewControllerAnimated(true, completion = null)
        onFilePicked(null)
    }
}
