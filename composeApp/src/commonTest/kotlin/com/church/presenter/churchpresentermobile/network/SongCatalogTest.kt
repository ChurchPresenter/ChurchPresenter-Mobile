package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.calendar.sync.CatalogRecord
import com.church.presenter.churchpresentermobile.calendar.sync.CatalogSong
import com.church.presenter.churchpresentermobile.calendar.sync.SongCatalogStore
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests the source-of-truth decision for song content.
 *
 * The behaviour under test is what standalone mode depends on: with no desktop
 * on the network, asking for songs must succeed from the on-device library
 * rather than fail against a host that isn't there.
 */
class SongCatalogTest {

    /** Fails the test if the desktop is touched — the point of standalone. */
    private class ForbiddenReader : SongReader {
        override suspend fun getSongs(): Result<List<Song>> =
            throw AssertionError("the desktop must not be contacted in standalone")

        override suspend fun getSongDetail(
            number: String,
            bookName: String?,
            songId: Int,
            title: String?,
        ): Result<SongDetail> =
            throw AssertionError("the desktop must not be contacted in standalone")
    }

    private class FakeReader(
        val songs: List<Song> = emptyList(),
        val detail: SongDetail = SongDetail(number = "1", title = "Remote song"),
        val books: Result<List<CatalogRecord>> = Result.success(emptyList()),
    ) : SongReader {
        var detailCalls = 0
        override suspend fun getSongs(): Result<List<Song>> = Result.success(songs)
        override suspend fun getSongCatalog(): Result<List<CatalogRecord>> = books
        override suspend fun getSongDetail(
            number: String,
            bookName: String?,
            songId: Int,
            title: String?,
        ): Result<SongDetail> {
            detailCalls++
            return Result.success(detail)
        }
    }

    private fun localSong(
        id: String = "uuid-1",
        number: String = "42",
        title: String = "Amazing Grace",
    ) = LocalSong(
        id = id,
        number = number,
        title = title,
        copyright = "Public domain",
        sections = listOf(
            LocalSongSection(SectionType.VERSE, "Amazing grace how sweet"),
            LocalSongSection(SectionType.CHORUS, "How sweet the sound"),
            LocalSongSection(SectionType.VERSE, "Twas grace that taught"),
        ),
    )

    private fun libraryWith(vararg songs: LocalSong): LibraryRepository {
        val repository = LibraryRepository(InMemoryFileStorage()) { 0L }
        songs.forEach { repository.upsertSong(it) }
        return repository
    }

    // ── Remote ───────────────────────────────────────────────────────────

