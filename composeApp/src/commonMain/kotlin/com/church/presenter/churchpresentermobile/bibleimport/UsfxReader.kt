package com.church.presenter.churchpresentermobile.bibleimport

import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlReader.textContent

/**
 * USFX — the XML scripture format eBible.org publishes — as the desktop's `UsfxToSpbConverter`
 * reads it.
 *
 * USFX is milestone-based: `<v id="1"/>` and `<c id="1"/>` are empty markers and a verse's text is
 * whatever follows until the next marker, mixed in with paragraph, poetry and word-level tags. So
 * this walks the document in order and accumulates text, which a pull reader does naturally.
 *
 * A verse marker with no text is dropped rather than written out empty: translations working from
 * the earliest manuscripts publish the disputed verses (Matthew 17:21, Acts 8:37…) as a marker
 * whose text is a footnote, and writing those out would put blank slides on the screen.
 */
internal object UsfxReader {

    /** USFM book codes for the 66-book canon, in order. */
    private val BOOK_NUMBERS: Map<String, Int> = listOf(
        "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA", "1KI", "2KI",
        "1CH", "2CH", "EZR", "NEH", "EST", "JOB", "PSA", "PRO", "ECC", "SNG", "ISA", "JER",
        "LAM", "EZK", "DAN", "HOS", "JOL", "AMO", "OBA", "JON", "MIC", "NAM", "HAB", "ZEP",
        "HAG", "ZEC", "MAL",
        "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH", "PHP", "COL",
        "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS", "1PE", "2PE", "1JN", "2JN",
        "3JN", "JUD", "REV",
    ).withIndex().associate { (index, code) -> code to index + 1 }

    /**
     * Elements whose contents are not scripture text: footnotes and cross-references (which sit
     * inside the verse they annotate), headings, front matter and alternate numbering.
     *
     * `d` — the descriptor — is deliberately absent: it carries Psalm superscriptions, which are
     * scripture, and in some translations it wraps the verse marker itself.
     */
    private val SKIPPED_ELEMENTS = setOf(
        "f", "fe", "x", "ef", "ex",
        "s", "r", "sp", "ms", "mr",
        "toc", "h", "id", "ide", "rem",
        "fig", "cl", "cp", "va", "vp",
    )

    /** eBible's ISO 639-3 codes are lowercase; the app keys everything on uppercase. */
    private const val LANGUAGE_CODE_ELEMENT = "languageCode"

    fun read(reader: XmlPullReader, hints: ImportHints, progress: ProgressReporter): SourceBible {
        val collector = Collector(hints.bookNames, hints.language?.trim()?.uppercase()?.ifBlank { null })
        while (true) {
            val event = reader.next()
            if (event == XmlEvent.EOF) break
            when (event) {
                XmlEvent.START -> start(reader, collector, progress)
                XmlEvent.TEXT -> collector.text(reader.text)
                else -> Unit
            }
            progress.tick()
        }
        val books = collector.finish()
        return SourceBible(
            name = hints.name.ifBlank { "Unknown" },
            description = hints.rights,
            language = collector.language,
            books = books,
            title = hints.name,
            identifier = hints.identifier,
            rights = hints.rights,
            source = hints.source,
        )
    }

    private fun start(reader: XmlPullReader, collector: Collector, progress: ProgressReporter) {
        val tag = reader.name.lowercase()
        // Dropping an annotation is cosmetic; dropping a verse is a missing verse with nothing to
        // show for it. So a skipped element is walked anyway when a verse marker is inside it.
        if (tag in SKIPPED_ELEMENTS && !containsVerseMarker(reader)) return
        when {
            tag == "book" -> {
                collector.startBook(reader.attribute("id").orEmpty())
                progress.book()
            }
            tag == "c" -> collector.startChapter(reader.attribute("id").orEmpty())
            tag == "v" -> {
                collector.startVerse(reader.attribute("id").orEmpty())
                progress.verse()
            }
            tag == "ve" -> collector.endVerse()
            reader.name == LANGUAGE_CODE_ELEMENT -> collector.declareLanguage(reader.textContent())
        }
    }

