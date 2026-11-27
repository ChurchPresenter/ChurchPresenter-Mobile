package com.church.presenter.churchpresentermobile.present

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.duration
import platform.AVFoundation.naturalSize
import platform.AVFoundation.preferredTransform
import platform.AVFoundation.tracksWithMediaType
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.Foundation.seekToFileOffset
import platform.Foundation.writeToFile
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DIRECTORY = "videos"

/** Where in the clip the list's still is taken from — past any fade-in from black. */
private const val THUMB_AT_SECONDS = 1.0
private const val THUMB_TIMESCALE = 600
private const val THUMB_WIDTH_PX = 480.0
private const val THUMB_QUALITY = 0.8
private const val MS_PER_SECOND = 1000.0

/**
 * A `videos` folder in Documents, kept out of iCloud backup — a church's videos
 * can run to gigabytes, and they are copies of files the operator still has.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun videoStorageDirectory(): String? {
    val manager = NSFileManager.defaultManager
    val documents = manager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL
    val dir = documents?.URLByAppendingPathComponent(DIRECTORY)
    val path = dir?.path ?: return null
    if (!manager.fileExistsAtPath(path)) {
        manager.createDirectoryAtURL(dir, withIntermediateDirectories = true, attributes = null, error = null)
        dir.setResourceValue(true, forKey = "NSURLIsExcludedFromBackupKey", error = null)
    }
    return path
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun readFileRange(path: String, offset: Long, length: Int): ByteArray? {
    val handle = NSFileHandle.fileHandleForReadingAtPath(path) ?: return null
    try {
        handle.seekToFileOffset(offset.toULong())
        val data = handle.readDataOfLength(length.toULong())
        val count = data.length.toInt()
        if (count == 0) return ByteArray(0)
        return data.bytes?.readBytes(count)
    } finally {
        handle.closeFile()
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun probeVideoFile(path: String, thumbPath: String): VideoProbe? {
    val asset = AVURLAsset(uRL = NSURL.fileURLWithPath(path), options = null)
    val seconds = CMTimeGetSeconds(asset.duration)
    val track = asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? AVAssetTrack
    var width = 0
    var height = 0
    track?.let {
        val natural = it.naturalSize.useContents { this.width to this.height }
        // A clip shot upright stores landscape pixels plus a quarter-turn transform.
        val sideways = it.preferredTransform.useContents { abs(this.b) == 1.0 && abs(this.c) == 1.0 }
        width = (if (sideways) natural.second else natural.first).roundToInt()
        height = (if (sideways) natural.first else natural.second).roundToInt()
    }
    writeThumbnail(asset, thumbPath)
    return VideoProbe(
        durationMs = if (seconds.isNaN()) 0L else (seconds * MS_PER_SECOND).toLong(),
        width = width,
        height = height,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun writeThumbnail(asset: AVURLAsset, thumbPath: String) {
    val generator = AVAssetImageGenerator(asset = asset)
    generator.appliesPreferredTrackTransform = true
    generator.maximumSize = CGSizeMake(THUMB_WIDTH_PX, THUMB_WIDTH_PX)
    val image = generator.copyCGImageAtTime(
        CMTimeMakeWithSeconds(THUMB_AT_SECONDS, THUMB_TIMESCALE),
        actualTime = null,
        error = null,
    ) ?: return
    val jpeg = UIImageJPEGRepresentation(UIImage.imageWithCGImage(image), THUMB_QUALITY)
    CGImageRelease(image)
    jpeg?.writeToFile(thumbPath, atomically = true)
}
