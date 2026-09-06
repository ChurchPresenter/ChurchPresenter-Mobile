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

    // ── Directive aliases ────────────────────────────────────────────────
    //
    // ChordPro files come from a dozen different editors, each favouring a
    // different spelling. An unrecognised alias silently drops the field, so the
    // song imports with no title or no songbook and nothing says why.

    private fun parsed(text: String) = ChordProParser.parse(text, id = "s1")

    @Test
    fun `both title spellings are accepted`() {
        assertEquals("Amazing Grace", parsed("{title: Amazing Grace}\nwords").title)
        assertEquals("Amazing Grace", parsed("{t: Amazing Grace}\nwords").title)
    }

    @Test
    fun `both subtitle spellings become the author when no artist is given`() {
        assertEquals("John Newton", parsed("{subtitle: John Newton}\nwords").author)
        assertEquals("John Newton", parsed("{st: John Newton}\nwords").author)
    }

    @Test
    fun `an artist wins over a subtitle`() {
        val song = parsed("{artist: Chris Tomlin}\n{subtitle: arr. Newton}\nwords")

        assertEquals("Chris Tomlin", song.author)
    }

    @Test
    fun `composer is accepted as an artist`() {
        assertEquals("J S Bach", parsed("{composer: J S Bach}\nwords").author)
    }

    @Test
    fun `every songbook spelling is accepted`() {
        for (key in listOf("album", "book", "songbook")) {
            assertEquals("Hymns", parsed("{$key: Hymns}\nwords").bookName, key)
        }
    }

    @Test
    fun `both number spellings are accepted`() {
        assertEquals("42", parsed("{number: 42}\nwords").number)
        assertEquals("42", parsed("{no: 42}\nwords").number)
    }

    @Test
    fun `both copyright spellings are accepted`() {
        assertEquals("Public domain", parsed("{copyright: Public domain}\nwords").copyright)
        assertEquals("CCLI 1234", parsed("{ccli: CCLI 1234}\nwords").copyright)
    }

    @Test
    fun `directives are matched whatever case they arrive in`() {
        assertEquals("Amazing Grace", parsed("{TITLE: Amazing Grace}\nwords").title)
    }

    @Test
    fun `an unrecognised directive is ignored rather than projected`() {
        // Key, tempo and tab settings are for a musician, not the screen.
        val song = parsed("{key: G}\n{tempo: 90}\nwords")

        assertEquals(listOf("words"), song.sections.map { it.text })
    }

    @Test
    fun `a blank field is left unset rather than stored empty`() {
        val song = parsed("{book: }\n{copyright: }\nwords")

        assertNull(song.bookName)
        assertNull(song.copyright)
    }

    @Test
    fun `a file with no title takes the fallback`() {
        // The fallback is the file name, which is better than a blank row.
        val song = ChordProParser.parse("words", id = "s1", fallbackTitle = "amazing-grace")

        assertEquals("amazing-grace", song.title)
    }

    @Test
    fun `a title in the file wins over the fallback`() {
        val song = ChordProParser.parse("{title: Amazing Grace}\nwords", id = "s1", fallbackTitle = "file-name")

        assertEquals("Amazing Grace", song.title)
    }

    // ── Section types from comments ──────────────────────────────────────

    @Test
    fun `a comment naming a part sets that part's type`() {
        for ((label, type) in listOf(
            "Chorus" to SectionType.CHORUS,
            "Refrain" to SectionType.CHORUS,
            "Bridge" to SectionType.BRIDGE,
            "Tag" to SectionType.TAG,
            "Ending" to SectionType.ENDING,
            "Outro" to SectionType.ENDING,
            "Verse 2" to SectionType.VERSE,
        )) {
            val song = parsed("{c: $label}\nwords")

            assertEquals(type, song.sections.single().type, label)
        }
    }

    @Test
    fun `a numbered part name is still recognised`() {
        assertEquals(SectionType.CHORUS, parsed("{c: Chorus 2}\nwords").sections.single().type)
    }

    @Test
    fun `a part name is matched whatever case it arrives in`() {
        assertEquals(SectionType.BRIDGE, parsed("{c: BRIDGE}\nwords").sections.single().type)
    }

    @Test
    fun `every comment spelling is accepted`() {
        for (key in listOf("comment", "c", "ci")) {
            assertEquals(SectionType.CHORUS, parsed("{$key: Chorus}\nwords").sections.single().type, key)
        }
    }

    @Test
    fun `a comment becomes the section's label`() {
        assertEquals("Chorus", parsed("{c: Chorus}\nwords").sections.single().label)
    }

    @Test
    fun `an empty comment leaves the section unlabelled`() {
        assertNull(parsed("{c: }\nwords").sections.single().label)
    }

    // ── Explicit blocks ──────────────────────────────────────────────────

    @Test
    fun `a chorus block with a blank line inside stays one section`() {
        // The whole reason {soc}…{eoc} exists: blank-line splitting is suppressed
        // inside an explicit block.
        val song = parsed("{soc}\nline one\n\nline two\n{eoc}")

        assertEquals(1, song.sections.size)
        assertEquals(SectionType.CHORUS, song.sections.single().type)
    }

    @Test
    fun `text after a block closes returns to verses`() {
        val song = parsed("{soc}\nchorus words\n{eoc}\n\nverse words")

        assertEquals(2, song.sections.size)
        assertEquals(SectionType.CHORUS, song.sections[0].type)
        assertEquals(SectionType.VERSE, song.sections[1].type)
    }

    @Test
    fun `a labelled block keeps its label`() {
        val song = parsed("{soc: Chorus 1}\nwords\n{eoc}")

        assertEquals("Chorus 1", song.sections.single().label)
    }

    @Test
    fun `an empty section is dropped rather than projected blank`() {
        val song = parsed("{c: Chorus}\n\n{c: Verse}\nwords")

        assertTrue(song.sections.all { it.text.isNotBlank() })
    }
}
