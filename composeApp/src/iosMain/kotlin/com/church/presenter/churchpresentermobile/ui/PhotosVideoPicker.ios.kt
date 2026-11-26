package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationAssetRepresentationModeCurrent
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject

/** The type the picker is asked for: any movie, in whatever container Photos holds it. */
private const val MOVIE_TYPE = "public.movie"

private const val BYTES_PER_MB = 1024 * 1024

actual val photosLibraryIsSeparate: Boolean = true

/**
 * iOS actual — PHPickerViewController filtered to videos. Needs no photo-library
 * permission: the picker runs outside the app and hands back only what was chosen.
 */
@Composable
actual fun PhotosVideoPicker(
    onFilePicked: OnMediaPickedCallback,
    onError: (String) -> Unit,
    maxBytes: Long,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    // Held in composition: the picker's delegate property is weak.
    val delegate = remember { PhotosVideoDelegate(onFilePicked, onError, maxBytes) }
    content {
        val config = PHPickerConfiguration()
        config.selectionLimit = 1L
        config.filter = PHPickerFilter.videosFilter
        // The clip as stored, not transcoded to a "compatible" copy first — that
        // can take minutes for a long video and loses quality for nothing.
        config.preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCurrent
        val picker = PHPickerViewController(configuration = config)
        picker.delegate = delegate
        topController()?.presentViewController(picker, animated = true, completion = null)
    }
}

private fun topController(): UIViewController? {
    var vc: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

@OptIn(ExperimentalForeignApi::class)
private class PhotosVideoDelegate(
    private val onFilePicked: OnMediaPickedCallback,
    private val onError: (String) -> Unit,
    private val maxBytes: Long,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (provider == null) {
            onFilePicked(null)
            return
        }
        val name = provider.suggestedName
        provider.loadFileRepresentationForTypeIdentifier(MOVIE_TYPE) { url, _ ->
            // The URL is only valid inside this block, so the clip is copied out to a
            // temporary file of our own and streamed from there, then deleted.
            val copy = url?.let { copyOut(it, name) }
            NSOperationQueue.mainQueue.addOperationWithBlock { deliver(copy) }
        }
    }

    private fun deliver(copy: Pair<String, String>?) {
        if (copy == null) {
            onFilePicked(null)
            onError("Could not read the selected video")
            return
        }
        val (fileName, path) = copy
        val size = fileSizeAt(path)
        if (size > maxBytes) {
            NSFileManager.defaultManager.removeItemAtPath(path, error = null)
            onFilePicked(null)
            onError("Video is too large (${size / BYTES_PER_MB} MB). Maximum is ${maxBytes / BYTES_PER_MB} MB.")
            return
        }
        onFilePicked(streamingPickedFile(fileName, path, size, deleteAfter = true))
    }

    /** Copies [source] somewhere that outlives the picker. Returns the name to show and the copy's path. */
    private fun copyOut(source: NSURL, suggested: String?): Pair<String, String>? {
        val from = source.path ?: return null
        val ext = source.pathExtension?.lowercase()?.ifBlank { null } ?: "mov"
        val path = NSTemporaryDirectory() + "picked-" + NSUUID().UUIDString + "." + ext
        val copied = NSFileManager.defaultManager.copyItemAtPath(from, toPath = path, error = null)
        if (!copied) return null
        val base = suggested?.ifBlank { null } ?: source.lastPathComponent?.substringBeforeLast('.') ?: "Video"
        return "$base.$ext" to path
    }
}
