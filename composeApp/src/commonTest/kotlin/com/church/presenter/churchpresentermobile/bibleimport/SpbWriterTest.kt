package com.church.presenter.churchpresentermobile.bibleimport

import com.church.presenter.churchpresentermobile.library.SpbParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpbWriterTest {

    private fun bible(language: String? = "ENG", books: List<SourceBook>) = SourceBible(
        name = "King James Version",
        description = "Authorised\tVersion",
        language = language,
        books = books,
        rights = "Public Domain",
    )

    private val genesis = SourceBook(
        1, "Genesis",
        listOf(SourceChapter(1, listOf(SourceVerse(1, "In the beginning"), SourceVerse(2, "And the earth\nwas")))),
    )

    @Test
    fun `the header book list rule and verses are laid out as the desktop writes them`() {
        val text = SpbWriter.write(bible(books = listOf(genesis)))
        assertEquals(
            listOf(
                "##spDataVersion:\t1",
                "##Title:\tKing James Version",
                "##Abbreviation:\tKJV",
                "##Information:\tAuthorised Version",
                "##RightToLeft:\t",
                "##Copyright:\tPublic Domain",
                "1\tGenesis\t1",
                "-----",
                "B001C001V001\t1\t1\t1\tIn the beginning",
                "B001C001V002\t1\t1\t2\tAnd the earth was",
            ),
            text.lines().dropLast(1),
        )
    }

    @Test
    fun `a title and abbreviation chosen on the convert screen replace the file's`() {
        val text = SpbWriter.write(bible(books = listOf(genesis)), title = "Біблія (Огієнко)", abbreviation = "UKR_OGI")
        assertTrue("##Title:\tБіблія (Огієнко)" in text.lines())
        assertTrue("##Abbreviation:\tUKR_OGI" in text.lines())
    }

    @Test
    fun `a right-to-left language is flagged`() {
        assertTrue("##RightToLeft:\t1" in SpbWriter.write(bible("HEB", listOf(genesis))).lines())
    }

    @Test
    fun `what the writer writes the app's own reader reads back`() {
        val parsed = SpbParser.parse(SpbWriter.write(bible(books = listOf(genesis))))
        assertEquals("King James Version", parsed.title)
        assertEquals(2, parsed.verseCount)
        assertEquals("And the earth was", parsed.chapter(1, 1)[1].text)
    }

    @Test
    fun `a Septuagint psalm is coded in Hebrew numbering`() {
        val psalm = SourceBook(19, "Псалтирь", listOf(SourceChapter(22, listOf(SourceVerse(2, "Господь пасет меня")))))
        val line = SpbWriter.write(bible("RUS", listOf(psalm))).lines().last { it.startsWith("B") }
        assertEquals("B019C023V002\t19\t22\t2\tГосподь пасет меня", line)
    }

    @Test
    fun `a Septuagint psalm whose first verse is only its title codes it as verse zero`() {
        val psalm = SourceBook(
            19, "Псалтирь",
            listOf(SourceChapter(3, listOf(SourceVerse(1, "Псалом Давида."), SourceVerse(2, "Господи! как умножились враги мои!")))),
        )
        val codes = SpbWriter.write(bible("RUS", listOf(psalm))).lines().filter { it.startsWith("B") }.map { it.take(12) }
        // Psalm 3 is numbered alike in both traditions; only the verse shifts.
        assertEquals(listOf("B019C003V000", "B019C003V001"), codes)
    }

    @Test
    fun `the Psalms numbering exceptions match the desktop`() {
        assertEquals(9, SpbWriter.lxxToHebrewPsalm(9))
        assertEquals(11, SpbWriter.lxxToHebrewPsalm(10))
        assertEquals(114, SpbWriter.lxxToHebrewPsalm(113))
        assertEquals(116, SpbWriter.lxxToHebrewPsalm(115))
        assertEquals(147, SpbWriter.lxxToHebrewPsalm(146))
        assertEquals(150, SpbWriter.lxxToHebrewPsalm(150))
    }

    @Test
    fun `a psalm verse that opens with a bracketed title and then content is not a superscription`() {
        assertFalse(
            SpbWriter.isPsalmSuperscription(
                "«Псалом Давида.» Блажен муж, который не ходит на совет нечестивых и не стоит на пути грешных",
            ),
        )
        assertTrue(SpbWriter.isPsalmSuperscription("Псалом Давида, когда он бежал."))
    }
}

class BibleNamingTest {

    @Test
    fun `file stems follow the desktop's rules`() {
        assertEquals("ENG_ACV", BibleNaming.fileStem("ENG", "ACV"))
        assertEquals("SWA", BibleNaming.fileStem("SWA", "SWA"))
        assertEquals("AFR_3353", BibleNaming.fileStem("AFR", "AFR3353"))
        assertEquals("CZE_CSP", BibleNaming.fileStem("cze", "ČSP"))
        assertEquals("UND_X", BibleNaming.fileStem("", "x"))
    }

    @Test
    fun `a taken stem gets a numbered suffix`() {
        assertEquals("ENG_KJV_3", BibleNaming.deduplicate("ENG_KJV", setOf("ENG_KJV", "ENG_KJV_2")))
    }

    @Test
    fun `the abbreviation is the initials of the name`() {
        assertEquals("KJV", BibleNaming.abbreviation("King James  Version"))
    }

    @Test
    fun `a typed stem keeps its underscore and loses what a file name cannot carry`() {
        assertEquals("UKR_OGI", BibleNaming.typedStem("ukr_ogi/.."))
    }

    @Test
    fun `a Russian verse patch applies only to the verse and language it names`() {
        val truncated = "Сына одной женщины из дочерей Дановых... госпо"
        assertTrue(VersePatches.apply(truncated, "RUS", 14, 2, 14).endsWith("отца твоего."))
        assertEquals(truncated, VersePatches.apply(truncated, "UKR", 14, 2, 14))
        assertEquals("any", VersePatches.apply("any", "RUS", 1, 1, 1))
    }
}
