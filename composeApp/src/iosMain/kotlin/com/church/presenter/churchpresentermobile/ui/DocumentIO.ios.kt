package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject

private const val TAG = "DocumentIO"

/**
 * Opens a document with the system picker.
 *
 * The delegate is held in a `remember` so it is not collected while the picker
 * is on screen — UIKit keeps only a weak reference, and a collected delegate
 * silently produces a picker that never calls back.
 */
@Composable
actual fun TextDocumentPicker(
    onPicked: (PickedTextFile?) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val delegate = remember { PickerDelegate() }
    delegate.onPicked = onPicked
    delegate.onError = onError

    content {
        val controller = UIDocumentPickerViewController(
            documentTypes = listOf("public.json", "public.plain-text", "public.text", "public.data"),
            inMode = platform.UIKit.UIDocumentPickerMode.UIDocumentPickerModeImport,
        )
        controller.delegate = delegate
        rootViewController()?.presentViewController(controller, animated = true, completion = null)
            ?: onError("Could not open the file picker")
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PickerDelegate : NSObject(), UIDocumentPickerDelegateProtocol {
    var onPicked: ((PickedTextFile?) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) { onPicked?.invoke(null); return }

        // A picked file lives outside the app sandbox until access is claimed.
        val claimed = url.startAccessingSecurityScopedResource()
        try {
            val path: String? = url.path
            val text: String? = path?.let { filePath ->
                NSString.stringWithContentsOfFile(filePath, encoding = NSUTF8StringEncoding, error = null)
            }
            if (text == null) {
                onError?.invoke("Could not read that file")
                onPicked?.invoke(null)
            } else {
                onPicked?.invoke(PickedTextFile(text = text, fileName = url.lastPathComponent ?: "import"))
            }
        } finally {
            if (claimed) url.stopAccessingSecurityScopedResource()
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked?.invoke(null)
    }
}

/**
 * Writes the export to a temporary file and raises the share sheet, which
 * covers both "save to Files" and "send it on" in one gesture.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun TextDocumentExporter(
    onError: (String) -> Unit,
    content: @Composable (share: (text: String, suggestedName: String) -> Unit) -> Unit,
) {
    content { text, suggestedName ->
        val path = NSTemporaryDirectory() + suggestedName
        val written = (text as NSString).writeToFile(
            path = path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        if (!written) {
            Logger.e(TAG, "could not write export to $path")
            onError("Could not prepare that file")
            return@content
        }

        val url = NSURL.fileURLWithPath(path)
        val controller = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        val root = rootViewController()
        if (root == null) {
            onError("Could not open the share sheet")
        } else {
            // Required on iPad, where an unanchored sheet crashes.
            controller.popoverPresentationController?.sourceView = root.view
            root.presentViewController(controller, animated = true, completion = null)
        }
    }
}

private fun rootViewController(): UIViewController? =
    UIApplication.sharedApplication.keyWindow?.rootViewController
