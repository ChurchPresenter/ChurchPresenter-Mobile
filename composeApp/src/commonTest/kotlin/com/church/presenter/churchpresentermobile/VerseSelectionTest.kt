package com.church.presenter.churchpresentermobile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reading a verse selection off a schedule row.
 *
 * The string comes from a desktop, and sometimes from a person typing into one,
 * so it arrives as "16", "16-18", "3,4,7", "3-5,7" or something nobody planned
 * for. Getting it wrong highlights the wrong lines on the wall — or, if a stray
 * character threw, opened nothing at all — so the rule is to take what parses
 * and quietly drop what does not.
 */
class VerseSelectionTest {

    // ── The shapes the desktop sends ─────────────────────────────────────

    @Test
    fun `a single verse is that verse`() {
        assertEquals(setOf(16), parseVerseString("16"))
    }

    @Test
    fun `a range is every verse in it`() {
        assertEquals(setOf(16, 17, 18), parseVerseString("16-18"))
    }

    @Test
    fun `a range includes both ends`() {
        assertEquals(setOf(3, 4, 5, 6, 7), parseVerseString("3-7"))
    }

    @Test
    fun `a list is each verse named`() {
        assertEquals(setOf(3, 4, 7), parseVerseString("3,4,7"))
    }

    @Test
    fun `a list of ranges is all of them`() {
        assertEquals(setOf(3, 4, 5, 7), parseVerseString("3-5,7"))
    }

    @Test
    fun `several ranges are all included`() {
        assertEquals(setOf(1, 2, 3, 8, 9, 10), parseVerseString("1-3,8-10"))
    }

    @Test
    fun `a range of one verse is that verse`() {
        assertEquals(setOf(5), parseVerseString("5-5"))
    }

    @Test
    fun `space around the numbers is ignored`() {
        assertEquals(setOf(3, 4, 7), parseVerseString(" 3 , 4 , 7 "))
    }

    @Test
    fun `space inside a range is ignored`() {
        assertEquals(setOf(3, 4, 5), parseVerseString("3 - 5"))
    }

    @Test
    fun `a trailing comma is harmless`() {
        assertEquals(setOf(3, 4), parseVerseString("3,4,"))
    }

    @Test
    fun `a leading comma is harmless`() {
        assertEquals(setOf(3, 4), parseVerseString(",3,4"))
    }

    @Test
    fun `a repeated verse is listed once`() {
        // The selection is a set: the wall highlights a verse or it does not.
        assertEquals(setOf(3, 4), parseVerseString("3,4,3"))
    }

    @Test
    fun `overlapping ranges merge`() {
        assertEquals(setOf(1, 2, 3, 4), parseVerseString("1-3,2-4"))
    }

    // ── Nothing selected ─────────────────────────────────────────────────

    @Test
    fun `no verse string selects nothing`() {
        // A whole chapter, which is an ordinary thing to project.
        assertTrue(parseVerseString(null).isEmpty())
    }

    @Test
    fun `an empty verse string selects nothing`() {
        assertTrue(parseVerseString("").isEmpty())
    }

    @Test
    fun `a verse string of spaces selects nothing`() {
        assertTrue(parseVerseString("   ").isEmpty())
    }

    @Test
    fun `a comma on its own selects nothing`() {
        assertTrue(parseVerseString(",").isEmpty())
    }

    // ── Things nobody planned for ────────────────────────────────────────

    @Test
    fun `a word instead of a number is skipped`() {
        assertTrue(parseVerseString("sixteen").isEmpty())
    }

    @Test
    fun `a word among numbers does not stop the rest`() {
        // One stray token must not lose the whole reference.
        assertEquals(setOf(3, 7), parseVerseString("3,four,7"))
    }

    @Test
    fun `a range with no start is skipped`() {
        assertTrue(parseVerseString("-5").isEmpty())
    }

    @Test
    fun `a range with no start does not stop the rest`() {
        assertEquals(setOf(9), parseVerseString("-5,9"))
    }

    @Test
    fun `a range with no end is read as a single verse`() {
        // "16-" is someone who stopped typing; the verse they named still counts.
        assertEquals(setOf(16), parseVerseString("16-"))
    }

    @Test
    fun `a range with an unreadable end is read as a single verse`() {
        assertEquals(setOf(16), parseVerseString("16-end"))
    }

    @Test
    fun `a backwards range selects nothing`() {
        // Nothing to walk through, rather than an error.
        assertTrue(parseVerseString("18-16").isEmpty())
    }

    @Test
    fun `a backwards range does not stop the rest`() {
        assertEquals(setOf(20), parseVerseString("18-16,20"))
    }

    @Test
    fun `a verse of zero is taken at face value`() {
        // Not this parser's business to know how Bibles are numbered.
        assertEquals(setOf(0), parseVerseString("0"))
    }

    @Test
    fun `a negative number is read as a range`() {
        // "-3" has no start, so nothing is selected from it.
        assertTrue(parseVerseString("-3").isEmpty())
    }

    @Test
    fun `a decimal is skipped`() {
        assertTrue(parseVerseString("3.5").isEmpty())
    }

    @Test
    fun `a very large verse number is kept`() {
        assertEquals(setOf(176), parseVerseString("176"))
    }

    @Test
    fun `a number too large to be a verse is still kept`() {
        // Clamping belongs to the screen that knows the chapter's length.
        assertEquals(setOf(9999), parseVerseString("9999"))
    }

    @Test
    fun `a number beyond what fits is skipped rather than throwing`() {
        assertTrue(parseVerseString("99999999999999999999").isEmpty())
    }

    @Test
    fun `a long range is fully expanded`() {
        assertEquals((1..176).toSet(), parseVerseString("1-176"))
    }

    @Test
    fun `a double dash still reads the numbers on either side`() {
        // A slip of the keyboard should not lose the reference.
        assertEquals(setOf(3, 4, 5), parseVerseString("3--5"))
    }

    @Test
    fun `punctuation on its own is skipped`() {
        assertTrue(parseVerseString(";;;").isEmpty())
    }

    @Test
    fun `a semicolon-separated list is not split`() {
        // The desktop separates with commas; a semicolon list is one unreadable
        // token rather than three verses.
        assertTrue(parseVerseString("3;4;7").isEmpty())
    }

    @Test
    fun `a verse with a letter suffix is skipped`() {
        assertTrue(parseVerseString("16a").isEmpty())
    }

    @Test
    fun `a mixed selection keeps everything readable in it`() {
        assertEquals(setOf(1, 2, 3, 9, 12), parseVerseString("1-3, nine, 9, 12"))
    }
}
