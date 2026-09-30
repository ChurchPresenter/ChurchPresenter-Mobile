package com.church.presenter.churchpresentermobile.bibleimport

/** The file is XML, but none of the three Bible dialects — or not XML at all. */
internal class NotABibleException(message: String) : IllegalArgumentException(message)

/**
 * What the caller knows about a file before reading it, which wins over what the file says.
 *
 * The download catalogues know a translation's language, title and copyright; a Holy Bible XML
 * file carries no language code at all, and a USFX file carries no title. A file picked off the
 * device has none of this, and every field is then read from the file where it can be.
 *
 * @param path Where the file came from. Only consulted to correct Zefania modules that declare
 *   Russian while sitting in the archive's Ukrainian folder.
 * @param bookNames USFX book code to name, from the `BookNames.xml` eBible ships alongside.
 */
internal data class ImportHints(
    val language: String? = null,
    val name: String = "",
    val rights: String = "",
    val source: String = "",
    val identifier: String = "",
    val path: String = "",
    val bookNames: Map<String, String> = emptyMap(),
)

/**
 * Reads a Bible out of Zefania, Holy Bible (Beblia) or USFX XML, deciding which by the root element.
 *
 * The phone's port of the desktop's `XmlToSpbConverter.parse` / `UsfxToSpbConverter.parse`: the
 * same rules for book names, languages and verse patches, over [XmlPullReader] instead of a DOM.
 */
internal object BibleXmlReader {

    /** Progress is reported in about a hundred steps, however large the file. */
    internal const val PROGRESS_STEP = 0.01f

    /**
     * Which dialect [text] is in, from its first element — without reading any further.
     *
     * Null when the root is none of `<XMLBIBLE>`, `<usfx>` or a `<bible>` that names itself, and
     * when the text is not XML at all.
     */
    fun detect(text: String): BibleXmlFormat? = try {
        val reader = XmlPullReader(text)
        var event = reader.next()
        while (event != XmlEvent.START && event != XmlEvent.EOF) event = reader.next()
        if (event == XmlEvent.EOF) null else formatOf(reader)
    } catch (_: XmlFormatException) {
        null
    }

    private fun formatOf(reader: XmlPullReader): BibleXmlFormat? = when {
        reader.name.equals("XMLBIBLE", ignoreCase = true) -> BibleXmlFormat.ZEFANIA
        reader.name.equals("usfx", ignoreCase = true) -> BibleXmlFormat.USFX
        reader.name == "bible" && BebliaReader.TITLE_ATTRIBUTES.any { !reader.attribute(it).isNullOrBlank() } ->
            BibleXmlFormat.BEBLIA
        else -> null
    }

    /**
     * Reads the whole Bible.
     *
     * @param onProgress How far through the file, and how many books and verses so far.
     * @throws NotABibleException the root is not one of the three dialects.
     * @throws XmlFormatException the file is not well-formed.
     */
    fun read(
        text: String,
        hints: ImportHints = ImportHints(),
        onProgress: (ConvertProgress) -> Unit = {},
    ): SourceBible {
        val format = detect(text) ?: throw NotABibleException(
            "no <XMLBIBLE>, <usfx> or <bible> root was found",
        )
        val reader = XmlPullReader(text)
        val progress = ProgressReporter(reader, onProgress)
        val bible = when (format) {
            BibleXmlFormat.ZEFANIA -> ZefaniaReader.read(reader, hints, progress)
            BibleXmlFormat.BEBLIA -> BebliaReader.read(reader, hints, progress)
            BibleXmlFormat.USFX -> UsfxReader.read(reader, hints, progress)
        }
        onProgress(ConvertProgress(1f, bible.books.size, bible.verseCount))
        return bible
    }

    /**
     * Everything under the element whose START was just read, as one string — the DOM's
     * `textContent`, which is what the desktop reads a Zefania verse as.
     */
    internal fun XmlPullReader.textContent(): String {
        val out = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (next()) {
                XmlEvent.TEXT -> out.append(text)
                XmlEvent.START -> depth++
                XmlEvent.END -> depth--
                XmlEvent.EOF -> depth = 0
            }
        }
        return out.toString()
    }
}

/** How far a conversion has got: [fraction] of the file, and what it has read so far. */
data class ConvertProgress(val fraction: Float, val books: Int, val verses: Int)

/** Calls back as the reader moves through the text, a hundred times at most. */
internal class ProgressReporter(
    private val reader: XmlPullReader,
    private val onProgress: (ConvertProgress) -> Unit,
) {
    private var reported = 0f
    private var books = 0
    private var verses = 0

    fun book() {
        books++
    }

    fun verse() {
        verses++
    }

    fun tick() {
        val fraction = reader.position.toFloat() / reader.length.coerceAtLeast(1)
        if (fraction - reported >= BibleXmlReader.PROGRESS_STEP) {
            reported = fraction
            onProgress(ConvertProgress(fraction, books, verses))
        }
    }
}

/**
 * Collapses runs of whitespace — line wrapping in the source — into single spaces, and trims.
 *
 * Only ASCII space, tab and line breaks count. Typographic spaces are content: the World English
 * Bible puts a thin space between nested closing quotes (`’ ”`), and folding it into a plain space
 * would change the verse from what the desktop shows.
 */
internal fun String.normalizeSpace(): String {
    val out = StringBuilder(length)
    var pendingSpace = false
    for (char in this) {
        if (char in ASCII_WHITESPACE) {
            pendingSpace = out.isNotEmpty()
        } else {
            if (pendingSpace) out.append(' ')
            pendingSpace = false
            out.append(char)
        }
    }
    return out.toString()
}

private const val ASCII_WHITESPACE = " \n\r\t"

/** The name the app gives book [number] in [language]: its own table, else English. */
internal fun tableBookName(number: Int, language: String?): String =
    BookNames.LANGUAGE_LOOKUPS[language?.uppercase()]?.get(number)
        ?: BookNames.ENGLISH[number]
        ?: "Book $number"
