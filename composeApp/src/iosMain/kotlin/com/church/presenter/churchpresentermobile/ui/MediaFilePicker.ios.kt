package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import io.ktor.utils.io.writeFully
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject
import platform.posix.time

private const val BYTES_PER_MB = 1024 * 1024

/** Read per chunk while streaming a picked file — keeps progress smooth and memory flat. */
private const val READ_CHUNK_BYTES = 256 * 1024

/**
 * iOS actual — presents a document picker filtered to [extensions]. The picker hands back a
 * copy of the file, read and streamed to the channel in chunks so a large video is never held whole.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MediaFilePicker(
    onFilePicked: OnMediaPickedCallback,
    onError: (String) -> Unit,
    maxBytes: Long,
    extensions: Set<String>,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val delegate = remember(extensions) { IosMediaPickerDelegate(onFilePicked, onError, maxBytes, extensions) }

    content {
        val utTypes: List<UTType> = extensions.mapNotNull { ext ->
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
    private val extensions: Set<String>,
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
            if (ext !in extensions) {
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    onFilePicked(null)
                    onError("Unsupported file type: .$ext")
                }
                return@addOperationWithBlock
            }

            // asCopy = true put the file in our own temporary folder, so it can be
            // read in chunks rather than loaded whole: a 2 GB video copied into
            // the standalone library must never be one allocation.
            val path = url.path ?: run {
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    onFilePicked(null)
                    onError("Could not read the selected file")
                }
                return@addOperationWithBlock
            }
            val size = (NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
                ?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
            if (size > maxBytes) {
                val mb = size / BYTES_PER_MB
                NSOperationQueue.mainQueue.addOperationWithBlock {
                    onFilePicked(null)
                    onError("File is too large ($mb MB). Maximum is ${maxBytes / BYTES_PER_MB} MB.")
                }
                return@addOperationWithBlock
            }

            val picked = PickedMediaFile(rawName, size) { channel, onProgress ->
                val handle = NSFileHandle.fileHandleForReadingAtPath(path)
                    ?: error("Could not open the selected file")
                try {
                    var sent = 0L
                    var bytes = handle.readChunk()
                    while (bytes != null) {
                        channel.writeFully(bytes, 0, bytes.size)
                        sent += bytes.size
                        onProgress(sent)
                        bytes = handle.readChunk()
                    }
                    channel.flush()
                } finally {
                    handle.closeFile()
                }
            }
            NSOperationQueue.mainQueue.addOperationWithBlock { onFilePicked(picked) }
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        controller.dismissViewControllerAnimated(true, completion = null)
        onFilePicked(null)
    }
}

/** The next chunk of the file, or null at its end. */
@OptIn(ExperimentalForeignApi::class)
private fun NSFileHandle.readChunk(): ByteArray? {
    val chunk = readDataOfLength(READ_CHUNK_BYTES.toULong())
    val count = chunk.length.toInt()
    return if (count == 0) null else chunk.bytes?.readBytes(count)
}