    /**
     * Whether the element whose START was just read has a `<v>` inside it.
     *
     * Looks ahead and comes back: when it does not, the element has been consumed — which is the
     * skip — and when it does, the reader is put back so the element is walked like any other.
     */
    private fun containsVerseMarker(reader: XmlPullReader): Boolean {
        val mark = reader.mark()
        var depth = 1
        while (depth > 0) {
            when (reader.next()) {
                XmlEvent.START -> {
                    if (reader.name.lowercase() == "v") {
                        reader.restore(mark)
                        return true
                    }
                    depth++
                }
                XmlEvent.END -> depth--
                XmlEvent.EOF -> depth = 0
                XmlEvent.TEXT -> Unit
            }
        }
        return false
    }

    internal fun bookNumberFor(code: String): Int? = BOOK_NUMBERS[code.trim().uppercase()]

    /**
     * `<book code="GEN" abbr="Genesis" short="Genesis" long="Genesis"/>` → `GEN` to `Genesis`.
     *
     * eBible ships this beside the USFX, and it names the books in the translation's own language —
     * which is why eBible modules need none of the curated tables the other dialects fall back on.
     */
    fun parseBookNames(text: String): Map<String, String> {
        val names = mutableMapOf<String, String>()
        val reader = XmlPullReader(text)
        var event = reader.next()
        while (event != XmlEvent.EOF) {
            if (event == XmlEvent.START && reader.name == "book") bookName(reader)?.let { (code, label) -> names[code] = label }
            event = reader.next()
        }
        return names
    }

    /** One `<book>` entry as its code and the name to show, or null when it names nothing. */
    private fun bookName(reader: XmlPullReader): Pair<String, String>? {
        val code = reader.attribute("code")?.trim()?.uppercase().orEmpty()
        // `short` is what a book list wants; `abbr` and `long` are the fallbacks.
        val label = listOf("short", "abbr", "long")
            .map { reader.attribute(it)?.trim().orEmpty() }
            .firstOrNull { it.isNotEmpty() }
        return if (code.isEmpty() || label == null) null else code to label
    }

    /** Accumulates the running text into books, chapters and verses as the milestones go past. */
    private class Collector(private val bookNames: Map<String, String>, var language: String?) {
        private var bookNumber: Int? = null
        private var bookName = ""
        private var chapterNumber = 0
        private var verseNumber: Int? = null
        private val text = StringBuilder()

        private val chapters = linkedMapOf<Int, MutableList<SourceVerse>>()
        private val books = mutableListOf<SourceBook>()

        fun declareLanguage(code: String) {
            if (language == null) language = code.trim().uppercase().ifBlank { null }
        }

        fun startBook(code: String) {
            flushBook()
            bookNumber = bookNumberFor(code)
            // The translation's own name first, then the app's table, then English.
            bookName = bookNames[code.trim().uppercase()]
                ?: bookNumber?.let { tableBookName(it, language) }
                ?: code
            chapterNumber = 0
        }

        fun startChapter(id: String) {
            flushVerse()
            chapterNumber = id.trim().toIntOrNull() ?: (chapterNumber + 1)
        }

        fun startVerse(id: String) {
            flushVerse()
            // Ranges like `17-18` are stored under their first number.
            verseNumber = id.trim().substringBefore('-').toIntOrNull()
        }

        fun endVerse() = flushVerse()

        fun text(value: String) {
            if (verseNumber != null) text.append(value)
        }

        private fun flushVerse() {
            val verse = verseNumber
            val body = text.toString().normalizeSpace()
            text.setLength(0)
            verseNumber = null
            if (verse == null || body.isEmpty() || bookNumber == null) return
            chapters.getOrPut(chapterNumber) { mutableListOf() }.add(SourceVerse(verse, body))
        }

        private fun flushBook() {
            flushVerse()
            val number = bookNumber ?: return
            if (chapters.isNotEmpty()) {
                books.add(SourceBook(number, bookName, chapters.map { (chapter, verses) -> SourceChapter(chapter, verses) }))
            }
            chapters.clear()
            bookNumber = null
        }

        fun finish(): List<SourceBook> {
            flushBook()
            return books.sortedBy { it.number }
        }
    }
}
