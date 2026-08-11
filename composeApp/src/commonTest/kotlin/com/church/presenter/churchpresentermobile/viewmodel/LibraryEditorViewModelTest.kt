package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LibraryField
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
