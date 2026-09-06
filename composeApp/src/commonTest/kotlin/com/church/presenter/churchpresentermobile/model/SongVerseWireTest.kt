package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Every spelling a desktop has ever used for a song's verses.
 *
 * [SongVerse] carries the same three facts — which verse it is, what it is
 * called, and the words — under nineteen field names between them, because the
 * desktop's song serialiser has changed shape several times and a church may be
 * running any of those versions. The accessors pick the first spelling present.
 *
 * A wrong `@SerialName` does not fail: the field is simply absent, the accessor
 * falls through, and the operator gets a slide with no words on it in front of a
 * congregation. Nothing else in the app would show it, so each alias is decoded
 * here on its own.
 */
class SongVerseWireTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(body: String) = json.decodeFromString(SongVerse.serializer(), body)

    // ── Which verse it is ────────────────────────────────────────────────

    @Test
    fun `every spelling of the verse number decodes`() {
        assertEquals(2, decode("""{"number":2}""").number)
        assertEquals(2, decode("""{"verse":2}""").verse)
        assertEquals(2, decode("""{"index":2}""").index)
        assertEquals(2, decode("""{"verse-number":2}""").verseNumberKebab)
        assertEquals(2, decode("""{"verseNumber":2}""").verseNumberCamel)
        assertEquals(2, decode("""{"slide-index":2}""").slideIndex)
    }

    @Test
    fun `whichever spelling arrives, the verse is labelled by its number`() {
        for (key in listOf("number", "verse", "verse-number", "verseNumber", "index")) {
            assertEquals("2", decode("""{"$key":2}""").displayLabel, key)
        }
    }

    // ── What it is called ────────────────────────────────────────────────

    @Test
    fun `every spelling of the label decodes`() {
        assertEquals("Chorus", decode("""{"label":"Chorus"}""").label)
        assertEquals("Chorus", decode("""{"type":"Chorus"}""").type)
        assertEquals("Chorus", decode("""{"name":"Chorus"}""").name)
    }

    @Test
    fun `whichever spelling arrives, the section is named`() {
        for (key in listOf("label", "type", "name")) {
            assertEquals("Chorus", decode("""{"$key":"Chorus"}""").displayLabel, key)
        }
    }

    @Test
    fun `a name wins over a number`() {
        // "Chorus" is more use to an operator than "2".
        assertEquals("Chorus", decode("""{"label":"Chorus","number":2}""").displayLabel)
    }

    @Test
    fun `a blank name falls through to the next spelling`() {
        // A desktop that sends an empty label rather than omitting it would
        // otherwise leave the section list with a gap in it.
        assertEquals("Chorus", decode("""{"label":"","type":"Chorus"}""").displayLabel)
    }

    @Test
    fun `a verse with nothing to call it has no label rather than a blank one`() {
        assertNull(decode("""{"text":"words"}""").displayLabel)
    }

    // ── The words ────────────────────────────────────────────────────────

    @Test
    fun `every spelling of the lines list decodes`() {
        assertEquals(listOf("one", "two"), decode("""{"lines":["one","two"]}""").lines)
        assertEquals(listOf("one"), decode("""{"verse-lines":["one"]}""").verseLinesKebab)
        assertEquals(listOf("one"), decode("""{"verseLines":["one"]}""").verseLinesCamel)
    }

    @Test
    fun `every spelling of the whole text decodes`() {
        assertEquals("words", decode("""{"text":"words"}""").text)
        assertEquals("words", decode("""{"content":"words"}""").content)
        assertEquals("words", decode("""{"lyrics":"words"}""").lyrics)
        assertEquals("words", decode("""{"words":"words"}""").words)
        assertEquals("words", decode("""{"body":"words"}""").body)
        assertEquals("words", decode("""{"verse-text":"words"}""").verseTextKebab)
        assertEquals("words", decode("""{"verseText":"words"}""").verseTextCamel)
        assertEquals("words", decode("""{"slide-text":"words"}""").slideText)
        assertEquals("words", decode("""{"lyric-text":"words"}""").lyricText)
    }

    @Test
    fun `whichever spelling arrives, the words reach the slide`() {
        val spellings = listOf(
            "text", "content", "lyrics", "words", "body",
            "verse-text", "verseText", "slide-text", "lyric-text",
        )
        for (key in spellings) {
            assertEquals("Amazing grace", decode("""{"$key":"Amazing grace"}""").displayText, key)
        }
    }

    @Test
    fun `a lines list is joined with newlines`() {
        // Line breaks are the shape of a verse; joining with spaces would run
        // the whole thing into one paragraph on the audience screen.
        assertEquals(
            "Amazing grace\nhow sweet the sound",
            decode("""{"lines":["Amazing grace","how sweet the sound"]}""").displayText,
        )
    }

    @Test
    fun `a lines list wins over a single blob of text`() {
        assertEquals("one\ntwo", decode("""{"lines":["one","two"],"text":"ignored"}""").displayText)
    }

    @Test
    fun `an empty lines list falls through to the text`() {
        // A desktop sending "lines": [] rather than omitting it would otherwise
        // project a blank slide.
        assertEquals("words", decode("""{"lines":[],"text":"words"}""").displayText)
    }

    @Test
    fun `a blank text falls through to the next spelling`() {
        assertEquals("words", decode("""{"text":"","content":"words"}""").displayText)
    }

    @Test
    fun `a verse with no words at all is empty rather than null`() {
        // The renderer draws this straight; null would be shown as "null".
        assertEquals("", decode("""{"number":1}""").displayText)
    }

    @Test
    fun `a field this build does not know about is ignored`() {
        assertEquals("words", decode("""{"text":"words","somethingNew":{"a":1}}""").displayText)
    }
}
