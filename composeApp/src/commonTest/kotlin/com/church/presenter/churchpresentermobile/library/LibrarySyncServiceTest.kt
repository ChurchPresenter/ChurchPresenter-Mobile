package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LocalSetlist
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SongVerse
import com.church.presenter.churchpresentermobile.model.SyncOutcome
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LibrarySyncServiceTest {

    private fun repository() = LibraryRepository(InMemoryFileStorage()) { 1_000L }

    private fun catalogueSong(number: String, title: String = "Song $number") =
        Song(number = number, title = title, bookName = "Hymns")

    private fun detail(number: String, vararg verses: String) = SongDetail(
        number = number,
        title = "Song $number",
        songbook = "Hymns",
        verses = verses.mapIndexed { i, text -> SongVerse(number = i + 1, text = text) },
    )

    private fun service(
        repository: LibraryRepository,
        catalogue: Result<List<Song>>,
        details: (Song) -> Result<SongDetail> = { Result.success(detail(it.number, "words")) },
    ) = LibrarySyncService(
        repository = repository,
        fetchCatalogue = { catalogue },
        fetchDetail = { details(it) },
        now = { 2_000L },
    )

    // ── Happy path ───────────────────────────────────────────────────────

    @Test
    fun `a sync pulls the catalogue into the library`() = runTest {
        val repository = repository()
        val outcome = service(
            repository,
            Result.success(listOf(catalogueSong("1"), catalogueSong("2"))),
        ).sync()

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(2, outcome.songCount)
        assertEquals(2, repository.songs.size)
        assertTrue(repository.songs.all { it.origin == ContentOrigin.DESKTOP })
    }

    @Test
    fun `lyrics become typed sections`() = runTest {
        val repository = repository()
        service(
            repository,
            Result.success(listOf(catalogueSong("1"))),
            details = {
                Result.success(
                    SongDetail(
                        number = "1",
                        title = "Song 1",
                        verses = listOf(
                            SongVerse(number = 1, text = "verse one"),
                            SongVerse(label = "Chorus", text = "the chorus"),
                            SongVerse(label = "Bridge", text = "the bridge"),
                        ),
                    )
                )
            },
        ).sync()

        val sections = repository.songs.single().sections
        assertEquals(
            listOf(SectionType.VERSE, SectionType.CHORUS, SectionType.BRIDGE),
            sections.map { it.type },
        )
    }

    @Test
    fun `a plain-text song is split into stanzas on blank lines`() = runTest {
        val repository = repository()
        service(
            repository,
            Result.success(listOf(catalogueSong("1"))),
            details = { Result.success(SongDetail(number = "1", title = "Song 1", text = "one\n\ntwo\n\nthree")) },
        ).sync()

        assertEquals(3, repository.songs.single().sections.size)
    }

    @Test
    fun `an empty catalogue succeeds without touching the library`() = runTest {
        val repository = repository()
        val outcome = service(repository, Result.success(emptyList())).sync()

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(0, outcome.songCount)
    }

    // ── Failure ──────────────────────────────────────────────────────────

    @Test
    fun `an unreachable desktop fails without changing anything`() = runTest {
        val repository = repository()
        repository.upsertSong(
            LocalSong(id = "mine", number = "900", title = "Mine",
                sections = listOf(LocalSongSection(SectionType.VERSE, "words")))
        )

        val outcome = service(repository, Result.failure(IllegalStateException("Connection refused"))).sync()

        assertIs<SyncOutcome.Failed>(outcome)
        assertEquals("Connection refused", outcome.message)
        assertEquals(1, repository.songs.size, "the existing library must be untouched")
    }

    /** One unreadable song must not cost the operator the whole catalogue. */
    @Test
    fun `a song whose lyrics fail is counted and skipped, not fatal`() = runTest {
        val repository = repository()
        val outcome = service(
            repository,
            Result.success(listOf(catalogueSong("1"), catalogueSong("2"), catalogueSong("3"))),
            details = { song ->
                if (song.number == "2") Result.failure(IllegalStateException("500"))
                else Result.success(detail(song.number, "words"))
            },
        ).sync()

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(1, outcome.failedCount)
        assertEquals(2, outcome.songCount)
        assertEquals(listOf("1", "3"), repository.songs.map { it.number })
    }

    // ── Merge behaviour through the service ──────────────────────────────

    @Test
    fun `an edited song survives a re-sync and is reported`() = runTest {
        val repository = repository()
        // Arrives from the desktop…
        service(repository, Result.success(listOf(catalogueSong("1")))).sync()
        // …then the operator fixes a typo, which flags it as an override.
        val synced = repository.songs.single()
        repository.upsertSong(
            synced.copy(sections = listOf(LocalSongSection(SectionType.VERSE, "my corrected words")))
        )
        assertEquals(ContentOrigin.LOCAL_OVERRIDE, repository.songs.single().origin)

        val outcome = service(
            repository,
            Result.success(listOf(catalogueSong("1"))),
            details = { Result.success(detail(it.number, "the server's words")) },
        ).sync()

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(1, outcome.keptLocal)
        assertEquals("my corrected words", repository.songs.single().sections.single().text)
    }

    @Test
    fun `a locally authored song is untouched by a sync`() = runTest {
        val repository = repository()
        repository.upsertSong(
            LocalSong(id = "mine", number = "900", title = "Our Church Song",
                sections = listOf(LocalSongSection(SectionType.VERSE, "words")))
        )

        service(repository, Result.success(listOf(catalogueSong("1")))).sync()

        assertEquals("Our Church Song", repository.songs.first { it.id == "mine" }.title)
        assertEquals(2, repository.songs.size)
    }

    @Test
    fun `a song still used by a setlist is not removed when it leaves the desktop`() = runTest {
        val repository = repository()
        service(repository, Result.success(listOf(catalogueSong("1"), catalogueSong("2")))).sync()
        val keeper = repository.songs.first { it.number == "2" }
        repository.upsertSetlist(
            LocalSetlist(
                id = "set",
                name = "Sunday",
                entries = listOf(LocalSetlistEntry(SetlistEntryType.SONG, keeper.id)),
            )
        )

        service(repository, Result.success(listOf(catalogueSong("1")))).sync()

        assertTrue(repository.songs.any { it.id == keeper.id }, "next Sunday's set must not lose an item")
    }

    @Test
    fun `announcements are left alone by a song sync`() = runTest {
        val repository = repository()
        repository.upsertAnnouncement(
            com.church.presenter.churchpresentermobile.model.LocalAnnouncement(id = "a1", body = "Welcome")
        )

        service(repository, Result.success(listOf(catalogueSong("1")))).sync()

        assertEquals(1, repository.announcements.size)
    }

    // ── Progress ─────────────────────────────────────────────────────────

    @Test
    fun `progress starts and ends idle`() = runTest {
        val repository = repository()
        val sync = service(repository, Result.success(listOf(catalogueSong("1"))))

        assertFalse(sync.progress.value.isRunning)
        sync.sync()
        assertFalse(sync.progress.value.isRunning)
    }

    @Test
    fun `progress reports a fraction of zero before the total is known`() {
        assertEquals(0f, com.church.presenter.churchpresentermobile.model.SyncProgress.IDLE.fraction)
    }

    // ── Section typing ───────────────────────────────────────────────────

    @Test
    fun `section labels map onto typed sections`() {
        val sync = service(repository(), Result.success(emptyList()))

        assertEquals(SectionType.CHORUS, sync.sectionTypeFor("Chorus"))
        assertEquals(SectionType.CHORUS, sync.sectionTypeFor("refrain 2"))
        assertEquals(SectionType.BRIDGE, sync.sectionTypeFor("BRIDGE"))
        assertEquals(SectionType.TAG, sync.sectionTypeFor("Tag"))
        assertEquals(SectionType.ENDING, sync.sectionTypeFor("Outro"))
        // A bare number is a verse — the common case.
        assertEquals(SectionType.VERSE, sync.sectionTypeFor("3"))
        assertEquals(SectionType.VERSE, sync.sectionTypeFor(null))
    }
}
