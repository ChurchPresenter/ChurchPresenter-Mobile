package com.church.presenter.churchpresentermobile.tv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The edges of what a TV accepts: a frame header off the wire and an address typed
 * with a remote. A bad one must be refused here, not drawn or dialled.
 */
class TvInputBoundsTest {

    /** A header of [numbers] with one byte of picture after it. */
    private fun message(vararg numbers: Int): ByteArray =
        numbers.flatMap { n -> listOf(n shr 24, n shr 16, n shr 8, n).map(Int::toByte) }.toByteArray() +
            byteArrayOf(1)

    @Test
    fun aFrameInTheFarCornerFitsExactly() {
        assertNotNull(parseFrameHeader(message(1820, 980, 100, 100, 1920, 1080)))
    }

    @Test
    fun aFrameFromAnOutputOfNoHeightIsRefused() {
        assertNull(parseFrameHeader(message(0, 0, 10, 10, 1920, 0)))
    }

    @Test
    fun aRectangleOfNoWidthIsRefused() {
        assertNull(parseFrameHeader(message(0, 0, 0, 10, 1920, 1080)))
    }

    @Test
    fun aRectangleOfNoHeightIsRefused() {
        assertNull(parseFrameHeader(message(0, 0, 10, 0, 1920, 1080)))
    }

    @Test
    fun aRectangleLeftOfTheOutputIsRefused() {
        assertNull(parseFrameHeader(message(-1, 0, 10, 10, 1920, 1080)))
    }

    @Test
    fun aRectangleAboveTheOutputIsRefused() {
        assertNull(parseFrameHeader(message(0, -1, 10, 10, 1920, 1080)))
    }

    @Test
    fun aRectangleBelowTheOutputIsRefused() {
        assertNull(parseFrameHeader(message(0, 1000, 10, 100, 1920, 1080)))
    }

    @Test
    fun aPortOutOfRangeIsRefused() {
        assertNull(parseAddress("192.168.1.20:70000"))
    }

    @Test
    fun aHostWithASpaceInItIsRefused() {
        assertNull(parseAddress("main hall:8766"))
    }

    @Test
    fun anAddressWithNoHostIsRefused() {
        assertNull(parseAddress(":8766"))
    }

    @Test
    fun aTypedAddressStartsOnTheFirstOutputWithNoKey() {
        val source = assertNotNull(parseAddress("192.168.1.20:8766"))

        assertEquals(1, source.output)
        assertEquals("", source.apiKey)
    }
}
