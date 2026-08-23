package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse

/**
 * Reads the `.spb` Bible module format the desktop serves from
 * `GET /api/bible/file/translation/{index}`.
 *
 * Mirrors the desktop's own reader (`bible/Bible.kt`, `bible/SpbFormat.kt`) rather than
 * reimplementing the Bible against the chapter API: one 4.6 MB download beats 1,189 requests,
 * and the two sides then agree on numbering by construction. Where this file looks fussy it is
 * because the desktop is fussy in the same place, and the modules that exercise those corners —
 * Russian and Synodal texts — are the ones most likely to be in use.
 *
 * A module is plain text: `##` metadata, book headers, a `-----` rule, then verses. So this
 * lives in commonMain with no platform library behind it.
 *
 * Nothing here throws. A module truncated three books in opens on those three books, because
 * the alternative is a Bible tab showing nothing during a service over a byte near the end of
 * a file.
 */
object SpbParser {

    /** `B001C001V001 1 1 1 In the beginning…` — code, then display book/chapter/verse, then text. */
    private val VERSE_LINE = Regex("""^(B\d{3}C\d{3}V\d{3})\s+(\d+)\s+(\d+)\s+(\d+)\s+(.*)""")

    /** A bare code on its own line opens the legacy form, whose text follows on later lines. */
    private val CODE_ONLY = Regex("""^B(\d{3})C(\d{3})V(\d{3})$""")

    /** `1 Genesis 50` — book number, name, chapter count. */
    private val BOOK_HEADER = Regex("""^(\d+)\s+(.+?)\s+(\d+)$""")

    /**
     * Book names only, without walking the verses.
     *
     * Stops at the `-----` rule or at the first verse line, whichever comes first — some modules
     * carry no rule, and without the second condition this would read all 4.6 MB to build a list
     * it already had after the first fifty lines.
     */
    fun parseBooks(text: String, fileName: String = ""): List<BibleBook> =
        parseHeaderOnly(text).books.ifEmpty { parse(text, fileName).books }

    /** The whole module: its title, its books, and its verses keyed for chapter lookup. */
    fun parse(text: String, fileName: String = ""): ParsedBible {
        var title = ""
        val headerOrder = mutableListOf<Int>()
        val names = mutableMapOf<Int, String>()
        val declared = mutableMapOf<Int, Int>()
        val chapters = mutableMapOf<Int, MutableList<BibleVerse>>()
        val chaptersSeen = mutableMapOf<Int, MutableSet<Int>>()
        var headerDone = false

        // The legacy form: a bare code line, then its text on the lines after it, until the next
        // code line or the end of the file.
        var pendingCode: MatchResult? = null
        val pendingText = StringBuilder()

        fun flushPending() {
            val code = pendingCode ?: return
            add(
                chapters, chaptersSeen,
                book = code.groupValues[1].toInt(),
                chapter = code.groupValues[2].toInt(),
                number = code.groupValues[3].toInt(),
                text = pendingText.toString().trim(),
            )
            pendingCode = null
            pendingText.setLength(0)
        }

        for (raw in text.lineSequence()) {
            val line = raw.trim()

            if (line.startsWith(TITLE_PREFIX)) {
                title = line.removePrefix(TITLE_PREFIX).trim()
                continue
            }
            if (line.startsWith("##")) continue

            if (!headerDone) {
                val header = BOOK_HEADER.matchEntire(line)
                if (header != null) {
                    val id = header.groupValues[1].toInt()
                    headerOrder += id
                    names[id] = header.groupValues[2].trim()
                    declared[id] = header.groupValues[3].toInt()
                    continue
                }
                // The rule is consumed here; a line starting with B ends the header but is still
                // a verse and must fall through to be read as one.
                if (line.startsWith(SEPARATOR)) { headerDone = true; continue }
                if (line.startsWith("B")) headerDone = true
            }
            if (line.startsWith(SEPARATOR)) continue

            val verse = VERSE_LINE.matchEntire(line)
            if (verse != null) {
                flushPending()
                // The code's numbers are the module's internal (Hebrew) numbering; the three that
                // follow are what the reader is shown. File by the latter, or a Russian module
                // lands its verses in the wrong chapter.
                add(
                    chapters, chaptersSeen,
                    book = verse.groupValues[2].toInt(),
                    chapter = verse.groupValues[3].toInt(),
                    number = verse.groupValues[4].toInt(),
                    text = verse.groupValues[5].trim(),
                )
                continue
            }

            val codeOnly = CODE_ONLY.matchEntire(line)
            if (codeOnly != null) {
                flushPending()
                pendingCode = codeOnly
            } else if (pendingCode != null && line.isNotEmpty()) {
                if (pendingText.isNotEmpty()) pendingText.append('\n')
                pendingText.append(line)
            }
        }
        flushPending()

        return ParsedBible(
            title = title.ifBlank { fileName.substringBeforeLast(".") },
            books = books(headerOrder, names, declared, chaptersSeen),
            versesByChapter = chapters,
        )
    }

