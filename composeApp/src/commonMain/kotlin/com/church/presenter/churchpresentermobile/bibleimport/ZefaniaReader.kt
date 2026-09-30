package com.church.presenter.churchpresentermobile.bibleimport

import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlReader.textContent

/**
 * Zefania XML — `<XMLBIBLE><BIBLEBOOK bnumber><CHAPTER cnumber><VERS vnumber>` — as the desktop's
 * `XmlToSpbConverter` reads it.
 *
 * Book names follow the desktop's rules, which do not trust the module: an English module's own
 * `bname` is used, a language the app has a table for uses the table, and anything else takes the
 * name its first chapter's `<CAPTION>` states ("1. Genesis") before falling back to English.
 */
internal object ZefaniaReader {

    /** Longer than this and a chapter caption is a sentence about the book, not its name. */
    private const val MAX_CAPTION_NAME_LENGTH = 30

    /** What a module's `<INFORMATION>` block states about itself. */
    private data class Information(
        val description: String = "",
        val title: String = "",
        val identifier: String = "",
        val rights: String = "",
        val source: String = "",
        val language: String? = null,
    )

    /** A book as read, before its name can be decided — that needs the whole file's language. */
    private class RawBook(val number: Int, val declaredName: String?) {
        val chapters = mutableListOf<SourceChapter>()
        var captionName: String? = null
    }

    fun read(reader: XmlPullReader, hints: ImportHints, progress: ProgressReporter): SourceBible {
        var bibleName = ""
        var info = Information()
        val books = mutableListOf<RawBook>()
        var book: RawBook? = null
        var chapterNumber = 0
        var verses = mutableListOf<SourceVerse>()

        while (true) {
            when (reader.next()) {
                XmlEvent.START -> when (reader.name) {
                    "XMLBIBLE" -> bibleName = reader.attribute("biblename").orEmpty()
                    "INFORMATION" -> info = readInformation(reader, hints)
                    "BIBLEBOOK" -> book = RawBook(
                        number = reader.attribute("bnumber")?.trim()?.toIntOrNull() ?: 0,
                        declaredName = listOf("bname", "bsname")
                            .firstNotNullOfOrNull { reader.attribute(it)?.takeIf(String::isNotBlank) },
                    ).also {
                        books.add(it)
                        progress.book()
                    }
                    "CHAPTER" -> {
                        chapterNumber = reader.attribute("cnumber")?.trim()?.toIntOrNull() ?: 0
                        verses = mutableListOf()
                    }
                    "CAPTION" -> readCaption(reader, book)
                    "VERS" -> {
                        verses.add(
                            SourceVerse(
                                number = reader.attribute("vnumber")?.trim()?.toIntOrNull() ?: 0,
                                text = reader.textContent().normalizeSpace(),
                            ),
                        )
                        progress.verse()
                    }
                }
                XmlEvent.END -> if (reader.name == "CHAPTER") {
                    book?.chapters?.add(SourceChapter(chapterNumber, verses))
                }
                XmlEvent.TEXT -> Unit
                XmlEvent.EOF -> break
            }
            progress.tick()
        }

        val language = info.language?.takeIf { it.isNotBlank() }
            ?: hints.language?.trim()?.uppercase()?.ifBlank { null }
        return SourceBible(
            name = hints.name.ifBlank { bibleName.ifBlank { info.title } }.ifBlank { "Unknown" },
            description = info.description,
            language = language,
            books = books.map { raw -> finish(raw, language) },
            title = info.title,
            identifier = hints.identifier.ifBlank { info.identifier },
            rights = hints.rights.ifBlank { info.rights },
            source = hints.source.ifBlank { info.source },
        )
    }

    private fun finish(raw: RawBook, language: String?): SourceBook = SourceBook(
        number = raw.number,
        name = bookName(raw, language),
        chapters = raw.chapters.map { chapter ->
            chapter.copy(
                verses = chapter.verses.map { verse ->
                    verse.copy(text = VersePatches.apply(verse.text, language, raw.number, chapter.number, verse.number))
                },
            )
        },
    )

    private fun bookName(raw: RawBook, language: String?): String {
        val canonical = BookNames.ENGLISH[raw.number] ?: "Book ${raw.number}"
        if (language == "ENG") raw.declaredName?.let { return it }
        if (language != null && language in BookNames.LANGUAGE_LOOKUPS) {
            return BookNames.LANGUAGE_LOOKUPS[language]?.get(raw.number) ?: canonical
        }
        return raw.captionName ?: canonical
    }

    /** The first caption of a book's first chapter, when it reads like "1. Genesis". */
    private fun readCaption(reader: XmlPullReader, book: RawBook?) {
        val text = reader.textContent().trim()
        if (book == null || book.chapters.isNotEmpty() || book.captionName != null) return
        val name = text.substringAfterLast(".").trim()
        if ("." in text && name.isNotBlank() && name.length < MAX_CAPTION_NAME_LENGTH) book.captionName = name
    }

    private fun readInformation(reader: XmlPullReader, hints: ImportHints): Information {
        var read = Information()
        var depth = 1
        while (depth > 0) {
            when (reader.next()) {
                XmlEvent.START -> {
                    val field = reader.name
                    val text = reader.textContent()
                    read = when (field) {
                        "description" -> read.copy(description = text.normalizeSpace())
                        "title" -> read.copy(title = text.trim())
                        "identifier" -> read.copy(identifier = text.trim())
                        "rights" -> read.copy(rights = text.normalizeSpace())
                        "source" -> read.copy(source = text.trim())
                        "language" -> read.copy(language = declaredLanguage(text, hints.path))
                        else -> read
                    }
                }
                XmlEvent.END -> depth--
                XmlEvent.TEXT -> Unit
                XmlEvent.EOF -> depth = 0
            }
        }
        return read
    }

    /**
     * The language the file declares, corrected against where it came from: real archive entries
     * declare `RUS` on a Ukrainian module, which would install with Russian book names over
     * Ukrainian text.
     */
    private fun declaredLanguage(text: String, path: String): String {
        val declared = text.trim().uppercase()
        val lowerPath = path.lowercase()
        val ukrainianPath = "ukrainian" in lowerPath || "українська" in lowerPath || "/ukr/" in lowerPath
        return if (declared == "RUS" && ukrainianPath) "UKR" else declared
    }
}
