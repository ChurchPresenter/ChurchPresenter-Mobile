package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the on-device library document: how items name themselves in a list,
 * and what an older build's file still reads as.
 */
class LibraryModelsTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private fun song(id: String = "s1", number: String = "", title: String = "") =
        LocalSong(id = id, number = number, title = title)

    // ── displayTitle ─────────────────────────────────────────────────────

    @Test
    fun `a numbered song shows its number before the title`() {
        assertEquals("42 Amazing Grace", song(number = "42", title = "Amazing Grace").displayTitle)
    }

    @Test
    fun `a hymnal suffix survives, which is why the number is a string`() {
        assertEquals("10b Be Thou My Vision", song(number = "10b", title = "Be Thou My Vision").displayTitle)
    }

    @Test
    fun `an unnumbered song is just its title, with no leading space`() {
        assertEquals("Amazing Grace", song(title = "Amazing Grace").displayTitle)
    }

    @Test
    fun `a number with no title stands alone`() {
        assertEquals("42", song(number = "42").displayTitle)
    }

    @Test
    fun `a blank song has a blank display title rather than a stray separator`() {
        assertEquals("", song().displayTitle)
        // Whitespace-only fields are dropped too, not joined into " ".
        assertEquals("", song(number = "  ", title = "\t").displayTitle)
    }

    // ── LibraryData ──────────────────────────────────────────────────────

    @Test
    fun `an empty library is empty and counts nothing`() {
        assertTrue(LibraryData.EMPTY.isEmpty)
        assertEquals(0, LibraryData.EMPTY.itemCount)
        assertEquals(LIBRARY_SCHEMA_VERSION, LibraryData.EMPTY.version)
    }

    @Test
    fun `any one kind of content makes the library non-empty`() {
        assertFalse(LibraryData(songs = listOf(song())).isEmpty)
        assertFalse(LibraryData(announcements = listOf(LocalAnnouncement("a1"))).isEmpty)
        assertFalse(LibraryData(setlists = listOf(LocalSetlist("l1"))).isEmpty)
    }

    @Test
    fun `itemCount adds up every kind, not just songs`() {
        val library = LibraryData(
            songs = listOf(song("s1"), song("s2")),
            announcements = listOf(LocalAnnouncement("a1")),
            setlists = listOf(LocalSetlist("l1")),
        )

        assertEquals(4, library.itemCount)
    }

    // ── Persistence ──────────────────────────────────────────────────────

    @Test
    fun `a full library round-trips through JSON`() {
        val library = LibraryData(
            songs = listOf(
                LocalSong(
                    id = "s1",
                    number = "42",
                    title = "Amazing Grace",
                    author = "John Newton",
                    bookName = "Hymns",
                    copyright = "Public domain",
                    sections = listOf(
                        LocalSongSection(SectionType.VERSE, "Amazing grace"),
                        LocalSongSection(SectionType.CHORUS, "How sweet", label = "Chorus 1a"),
                    ),
                    origin = ContentOrigin.LOCAL_OVERRIDE,
                    updatedAt = 1_700_000_000_000L,
                ),
            ),
            announcements = listOf(LocalAnnouncement("a1", "Welcome", "Service starts at 10")),
            setlists = listOf(
                LocalSetlist(
                    id = "l1",
                    name = "Sunday",
                    entries = listOf(
                        LocalSetlistEntry(SetlistEntryType.SONG, "s1", "Amazing Grace"),
                        LocalSetlistEntry(SetlistEntryType.BIBLE, "John:3:16-18"),
                    ),
                ),
            ),
        )

        assertEquals(library, json.decodeFromString<LibraryData>(json.encodeToString(library)))
    }

    @Test
    fun `a library file written by an older build still loads`() {
        // Only the fields the first release wrote; everything added since must default.
        val stored = """{"version":1,"songs":[{"id":"s1","title":"Amazing Grace"}]}"""

        val library = json.decodeFromString<LibraryData>(stored)

        val song = library.songs.single()
        assertEquals("", song.number)
        assertEquals(null, song.author)
        assertEquals(emptyList(), song.sections)
        assertEquals(0L, song.updatedAt)
        // An item with no recorded origin is the user's own, so sync will not overwrite it.
        assertEquals(ContentOrigin.LOCAL, song.origin)
        assertEquals(emptyList(), library.announcements)
        assertEquals(1, library.itemCount)
    }

    @Test
    fun `a section defaults to a verse with no text`() {
        val section = LocalSongSection()

        assertEquals(SectionType.VERSE, section.type)
        assertEquals("", section.text)
        assertEquals(null, section.label)
    }

    @Test
    fun `a setlist entry keeps its passage reference verbatim`() {
        // Bible entries carry a passage rather than an id, so the string must survive as-is.
        val entry = LocalSetlistEntry(SetlistEntryType.BIBLE, "John:3:16-18", "John 3:16-18")

        assertEquals(entry, json.decodeFromString<LocalSetlistEntry>(json.encodeToString(entry)))
        assertEquals("John:3:16-18", entry.reference)
    }
}
