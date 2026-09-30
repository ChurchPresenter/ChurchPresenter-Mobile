package com.church.presenter.churchpresentermobile.bibleimport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The three dialects, read the way the desktop's converters read them. */
class BibleXmlReaderTest {

    private val zefania = """
        <?xml version="1.0" encoding="utf-8"?>
        <XMLBIBLE biblename="King James Version">
          <INFORMATION>
            <title>KJV</title>
            <description>The Authorised
              Version</description>
            <language>ENG</language>
            <rights>Public Domain</rights>
          </INFORMATION>
          <BIBLEBOOK bnumber="1" bname="Genesis">
            <CHAPTER cnumber="1">
              <CAPTION>1. Genesis</CAPTION>
              <VERS vnumber="1">In the beginning God created
                the heaven and the earth.</VERS>
              <VERS vnumber="2">And the earth was <STYLE fs="italic">without</STYLE> form.</VERS>
            </CHAPTER>
          </BIBLEBOOK>
          <BIBLEBOOK bnumber="43" bname="John">
            <CHAPTER cnumber="3"><VERS vnumber="16">For God so loved the world.</VERS></CHAPTER>
          </BIBLEBOOK>
        </XMLBIBLE>
    """.trimIndent()

    @Test
    fun `the root element decides the format`() {
        assertEquals(BibleXmlFormat.ZEFANIA, BibleXmlReader.detect(zefania))
        assertEquals(BibleXmlFormat.USFX, BibleXmlReader.detect("<?xml version=\"1.0\"?><usfx><book id=\"GEN\"/></usfx>"))
        assertEquals(BibleXmlFormat.BEBLIA, BibleXmlReader.detect("<bible translation=\"English KJV\"></bible>"))
    }

    @Test
    fun `a bible root that names nothing is not Beblia`() {
        assertNull(BibleXmlReader.detect("<bible></bible>"))
    }

    @Test
    fun `an unrelated XML file or plain text is refused`() {
        assertNull(BibleXmlReader.detect("<html><body/></html>"))
        assertNull(BibleXmlReader.detect("just some text"))
        assertFailsWith<NotABibleException> { BibleXmlReader.read("<songs/>") }
    }

    @Test
    fun `Zefania metadata books chapters and verses are read`() {
        val bible = BibleXmlReader.read(zefania)
        assertEquals("King James Version", bible.name)
        assertEquals("ENG", bible.language)
        assertEquals("The Authorised Version", bible.description)
        assertEquals("Public Domain", bible.rights)
        assertEquals(listOf(1, 43), bible.books.map { it.number })
        assertEquals(3, bible.verseCount)
        assertEquals(
            "In the beginning God created the heaven and the earth.",
            bible.books[0].chapters[0].verses[0].text,
        )
    }

    @Test
    fun `a Zefania verse is the text of everything inside it`() {
        val verse = BibleXmlReader.read(zefania).books[0].chapters[0].verses[1]
        assertEquals("And the earth was without form.", verse.text)
    }

    @Test
    fun `an English Zefania module keeps its own book names`() {
        val xml = zefania.replace("bname=\"Genesis\"", "bname=\"The First Book of Moses\"")
        assertEquals("The First Book of Moses", BibleXmlReader.read(xml).books[0].name)
    }

    @Test
    fun `a language with a table uses the table over the module's bname`() {
        val xml = zefania.replace("<language>ENG</language>", "<language>RUS</language>")
        assertEquals("Бытие", BibleXmlReader.read(xml).books[0].name)
    }

    @Test
    fun `a language with no table takes the name from the first caption`() {
        val xml = zefania.replace("<language>ENG</language>", "<language>XYZ</language>")
            .replace("<CAPTION>1. Genesis</CAPTION>", "<CAPTION>1. Mwanzo wa Kitabu</CAPTION>")
        assertEquals("Mwanzo wa Kitabu", BibleXmlReader.read(xml).books[0].name)
    }

    @Test
    fun `a Russian declaration from the Ukrainian folder is read as Ukrainian`() {
        val xml = zefania.replace("<language>ENG</language>", "<language>RUS</language>")
        val bible = BibleXmlReader.read(xml, ImportHints(path = "Bibles/UKR/SF_2009_UKR_OGI.zip"))
        assertEquals("UKR", bible.language)
        assertEquals("Буття", bible.books[0].name)
    }

    @Test
    fun `a module that declares no language takes the caller's`() {
        val xml = zefania.replace("<language>ENG</language>", "")
        assertEquals("DEU", BibleXmlReader.read(xml, ImportHints(language = "deu")).language)
    }

    @Test
    fun `progress climbs to one and ends on the full count`() {
        val seen = mutableListOf<ConvertProgress>()
        BibleXmlReader.read(zefania) { seen += it }
        assertEquals(ConvertProgress(1f, books = 2, verses = 3), seen.last())
        assertEquals(seen.map { it.fraction }.sorted(), seen.map { it.fraction })
        assertEquals(seen.map { it.verses }.sorted(), seen.map { it.verses })
    }

