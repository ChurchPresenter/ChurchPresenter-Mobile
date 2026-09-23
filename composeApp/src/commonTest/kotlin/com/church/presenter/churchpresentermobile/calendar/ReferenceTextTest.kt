package com.church.presenter.churchpresentermobile.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReferenceTextTest {

    private fun parse(text: String) = parseReference(text, CANONICAL_BOOKS)

    @Test
    fun theUsualFormsParse() {
        assertEquals("John 3:16", parse("John 3:16")!!.text)
        assertEquals("John 3:16-17", parse("john 3:16-17")!!.text)
        assertEquals("John 3:16-17", parse("Jn 3.16–17")!!.text)
        assertEquals("1 John 3", parse("1jn 3")!!.text)
        assertEquals("Psalms 100:1-5", parse("Ps 100:1-5")!!.text)
        assertEquals("Song of Solomon 2:1", parse("Song of 2:1")!!.text)
        assertEquals("Genesis 1:1", parse("  Genesis   1 : 1  ")!!.text)
    }

    @Test
    fun whatIsParsedIsTheBookChapterAndVerses() {
        val ref = parse("Romans 8:28-30")!!
        assertEquals(45, ref.book.number)
        assertEquals(8, ref.chapter)
        assertEquals(28, ref.verseFrom)
        assertEquals(30, ref.verseTo)
        assertNull(parse("Romans 8")!!.verseFrom)
    }

    @Test
    fun aBackwardsRangeKeepsOnlyItsStart() {
        val ref = parse("John 3:20-16")!!
        assertEquals(20, ref.verseFrom)
        assertNull(ref.verseTo)
        assertEquals("John 3:20", ref.text)
        assertEquals("John 3:16", parse("John 3:16-16")!!.text)
    }

    @Test
    fun whatIsNotAReferenceIsNull() {
        assertNull(parse("Amazing Grace"))
        assertNull(parse("John"))
        assertNull(parse("John 99"))
        assertNull(parse("Jhn 3:16"))
        assertNull(parse("3:16"))
        assertNull(parse(""))
    }

    @Test
    fun booksMatchExactlyThenByPrefixThenByAlias() {
        assertEquals("Judges", findBook("Jud", CANONICAL_BOOKS)!!.name)
        assertEquals("Jude", findBook("Jude", CANONICAL_BOOKS)!!.name)
        assertEquals("Philippians", findBook("Phil", CANONICAL_BOOKS)!!.name)
        assertEquals("Philemon", findBook("Phm", CANONICAL_BOOKS)!!.name)
        assertEquals("Revelation", findBook("rev", CANONICAL_BOOKS)!!.name)
        assertNull(findBook("", CANONICAL_BOOKS))
        assertNull(findBook("Xyz", CANONICAL_BOOKS))
        assertEquals(66, CANONICAL_BOOKS.size)
        assertEquals(150, CANONICAL_BOOKS.first { it.name == "Psalms" }.chapters)
    }
}
