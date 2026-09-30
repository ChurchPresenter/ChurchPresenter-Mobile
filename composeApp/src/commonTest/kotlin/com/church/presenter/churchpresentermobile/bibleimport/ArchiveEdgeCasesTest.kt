package com.church.presenter.churchpresentermobile.bibleimport

import com.church.presenter.churchpresentermobile.library.StoredZip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Damaged downloads: every way a zip or a deflate stream can be wrong is refused, not misread. */
class ArchiveEdgeCasesTest {

    private val zip = StoredZip.write(
        listOf("a.xml" to "<a/>".encodeToByteArray(), "b.xml" to "<b/>".encodeToByteArray()),
    )

    private fun inflate(vararg bytes: Int, max: Int = 1_000) =
        bytes.map { it.toByte() }.toByteArray().let { Inflater.inflate(it, 0, it.size, 0, max) }

    /** Index of the first occurrence of a little-endian [signature] in [bytes]. */
    private fun indexOf(bytes: ByteArray, signature: Int): Int = (0..bytes.size - 4).first { at ->
        (0 until 4).all { bytes[at + it] == ((signature shr (8 * it)) and 0xFF).toByte() }
    }

    @Test
    fun `a stored zip written by the app reads back`() {
        val reader = ZipReader(zip)
        assertEquals("<b/>", reader.read(reader.entries[1]).decodeToString())
    }

    @Test
    fun `an entry whose local header is not where the directory says is refused`() {
        val broken = zip.copyOf().also { it[0] = 0 }
        val reader = ZipReader(broken)
        assertFailsWith<CorruptArchiveException> { reader.read(reader.entries[0]) }
    }

    @Test
    fun `an unsupported compression method is refused by name`() {
        val central = indexOf(zip, 0x02014b50)
        val broken = zip.copyOf().also { it[central + 10] = 12 }
        val reader = ZipReader(broken)
        val error = assertFailsWith<CorruptArchiveException> { reader.read(reader.entries[0]) }
        assertEquals(true, error.message?.contains("method 12"))
    }

    @Test
    fun `a directory claiming too many entries is refused`() {
        val end = indexOf(zip, 0x06054b50)
        val broken = zip.copyOf().also { it[end + 10] = 100 }
        assertFailsWith<CorruptArchiveException> { ZipReader(broken) }
    }

    @Test
    fun `a directory that does not start with a header is refused`() {
        val central = indexOf(zip, 0x02014b50)
        val broken = zip.copyOf().also { it[central] = 0 }
        assertFailsWith<CorruptArchiveException> { ZipReader(broken) }
    }

    @Test
    fun `an entry claiming more than two gigabytes is refused`() {
        val central = indexOf(zip, 0x02014b50)
        val broken = zip.copyOf().also { it[central + 27] = 0xFF.toByte() }
        assertFailsWith<CorruptArchiveException> { ZipReader(broken) }
    }

    @Test
    fun `an entry running past the end of the file is refused`() {
        val central = indexOf(zip, 0x02014b50)
        val broken = zip.copyOf().also { it[central + 21] = 0x7F }
        val reader = ZipReader(broken)
        assertFailsWith<CorruptArchiveException> { reader.read(reader.entries[0]) }
    }

    // ── Deflate ─────────────────────────────────────────────────────────────

    @Test
    fun `the reserved block type is refused`() {
        assertFailsWith<CorruptArchiveException> { inflate(0x07) }
    }

    @Test
    fun `a stored block whose length and complement disagree is refused`() {
        assertFailsWith<CorruptArchiveException> { inflate(0x01, 0x05, 0x00, 0x00, 0x00) }
    }

    @Test
    fun `a stored block shorter than it claims is refused`() {
        assertFailsWith<CorruptArchiveException> { inflate(0x01, 0x05, 0x00, 0xFA, 0xFF, 0x41) }
        assertFailsWith<CorruptArchiveException> { inflate(0x01, 0x05) }
    }

    @Test
    fun `a stored block past the output limit is refused`() {
        assertFailsWith<CorruptArchiveException> { inflate(0x01, 0x03, 0x00, 0xFC, 0xFF, 0x41, 0x42, 0x43, max = 2) }
    }

    @Test
    fun `a dynamic block declaring impossible code counts is refused`() {
        assertFailsWith<CorruptArchiveException> { inflate(0xFD, 0xFF, 0xFF) }
    }

    @Test
    fun `an empty input is refused`() {
        assertFailsWith<CorruptArchiveException> { inflate() }
    }
}
