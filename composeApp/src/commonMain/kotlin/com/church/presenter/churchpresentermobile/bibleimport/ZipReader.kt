package com.church.presenter.churchpresentermobile.bibleimport

import com.church.presenter.churchpresentermobile.library.StoredZip

/** One file inside a zip, located by its central-directory record but not yet extracted. */
internal class ZipEntry internal constructor(
    val name: String,
    val compressedSize: Int,
    val size: Int,
    internal val method: Int,
    internal val crc: Int,
    internal val localHeaderOffset: Int,
) {
    /** The part of [name] after the last slash — archives are matched on it, never on paths. */
    val baseName: String get() = name.substringAfterLast('/')
}

/**
 * Reads the zips eBible and the Zefania archive publish: stored or deflated entries, no
 * encryption, no zip64 — a Bible zip is a few megabytes and a handful of entries.
 *
 * Entries are only ever read into memory, by base name, and never written to a path taken from
 * the archive, so a hostile entry name ("../../x") has nowhere to go. The limits below are the
 * desktop's: 64 entries and 256 MB extracted, which no Bible comes near and a zip bomb does.
 */
internal class ZipReader(private val bytes: ByteArray) {

    val entries: List<ZipEntry> = readCentralDirectory()

    /** The entry's bytes, checked against its CRC. */
    fun read(entry: ZipEntry): ByteArray {
        if (int(entry.localHeaderOffset) != LOCAL_HEADER) corrupt("bad local header for ${entry.name}")
        val nameLength = short(entry.localHeaderOffset + LOCAL_NAME_LENGTH)
        val extraLength = short(entry.localHeaderOffset + LOCAL_EXTRA_LENGTH)
        val dataStart = entry.localHeaderOffset + LOCAL_HEADER_SIZE + nameLength + extraLength
        if (dataStart + entry.compressedSize > bytes.size) corrupt("${entry.name} ends early")
        val data = when (entry.method) {
            METHOD_STORED -> bytes.copyOfRange(dataStart, dataStart + entry.compressedSize)
            METHOD_DEFLATED -> Inflater.inflate(bytes, dataStart, entry.compressedSize, entry.size, MAX_EXTRACTED_BYTES)
            else -> corrupt("${entry.name} uses compression method ${entry.method}")
        }
        if (data.size != entry.size || StoredZip.crc32(data) != entry.crc) {
            corrupt("${entry.name} failed its checksum")
        }
        return data
    }

    private fun readCentralDirectory(): List<ZipEntry> {
        val endRecord = findEndRecord()
        val count = short(endRecord + END_ENTRY_COUNT)
        if (count > MAX_ENTRIES) corrupt("$count entries is more than a Bible needs")
        var offset = int(endRecord + END_DIRECTORY_OFFSET)
        var totalSize = 0L
        return List(count) {
            if (offset + CENTRAL_HEADER_SIZE > bytes.size || int(offset) != CENTRAL_HEADER) {
                corrupt("bad central directory")
            }
            val nameLength = short(offset + CENTRAL_NAME_LENGTH)
            val entry = ZipEntry(
                name = bytes.decodeToString(offset + CENTRAL_HEADER_SIZE, offset + CENTRAL_HEADER_SIZE + nameLength),
                compressedSize = int(offset + CENTRAL_COMPRESSED_SIZE),
                size = int(offset + CENTRAL_SIZE),
                method = short(offset + CENTRAL_METHOD),
                crc = int(offset + CENTRAL_CRC),
                localHeaderOffset = int(offset + CENTRAL_LOCAL_OFFSET),
            )
            // A negative size is a value past 2 GB read as signed — zip64 territory, not a Bible.
            if (entry.size < 0 || entry.compressedSize < 0) corrupt("${entry.name} is too large")
            totalSize += entry.size
            if (totalSize > MAX_EXTRACTED_BYTES) corrupt("archive expands past its limit")
            offset += CENTRAL_HEADER_SIZE + nameLength +
                short(offset + CENTRAL_EXTRA_LENGTH) + short(offset + CENTRAL_COMMENT_LENGTH)
            entry
        }
    }

    /** The end-of-central-directory record, searched for backwards past any archive comment. */
    private fun findEndRecord(): Int {
        val lowest = maxOf(0, bytes.size - END_RECORD_SIZE - MAX_COMMENT)
        var index = bytes.size - END_RECORD_SIZE
        while (index >= lowest) {
            if (int(index) == END_RECORD) return index
            index--
        }
        corrupt("not a zip archive")
    }

    private fun short(at: Int): Int =
        (bytes[at].toInt() and BYTE_MASK) or ((bytes[at + 1].toInt() and BYTE_MASK) shl BITS_PER_BYTE)

    private fun int(at: Int): Int = short(at) or (short(at + 2) shl (BITS_PER_BYTE * 2))

    private companion object {
        const val LOCAL_HEADER = 0x04034b50
        const val CENTRAL_HEADER = 0x02014b50
        const val END_RECORD = 0x06054b50
        const val LOCAL_HEADER_SIZE = 30
        const val LOCAL_NAME_LENGTH = 26
        const val LOCAL_EXTRA_LENGTH = 28
        const val CENTRAL_HEADER_SIZE = 46
        const val CENTRAL_METHOD = 10
        const val CENTRAL_CRC = 16
        const val CENTRAL_COMPRESSED_SIZE = 20
        const val CENTRAL_SIZE = 24
        const val CENTRAL_NAME_LENGTH = 28
        const val CENTRAL_EXTRA_LENGTH = 30
        const val CENTRAL_COMMENT_LENGTH = 32
        const val CENTRAL_LOCAL_OFFSET = 42
        const val END_RECORD_SIZE = 22
        const val END_ENTRY_COUNT = 10
        const val END_DIRECTORY_OFFSET = 16
        const val MAX_COMMENT = 0xFFFF
        const val METHOD_STORED = 0
        const val METHOD_DEFLATED = 8
        const val MAX_ENTRIES = 64
        const val MAX_EXTRACTED_BYTES = 256 * 1024 * 1024
        const val BYTE_MASK = 0xFF
        const val BITS_PER_BYTE = 8
    }
}