    /** Header block alone, abandoned as soon as the verses start. */
    private fun parseHeaderOnly(text: String): ParsedBible {
        val headerOrder = mutableListOf<Int>()
        val names = mutableMapOf<Int, String>()
        val declared = mutableMapOf<Int, Int>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("##") || line.isEmpty()) continue
            if (line.startsWith(SEPARATOR) || line.startsWith("B")) break
            val header = BOOK_HEADER.matchEntire(line) ?: continue
            val id = header.groupValues[1].toInt()
            headerOrder += id
            names[id] = header.groupValues[2].trim()
            declared[id] = header.groupValues[3].toInt()
        }
        return ParsedBible(
            title = "",
            books = books(headerOrder, names, declared, emptyMap()),
            versesByChapter = emptyMap(),
        )
    }

    /**
     * Books in header order, then any that appeared only in the verse data.
     *
     * A module is allowed to omit the header entirely, and one that does still has to open.
     */
    private fun books(
        headerOrder: List<Int>,
        names: Map<Int, String>,
        declared: Map<Int, Int>,
        chaptersSeen: Map<Int, Set<Int>>,
    ): List<BibleBook> {
        val ordered = headerOrder + chaptersSeen.keys.filter { it !in headerOrder }.sorted()
        return ordered.map { id ->
            BibleBook(
                name = names[id] ?: "Book $id",
                bookId = id,
                // The header's own count when it gave one: a module carrying only Genesis 1-2
                // still has fifty chapters, and the book list must not shrink to what downloaded.
                chapterTotal = declared[id] ?: chaptersSeen[id]?.maxOrNull() ?: 0,
            )
        }
    }

    /**
     * Records one verse, merging it into the previous when they share a number.
     *
     * The desktop merges these on read; doing it here reaches the same result. Skipping it shows
     * a doubled verse 1 in exactly the modules that split a verse across two lines.
     */
    private fun add(
        chapters: MutableMap<Int, MutableList<BibleVerse>>,
        chaptersSeen: MutableMap<Int, MutableSet<Int>>,
        book: Int,
        chapter: Int,
        number: Int,
        text: String,
    ) {
        chaptersSeen.getOrPut(book) { mutableSetOf() } += chapter
        val verses = chapters.getOrPut(key(book, chapter)) { mutableListOf() }
        val previous = verses.lastOrNull()
        if (previous != null && previous.number == number) {
            verses[verses.lastIndex] = BibleVerse(
                verse = number,
                text = "${previous.displayText} $text".trim(),
            )
        } else {
            verses += BibleVerse(verse = number, text = text)
        }
    }

    internal fun key(book: Int, chapter: Int): Int = book * CHAPTER_KEY_STRIDE + chapter

    private const val TITLE_PREFIX = "##Title:"
    private const val SEPARATOR = "-----"

    /** Comfortably above any book's chapter count, so a key cannot collide with the next book. */
    private const val CHAPTER_KEY_STRIDE = 1000
}

/**
 * One parsed module, held in memory.
 *
 * Kept whole rather than re-read per chapter: [FileStore] hands back an entire document, and a
 * 4.6 MB read plus reparse on every chapter turn would be felt on stage.
 */
data class ParsedBible(
    val title: String,
    val books: List<BibleBook>,
    private val versesByChapter: Map<Int, List<BibleVerse>>,
) {
    /** Verses of one chapter, in file order. Empty when the module does not carry it. */
    fun chapter(bookNumber: Int, chapter: Int): List<BibleVerse> =
        versesByChapter[SpbParser.key(bookNumber, chapter)].orEmpty()

    /** How many verses the module carried — what the operator is told after a download. */
    val verseCount: Int get() = versesByChapter.values.sumOf { it.size }

    /** True when the module carried no verses at all — a download that arrived as junk. */
    val isEmpty: Boolean get() = versesByChapter.isEmpty()

    companion object {
        val EMPTY = ParsedBible(title = "", books = emptyList(), versesByChapter = emptyMap())
    }
}
