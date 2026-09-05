package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.SectionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChordProParserTest {

    private fun parse(text: String, fallbackTitle: String = "") =
        ChordProParser.parse(text, id = "test-id", fallbackTitle = fallbackTitle)

    // ── Metadata ─────────────────────────────────────────────────────────

    @Test
    fun `title and artist directives are read`() {
        val song = parse(
            """
            {title: Amazing Grace}
            {artist: John Newton}
            Amazing grace, how sweet the sound
            """.trimIndent()
        )

        assertEquals("Amazing Grace", song.title)
        assertEquals("John Newton", song.author)
    }

    @Test
    fun `short directive aliases work`() {
        val song = parse(
            """
            {t: Amazing Grace}
            {st: John Newton}
            Words
            """.trimIndent()
        )

        assertEquals("Amazing Grace", song.title)
        assertEquals("John Newton", song.author)
    }

    @Test
    fun `songbook number and copyright are read`() {
        val song = parse(
            """
            {title: Amazing Grace}
            {songbook: Hymns}
            {number: 42}
            {ccli: 12345}
            Words
            """.trimIndent()
        )

        assertEquals("Hymns", song.bookName)
        assertEquals("42", song.number)
        assertEquals("12345", song.copyright)
    }

    /** A file's name is usually the right title when it carries no directive. */
    @Test
    fun `a missing title falls back to the file name`() {
        val song = parse("Just some words", fallbackTitle = "Amazing Grace")
        assertEquals("Amazing Grace", song.title)
    }

    @Test
    fun `a title directive beats the file name`() {
        val song = parse("{title: Real Title}\nWords", fallbackTitle = "file-name")
        assertEquals("Real Title", song.title)
    }

    @Test
    fun `unknown directives are ignored rather than projected`() {
        val song = parse(
            """
            {title: Song}
            {key: G}
            {tempo: 72}
            {capo: 2}
            The only words
            """.trimIndent()
        )

        assertEquals(1, song.sections.size)
        assertEquals("The only words", song.sections.single().text)
    }

    // ── Chords ───────────────────────────────────────────────────────────

    /** A congregation reads words, not chord symbols. */
    @Test
    fun `chords are stripped from the projected text`() {
        val song = parse("[G]Amazing [D]grace, how [Em]sweet the [C]sound")
        assertEquals("Amazing grace, how sweet the sound", song.sections.single().text)
    }

    @Test
    fun `complex chord shapes are stripped`() {
        val song = parse("[Am7]Words [G/B]with [Csus4]odd [F#m7b5]chords")
        assertEquals("Words with odd chords", song.sections.single().text)
    }

    @Test
    fun `a line that is only chords becomes blank rather than a stray slide`() {
        val song = parse(
            """
            [G] [D] [Em] [C]
            Amazing grace
            """.trimIndent()
        )
        assertEquals("Amazing grace", song.sections.single().text.trim())
    }

    // ── Sections ─────────────────────────────────────────────────────────

    @Test
    fun `blank lines split stanzas`() {
        val song = parse(
            """
            verse one line one
            verse one line two

            verse two line one
            verse two line two
            """.trimIndent()
        )

        assertEquals(2, song.sections.size)
        assertEquals("verse one line one\nverse one line two", song.sections[0].text)
    }

    @Test
    fun `a chorus block becomes a chorus section`() {
        val song = parse(
            """
            {title: Song}
            verse words

            {start_of_chorus}
            chorus words
            {end_of_chorus}

            more verse words
            """.trimIndent()
        )

        assertEquals(3, song.sections.size)
        assertEquals(SectionType.VERSE, song.sections[0].type)
        assertEquals(SectionType.CHORUS, song.sections[1].type)
        assertEquals("chorus words", song.sections[1].text)
        assertEquals(SectionType.VERSE, song.sections[2].type)
    }

    @Test
    fun `the short soc and eoc aliases work`() {
        val song = parse(
            """
            {soc}
            chorus words
            {eoc}
            """.trimIndent()
        )
        assertEquals(SectionType.CHORUS, song.sections.single().type)
    }

    @Test
    fun `a bridge block becomes a bridge section`() {
        val song = parse("{sob}\nbridge words\n{eob}")
        assertEquals(SectionType.BRIDGE, song.sections.single().type)
    }

    /** A blank line inside an explicit block is part of the block, not a split. */
    @Test
    fun `a blank line inside a chorus block does not split it`() {
        val song = parse(
            """
            {start_of_chorus}
            first half

            second half
            {end_of_chorus}
            """.trimIndent()
        )

        assertEquals(1, song.sections.size)
        assertTrue(song.sections.single().text.contains("first half"))
        assertTrue(song.sections.single().text.contains("second half"))
    }

    @Test
    fun `a comment names the section that follows`() {
        val song = parse(
            """
            {comment: Chorus}
            chorus words

            {comment: Verse 2}
            verse words
            """.trimIndent()
        )

        assertEquals(2, song.sections.size)
        assertEquals("Chorus", song.sections[0].label)
        assertEquals(SectionType.CHORUS, song.sections[0].type)
        assertEquals("Verse 2", song.sections[1].label)
        assertEquals(SectionType.VERSE, song.sections[1].type)
    }

    @Test
    fun `a bridge comment types the section as a bridge`() {
        val song = parse("{c: Bridge}\nbridge words")
        assertEquals(SectionType.BRIDGE, song.sections.single().type)
    }

    @Test
    fun `a labelled chorus block keeps its label`() {
        val song = parse("{start_of_chorus: Final Chorus}\nwords\n{end_of_chorus}")
        assertEquals("Final Chorus", song.sections.single().label)
    }

    // ── Plain text ───────────────────────────────────────────────────────

    /** Many files described as ChordPro carry no directives at all. */
    @Test
    fun `plain text with no directives still parses into stanzas`() {
        val song = parse(
            """
            Amazing grace how sweet the sound
            That saved a wretch like me

            I once was lost but now am found
            Was blind but now I see
            """.trimIndent(),
            fallbackTitle = "Amazing Grace",
        )

        assertEquals("Amazing Grace", song.title)
        assertEquals(2, song.sections.size)
        assertTrue(song.sections.all { it.type == SectionType.VERSE })
    }

    // ── Edge cases ───────────────────────────────────────────────────────

    @Test
    fun `an empty file yields a song with no sections rather than crashing`() {
        val song = parse("")
        assertTrue(song.sections.isEmpty())
        assertEquals("", song.title)
    }

    @Test
    fun `a file of only metadata yields no sections`() {
        assertTrue(parse("{title: Song}\n{key: G}").sections.isEmpty())
    }

    @Test
    fun `runs of blank lines do not produce empty sections`() {
        val song = parse("one\n\n\n\n\ntwo")
        assertEquals(2, song.sections.size)
    }

    @Test
    fun `trailing whitespace is trimmed from sections`() {
        val song = parse("words with trailing spaces   \n\nmore")
        assertEquals("words with trailing spaces", song.sections[0].text)
    }

    @Test
    fun `windows line endings are handled`() {
        val song = parse("verse one\r\n\r\nverse two")
        assertEquals(2, song.sections.size)
    }

    @Test
    fun `no metadata leaves the optional fields null`() {
        val song = parse("just words")
        assertNull(song.author)
        assertNull(song.bookName)
        assertNull(song.copyright)
    }

    // ── Detection ────────────────────────────────────────────────────────

    @Test
    fun `a file with directives is detected as chordpro`() {
        assertTrue(ChordProParser.looksLikeChordPro("{title: Song}\nwords"))
    }

    @Test
    fun `a file with chords is detected as chordpro`() {
        assertTrue(ChordProParser.looksLikeChordPro("[G]Amazing grace"))
    }

    @Test
    fun `plain prose is not detected as chordpro`() {
        assertFalse(ChordProParser.looksLikeChordPro("Amazing grace how sweet the sound"))
    }
}
