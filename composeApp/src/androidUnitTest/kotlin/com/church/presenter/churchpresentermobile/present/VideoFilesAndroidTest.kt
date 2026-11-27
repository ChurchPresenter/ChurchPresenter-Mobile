package com.church.presenter.churchpresentermobile.present

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What Android reads out of a video at import: its length, its shape and a still. */
class VideoFilesAndroidTest {

    private val dir: File = Files.createTempDirectory("probe").toFile()

    @AfterTest
    fun cleanUp() {
        unmockkAll()
        dir.deleteRecursively()
    }

    private fun retrieverReturns(duration: String?, width: String?, height: String?, rotation: String?, frame: Bitmap?) {
        mockkConstructor(MediaMetadataRetriever::class)
        every { anyConstructed<MediaMetadataRetriever>().setDataSource(any<String>()) } just runs
        every { anyConstructed<MediaMetadataRetriever>().release() } just runs
        every {
            anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        } returns duration
        every {
            anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        } returns width
        every {
            anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        } returns height
        every {
            anyConstructed<MediaMetadataRetriever>().extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        } returns rotation
        every { anyConstructed<MediaMetadataRetriever>().getFrameAtTime(any(), any()) } returns frame
    }

    @Test
    fun `a landscape clip reports its length and size`() = runBlocking {
        retrieverReturns("180000", "1920", "1080", "0", frame = null)

        val probe = probeVideoFile("/v.mp4", File(dir, "v.jpg").path)

        assertEquals(VideoProbe(180_000L, 1920, 1080), probe)
    }

    @Test
    fun `a clip shot upright reports the shape the room will see`() = runBlocking {
        // Stored as landscape pixels plus a quarter turn.
        retrieverReturns("30000", "1920", "1080", "90", frame = null)

        assertEquals(VideoProbe(30_000L, 1080, 1920), probeVideoFile("/v.mp4", File(dir, "v.jpg").path))
    }

    @Test
    fun `missing details read as zero rather than failing the import`() = runBlocking {
        retrieverReturns(null, null, "abc", null, frame = null)

        assertEquals(VideoProbe(0L, 0, 0), probeVideoFile("/v.mp4", File(dir, "v.jpg").path))
    }

    @Test
    fun `a large frame is shrunk before it is saved as the still`() = runBlocking {
        val frame = mockk<Bitmap>(relaxed = true) {
            every { width } returns 1920
            every { height } returns 1080
        }
        val scaled = mockk<Bitmap>(relaxed = true)
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(frame, 480, 270, true) } returns scaled
        retrieverReturns("1000", "1920", "1080", "0", frame = frame)
        val thumb = File(dir, "v.jpg")

        probeVideoFile("/v.mp4", thumb.path)

        verify { scaled.compress(Bitmap.CompressFormat.JPEG, any(), any()) }
        verify { scaled.recycle() }
        verify { frame.recycle() }
    }

    @Test
    fun `a small frame is saved as it is`() = runBlocking {
        val frame = mockk<Bitmap>(relaxed = true) {
            every { width } returns 320
            every { height } returns 180
        }
        retrieverReturns("1000", "320", "180", "0", frame = frame)

        probeVideoFile("/v.mp4", File(dir, "v.jpg").path)

        verify { frame.compress(Bitmap.CompressFormat.JPEG, any(), any()) }
    }

    @Test
    fun `a range is read from where it is asked`() {
        val file = File(dir, "clip.mp4").apply { writeBytes(ByteArray(100) { it.toByte() }) }

        assertEquals((40 until 50).map { it.toByte() }, readFileRange(file.path, 40L, 10)?.toList())
        assertEquals(0, readFileRange(file.path, 100L, 10)?.size)
        assertNull(readFileRange(File(dir, "missing").path, 0L, 10))
    }

    @Test
    fun `with no app context there is nowhere to keep videos`() {
        assertNull(videoStorageDirectory())
        assertNull(createVideoFiles())
    }
}
