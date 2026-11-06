package com.church.presenter.churchpresentermobile.tv

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * The desktop sends only what changed, so the TV's picture is the sum of every
 * rectangle. On the JVM target because it needs a real bitmap, which the JS test
 * runtime does not have.
 */
class FrameCompositorTest {

    private val painted = mutableListOf<FrameHeader>()
    private var created = 0

    private fun compositor(decode: (ByteArray) -> ImageBitmap = { ImageBitmap(1, 1) }) = FrameCompositor(
        decode = decode,
        paint = { _, _, header -> painted += header },
        create = { width, height ->
            created++
            ImageBitmap(width, height)
        },
    )

    private fun message(vararg numbers: Int): ByteArray =
        numbers.flatMap { n -> listOf(n ushr 24, n ushr 16, n ushr 8, n).map { it.toByte() } }.toByteArray() +
            byteArrayOf(1)

    @Test
    fun `each rectangle is painted where its header says`() {
        val pictures = compositor()

        pictures.apply(message(0, 0, 1920, 1080, 1920, 1080))
        pictures.apply(message(100, 200, 50, 60, 1920, 1080))

        assertEquals(listOf(0 to 0, 100 to 200), painted.map { it.x to it.y })
    }

    @Test
    fun `later rectangles land on the same picture`() {
        val pictures = compositor()

        val first = pictures.apply(message(0, 0, 10, 10, 100, 100))
        val second = pictures.apply(message(5, 5, 10, 10, 100, 100))

        assertSame(first, second)
        assertEquals(1, created)
    }

    @Test
    fun `the output changing size starts a new picture`() {
        val pictures = compositor()

        val small = pictures.apply(message(0, 0, 10, 10, 100, 100))
        val large = assertNotNull(pictures.apply(message(0, 0, 10, 10, 200, 100)))

        assertNotSame(small, large)
        assertEquals(200, large.width)
    }

    @Test
    fun `a picture that will not decode is skipped, not fatal`() {
        val pictures = compositor(decode = { error("not an image") })

        assertNull(pictures.apply(message(0, 0, 10, 10, 100, 100)))
    }

    @Test
    fun `a malformed message is skipped`() {
        assertNull(compositor().apply(ByteArray(3)))
    }
}
