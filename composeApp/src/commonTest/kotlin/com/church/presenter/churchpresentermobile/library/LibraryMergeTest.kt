package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * These rules decide whether a re-sync can destroy an evening of the operator's
 * work, so they are pinned exhaustively.
 */
class LibraryMergeTest {

    private fun song(
        id: String,
        number: String = "42",
        title: String = "Amazing Grace",
        book: String? = "Hymns",
        body: String = "words",
        origin: ContentOrigin = ContentOrigin.DESKTOP,
    ) = LocalSong(
        id = id,
        number = number,
        title = title,
        bookName = book,
        sections = listOf(LocalSongSection(SectionType.VERSE, body)),
        origin = origin,
    )

    // ── Adding ───────────────────────────────────────────────────────────

    @Test
    fun `a first sync adds everything as desktop content`() {
        val result = LibraryMerge.mergeSongs(
            existing = emptyList(),
            incoming = listOf(song("d1", number = "1"), song("d2", number = "2")),
        )

        assertEquals(2, result.added)
        assertEquals(0, result.updated)
        assertTrue(result.songs.all { it.origin == ContentOrigin.DESKTOP })
    }

    @Test
    fun `a desktop song is matched on songbook and number, not on id`() {
        val existing = listOf(song("local-id", number = "42", book = "Hymns"))
        val incoming = listOf(song("server-id", number = "42", book = "Hymns", body = "updated"))

        val result = LibraryMerge.mergeSongs(existing, incoming)

        assertEquals(1, result.songs.size)
        assertEquals(1, result.updated)
        assertEquals("updated", result.songs.single().sections.single().text)
    }

    /** Setlists point at ids, so a re-sync must not renumber the world. */
    @Test
    fun `an updated song keeps its existing id`() {
        val existing = listOf(song("stable-id", number = "42"))
        val incoming = listOf(song("fresh-id", number = "42", body = "updated"))

        assertEquals("stable-id", LibraryMerge.mergeSongs(existing, incoming).songs.single().id)
    }

    /**
     * Two songbooks routinely both have a hymn 42; matching must not collapse
     * them into one.
     */
    @Test
    fun `the same number in a different songbook is a different song`() {
        val existing = listOf(song("d1", number = "42", book = "Hymns"))
        val incoming = listOf(
            song("s1", number = "42", book = "Hymns", body = "hymnal words"),
            song("s2", number = "42", book = "Mission Praise", body = "other words"),
        )

        val result = LibraryMerge.mergeSongs(existing, incoming)

        assertEquals(2, result.songs.size)
        assertEquals(1, result.added, "the Mission Praise one is new")
        assertEquals(1, result.updated, "the Hymns one already existed")
        assertEquals(0, result.removed)
    }

    @Test
    fun `a song with no number is matched on its title`() {
        val existing = listOf(song("d1", number = "", title = "Untitled Hymn"))
        val incoming = listOf(song("d2", number = "", title = "Untitled Hymn", body = "updated"))

        val result = LibraryMerge.mergeSongs(existing, incoming)

        assertEquals(1, result.songs.size)
        assertEquals(1, result.updated)
    }

    // ── Protecting the user's work ───────────────────────────────────────

    @Test
    fun `a locally authored song is never touched`() {
        val mine = song("mine", number = "900", title = "Our Church Song", origin = ContentOrigin.LOCAL)

        val result = LibraryMerge.mergeSongs(listOf(mine), listOf(song("d1", number = "1")))

        assertEquals(mine, result.songs.first { it.id == "mine" })
    }

    @Test
    fun `an edited desktop song survives a re-sync`() {
        val edited = song("e1", number = "42", body = "my corrected words", origin = ContentOrigin.LOCAL_OVERRIDE)
        val incoming = listOf(song("d1", number = "42", body = "the server's words"))

        val result = LibraryMerge.mergeSongs(listOf(edited), incoming)

        assertEquals(1, result.songs.size)
        assertEquals("my corrected words", result.songs.single().sections.single().text)
        assertEquals(ContentOrigin.LOCAL_OVERRIDE, result.songs.single().origin)
        assertEquals(1, result.kept, "the skip must be reported, not silent")
    }

