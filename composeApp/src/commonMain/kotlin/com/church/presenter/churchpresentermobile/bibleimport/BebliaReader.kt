package com.church.presenter.churchpresentermobile.bibleimport

/**
 * The "Holy Bible XML" dialect published by Beblia — five elements, no inline markup:
 * ```xml
 * <bible translation="English KJV" status="Public Domain">
 *   <testament name="Old">
 *     <book number="1"><chapter number="1"><verse number="1">In the beginning…</verse>
 * ```
 *
 * A port of the desktop's `BebliaParser`. Books are identified by a bare number, so their names
 * come from [BookNames] — English for most languages, because the names are not in the file to be
 * read. The root spells its title, copyright and URL several ways, and each is read as the first
 * non-blank spelling; whatever the caller already knows wins outright.
 */
internal object BebliaReader {

    private const val MIN_BOOK = 1
    private const val MAX_BOOK = 66

    val TITLE_ATTRIBUTES = listOf("translation", "name", "language")
    private val RIGHTS_ATTRIBUTES = listOf("status", "info", "version")
    private val SOURCE_ATTRIBUTES = listOf("link", "site")

    /** Language names these files' titles use, for a file that arrives with no language of its own. */
    private val TITLE_LANGUAGES = listOf(
        "Russian" to "RUS",
        "Ukrainian" to "UKR",
        "English" to "ENG",
        "German" to "DEU",
        "French" to "FRA",
        "Spanish" to "SPA",
        "Portuguese" to "POR",
        "Italian" to "ITA",
        "Dutch" to "NLD",
        "Polish" to "POL",
        "Chinese" to "ZHO",
        "Korean" to "KOR",
        "Arabic" to "ARA",
        "Hebrew" to "HEB",
    )

    /** The state of one read: book, chapter and verse, and the verse text so far. */
    private class Builder(val hints: ImportHints) {
        var rootTitle = ""
        var rootRights = ""
        var rootSource = ""
        var language: String? = null
        val books = mutableListOf<SourceBook>()

        var bookNumber = 0
        var chapterNumber = 0
        var verseNumber = 0
        var chapters = mutableListOf<SourceChapter>()
        var verses = mutableListOf<SourceVerse>()
        val verseText = StringBuilder()
        var inVerse = false
    }

    fun read(reader: XmlPullReader, hints: ImportHints, progress: ProgressReporter): SourceBible {
        val state = Builder(hints)
        while (true) {
            when (reader.next()) {
                XmlEvent.START -> {
                    start(reader, state)
                    when (reader.name) {
                        "book" -> progress.book()
                        "verse" -> progress.verse()
                    }
                }
                XmlEvent.TEXT -> if (state.inVerse) state.verseText.append(reader.text)
                XmlEvent.END -> end(reader.name, state)
                XmlEvent.EOF -> break
            }
            progress.tick()
        }
        return SourceBible(
            name = hints.name.ifBlank { state.rootTitle }.ifBlank { "Unknown" },
            description = "",
            language = state.language,
            books = state.books,
            title = hints.name.ifBlank { state.rootTitle },
            identifier = hints.identifier,
            rights = hints.rights.ifBlank { state.rootRights },
            source = hints.source.ifBlank { state.rootSource },
        )
    }

    private fun start(reader: XmlPullReader, state: Builder) {
        when (reader.name) {
            "bible" -> {
                state.rootTitle = reader.firstAttribute(TITLE_ATTRIBUTES)
                state.rootRights = reader.firstAttribute(RIGHTS_ATTRIBUTES)
                state.rootSource = reader.firstAttribute(SOURCE_ATTRIBUTES)
                state.language = state.hints.language?.trim()?.uppercase()?.ifBlank { null }
                    ?: languageFromTitle(state.hints.name.ifBlank { state.rootTitle })
            }
            "book" -> {
                state.bookNumber = reader.number()
                state.chapters = mutableListOf()
            }
            "chapter" -> {
                state.chapterNumber = reader.number()
                state.verses = mutableListOf()
            }
            "verse" -> {
                state.verseNumber = reader.number()
                state.verseText.setLength(0)
                state.inVerse = true
            }
        }
    }

    private fun end(name: String, state: Builder) {
        when (name) {
            "verse" -> {
                state.inVerse = false
                val text = VersePatches.apply(
                    state.verseText.toString().normalizeSpace(),
                    state.language, state.bookNumber, state.chapterNumber, state.verseNumber,
                )
                state.verses.add(SourceVerse(state.verseNumber, text))
            }
            "chapter" -> state.chapters.add(SourceChapter(state.chapterNumber, state.verses))
            // A book numbered outside the canon is dropped rather than failing the whole file.
            "book" -> if (state.bookNumber in MIN_BOOK..MAX_BOOK) {
                state.books.add(
                    SourceBook(state.bookNumber, tableBookName(state.bookNumber, state.language), state.chapters),
                )
            }
        }
    }

    internal fun languageFromTitle(title: String): String? =
        TITLE_LANGUAGES.firstOrNull { (name, _) -> title.contains(name, ignoreCase = true) }?.second

    private fun XmlPullReader.number(): Int = attribute("number")?.trim()?.toIntOrNull() ?: 0

    private fun XmlPullReader.firstAttribute(names: List<String>): String =
        names.firstNotNullOfOrNull { attribute(it)?.trim()?.ifBlank { null } }.orEmpty()
}