    @Test
    fun `the preview is John 3 16 when the Bible has it`() {
        assertEquals(
            "John 3:16" to "For God so loved the world.",
            BibleXmlReader.read(zefania).previewVerse(),
        )
    }

    private val beblia = """
        <bible translation="Russian Synodal" status="Public Domain" link="https://example.org">
          <testament name="Old">
            <book number="1"><chapter number="1"><verse number="1">В начале сотворил Бог небо и землю.</verse></chapter></book>
            <book number="99"><chapter number="1"><verse number="1">Apocrypha</verse></chapter></book>
          </testament>
        </bible>
    """.trimIndent()

    @Test
    fun `Beblia takes its title rights and source from the root`() {
        val bible = BibleXmlReader.read(beblia)
        assertEquals("Russian Synodal", bible.name)
        assertEquals("Public Domain", bible.rights)
        assertEquals("https://example.org", bible.source)
    }

    @Test
    fun `Beblia with no language reads it off the title and names books from the table`() {
        val bible = BibleXmlReader.read(beblia)
        assertEquals("RUS", bible.language)
        assertEquals("Бытие", bible.books.single().name)
    }

    @Test
    fun `a Beblia book outside the canon is dropped`() {
        assertEquals(listOf(1), BibleXmlReader.read(beblia).books.map { it.number })
    }

    @Test
    fun `what the catalogue knows wins over the Beblia root`() {
        val bible = BibleXmlReader.read(beblia, ImportHints(language = "ukr", name = "Named by catalogue", rights = "CC"))
        assertEquals("Named by catalogue", bible.name)
        assertEquals("UKR", bible.language)
        assertEquals("CC", bible.rights)
        assertEquals("Буття", bible.books.single().name)
    }

    private val usfx = """
        <?xml version="1.0" encoding="utf-8"?>
        <usfx xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <languageCode>eng</languageCode>
          <book id="GEN">
            <id id="GEN">World English Bible</id>
            <h>Genesis</h>
            <toc level="1">The First Book of Moses</toc>
            <c id="1"/>
            <s>The Creation</s>
            <p><v id="1"/>In the beginning, God<f caller="+"><fr>1:1</fr> The Hebrew word</f> created
              the heavens and the earth.<ve/>
            <v id="2"/>The earth was formless.<ve/></p>
            <d><v id="3"/>A psalm title in a descriptor.<ve/></d>
            <f><v id="4"/>Verse inside a footnote element.</f>
            <v id="5"/><f>Only a footnote here.</f><ve/>
            <v id="6-7"/>A bridged verse.<ve/>
          </book>
          <book id="MAT"><c id="17"/><p><v id="1"/>After six days.<ve/></p></book>
        </usfx>
    """.trimIndent()

    @Test
    fun `USFX text is gathered between milestones with footnotes and headings dropped`() {
        val verses = BibleXmlReader.read(usfx).books[0].chapters[0].verses
        assertEquals("In the beginning, God created the heavens and the earth.", verses[0].text)
        assertEquals("The earth was formless.", verses[1].text)
    }

    @Test
    fun `line wrapping collapses but a typographic thin space survives`() {
        assertEquals("walk.’ ” next", "  walk.’ ”\n    next ".normalizeSpace())
    }

    @Test
    fun `a USFX descriptor is scripture and kept`() {
        assertEquals("A psalm title in a descriptor.", BibleXmlReader.read(usfx).books[0].chapters[0].verses[2].text)
    }

    @Test
    fun `a skipped element with a verse marker inside is walked rather than dropped`() {
        val verse = BibleXmlReader.read(usfx).books[0].chapters[0].verses.first { it.number == 4 }
        assertEquals("Verse inside a footnote element.", verse.text)
    }

    @Test
    fun `a USFX verse that is only a footnote is left out`() {
        assertTrue(BibleXmlReader.read(usfx).books[0].chapters[0].verses.none { it.number == 5 })
    }

    @Test
    fun `a bridged USFX verse is stored under its first number`() {
        assertTrue(BibleXmlReader.read(usfx).books[0].chapters[0].verses.any { it.number == 6 })
    }

    @Test
    fun `USFX books are numbered from their codes and read their language from the file`() {
        val bible = BibleXmlReader.read(usfx)
        assertEquals(listOf(1, 40), bible.books.map { it.number })
        assertEquals("ENG", bible.language)
        assertEquals("Genesis", bible.books[0].name)
    }

    @Test
    fun `BookNames xml names the USFX books in the translation's own language`() {
        val names = UsfxReader.parseBookNames(
            """<BookNames><book code="GEN" abbr="Gen" short="Буття" long="Перша книга Мойсеєва"/></BookNames>""",
        )
        assertEquals(mapOf("GEN" to "Буття"), names)
        assertEquals("Буття", BibleXmlReader.read(usfx, ImportHints(bookNames = names)).books[0].name)
    }
}
