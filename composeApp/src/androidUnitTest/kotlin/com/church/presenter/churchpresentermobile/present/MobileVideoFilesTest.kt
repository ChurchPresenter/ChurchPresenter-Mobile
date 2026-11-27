package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Videos copied into, read from and removed from a real folder. */
class MobileVideoFilesTest {

    private val root: File = Files.createTempDirectory("videos").toFile()
    private val files = MobileVideoFiles(root.absolutePath)

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun picked(bytes: ByteArray) = PickedMediaFile("clip.mp4", bytes.size.toLong()) { channel, onProgress ->
        // In two writes, the way a picker streams a large file.
        val half = bytes.size / 2
        channel.writeFully(bytes, 0, half)
        onProgress(half.toLong())
        channel.writeFully(bytes, half, bytes.size)
        onProgress(bytes.size.toLong())
    }

    @Test
    fun `a picked video is copied in whole and reports its progress`() = runBlocking {
        val bytes = ByteArray(700_000) { (it % 97).toByte() }
        val progress = mutableListOf<Long>()

        val written = files.importFrom(picked(bytes), "v1.mp4") { progress += it }

        assertEquals(bytes.size.toLong(), written)
        assertEquals(bytes.size.toLong(), files.size("v1.mp4"))
        assertEquals(bytes.size.toLong(), progress.last())
        assertTrue(File(files.pathOf("v1.mp4")).readBytes().contentEquals(bytes))
        assertFalse(File(root, "v1.mp4.part").exists(), "the partial file is renamed, not left behind")
    }

    @Test
    fun `a copy that fails part-way leaves nothing behind`() {
        val failing = PickedMediaFile("clip.mp4", 10L) { channel, _ ->
            channel.writeFully(ByteArray(5))
            error("the picker lost the file")
        }

        assertFailsWith<IllegalStateException> { runBlocking { files.importFrom(failing, "v1.mp4") {} } }
        assertEquals(0L, files.size("v1.mp4"))
        assertTrue(root.listFiles().orEmpty().isEmpty(), "no partial file and no final file")
    }

    @Test
    fun `a range is read from the middle of the file without reading what comes before`() = runBlocking {
        val bytes = ByteArray(1_000) { it.toByte() }
        files.importFrom(picked(bytes), "v1.mp4") {}

        assertEquals(bytes.copyOfRange(500, 510).toList(), files.read("v1.mp4", 500L, 10)?.toList())
        assertEquals(bytes.copyOfRange(995, 1_000).toList(), files.read("v1.mp4", 995L, 64)?.toList(), "short at the end")
        assertEquals(0, files.read("v1.mp4", 1_000L, 10)?.size, "nothing past the end")
        assertNull(files.read("missing.mp4", 0L, 10))
    }

    @Test
    fun `a deleted video is gone and deleting it again is harmless`() = runBlocking {
        files.importFrom(picked(ByteArray(10)), "v1.mp4") {}

        files.delete("v1.mp4")
        files.delete("v1.mp4")

        assertEquals(0L, files.size("v1.mp4"))
    }

    @Test
    fun `probing a file that is not a video does not throw`() = runBlocking {
        files.importFrom(picked(ByteArray(10)), "v1.mp4") {}
        // The stub framework has no decoder; the point is that a bad file is a
        // video with no details, never a crash mid-import.
        files.probe("v1.mp4", "v1.jpg")
        Unit
    }
}
