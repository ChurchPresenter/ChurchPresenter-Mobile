package com.church.presenter.churchpresentermobile.bibleimport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The three dialects as the archives actually publish them: attributes missing, names absent. */
class ReaderEdgeCasesTest {

    private fun zefania(body: String, root: String = """biblename="X"""") = "<XMLBIBLE $root>$body</XMLBIBLE>"

    @Test
    fun `missing or garbled Zefania numbers read as zero rather than failing`() {
        val bible = BibleXmlReader.read(
            zefania("""<BIBLEBOOK><CHAPTER cnumber="x"><VERS>text</VERS></CHAPTER></BIBLEBOOK>"""),
        )
        val book = bible.books.single()
        assertEquals(0, book.number)
        assertEquals(0, book.chapters.single().number)
        assertEquals(0, book.chapters.single().verses.single().number)
        assertEquals("Book 0", book.name)
    }

    @Test
    fun `a Zefania module names itself from its title and failing that is Unknown`() {
        val titled = BibleXmlReader.read(zefania("<INFORMATION><title>From title</title></INFORMATION>", root = ""))
        assertEquals("From title", titled.name)
        assertEquals("Unknown", BibleXmlReader.read(zefania("", root = "")).name)
        assertEquals("From hints", BibleXmlReader.read(zefania(""), ImportHints(name = "From hints")).name)
    }

    @Test
    fun `every INFORMATION field is read and the caller's win`() {
        val info = """<INFORMATION><identifier> ID </identifier><source> src </source><rights>r</rights>
            |<description>d</description><publisher>ignored</publisher></INFORMATION>""".trimMargin()
        val bible = BibleXmlReader.read(zefania(info))
        assertEquals("ID", bible.identifier)
        assertEquals("src", bible.source)
        val hinted = BibleXmlReader.read(zefania(info), ImportHints(identifier = "H", rights = "HR", source = "HS"))
        assertEquals(listOf("H", "HR", "HS"), listOf(hinted.identifier, hinted.rights, hinted.source))
    }

    @Test
    fun `an English book with only a short name uses it and an unknown language falls back to English`() {
        val english = BibleXmlReader.read(
            zefania("""<INFORMATION><language>ENG</language></INFORMATION><BIBLEBOOK bnumber="1" bsname="Gen"/>"""),
        )
        assertEquals("Gen", english.books.single().name)
        val unknown = BibleXmlReader.read(
            zefania("""<INFORMATION><language>XYZ</language></INFORMATION><BIBLEBOOK bnumber="2"/>"""),
        )
        assertEquals("Exodus", unknown.books.single().name)
    }

    @Test
    fun `a caption that is a sentence or has no number is not a book name`() {
        fun named(caption: String) = BibleXmlReader.read(
            zefania(
                """<INFORMATION><language>XYZ</language></INFORMATION><BIBLEBOOK bnumber="1">""" +
                    """<CHAPTER cnumber="1"><CAPTION>$caption</CAPTION></CHAPTER></BIBLEBOOK>""",
            ),
        ).books.single().name
        assertEquals("Genesis", named("Mwanzo"), "no numbering dot: not a name")
        assertEquals("Genesis", named("1. This is a long sentence about the book and its author"))
        assertEquals("Genesis", named("1."))
    }

    @Test
    fun `a chapter outside any book and a caption before any book are ignored`() {
        val bible = BibleXmlReader.read(
            zefania("""<CAPTION>1. Loose</CAPTION><CHAPTER cnumber="1"><VERS vnumber="1">x</VERS></CHAPTER>"""),
        )
        assertTrue(bible.books.isEmpty())
    }

    @Test
    fun `a blank language hint is no language`() {
        assertNull(BibleXmlReader.read(zefania(""), ImportHints(language = "  ")).language)
    }

    @Test
    fun `a Russian module outside a Ukrainian folder stays Russian`() {
        val bible = BibleXmlReader.read(
            zefania("<INFORMATION><language>RUS</language></INFORMATION>"),
            ImportHints(path = "Bibles/RUS/x.zip"),
        )
        assertEquals("RUS", bible.language)
    }

    // ── Holy Bible XML ──────────────────────────────────────────────────────

    @Test
    fun `a Beblia root with no rights or link reads them as blank and an unknown title as no language`() {
        val bible = BibleXmlReader.read(
            """<bible name="Mystery Bible"><book number="1"><chapter number="1">""" +
                """<verse number="1">a</verse></chapter></book></bible>""",
        )
        assertEquals("Mystery Bible", bible.name)
        assertEquals("", bible.rights)
        assertNull(bible.language)
        assertEquals("Genesis", bible.books.single().name)
    }

    @Test
    fun `a Beblia verse or book with no number reads as zero and a book zero is dropped`() {
        val bible = BibleXmlReader.read(
            """<bible translation="English"><book><chapter><verse>a</verse></chapter></book></bible>""",
        )
        assertTrue(bible.books.isEmpty())
    }

    // ── USFX ────────────────────────────────────────────────────────────────

    @Test
    fun `an unknown USFX book and text outside any verse are skipped`() {
        val bible = BibleXmlReader.read(
            """<usfx><book id="XXA"><c id="1"/><v id="1"/>apocryphal<ve/></book>""" +
                """<book id="JHN">intro text<c id="1"/><v id="1"/>In the beginning was the Word.<ve/></book></usfx>""",
            ImportHints(language = "ENG"),
        )
        assertEquals(listOf(43), bible.books.map { it.number })
        assertEquals("John", bible.books.single().name)
        assertEquals("In the beginning was the Word.", bible.books.single().chapters.single().verses.single().text)
    }

    @Test
    fun `a USFX chapter with no number follows the previous one`() {
        val bible = BibleXmlReader.read(
            """<usfx><book id="GEN"><c id="1"/><v id="1"/>a<c id="?"/><v id="1"/>b</book></usfx>""",
        )
        assertEquals(listOf(1, 2), bible.books.single().chapters.map { it.number })
    }

    @Test
    fun `a language code in the file does not override the caller's`() {
        val bible = BibleXmlReader.read(
            """<usfx><languageCode>eng</languageCode></usfx>""",
            ImportHints(language = "UKR"),
        )
        assertEquals("UKR", bible.language)
        assertEquals("Unknown", bible.name)
    }

    @Test
    fun `a skipped element closes around nested elements without a verse inside`() {
        val bible = BibleXmlReader.read(
            """<usfx><book id="GEN"><c id="1"/><v id="1"/>kept""" +
                """<f><fr>1:1</fr><ft>note <b>bold</b></ft></f> too<ve/></book></usfx>""",
        )
        assertEquals("kept too", bible.books.single().chapters.single().verses.single().text)
    }

    @Test
    fun `book names with no code or no label are ignored`() {
        val names = UsfxReader.parseBookNames(
            """<b><book code="" short="x"/><book code="GEN"/><book code="exo" abbr="Ex"/></b>""",
        )
        assertEquals(mapOf("EXO" to "Ex"), names)
    }

    // ── The preview ─────────────────────────────────────────────────────────

    @Test
    fun `the preview falls back to the first verse and is absent for an empty Bible`() {
        val genesisOnly = SourceBible(
            "x",
            "",
            null,
            listOf(SourceBook(1, "Genesis", listOf(SourceChapter(1, listOf(SourceVerse(1, "In the beginning")))))),
        )
        assertEquals("Genesis 1:1" to "In the beginning", genesisOnly.previewVerse())
        val johnWithout316 = SourceBible(
            "x",
            "",
            null,
            listOf(SourceBook(43, "John", listOf(SourceChapter(1, listOf(SourceVerse(1, "Word")))))),
        )
        assertEquals("John 1:1" to "Word", johnWithout316.previewVerse())
        assertNull(SourceBible("x", "", null, emptyList()).previewVerse())
        assertNull(SourceBible("x", "", null, listOf(SourceBook(1, "Genesis", emptyList()))).previewVerse())
    }

    @Test
    fun `slugs fold accents and duplicate stems keep counting`() {
        assertEquals("AAEEIOUNC", BibleNaming.slug("ÁåÉęÍÓÚÑÇ"))
        assertEquals("", BibleNaming.slug(null))
        assertEquals("X_4", BibleNaming.deduplicate("X", setOf("X", "X_2", "X_3")))
        assertEquals("Y", BibleNaming.deduplicate("Y", setOf("X")))
    }
}
