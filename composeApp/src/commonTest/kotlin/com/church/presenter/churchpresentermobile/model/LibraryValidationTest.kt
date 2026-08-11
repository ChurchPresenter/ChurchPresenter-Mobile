package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryValidationTest {

    private fun song(
        id: String = "s1",
        number: String = "42",
        title: String = "Amazing Grace",
        bookName: String? = "Hymns",
        sections: List<LocalSongSection> = listOf(LocalSongSection(SectionType.VERSE, "Amazing grace")),
    ) = LocalSong(id = id, number = number, title = title, bookName = bookName, sections = sections)

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun `a complete song is valid`() {
        val result = LibraryValidation.validateSong(song())
        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `a song needs a title`() {
        val result = LibraryValidation.validateSong(song(title = "   "))
        assertFalse(result.isValid)
        assertTrue(LibraryField.TITLE in result.errors)
    }

    @Test
    fun `a song needs at least one section with text`() {
        val result = LibraryValidation.validateSong(song(sections = emptyList()))
        assertTrue(LibraryField.SECTIONS in result.errors)
    }

    @Test
    fun `sections that are all blank do not count`() {
        val result = LibraryValidation.validateSong(
            song(sections = listOf(LocalSongSection(SectionType.VERSE, "   ")))
        )
        assertTrue(LibraryField.SECTIONS in result.errors)
    }

    @Test
    fun `a song without a number is fine`() {
        assertTrue(LibraryValidation.validateSong(song(number = "")).isValid)
    }

    @Test
    fun `an over-long section is an error`() {
        val result = LibraryValidation.validateSong(
            song(sections = listOf(LocalSongSection(SectionType.VERSE, "x".repeat(LibraryValidation.MAX_SECTION_CHARS + 1))))
        )
        assertFalse(result.isValid)
        assertTrue(LibraryField.SECTIONS in result.errors)
    }

    /**
     * A warning, not an error: the same number in two different songbooks is
     * normal, so refusing to save would be wrong.
     */
    @Test
    fun `a duplicate number in the same book warns but still saves`() {
        val existing = listOf(song(id = "other", number = "42", bookName = "Hymns"))
        val result = LibraryValidation.validateSong(song(id = "s1"), existing)

        assertTrue(result.isValid, "a clash must not block saving")
        assertTrue(LibraryField.NUMBER in result.warnings)
    }

    @Test
    fun `the same number in a different book is not a clash`() {
        val existing = listOf(song(id = "other", number = "42", bookName = "Mission Praise"))
        val result = LibraryValidation.validateSong(song(id = "s1", bookName = "Hymns"), existing)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `a song does not clash with itself when edited`() {
        val existing = listOf(song(id = "s1", number = "42"))
        val result = LibraryValidation.validateSong(song(id = "s1", title = "Edited"), existing)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `a very long section warns that it may not fit one slide`() {
        val manyLines = (1..LibraryValidation.LONG_SECTION_LINES + 2).joinToString("\n") { "line $it" }
        val result = LibraryValidation.validateSong(
            song(sections = listOf(LocalSongSection(SectionType.VERSE, manyLines)))
        )
        assertTrue(result.isValid)
        assertTrue(LibraryField.SECTIONS in result.warnings)
    }

    // ── Announcements ────────────────────────────────────────────────────

    @Test
    fun `an announcement needs a body`() {
        val result = LibraryValidation.validateAnnouncement(LocalAnnouncement(id = "a", body = "  "))
        assertFalse(result.isValid)
        assertTrue(LibraryField.BODY in result.errors)
    }

    @Test
    fun `an announcement with a body is valid without a title`() {
        assertTrue(
            LibraryValidation.validateAnnouncement(LocalAnnouncement(id = "a", body = "Welcome")).isValid
        )
    }

    @Test
    fun `an over-long announcement is an error`() {
        val result = LibraryValidation.validateAnnouncement(
            LocalAnnouncement(id = "a", body = "x".repeat(LibraryValidation.MAX_SECTION_CHARS + 1))
        )
        assertFalse(result.isValid)
    }

    // ── Setlists ─────────────────────────────────────────────────────────

    private val library = LibraryData(
        songs = listOf(song(id = "s1")),
        announcements = listOf(LocalAnnouncement(id = "a1", body = "Welcome")),
    )

    @Test
    fun `a setlist needs a name and at least one entry`() {
        val result = LibraryValidation.validateSetlist(LocalSetlist(id = "set", name = ""), library)
        assertTrue(LibraryField.NAME in result.errors)
        assertTrue(LibraryField.ENTRIES in result.errors)
    }

    @Test
    fun `a setlist whose entries all resolve is clean`() {
        val setlist = LocalSetlist(
            id = "set",
            name = "Sunday",
            entries = listOf(
                LocalSetlistEntry(SetlistEntryType.SONG, "s1"),
                LocalSetlistEntry(SetlistEntryType.ANNOUNCEMENT, "a1"),
            ),
        )
        val result = LibraryValidation.validateSetlist(setlist, library)
        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty())
    }

    /** Better to find a deleted song before the service than during it. */
    @Test
    fun `a setlist pointing at a deleted song warns`() {
        val setlist = LocalSetlist(
            id = "set",
            name = "Sunday",
            entries = listOf(LocalSetlistEntry(SetlistEntryType.SONG, "gone")),
        )
        val result = LibraryValidation.validateSetlist(setlist, library)

        assertTrue(result.isValid, "a stale reference must not block saving")
        assertEquals("1 item is no longer in your library", result.warnings[LibraryField.ENTRIES])
    }

    @Test
    fun `multiple stale references are counted`() {
        val setlist = LocalSetlist(
            id = "set",
            name = "Sunday",
            entries = listOf(
                LocalSetlistEntry(SetlistEntryType.SONG, "gone1"),
                LocalSetlistEntry(SetlistEntryType.ANNOUNCEMENT, "gone2"),
            ),
        )
        val result = LibraryValidation.validateSetlist(setlist, library)
        assertEquals("2 items are no longer in your library", result.warnings[LibraryField.ENTRIES])
    }

    /** A Bible entry is a passage, not a library id — there is nothing to resolve. */
    @Test
    fun `bible entries are never reported as missing`() {
        val setlist = LocalSetlist(
            id = "set",
            name = "Sunday",
            entries = listOf(LocalSetlistEntry(SetlistEntryType.BIBLE, "John:3:16")),
        )
        assertTrue(LibraryValidation.validateSetlist(setlist, library).warnings.isEmpty())
    }
}
