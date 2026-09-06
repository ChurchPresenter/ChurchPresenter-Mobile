package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.LibrarySyncState
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import com.church.presenter.churchpresentermobile.testutil.tearDown
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import com.church.presenter.churchpresentermobile.model.SyncOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the songbook picker and the remembered sync state.
 *
 * The sync run itself is covered by `LibrarySyncServiceTest`; what matters here
 * is what the sheet decides *before* starting one — which books get copied, and
 * whether the button should be live at all.
 */
class LibrarySyncViewModelTest {

    private val catalogue = """
        {"song-book":[
          {"book-name":"Hymns","song-total":2,"songs":[
            {"id":1,"number":"1","title":"Amazing Grace"},
            {"id":2,"number":"2","title":"How Great"}
          ]},
          {"book-name":"Chorus","song-total":1,"songs":[
            {"id":3,"number":"10","title":"Shout"}
          ]}
        ]}
    """.trimIndent()

    private class Fixture(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        storedState: String? = null,
    ) {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        val viewModel: LibrarySyncViewModel

        init {
            storedState?.let { settings.librarySyncStateJson = it }
            val songService = SongService(settings, FakeWsSender(), mockClient { respond(body, status) })
            viewModel = LibrarySyncViewModel(repository, settings, songService)
        }
    }

    private fun fixture(
        body: String = "",
        status: HttpStatusCode = HttpStatusCode.OK,
        storedState: String? = null,
    ) = Fixture(body.ifEmpty { catalogue }, status, storedState)

    // ── Loading the songbook list ────────────────────────────────────────