    @Test
    fun remoteListDelegatesToTheDesktop() = runTest {
        val remote = FakeReader(songs = listOf(Song(number = "1", title = "From the desktop")))
        val catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), remote, libraryWith(localSong()))

        val songs = catalog.list().getOrThrow()

        assertEquals(listOf("From the desktop"), songs.map { it.title })
        assertFalse(catalog.isLocal, "remote mode must not read the library even when one exists")
    }

    @Test
    fun remoteDetailUsesTheDesktopPayload() = runTest {
        val remote = FakeReader()
        val catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), remote, libraryWith(localSong()))

        val loaded = catalog.detail(Song(number = "1", title = "Remote song")).getOrThrow()

        assertEquals(1, remote.detailCalls)
        assertEquals("Remote song", loaded.detail.title)
    }

    // ── Planning ─────────────────────────────────────────────────────────

    private class UnreachableReader : SongReader {
        override suspend fun getSongs(): Result<List<Song>> = Result.failure(IllegalStateException("no desktop"))
        override suspend fun getSongDetail(
            number: String,
            bookName: String?,
            songId: Int,
            title: String?,
        ): Result<SongDetail> = Result.failure(IllegalStateException("no desktop"))
    }

    @Test
    fun planningUsesTheDesktopWhileItAnswers() = runTest {
        val remote = FakeReader(songs = listOf(Song(number = "1", title = "From the desktop")))
        val catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), remote, libraryWith(localSong()))

        assertEquals(listOf("From the desktop"), catalog.listForPlanning().map { it.title })
    }

    @Test
    fun planningFallsBackToTheLibraryWhenTheDesktopIsOff() = runTest {
        val catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), UnreachableReader(), libraryWith(localSong()))

        val songs = catalog.listForPlanning()

        assertEquals(listOf("Amazing Grace"), songs.map { it.title })
        assertEquals("::42", songs.single().desktopSongId)
    }

    @Test
    fun planningWithNoDesktopAndNoLibraryIsAnEmptyListNotAnError() = runTest {
        val catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), UnreachableReader())

        assertEquals(emptyList(), catalog.listForPlanning())
    }

    // ── Songbooks from the desktop ───────────────────────────────────────

    private val hymnal = CatalogRecord(
        "Hymnal",
        songs = listOf(CatalogSong("42", "Amazing Grace", 270), CatalogSong("7", "Unsung")),
    )

    @Test
    fun aDesktopThatAnswersAlsoHandsOverItsSongbooksAndTheirLengths() = runTest {
        val store = SongCatalogStore(InMemoryFileStorage())
        val remote = FakeReader(
            songs = listOf(Song(number = "42", title = "Amazing Grace", bookName = "Hymnal")),
            books = Result.success(listOf(hymnal)),
        )
        val catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), remote, catalogStore = store)

        val songs = catalog.listForPlanning()

        assertEquals(listOf("Amazing Grace"), songs.map { it.title })
        assertEquals(270, catalog.durations().secondsFor(songs.single()))
        assertEquals(2, store.songs().size)
    }

    @Test
    fun withTheDesktopOffPlanningUsesTheSongbooksItSentLast() = runTest {
        val store = SongCatalogStore(InMemoryFileStorage())
        store.replaceAll(listOf(hymnal))
        val catalog =
            SongCatalog(MutableStateFlow(AppMode.REMOTE), UnreachableReader(), libraryWith(localSong()), store)

        val songs = catalog.listForPlanning()

        val labels = songs.map { "${it.number} - ${it.title}" }.sorted()
        assertEquals(listOf("7 - Unsung", "42 - Amazing Grace").sorted(), labels)
        assertEquals("Hymnal::42", songs.first { it.number == "42" }.desktopSongId)
        assertEquals(270, catalog.durations().secondsFor(songs.first { it.number == "42" }))
        assertEquals(null, catalog.durations().secondsFor(songs.first { it.number == "7" }))
    }

    @Test
    fun nothingRememberedIsNoDurationsNotAnError() = runTest {
        val catalog = SongCatalog(MutableStateFlow(AppMode.STANDALONE), ForbiddenReader(), libraryWith(localSong()))

        assertEquals(null, catalog.durations().secondsFor(Song(number = "42", title = "Amazing Grace")))
    }

    // ── Standalone ───────────────────────────────────────────────────────

    @Test
    fun standaloneListsLibrarySongsAndNeverContactsTheDesktop() = runTest {
        val catalog = SongCatalog(
            MutableStateFlow(AppMode.STANDALONE),
            ForbiddenReader(),
            libraryWith(localSong(), localSong(id = "uuid-2", number = "7", title = "Be Thou My Vision")),
        )

        val songs = catalog.list().getOrThrow()

        assertEquals(setOf("Amazing Grace", "Be Thou My Vision"), songs.map { it.title }.toSet())
        assertEquals(setOf("uuid-1", "uuid-2"), songs.mapNotNull { it.localId }.toSet())
    }

    @Test
    fun anEmptyLibraryIsAnEmptyListNotAFailure() = runTest {
        // The regression this whole change exists for: standalone showed
        // "Failed to load songs" because an absent desktop was reported as an error.
        val catalog = SongCatalog(
            MutableStateFlow(AppMode.STANDALONE),
            ForbiddenReader(),
            libraryWith(),
        )

        val result = catalog.list()

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrThrow())
    }

    @Test
    fun standaloneDetailBuildsTheDeckFromTheLibrarySong() = runTest {
        val song = localSong()
        val catalog = SongCatalog(MutableStateFlow(AppMode.STANDALONE), ForbiddenReader(), libraryWith(song))
        val row = catalog.list().getOrThrow().single()

        val loaded = catalog.detail(row).getOrThrow()

        // fromLocalSong, not fromSong: the typed sections, the section labels and
        // the copyright footer only survive the local builder.
        assertEquals(3, loaded.deck.slides.size)
        assertEquals("uuid-1", loaded.deck.slides.first().sourceId)
        assertEquals("Public domain", loaded.deck.slides.first().footer)
        assertEquals(listOf("Verse 1", "Chorus", "Verse 2"), loaded.detail.allVerses.map { it.label })
    }

    @Test
    fun standaloneDetailFollowsAnEditMadeAfterTheListWasBuilt() = runTest {
        val library = libraryWith(localSong())
        val catalog = SongCatalog(MutableStateFlow(AppMode.STANDALONE), ForbiddenReader(), library)
        val staleRow = catalog.list().getOrThrow().single()

        library.upsertSong(localSong().copy(title = "Amazing Grace (revised)"))

        assertEquals("Amazing Grace (revised)", catalog.detail(staleRow).getOrThrow().detail.title)
    }

    @Test
    fun standaloneDetailFailsForASongThatWasDeleted() = runTest {
        val catalog = SongCatalog(MutableStateFlow(AppMode.STANDALONE), ForbiddenReader(), libraryWith())

        val result = catalog.detail(Song(number = "42", title = "Gone", localId = "uuid-1"))

        assertTrue(result.isFailure)
        assertIs<NoSuchElementException>(result.exceptionOrNull())
    }

    // ── Mode switching ───────────────────────────────────────────────────

    @Test
    fun switchingModeAtRuntimeChangesTheSourceOnTheNextCall() = runTest {
        // The operator can change mode in Settings without the app restarting,
        // so the source must be decided per call rather than captured once.
        val mode = MutableStateFlow(AppMode.REMOTE)
        val remote = FakeReader(songs = listOf(Song(number = "1", title = "From the desktop")))
        val catalog = SongCatalog(mode, remote, libraryWith(localSong()))

        assertEquals(listOf("From the desktop"), catalog.list().getOrThrow().map { it.title })

        mode.value = AppMode.STANDALONE

        assertEquals(listOf("Amazing Grace"), catalog.list().getOrThrow().map { it.title })
    }

    @Test
    fun withoutALibraryStandaloneStillUsesTheDesktop() = runTest {
        // Previews and older call sites construct the catalog with no library;
        // they must keep working rather than silently returning nothing.
        val remote = FakeReader(songs = listOf(Song(number = "1", title = "From the desktop")))
        val catalog = SongCatalog(MutableStateFlow(AppMode.STANDALONE), remote, library = null)

        assertFalse(catalog.isLocal)
        assertEquals(listOf("From the desktop"), catalog.list().getOrThrow().map { it.title })
    }
}
