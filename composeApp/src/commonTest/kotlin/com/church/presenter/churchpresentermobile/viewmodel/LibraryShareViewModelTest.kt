package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.CpsetSerializer
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.ConflictResolution
import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.CpsetDocument
import com.church.presenter.churchpresentermobile.model.CpsetError
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LibraryShareViewModelTest {

    private fun fixture(): Pair<LibraryShareViewModel, LibraryRepository> {
        val repository = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        return LibraryShareViewModel(repository, appVersion = "test", now = { 42L }) to repository
    }

    private fun song(id: String, number: String = "42", title: String = "Amazing Grace") = LocalSong(
        id = id,
        number = number,
        title = title,
        bookName = "Hymns",
        sections = listOf(LocalSongSection(SectionType.VERSE, "words")),
    )

    // ── Export ───────────────────────────────────────────────────────────

    @Test
    fun `exporting produces a readable document`() {
        val (vm, repository) = fixture()
        repository.upsertSong(song("s1"))

        val result = CpsetSerializer.read(vm.exportText())

        assertIs<com.church.presenter.churchpresentermobile.model.CpsetReadResult.Success>(result)
        assertEquals(1, result.document.songs.size)
    }

    @Test
    fun `the export file name carries our extension`() {
        val (vm, _) = fixture()
        assertTrue(vm.exportFileName().endsWith(".cpset"))
    }

    // ── Classifying a picked file ────────────────────────────────────────

    /**
     * Android routinely reports an emailed .cho as application/octet-stream, so
     * the file is classified by content rather than by MIME type or extension.
     */
    @Test
    fun `a cpset document is recognised by its content`() {
        val (vm, _) = fixture()
        val text = CpsetSerializer.write(CpsetDocument(songs = listOf(song("s1"))))
        assertTrue(vm.looksLikeCpset(text))
    }

    @Test
    fun `a chordpro file is not mistaken for a cpset`() {
        val (vm, _) = fixture()
        assertFalse(vm.looksLikeCpset("{title: Amazing Grace}\n[G]Amazing grace"))
    }

    @Test
    fun `unrelated json is not mistaken for a cpset`() {
        val (vm, _) = fixture()
        assertFalse(vm.looksLikeCpset("""{"some":"other json"}"""))
    }

    // ── Importing ────────────────────────────────────────────────────────

    @Test
    fun `importing a cpset previews before writing anything`() {
        val (vm, repository) = fixture()
        val text = CpsetSerializer.write(CpsetDocument(songs = listOf(song("s1"))))

        vm.onFilePicked(text, "library.cpset")

        val state = vm.uiState.value
        assertIs<ShareUiState.Previewing>(state)
        assertEquals(1, state.preview.newCount)
        assertTrue(repository.songs.isEmpty(), "nothing may be written before the user confirms")
    }

    @Test
    fun `confirming writes the items in`() {
        val (vm, repository) = fixture()
        vm.onFilePicked(CpsetSerializer.write(CpsetDocument(songs = listOf(song("s1")))), "library.cpset")

        vm.confirmImport(ConflictResolution.KEEP_MINE)

        assertEquals(1, repository.songs.size)
        assertIs<ShareUiState.Imported>(vm.uiState.value)
    }

    @Test
    fun `imported songs are marked local so a desktop sync will not replace them`() {
        val (vm, repository) = fixture()
        val incoming = song("s1").copy(origin = ContentOrigin.DESKTOP)
        vm.onFilePicked(CpsetSerializer.write(CpsetDocument(songs = listOf(incoming))), "library.cpset")

        vm.confirmImport(ConflictResolution.KEEP_MINE)

        assertEquals(ContentOrigin.LOCAL, repository.songs.single().origin)
    }

    @Test
    fun `keeping mine leaves a conflicting song untouched`() {
        val (vm, repository) = fixture()
        repository.upsertSong(song("mine").copy(
            sections = listOf(LocalSongSection(SectionType.VERSE, "my words"))
        ))
        val incoming = song("theirs").copy(
            sections = listOf(LocalSongSection(SectionType.VERSE, "their words"))
        )
        vm.onFilePicked(CpsetSerializer.write(CpsetDocument(songs = listOf(incoming))), "library.cpset")

        vm.confirmImport(ConflictResolution.KEEP_MINE)

        assertEquals("my words", repository.songs.single().sections.single().text)
    }

    @Test
    fun `replacing takes the file's version`() {
        val (vm, repository) = fixture()
        repository.upsertSong(song("mine").copy(
            sections = listOf(LocalSongSection(SectionType.VERSE, "my words"))
        ))
        val incoming = song("theirs").copy(
            sections = listOf(LocalSongSection(SectionType.VERSE, "their words"))
        )
        vm.onFilePicked(CpsetSerializer.write(CpsetDocument(songs = listOf(incoming))), "library.cpset")

        vm.confirmImport(ConflictResolution.REPLACE)

        assertEquals("their words", repository.songs.single().sections.single().text)
    }

    // ── ChordPro ─────────────────────────────────────────────────────────

    @Test
    fun `a chordpro file imports as one song`() {
        val (vm, repository) = fixture()

        vm.onFilePicked(
            """
            {title: Amazing Grace}
            [G]Amazing grace how sweet the sound

            {soc}
            chorus words
            {eoc}
            """.trimIndent(),
            "amazing-grace.cho",
        )
        vm.confirmImport(ConflictResolution.KEEP_MINE)

        val imported = repository.songs.single()
        assertEquals("Amazing Grace", imported.title)
        assertEquals(2, imported.sections.size)
        assertEquals(SectionType.CHORUS, imported.sections[1].type)
    }

    @Test
    fun `a chordpro file with no title takes it from the file name`() {
        val (vm, repository) = fixture()

        vm.onFilePicked("Just some words", "Be Thou My Vision.chordpro")
        vm.confirmImport(ConflictResolution.KEEP_MINE)

        assertEquals("Be Thou My Vision", repository.songs.single().title)
    }

    // ── Errors ───────────────────────────────────────────────────────────

    @Test
    fun `an unreadable cpset reports an error without writing`() {
        val (vm, repository) = fixture()

        vm.onFilePicked("""{"format": "churchpresenter.set", broken""", "bad.cpset")

        assertIs<ShareUiState.Error>(vm.uiState.value)
        assertTrue(repository.songs.isEmpty())
    }

    @Test
    fun `a file from a newer app version is refused`() {
        val (vm, _) = fixture()

        vm.onFilePicked("""{"format":"churchpresenter.set","version":99,"songs":[]}""", "future.cpset")

        val state = vm.uiState.value
        assertIs<ShareUiState.Error>(state)
        assertEquals(CpsetError.TOO_NEW, state.error)
    }

    @Test
    fun `an empty song file reports empty rather than importing a blank song`() {
        val (vm, repository) = fixture()

        vm.onFilePicked("{title: Nothing Here}", "empty.cho")

        val state = vm.uiState.value
        assertIs<ShareUiState.Error>(state)
        assertEquals(CpsetError.EMPTY, state.error)
        assertTrue(repository.songs.isEmpty())
    }

    @Test
    fun `dismissing returns to the idle state`() {
        val (vm, _) = fixture()
        vm.onFilePicked("bad", "bad.cpset")

        vm.dismiss()

        assertEquals(ShareUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `confirming with nothing pending is a no-op`() {
        val (vm, repository) = fixture()
        vm.confirmImport(ConflictResolution.REPLACE)
        assertTrue(repository.songs.isEmpty())
    }
}