    @Test
    fun `no books are offered until they are asked for`() = runVmTestUnconfined {
        val f = fixture()
        try {
            assertTrue(f.viewModel.books.value.isEmpty())
            assertTrue(f.viewModel.selectedBooks.value.isEmpty())
            assertFalse(f.viewModel.isLoadingBooks.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the songbooks are read off the catalogue, deduplicated and sorted`() = runVmTestUnconfined {
        // No second endpoint — the catalogue already names each song's book.
        val f = fixture()
        try {
            f.viewModel.loadBooks()
            val books = f.viewModel.books.first { it.isNotEmpty() }

            assertEquals(listOf("Chorus", "Hymns"), books)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `every book arrives ticked, so ignoring the picker copies everything`() = runVmTestUnconfined {
        val f = fixture()
        try {
            f.viewModel.loadBooks()
            f.viewModel.books.first { it.isNotEmpty() }

            assertEquals(setOf("Chorus", "Hymns"), f.viewModel.selectedBooks.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a song with no book name is not offered as one`() = runVmTestUnconfined {
        val body = """
            {"song-book":[
              {"book-name":"","song-total":1,"songs":[{"id":1,"number":"1","title":"Loose"}]},
              {"book-name":"Hymns","song-total":1,"songs":[{"id":2,"number":"2","title":"Grace"}]}
            ]}
        """.trimIndent()
        val f = fixture(body = body)
        try {
            f.viewModel.loadBooks()
            val books = f.viewModel.books.first { it.isNotEmpty() }

            assertEquals(listOf("Hymns"), books)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a failed catalogue request leaves the list empty rather than half-built`() = runVmTestUnconfined {
        val f = fixture(body = "nope", status = HttpStatusCode.ServiceUnavailable)
        try {
            f.viewModel.loadBooks()
            f.viewModel.isLoadingBooks.first { !it }

            assertTrue(f.viewModel.books.value.isEmpty())
            assertTrue(f.viewModel.selectedBooks.value.isEmpty())
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the loading flag is lowered even when the request fails`() = runVmTestUnconfined {
        // Otherwise the guard in loadBooks would refuse every later attempt.
        val f = fixture(body = "nope", status = HttpStatusCode.ServiceUnavailable)
        try {
            f.viewModel.loadBooks()

            assertFalse(f.viewModel.isLoadingBooks.first { !it })
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Ticking books ────────────────────────────────────────────────────

    @Test
    fun `toggling a book unticks it, and toggling again puts it back`() = runVmTestUnconfined {
        val f = fixture()
        try {
            f.viewModel.loadBooks()
            f.viewModel.books.first { it.isNotEmpty() }

            f.viewModel.toggleBook("Hymns")
            assertEquals(setOf("Chorus"), f.viewModel.selectedBooks.value)

            f.viewModel.toggleBook("Hymns")
            assertEquals(setOf("Chorus", "Hymns"), f.viewModel.selectedBooks.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `select all and clear move the whole list`() = runVmTestUnconfined {
        val f = fixture()
        try {
            f.viewModel.loadBooks()
            f.viewModel.books.first { it.isNotEmpty() }

            f.viewModel.clearBooks()
            assertTrue(f.viewModel.selectedBooks.value.isEmpty())

            f.viewModel.selectAllBooks()
            assertEquals(setOf("Chorus", "Hymns"), f.viewModel.selectedBooks.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── canSync ──────────────────────────────────────────────────────────

    @Test
    fun `copying everything can always sync`() = runVmTestUnconfined {
        val f = fixture()
        try {
            assertFalse(f.viewModel.chooseBooks.value)
            assertTrue(f.viewModel.canSync)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `picking books with none ticked is not a sync`() = runVmTestUnconfined {
        val f = fixture()
        try {
            f.viewModel.setChooseBooks(true)
            f.viewModel.books.first { it.isNotEmpty() }
            f.viewModel.clearBooks()

            assertFalse(f.viewModel.canSync, "every book unticked should disable Sync")
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `switching back to all songbooks ignores a leftover empty selection`() = runVmTestUnconfined {
        // The bug this flag exists for: with the choice living in the sheet,
        // switching back to "all" kept the last ticked set and quietly copied it.
        val f = fixture()
        try {
            f.viewModel.setChooseBooks(true)
            f.viewModel.books.first { it.isNotEmpty() }
            f.viewModel.clearBooks()
            assertFalse(f.viewModel.canSync)

            f.viewModel.setChooseBooks(false)

            assertTrue(f.viewModel.canSync, "'all songbooks' must not be blocked by a stale selection")
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `asking to pick books fetches the list on first ask only`() = runVmTestUnconfined {
        val f = fixture()
        try {
            f.viewModel.setChooseBooks(true)
            val first = f.viewModel.books.first { it.isNotEmpty() }

            // A second ask must not refetch and re-tick what the user just changed.
            f.viewModel.toggleBook("Hymns")
            f.viewModel.setChooseBooks(false)
            f.viewModel.setChooseBooks(true)

            assertEquals(first, f.viewModel.books.value)
            assertEquals(setOf("Chorus"), f.viewModel.selectedBooks.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Remembered state ─────────────────────────────────────────────────

    @Test
    fun `with nothing stored, the library has never synced`() = runVmTestUnconfined {
        val f = fixture()
        try {
            assertEquals(LibrarySyncState.NEVER, f.viewModel.state.value)
            assertFalse(f.viewModel.state.value.hasEverSynced)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a stored sync state is read back`() = runVmTestUnconfined {
        val stored = """{"lastSyncEpochMs":1700000000000,"sourceHost":"office-mac","songCount":240,
            "failedCount":2,"keptLocalCount":5}""".trimIndent()
        val f = fixture(storedState = stored)
        try {
            val state = f.viewModel.state.value

            assertTrue(state.hasEverSynced)
            assertTrue(state.wasPartial)
            assertEquals("office-mac", state.sourceHost)
            assertEquals(240, state.songCount)
            assertEquals(5, state.keptLocalCount)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `an unreadable stored state falls back to never synced instead of crashing`() = runVmTestUnconfined {
        // A truncated write must not take the Library screen down with it.
        val f = fixture(storedState = "{not json")
        try {
            assertEquals(LibrarySyncState.NEVER, f.viewModel.state.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Outcome banner ───────────────────────────────────────────────────

    @Test
    fun `no outcome is shown before a sync has run`() = runVmTestUnconfined {
        val f = fixture()
        try {
            assertNull(f.viewModel.outcome.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `dismissing the outcome clears the banner`() = runVmTestUnconfined {
        val f = fixture()
        try {
            f.viewModel.dismissOutcome()

            assertNull(f.viewModel.outcome.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `cancelling when nothing is running is harmless`() = runVmTestUnconfined {
        val f = fixture()
        try {
            f.viewModel.cancel()

            assertNull(f.viewModel.outcome.value)
            assertFalse(f.viewModel.progress.value.isRunning)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── A sync from end to end ───────────────────────────────────────────
    //
    // Drives the real LibrarySyncService through a mocked catalogue + detail
    // endpoint, so the outcome banner and the persisted state are exercised
    // together — the state is what the Library screen reads on a Sunday morning
    // to answer "is this current?" without touching the network.

    /**
     * A ViewModel whose desktop answers both halves of a sync: the catalogue on
     * `/songs`, and real lyrics on `/songs/{number}`. The shared [fixture] above
     * returns one body for every path, which is fine for the book picker but
     * gives the sync nothing to write.
     */
    private class SyncFixture(private val detailFails: Boolean = false) {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        val viewModel: LibrarySyncViewModel

        init {
            val client = mockClient { path ->
                when {
                    detailFails && path.contains("/songs/") -> respond("no", HttpStatusCode.ServiceUnavailable)
                    path.contains("/songs/") -> respond(
                        """{"number":"1","title":"Amazing Grace",
                           "verses":[{"label":"Verse 1","lines":["Amazing grace"]}]}""".trimIndent(),
                    )
                    else -> respond(CATALOGUE)
                }
            }
            viewModel = LibrarySyncViewModel(repository, settings, SongService(settings, FakeWsSender(), client))
        }

        companion object {
            val CATALOGUE = """
                {"song-book":[{"book-name":"Hymns","song-total":1,"songs":[
                  {"id":1,"number":"1","title":"Amazing Grace"}
                ]}]}
            """.trimIndent()
        }
    }

    @Test
    fun `a successful sync writes the songs and remembers that it happened`() = runVmTestUnconfined {
        val f = SyncFixture()
        try {
            f.viewModel.sync()
            val outcome = f.viewModel.outcome.first { it != null }

            assertIs<SyncOutcome.Success>(outcome)
            assertTrue(f.repository.songs.isNotEmpty(), "the catalogue should have been written in")
            assertTrue(f.viewModel.state.value.hasEverSynced)
            assertEquals(f.repository.songs.size, f.viewModel.state.value.songCount)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the remembered state names the desktop it came from`() = runVmTestUnconfined {
        // A different host means the staleness figure is about someone else's
        // catalogue, so the screen has to be able to say which.
        val f = fixture()
        try {
            f.viewModel.sync()
            f.viewModel.outcome.first { it != null }

            assertEquals(f.settings.host, f.viewModel.state.value.sourceHost)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the remembered state survives being read back by a new view model`() = runVmTestUnconfined {
        // It is persisted as JSON in settings; the screen builds its own ViewModel.
        val f = fixture()
        try {
            f.viewModel.sync()
            f.viewModel.outcome.first { it != null }
            val written = f.viewModel.state.value

            val reopened = LibrarySyncViewModel(
                f.repository,
                f.settings,
                SongService(f.settings, FakeWsSender(), mockClient { respond(SyncFixture.CATALOGUE) }),
            )
            try {
                assertEquals(written, reopened.state.value)
            } finally {
                tearDown(reopened)
            }
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a second sync while one is running is ignored`() = runVmTestUnconfined {
        // The button stays tappable while the sheet is open; a second press must
        // not start a competing run that writes the same songs twice.
        val f = fixture()
        try {
            f.viewModel.sync()
            f.viewModel.sync()
            f.viewModel.outcome.first { it != null }

            val ids = f.repository.songs.map { it.id }
            assertEquals(ids.size, ids.toSet().size, "a song was written twice")
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the outcome banner can be dismissed after a sync`() = runVmTestUnconfined {
        val f = SyncFixture()
        try {
            f.viewModel.sync()
            f.viewModel.outcome.first { it != null }

            f.viewModel.dismissOutcome()

            assertNull(f.viewModel.outcome.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a sync that cannot reach the desktop reports failure and writes nothing`() = runVmTestUnconfined {
        val f = Fixture(body = "nope", status = HttpStatusCode.ServiceUnavailable)
        try {
            f.viewModel.sync()
            val outcome = f.viewModel.outcome.first { it != null }

            assertIs<SyncOutcome.Failed>(outcome)
            assertTrue(f.repository.songs.isEmpty())
            assertFalse(f.viewModel.state.value.hasEverSynced, "a failed sync is not a sync")
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `progress returns to idle once the run finishes`() = runVmTestUnconfined {
        val f = SyncFixture()
        try {
            f.viewModel.sync()
            f.viewModel.outcome.first { it != null }

            assertFalse(f.viewModel.progress.value.isRunning)
            assertFalse(f.viewModel.progress.value.isPreparing)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Which books a sync actually copies ───────────────────────────────
    //
    // `null` means the whole catalogue. The selection is only honoured when the
    // operator opened the picker *and* the list arrived — the two conditions that
    // together stop a stale or empty tick-list silently narrowing a sync.

    /** Records the book filter each sync was started with. */
    private class RecordingSync {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        val viewModel: LibrarySyncViewModel

        init {
            val client = mockClient { path ->
                if (path.contains("/songs/")) respond(
                    """{"number":"1","title":"Amazing Grace","verses":[{"lines":["Amazing grace"]}]}""",
                )
                else respond(SyncFixture.CATALOGUE)
            }
            viewModel = LibrarySyncViewModel(repository, settings, SongService(settings, FakeWsSender(), client))
        }
    }

    @Test
    fun `by default a sync copies the whole catalogue`() = runVmTestUnconfined {
        val f = RecordingSync()
        try {
            assertFalse(f.viewModel.chooseBooks.value)

            f.viewModel.sync()
            f.viewModel.outcome.first { it != null }

            assertTrue(f.repository.songs.isNotEmpty())
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `picking books with a loaded list narrows the sync to them`() = runVmTestUnconfined {
        val f = RecordingSync()
        try {
            f.viewModel.setChooseBooks(true)
            f.viewModel.books.first { it.isNotEmpty() }

            f.viewModel.sync()
            val outcome = f.viewModel.outcome.first { it != null }

            assertIs<SyncOutcome.Success>(outcome)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `picking books before the list arrives still copies everything`() = runVmTestUnconfined {
        // With no book list there is nothing to narrow by, so the selection is
        // ignored rather than producing an empty sync.
        val f = RecordingSync()
        try {
            f.viewModel.setChooseBooks(true)
            // Deliberately not awaiting the book list.

            f.viewModel.sync()
            val outcome = f.viewModel.outcome.first { it != null }

            assertIs<SyncOutcome.Success>(outcome)
            assertTrue(f.repository.songs.isNotEmpty())
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `switching back to all songbooks copies everything again`() = runVmTestUnconfined {
        // The stale-selection bug the chooseBooks flag exists to prevent.
        val f = RecordingSync()
        try {
            f.viewModel.setChooseBooks(true)
            f.viewModel.books.first { it.isNotEmpty() }
            f.viewModel.clearBooks()
            f.viewModel.setChooseBooks(false)

            f.viewModel.sync()
            f.viewModel.outcome.first { it != null }

            assertTrue(f.repository.songs.isNotEmpty(), "an empty tick-list must not narrow an 'all books' sync")
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a new sync clears the previous outcome before starting`() = runVmTestUnconfined {
        // Otherwise the banner from the last run is still on screen while the next
        // one is in flight, describing a sync that already finished.
        val f = RecordingSync()
        try {
            f.viewModel.sync()
            f.viewModel.outcome.first { it != null }

            f.viewModel.sync()
            f.viewModel.outcome.first { it != null }

            assertIs<SyncOutcome.Success>(f.viewModel.outcome.value)
        } finally {
            tearDown(f.viewModel)
        }
    }
}
