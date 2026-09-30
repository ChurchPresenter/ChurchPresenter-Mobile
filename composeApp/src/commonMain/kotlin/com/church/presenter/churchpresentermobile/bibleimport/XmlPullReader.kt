package com.church.presenter.churchpresentermobile.bibleimport

/** A document this reader could not make sense of: unterminated markup, a stray `<`, and so on. */
internal class XmlFormatException(message: String) : IllegalArgumentException(message)

/** What [XmlPullReader.next] just read. */
internal enum class XmlEvent { START, END, TEXT, EOF }

/**
 * Just enough of an XML pull parser to read Bible modules, with nothing platform-specific behind it.
 *
 * The desktop reads these files with `javax.xml`, which does not exist on iOS. A general XML
 * library would be a large dependency for three flat dialects, so this reads the subset those
 * dialects use: elements, attributes, text, CDATA and the character references. Comments,
 * processing instructions and the `<!DOCTYPE>` are skipped — and a DOCTYPE's entities are never
 * expanded, which is also what keeps a downloaded file from defining a billion-laughs bomb.
 *
 * It pulls rather than building a tree because a Holy Bible XML file reaches 21 MB: a tree of
 * 31,000 verse elements is hundreds of megabytes on a phone, where a pull costs the text itself.
 *
 * Element and attribute names are reported without a namespace prefix — none of the dialects
 * relies on one, and USFX files from some tools carry a default namespace.
 *
 * `<a/>` is reported as a [XmlEvent.START] followed by an [XmlEvent.END], so a reader never has to
 * tell the two shapes apart.
 */
internal class XmlPullReader(private val source: String) {

    /** Position in [source]; with [length] it is the only progress signal a pull parse has. */
    var position: Int = 0
        private set

    val length: Int get() = source.length

    /** The element [XmlEvent.START] or [XmlEvent.END] just read. */
    var name: String = ""
        private set

    /** The text [XmlEvent.TEXT] just read, entities decoded. */
    var text: String = ""
        private set

    private var attributes: Map<String, String> = emptyMap()

    /** Set after `<a/>`: the next call reports the END for the element just started. */
    private var pendingEnd: String? = null

    /** An attribute of the element just started, or null. */
    fun attribute(name: String): String? = attributes[name]

    /** The reader's full state, for [restore]. Lets a caller look ahead and then go back. */
    fun mark(): Mark = Mark(position, pendingEnd)

    fun restore(mark: Mark) {
        position = mark.position
        pendingEnd = mark.pendingEnd
    }

    class Mark internal constructor(internal val position: Int, internal val pendingEnd: String?)

    /** Reads the next event, skipping comments, declarations and processing instructions. */
    fun next(): XmlEvent {
        var event: XmlEvent? = pendingEnd?.let { element ->
            pendingEnd = null
            name = element
            XmlEvent.END
        }
        while (event == null) {
            event = when {
                position >= source.length -> XmlEvent.EOF
                source[position] != '<' -> readText()
                source.startsWith("<!--", position) -> null.also { position = indexPast(source, position, "-->") }
                source.startsWith(CDATA_OPEN, position) -> readCdata()
                source.startsWith("<!", position) -> skipDeclaration()
                source.startsWith("<?", position) -> null.also { position = indexPast(source, position, "?>") }
                source.startsWith("</", position) -> readEnd()
                else -> readStart()
            }
        }
        return event
    }

    private fun readText(): XmlEvent {
        val end = source.indexOf('<', position).let { if (it < 0) source.length else it }
        val raw = source.substring(position, end)
        position = end
        text = if ('&' in raw) decodeEntities(raw) else raw
        return XmlEvent.TEXT
    }

    private fun readCdata(): XmlEvent {
        val start = position + CDATA_OPEN.length
        val end = source.indexOf(CDATA_CLOSE, start)
        if (end < 0) fail("unterminated CDATA section at $position")
        text = source.substring(start, end)
        position = end + CDATA_CLOSE.length
        return XmlEvent.TEXT
    }

    private fun readEnd(): XmlEvent {
        val close = source.indexOf('>', position)
        if (close < 0) fail("unterminated end tag at $position")
        name = localName(source.substring(position + 2, close).trim())
        position = close + 1
        return XmlEvent.END
    }

    private fun readStart(): XmlEvent {
        var index = position + 1
        val nameStart = index
        while (index < source.length && !source[index].isTagDelimiter()) index++
        if (index == nameStart) fail("stray '<' at $position")
        name = localName(source.substring(nameStart, index))

        val read = mutableMapOf<String, String>()
        var closed = false
        while (!closed) {
            index = skipSpace(source, index)
            when (source.getOrNull(index)) {
                null -> fail("unterminated start tag <$name>")
                '>' -> {
                    position = index + 1
                    closed = true
                }
                '/' -> {
                    if (source.getOrNull(index + 1) != '>') fail("bad '/' in <$name>")
                    position = index + 2
                    pendingEnd = name
                    closed = true
                }
                else -> index = readAttribute(index, read)
            }
        }
        attributes = read
        return XmlEvent.START
    }

