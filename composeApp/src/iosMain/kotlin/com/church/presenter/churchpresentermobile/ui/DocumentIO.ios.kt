package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.memcpy

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

/** Same picker as [TextDocumentPicker]; the file comes back as bytes rather than as UTF-8 text. */
@Composable
actual fun BinaryDocumentPicker(
    onPicked: (PickedBinaryFile?) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val delegate = remember { BinaryPickerDelegate() }
    delegate.onPicked = onPicked
    delegate.onError = onError

    content {
        val controller = UIDocumentPickerViewController(
            documentTypes = listOf("public.xml", "public.data"),
            inMode = platform.UIKit.UIDocumentPickerMode.UIDocumentPickerModeImport,
        )
        controller.delegate = delegate
        rootViewController()?.presentViewController(controller, animated = true, completion = null)
            ?: onError("Could not open the file picker")
    }
}

@OptIn(ExperimentalForeignApi::class)
private class BinaryPickerDelegate : NSObject(), UIDocumentPickerDelegateProtocol {
    var onPicked: ((PickedBinaryFile?) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) { onPicked?.invoke(null); return }

        val claimed = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url)
            if (data == null) {
                onError?.invoke("Could not read that file")
                onPicked?.invoke(null)
            } else {
                val bytes = ByteArray(data.length.toInt())
                if (bytes.isNotEmpty()) {
                    bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
                }
                onPicked?.invoke(PickedBinaryFile(bytes = bytes, fileName = url.lastPathComponent ?: "import.xml"))
            }
        } finally {
            if (claimed) url.stopAccessingSecurityScopedResource()
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked?.invoke(null)
    }
}

/** Same picker as [BinaryDocumentPicker], showing only pictures. */
@Composable
actual fun ImageFilePicker(
    onPicked: (PickedBinaryFile?) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val delegate = remember { BinaryPickerDelegate() }
    delegate.onPicked = onPicked
    delegate.onError = onError

    content {
        val controller = UIDocumentPickerViewController(
            documentTypes = listOf("public.image"),
            inMode = platform.UIKit.UIDocumentPickerMode.UIDocumentPickerModeImport,
        )
        controller.delegate = delegate
        rootViewController()?.presentViewController(controller, animated = true, completion = null)
            ?: onError("Could not open the file picker")
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
    content: @Composable (share: (writer: TextDocumentWriter, suggestedName: String) -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Both presses would write the same file, and each would raise its own share sheet.
    var writing by remember { mutableStateOf(false) }

    content { writer, suggestedName ->
        if (!writing) {
            writing = true
            scope.launch {
                try {
                    val path = NSTemporaryDirectory() + suggestedName
                    val written = withContext(Dispatchers.IO) {
                        runCatching { writeStreamed(path, writer) }.getOrDefault(false)
                    }
                    shareWritten(written, path, onError)
                } finally {
                    writing = false
                }
            }
        }
    }
}

/** Writes [writer]'s pieces straight to [path], UTF-8, so the document is never whole in memory. */
@OptIn(ExperimentalForeignApi::class)
private fun writeStreamed(path: String, writer: TextDocumentWriter): Boolean {
    val file = fopen(path, "w") ?: return false
    var ok = true
    try {
        writer { piece -> if (fputs(piece, file) < 0) ok = false }
    } finally {
        if (fclose(file) != 0) ok = false
    }
    return ok
}

/** The bytes go through NSData; the share sheet is the same one the text export uses. */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun BinaryDocumentExporter(
    onError: (String) -> Unit,
    content: @Composable (share: (bytes: ByteArray, suggestedName: String, mimeType: String) -> Unit) -> Unit,
) {
    content { bytes, suggestedName, _ ->
        val path = NSTemporaryDirectory() + suggestedName
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        shareWritten(data.writeToFile(path, atomically = true), path, onError)
    }
}

/** Raises the share sheet for the file at [path], or reports why it cannot. */
private fun shareWritten(written: Boolean, path: String, onError: (String) -> Unit) {
    if (!written) {
        Logger.e(TAG, "could not write export to $path")
        onError("Could not prepare that file")
        return
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

private fun rootViewController(): UIViewController? =
    UIApplication.sharedApplication.keyWindow?.rootViewController
