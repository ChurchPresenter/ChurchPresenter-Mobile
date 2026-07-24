package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests the field-alias fallback logic on [SongVerse] and [SongDetail]. */
class SongModelTest {

    // ── SongVerse.displayLabel ───────────────────────────────────────────────

    @Test
    fun displayLabelPrefersLabelThenTypeThenName() {
        assertEquals("Chorus", SongVerse(label = "Chorus", type = "verse", name = "x").displayLabel)
        assertEquals("Bridge", SongVerse(type = "Bridge", name = "x").displayLabel)
        assertEquals("Intro", SongVerse(name = "Intro").displayLabel)
    }

    @Test
    fun displayLabelBlankTextFieldsFallThroughToNumber() {
        assertEquals("2", SongVerse(label = "  ", type = "", number = 2).displayLabel)
    }

    @Test
    fun displayLabelFallsThroughNumberAliases() {
        assertEquals("1", SongVerse(number = 1).displayLabel)
        assertEquals("3", SongVerse(verse = 3).displayLabel)
        assertEquals("4", SongVerse(verseNumberKebab = 4).displayLabel)
        assertEquals("5", SongVerse(verseNumberCamel = 5).displayLabel)
        assertEquals("6", SongVerse(index = 6).displayLabel)
        assertNull(SongVerse().displayLabel)
    }

    // ── SongVerse.displayText ────────────────────────────────────────────────

    @Test
    fun displayTextJoinsLinesFirst() {
        assertEquals("a\nb", SongVerse(lines = listOf("a", "b"), text = "ignored").displayText)
        assertEquals("k1\nk2", SongVerse(verseLinesKebab = listOf("k1", "k2")).displayText)
        assertEquals("c1", SongVerse(verseLinesCamel = listOf("c1")).displayText)
    }

    @Test
    fun displayTextFallsBackToFirstNonBlankTextField() {
        assertEquals("content", SongVerse(text = "  ", content = "content").displayText)
        assertEquals("body", SongVerse(body = "body").displayText)
        assertEquals("slide", SongVerse(slideText = "slide").displayText)
    }

    @Test
    fun displayTextEmptyWhenNothingPresent() {
        assertEquals("", SongVerse().displayText)
        assertEquals("", SongVerse(lines = emptyList()).displayText)
    }

    // ── SongDetail.bookName ──────────────────────────────────────────────────

    @Test
    fun bookNamePrefersKebabThenFallsThrough() {
        assertEquals("Hymns", SongDetail(bookNameKebab = "Hymns", songbook = "Other").bookName)
        assertEquals("Camel", SongDetail(bookNameCamel = "Camel").bookName)
        assertEquals("Snake", SongDetail(bookNameSnake = "Snake").bookName)
        assertEquals("SB", SongDetail(songbook = "SB").bookName)
        assertEquals("SBK", SongDetail(songBookKebab = "SBK").bookName)
    }

    @Test
    fun bookNameSkipsBlankAndIsNullWhenAbsent() {
        assertEquals("Real", SongDetail(bookNameKebab = "   ", bookNameCamel = "Real").bookName)
        assertNull(SongDetail().bookName)
    }

    // ── SongDetail.allVerses / hasLyrics / plainText ─────────────────────────

    @Test
    fun allVersesReturnsFirstNonEmptyContainer() {
        val v = listOf(SongVerse(number = 1))
        assertEquals(v, SongDetail(verses = v).allVerses)
        assertEquals(v, SongDetail(verses = emptyList(), sections = v).allVerses)
        assertEquals(v, SongDetail(slides = v).allVerses)
        assertTrue(SongDetail().allVerses.isEmpty())
    }

    @Test
    fun hasLyricsTrueViaVersesOrPlainText() {
        assertTrue(SongDetail(verses = listOf(SongVerse(number = 1))).hasLyrics)
        assertTrue(SongDetail(text = "some words").hasLyrics)
        assertFalse(SongDetail().hasLyrics)
        assertFalse(SongDetail(text = "   ").hasLyrics)
    }

    @Test
    fun plainTextPrefersTextThenFallsThrough() {
        assertEquals("t", SongDetail(text = "t", content = "c").plainText)
        assertEquals("c", SongDetail(text = " ", content = "c").plainText)
        assertEquals("lk", SongDetail(lyricsTextKebab = "lk").plainText)
        assertNull(SongDetail().plainText)
    }
}
