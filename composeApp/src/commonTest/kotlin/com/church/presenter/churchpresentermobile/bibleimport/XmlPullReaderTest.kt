package com.church.presenter.churchpresentermobile.bibleimport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class XmlPullReaderTest {

    /** Every event as a readable token: `<a k=v>`, `</a>`, `"text"`. */
    private fun events(xml: String, vararg attributes: String): List<String> {
        val reader = XmlPullReader(xml)
        val out = mutableListOf<String>()
        while (true) {
            when (reader.next()) {
                XmlEvent.START -> out += "<${reader.name}" +
                    attributes.mapNotNull { key -> reader.attribute(key)?.let { " $key=$it" } }.joinToString("") + ">"
                XmlEvent.END -> out += "</${reader.name}>"
                XmlEvent.TEXT -> out += "\"${reader.text}\""
                XmlEvent.EOF -> return out
            }
        }
    }

    @Test
    fun `elements attributes and text are reported in document order`() {
        assertEquals(
            listOf("<BIBLEBOOK bnumber=1>", "<VERS vnumber=2>", "\"Hi\"", "</VERS>", "</BIBLEBOOK>"),
            events("""<BIBLEBOOK bnumber="1"><VERS vnumber='2'>Hi</VERS></BIBLEBOOK>""", "bnumber", "vnumber"),
        )
    }

    @Test
    fun `a self-closing element is a start followed by an end`() {
        assertEquals(listOf("<v id=1>", "</v>", "\"text\""), events("""<v id="1"/>text""", "id"))
    }

    @Test
    fun `the declaration comments and doctype are skipped`() {
        val xml = """<?xml version="1.0"?><!-- a comment --><!DOCTYPE x [<!ENTITY a "b">]><root/>"""
        assertEquals(listOf("<root>", "</root>"), events(xml))
    }

    @Test
    fun `entities and character references are decoded in text and attributes`() {
        assertEquals(
            listOf("<a t=x&y>", "\"<б> ф & 😀 'q'\"", "</a>"),
            events("""<a t="x&amp;y">&lt;&#1073;&gt; &#x444; &amp; &#128512; &apos;q&apos;</a>""", "t"),
        )
    }

    @Test
    fun `an unknown entity is left as written rather than dropped`() {
        assertEquals("a &unknown; b", XmlPullReader.decodeEntities("a &unknown; b"))
    }

    @Test
    fun `CDATA is text`() {
        assertEquals(listOf("<a>", "\"1 < 2\"", "</a>"), events("<a><![CDATA[1 < 2]]></a>"))
    }

    @Test
    fun `a namespace prefix is dropped from element names`() {
        assertEquals(listOf("<book>", "</book>"), events("<u:book></u:book>"))
    }

    @Test
    fun `mark and restore return to the same place`() {
        val reader = XmlPullReader("<a><b/><c/></a>")
        reader.next()
        val mark = reader.mark()
        reader.next()
        assertEquals("b", reader.name)
        reader.next()
        reader.next()
        assertEquals("c", reader.name)
        reader.restore(mark)
        reader.next()
        assertEquals("b", reader.name)
        assertEquals(XmlEvent.END, reader.next())
    }

    @Test
    fun `an attribute the element does not carry is null`() {
        val reader = XmlPullReader("<a b=\"1\"/>")
        reader.next()
        assertNull(reader.attribute("c"))
    }

    @Test
    fun `an unterminated tag is refused`() {
        assertFailsWith<XmlFormatException> { events("<a b=\"1\"") }
    }

    @Test
    fun `an unquoted attribute is refused`() {
        assertFailsWith<XmlFormatException> { events("<a b=1></a>") }
    }
}

class XmlTextTest {

    @Test
    fun `UTF-8 is the default`() {
        assertEquals("<a>Бог</a>", XmlText.decode("<a>Бог</a>".encodeToByteArray()))
    }

    @Test
    fun `a UTF-8 byte-order mark is dropped`() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "<a/>".encodeToByteArray()
        assertEquals("<a/>", XmlText.decode(bytes))
    }

    @Test
    fun `UTF-16 little-endian with its mark decodes`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), '<'.code.toByte(), 0, 'a'.code.toByte(), 0, 0x11, 0x04)
        assertEquals("<aБ", XmlText.decode(bytes))
    }

    @Test
    fun `a declared windows-1251 file decodes its Cyrillic`() {
        val declaration = """<?xml version="1.0" encoding="windows-1251"?><a>""".encodeToByteArray()
        // "Бог ё №" in windows-1251.
        val body = byteArrayOf(0xC1.toByte(), 0xEE.toByte(), 0xE3.toByte(), 0x20, 0xB8.toByte(), 0x20, 0xB9.toByte())
        assertEquals("""<?xml version="1.0" encoding="windows-1251"?><a>Бог ё №""", XmlText.decode(declaration + body))
    }

    @Test
    fun `a declared Latin-1 file decodes its accents`() {
        val declaration = """<?xml version="1.0" encoding="ISO-8859-1"?>""".encodeToByteArray()
        assertEquals("é", XmlText.decode(declaration + byteArrayOf(0xE9.toByte())).substringAfter("?>"))
    }

    @Test
    fun `windows-1252 decodes its curly quotes`() {
        val declaration = """<?xml version="1.0" encoding="windows-1252"?>""".encodeToByteArray()
        val body = byteArrayOf(0x93.toByte(), 'x'.code.toByte(), 0x94.toByte())
        assertEquals("“x”", XmlText.decode(declaration + body).substringAfter("?>"))
    }

    @Test
    fun `an encoding with no table is refused by name`() {
        val bytes = """<?xml version="1.0" encoding="Shift_JIS"?><a/>""".encodeToByteArray()
        val error = assertFailsWith<UnsupportedEncodingException> { XmlText.decode(bytes) }
        assertEquals("Shift_JIS", error.encoding)
    }
}
