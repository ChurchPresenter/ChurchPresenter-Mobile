package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

private const val TAG = "VideoFiles"

/** Bytes moved per read while copying a picked video in. */
private const val COPY_CHUNK_BYTES = 256 * 1024

actual fun createVideoFiles(): VideoFiles? = videoStorageDirectory()?.let(::MobileVideoFiles)

/**
 * Videos as files in the app's private storage, shared by Android and iOS.
 *
 * Copying and deleting go through kotlinx-io, which both platforms implement the
 * same way. Reading a range does not: kotlinx-io can only skip forward by
 * reading, so a TV seeking to the last minute of a 1 GB file would read the
 * first 990 MB to get there. Each platform seeks natively instead.
 *
 * `internal` rather than private so its copy, range and cleanup paths can be
 * tested against a real temporary folder.
 */
internal class MobileVideoFiles(private val root: String) : VideoFiles {

    private fun path(name: String) = Path(root, name)

    override suspend fun importFrom(file: PickedMediaFile, name: String, onProgress: (Long) -> Unit): Long =
        withContext(Dispatchers.Default) {
            val partial = Path(root, "$name.part")
            runCatching { copyInto(file, partial, onProgress) }
                .onSuccess { SystemFileSystem.atomicMove(partial, path(name)) }
                .onFailure { SystemFileSystem.delete(partial, mustExist = false) }
                .getOrThrow()
        }

    /**
     * Drains the picked file into [target]. The picker writes into a channel (it
     * was built for uploads); reading that channel into a file keeps the import
     * streaming, so a large video is never held in memory.
     */
    private suspend fun copyInto(file: PickedMediaFile, target: Path, onProgress: (Long) -> Unit): Long {
        var written = 0L
        SystemFileSystem.sink(target).buffered().use { sink ->
            coroutineScope {
                val channel = ByteChannel()
                launch {
                    try {
                        file.streamTo(channel) { }
                    } finally {
                        channel.flushAndClose()
                    }
                }
                val buffer = ByteArray(COPY_CHUNK_BYTES)
                var read = channel.readAvailable(buffer, 0, buffer.size)
                while (read >= 0) {
                    if (read > 0) {
                        sink.write(buffer, 0, read)
                        written += read
                        onProgress(written)
                    }
                    read = channel.readAvailable(buffer, 0, buffer.size)
                }
            }
        }
        return written
    }

    override suspend fun probe(name: String, thumbName: String): VideoProbe? =
        runCatching { probeVideoFile(pathOf(name), pathOf(thumbName)) }
            .onFailure { Logger.e(TAG, "probe '$name' failed: ${it.message}") }
            .getOrNull()

    override fun pathOf(name: String): String = path(name).toString()

    override fun size(name: String): Long =
        runCatching { SystemFileSystem.metadataOrNull(path(name))?.size ?: 0L }.getOrDefault(0L)

    override fun read(name: String, offset: Long, length: Int): ByteArray? =
        runCatching { readFileRange(pathOf(name), offset, length) }
            .onFailure { Logger.e(TAG, "read '$name' @ $offset failed: ${it.message}") }
            .getOrNull()

    override fun delete(name: String) {
        runCatching { SystemFileSystem.delete(path(name), mustExist = false) }
            .onFailure { Logger.e(TAG, "delete '$name' failed: ${it.message}") }
    }
}
