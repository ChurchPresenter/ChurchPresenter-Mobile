package com.church.presenter.churchpresentermobile.bibleimport

/** One verse as a source file states it, before it is written out as `.spb`. */
internal data class SourceVerse(val number: Int, val text: String)

internal data class SourceChapter(val number: Int, val verses: List<SourceVerse>)

internal data class SourceBook(val number: Int, val name: String, val chapters: List<SourceChapter>)

/**
 * A Bible read out of Zefania, Holy Bible (Beblia) or USFX XML — the shape all three readers
 * produce, so one writer serves them. Mirrors the desktop's `ParsedBible` in `bible-formats`,
 * renamed so it cannot be confused with the `.spb` reader's own `ParsedBible` in `library`.
 *
 * @param language Uppercase three-letter code, when the file or the catalogue named one.
 */
internal data class SourceBible(
    val name: String,
    val description: String,
    val language: String?,
    val books: List<SourceBook>,
    val title: String = "",
    val identifier: String = "",
    val rights: String = "",
    val source: String = "",
) {
    val verseCount: Int get() = books.sumOf { book -> book.chapters.sumOf { it.verses.size } }

    val hasVerses: Boolean get() = verseCount > 0

    /** The first verse of John 3:16 or, failing that, of the first book — for the convert preview. */
    fun previewVerse(): Pair<String, String>? {
        val john = books.firstOrNull { it.number == JOHN }
        val johnVerse = john?.chapters?.firstOrNull { it.number == JOHN_CHAPTER }
            ?.verses?.firstOrNull { it.number == JOHN_VERSE }
        if (john != null && johnVerse != null) return "${john.name} $JOHN_CHAPTER:$JOHN_VERSE" to johnVerse.text
        val book = books.firstOrNull()
        val chapter = book?.chapters?.firstOrNull()
        val verse = chapter?.verses?.firstOrNull()
        return if (book != null && chapter != null && verse != null) {
            "${book.name} ${chapter.number}:${verse.number}" to verse.text
        } else {
            null
        }
    }

    private companion object {
        const val JOHN = 43
        const val JOHN_CHAPTER = 3
        const val JOHN_VERSE = 16
    }
}

/** Which of the three dialects a file is written in. */
enum class BibleXmlFormat(val label: String) {
    ZEFANIA("Zefania XML"),
    USFX("USFX"),
    BEBLIA("Beblia XML"),
}
