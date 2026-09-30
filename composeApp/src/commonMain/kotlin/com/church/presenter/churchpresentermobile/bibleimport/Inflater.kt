package com.church.presenter.churchpresentermobile.bibleimport

/** Compressed data that does not decode: a truncated download, or not deflate at all. */
internal class CorruptArchiveException(message: String) : IllegalArgumentException(message)

/** Throws [CorruptArchiveException] — the one way the inflater and the zip reader give up. */
internal fun corrupt(message: String): Nothing = throw CorruptArchiveException(message)

/**
 * Raw DEFLATE (RFC 1951) decompression, with nothing platform-specific behind it.
 *
 * eBible and the Zefania archive publish their Bibles as zips. Android and the desktop have
 * `java.util.zip`; iOS would need zlib through cinterop, and a second implementation per platform
 * is two things to get wrong. This is a direct port of Mark Adler's `puff.c` — zlib's reference
 * decoder, written for clarity over speed. A 20 MB Bible decodes in well under a second, once, at
 * install time, which is all it is ever asked to do.
 *
 * @param maxOutput Refuses to grow past this, so a zip bomb fails instead of exhausting memory.
 */
internal class Inflater(
    private val input: ByteArray,
    private var position: Int,
    private val end: Int,
    expectedSize: Int,
    private val maxOutput: Int,
) {
    private var output = ByteArray(expectedSize.coerceIn(MIN_BUFFER, maxOutput))
    private var outSize = 0
    private var bitBuffer = 0
    private var bitCount = 0

    fun inflate(): ByteArray {
        do {
            val last = bits(1) == 1
            when (bits(2)) {
                0 -> stored()
                1 -> codes(FIXED.lengths, FIXED.distances)
                2 -> dynamic()
                else -> corrupt("invalid block type")
            }
        } while (!last)
        return output.copyOf(outSize)
    }

    private fun bits(need: Int): Int {
        var value = bitBuffer
        while (bitCount < need) {
            if (position >= end) corrupt("compressed data ends early")
            value = value or ((input[position++].toInt() and BYTE_MASK) shl bitCount)
            bitCount += BITS_PER_BYTE
        }
        bitBuffer = value ushr need
        bitCount -= need
        return value and ((1 shl need) - 1)
    }

    private fun stored() {
        bitBuffer = 0
        bitCount = 0
        if (position + STORED_HEADER > end) corrupt("stored block ends early")
        val length = (input[position].toInt() and BYTE_MASK) or ((input[position + 1].toInt() and BYTE_MASK) shl BITS_PER_BYTE)
        val complement = (input[position + 2].toInt() and BYTE_MASK) or ((input[position + 3].toInt() and BYTE_MASK) shl BITS_PER_BYTE)
        if (length != complement.inv() and SHORT_MASK) corrupt("stored length mismatch")
        position += STORED_HEADER
        if (position + length > end) corrupt("stored block ends early")
        ensure(length)
        input.copyInto(output, outSize, position, position + length)
        outSize += length
        position += length
    }

    private fun decode(huffman: Huffman): Int {
        var code = 0
        var first = 0
        var index = 0
        for (length in 1..MAX_BITS) {
            code = code or bits(1)
            val count = huffman.count[length]
            if (code - count < first) return huffman.symbol[index + (code - first)]
            index += count
            first = (first + count) shl 1
            code = code shl 1
        }
        corrupt("ran out of codes")
    }

    private fun codes(lengths: Huffman, distances: Huffman) {
        while (true) {
            var symbol = decode(lengths)
            when {
                symbol < END_OF_BLOCK -> {
                    ensure(1)
                    output[outSize++] = symbol.toByte()
                }
                symbol == END_OF_BLOCK -> return
                else -> {
                    symbol -= LENGTH_CODES_START
                    if (symbol >= LENGTH_BASE.size) corrupt("invalid length code")
                    val length = LENGTH_BASE[symbol] + bits(LENGTH_EXTRA[symbol])
                    val distanceSymbol = decode(distances)
                    if (distanceSymbol >= DISTANCE_BASE.size) corrupt("invalid distance code")
                    val distance = DISTANCE_BASE[distanceSymbol] + bits(DISTANCE_EXTRA[distanceSymbol])
                    if (distance > outSize) corrupt("distance too far back")
                    ensure(length)
                    // Byte by byte on purpose: a copy may overlap itself, which is how runs are encoded.
                    repeat(length) {
                        output[outSize] = output[outSize - distance]
                        outSize++
                    }
                }
            }
        }
    }

    private fun dynamic() {
        val literalCount = bits(LITERAL_COUNT_BITS) + LITERAL_COUNT_BASE
        val distanceCount = bits(DISTANCE_COUNT_BITS) + 1
        val codeCount = bits(CODE_COUNT_BITS) + CODE_COUNT_BASE
        if (literalCount > MAX_LITERAL_CODES || distanceCount > MAX_DISTANCE_CODES) {
            corrupt("bad code counts")
        }
        val lengths = IntArray(MAX_LITERAL_CODES + MAX_DISTANCE_CODES)
        for (i in 0 until codeCount) lengths[CODE_LENGTH_ORDER[i]] = bits(CODE_LENGTH_BITS)
        val lengthCode = Huffman.build(lengths, CODE_LENGTH_ORDER.size)

        var index = 0
        while (index < literalCount + distanceCount) {
            val symbol = decode(lengthCode)
            if (symbol < REPEAT_PREVIOUS) {
                lengths[index++] = symbol
                continue
            }
            var repeated = 0
            val times = when (symbol) {
                REPEAT_PREVIOUS -> {
                    if (index == 0) corrupt("repeat with no previous length")
                    repeated = lengths[index - 1]
                    REPEAT_SHORT_BASE + bits(2)
                }
                REPEAT_ZERO_SHORT -> REPEAT_SHORT_BASE + bits(REPEAT_ZERO_SHORT_BITS)
                else -> REPEAT_ZERO_LONG_BASE + bits(REPEAT_ZERO_LONG_BITS)
            }
            if (index + times > literalCount + distanceCount) corrupt("too many lengths")
            repeat(times) { lengths[index++] = repeated }
        }
        if (lengths[END_OF_BLOCK] == 0) corrupt("no end-of-block code")
        val literals = Huffman.build(lengths, literalCount)
        val distances = Huffman.build(lengths.copyOfRange(literalCount, literalCount + distanceCount), distanceCount)
        codes(literals, distances)
    }

    private fun ensure(extra: Int) {
        val needed = outSize + extra
        if (needed <= output.size) return
        if (needed > maxOutput) corrupt("expands past $maxOutput bytes")
        output = output.copyOf(maxOf(needed, minOf(output.size * 2, maxOutput)))
    }

    /** A canonical Huffman code: how many codes of each length, and the symbols in code order. */
    private class Huffman(val count: IntArray, val symbol: IntArray) {
        companion object {
            fun build(lengths: IntArray, n: Int): Huffman {
                val count = IntArray(MAX_BITS + 1)
                for (i in 0 until n) count[lengths[i]]++
                val offsets = IntArray(MAX_BITS + 1)
                for (length in 1 until MAX_BITS) offsets[length + 1] = offsets[length] + count[length]
                val symbol = IntArray(n)
                for (i in 0 until n) if (lengths[i] != 0) symbol[offsets[lengths[i]]++] = i
                count[0] = 0
                return Huffman(count, symbol)
            }
        }
    }

    private class FixedCodes(val lengths: Huffman, val distances: Huffman)

    companion object {
        /** Deflate everything from [offset] to [offset] + [length] of [input]. */
        fun inflate(input: ByteArray, offset: Int, length: Int, expectedSize: Int, maxOutput: Int): ByteArray =
            Inflater(input, offset, offset + length, expectedSize, maxOutput).inflate()

        private const val BYTE_MASK = 0xFF
        private const val SHORT_MASK = 0xFFFF
        private const val BITS_PER_BYTE = 8
        private const val MIN_BUFFER = 1024
        private const val STORED_HEADER = 4
        private const val MAX_BITS = 15
        private const val END_OF_BLOCK = 256
        private const val LENGTH_CODES_START = 257
        private const val MAX_LITERAL_CODES = 286
        private const val MAX_DISTANCE_CODES = 30
        private const val FIXED_LITERAL_CODES = 288
        private const val LITERAL_COUNT_BITS = 5
        private const val LITERAL_COUNT_BASE = 257
        private const val DISTANCE_COUNT_BITS = 5
        private const val CODE_COUNT_BITS = 4
        private const val CODE_COUNT_BASE = 4
        private const val CODE_LENGTH_BITS = 3
        private const val REPEAT_PREVIOUS = 16
        private const val REPEAT_ZERO_SHORT = 17
        private const val REPEAT_SHORT_BASE = 3
        private const val REPEAT_ZERO_SHORT_BITS = 3
        private const val REPEAT_ZERO_LONG_BASE = 11
        private const val REPEAT_ZERO_LONG_BITS = 7

        private val CODE_LENGTH_ORDER = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

        private val LENGTH_BASE = intArrayOf(
            3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
            35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258,
        )
        private val LENGTH_EXTRA = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
            3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0,
        )
        private val DISTANCE_BASE = intArrayOf(
            1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
            257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577,
        )
        private val DISTANCE_EXTRA = intArrayOf(
            0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
            7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13,
        )

        /** The fixed code of RFC 1951 §3.2.6, built once. */
        private val FIXED: FixedCodes by lazy {
            val lengths = IntArray(FIXED_LITERAL_CODES) { symbol ->
                when {
                    symbol < 144 -> 8
                    symbol < 256 -> 9
                    symbol < 280 -> 7
                    else -> 8
                }
            }
            FixedCodes(
                Huffman.build(lengths, FIXED_LITERAL_CODES),
                Huffman.build(IntArray(MAX_DISTANCE_CODES) { 5 }, MAX_DISTANCE_CODES),
            )
        }
    }
}
