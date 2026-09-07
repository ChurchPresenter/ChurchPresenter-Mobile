package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LibraryField
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryEditorViewModelTest {

    private fun fixture(): Pair<LibraryEditorViewModel, LibraryRepository> {
        val repository = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        return LibraryEditorViewModel(repository) to repository
    }

    // ── New song ─────────────────────────────────────────────────────────

    @Test
    fun `a new song starts with one empty verse and is not yet savable`() {
        val (vm, _) = fixture()
        vm.editSong(null)

        assertEquals(1, vm.song.value.sections.size)
        assertEquals(SectionType.VERSE, vm.song.value.sections.single().type)
        assertFalse(vm.validation.value.isValid, "a blank song should not save")
        assertFalse(vm.isDirty.value)
    }

    @Test
    fun `filling in a title and a verse makes it savable`() {
        val (vm, repository) = fixture()
        vm.editSong(null)

        vm.setSongTitle("Amazing Grace")
        vm.setSectionText(0, "Amazing grace, how sweet the sound")

        assertTrue(vm.validation.value.isValid)
        assertTrue(vm.saveSong())
        assertEquals("Amazing Grace", repository.songs.single().title)
    }

    @Test
    fun `editing marks the editor dirty and saving clears it`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSongTitle("Title")
        vm.setSectionText(0, "Words")

        assertTrue(vm.isDirty.value)
        vm.saveSong()
        assertFalse(vm.isDirty.value)
    }

    @Test
    fun `an invalid song refuses to save and writes nothing`() {
        val (vm, repository) = fixture()
        vm.editSong(null)
        vm.setSongTitle("Has a title but no words")

        assertFalse(vm.saveSong())
        assertTrue(repository.songs.isEmpty())
    }

    /** Empty sections would otherwise project as blank slides mid-song. */
    @Test
    fun `blank sections are dropped on save`() {
        val (vm, repository) = fixture()
        vm.editSong(null)
        vm.setSongTitle("Song")
        vm.setSectionText(0, "Real words")
        vm.addSection()
        vm.addSection(SectionType.CHORUS)
        vm.setSectionText(2, "Chorus words")

        vm.saveSong()

        assertEquals(2, repository.songs.single().sections.size)
    }

    // ── Editing an existing song ─────────────────────────────────────────

    @Test
    fun `editing an existing song loads it`() {
        val (vm, repository) = fixture()
        vm.editSong(null)
        vm.setSongTitle("Original")
        vm.setSectionText(0, "Words")
        vm.saveSong()
        val id = repository.songs.single().id

        val (other, _) = LibraryEditorViewModel(repository) to repository
        other.editSong(id)

        assertEquals("Original", other.song.value.title)
        assertFalse(other.isDirty.value)
    }

    @Test
    fun `editing an unknown id starts a new song rather than failing`() {
        val (vm, _) = fixture()
        vm.editSong("does-not-exist")
        assertEquals("", vm.song.value.title)
    }

    // ── Sections ─────────────────────────────────────────────────────────

    @Test
    fun `sections can be reordered`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "first")
        vm.addSection()
        vm.setSectionText(1, "second")

        vm.moveSectionDown(0)

        assertEquals(listOf("second", "first"), vm.song.value.sections.map { it.text })
    }

    @Test
    fun `moving past the ends is a no-op rather than a crash`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "only")

        vm.moveSectionUp(0)
        vm.moveSectionDown(0)

        assertEquals(listOf("only"), vm.song.value.sections.map { it.text })
    }

    @Test
    fun `a section can be removed`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "keep")
        vm.addSection()
        vm.setSectionText(1, "drop")

        vm.removeSection(1)

        assertEquals(listOf("keep"), vm.song.value.sections.map { it.text })
    }

    @Test
    fun `section type can be changed`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionType(0, SectionType.CHORUS)
        assertEquals(SectionType.CHORUS, vm.song.value.sections.single().type)
    }

    /** A stanza pasted from a hymnal arrives as one block that will not fit. */
    @Test
    fun `splitting a section breaks it on blank lines`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "verse one\nline two\n\nverse two\nline two")

        vm.splitSection(0)

        assertEquals(2, vm.song.value.sections.size)
        assertEquals("verse one\nline two", vm.song.value.sections[0].text)
        assertEquals("verse two\nline two", vm.song.value.sections[1].text)
    }

    @Test
    fun `splitting a section with nothing to split on leaves it alone`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "one continuous stanza")

        vm.splitSection(0)

        assertEquals(1, vm.song.value.sections.size)
    }

    @Test
    fun `the split keeps the section type of the original`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionType(0, SectionType.BRIDGE)
        vm.setSectionText(0, "part one\n\npart two")

        vm.splitSection(0)

        assertTrue(vm.song.value.sections.all { it.type == SectionType.BRIDGE })
    }

    @Test
    fun `an out-of-range section edit is ignored`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(99, "nowhere")
        assertEquals(1, vm.song.value.sections.size)
    }

    // ── Preview ──────────────────────────────────────────────────────────

    @Test
    fun `the preview renders the section being edited`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSongTitle("Amazing Grace")
        vm.setSectionText(0, "Amazing grace")
        vm.addSection(SectionType.CHORUS)
        vm.setSectionText(1, "Praise the Lord")

        vm.previewSection(1)

        assertEquals("Praise the Lord", vm.previewSlide.value.body)
        assertEquals("Amazing Grace · Chorus", vm.previewSlide.value.reference)
    }

    // ── Announcements ────────────────────────────────────────────────────

    @Test
    fun `an announcement needs a body before it will save`() {
        val (vm, repository) = fixture()
        vm.editAnnouncement(null)

        assertFalse(vm.saveAnnouncement())

        vm.setAnnouncementBody("Prayer meeting Wednesday")
        assertTrue(vm.saveAnnouncement())
        assertEquals("Prayer meeting Wednesday", repository.announcements.single().body)
    }

    @Test
    fun `an over-long announcement reports against the body field`() {
        val (vm, _) = fixture()
        vm.editAnnouncement(null)
        vm.setAnnouncementBody("x".repeat(5_000))

        assertTrue(LibraryField.BODY in vm.validation.value.errors)
    }

    // ── Optional song fields ─────────────────────────────────────────────
    //
    // Author, songbook and copyright are all nullable on [LocalSong]; the editor
    // stores blank as null rather than as "", so a cleared field disappears from
    // the row rather than leaving an empty line under the title.

    @Test
    fun `the optional fields are stored as typed`() {
        val (vm, repository) = fixture()
        vm.editSong(null)
        vm.setSongTitle("Amazing Grace")
        vm.setSectionText(0, "words")

        vm.setSongAuthor("John Newton")
        vm.setSongBook("Hymns")
        vm.setSongCopyright("Public domain")
        vm.saveSong()

        val song = repository.songs.single()
        assertEquals("John Newton", song.author)
        assertEquals("Hymns", song.bookName)
        assertEquals("Public domain", song.copyright)
    }

    @Test
    fun `clearing an optional field stores null rather than an empty string`() {
        val (vm, repository) = fixture()
        vm.editSong(null)
        vm.setSongTitle("Amazing Grace")
        vm.setSectionText(0, "words")
        vm.setSongAuthor("John Newton")

        vm.setSongAuthor("")
        vm.saveSong()

        assertNull(repository.songs.single().author)
    }

    @Test
    fun `a whitespace-only optional field is also stored as null`() {
        val (vm, repository) = fixture()
        vm.editSong(null)
        vm.setSongTitle("Amazing Grace")
        vm.setSectionText(0, "words")

        vm.setSongBook("   ")
        vm.setSongCopyright("\t")
        vm.saveSong()

        assertNull(repository.songs.single().bookName)
        assertNull(repository.songs.single().copyright)
    }

    // ── Reordering sections ──────────────────────────────────────────────

    @Test
    fun `a section can be moved up and back down`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "first")
        vm.addSection()
        vm.setSectionText(1, "second")

        vm.moveSectionUp(1)
        assertEquals(listOf("second", "first"), vm.song.value.sections.map { it.text })

        vm.moveSectionDown(0)
        assertEquals(listOf("first", "second"), vm.song.value.sections.map { it.text })
    }

    @Test
    fun `moving the first section up does nothing`() {
        // The buttons stay tappable at the ends; the guard is what keeps that from
        // dropping a section off the list.
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "only")
        vm.addSection()
        vm.setSectionText(1, "second")

        vm.moveSectionUp(0)

        assertEquals(listOf("only", "second"), vm.song.value.sections.map { it.text })
    }

    @Test
    fun `moving the last section down does nothing`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "first")
        vm.addSection()
        vm.setSectionText(1, "last")

        vm.moveSectionDown(1)

        assertEquals(listOf("first", "last"), vm.song.value.sections.map { it.text })
    }

    @Test
    fun `moving a section that is not there does nothing`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "only")

        vm.moveSectionUp(9)
        vm.moveSectionDown(9)

        assertEquals(1, vm.song.value.sections.size)
    }

    // ── Splitting a pasted stanza ────────────────────────────────────────

    @Test
    fun `a block pasted from a hymnal splits on its blank lines`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "verse one\n\nverse two\n\nverse three")

        vm.splitSection(0)

        assertEquals(listOf("verse one", "verse two", "verse three"), vm.song.value.sections.map { it.text })
    }

    @Test
    fun `a section with nothing to split on is left alone`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "a single stanza with no blank line")

        vm.splitSection(0)

        assertEquals(1, vm.song.value.sections.size)
    }

    @Test
    fun `splitting a section that is not there does nothing`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "words")

        vm.splitSection(9)

        assertEquals(1, vm.song.value.sections.size)
    }

    @Test
    fun `a split drops the label so the parts are renumbered`() {
        // The original label described the whole block; keeping it would leave
        // three sections all called "Verse 1".
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSectionText(0, "one\n\ntwo")

        vm.splitSection(0)

        assertTrue(vm.song.value.sections.all { it.label == null }, vm.song.value.sections.toString())
    }

    // ── Announcements ────────────────────────────────────────────────────

    @Test
    fun `a new announcement starts blank and not dirty`() {
        val (vm, _) = fixture()

        vm.editAnnouncement(null)

        assertEquals("", vm.announcement.value.title)
        assertEquals("", vm.announcement.value.body)
        assertFalse(vm.isDirty.value)
    }

    @Test
    fun `editing an existing announcement loads it`() {
        val (vm, repository) = fixture()
        repository.upsertAnnouncement(LocalAnnouncement("a1", title = "Welcome", body = "Service at 10"))

        vm.editAnnouncement("a1")

        assertEquals("Welcome", vm.announcement.value.title)
        assertEquals("Service at 10", vm.announcement.value.body)
        assertFalse(vm.isDirty.value)
    }

    @Test
    fun `editing an announcement that is not there starts a blank one`() {
        // Reachable from a stale link after the notice was deleted elsewhere.
        val (vm, _) = fixture()

        vm.editAnnouncement("no-such-id")

        assertEquals("", vm.announcement.value.title)
    }

    // ── The fields the editor writes ─────────────────────────────────────
    //
    // Every one of these is a text box the operator types into, and each has a
    // different rule about what an empty box means. Getting that wrong writes
    // an empty string where the model expects "absent", which then shows as a
    // blank line in the song list rather than as nothing at all.

    @Test
    fun `a hymnal number is kept as typed`() {
        // A string, not a number: hymnals use "42a" and "10b".
        val (vm, repository) = fixture()
        vm.editSong(null)

        vm.setSongTitle("Amazing Grace")
        vm.setSongNumber("42a")
        vm.setSectionText(0, "Words")
        vm.saveSong()

        assertEquals("42a", repository.songs.single().number)
    }

    @Test
    fun `a number can be cleared back to nothing`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSongNumber("42")

        vm.setSongNumber("")

        assertEquals("", vm.song.value.number)
    }

    @Test
    fun `setting a number marks the editor dirty`() {
        // The discard prompt keys off this; a change that does not register
        // would be thrown away without asking.
        val (vm, _) = fixture()
        vm.editSong(null)

        vm.setSongNumber("42")

        assertTrue(vm.isDirty.value)
    }

    @Test
    fun `an author left blank is absent rather than empty`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSongAuthor("John Newton")

        vm.setSongAuthor("   ")

        assertNull(vm.song.value.author, "a blank author should be absent")
    }

    @Test
    fun `a songbook left blank is absent rather than empty`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSongBook("Hymns")

        vm.setSongBook("")

        assertNull(vm.song.value.bookName)
    }

    @Test
    fun `a copyright left blank is absent rather than empty`() {
        val (vm, _) = fixture()
        vm.editSong(null)
        vm.setSongCopyright("CCLI 12345")

        vm.setSongCopyright("")

        assertNull(vm.song.value.copyright)
    }

    @Test
    fun `the optional fields survive a save`() {
        val (vm, repository) = fixture()
        vm.editSong(null)
        vm.setSongTitle("Amazing Grace")
        vm.setSongNumber("42")
        vm.setSongAuthor("John Newton")
        vm.setSongBook("Hymns")
        vm.setSongCopyright("Public domain")
        vm.setSectionText(0, "Words")

        vm.saveSong()

        val saved = repository.songs.single()
        assertEquals("John Newton", saved.author)
        assertEquals("Hymns", saved.bookName)
        assertEquals("Public domain", saved.copyright)
    }

    // ── Notices ──────────────────────────────────────────────────────────

    @Test
    fun `a notice's title is kept as typed`() {
        val (vm, repository) = fixture()
        vm.editAnnouncement(null)

        vm.setAnnouncementTitle("Bring a dish")
        vm.setAnnouncementBody("Shared lunch after the service")
        vm.saveAnnouncement()

        assertEquals("Bring a dish", repository.announcements.single().title)
    }

    @Test
    fun `setting a notice title marks the editor dirty`() {
        val (vm, _) = fixture()
        vm.editAnnouncement(null)

        vm.setAnnouncementTitle("Bring a dish")

        assertTrue(vm.isDirty.value)
    }

    @Test
    fun `a notice title can be cleared`() {
        // Unlike a song's author, a blank title is kept as blank: the row falls
        // back to the notice's first line, which is still something to read.
        val (vm, _) = fixture()
        vm.editAnnouncement(null)
        vm.setAnnouncementTitle("Bring a dish")

        vm.setAnnouncementTitle("")

        assertEquals("", vm.announcement.value.title)
    }
}
