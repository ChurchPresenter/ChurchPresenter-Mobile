package com.church.presenter.churchpresentermobile.calendar

/** A book of the Bible as the picker lists it: its name and how many chapters it has. */
data class PickerBook(val number: Int, val name: String, val chapters: Int)

/** A parsed `John 3:16-17`: the book, the chapter, and the verse or range within it. */
data class ParsedReference(val book: PickerBook, val chapter: Int, val verseFrom: Int?, val verseTo: Int?) {
    val text: String
        get() = buildString {
            append(book.name).append(' ').append(chapter)
            if (verseFrom != null) {
                append(':').append(verseFrom)
                if (verseTo != null && verseTo != verseFrom) append('-').append(verseTo)
            }
        }
}

private const val GROUP_BOOK = 1
private const val GROUP_CHAPTER = 2
private const val GROUP_FROM = 3
private const val GROUP_TO = 4

private val REFERENCE = Regex(
    """^\s*((?:[1-3]\s*)?[A-Za-z][A-Za-z .]*?)\s*(\d+)(?:\s*[:.]\s*(\d+)(?:\s*[-–]\s*(\d+))?)?\s*$""",
)

/**
 * Reads a typed reference against [books]: `Ps 100:1-5`, `1 John 3`, `Genesis 1:1`. Book names
 * match on a prefix and ignore case and spaces, so `1jn` and `Song of` both resolve. Null when
 * the text is not a reference — which is what makes the search field double as one.
 */
fun parseReference(text: String, books: List<PickerBook>): ParsedReference? {
    val match = REFERENCE.matchEntire(text) ?: return null
    val groups = match.groupValues
    val bookText = groups[GROUP_BOOK]
    val chapterText = groups[GROUP_CHAPTER]
    val fromText = groups[GROUP_FROM]
    val toText = groups[GROUP_TO]
    val book = findBook(bookText, books) ?: return null
    val chapter = chapterText.toIntOrNull()?.takeIf { it in 1..book.chapters } ?: return null
    val from = fromText.toIntOrNull()
    val to = toText.toIntOrNull()?.takeIf { from != null && it >= from }
    return ParsedReference(book, chapter, from, to)
}

fun findBook(text: String, books: List<PickerBook>): PickerBook? {
    val key = normalizeBook(text)
    if (key.isEmpty()) return null
    return books.firstOrNull { normalizeBook(it.name) == key }
        ?: books.firstOrNull { normalizeBook(it.name).startsWith(key) }
        ?: BOOK_ALIASES[key]?.let { alias -> books.firstOrNull { normalizeBook(it.name) == alias } }
}

private fun normalizeBook(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

private val BOOK_ALIASES = mapOf(
    "ps" to "psalms", "psalm" to "psalms", "sos" to "songofsolomon", "songofsongs" to "songofsolomon",
    "rev" to "revelation", "mt" to "matthew", "mk" to "mark", "lk" to "luke", "jn" to "john",
    "1jn" to "1john", "2jn" to "2john", "3jn" to "3john", "phil" to "philippians", "phm" to "philemon",
    "gen" to "genesis", "ex" to "exodus", "dt" to "deuteronomy", "isa" to "isaiah", "jer" to "jeremiah",
)

/** The 66 books with their chapter counts, for when neither a desktop nor a downloaded Bible is at hand. */
val CANONICAL_BOOKS: List<PickerBook> = listOf(
    "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36, "Deuteronomy" to 34,
    "Joshua" to 24, "Judges" to 21, "Ruth" to 4, "1 Samuel" to 31, "2 Samuel" to 24,
    "1 Kings" to 22, "2 Kings" to 25, "1 Chronicles" to 29, "2 Chronicles" to 36, "Ezra" to 10,
    "Nehemiah" to 13, "Esther" to 10, "Job" to 42, "Psalms" to 150, "Proverbs" to 31,
    "Ecclesiastes" to 12, "Song of Solomon" to 8, "Isaiah" to 66, "Jeremiah" to 52, "Lamentations" to 5,
    "Ezekiel" to 48, "Daniel" to 12, "Hosea" to 14, "Joel" to 3, "Amos" to 9,
    "Obadiah" to 1, "Jonah" to 4, "Micah" to 7, "Nahum" to 3, "Habakkuk" to 3,
    "Zephaniah" to 3, "Haggai" to 2, "Zechariah" to 14, "Malachi" to 4,
    "Matthew" to 28, "Mark" to 16, "Luke" to 24, "John" to 21, "Acts" to 28,
    "Romans" to 16, "1 Corinthians" to 16, "2 Corinthians" to 13, "Galatians" to 6, "Ephesians" to 6,
    "Philippians" to 4, "Colossians" to 4, "1 Thessalonians" to 5, "2 Thessalonians" to 3, "1 Timothy" to 6,
    "2 Timothy" to 4, "Titus" to 3, "Philemon" to 1, "Hebrews" to 13, "James" to 5,
    "1 Peter" to 5, "2 Peter" to 3, "1 John" to 5, "2 John" to 1, "3 John" to 1,
    "Jude" to 1, "Revelation" to 22,
).mapIndexed { index, (name, chapters) -> PickerBook(index + 1, name, chapters) }
