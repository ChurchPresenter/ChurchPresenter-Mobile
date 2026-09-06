package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LibraryData
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSetlist
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryRepositoryTest {

    private var clock = 1_000L

    private fun repo(storage: InMemoryFileStorage = InMemoryFileStorage()) =
        LibraryRepository(storage) { clock } to storage

    private fun song(
        id: String = "s1",
        number: String = "42",
        title: String = "Amazing Grace",
        origin: ContentOrigin = ContentOrigin.LOCAL,
    ) = LocalSong(
        id = id,
        number = number,
        title = title,
        origin = origin,
        sections = listOf(LocalSongSection(SectionType.VERSE, "Amazing grace")),
    )

    // ── Load ─────────────────────────────────────────────────────────────

    @Test
    fun `a fresh install loads an empty library`() {
        val (repository, _) = repo()
        assertTrue(repository.load().isEmpty)
    }

    @Test
    fun `saved content survives a reload`() {
        val storage = InMemoryFileStorage()
        val (first, _) = repo(storage)
        first.upsertSong(song())

        val (second, _) = repo(storage)
        val loaded = second.load()

        assertEquals(1, loaded.songs.size)
        assertEquals("Amazing Grace", loaded.songs.single().title)
    }

    /**
     * The failure this guards against is a phone killed mid-write during a
     * service — losing a hand-typed songbook would be unforgivable.
     */
    @Test
    fun `a corrupt library is recovered from the backup`() {
        val storage = InMemoryFileStorage()
        val (first, _) = repo(storage)
        first.upsertSong(song())
        // Second write rolls the good copy into the backup.
        first.upsertSong(song(id = "s2", number = "43", title = "Be Thou My Vision"))

        storage.corrupt(LIBRARY_FILE)

        val (second, _) = repo(storage)
        val recovered = second.load()

        assertFalse(recovered.isEmpty, "should have fallen back to the backup")
        assertEquals(1, recovered.songs.size, "the backup holds the state before the last write")
    }

    @Test
    fun `an unparseable library with no backup degrades to empty rather than crashing`() {
        val storage = InMemoryFileStorage(mapOf(LIBRARY_FILE to "{ not json"))
        val (repository, _) = repo(storage)
        assertTrue(repository.load().isEmpty)
    }

    @Test
    fun `unknown fields from a newer app version are ignored`() {
        val storage = InMemoryFileStorage(
            mapOf(LIBRARY_FILE to """{"version":1,"songs":[],"futureField":"whatever"}""")
        )
        val (repository, _) = repo(storage)
        assertTrue(repository.load().isEmpty)
    }

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun `upsert adds then updates in place rather than duplicating`() {
        val (repository, _) = repo()
        repository.upsertSong(song())
        repository.upsertSong(song(title = "Amazing Grace (rev)"))

        assertEquals(1, repository.songs.size)
        assertEquals("Amazing Grace (rev)", repository.songs.single().title)
    }

    @Test
    fun `upsert stamps the edit time`() {
        val (repository, _) = repo()
        clock = 5_000L
        repository.upsertSong(song())
        assertEquals(5_000L, repository.songs.single().updatedAt)
    }

    /** This flag is what stops a re-sync silently discarding the operator's work. */
    @Test
    fun `editing a desktop song marks it as a local override`() {
        val (repository, _) = repo()
        repository.upsertSong(song(origin = ContentOrigin.DESKTOP))

        repository.upsertSong(song(title = "My edit"))

        assertEquals(ContentOrigin.LOCAL_OVERRIDE, repository.songs.single().origin)
    }

    @Test
    fun `editing a local song leaves it local`() {
        val (repository, _) = repo()
        repository.upsertSong(song())
        repository.upsertSong(song(title = "Edited"))
        assertEquals(ContentOrigin.LOCAL, repository.songs.single().origin)
    }

    @Test
    fun `an override stays an override across further edits`() {
        val (repository, _) = repo()
        repository.upsertSong(song(origin = ContentOrigin.DESKTOP))
        repository.upsertSong(song(title = "First edit"))
        repository.upsertSong(song(title = "Second edit"))
        assertEquals(ContentOrigin.LOCAL_OVERRIDE, repository.songs.single().origin)
    }

    @Test
    fun `a newly synced desktop song keeps its desktop origin`() {
        val (repository, _) = repo()
        repository.upsertSong(song(origin = ContentOrigin.DESKTOP))
        assertEquals(ContentOrigin.DESKTOP, repository.songs.single().origin)
    }

    @Test
    fun `delete removes only the named song`() {
        val (repository, _) = repo()
        repository.upsertSong(song(id = "s1"))
        repository.upsertSong(song(id = "s2", number = "43"))

        repository.deleteSong("s1")

        assertEquals(listOf("s2"), repository.songs.map { it.id })
    }

    @Test
    fun `deleting an unknown id is harmless`() {
        val (repository, _) = repo()
        repository.upsertSong(song())
        repository.deleteSong("nope")
        assertEquals(1, repository.songs.size)
    }

    @Test
    fun `lookup by id returns null when absent`() {
        val (repository, _) = repo()
        assertNull(repository.song("missing"))
        repository.upsertSong(song())
        assertEquals("Amazing Grace", repository.song("s1")?.title)
    }

    // ── Announcements & setlists ─────────────────────────────────────────

    @Test
    fun `announcements round-trip and follow the same override rule`() {
        val storage = InMemoryFileStorage()
        val (repository, _) = repo(storage)
        repository.upsertAnnouncement(
            LocalAnnouncement(id = "a1", title = "Welcome", body = "Hi", origin = ContentOrigin.DESKTOP)
        )
        repository.upsertAnnouncement(LocalAnnouncement(id = "a1", title = "Welcome", body = "Edited"))

        val reloaded = LibraryRepository(storage) { clock }.load()
        assertEquals("Edited", reloaded.announcements.single().body)
        assertEquals(ContentOrigin.LOCAL_OVERRIDE, reloaded.announcements.single().origin)
    }

    @Test
    fun `setlists round-trip`() {
        val storage = InMemoryFileStorage()
        val (repository, _) = repo(storage)
        repository.upsertSetlist(LocalSetlist(id = "set1", name = "Sunday morning"))

        assertEquals("Sunday morning", LibraryRepository(storage) { clock }.load().setlists.single().name)
    }

    @Test
    fun `deleting a setlist leaves the songs it referenced alone`() {
        val (repository, _) = repo()
        repository.upsertSong(song())
        repository.upsertSetlist(LocalSetlist(id = "set1", name = "Sunday"))

        repository.deleteSetlist("set1")

        assertTrue(repository.setlists.isEmpty())
        assertEquals(1, repository.songs.size)
    }

    // ── Bulk ─────────────────────────────────────────────────────────────

    @Test
    fun `replaceAll swaps the whole library`() {
        val (repository, _) = repo()
        repository.upsertSong(song())

        repository.replaceAll(LibraryData(songs = listOf(song(id = "new", title = "Replaced"))))

        assertEquals(listOf("Replaced"), repository.songs.map { it.title })
    }

    @Test
    fun `clear empties the library and persists that`() {
        val storage = InMemoryFileStorage()
        val (repository, _) = repo(storage)
        repository.upsertSong(song())

        repository.clear()

        assertTrue(repository.library.value.isEmpty)
        assertTrue(LibraryRepository(storage) { clock }.load().isEmpty)
    }

    @Test
    fun `clear deletes the backup as well as the library`() {
        // Otherwise everything the user asked to delete is still on the device,
        // in library.bak, taking the space it always did.
        val storage = InMemoryFileStorage()
        val (repository, _) = repo(storage)
        repository.upsertSong(song())
        repository.upsertSong(song(id = "s2", title = "Be Thou My Vision"))

        repository.clear()

        assertFalse(storage.contains(LIBRARY_BACKUP_FILE))
    }

    @Test
    fun `clearing songs keeps notices`() {
        val (repository, _) = repo()
        repository.upsertSong(song())
        repository.upsertAnnouncement(LocalAnnouncement(id = "a1", title = "Welcome"))

        repository.clearSongs()

        assertTrue(repository.songs.isEmpty())
        assertEquals(listOf("a1"), repository.announcements.map { it.id })
    }

    @Test
    fun `clearing songs drops the service entries that pointed at them`() {
        // A running order full of rows that open nothing is worse than an empty one.
        val (repository, _) = repo()
        repository.upsertSong(song(id = "s1"))
        repository.upsertSetlist(
            LocalSetlist(
                id = "current-service",
                entries = listOf(
                    LocalSetlistEntry(SetlistEntryType.SONG, "s1", "Amazing Grace"),
                    LocalSetlistEntry(SetlistEntryType.BIBLE, "John:3:16", "John 3:16"),
                    LocalSetlistEntry(SetlistEntryType.ANNOUNCEMENT, "a1", "Welcome"),
                ),
            )
        )

        repository.clearSongs()

        val entries = repository.setlist("current-service")!!.entries
        assertEquals(
            listOf(SetlistEntryType.BIBLE, SetlistEntryType.ANNOUNCEMENT),
            entries.map { it.type },
        )
    }

    @Test
    fun `clearing songs deletes the backup too`() {
        val storage = InMemoryFileStorage()
        val (repository, _) = repo(storage)
        repository.upsertSong(song())
        repository.upsertSong(song(id = "s2", title = "Be Thou My Vision"))

        repository.clearSongs()

        assertFalse(storage.contains(LIBRARY_BACKUP_FILE))
        assertTrue(LibraryRepository(storage) { clock }.load().songs.isEmpty())
    }

    // ── Persistence behaviour ────────────────────────────────────────────

    @Test
    fun `a failing disk does not lose the in-memory library`() {
        val storage = InMemoryFileStorage()
        val (repository, _) = repo(storage)
        storage.failWrites = true

        repository.upsertSong(song())

        assertEquals(1, repository.songs.size, "the operator should still see their edit")
        assertFalse(storage.contains(LIBRARY_FILE))
    }

    @Test
    fun `the library flow emits on every change`() {
        val (repository, _) = repo()
        val seen = mutableListOf<Int>()

        seen += repository.library.value.itemCount
        repository.upsertSong(song())
        seen += repository.library.value.itemCount
        repository.upsertAnnouncement(LocalAnnouncement(id = "a1", body = "Hi"))
        seen += repository.library.value.itemCount

        assertEquals(listOf(0, 1, 2), seen)
    }
}
