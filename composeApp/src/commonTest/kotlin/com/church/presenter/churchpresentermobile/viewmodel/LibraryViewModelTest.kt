package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the library browser's filtering and search.
 *
 * The behaviour worth pinning down here is that browsing is *only* browsing —
 * the screen used to project whatever row you tapped, so opening your own
 * library put it on the audience screen. This ViewModel holds no presenter at
 * all, which is what makes that impossible rather than merely unlikely.
 */
class LibraryViewModelTest {

    private fun fixture(
        songs: List<LocalSong> = emptyList(),
        announcements: List<LocalAnnouncement> = emptyList(),
    ): Pair<LibraryViewModel, LibraryRepository> {
        val repository = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        songs.forEach(repository::upsertSong)
        announcements.forEach(repository::upsertAnnouncement)
        return LibraryViewModel(repository) to repository
    }

    private fun song(id: String, title: String, number: String = "") =
        LocalSong(id = id, number = number, title = title)

    private val grace = song("s1", "Amazing Grace", "42")
    private val vision = song("s2", "Be Thou My Vision", "10")
    private val welcome = LocalAnnouncement("a1", title = "Welcome", body = "Service starts at 10")
    private val giving = LocalAnnouncement("a2", title = "Giving", body = "Details at the back")

    // ── Defaults ─────────────────────────────────────────────────────────

    @Test
    fun `starts unfiltered with an empty query`() {
        val (vm, _) = fixture()

        assertEquals("", vm.query.value)
        assertEquals(LibraryFilter.ALL, vm.filter.value)
    }

    @Test
    fun `an empty query shows everything`() {
        val (vm, _) = fixture(songs = listOf(grace, vision), announcements = listOf(welcome))

        assertEquals(2, vm.visibleSongs().size)
        assertEquals(1, vm.visibleAnnouncements().size)
    }

    // ── Search ───────────────────────────────────────────────────────────

    @Test
    fun `a query narrows the songs`() {
        val (vm, _) = fixture(songs = listOf(grace, vision))

        vm.setQuery("grace")

        assertEquals(listOf("s1"), vm.visibleSongs().map { it.id })
    }

    @Test
    fun `a query narrows announcements too`() {
        val (vm, _) = fixture(announcements = listOf(welcome, giving))

        vm.setQuery("giving")

        assertEquals(listOf("a2"), vm.visibleAnnouncements().map { it.id })
    }

    @Test
    fun `a query that matches nothing shows nothing rather than everything`() {
        val (vm, _) = fixture(songs = listOf(grace, vision), announcements = listOf(welcome))

        vm.setQuery("zzzzzzz")

        assertTrue(vm.visibleSongs().isEmpty())
        assertTrue(vm.visibleAnnouncements().isEmpty())
    }

    @Test
    fun `clearing the query restores the full list`() {
        val (vm, _) = fixture(songs = listOf(grace, vision))
        vm.setQuery("grace")

        vm.setQuery("")

        assertEquals(2, vm.visibleSongs().size)
    }

    // ── Filters ──────────────────────────────────────────────────────────

    @Test
    fun `the songs filter hides announcements`() {
        val (vm, _) = fixture(songs = listOf(grace), announcements = listOf(welcome))

        vm.setFilter(LibraryFilter.SONGS)

        assertEquals(1, vm.visibleSongs().size)
        assertTrue(vm.visibleAnnouncements().isEmpty())
    }

    @Test
    fun `the announcements filter hides songs`() {
        val (vm, _) = fixture(songs = listOf(grace), announcements = listOf(welcome))

        vm.setFilter(LibraryFilter.ANNOUNCEMENTS)

        assertTrue(vm.visibleSongs().isEmpty())
        assertEquals(1, vm.visibleAnnouncements().size)
    }

    @Test
    fun `the setlists filter hides both other kinds`() {
        val (vm, _) = fixture(songs = listOf(grace), announcements = listOf(welcome))

        vm.setFilter(LibraryFilter.SETLISTS)

        assertTrue(vm.visibleSongs().isEmpty())
        assertTrue(vm.visibleAnnouncements().isEmpty())
    }

    @Test
    fun `filter and query apply together`() {
        val (vm, _) = fixture(songs = listOf(grace, vision), announcements = listOf(welcome))

        vm.setFilter(LibraryFilter.SONGS)
        vm.setQuery("vision")

        assertEquals(listOf("s2"), vm.visibleSongs().map { it.id })
        assertTrue(vm.visibleAnnouncements().isEmpty())
    }

    // ── Deletion ─────────────────────────────────────────────────────────

    @Test
    fun `deleting a song removes it from the library and the list`() {
        val (vm, repository) = fixture(songs = listOf(grace, vision))

        vm.deleteSong("s1")

        assertEquals(listOf("s2"), repository.songs.map { it.id })
        assertEquals(listOf("s2"), vm.visibleSongs().map { it.id })
    }

    @Test
    fun `deleting an announcement removes it`() {
        val (vm, repository) = fixture(announcements = listOf(welcome, giving))

        vm.deleteAnnouncement("a1")

        assertEquals(listOf("a2"), repository.announcements.map { it.id })
        assertEquals(listOf("a2"), vm.visibleAnnouncements().map { it.id })
    }

    @Test
    fun `deleting an id that is not there changes nothing`() {
        val (vm, repository) = fixture(songs = listOf(grace))

        vm.deleteSong("does-not-exist")

        assertEquals(listOf("s1"), repository.songs.map { it.id })
    }

    // ── The library flow ─────────────────────────────────────────────────

    @Test
    fun `the library flow follows edits made elsewhere`() {
        // The Library tab and the editor share a repository; a save in one must
        // show up in the other without a reload.
        val (vm, repository) = fixture(songs = listOf(grace))

        repository.upsertSong(song("s3", "New Song"))

        assertEquals(2, vm.library.value.songs.size)
        assertEquals(2, vm.visibleSongs().size)
    }
}
