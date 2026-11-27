package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.library.FileStore
import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val TAG = "VideoLibrary"

/** Path prefix the phone's own server answers video requests on. */
const val VIDEO_ROUTE = "video"

/** The document listing every imported video, next to the song library. */
internal const val VIDEO_INDEX_FILE = "videos.json"

/** Extensions a phone, a TV and a browser can all be expected to play. */
val VIDEO_IMPORT_EXTENSIONS: Set<String> = setOf("mp4", "m4v", "mov", "webm")

/** Largest video the operator may copy in. A long service video at 1080p, not a film. */
const val MAX_VIDEO_IMPORT_BYTES: Long = 4L * 1024 * 1024 * 1024

/** What a platform could read out of a video file. */
data class VideoProbe(val durationMs: Long, val width: Int, val height: Int)

/**
 * The bytes behind [LocalVideo]s: the platform's file system, cut down to what the
 * library and the server need.
 *
 * An interface so the library can be tested with an in-memory fake; the real one
 * lives in mobileMain, and the targets with nowhere to keep a video (the browser
 * build, the JVM test host) have none at all — [createVideoFiles] returns null and
 * the Media tab says so.
 */
interface VideoFiles {

    /** Copies [file] into storage as [name], returning the bytes written. */
    suspend fun importFrom(file: PickedMediaFile, name: String, onProgress: (Long) -> Unit): Long

    /** Reads the duration and size, and writes a still to [thumbName]. Null when unreadable. */
    suspend fun probe(name: String, thumbName: String): VideoProbe?

    /** The file's absolute path, for a player on this device. */
    fun pathOf(name: String): String

    /** Size in bytes, or 0 when the file is gone. */
    fun size(name: String): Long

    /**
     * Up to [length] bytes from [offset] — what the server hands a TV asking for a
     * range. Null when the file is gone.
     */
    fun read(name: String, offset: Long, length: Int): ByteArray?

    /** Deletes [name] if it exists. */
    fun delete(name: String)
}

/** Creates this platform's [VideoFiles], or null where videos cannot be kept. */
expect fun createVideoFiles(): VideoFiles?

/** A stored video as the server sees it: where to read it from, and what to call it. */
class ServedVideo(
    val size: Long,
    val contentType: String,
    val read: (offset: Long, length: Int) -> ByteArray?,
)

/**
 * Where the presentation server gets video and thumbnail bytes. The video twin of
 * [PhotoSource], kept separate because a video is never held in memory: it is read
 * a range at a time, as a TV's player asks for it.
 */
fun interface VideoSource {
    fun video(path: String): ServedVideo?

    companion object {
        val NONE: VideoSource = VideoSource { null }
    }
}

/**
 * The videos the operator has copied onto this device.
 *
 * Unlike [PhotoLibrary] these persist: a welcome video or a countdown is played
 * every week, and re-picking a 200 MB file each Sunday is the opposite of
 * "plays offline". The files live in app storage; the list of them is a small
 * JSON document in [store].
 *
 * Both renderers reach a video by URL, as they reach a photo, so the browser
 * screen and the in-process one play from the identical address — see
 * [serveFrom].
 */
