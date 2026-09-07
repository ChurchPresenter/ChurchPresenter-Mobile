package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.CPSET_VERSION
import com.church.presenter.churchpresentermobile.model.ConflictResolution
import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.CpsetDocument
import com.church.presenter.churchpresentermobile.model.CpsetError
import com.church.presenter.churchpresentermobile.model.CpsetReadResult
import com.church.presenter.churchpresentermobile.model.LibraryData
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSetlist
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CpsetSerializerTest {

    private fun song(
        id: String,
        number: String = "42",
        title: String = "Amazing Grace",
        body: String = "words",
        book: String? = "Hymns",
    ) = LocalSong(
        id = id,
        number = number,
        title = title,
        bookName = book,
        sections = listOf(LocalSongSection(SectionType.VERSE, body)),
    )

    // ── Round trip ───────────────────────────────────────────────────────

    @Test
    fun `a document survives a round trip`() {
        val original = CpsetDocument(
            exportedAt = 1_700_000_000_000L,
            appVersion = "1.0.18",
            name = "Sunday morning",
            songs = listOf(song("s1")),
            announcements = listOf(LocalAnnouncement(id = "a1", title = "Welcome", body = "Hi")),
            setlists = listOf(
                LocalSetlist(
                    id = "set1",
                    name = "Sunday",
                    entries = listOf(LocalSetlistEntry(SetlistEntryType.SONG, "s1")),
                )
            ),
        )

        val result = CpsetSerializer.read(CpsetSerializer.write(original))

        assertIs<CpsetReadResult.Success>(result)
        assertEquals(original, result.document)
    }

    @Test
    fun `exporting the library carries everything`() {
        val library = LibraryData(
            songs = listOf(song("s1"), song("s2", number = "43")),
            announcements = listOf(LocalAnnouncement(id = "a1", body = "Hi")),
        )

        val document = CpsetSerializer.exportLibrary(library, name = "My library", exportedAt = 1L)

        assertEquals(2, document.songs.size)
        assertEquals(1, document.announcements.size)
        assertEquals(CPSET_VERSION, document.version)
    }

    /** Sharing one Sunday, not an entire hymnal, is the common case. */
    @Test
    fun `exporting a setlist carries only what it uses`() {
        val library = LibraryData(
            songs = listOf(song("s1"), song("s2", number = "43"), song("s3", number = "44")),
            announcements = listOf(
                LocalAnnouncement(id = "a1", body = "used"),
                LocalAnnouncement(id = "a2", body = "unused"),
            ),
            setlists = listOf(
                LocalSetlist(
                    id = "set1",
                    name = "Sunday",
                    entries = listOf(
                        LocalSetlistEntry(SetlistEntryType.SONG, "s1"),
                        LocalSetlistEntry(SetlistEntryType.ANNOUNCEMENT, "a1"),
                    ),
                )
            ),
        )

        val document = CpsetSerializer.exportSetlist(library, "set1", exportedAt = 1L)

        assertEquals(listOf("s1"), document?.songs?.map { it.id })
        assertEquals(listOf("a1"), document?.announcements?.map { it.id })
        assertEquals("Sunday", document?.name)
    }

    @Test
    fun `exporting an unknown setlist yields nothing`() {
        assertEquals(null, CpsetSerializer.exportSetlist(LibraryData.EMPTY, "nope", 1L))
    }

    // ── Reading failures ─────────────────────────────────────────────────

    @Test
    fun `unparseable text is reported as unreadable`() {
        val result = CpsetSerializer.read("this is not json at all")
        assertIs<CpsetReadResult.Failure>(result)
        assertEquals(CpsetError.UNREADABLE, result.error)
    }

    @Test
    fun `valid json that is not one of our files is rejected`() {
        val result = CpsetSerializer.read("""{"format":"something.else","version":1,"songs":[]}""")
        assertIs<CpsetReadResult.Failure>(result)
        assertEquals(CpsetError.WRONG_FORMAT, result.error)
    }

    /** Silently dropping fields the sender cared about is worse than refusing. */
    @Test
    fun `a newer format version is refused rather than partially read`() {
        val result = CpsetSerializer.read(
            """{"format":"churchpresenter.set","version":${CPSET_VERSION + 1},"songs":[]}"""
        )
        assertIs<CpsetReadResult.Failure>(result)
        assertEquals(CpsetError.TOO_NEW, result.error)
    }

    @Test
    fun `an older format version is still accepted`() {
        val document = CpsetDocument(version = 1, songs = listOf(song("s1")))
        assertIs<CpsetReadResult.Success>(CpsetSerializer.read(CpsetSerializer.write(document)))
    }

    @Test
    fun `a document with nothing in it is reported as empty`() {
        val result = CpsetSerializer.read(CpsetSerializer.write(CpsetDocument()))
        assertIs<CpsetReadResult.Failure>(result)
        assertEquals(CpsetError.EMPTY, result.error)
    }

    @Test
    fun `unknown fields from a newer writer at the same version are ignored`() {
        val result = CpsetSerializer.read(
            """{"format":"churchpresenter.set","version":1,"mood":"joyful",
               "songs":[{"id":"s1","title":"Song","sections":[]}]}"""
        )
        assertIs<CpsetReadResult.Success>(result)
        assertEquals(1, result.document.songs.size)
    }

    // ── Preview ──────────────────────────────────────────────────────────

    @Test
    fun `everything is new against an empty library`() {
        val document = CpsetDocument(songs = listOf(song("s1"), song("s2", number = "43")))

        val preview = CpsetSerializer.preview(document, LibraryData.EMPTY)

        assertEquals(2, preview.newCount)
        assertEquals(0, preview.conflictCount)
    }

    @Test
    fun `a song already present is reported as a conflict, not silently skipped`() {
        val library = LibraryData(songs = listOf(song("mine", number = "42")))
        val document = CpsetDocument(songs = listOf(song("theirs", number = "42", body = "their words")))

        val preview = CpsetSerializer.preview(document, library)

        assertEquals(0, preview.newCount)
        assertEquals(1, preview.conflictCount)
    }

    @Test
    fun `conflict matching uses songbook and number like a desktop sync does`() {
        val library = LibraryData(songs = listOf(song("mine", number = "42", book = "Hymns")))
        val document = CpsetDocument(
            songs = listOf(
                song("a", number = "42", book = "Hymns"),
                song("b", number = "42", book = "Mission Praise"),
            )
        )

        val preview = CpsetSerializer.preview(document, library)

        assertEquals(1, preview.conflictCount)
        assertEquals(1, preview.newCount)
    }

    @Test
    fun `an announcement is matched on its title`() {
        val library = LibraryData(
            announcements = listOf(LocalAnnouncement(id = "a1", title = "Welcome", body = "old"))
        )
        val document = CpsetDocument(
            announcements = listOf(LocalAnnouncement(id = "a2", title = "Welcome", body = "new"))
        )

        assertEquals(1, CpsetSerializer.preview(document, library).conflictCount)
    }

    // ── Apply ────────────────────────────────────────────────────────────

    @Test
    fun `keeping mine leaves conflicting items untouched`() {
        val library = LibraryData(songs = listOf(song("mine", number = "42", body = "my words")))
        val document = CpsetDocument(songs = listOf(song("theirs", number = "42", body = "their words")))
        val preview = CpsetSerializer.preview(document, library)

        val merged = CpsetSerializer.apply(preview, library, ConflictResolution.KEEP_MINE)

        assertEquals(1, merged.songs.size)
        assertEquals("my words", merged.songs.single().sections.single().text)
    }

    @Test
    fun `replacing takes the file's version but keeps the existing id`() {
        val library = LibraryData(songs = listOf(song("mine", number = "42", body = "my words")))
        val document = CpsetDocument(songs = listOf(song("theirs", number = "42", body = "their words")))
        val preview = CpsetSerializer.preview(document, library)

        val merged = CpsetSerializer.apply(preview, library, ConflictResolution.REPLACE)

        assertEquals(1, merged.songs.size)
        assertEquals("their words", merged.songs.single().sections.single().text)
        assertEquals("mine", merged.songs.single().id, "setlists pointing at this must still resolve")
    }

    @Test
    fun `new items are added under both resolutions`() {
        val library = LibraryData(songs = listOf(song("mine", number = "42")))
        val document = CpsetDocument(songs = listOf(song("new", number = "43")))
        val preview = CpsetSerializer.preview(document, library)

        ConflictResolution.entries.forEach { resolution ->
            assertEquals(2, CpsetSerializer.apply(preview, library, resolution).songs.size)
        }
    }

    /** Imported content came from a person, so a desktop sync must not replace it. */
    @Test
    fun `imported items are marked as locally owned`() {
        val document = CpsetDocument(
            songs = listOf(song("s1").copy(origin = ContentOrigin.DESKTOP))
        )
        val preview = CpsetSerializer.preview(document, LibraryData.EMPTY)

        val merged = CpsetSerializer.apply(preview, LibraryData.EMPTY, ConflictResolution.KEEP_MINE)

        assertEquals(ContentOrigin.LOCAL, merged.songs.single().origin)
    }

    @Test
    fun `importing stamps the time so the library shows it as recent`() {
        val document = CpsetDocument(songs = listOf(song("s1")))
        val preview = CpsetSerializer.preview(document, LibraryData.EMPTY)

        val merged = CpsetSerializer.apply(preview, LibraryData.EMPTY, ConflictResolution.KEEP_MINE, importedAt = 99L)

        assertEquals(99L, merged.songs.single().updatedAt)
    }

    @Test
    fun `a setlist already present is not duplicated`() {
        val setlist = LocalSetlist(id = "set1", name = "Sunday")
        val library = LibraryData(setlists = listOf(setlist))
        val document = CpsetDocument(songs = listOf(song("s1")), setlists = listOf(setlist))

        val preview = CpsetSerializer.preview(document, library)
        val merged = CpsetSerializer.apply(preview, library, ConflictResolution.KEEP_MINE)

        assertEquals(1, merged.setlists.size)
    }

    @Test
    fun `applying an empty preview changes nothing`() {
        val library = LibraryData(songs = listOf(song("mine")))
        val preview = CpsetSerializer.preview(CpsetDocument(), library)

        assertTrue(preview.isEmpty)
        assertEquals(library, CpsetSerializer.apply(preview, library, ConflictResolution.KEEP_MINE))
    }

    // ── Apply: announcements ─────────────────────────────────────────────
    //
    // The same three rules as songs, on the other kind of content. Worth stating
    // separately because they are a separate code path — a fix applied to one
    // branch and not the other is invisible until a church imports notices.

    private fun notice(id: String, title: String = "Welcome", body: String = "Service at 10") =
        LocalAnnouncement(id = id, title = title, body = body)

    @Test
    fun `a new announcement is imported and marked local`() {
        // Marked LOCAL so a later desktop sync treats it as the user's own and
        // does not overwrite it.
        val document = CpsetDocument(announcements = listOf(notice("theirs")))
        val preview = CpsetSerializer.preview(document, LibraryData.EMPTY)

        val merged = CpsetSerializer.apply(preview, LibraryData.EMPTY, ConflictResolution.KEEP_MINE, importedAt = 99L)

        assertEquals(1, merged.announcements.size)
        assertEquals(ContentOrigin.LOCAL, merged.announcements.single().origin)
        assertEquals(99L, merged.announcements.single().updatedAt)
    }

    @Test
    fun `keeping mine leaves a conflicting announcement untouched`() {
        val library = LibraryData(announcements = listOf(notice("mine", body = "my words")))
        val document = CpsetDocument(announcements = listOf(notice("theirs", body = "their words")))
        val preview = CpsetSerializer.preview(document, library)

        val merged = CpsetSerializer.apply(preview, library, ConflictResolution.KEEP_MINE)

        assertEquals(1, merged.announcements.size)
        assertEquals("my words", merged.announcements.single().body)
    }

    @Test
    fun `replacing takes the file's announcement but keeps the existing id`() {
        val library = LibraryData(announcements = listOf(notice("mine", body = "my words")))
        val document = CpsetDocument(announcements = listOf(notice("theirs", body = "their words")))
        val preview = CpsetSerializer.preview(document, library)

        val merged = CpsetSerializer.apply(preview, library, ConflictResolution.REPLACE)

        assertEquals(1, merged.announcements.size)
        assertEquals("their words", merged.announcements.single().body)
        assertEquals("mine", merged.announcements.single().id, "setlists pointing at this must still resolve")
    }

    @Test
    fun `songs and announcements are merged in the same pass`() {
        val library = LibraryData(
            songs = listOf(song("mySong", body = "my words")),
            announcements = listOf(notice("myNotice", body = "my notice")),
        )
        val document = CpsetDocument(
            songs = listOf(song("theirSong", body = "their words")),
            announcements = listOf(notice("theirNotice", body = "their notice")),
        )
        val preview = CpsetSerializer.preview(document, library)

        val merged = CpsetSerializer.apply(preview, library, ConflictResolution.REPLACE)

        assertEquals("their words", merged.songs.single().sections.single().text)
        assertEquals("their notice", merged.announcements.single().body)
    }
}
