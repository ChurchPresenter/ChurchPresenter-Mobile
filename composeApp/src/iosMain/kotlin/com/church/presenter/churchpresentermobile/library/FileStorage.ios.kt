package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

private const val TAG = "FileStorage"
private const val DIRECTORY = "library"

/**
 * Backed by a `library` folder in the app's Documents directory.
 *
 * Excluded from iCloud backup: the library can be large, is reproducible from
 * an export, and silently consuming a user's iCloud quota is a poor trade.
 */
@OptIn(ExperimentalForeignApi::class)
class IosFileStore : FileStore {

    private val manager = NSFileManager.defaultManager

    private val root: String? by lazy {
        runCatching {
            val documents = manager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
                .firstOrNull() as? NSURL ?: return@runCatching null
            val dir = documents.URLByAppendingPathComponent(DIRECTORY) ?: return@runCatching null
            val path = dir.path ?: return@runCatching null
            if (!manager.fileExistsAtPath(path)) {
                manager.createDirectoryAtURL(dir, withIntermediateDirectories = true, attributes = null, error = null)
                dir.setResourceValue(true, forKey = "NSURLIsExcludedFromBackupKey", error = null)
            }
            path
        }.onFailure { Logger.e(TAG, "could not resolve library directory: ${it.message}") }.getOrNull()
    }

    private fun pathFor(name: String): String? = root?.let { "$it/$name" }

    override fun read(name: String): String? = runCatching {
        val path = pathFor(name) ?: return null
        if (!manager.fileExistsAtPath(path)) return null
        NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)
    }.onFailure { Logger.e(TAG, "read '$name' failed: ${it.message}") }.getOrNull()

    override fun write(name: String, text: String) {
        runCatching {
            val path = pathFor(name) ?: return
            // atomically = true is a write-then-rename, so a process killed
            // mid-write leaves the previous good file intact.
            (text as NSString).writeToFile(
                path = path,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
        }.onFailure { Logger.e(TAG, "write '$name' failed: ${it.message}") }
    }

    override fun delete(name: String) {
        runCatching {
            val path = pathFor(name) ?: return
            if (manager.fileExistsAtPath(path)) manager.removeItemAtPath(path, error = null)
        }
    }

    override fun list(): List<String> = runCatching {
        val path = root ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        (manager.contentsOfDirectoryAtPath(path, error = null) as? List<String>).orEmpty()
    }.getOrDefault(emptyList())

    override fun sizeBytes(name: String): Long = runCatching {
        val path = pathFor(name) ?: return 0L
        val attributes = manager.attributesOfItemAtPath(path, error = null) ?: return 0L
        (attributes["NSFileSize"] as? Number)?.toLong() ?: 0L
    }.getOrDefault(0L)
}

actual fun createFileStore(): FileStore = IosFileStore()