class VideoLibrary(
    private val files: VideoFiles?,
    private val store: FileStore,
    private val newId: () -> String,
    private val now: () -> Long,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _videos = MutableStateFlow<List<LocalVideo>>(emptyList())

    /** Every imported video, newest first. */
    val videos: StateFlow<List<LocalVideo>> = _videos.asStateFlow()

    private val _baseUrl = MutableStateFlow<String?>(null)

    /** Where videos are served from, or null while the server is down. */
    val baseUrl: StateFlow<String?> = _baseUrl.asStateFlow()

    /** False where the platform has nowhere to keep a video. */
    val isSupported: Boolean get() = files != null

    /** Reads the stored list, dropping entries whose file has gone missing. */
    fun load() {
        val stored = store.read(VIDEO_INDEX_FILE) ?: return
        val decoded = runCatching { json.decodeFromString(ListSerializer(LocalVideo.serializer()), stored) }
            .onFailure { Logger.e(TAG, "could not read the video list: ${it.message}") }
            .getOrDefault(emptyList())
        val present = decoded.filter { (files?.size(it.fileName) ?: 0L) > 0L }
        _videos.value = present
        if (present.size != decoded.size) save()
    }

    /** Tells the library where the server is reachable, or null when it stops. */
    fun serveFrom(baseUrl: String?) {
        _baseUrl.value = baseUrl?.trimEnd('/')
    }

    /**
     * Copies [file] in and lists it.
     *
     * Written to its final name only once the copy and probe succeed, so a phone
     * killed mid-import leaves at worst an orphan file, never a listed video that
     * will not play.
     */
    suspend fun import(file: PickedMediaFile, onProgress: (Long) -> Unit = {}): Result<LocalVideo> {
        val target = files ?: return Result.failure(IllegalStateException("Videos are not supported here"))
        val ext = file.fileName.substringAfterLast('.', "").lowercase()
        if (ext !in VIDEO_IMPORT_EXTENSIONS) {
            return Result.failure(IllegalArgumentException("Unsupported video type: .$ext"))
        }
        val id = newId()
        val fileName = "$id.$ext"
        val thumbName = "$id.jpg"
        return runCatching {
            val written = target.importFrom(file, fileName, onProgress)
            check(written > 0L) { "The video was empty" }
            val probe = target.probe(fileName, thumbName)
            val video = LocalVideo(
                id = id,
                title = titleFrom(file.fileName),
                fileName = fileName,
                sizeBytes = written,
                durationMs = probe?.durationMs ?: 0L,
                width = probe?.width ?: 0,
                height = probe?.height ?: 0,
                thumbName = thumbName.takeIf { target.size(it) > 0L },
                addedAt = now(),
            )
            _videos.value = listOf(video) + _videos.value
            save()
            Logger.d(TAG, "import — ${video.title} ($written bytes, ${video.durationMs} ms)")
            video
        }.onFailure {
            target.delete(fileName)
            target.delete(thumbName)
            Logger.e(TAG, "import failed: ${it.message}", it)
        }
    }

    /** Renames [id]. Blank titles are ignored. */
    fun rename(id: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return
        _videos.value = _videos.value.map { if (it.id == id) it.copy(title = trimmed) else it }
        save()
    }

    /** Deletes [id] and its files. */
    fun remove(id: String) {
        val video = _videos.value.firstOrNull { it.id == id } ?: return
        files?.delete(video.fileName)
        video.thumbName?.let { files?.delete(it) }
        _videos.value = _videos.value.filterNot { it.id == id }
        save()
    }

    /** Total bytes on disk, for the list's header. */
    val totalBytes: Long get() = _videos.value.sumOf { it.sizeBytes }

    /**
     * Where an output should load [video] from: the server's address when it is
     * running, so a TV or browser can reach it, and otherwise the file itself —
     * enough for a screen plugged into this phone, which needs no network.
     */
    fun playbackUrl(video: LocalVideo): String? {
        _baseUrl.value?.let { return "$it/$VIDEO_ROUTE/${video.fileName}" }
        val path = files?.pathOf(video.fileName) ?: return null
        return "file://$path"
    }

    /**
     * The `file://` address of the video behind a served [url], when it is one of
     * this library's — see [LocalVideoFiles].
     */
    fun localFileUrl(url: String): String? {
        val name = url.substringAfter("/$VIDEO_ROUTE/", "").substringBefore('?')
        if (name.isEmpty() || _videos.value.none { it.fileName == name }) return null
        return files?.pathOf(name)?.let { "file://$it" }
    }

    /** The thumbnail's local path, for the list. */
    fun thumbnailPath(video: LocalVideo): String? =
        video.thumbName?.let { name -> files?.pathOf(name) }

    /** This library as something the server can serve from. */
    val source: VideoSource = VideoSource { name ->
        val owned = _videos.value.any { it.fileName == name || it.thumbName == name }
        val backing = files
        if (!owned || backing == null) return@VideoSource null
        val size = backing.size(name)
        if (size <= 0L) return@VideoSource null
        ServedVideo(size = size, contentType = contentTypeFor(name)) { offset, length ->
            backing.read(name, offset, length)
        }
    }

    private fun save() {
        runCatching {
            store.write(VIDEO_INDEX_FILE, json.encodeToString(ListSerializer(LocalVideo.serializer()), _videos.value))
        }.onFailure { Logger.e(TAG, "could not save the video list: ${it.message}") }
    }

    internal companion object {
        /** "Welcome_Video-final.mp4" → "Welcome Video-final". */
        fun titleFrom(fileName: String): String =
            fileName.substringBeforeLast('.').replace('_', ' ').trim().ifBlank { fileName }

        fun contentTypeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "m4v" -> "video/x-m4v"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "video/mp4"
        }
    }
}
