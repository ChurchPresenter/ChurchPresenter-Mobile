package com.church.presenter.churchpresentermobile.bibleimport

/** A file declaring an encoding [XmlText] has no table for. */
internal class UnsupportedEncodingException(val encoding: String) :
    IllegalArgumentException("unsupported encoding '$encoding'")

/**
 * An XML file's bytes as text, honouring its byte-order mark and `encoding=` declaration.
 *
 * Kotlin's common library decodes UTF-8 and nothing else, but the Zefania archive predates
 * UTF-8 being universal: some modules are Latin-1, and Russian and Ukrainian ones are sometimes
 * Windows-1251. Those are the encodings covered here. Anything else is refused by name rather
 * than decoded as UTF-8 into a Bible full of replacement characters.
 */
internal object XmlText {

    private const val BYTE_MASK = 0xFF
    private const val BITS_PER_BYTE = 8
    private const val ASCII_LIMIT = 0x80
    private const val UPPER_CONTROL_START = 0x80
    private const val UPPER_CONTROL_END = 0xA0
    private const val DECLARATION_SCAN_BYTES = 256

    private val ENCODING_ATTRIBUTE = Regex("""encoding\s*=\s*["']([A-Za-z0-9._-]+)["']""")

    fun decode(bytes: ByteArray): String {
        bomEncoding(bytes)?.let { (encoding, skip) -> return decodeAs(bytes, skip, encoding) }
        val declared = declaredEncoding(bytes) ?: "utf-8"
        return decodeAs(bytes, 0, declared)
    }

    private fun bomEncoding(bytes: ByteArray): Pair<String, Int>? {
        fun at(i: Int) = bytes.getOrNull(i)?.toInt()?.and(BYTE_MASK)
        return when {
            at(0) == 0xEF && at(1) == 0xBB && at(2) == 0xBF -> "utf-8" to 3
            at(0) == 0xFF && at(1) == 0xFE -> "utf-16le" to 2
            at(0) == 0xFE && at(1) == 0xFF -> "utf-16be" to 2
            else -> null
        }
    }

    /** The `encoding` of an `<?xml …?>` declaration, read as ASCII from the file's first bytes. */
    private fun declaredEncoding(bytes: ByteArray): String? {
        val head = bytes.copyOf(minOf(bytes.size, DECLARATION_SCAN_BYTES))
            .map { (it.toInt() and BYTE_MASK).toChar() }
            .joinToString("")
        if (!head.trimStart().startsWith("<?xml")) return null
        val declaration = head.substringBefore("?>")
        return ENCODING_ATTRIBUTE.find(declaration)?.groupValues?.get(1)
    }

    private fun decodeAs(bytes: ByteArray, skip: Int, encoding: String): String =
        when (encoding.lowercase().replace("_", "-")) {
            "utf-8", "utf8" -> bytes.decodeToString(skip, bytes.size)
            "utf-16le" -> decodeUtf16(bytes, skip, littleEndian = true)
            "utf-16be", "utf-16" -> decodeUtf16(bytes, skip, littleEndian = false)
            "iso-8859-1", "latin1", "latin-1", "iso8859-1", "us-ascii", "ascii" ->
                decodeSingleByte(bytes, skip) { it.toChar() }
            "windows-1252", "cp1252" -> decodeSingleByte(bytes, skip, ::windows1252)
            "windows-1251", "cp1251" -> decodeSingleByte(bytes, skip, ::windows1251)
            else -> throw UnsupportedEncodingException(encoding)
        }

    private fun decodeUtf16(bytes: ByteArray, skip: Int, littleEndian: Boolean): String {
        val chars = CharArray((bytes.size - skip) / 2)
        for (i in chars.indices) {
            val a = bytes[skip + i * 2].toInt() and BYTE_MASK
            val b = bytes[skip + i * 2 + 1].toInt() and BYTE_MASK
            chars[i] = (if (littleEndian) (b shl BITS_PER_BYTE) or a else (a shl BITS_PER_BYTE) or b).toChar()
        }
        return chars.concatToString()
    }

    private inline fun decodeSingleByte(bytes: ByteArray, skip: Int, map: (Int) -> Char): String {
        val chars = CharArray(bytes.size - skip)
        for (i in chars.indices) chars[i] = map(bytes[skip + i].toInt() and BYTE_MASK)
        return chars.concatToString()
    }

    /** Windows-1252: Latin-1 with printable characters where Latin-1 has C1 controls. */
    private fun windows1252(byte: Int): Char =
        if (byte in UPPER_CONTROL_START until UPPER_CONTROL_END) WINDOWS_1252_HIGH[byte - UPPER_CONTROL_START]
        else byte.toChar()

    /** Windows-1251: ASCII, then Cyrillic — `А`…`я` contiguous from 0xC0. */
    private fun windows1251(byte: Int): Char = when {
        byte < ASCII_LIMIT -> byte.toChar()
        byte >= CYRILLIC_START -> (CYRILLIC_A + (byte - CYRILLIC_START)).toChar()
        else -> WINDOWS_1251_HIGH[byte - ASCII_LIMIT]
    }

    private const val CYRILLIC_START = 0xC0
    private const val CYRILLIC_A = 0x0410

    /** 0x80–0x9F. `�` marks the code points the encoding leaves undefined. */
    private val WINDOWS_1252_HIGH = charArrayOf(
        '€', '�', '‚', 'ƒ', '„', '…', '†', '‡',
        'ˆ', '‰', 'Š', '‹', 'Œ', '�', 'Ž', '�',
        '�', '‘', '’', '“', '”', '•', '–', '—',
        '˜', '™', 'š', '›', 'œ', '�', 'ž', 'Ÿ',
    )

    /** 0x80–0xBF. */
    private val WINDOWS_1251_HIGH = charArrayOf(
        'Ђ', 'Ѓ', '‚', 'ѓ', '„', '…', '†', '‡',
        '€', '‰', 'Љ', '‹', 'Њ', 'Ќ', 'Ћ', 'Џ',
        'ђ', '‘', '’', '“', '”', '•', '–', '—',
        '�', '™', 'љ', '›', 'њ', 'ќ', 'ћ', 'џ',
        ' ', 'Ў', 'ў', 'Ј', '¤', 'Ґ', '¦', '§',
        'Ё', '©', 'Є', '«', '¬', '­', '®', 'Ї',
        '°', '±', 'І', 'і', 'ґ', 'µ', '¶', '·',
        'ё', '№', 'є', '»', 'ј', 'Ѕ', 'ѕ', 'ї',
    )
}
