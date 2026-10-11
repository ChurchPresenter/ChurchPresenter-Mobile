package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Rebuilding the desktop's slide list from a song's sections.
 *
 * The index a verse card sends (`select_song_section`) and the index the desktop
 * reports as live both address the desktop's *slides*, and the desktop cuts a
 * section into several wherever a `[---]` break is written in it. Every rule here
 * mirrors one on the desktop (SongsViewModelSections); getting one wrong moves
 * every later index by one, which is the bug this class exists for: tap verse 2,
 * get the second half of verse 1.
 */
class SlideBreaksTest {

    /** Each slide as "source:part/parts:text", lines joined with "|". */
    private fun slides(vararg sections: String) =
        SlideBreaks.pieces(sections.toList()).map {
            "${it.source}:${it.part}/${it.parts}:${it.text.replace("\n", "|")}"
        }

    // ── What counts as a break ───────────────────────────────────────────

    @Test
    fun `the written marker is a break`() {
        assertTrue(SlideBreaks.isBreak("[---]"))
    }

    @Test
    fun `the desktop's other spellings are breaks too`() {
        // The desktop accepts any run of dashes and spaces in square or curly
        // brackets; a song typed there with one of these has to split here too.
        listOf("[-]", "[ - ]", "{---}", "  [---]  ", "[- - -]").forEach {
            assertTrue(SlideBreaks.isBreak(it), it)
        }
    }

    @Test
    fun `a lyric that happens to contain brackets is not a break`() {
        listOf("[G]Amazing grace", "[---] and more", "[]", "[ ]", "[Verse 1]").forEach {
            assertFalse(SlideBreaks.isBreak(it), it)
        }
    }

    // ── Splitting ────────────────────────────────────────────────────────

    @Test
    fun `a section without a break is one slide`() {
        assertEquals(listOf("0:0/1:one", "1:0/1:two"), slides("one", "two"))
    }

    @Test
    fun `a break splits its section and moves every later slide along`() {
        // The reported bug: with verse 1 split, verse 2 is the desktop's third slide.
        assertEquals(
            listOf("0:0/2:a1|a2", "0:1/2:a3|a4", "1:0/1:b1|b2"),
            slides("a1\na2\n[---]\na3\na4", "b1\nb2"),
        )
    }

    @Test
    fun `the marker itself never reaches a slide`() {
        val text = SlideBreaks.pieces(listOf("one\n[---]\ntwo")).joinToString { it.text }

        assertFalse("---" in text)
    }

    @Test
    fun `a leading, doubled or trailing break adds no blank slide`() {
        // The desktop only ends a slide that has something in it.
        assertEquals(listOf("0:0/2:x", "0:1/2:y"), slides("[---]\nx\n[---]\n[---]\ny\n[---]"))
    }

    @Test
    fun `a section that is only breaks and blank lines makes no slide`() {
        assertEquals(listOf("1:0/1:words"), slides("[---]\n\n[---]", "words"))
    }

    // ── Chord-only slides ────────────────────────────────────────────────

    @Test
    fun `a slide of only chords is folded into the one after it`() {
        // An intro has no words to show, so the desktop gives it no slide of its own.
        assertEquals(listOf("1:0/1:[G] [C]|[G]Amazing grace"), slides("[G] [C]", "[G]Amazing grace"))
    }

    @Test
    fun `chords split off by a break ride on the next half of the same section`() {
        assertEquals(listOf("0:0/2:one", "0:1/2:[D]|two"), slides("one\n[---]\n[D]\n[---]\ntwo"))
    }

    @Test
    fun `an outro of only chords is folded into the slide before it`() {
        assertEquals(listOf("0:0/1:words|[G] [D]"), slides("words", "[G] [D]"))
    }

    @Test
    fun `a song of nothing but chords is left as written`() {
        // The desktop does the same rather than show no slides at all.
        assertEquals(listOf("0:0/1:[G]", "1:0/1:[C]"), slides("[G]", "[C]"))
    }
}
