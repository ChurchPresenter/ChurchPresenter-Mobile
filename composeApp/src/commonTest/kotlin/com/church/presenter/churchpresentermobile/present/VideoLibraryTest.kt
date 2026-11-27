package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Files in a map; enough to exercise every path the library takes through [VideoFiles]. */
internal class FakeVideoFiles(private val probe: VideoProbe? = VideoProbe(180_000L, 1920, 1080)) : VideoFiles {
    val files = mutableMapOf<String, ByteArray>()
    var failImport = false

    override suspend fun importFrom(file: PickedMediaFile, name: String, onProgress: (Long) -> Unit): Long {
        check(!failImport) { "disk full" }
        val bytes = file.fileName.encodeToByteArray() + ByteArray(file.sizeBytes.toInt())
        files[name] = bytes
        onProgress(bytes.size.toLong())
        return bytes.size.toLong()
    }

    override suspend fun probe(name: String, thumbName: String): VideoProbe? {
        if (probe != null) files[thumbName] = byteArrayOf(1, 2, 3)
        return probe
    }

    override fun pathOf(name: String): String = "/data/videos/$name"

    override fun size(name: String): Long = files[name]?.size?.toLong() ?: 0L

    override fun read(name: String, offset: Long, length: Int): ByteArray? {
        val bytes = files[name] ?: return null
        val start = offset.toInt().coerceAtMost(bytes.size)
        return bytes.copyOfRange(start, (start + length).coerceAtMost(bytes.size))
    }

    override fun delete(name: String) {
        files.remove(name)
    }
}

class VideoLibraryTest {

    private var ids = 0
    private val files = FakeVideoFiles()
    private val store = InMemoryFileStorage()
    private fun library(backing: VideoFiles? = files) =
        VideoLibrary(backing, store, newId = { "id${++ids}" }, now = { 42L })

    private fun picked(name: String, size: Int = 10) = PickedMediaFile(name, size.toLong()) { channel, _ ->
        channel.writeFully(ByteArray(size))
    }

    @Test
    fun `an imported video is listed with what was read from it`() = runTest {
        val lib = library()
        val video = lib.import(picked("Welcome_video.mp4")).getOrThrow()

        assertEquals("Welcome video", video.title)
        assertEquals("id1.mp4", video.fileName)
        assertEquals(180_000L, video.durationMs)
        assertEquals(1920, video.width)
        assertEquals("id1.jpg", video.thumbName)
        assertEquals(42L, video.addedAt)
        assertEquals(listOf(video), lib.videos.value)
    }

    @Test
    fun `the newest video is listed first`() = runTest {
        val lib = library()
        lib.import(picked("a.mp4"))
        lib.import(picked("b.mov"))
        assertEquals(listOf("b", "a"), lib.videos.value.map { it.title })
    }

    @Test
    fun `a file the hall screen cannot play is refused before anything is copied`() = runTest {
        val lib = library()
        assertTrue(lib.import(picked("song.mp3")).isFailure)
        assertTrue(files.files.isEmpty())
        assertTrue(lib.videos.value.isEmpty())
    }

    @Test
    fun `a failed copy leaves nothing behind`() = runTest {
        files.failImport = true
        val lib = library()
        assertTrue(lib.import(picked("a.mp4")).isFailure)
        assertTrue(lib.videos.value.isEmpty())
        assertTrue(files.files.isEmpty())
    }

    @Test
    fun `a video with no still is still listed`() = runTest {
        val lib = library(FakeVideoFiles(probe = null))
        val video = lib.import(picked("a.mp4")).getOrThrow()
        assertNull(video.thumbName)
        assertEquals(0L, video.durationMs)
    }

    @Test
    fun `the list survives a restart`() = runTest {
        library().import(picked("a.mp4"))
        val reopened = library().also { it.load() }
        assertEquals(listOf("a"), reopened.videos.value.map { it.title })
    }

    @Test
    fun `a video whose file has gone is dropped on load`() = runTest {
        val first = library()
        val video = first.import(picked("a.mp4")).getOrThrow()
        files.delete(video.fileName)
        val reopened = library().also { it.load() }
        assertTrue(reopened.videos.value.isEmpty())
    }

    @Test
    fun `removing a video deletes its file and its still`() = runTest {
        val lib = library()
        val video = lib.import(picked("a.mp4")).getOrThrow()
        lib.remove(video.id)
        assertTrue(lib.videos.value.isEmpty())
        assertTrue(files.files.isEmpty())
    }

    @Test
    fun `renaming ignores a blank title`() = runTest {
        val lib = library()
        val video = lib.import(picked("a.mp4")).getOrThrow()
        lib.rename(video.id, "  ")
        assertEquals("a", lib.videos.value.single().title)
        lib.rename(video.id, " Countdown ")
        assertEquals("Countdown", lib.videos.value.single().title)
    }

    @Test
    fun `a video plays from the server once it runs, and from the file before`() = runTest {
        val lib = library()
        val video = lib.import(picked("a.mp4")).getOrThrow()
        assertEquals("file:///data/videos/id1.mp4", lib.playbackUrl(video))
        lib.serveFrom("http://10.0.0.2:8766/")
        assertEquals("http://10.0.0.2:8766/video/id1.mp4", lib.playbackUrl(video))
    }

