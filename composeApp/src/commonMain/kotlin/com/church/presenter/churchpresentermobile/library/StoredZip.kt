package com.church.presenter.churchpresentermobile.library

private const val ZIP_LOCAL_HEADER = 0x04034b50
private const val ZIP_CENTRAL_HEADER = 0x02014b50
private const val ZIP_END_OF_CENTRAL_DIR = 0x06054b50

/** Bit 11 of the general-purpose flags: names are UTF-8. */
private const val ZIP_FLAG_UTF8 = 0x0800

/** "Version needed to extract" — 2.0, the floor for a stored entry. */
private const val ZIP_VERSION = 20
private const val ZIP_METHOD_STORE = 0

/** A fixed DOS date/time (1980-01-01 00:00): Excel does not read it, and a real one adds nothing. */
private const val ZIP_DOS_TIME = 0
private const val ZIP_DOS_DATE = 0x0021

private const val CRC_POLYNOMIAL = 0xEDB88320.toInt()
private const val BYTE_MASK = 0xFF
private const val BITS_PER_BYTE = 8
private const val SHORT_BYTES = 2
private const val INT_BYTES = 4

/**
 * A zip file that only *stores* its entries, with no library behind it.
 *
 * Enough for an `.xlsx`, whose parts are small XML: the headers, a central
 * directory and a CRC-32 per entry, and nothing compressed. Written for
 * [XlsxWriter], which is the only caller.
 */
internal object StoredZip {

    /** A stored zip of [entries], each a path and its bytes. */
    fun write(entries: List<Pair<String, ByteArray>>): ByteArray {
        val body = ByteSink()
        val central = ByteSink()
        for ((path, data) in entries) {
            val name = path.encodeToByteArray()
            val crc = crc32(data)
            val offset = body.size
            body.int(ZIP_LOCAL_HEADER).short(ZIP_VERSION).short(ZIP_FLAG_UTF8).short(ZIP_METHOD_STORE)
                .short(ZIP_DOS_TIME).short(ZIP_DOS_DATE).int(crc).int(data.size).int(data.size)
                .short(name.size).short(0).bytes(name).bytes(data)
            central.int(ZIP_CENTRAL_HEADER).short(ZIP_VERSION).short(ZIP_VERSION).short(ZIP_FLAG_UTF8)
                .short(ZIP_METHOD_STORE).short(ZIP_DOS_TIME).short(ZIP_DOS_DATE).int(crc).int(data.size)
                .int(data.size).short(name.size).short(0).short(0).short(0).short(0).int(0).int(offset)
                .bytes(name)
        }
        val centralOffset = body.size
        body.bytes(central.toByteArray())
        body.int(ZIP_END_OF_CENTRAL_DIR).short(0).short(0).short(entries.size).short(entries.size)
            .int(central.size).int(centralOffset).short(0)
        return body.toByteArray()
    }

    private val crcTable: IntArray = IntArray(BYTE_MASK + 1) { n ->
        var c = n
        repeat(BITS_PER_BYTE) { c = if (c and 1 != 0) CRC_POLYNOMIAL xor (c ushr 1) else c ushr 1 }
        c
    }

    /** The CRC-32 of [bytes], as every zip reader checks it. */
    fun crc32(bytes: ByteArray): Int {
        var crc = -1
        for (b in bytes) crc = crcTable[(crc xor b.toInt()) and BYTE_MASK] xor (crc ushr BITS_PER_BYTE)
        return crc.inv()
    }

    /** A growable byte buffer writing little-endian fields, which is all a zip is. */
    private class ByteSink {
        private var buffer = ByteArray(INITIAL_CAPACITY)
        var size = 0
            private set

        fun short(value: Int): ByteSink = put(value, SHORT_BYTES)
        fun int(value: Int): ByteSink = put(value, INT_BYTES)

        fun bytes(data: ByteArray): ByteSink {
            ensure(data.size)
            data.copyInto(buffer, size)
            size += data.size
            return this
        }

        private fun put(value: Int, count: Int): ByteSink {
            ensure(count)
            for (i in 0 until count) buffer[size + i] = (value ushr (i * BITS_PER_BYTE) and BYTE_MASK).toByte()
            size += count
            return this
        }

        private fun ensure(extra: Int) {
            if (size + extra > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
        }

        fun toByteArray(): ByteArray = buffer.copyOf(size)

        private companion object {
            const val INITIAL_CAPACITY = 4096
        }
    }
}