    /** Reads `key="value"` from [start] into [into], returning the index just past it. */
    private fun readAttribute(start: Int, into: MutableMap<String, String>): Int {
        var index = start
        while (index < source.length && source[index] != '=' && !source[index].isTagDelimiter()) index++
        val key = localName(source.substring(start, index))
        index = skipSpace(source, index)
        // An HTML-style bare attribute. Not XML, but not worth failing a whole Bible over.
        if (source.getOrNull(index) != '=') {
            into[key] = ""
            return index
        }
        index = skipSpace(source, index + 1)
        val quote = source.getOrNull(index)
        if (quote != '"' && quote != '\'') fail("unquoted attribute '$key' in <$name>")
        val close = source.indexOf(quote, index + 1)
        if (close < 0) fail("unterminated attribute '$key' in <$name>")
        val raw = source.substring(index + 1, close)
        into[key] = if ('&' in raw) decodeEntities(raw) else raw
        return close + 1
    }

    /** `<!DOCTYPE …>`, including an internal subset in square brackets. */
    private fun skipDeclaration(): XmlEvent? {
        var index = position + 2
        var bracketDepth = 0
        while (index < source.length && !(source[index] == '>' && bracketDepth <= 0)) {
            when (source[index]) {
                '[' -> bracketDepth++
                ']' -> bracketDepth--
            }
            index++
        }
        if (index >= source.length) fail("unterminated declaration at $position")
        position = index + 1
        // No event: [next] keeps looking.
        return null
    }

    companion object {
        private const val CDATA_OPEN = "<![CDATA["
        private const val CDATA_CLOSE = "]]>"

        /**
         * Replaces `&amp;`, `&#1076;`, `&#x444;` and friends.
         *
         * An unknown or malformed reference is left as written: dropping it would silently change
         * the verse, where a visible `&foo;` at least shows something is off.
         */
        internal fun decodeEntities(raw: String): String = XmlEntities.decode(raw)
    }
}

/** Throws the reader's one kind of error — the single place a malformed document is reported. */
private fun fail(message: String): Nothing = throw XmlFormatException(message)

/** The index just past the next [terminator] from [from]. */
private fun indexPast(source: String, from: Int, terminator: String): Int {
    val end = source.indexOf(terminator, from)
    if (end < 0) fail("unterminated markup at $from")
    return end + terminator.length
}

private fun skipSpace(source: String, from: Int): Int {
    var index = from
    while (index < source.length && source[index].isWhitespace()) index++
    return index
}

private fun Char.isTagDelimiter(): Boolean = isWhitespace() || this == '>' || this == '/' || this == '='

private fun localName(qualified: String): String = qualified.substringAfter(':')

/** Character references and the handful of named entities these files use. */
private object XmlEntities {
    private const val HEX_RADIX = 16
    private const val DECIMAL_RADIX = 10

    /** Longest entity worth looking for; anything longer is a bare ampersand. */
    private const val MAX_ENTITY_LENGTH = 10

    private const val MAX_BMP = 0xFFFF
    private const val MAX_CODE_POINT = 0x10FFFF
    private const val SUPPLEMENTARY_BASE = 0x10000
    private const val HIGH_SURROGATE_BASE = 0xD800
    private const val LOW_SURROGATE_BASE = 0xDC00
    private const val SURROGATE_SHIFT = 10
    private const val SURROGATE_MASK = 0x3FF

    private val NAMED = mapOf(
        "lt" to "<", "gt" to ">", "amp" to "&", "quot" to "\"", "apos" to "'",
        // Not predefined in XML, but HTML exports declare it in a DOCTYPE this never reads.
        "nbsp" to " ",
    )

    fun decode(raw: String): String {
        val out = StringBuilder(raw.length)
        var index = 0
        while (index < raw.length) {
            val semicolon = if (raw[index] == '&') raw.indexOf(';', index) else -1
            val decoded = if (semicolon < 0 || semicolon - index > MAX_ENTITY_LENGTH) {
                null
            } else {
                entity(raw.substring(index + 1, semicolon))
            }
            if (decoded == null) {
                out.append(raw[index])
                index++
            } else {
                out.append(decoded)
                index = semicolon + 1
            }
        }
        return out.toString()
    }

    private fun entity(body: String): String? {
        if (!body.startsWith("#")) return NAMED[body]
        val hex = body.startsWith("#x") || body.startsWith("#X")
        val code = if (hex) body.substring(2).toIntOrNull(HEX_RADIX) else body.substring(1).toIntOrNull(DECIMAL_RADIX)
        return code?.let(::codePointToString)
    }

    private fun codePointToString(code: Int): String? = when {
        code < 0 || code > MAX_CODE_POINT -> null
        code <= MAX_BMP -> code.toChar().toString()
        else -> {
            val offset = code - SUPPLEMENTARY_BASE
            charArrayOf(
                (HIGH_SURROGATE_BASE + (offset shr SURROGATE_SHIFT)).toChar(),
                (LOW_SURROGATE_BASE + (offset and SURROGATE_MASK)).toChar(),
            ).concatToString()
        }
    }
}
