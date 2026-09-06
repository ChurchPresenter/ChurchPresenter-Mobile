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

    // ── How a match is ranked ────────────────────────────────────────────
    //
    // The order is what makes the list useful: an operator typing "42" on a
    // Sunday morning wants hymn 42 first, not every song whose lyrics happen to
    // contain the digits.

    private fun ranked(query: String, vararg songs: LocalSong): List<String> =
        LibrarySearch.songs(songs.toList(), query).map { it.title }

    private fun hymn(
        title: String,
        number: String = "",
        author: String? = null,
        book: String? = null,
        body: String = "words",
    ) = LocalSong(
        id = title,
        number = number,
        title = title,
        author = author,
        bookName = book,
        sections = listOf(LocalSongSection(SectionType.VERSE, body)),
    )

    @Test
    fun `an exact number beats everything else`() {
        val order = ranked(
            "42",
            hymn("Contains 42 in the words", body = "we sang 42 times"),
            hymn("Amazing Grace", number = "42"),
        )

        assertEquals("Amazing Grace", order.first())
    }

    @Test
    fun `a number prefix beats a title match`() {
        val order = ranked("4", hymn("Four Seasons"), hymn("Amazing Grace", number = "42"))

        assertEquals("Amazing Grace", order.first())
    }

    @Test
    fun `an exact title beats a title prefix`() {
        val order = ranked("grace", hymn("Grace Abounding"), hymn("Grace"))

        assertEquals("Grace", order.first())
    }

    @Test
    fun `a title prefix beats a title containing the query`() {
        val order = ranked("grace", hymn("Amazing Grace"), hymn("Grace Abounding"))

        assertEquals("Grace Abounding", order.first())
    }

    @Test
    fun `a title match beats an author match`() {
        val order = ranked("newton", hymn("Newton's Hymn"), hymn("Amazing Grace", author = "John Newton"))

        assertEquals("Newton's Hymn", order.first())
    }

    @Test
    fun `an author match is found when nothing else matches`() {
        assertEquals(listOf("Amazing Grace"), ranked("newton", hymn("Amazing Grace", author = "John Newton")))
    }

    @Test
    fun `a songbook match is found when nothing else matches`() {
        assertEquals(listOf("Amazing Grace"), ranked("hymns", hymn("Amazing Grace", book = "Hymns")))
    }

    @Test
    fun `a match in the lyrics is found last`() {
        val order = ranked(
            "wretch",
            hymn("Wretch In The Title"),
            hymn("Amazing Grace", body = "saved a wretch like me"),
        )

        assertEquals("Wretch In The Title", order.first())
    }

    @Test
    fun `a song matching nothing is left out`() {
        assertTrue(ranked("zzzz", hymn("Amazing Grace")).isEmpty())
    }

    @Test
    fun `matching ignores case on every field`() {
        assertEquals(listOf("Amazing Grace"), ranked("AMAZING", hymn("Amazing Grace")))
        assertEquals(listOf("Amazing Grace"), ranked("NEWTON", hymn("Amazing Grace", author = "John Newton")))
        assertEquals(listOf("Amazing Grace"), ranked("HYMNS", hymn("Amazing Grace", book = "Hymns")))
    }

    @Test
    fun `a song with no number is not matched by an empty number`() {
        // "".startsWith(q) is true for any q, so the emptiness guard is what keeps
        // every unnumbered song from ranking as a number prefix.
        val order = ranked("grace", hymn("Amazing Grace"), hymn("Grace"))

        assertEquals("Grace", order.first())
    }

    @Test
    fun `a song with no author or book is still searchable by title`() {
        assertEquals(listOf("Amazing Grace"), ranked("amazing", hymn("Amazing Grace")))
    }
}
