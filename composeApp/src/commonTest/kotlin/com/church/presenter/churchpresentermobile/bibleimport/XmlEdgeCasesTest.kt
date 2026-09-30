package com.church.presenter.churchpresentermobile.bibleimport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The XML reader and the text decoder on the malformed and unusual input real archives contain. */
class XmlEdgeCasesTest {

    private fun readAll(xml: String): List<String> {
        val reader = XmlPullReader(xml)
        val out = mutableListOf<String>()
        while (true) {
            when (reader.next()) {
                XmlEvent.START -> out += "<${reader.name}>"
                XmlEvent.END -> out += "</${reader.name}>"
                XmlEvent.TEXT -> out += reader.text
                XmlEvent.EOF -> return out
            }
        }
    }

    @Test
    fun `each kind of unterminated markup is reported rather than read past`() {
        listOf(
            "<a><![CDATA[never closed",
            "<a></a",
            "<a><!-- never closed",
            "<?xml version='1.0'",
            "<!DOCTYPE bible [<!ENTITY x 'y'>",
            "<a b='never closed></a>",
            "<a b",
        ).forEach { xml -> assertFailsWith<XmlFormatException>(xml) { readAll(xml) } }
    }

    @Test
    fun `a stray angle bracket or slash is refused`() {
        assertFailsWith<XmlFormatException> { readAll("< a/>") }
        assertFailsWith<XmlFormatException> { readAll("<a /x>") }
    }

    @Test
    fun `a bare attribute and single-quoted values are read`() {
        val reader = XmlPullReader("<verse hidden number='3'>x</verse>")
        reader.next()
        assertEquals("", reader.attribute("hidden"))
        assertEquals("3", reader.attribute("number"))
    }

    @Test
    fun `text running to the end of the document is still text`() {
        assertEquals(listOf("<a>", "</a>", "tail"), readAll("<a/>tail"))
    }

    @Test
    fun `a doctype's internal subset with nested brackets is skipped whole`() {
        assertEquals(listOf("<r>", "</r>"), readAll("<!DOCTYPE r [<!ELEMENT r (#PCDATA)> [x] ]><r/>"))
    }

    @Test
    fun `references that do not decode are left as written`() {
        assertEquals(
            "&#xZZ; &#99999999; &verylongentityname; & alone",
            XmlPullReader.decodeEntities("&#xZZ; &#99999999; &verylongentityname; & alone"),
        )
        assertEquals("A B", XmlPullReader.decodeEntities("&#X41;&nbsp;B").replace(' ', ' '))
    }

    // ── Decoding bytes to text ──────────────────────────────────────────────

    @Test
    fun `UTF-16 big-endian with its mark decodes`() {
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0, '<'.code.toByte(), 0x04, 0x11)
        assertEquals("<Б", XmlText.decode(bytes))
    }

    @Test
    fun `a declaration without an encoding is UTF-8`() {
        assertEquals(
            "<?xml version=\"1.0\"?><a>é</a>",
            XmlText.decode("<?xml version=\"1.0\"?><a>é</a>".encodeToByteArray()),
        )
    }

    @Test
    fun `the spellings real files use for each encoding are all recognised`() {
        fun declared(encoding: String, vararg body: Int) =
            XmlText.decode(
                "<?xml version=\"1.0\" encoding=\"$encoding\"?>".encodeToByteArray() +
                    body.map { it.toByte() }.toByteArray(),
            )
                .substringAfter("?>")
        assertEquals("é", declared("utf8", 0xC3, 0xA9))
        assertEquals("é", declared("latin1", 0xE9))
        assertEquals("A", declared("US-ASCII", 0x41))
        assertEquals("Ђ", declared("cp1251", 0x80))
        assertEquals("їA", declared("windows-1251", 0xBF, 0x41))
        assertEquals("é", declared("cp1252", 0xE9))
    }

    @Test
    fun `UTF-16 with no mark is recognised by the zero byte beside its first bracket`() {
        val text = "<a>Бог</a>"
        val bigEndian = text.flatMap { listOf((it.code shr 8).toByte(), it.code.toByte()) }.toByteArray()
        val littleEndian = text.flatMap { listOf(it.code.toByte(), (it.code shr 8).toByte()) }.toByteArray()
        assertEquals(text, XmlText.decode(bigEndian))
        assertEquals(text, XmlText.decode(littleEndian))
    }
}
