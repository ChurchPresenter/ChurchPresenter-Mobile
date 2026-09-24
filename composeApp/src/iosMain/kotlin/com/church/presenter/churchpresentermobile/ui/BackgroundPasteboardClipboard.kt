package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.NativeClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.UIKit.UIPasteboard

/**
 * A [Clipboard] that reads the pasteboard off the main thread.
 *
 * Compose's own iOS clipboard reads `UIPasteboard.string` on the main thread
 * when a paste happens. Since iOS 16 that read blocks until the user answers
 * the "Allow Paste" prompt, so a hardware-keyboard ⌘V froze the whole app for
 * as long as the prompt was up (CHURCH-PRESENTER-MOBILE-1Y). Reading on a
 * background dispatcher lets the paste coroutine suspend instead.
 *
 * Only plain text is carried, which is all Compose's iOS clipboard supports.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class BackgroundPasteboardClipboard : Clipboard {

    override val nativeClipboard: NativeClipboard
        get() = UIPasteboard.generalPasteboard

    override suspend fun getClipEntry(): ClipEntry? {
        // hasStrings does not trigger the paste prompt; string does.
        if (!nativeClipboard.hasStrings) return null
        val text = withContext(Dispatchers.Default) { nativeClipboard.string } ?: return null
        return ClipEntry.withPlainText(text)
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry == null) {
            nativeClipboard.items = emptyList<Map<String, Any>>()
        } else {
            nativeClipboard.string = clipEntry.getPlainText()
        }
    }
}
