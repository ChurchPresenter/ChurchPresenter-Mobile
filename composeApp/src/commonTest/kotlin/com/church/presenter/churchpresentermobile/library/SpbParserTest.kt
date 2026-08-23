package com.church.presenter.churchpresentermobile.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Reading a `.spb` module, which is what a downloaded Bible arrives as.
 *
 * The fixtures below are the real format, taken from a module the desktop ships.
 */
class SpbParserTest {

    private val module = """
        ##Title: King James Version
        1 Genesis 50
        2 Exodus 40
        -----
        B001C001V001 1 1 1 In the beginning God created the heavens and the earth.
        B001C001V002 1 1 2 Now the earth was formless and void.
        B001C002V001 1 2 1 Thus the heavens and the earth were finished.
        B002C001V001 2 1 1 Now these are the names of the children of Israel.
    """.trimIndent()

    @Test
    fun aModuleParsesToItsBooksAndVerses() {
        val bible = SpbParser.parse(module)

        assertEquals("King James Version", bible.title)
        assertEquals(listOf("Genesis", "Exodus"), bible.books.map { it.displayName })
        assertEquals(50, bible.books.first().totalChapters)
    }

    @Test
    fun versesComeBackByBookAndChapter() {
        val bible = SpbParser.parse(module)

        val genesisOne = bible.chapter(bookNumber = 1, chapter = 1)
        assertEquals(2, genesisOne.size)
        assertEquals(1, genesisOne.first().number)
        assertEquals("In the beginning God created the heavens and the earth.", genesisOne.first().displayText)
        assertEquals(1, bible.chapter(bookNumber = 1, chapter = 2).size)
        assertEquals(1, bible.chapter(bookNumber = 2, chapter = 1).size)
    }

    @Test
    fun aChapterTheModuleDoesNotCarryIsEmptyRatherThanAFailure() {
        val bible = SpbParser.parse(module)

        assertTrue(bible.chapter(bookNumber = 1, chapter = 99).isEmpty())
        assertTrue(bible.chapter(bookNumber = 66, chapter = 1).isEmpty())
    }

    @Test
    fun theDisplayNumbersWinOverTheCodesOwn() {
        // The B###C###V### code carries the module's internal (Hebrew) numbering and the three
        // numbers after it carry what the reader is shown. They disagree on purpose in modules
        // like the Russian synodal text, and filing by the code would land verses in the wrong
        // chapter — so this pins which one is used.
        val bible = SpbParser.parse(
            """
            ##Title: Divergent
            1 Genesis 1
            -----
            B001C001V001 1 3 7 A verse the module shows at 3:7.
            """.trimIndent()
        )

        assertTrue(bible.chapter(bookNumber = 1, chapter = 1).isEmpty())
        val shown = bible.chapter(bookNumber = 1, chapter = 3)
        assertEquals(1, shown.size)
        assertEquals(7, shown.first().number)
    }

    @Test
    fun booksCanBeReadWithoutWalkingTheVerses() {
        // The point of the fast path: fill the book list off a 4.6 MB file without paying for it.
        val books = SpbParser.parseBooks(module)

        assertEquals(listOf("Genesis", "Exodus"), books.map { it.displayName })
    }

    @Test
    fun aTruncatedModuleOpensOnWhatItHas() {
        // A file cut short mid-download must still open on the books that arrived — a Bible tab
        // showing nothing mid-service is worse than one showing Genesis.
        val bible = SpbParser.parse(module.substringBefore("B002C001V001").trim())

        assertEquals(listOf("Genesis", "Exodus"), bible.books.map { it.displayName })
        assertEquals(2, bible.chapter(bookNumber = 1, chapter = 1).size)
        assertTrue(bible.chapter(bookNumber = 2, chapter = 1).isEmpty())
    }

    @Test
    fun junkIsReportedAsEmptyRatherThanParsedIntoNonsense() {
        val bible = SpbParser.parse("<!doctype html><html>404 Not Found</html>")

        assertTrue(bible.isEmpty)
        assertTrue(bible.books.isEmpty())
    }

    @Test
    fun aRealModuleIsNotMistakenForJunk() {
        assertFalse(SpbParser.parse(module).isEmpty)
    }

