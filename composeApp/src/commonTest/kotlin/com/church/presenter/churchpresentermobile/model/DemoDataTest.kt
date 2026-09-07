package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the canned content demo mode serves.
 *
 * These are the fallbacks in particular: demo mode has to answer *every* lookup,
 * including ones nothing was written for, because there is no network behind it
 * to fail over to. A null or empty answer here is a blank screen in a demo.
 */
class DemoDataTest {

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun `the catalogue is not empty and every song is identifiable`() {
        assertTrue(DemoData.songs.isNotEmpty())
        assertTrue(DemoData.songs.all { it.number.isNotBlank() }, "a song with no number cannot be looked up")
        assertTrue(DemoData.songs.all { it.title.isNotBlank() })
    }

    @Test
    fun `every catalogue song has lyrics to project`() {
        for (song in DemoData.songs) {
            val detail = DemoData.getSongDetail(song.number)

            assertTrue(detail.hasLyrics, "song ${song.number} '${song.title}' has nothing to show")
            assertEquals(song.number, detail.number)
        }
    }

    @Test
    fun `a song with no written detail still gets verses built from its catalogue entry`() {
        // The generated fallback: enough structure that the slide builder has
        // something to lay out, named after the song it stands in for.
        val song = DemoData.songs.first()

        val detail = DemoData.getSongDetail(song.number)

        assertTrue(detail.allVerses.isNotEmpty())
        assertTrue(detail.allVerses.all { it.displayText.isNotBlank() })
    }

    @Test
    fun `an unknown song number still returns something projectable`() {
        // Nothing in the catalogue matches, so the last fallback answers.
        val detail = DemoData.getSongDetail("no-such-number")

        assertEquals("no-such-number", detail.number)
        assertTrue(!detail.title.isNullOrBlank())
        assertTrue(detail.hasLyrics, "demo mode has no network to fall back to")
    }

    @Test
    fun `an empty song number is answered rather than refused`() {
        val detail = DemoData.getSongDetail("")

        assertTrue(!detail.title.isNullOrBlank())
        assertTrue(detail.hasLyrics)
    }

    // ── Bible ────────────────────────────────────────────────────────────

    @Test
    fun `the book list is populated and each book is named`() {
        assertTrue(DemoData.books.isNotEmpty())
        assertTrue(DemoData.books.all { it.displayName.isNotBlank() })
    }

    @Test
    fun `every book's first chapter has verses`() {
        for (book in DemoData.books) {
            val verses = DemoData.getVerses(book.displayName, 1)

            assertTrue(verses.isNotEmpty(), "${book.displayName} 1 has no verses")
            assertTrue(verses.all { !it.text.isNullOrBlank() })
        }
    }

    @Test
    fun `a book is matched whatever case or padding it arrives in`() {
        val book = DemoData.books.first()

        val exact = DemoData.getVerses(book.displayName, 1)

        assertEquals(exact, DemoData.getVerses(book.displayName.uppercase(), 1))
        assertEquals(exact, DemoData.getVerses("  ${book.displayName}  ", 1))
    }

    @Test
    fun `an unwritten chapter falls back to the book's other chapter rather than nothing`() {
        val book = DemoData.books.first()

        val verses = DemoData.getVerses(book.displayName, 999)

        assertTrue(verses.isNotEmpty())
    }

    @Test
    fun `an unknown book still returns generated verses naming what was asked for`() {
        val verses = DemoData.getVerses("Habakkuk", 3)

        assertEquals(5, verses.size)
        assertTrue(verses.all { it.text?.contains("Habakkuk 3:") == true }, "${verses.first().text}")
        assertEquals(listOf(1, 2, 3, 4, 5), verses.map { it.verse })
    }

    // ── The other canned collections ─────────────────────────────────────

    @Test
    fun `the schedule, pictures and presentations all have content`() {
        assertTrue(DemoData.scheduleItems.isNotEmpty())
        assertTrue(DemoData.presentations.isNotEmpty())
        assertTrue(DemoData.picturesFolder.allImages.isNotEmpty())
    }

    @Test
    fun `every schedule item has something to show in the row`() {
        assertTrue(DemoData.scheduleItems.all { it.displayTitle.isNotBlank() })
    }
}
