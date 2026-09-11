package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * What tells one song apart from another in a list.
 *
 * This existed as `number == number && bookName == bookName` and was wrong: a
 * library can hold several songs with no number, or the same number twice in one
 * book, and that pair marks every one of them as the open song. Nobody could see
 * it on a phone, where the list vanishes the moment a song opens — beside a
 * detail pane the whole run lights up at once.
 */
class SongIdentityTest {

    @Test
    fun `two songs with no number and the same book are still different songs`() {
        val a = Song(id = 1, number = "", title = "Amazing Grace", bookName = "Hymnal")
        val b = Song(id = 2, number = "", title = "How Great Thou Art", bookName = "Hymnal")

        assertNotEquals(a.identity, b.identity)
    }

    @Test
    fun `two songs sharing a number in one book are still different songs`() {
        // Real songbooks do this — a number reused across a revision, or an
        // import that lost the numbering.
        val a = Song(id = 7, number = "42", title = "One", bookName = "Hymnal")
        val b = Song(id = 8, number = "42", title = "Another", bookName = "Hymnal")

        assertNotEquals(a.identity, b.identity)
    }

    @Test
    fun `the local library's own id wins`() {
        // Two rows of the on-device library, same number and book, different UUIDs.
        val a = Song(number = "1", title = "One", bookName = "Hymnal", localId = "uuid-a")
        val b = Song(number = "1", title = "One", bookName = "Hymnal", localId = "uuid-b")

        assertNotEquals(a.identity, b.identity)
    }

    @Test
    fun `the same song is the same song`() {
        val song = Song(id = 3, number = "3", title = "Third", bookName = "Hymnal")

        assertEquals(song.identity, song.copy().identity)
    }

    @Test
    fun `a song reloaded from the desktop is still recognised`() {
        // The list is refetched constantly; a new object with the same row id has
        // to keep matching, or the open song's highlight drops off on every poll.
        val before = Song(id = 3, number = "3", title = "Third", bookName = "Hymnal")
        val after = Song(id = 3, number = "3", title = "Third", bookName = "Hymnal")

        assertEquals(before.identity, after.identity)
    }

    @Test
    fun `a song carrying neither id falls back to number and book`() {
        // No worse than what it replaces, and the only case where the old rule
        // still applies.
        val a = Song(number = "5", title = "Five", bookName = "Hymnal")
        val b = Song(number = "5", title = "Five", bookName = "Hymnal")
        val c = Song(number = "6", title = "Six", bookName = "Hymnal")

        assertEquals(a.identity, b.identity)
        assertNotEquals(a.identity, c.identity)
    }

    @Test
    fun `the same number in two songbooks is two songs`() {
        val hymnal = Song(number = "1", title = "One", bookName = "Hymnal")
        val modern = Song(number = "1", title = "One", bookName = "Modern")

        assertNotEquals(hymnal.identity, modern.identity)
    }
}