    @Test
    fun `the server is given ranges of a listed video and nothing else`() = runTest {
        val lib = library()
        val video = lib.import(picked("a.mp4", size = 4)).getOrThrow()
        val served = assertNotNull(lib.source.video(video.fileName))
        assertEquals("video/mp4", served.contentType)
        assertEquals(files.size(video.fileName), served.size)
        assertEquals(2, served.read(0L, 2)?.size)
        assertNotNull(lib.source.video("id1.jpg"), "the still is served too")
        assertNull(lib.source.video("other.mp4"))
    }

    @Test
    fun `a platform with no storage says so and imports nothing`() = runTest {
        val lib = library(backing = null)
        assertFalse(lib.isSupported)
        assertTrue(lib.import(picked("a.mp4")).isFailure)
        assertNull(lib.source.video("id1.mp4"))
    }

    @Test
    fun `the phone's own players find the file behind a served address`() = runTest {
        val lib = library()
        val video = lib.import(picked("a.mp4")).getOrThrow()
        assertEquals("file:///data/videos/id1.mp4", lib.localFileUrl("http://10.0.0.2:8766/video/id1.mp4"))
        assertEquals("file:///data/videos/id1.mp4", lib.localFileUrl("http://10.0.0.2:8766/video/id1.mp4?v=2"))
        assertNull(lib.localFileUrl("http://10.0.0.2:8766/video/other.mp4"), "not one of ours")
        assertNull(lib.localFileUrl("https://example.com/clip.mp4"))
        assertEquals(video.fileName, "id1.mp4")
    }

    @Test
    fun `an address with no local copy plays as it came`() {
        val previous = LocalVideoFiles.resolve
        try {
            LocalVideoFiles.resolve = { url -> if (url.endsWith("mine.mp4")) "file:///v/mine.mp4" else null }
            assertEquals("file:///v/mine.mp4", LocalVideoFiles.playable("http://phone/video/mine.mp4"))
            assertEquals("http://phone/video/theirs.mp4", LocalVideoFiles.playable("http://phone/video/theirs.mp4"))
        } finally {
            LocalVideoFiles.resolve = previous
        }
    }

    @Test
    fun `a corrupt saved list opens empty rather than crashing`() {
        store.write(VIDEO_INDEX_FILE, "{not json")
        val lib = library().also { it.load() }
        assertTrue(lib.videos.value.isEmpty())
    }

    @Test
    fun `an empty file is refused and leaves nothing behind`() = runTest {
        val empty = object : VideoFiles by files {
            override suspend fun importFrom(file: PickedMediaFile, name: String, onProgress: (Long) -> Unit) = 0L
        }
        val lib = library(empty)
        assertTrue(lib.import(picked("a.mp4")).isFailure)
        assertTrue(lib.videos.value.isEmpty())
    }

    @Test
    fun `renaming or removing an unknown video changes nothing`() = runTest {
        val lib = library()
        val video = lib.import(picked("a.mp4")).getOrThrow()
        lib.rename("nope", "Other")
        lib.remove("nope")
        assertEquals(listOf(video), lib.videos.value)
    }

    @Test
    fun `with nowhere to keep videos there is no address and no still`() {
        val lib = library(backing = null)
        val video = LocalVideo("x", "x", "x.mp4", thumbName = "x.jpg")
        assertNull(lib.playbackUrl(video))
        assertNull(lib.thumbnailPath(video))
        assertNull(lib.localFileUrl("http://phone/video/x.mp4"))
    }

    @Test
    fun `a still is found for the list, and a video without one has none`() = runTest {
        val lib = library()
        val video = lib.import(picked("a.mp4")).getOrThrow()
        assertEquals("/data/videos/id1.jpg", lib.thumbnailPath(video))
        assertNull(lib.thumbnailPath(video.copy(thumbName = null)))
    }

    @Test
    fun `a listed video whose file vanished is not served`() = runTest {
        val lib = library()
        val video = lib.import(picked("a.mp4")).getOrThrow()
        files.delete(video.fileName)
        assertNull(lib.source.video(video.fileName))
    }

    @Test
    fun `a title is the file's name without its extension`() {
        assertEquals("Welcome video", VideoLibrary.titleFrom("Welcome_video.mp4"))
        assertEquals(".mp4", VideoLibrary.titleFrom(".mp4"))
        assertEquals("1.2 GB", VideoLibrary.titleFrom("1.2 GB.mov"))
    }

    @Test
    fun `the total size adds up every video`() = runTest {
        val lib = library()
        lib.import(picked("a.mp4", size = 10))
        lib.import(picked("b.mp4", size = 20))
        assertEquals(lib.videos.value.sumOf { it.sizeBytes }, lib.totalBytes)
    }

    @Test
    fun `content types follow the extension`() {
        assertEquals("video/quicktime", VideoLibrary.contentTypeFor("a.MOV"))
        assertEquals("video/webm", VideoLibrary.contentTypeFor("a.webm"))
        assertEquals("image/jpeg", VideoLibrary.contentTypeFor("a.jpg"))
        assertEquals("video/mp4", VideoLibrary.contentTypeFor("a.mp4"))
    }
}
