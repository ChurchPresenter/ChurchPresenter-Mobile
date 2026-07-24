package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests alias fallbacks on Bible DTOs ([BibleBook], [BibleVerse], and responses). */
class BibleModelTest {

    // ── BibleBook ────────────────────────────────────────────────────────────

    @Test
    fun bookDisplayNameFallsThroughToUnknown() {
        assertEquals("John", BibleBook(name = "John", bookName = "x").displayName)
        assertEquals("Mark", BibleBook(bookName = "Mark").displayName)
        assertEquals("Luke", BibleBook(bookNameAlt = "Luke").displayName)
        assertEquals("id-42", BibleBook(id = "id-42").displayName)
        assertEquals("Unknown", BibleBook().displayName)
        assertEquals("Real", BibleBook(name = "  ", bookName = "Real").displayName)
    }

    @Test
    fun totalChaptersPrefersTotalThenCountThenListSize() {
        assertEquals(50, BibleBook(chapterTotal = 50).totalChapters)
        assertEquals(40, BibleBook(chapterCount = 40).totalChapters)
        assertEquals(2, BibleBook(chapters = listOf(BibleChapterInfo(1), BibleChapterInfo(2))).totalChapters)
        assertEquals(0, BibleBook().totalChapters)
    }

    @Test
    fun allBooksPrefersNonEmptyBooksThenBible() {
        val books = listOf(BibleBook(name = "A"))
        val bible = listOf(BibleBook(name = "B"))
        assertEquals(books, BibleBooksResponse(books = books, bible = bible).allBooks)
        // Empty `books` must fall through to `bible`.
        assertEquals(bible, BibleBooksResponse(books = emptyList(), bible = bible).allBooks)
        assertTrue(BibleBooksResponse().allBooks.isEmpty())
    }

    // ── BibleVerse ───────────────────────────────────────────────────────────

    @Test
    fun verseNumberFallsThrough() {
        assertEquals(5, BibleVerse(verse = 5).number)
        assertEquals(6, BibleVerse(verseNumber = 6).number)
        assertEquals(7, BibleVerse(verseNumberAlt = 7).number)
        assertEquals(0, BibleVerse().number)
    }

    @Test
    fun verseDisplayTextPrefersTextThenContent() {
        assertEquals("t", BibleVerse(text = "t", content = "c").displayText)
        assertEquals("c", BibleVerse(text = "  ", content = "c").displayText)
        assertEquals("", BibleVerse().displayText)
    }

    // ── BibleChapterResponse ─────────────────────────────────────────────────

    @Test
    fun chapterAllVersesPrefersNonEmptyVersesThenContent() {
        val verses = listOf(BibleVerse(verse = 1))
        val content = listOf(BibleVerse(verse = 2))
        assertEquals(verses, BibleChapterResponse(verses = verses, content = content).allVerses)
        assertEquals(content, BibleChapterResponse(verses = emptyList(), content = content).allVerses)
        assertTrue(BibleChapterResponse().allVerses.isEmpty())
    }
}
