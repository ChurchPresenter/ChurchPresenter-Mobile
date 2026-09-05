package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibrarySearchTest {

    private fun song(
        id: String,
        number: String = "",
        title: String = "",
        author: String? = null,
        body: String = "",
    ) = LocalSong(
        id = id,
        number = number,
        title = title,
        author = author,
        sections = listOf(LocalSongSection(SectionType.VERSE, body)),
    )

    private val library = listOf(
        song("a", number = "23", title = "The Lord's My Shepherd"),
        song("b", number = "230", title = "Amazing Grace", body = "saved a wretch like me, 23 years"),
        song("c", number = "7", title = "Grace Greater Than Our Sin"),
        song("d", number = "88", title = "How Great Thou Art", author = "Stuart Hine"),
        song("e", number = "12", title = "Blessed Assurance", body = "This is my story, grace divine"),
    )

    @Test
    fun `a blank query returns everything unchanged`() {
        assertEquals(library, LibrarySearch.songs(library, ""))
        assertEquals(library, LibrarySearch.songs(library, "   "))
    }

    /**
     * The point of ranking rather than filtering: an operator typing "23"
     * mid-service wants hymn 23 first, not a lyric that happens to contain it.
     */
    @Test
    fun `an exact number outranks a number prefix and a lyric mention`() {
        val results = LibrarySearch.songs(library, "23")

        assertEquals("a", results.first().id)
        assertTrue(results.map { it.id }.containsAll(listOf("a", "b")))
        assertTrue(results.indexOfFirst { it.id == "a" } < results.indexOfFirst { it.id == "b" })
    }

    @Test
    fun `a title prefix outranks a title substring`() {
        val results = LibrarySearch.songs(library, "grace")

        // "Grace Greater…" starts with it; "Amazing Grace" merely contains it.
        assertEquals("c", results.first().id)
    }

    @Test
    fun `a title match outranks a lyrics match`() {
        val results = LibrarySearch.songs(library, "grace").map { it.id }
        assertTrue(results.indexOf("b") < results.indexOf("e"), "title should beat body")
    }

    @Test
    fun `search is case insensitive`() {
        assertEquals(
            LibrarySearch.songs(library, "amazing").map { it.id },
            LibrarySearch.songs(library, "AMAZING").map { it.id },
        )
    }

    @Test
    fun `the author is searchable`() {
        assertEquals(listOf("d"), LibrarySearch.songs(library, "Stuart Hine").map { it.id })
    }

    @Test
    fun `lyrics are searchable when nothing better matches`() {
        assertEquals(listOf("b"), LibrarySearch.songs(library, "wretch").map { it.id })
    }

    @Test
    fun `a query matching nothing returns nothing`() {
        assertTrue(LibrarySearch.songs(library, "zzzz").isEmpty())
    }

    @Test
    fun `whitespace around the query is ignored`() {
        assertEquals(
            LibrarySearch.songs(library, "grace").map { it.id },
            LibrarySearch.songs(library, "  grace  ").map { it.id },
        )
    }

    /** Results must not shuffle as the user keeps typing the same-scoring query. */
    @Test
    fun `equal scores keep library order`() {
        val ties = listOf(
            song("x", title = "Holy Holy Holy"),
            song("y", title = "Holy Ground"),
            song("z", title = "Holy Spirit Come"),
        )
        assertEquals(listOf("x", "y", "z"), LibrarySearch.songs(ties, "holy").map { it.id })
    }

    @Test
    fun `a song with no number is still findable by title`() {
        val unnumbered = listOf(song("n", title = "Untitled Hymn"))
        assertEquals(listOf("n"), LibrarySearch.songs(unnumbered, "untitled").map { it.id })
    }

    // ── Announcements ────────────────────────────────────────────────────

    private val notices = listOf(
        LocalAnnouncement(id = "n1", title = "Prayer meeting", body = "Wednesday at seven"),
        LocalAnnouncement(id = "n2", title = "Welcome", body = "Prayer requests to the office"),
    )

    @Test
    fun `announcement titles outrank bodies`() {
        assertEquals(listOf("n1", "n2"), LibrarySearch.announcements(notices, "prayer").map { it.id })
    }

    @Test
    fun `a blank announcement query returns everything`() {
        assertEquals(notices, LibrarySearch.announcements(notices, ""))
    }
}
