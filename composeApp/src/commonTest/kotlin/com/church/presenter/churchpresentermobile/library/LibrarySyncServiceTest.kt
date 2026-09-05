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

    private fun catalogueSong(number: String, title: String = "Song $number", book: String = "Hymns") =
        Song(number = number, title = title, bookName = book)

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

    // ── Cancelling ───────────────────────────────────────────────────────

    @Test
    fun `cancelling reports the cancelled outcome and keeps what was written`() = runTest {
        // Previously cancel() killed the coroutine awaiting sync(), so the line
        // publishing the outcome never ran and the sheet just stopped.
        val repository = repository()
        val catalogue = (1..120).map { catalogueSong(it.toString()) }
        lateinit var sync: LibrarySyncService
        sync = LibrarySyncService(
            repository = repository,
            fetchCatalogue = { Result.success(catalogue) },
            fetchDetail = { song ->
                // Ask to stop once the first batch is safely written.
                if (song.number.toInt() > 60) sync.requestCancel()
                Result.success(detail(song.number, "words"))
            },
            now = { 2_000L },
        )

        val outcome = sync.sync()

        assertIs<SyncOutcome.Cancelled>(outcome)
        assertTrue(repository.songs.isNotEmpty(), "batches already written are kept")
        assertTrue(repository.songs.size < catalogue.size, "a cancelled sync stops short")
    }

    @Test
    fun `progress returns to idle however the sync ends`() = runTest {
        val failed = service(repository(), Result.failure(Exception("no route to host")))
        failed.sync()
        assertEquals(com.church.presenter.churchpresentermobile.model.SyncProgress.IDLE, failed.progress.value)

        val ok = service(repository(), Result.success(listOf(catalogueSong("1"))))
        ok.sync()
        assertEquals(com.church.presenter.churchpresentermobile.model.SyncProgress.IDLE, ok.progress.value)
    }

    @Test
    fun `progress is preparing until the catalogue answers`() = runTest {
        var seenWhileFetching: com.church.presenter.churchpresentermobile.model.SyncProgress? = null
        lateinit var sync: LibrarySyncService
        sync = LibrarySyncService(
            repository = repository(),
            fetchCatalogue = {
                seenWhileFetching = sync.progress.value
                Result.success(listOf(catalogueSong("1")))
            },
            fetchDetail = { Result.success(detail(it.number, "words")) },
            now = { 2_000L },
        )

        sync.sync()

        // Running with no total — the UI shows an indeterminate bar here rather
        // than "Copying 0 of 0…", which read as a hung app.
        assertTrue(seenWhileFetching!!.isPreparing)
        assertTrue(seenWhileFetching!!.isRunning)
    }

    @Test
    fun `every failed detail is counted exactly once`() = runTest {
        // The counters were incremented from four concurrent coroutines, which
        // loses updates on any dispatcher that is not single-threaded.
        val catalogue = (1..40).map { catalogueSong(it.toString()) }
        val outcome = service(
            repository(),
            Result.success(catalogue),
            details = { song ->
                if (song.number.toInt() % 3 == 0) Result.failure(Exception("unreadable"))
                else Result.success(detail(song.number, "words"))
            },
        ).sync()

        val expectedFailures = catalogue.count { it.number.toInt() % 3 == 0 }
        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(expectedFailures, outcome.failedCount)
        assertEquals(catalogue.size - expectedFailures, outcome.songCount)
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

    // ── Choosing songbooks ───────────────────────────────────────────────────

    @Test
    fun onlyTheChosenSongbooksAreCopied() = runTest {
        // A church that uses one of several books on the computer should not
        // have to carry the rest on a phone.
        val repository = repository()
        val catalogue = listOf(
            catalogueSong("1", book = "Hymns"),
            catalogueSong("2", book = "Chorus Book"),
            catalogueSong("3", book = "Hymns"),
        )

        val outcome = service(repository, Result.success(catalogue)).sync(setOf("Hymns"))

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(2, outcome.songCount)
        assertEquals(setOf("Hymns"), repository.library.value.songs.map { it.bookName }.toSet())
    }

    @Test
    fun noChoiceStillCopiesEverything() = runTest {
        // The default path, and what every caller did before books could be
        // chosen: null must not be read as "nothing selected".
        val repository = repository()
        val catalogue = listOf(
            catalogueSong("1", book = "Hymns"),
            catalogueSong("2", book = "Chorus Book"),
        )

        val outcome = service(repository, Result.success(catalogue)).sync(null)

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(2, outcome.songCount)
    }

    @Test
    fun choosingABookTheComputerDoesNotHaveCopiesNothing() = runTest {
        val repository = repository()
        val catalogue = listOf(catalogueSong("1", book = "Hymns"))

        val outcome = service(repository, Result.success(catalogue)).sync(setOf("Nothing Here"))

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(0, outcome.songCount)
        assertTrue(repository.library.value.songs.isEmpty())
    }

    // ── Catalogues larger than one batch ─────────────────────────────────

    @Test
    fun aCatalogueSpanningManyBatchesKeepsEveryBatch() = runTest {
        // The reported failure: copying several thousand songs left only the
        // last batch behind, because each batch's merge treated the songs of
        // every batch before it as songs the computer had deleted.
        val repository = repository()
        val catalogue = (1..601).map { catalogueSong(it.toString()) }

        val outcome = service(repository, Result.success(catalogue)).sync()

        assertIs<SyncOutcome.Success>(outcome)
        assertEquals(601, outcome.songCount)
        assertEquals(601, repository.songs.size)
    }

    @Test
    fun aBatchedSyncSurvivesBeingLoadedAgain() = runTest {
        // In memory is not the claim being made — the user restarted the app.
        val storage = InMemoryFileStorage()
        val repository = LibraryRepository(storage) { 1_000L }
        val catalogue = (1..601).map { catalogueSong(it.toString()) }

        service(repository, Result.success(catalogue)).sync()

        assertEquals(601, LibraryRepository(storage) { 1_000L }.load().songs.size)
    }

    @Test
    fun aSongWhoseLyricsFailedIsNotTreatedAsDeleted() = runTest {
        // It failed to download, which says nothing about whether the computer
        // still has it — and pruning it would lose an earlier good copy.
        val repository = repository()
        repository.upsertSong(
            LocalSong(
                id = "kept",
                number = "2",
                title = "Song 2",
                bookName = "Hymns",
                sections = listOf(LocalSongSection(type = SectionType.VERSE, text = "old words")),
                origin = ContentOrigin.DESKTOP,
            )
        )

        service(
            repository,
            Result.success(listOf(catalogueSong("1"), catalogueSong("2"))),
            details = { song ->
                if (song.number == "2") Result.failure(RuntimeException("no lyrics"))
                else Result.success(detail(song.number, "words"))
            },
        ).sync()

        assertEquals(setOf("1", "2"), repository.songs.map { it.number }.toSet())
    }

    @Test
    fun copyingOneBookLeavesTheOtherBooksAlone() = runTest {
        // Choosing a book is a narrower request, not an instruction to throw
        // away everything else already on the phone.
        val repository = repository()
        repository.upsertSong(
            LocalSong(
                id = "chorus-1",
                number = "1",
                title = "Chorus 1",
                bookName = "Chorus Book",
                sections = listOf(LocalSongSection(type = SectionType.VERSE, text = "words")),
                origin = ContentOrigin.DESKTOP,
            )
        )

        service(
            repository,
            Result.success(listOf(catalogueSong("1", book = "Hymns"))),
        ).sync(setOf("Hymns"))

        assertEquals(
            setOf("Chorus Book", "Hymns"),
            repository.songs.mapNotNull { it.bookName }.toSet(),
        )
    }

    @Test
    fun aSongTheComputerNoLongerHasIsStillDropped() = runTest {
        // The prune still has to happen — a full sync is how a deleted song
        // leaves the phone.
        val repository = repository()
        repository.upsertSong(
            LocalSong(
                id = "gone",
                number = "99",
                title = "Withdrawn",
                bookName = "Hymns",
                sections = listOf(LocalSongSection(type = SectionType.VERSE, text = "words")),
                origin = ContentOrigin.DESKTOP,
            )
        )

        service(repository, Result.success(listOf(catalogueSong("1")))).sync()

        assertEquals(listOf("1"), repository.songs.map { it.number })
    }

    @Test
    fun aCancelledSyncDeletesNothing() = runTest {
        // Half a catalogue is not evidence of a deletion.
        val repository = repository()
        repository.upsertSong(
            LocalSong(
                id = "existing",
                number = "99",
                title = "Song 99",
                bookName = "Hymns",
                sections = listOf(LocalSongSection(type = SectionType.VERSE, text = "words")),
                origin = ContentOrigin.DESKTOP,
            )
        )
        // Cancelled from inside the run — sync() clears the flag on entry, so
        // asking beforehand is not a cancellation.
        var syncService: LibrarySyncService? = null
        syncService = service(
            repository,
            Result.success(listOf(catalogueSong("1"), catalogueSong("2"))),
            details = { song ->
                syncService?.requestCancel()
                Result.success(detail(song.number, "words"))
            },
        )

        val outcome = syncService.sync()

        assertIs<SyncOutcome.Cancelled>(outcome)
        assertTrue(repository.songs.any { it.number == "99" })
    }
}
