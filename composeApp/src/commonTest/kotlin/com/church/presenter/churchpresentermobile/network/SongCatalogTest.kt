package com.church.presenter.churchpresentermobile.network

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
    ) : SongReader {
        var detailCalls = 0
        override suspend fun getSongs(): Result<List<Song>> = Result.success(songs)
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