    @Test
    fun aModuleWithNoTitleLineStillParses() {
        val bible = SpbParser.parse(
            """
            1 Genesis 1
            -----
            B001C001V001 1 1 1 In the beginning.
            """.trimIndent()
        )

        assertEquals("", bible.title)
        assertEquals(1, bible.chapter(bookNumber = 1, chapter = 1).size)
    }

    @Test
    fun theLegacyBareCodeFormJoinsTheLinesUnderIt() {
        // An older module writes the code alone, then the verse text on the lines after it. The
        // desktop still reads these, so a module that has not been re-exported must open here too.
        val bible = SpbParser.parse(
            """
            ##Title: Legacy
            1 Genesis 1
            -----
            B001C001V001
            In the beginning God created
            the heavens and the earth.
            B001C001V002
            And the earth was without form.
            """.trimIndent()
        )

        val verses = bible.chapter(bookNumber = 1, chapter = 1)
        assertEquals(2, verses.size)
        assertEquals("In the beginning God created\nthe heavens and the earth.", verses.first().displayText)
        // The last verse has no code line after it — it must still be flushed at end of file.
        assertEquals("And the earth was without form.", verses.last().displayText)
    }

    @Test
    fun twoLinesSharingAVerseNumberBecomeOneVerse() {
        // Synodal-style modules split a verse across two records. The desktop merges them on
        // read; not merging here shows the operator a doubled verse 1.
        val bible = SpbParser.parse(
            """
            ##Title: Split
            1 Genesis 1
            -----
            B001C001V001 1 1 1 In the beginning
            B001C001V001 1 1 1 God created the heavens.
            """.trimIndent()
        )

        val verses = bible.chapter(bookNumber = 1, chapter = 1)
        assertEquals(1, verses.size)
        assertEquals("In the beginning God created the heavens.", verses.first().displayText)
    }

    @Test
    fun aModuleWithNoSeparatorStillEndsItsHeaderAtTheFirstVerse() {
        val bible = SpbParser.parse(
            """
            ##Title: No rule
            1 Genesis 1
            B001C001V001 1 1 1 In the beginning.
            """.trimIndent()
        )

        assertEquals(listOf("Genesis"), bible.books.map { it.displayName })
        // The B line ends the header but is still a verse — losing it is the easy mistake here.
        assertEquals(1, bible.chapter(bookNumber = 1, chapter = 1).size)
    }

    @Test
    fun aBookPresentOnlyInTheVersesIsStillListed() {
        val bible = SpbParser.parse(
            """
            ##Title: Headerless
            -----
            B001C001V001 1 1 1 In the beginning.
            B002C001V001 2 1 1 These are the names.
            """.trimIndent()
        )

        assertEquals(2, bible.books.size)
        assertEquals(listOf(1, 2), bible.books.map { it.bookId })
    }

    @Test
    fun otherMetadataLinesAreIgnored() {
        val bible = SpbParser.parse(
            """
            ##Title: Meta
            ##Copyright: Public Domain
            ##Language: en
            1 Genesis 1
            -----
            B001C001V001 1 1 1 In the beginning.
            """.trimIndent()
        )

        assertEquals("Meta", bible.title)
        assertEquals(listOf("Genesis"), bible.books.map { it.displayName })
    }

    @Test
    fun withNoTitleTheFileNameStandsIn() {
        val bible = SpbParser.parse("1 Genesis 1\n-----\nB001C001V001 1 1 1 x", fileName = "en_KJV.spb")

        assertEquals("en_KJV", bible.title)
    }

    @Test
    fun verseCountIsWhatTheOperatorIsToldAfterADownload() {
        assertEquals(4, SpbParser.parse(module).verseCount)
    }

    @Test
    fun verseTextKeepsItsInternalSpacingButNotItsEdges() {
        val bible = SpbParser.parse(
            """
            ##Title: Spacing
            1 Genesis 1
            -----
            B001C001V001 1 1 1    And God said,  Let there be light.
            """.trimIndent()
        )

        assertEquals("And God said,  Let there be light.", bible.chapter(1, 1).first().displayText)
    }
}
