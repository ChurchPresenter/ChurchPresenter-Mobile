package com.church.presenter.churchpresentermobile.present

import kotlin.test.Test
import kotlin.test.assertEquals

class RangeRequestTest {

    private val size = 1_000L

    @Test
    fun `no header means the whole file`() {
        assertEquals(RangeRequest.Whole, parseByteRange(null, size))
        assertEquals(RangeRequest.Whole, parseByteRange("", size))
    }

    @Test
    fun `a closed range is honoured`() {
        assertEquals(RangeRequest.Part(0, 1), parseByteRange("bytes=0-1", size))
        assertEquals(2L, RangeRequest.Part(0, 1).length)
    }

    @Test
    fun `an open range runs to the end`() {
        assertEquals(RangeRequest.Part(500, 999), parseByteRange("bytes=500-", size))
    }

    @Test
    fun `a suffix asks for the last bytes`() {
        assertEquals(RangeRequest.Part(900, 999), parseByteRange("bytes=-100", size))
        assertEquals(RangeRequest.Part(0, 999), parseByteRange("bytes=-5000", size))
    }

    @Test
    fun `an end past the file is clamped to it`() {
        assertEquals(RangeRequest.Part(10, 999), parseByteRange("bytes=10-99999", size))
    }

    @Test
    fun `a start past the file cannot be satisfied`() {
        assertEquals(RangeRequest.Unsatisfiable, parseByteRange("bytes=1000-", size))
        assertEquals(RangeRequest.Unsatisfiable, parseByteRange("bytes=-0", size))
    }

    @Test
    fun `anything malformed or multi-range falls back to the whole file`() {
        assertEquals(RangeRequest.Whole, parseByteRange("items=0-1", size))
        assertEquals(RangeRequest.Whole, parseByteRange("bytes=abc-", size))
        assertEquals(RangeRequest.Whole, parseByteRange("bytes=5-2", size))
        assertEquals(RangeRequest.Whole, parseByteRange("bytes=0-1,5-6", size))
        assertEquals(RangeRequest.Whole, parseByteRange("bytes=7", size))
    }

    @Test
    fun `numbers too big to read fall back to the whole file`() {
        assertEquals(RangeRequest.Whole, parseByteRange("bytes=99999999999999999999-", size))
        assertEquals(RangeRequest.Whole, parseByteRange("bytes=0-99999999999999999999", size))
        assertEquals(RangeRequest.Whole, parseByteRange("bytes=-99999999999999999999", size))
        assertEquals(RangeRequest.Whole, parseByteRange("bytes=-", size))
    }

    @Test
    fun `an empty file is always sent whole`() {
        assertEquals(RangeRequest.Whole, parseByteRange("bytes=0-1", 0L))
    }

    @Test
    fun `surrounding spaces are ignored`() {
        assertEquals(RangeRequest.Part(0, 9), parseByteRange("  bytes=0-9  ", size))
    }

    @Test
    fun `the unit is case-insensitive`() {
        assertEquals(RangeRequest.Part(0, 9), parseByteRange("Bytes=0-9", size))
    }
}