    /** Otherwise the operator ends up with two hymn 42s and no idea which is live. */
    @Test
    fun `a desktop song does not duplicate a local song in the same slot`() {
        val mine = song("mine", number = "42", origin = ContentOrigin.LOCAL)

        val result = LibraryMerge.mergeSongs(listOf(mine), listOf(song("d1", number = "42")))

        assertEquals(1, result.songs.size)
        assertEquals("mine", result.songs.single().id)
        assertEquals(0, result.added)
        assertEquals(1, result.kept)
    }

    @Test
    fun `an unrelated local song is not counted as a conflict`() {
        val mine = song("mine", number = "900", origin = ContentOrigin.LOCAL)

        val result = LibraryMerge.mergeSongs(listOf(mine), listOf(song("d1", number = "42")))

        assertEquals(2, result.songs.size)
        assertEquals(0, result.kept, "no conflict, so nothing to report")
    }

    // ── Removal ──────────────────────────────────────────────────────────

    @Test
    fun `a desktop song deleted on the desktop is dropped`() {
        val existing = listOf(song("d1", number = "1"), song("d2", number = "2"))

        val result = LibraryMerge.mergeSongs(existing, listOf(song("d1", number = "1")))

        assertEquals(1, result.songs.size)
        assertEquals(1, result.removed)
    }

    /** Losing an item out of next Sunday's set is not a tidy-up. */
    @Test
    fun `a deleted desktop song is kept while a setlist still needs it`() {
        val existing = listOf(song("d1", number = "1"), song("needed", number = "2"))

        val result = LibraryMerge.mergeSongs(
            existing = existing,
            incoming = listOf(song("d1", number = "1")),
            referencedIds = setOf("needed"),
        )

        assertEquals(2, result.songs.size)
        assertNotNull(result.songs.firstOrNull { it.id == "needed" })
        assertEquals(0, result.removed)
    }

    @Test
    fun `local songs are never removed even when absent from the desktop`() {
        val mine = song("mine", number = "900", origin = ContentOrigin.LOCAL)

        val result = LibraryMerge.mergeSongs(listOf(mine), listOf(song("d1", number = "1")))

        assertNotNull(result.songs.firstOrNull { it.id == "mine" })
        assertEquals(0, result.removed)
    }

    @Test
    fun `an empty catalogue removes desktop content but keeps the user's`() {
        val existing = listOf(
            song("d1", number = "1"),
            song("mine", number = "900", origin = ContentOrigin.LOCAL),
        )

        val result = LibraryMerge.mergeSongs(existing, emptyList())

        assertEquals(listOf("mine"), result.songs.map { it.id })
        assertEquals(1, result.removed)
    }

    // ── Ordering ─────────────────────────────────────────────────────────

    @Test
    fun `songs come back in songbook then numeric order`() {
        val incoming = listOf(
            song("c", number = "100", book = "Hymns"),
            song("a", number = "2", book = "Hymns"),
            song("b", number = "10", book = "Hymns"),
        )

        val result = LibraryMerge.mergeSongs(emptyList(), incoming)

        assertEquals(listOf("2", "10", "100"), result.songs.map { it.number })
    }

    @Test
    fun `a song numbered 42a sorts with the forties, not alphabetically`() {
        val incoming = listOf(
            song("x", number = "9", book = "Hymns"),
            song("y", number = "42a", book = "Hymns"),
        )

        assertEquals(listOf("9", "42a"), LibraryMerge.mergeSongs(emptyList(), incoming).songs.map { it.number })
    }

    @Test
    fun `matching ignores case and surrounding whitespace`() {
        val existing = listOf(song("d1", number = "42A", book = "Hymns"))
        val incoming = listOf(song("d2", number = " 42a ", book = " hymns "))

        assertEquals(1, LibraryMerge.mergeSongs(existing, incoming).songs.size)
    }

    @Test
    fun `merging an empty library with an empty catalogue is a no-op`() {
        val result = LibraryMerge.mergeSongs(emptyList(), emptyList())
        assertTrue(result.songs.isEmpty())
        assertEquals(0, result.added + result.updated + result.kept + result.removed)
        assertNull(result.songs.firstOrNull())
    }
}
