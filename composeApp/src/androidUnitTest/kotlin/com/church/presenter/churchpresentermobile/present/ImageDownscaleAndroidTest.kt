package com.church.presenter.churchpresentermobile.present

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import java.io.OutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Shrinking a photo before it goes over the hall's Wi-Fi.
 *
 * A phone camera hands over something like 4000×3000; no output this app drives
 * is wider than 1920, so without this every byte of the difference is sent and
 * then thrown away by the browser as it scales the image to fit. The arithmetic
 * is the part that can be wrong — a sample size one step too large loses detail
 * that cannot come back, one step too small allocates the memory this exists to
 * avoid — and it is all reachable without a device.
 *
 * `BitmapFactory` and `Bitmap` are final and inert in the unit-test
 * `android.jar`, so they are mocked; everything between them is the real
 * function.
 */
class ImageDownscaleAndroidTest {

    @AfterTest
    fun unmock() = unmockkAll()

    /** A decoded bitmap of [width] × [height] that re-encodes to [encodedSize] bytes. */
    private fun bitmap(width: Int, height: Int, encodedSize: Int = 1_000): Bitmap =
        mockk(relaxed = true) {
            every { this@mockk.width } returns width
            every { this@mockk.height } returns height
            every { compress(any(), any(), any()) } answers {
                thirdArg<OutputStream>().write(ByteArray(encodedSize))
                true
            }
        }

    /**
     * Stands in for the platform decoder: reports [width] × [height] on the
     * header pass, and hands back a bitmap sampled down by `inSampleSize` on the
     * real one.
     */
    private fun decoding(width: Int, height: Int, encodedSize: Int = 1_000) {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } answers {
            val options = arg<BitmapFactory.Options>(3)
            if (options.inJustDecodeBounds) {
                options.outWidth = width
                options.outHeight = height
                null
            } else {
                val sample = options.inSampleSize.coerceAtLeast(1)
                bitmap(width / sample, height / sample, encodedSize)
            }
        }
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } answers {
            bitmap(secondArg(), thirdArg(), encodedSize)
        }
    }

    private val original = ByteArray(5_000_000)

    @Test
    fun `an image already within the limit is sent untouched`() {
        // No re-encode at all: a photo the operator already sized is not worth
        // losing a JPEG generation over.
        decoding(width = 1600, height = 900)

        assertSame(original, downscaleImage(original, maxEdge = 1920))
    }

    @Test
    fun `an image exactly at the limit is sent untouched`() {
        decoding(width = 1920, height = 1080)

        assertSame(original, downscaleImage(original, maxEdge = 1920))
    }

    @Test
    fun `an image that cannot be measured is sent untouched`() {
        // A file that is not an image at all, or a header the decoder rejects.
        // Sending it as-is beats sending nothing.
        decoding(width = 0, height = 0)

        assertSame(original, downscaleImage(original, maxEdge = 1920))
    }

    @Test
    fun `an oversized photo comes back smaller`() {
        decoding(width = 4000, height = 3000, encodedSize = 400_000)

        val result = downscaleImage(original, maxEdge = 1920)

        assertEquals(400_000, result.size)
    }

    @Test
    fun `the longest edge decides, whichever way the photo is turned`() {
        // Portrait: the height is the edge that has to come down to 1920.
        val scaled = slot<Int>()
        decoding(width = 3000, height = 4000, encodedSize = 400_000)
        every { Bitmap.createScaledBitmap(any(), any(), capture(scaled), any()) } answers {
            bitmap(secondArg(), thirdArg(), 400_000)
        }

        downscaleImage(original, maxEdge = 1920)

        assertEquals(1920, scaled.captured)
    }

    @Test
    fun `the aspect ratio is kept`() {
        val width = slot<Int>()
        val height = slot<Int>()
        decoding(width = 4000, height = 3000, encodedSize = 400_000)
        every { Bitmap.createScaledBitmap(any(), capture(width), capture(height), any()) } answers {
            bitmap(secondArg(), thirdArg(), 400_000)
        }

        downscaleImage(original, maxEdge = 1920)

        assertEquals(1920, width.captured)
        assertEquals(1440, height.captured)
    }

    @Test
    fun `sampling halves the decode rather than loading the whole photo`() {
        // inJustDecodeBounds reads the header alone; the real decode then asks
        // for a power-of-two reduction, so a 12-megapixel photo is never fully
        // in memory.
        val sample = slot<BitmapFactory.Options>()
        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } answers {
            bitmap(secondArg(), thirdArg(), 400_000)
        }
        every { BitmapFactory.decodeByteArray(any(), any(), any(), capture(sample)) } answers {
            val options = sample.captured
            if (options.inJustDecodeBounds) {
                options.outWidth = 8000
                options.outHeight = 6000
                null
            } else {
                bitmap(8000 / options.inSampleSize, 6000 / options.inSampleSize, 400_000)
            }
        }

        downscaleImage(original, maxEdge = 1920)

        assertTrue(sample.captured.inSampleSize > 1, "the full photo was decoded")
    }

    @Test
    fun `a photo whose re-encode would be larger is sent as it arrived`() {
        // A small PNG grows when written out as JPEG; keeping the bigger one
        // would make this a pessimisation.
        val tiny = ByteArray(2_000)
        decoding(width = 4000, height = 3000, encodedSize = 900_000)

        assertSame(tiny, downscaleImage(tiny, maxEdge = 1920))
    }

    @Test
    fun `a photo the decoder gives up on is sent as it arrived`() {
        // The header measured, but the full decode returned nothing — a
        // truncated file. Slower than it should be beats a blank slide.
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } answers {
            val options = arg<BitmapFactory.Options>(3)
            if (options.inJustDecodeBounds) {
                options.outWidth = 4000
                options.outHeight = 3000
            }
            null
        }

        assertSame(original, downscaleImage(original, maxEdge = 1920))
    }

    @Test
    fun `a decoder that throws does not lose the photo`() {
        // The whole function is wrapped for this: a photo that cannot be shrunk
        // still has to reach the screen.
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } throws
            OutOfMemoryError("bitmap too large")

        assertSame(original, downscaleImage(original, maxEdge = 1920))
    }

    @Test
    fun `an empty byte array is handled rather than crashing the picker`() {
        decoding(width = 0, height = 0)
        val empty = ByteArray(0)

        assertSame(empty, downscaleImage(empty, maxEdge = 1920))
    }

    @Test
    fun `the default limit is the widest screen this app drives`() {
        // 1920 is a 1080p projector or TV; nothing downstream is larger.
        assertEquals(1920, PHOTO_MAX_EDGE)
    }
}
