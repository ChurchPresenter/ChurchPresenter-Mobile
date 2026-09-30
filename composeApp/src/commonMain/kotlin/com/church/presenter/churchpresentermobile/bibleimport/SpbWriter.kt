package com.church.presenter.churchpresentermobile.bibleimport

/**
 * Writes a [SourceBible] as an `.spb` module — the desktop's own format, laid out exactly as its
 * `XmlToSpbConverter.write` lays it out. The readers differ from the desktop's in one respect only:
 * a verse is trimmed, where the desktop keeps a Zefania verse's stray leading or trailing space.
 *
 * ```
 * ##spDataVersion:	1
 * ##Title:	King James Version
 * ##Abbreviation:	KJV
 * 1	Genesis	50
 * -----
 * B001C001V001	1	1	1	In the beginning…
 * ```
 *
 * The `BxxxCxxxVxxx` code is what two translations are aligned on side by side. For a Septuagint
 * Psalter it is written in Hebrew numbering, and a psalm whose first verse is nothing but its title
 * codes that title as verse 0 — so the verses under it line up with a Hebrew-numbered translation.
 */
internal object SpbWriter {

    /** Longer than this and a psalm's first verse is content, whatever it opens with. */
    private const val MAX_SUPERSCRIPTION_LENGTH = 200

    /** How much text may follow a bracketed title before the verse counts as content. */
    private const val MAX_TITLE_REMAINDER = 40

    private const val PSALMS_BOOK_NUMBER = 19

    /** Languages whose Psalter follows the Septuagint's numbering (Orthodox traditions). */
    private val LXX_PSALM_LANGUAGES = setOf(
        "RUS", "UKR", "BEL",
        "SRP", "BUL", "MKD",
        "RON", "RUM", "MOL",
        "KAT", "GEO",
        "GRE", "GRC", "ELL",
        "AMH", "ETH",
        "COP",
        "SYR", "ARC",
    )

    /** The psalms numbered differently in the two traditions, LXX chapter to Hebrew chapter. */
    private val LXX_PSALM_EXCEPTIONS = mapOf(
        9 to 9, 113 to 114, 114 to 116, 115 to 116, 146 to 147, 147 to 147,
    )

    /** The runs where the Septuagint is exactly one behind the Hebrew numbering. */
    private val LXX_PSALMS_ONE_BEHIND = listOf(10..112, 116..145)

    private val BRACKETED_TITLE = Regex("«[^»]*»\\.?")

    private val TITLE_PATTERNS = listOf(
        "Псалом", "Молитва", "Начальнику", "Песнь", "Аллилуия",
        "Давида", "Асафа", "Кореевых", "Соломона", "Моисея", "Ефама", "Емана",
        "Psalm", "Prayer", "Song", "Maskil", "Miktam", "Shiggaion",
        "For the director", "Of David", "Of Asaph", "Of Solomon",
        "Пісня",
    )

    /**
     * The module text.
     *
     * @param title What the module is called in the translation list; defaults to the file's own.
     * @param abbreviation Shown beside every verse on screen; defaults to the title's initials, as
     *   the desktop does.
     */
    fun write(
        bible: SourceBible,
        title: String = bible.name,
        abbreviation: String = BibleNaming.abbreviation(title),
    ): String = buildString {
        val rtl = if (BookNames.isRightToLeft(bible.language)) "1" else ""
        append("##spDataVersion:\t1\n")
        append("##Title:\t${title.oneLine()}\n")
        append("##Abbreviation:\t${abbreviation.oneLine()}\n")
        append("##Information:\t${bible.description.oneLine()}\n")
        append("##RightToLeft:\t$rtl\n")
        // Attribution travels with the file, so it survives the module being copied elsewhere.
        if (bible.rights.isNotBlank()) append("##Copyright:\t${bible.rights.oneLine()}\n")
        if (bible.source.isNotBlank()) append("##Source:\t${bible.source.oneLine()}\n")
        for (book in bible.books) append("${book.number}\t${book.name.oneLine()}\t${book.chapters.size}\n")
        append("-----\n")
        for (book in bible.books) {
            for (chapter in book.chapters) appendChapter(bible.language, book, chapter)
        }
    }

    private fun StringBuilder.appendChapter(language: String?, book: SourceBook, chapter: SourceChapter) {
        val isLxxPsalm = language?.uppercase() in LXX_PSALM_LANGUAGES && book.number == PSALMS_BOOK_NUMBER
        val hasStandaloneTitle = isLxxPsalm && chapter.verses.isNotEmpty() &&
            isPsalmSuperscription(chapter.verses.first().text)
        val codeChapter = if (isLxxPsalm) lxxToHebrewPsalm(chapter.number) else chapter.number

        for (verse in chapter.verses) {
            val codeVerse = if (hasStandaloneTitle) verse.number - 1 else verse.number
            append('B').append(pad(book.number))
            append('C').append(pad(codeChapter))
            append('V').append(pad(codeVerse))
            append('\t').append(book.number)
            append('\t').append(chapter.number)
            append('\t').append(verse.number)
            append('\t').append(verse.text.oneLine())
            append('\n')
        }
    }

    /** `%03d`, which common Kotlin has no `format` for. */
    private fun pad(value: Int): String = value.toString().padStart(CODE_DIGITS, '0')

    private const val CODE_DIGITS = 3

    internal fun lxxToHebrewPsalm(lxxChapter: Int): Int = when {
        lxxChapter in LXX_PSALM_EXCEPTIONS -> LXX_PSALM_EXCEPTIONS.getValue(lxxChapter)
        LXX_PSALMS_ONE_BEHIND.any { lxxChapter in it } -> lxxChapter + 1
        else -> lxxChapter
    }

    /**
     * Whether a psalm's first verse is only its title — "Псалом Давида." — rather than a verse
     * that opens with one ("«Псалом Давида.» Блажен муж…").
     */
    internal fun isPsalmSuperscription(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > MAX_SUPERSCRIPTION_LENGTH) return false
        val withoutBrackets = trimmed.replace(BRACKETED_TITLE, "").trim()
        if (withoutBrackets.length > MAX_TITLE_REMAINDER) return false
        val hasTitle = TITLE_PATTERNS.any { trimmed.contains(it, ignoreCase = true) }
        return hasTitle || withoutBrackets.isEmpty()
    }

    /**
     * Header values and verse texts are tab-separated single lines, so a newline or tab inside one
     * would corrupt the file. The desktop does this for header values only; verses are included
     * here because a Zefania verse's text is the whole of its element, line breaks and all.
     */
    private fun String.oneLine(): String =
        replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim()
}
