package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.SongCatalog
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests [SongsViewModel]'s derived search/filter flows using demo-mode data
 * (no network). Assertions are on invariants, not specific demo titles.
 */
class SongsViewModelTest {

    private fun demoVm(): SongsViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return SongsViewModel(settings, ServerEventService(settings), isDemoMode = true)
    }

    @Test
    fun demoLoadPopulatesUnfiltered() = runVmTest {
        val vm = demoVm()
        advanceUntilIdle()
        assertTrue(vm.songs.value.isNotEmpty())
        assertFalse(vm.hasActiveFilter.value)
    }

    @Test
    fun availableBooksAreDistinctAndSorted() = runVmTest {
        val vm = demoVm()
        advanceUntilIdle()
        val books = vm.availableBooks.value
        assertEquals(books.distinct(), books)
        assertEquals(books.sorted(), books)
    }

    @Test
    fun selectingBookFiltersToThatBook() = runVmTest {
        val vm = demoVm()
        advanceUntilIdle()
        val book = vm.availableBooks.value.firstOrNull() ?: return@runVmTest
        vm.setSelectedBook(book)
        advanceUntilIdle()
        val filtered = vm.songs.value
        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { it.bookName == book })
        assertTrue(vm.hasActiveFilter.value)
    }

    @Test
    fun searchByNumberMatchesPrefix() = runVmTest {
        val vm = demoVm()
        advanceUntilIdle()
        val sample = vm.songs.value.first()
        vm.setSearchQuery(sample.number)
        advanceUntilIdle()
        val res = vm.songs.value
        assertTrue(res.any { it.number == sample.number })
        assertTrue(res.all {
            it.number.startsWith(sample.number, ignoreCase = true) ||
                it.title.contains(sample.number, ignoreCase = true)
        })
    }

    @Test
    fun searchByTitleMatchesContains() = runVmTest {
        val vm = demoVm()
        advanceUntilIdle()
        val sample = vm.songs.value.first()
        val fragment = sample.title.take(3)
        vm.setSearchQuery(fragment)
        advanceUntilIdle()
        val res = vm.songs.value
        assertTrue(res.any { it.title == sample.title })
        assertTrue(res.all {
            it.number.startsWith(fragment, ignoreCase = true) ||
                it.title.contains(fragment, ignoreCase = true)
        })
    }

    @Test
    fun noMatchYieldsEmptyAndClearingRestores() = runVmTest {
        val vm = demoVm()
        advanceUntilIdle()
        val total = vm.songs.value.size
        vm.setSearchQuery("zzz-no-such-song")
        advanceUntilIdle()
        assertTrue(vm.songs.value.isEmpty())
        vm.setSearchQuery("")
        vm.setSelectedBook(null)
        advanceUntilIdle()
        assertEquals(total, vm.songs.value.size)
        assertFalse(vm.hasActiveFilter.value)
    }

    // ── Opening a song ───────────────────────────────────────────────────

    @Test
    fun `opening a song loads its lyrics`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val song = vm.songs.value.first()

            vm.openSongDetail(song)
            advanceUntilIdle()

            assertEquals(song, vm.selectedSong.value)
            assertTrue(vm.songDetail.value?.hasLyrics == true)
            assertNull(vm.detailError.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `dismissing a song clears everything it opened`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.openSongDetail(vm.songs.value.first())
            advanceUntilIdle()

            vm.dismissSongDetail()

            assertNull(vm.selectedSong.value)
            assertNull(vm.songDetail.value)
            assertNull(vm.selectedVerseIndex.value)
            assertFalse(vm.isProjecting.value)
            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Opening by title, from the schedule drawer ───────────────────────

    @Test
    fun `a song can be opened by its title`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val song = vm.songs.value.first()

            vm.openSongByTitle(song.title, song.bookName)
            advanceUntilIdle()

            assertEquals(song.number, vm.selectedSong.value?.number)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the title is matched whatever case it arrives in`() = runVmTest {
        // It comes from a schedule row's display text, not from our own list.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val song = vm.songs.value.first()

            vm.openSongByTitle(song.title.uppercase(), null)
            advanceUntilIdle()

            assertEquals(song.number, vm.selectedSong.value?.number)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a title match wins even when the songbook does not`() = runVmTest {
        // Falls back to title-only rather than opening nothing: the schedule row
        // may name a book this catalogue spells differently.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val song = vm.songs.value.first()

            vm.openSongByTitle(song.title, "A Songbook That Does Not Exist")
            advanceUntilIdle()

            assertEquals(song.number, vm.selectedSong.value?.number)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `an unknown title leaves the screen where it was`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()

            vm.openSongByTitle("A Song Nobody Wrote", null)
            advanceUntilIdle()

            assertNull(vm.selectedSong.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Projecting and verse selection ───────────────────────────────────

    @Test
    fun `projecting turns on and off again`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.openSongDetail(vm.songs.value.first())
            advanceUntilIdle()

            vm.toggleProjecting()
            assertTrue(vm.isProjecting.value)

            vm.toggleProjecting()
            assertFalse(vm.isProjecting.value)
            assertNull(vm.selectedVerseIndex.value, "clearing must forget which verse was live")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a verse is only selectable while projecting`() = runVmTest {
        // Tapping a verse on a screen that is not live must not silently arm it;
        // the cast button is what puts a song up.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.openSongDetail(vm.songs.value.first())
            advanceUntilIdle()

            vm.selectVerse(2)

            assertNull(vm.selectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `selecting a verse while projecting records it`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.openSongDetail(vm.songs.value.first())
            advanceUntilIdle()
            vm.toggleProjecting()

            vm.selectVerse(2)
            advanceUntilIdle()

            assertEquals(2, vm.selectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `moving between verses keeps the latest`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.openSongDetail(vm.songs.value.first())
            advanceUntilIdle()
            vm.toggleProjecting()

            vm.selectVerse(0)
            vm.selectVerse(3)
            advanceUntilIdle()

            assertEquals(3, vm.selectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── The live paths ───────────────────────────────────────────────────
    //
    // Everything above runs in demo mode, which short-circuits before any
    // request. These drive the real ones: reading through an injected catalog,
    // the projection actions through an injected service.

    private val catalogueJson = """
        {"song-book":[{"book-name":"Hymns","song-total":1,"songs":[
          {"id":1,"number":"42","title":"Amazing Grace"}
        ]}]}
    """.trimIndent()

    private val detailJson = """
        {"number":"42","title":"Amazing Grace",
         "verses":[{"label":"Verse 1","lines":["Amazing grace"]},
                   {"label":"Chorus","lines":["How sweet the sound"]}]}
    """.trimIndent()

    private fun liveVm(ws: FakeWsSender = FakeWsSender()): SongsViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val reader = SongService(settings, ws, mockClient { path ->
            if (path.contains("/songs/")) respond(detailJson) else respond(catalogueJson)
        })
        return SongsViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            isDemoMode = false,
            sender = ws,
            catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), reader),
            serviceFactory = { SongService(it, ws, mockClient { respond("{}") }) },
        )
    }

    private suspend fun SongsViewModel.openFirstSong() {
        val song = songs.first { it.isNotEmpty() }.first()
        openSongDetail(song)
        songDetail.first { it != null }
    }

    @Test
    fun `the catalogue loads from the desktop`() = runVmTestUnconfined {
        val vm = liveVm()
        try {
            val songs = vm.songs.first { it.isNotEmpty() }

            assertEquals("Amazing Grace", songs.first().title)
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `opening a song fetches its lyrics`() = runVmTestUnconfined {
        val vm = liveVm()
        try {
            vm.openFirstSong()

            assertTrue(vm.songDetail.value?.hasLyrics == true)
            assertFalse(vm.isLoadingDetail.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `projecting sends the song to the desktop`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()

            vm.toggleProjecting()
            vm.isProjecting.first { it }

            // Casting a song goes live through `project`, which the desktop gates
            // behind an approval dialog — unlike `select_song`, which only moves
            // the desktop's own selection.
            assertEquals(WsMessageType.PROJECT, ws.lastType)
            assertTrue(ws.lastPayload.contains("Amazing Grace"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `turning projection off clears the desktop`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()
            vm.toggleProjecting()
            vm.isProjecting.first { it }

            vm.toggleProjecting()
            vm.isProjecting.first { !it }

            assertEquals(WsMessageType.CLEAR, ws.lastType)
            assertNull(vm.selectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `selecting a verse while live sends that section`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()
            vm.toggleProjecting()
            vm.isProjecting.first { it }

            vm.selectVerse(1)
            vm.selectedVerseIndex.first { it == 1 }

            assertEquals(WsMessageType.SELECT_SONG_SECTION, ws.lastType)
            assertTrue(ws.lastPayload.contains("\"section\":1"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the display tells the desktop`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()
            vm.toggleProjecting()
            vm.isProjecting.first { it }

            vm.clearDisplay()
            vm.isProjecting.first { !it }

            assertEquals(WsMessageType.CLEAR, ws.lastType)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding to the schedule sends the song and confirms`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()

            vm.addSongToSchedule()
            vm.scheduleAdded.first { it }

            assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
            assertTrue(ws.lastPayload.contains("Amazing Grace"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding with no song open says so rather than sending nothing`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.songs.first { it.isNotEmpty() }

            vm.addSongToSchedule()

            assertIs<ToastEvent.NoSongSelected>(vm.toastEvent.value)
            assertTrue(ws.calls.isEmpty())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a refused add raises a toast rather than confirming`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()
            ws.failWith(IllegalStateException("denied"))

            vm.addSongToSchedule()
            val toast = vm.toastEvent.first { it != null && it !is ToastEvent.SongAddedToSchedule }

            assertNotNull(toast)
            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a desktop clear resets what this screen thinks is live`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()
            vm.toggleProjecting()
            vm.isProjecting.first { it }

            vm.onDisplayCleared()

            assertFalse(vm.isProjecting.value)
            assertNull(vm.selectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `refreshing asks the desktop again`() = runVmTestUnconfined {
        val asked = MutableStateFlow(0)
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val reader = SongService(settings, ws, mockClient { asked.value += 1; respond(catalogueJson) })
        val vm = SongsViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            isDemoMode = false,
            sender = ws,
            catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), reader),
            serviceFactory = { SongService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            vm.songs.first { it.isNotEmpty() }
            val before = asked.value

            vm.refresh()

            asked.first { it > before }
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a consumed toast is cleared so it shows once`() = runVmTestUnconfined {
        val vm = liveVm()
        try {
            vm.songs.first { it.isNotEmpty() }
            vm.addSongToSchedule()
            vm.toastEvent.first { it != null }

            vm.toastShown()

            assertNull(vm.toastEvent.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── How a refusal is worded ──────────────────────────────────────────
    //
    // The desktop can refuse in several ways, and each needs different words in
    // front of the operator: an operator who pressed Deny, a blocked session, a
    // reason worth quoting, or a bare status with nothing to say. Collapsing
    // these into one "request failed" is what this mapping replaced.

    private suspend fun SongsViewModel.toastAfterFailedAdd(ws: FakeWsSender, error: Throwable): ToastEvent? {
        openFirstSong()
        ws.failWith(error)
        addSongToSchedule()
        return toastEvent.first { it != null && it !is ToastEvent.SongAddedToSchedule }
    }

    @Test
    fun `an operator pressing deny is reported as denied`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            val toast = vm.toastAfterFailedAdd(ws, ApiException(403, "denied"))

            assertIs<ToastEvent.RequestDenied>(toast)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the reason is matched whatever case the desktop sends`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            val toast = vm.toastAfterFailedAdd(ws, ApiException(403, "DENIED"))

            assertIs<ToastEvent.RequestDenied>(toast)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a blocked session is its own message`() = runVmTestUnconfined {
        // Distinct from denied: nothing the operator does on the phone will help
        // until the session is unblocked on the desktop.
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            val toast = vm.toastAfterFailedAdd(ws, ApiException(403, "blocked"))

            assertIs<ToastEvent.SessionBlocked>(toast)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a refusal with no reason shows the status code`() = runVmTestUnconfined {
        // Nothing useful to quote, so the number is all there is to show.
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            val toast = vm.toastAfterFailedAdd(ws, ApiException(503, null))

            assertIs<ToastEvent.RequestRejected>(toast)
            assertEquals(503, toast.httpStatus)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `any other reason is quoted back to the operator`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            val toast = vm.toastAfterFailedAdd(ws, ApiException(503, "No song folder loaded"))

            assertIs<ToastEvent.RequestRejectedWithReason>(toast)
            assertEquals("No song folder loaded", toast.reason)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failure that is not the desktop answering falls back`() = runVmTestUnconfined {
        // A dropped socket is not a refusal; it gets the ordinary failure message
        // with the network reason attached.
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            val toast = vm.toastAfterFailedAdd(ws, IllegalStateException("Connection refused"))

            assertIs<ToastEvent.FailedToAddSchedule>(toast)
        } finally {
            tearDown(vm)
        }
    }

    // ── When an action fails mid-service ─────────────────────────────────

    @Test
    fun `a failed clear still stops this screen showing as live`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()
            vm.toggleProjecting()
            vm.isProjecting.first { it }

            ws.failWith(IllegalStateException("socket closed"))
            vm.clearDisplay()

            assertFalse(vm.isProjecting.value)
            assertNull(vm.selectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed stop still leaves the screen not projecting`() = runVmTestUnconfined {
        // toggleProjecting off fires a clear; the flag flips first so the button
        // never sticks in "stop" when the request errors.
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()
            vm.toggleProjecting()
            vm.isProjecting.first { it }

            ws.failWith(IllegalStateException("socket closed"))
            vm.toggleProjecting()

            assertFalse(vm.isProjecting.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed verse move keeps the operator's position`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstSong()
            vm.toggleProjecting()
            vm.isProjecting.first { it }
            ws.failWith(IllegalStateException("socket closed"))

            vm.selectVerse(1)

            assertEquals(1, vm.selectedVerseIndex.value)
            assertNotNull(vm.error.first { it != null })
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a song whose lyrics will not load reports it on the detail screen`() = runVmTestUnconfined {
        // The list still works; only this song failed, so the error belongs to the
        // sheet rather than to the whole tab.
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val reader = SongService(settings, ws, mockClient { path ->
            if (path.contains("/songs/")) respond("boom", HttpStatusCode.InternalServerError)
            else respond(catalogueJson)
        })
        val vm = SongsViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            isDemoMode = false,
            sender = ws,
            catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), reader),
            serviceFactory = { SongService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            val song = vm.songs.first { it.isNotEmpty() }.first()

            vm.openSongDetail(song)
            val error = vm.detailError.first { it != null }

            assertNotNull(error)
            assertFalse(vm.isLoadingDetail.value)
            assertNull(vm.error.value, "one song failing is not a catalogue failure")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a catalogue that will not load is reported`() = runVmTestUnconfined {
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val reader = SongService(settings, ws, mockClient { respond("boom", HttpStatusCode.InternalServerError) })
        val vm = SongsViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            isDemoMode = false,
            sender = ws,
            catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), reader),
            serviceFactory = { SongService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            val error = vm.error.first { it != null }

            assertNotNull(error)
            assertTrue(vm.songs.value.isEmpty())
            assertFalse(vm.isLoading.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Demo mode ────────────────────────────────────────────────────────
    //
    // The catalogue shown on a phone that has never been pointed at a desktop.
    // Every action has to look like it worked without a request going anywhere:
    // an app-store reviewer taps Project and gets a toast, not a timeout.

    @Test
    fun `projecting in demo mode confirms without a desktop`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.openSongDetail(vm.songs.value.first())
            advanceUntilIdle()

            vm.toggleProjecting()
            advanceUntilIdle()

            assertTrue(vm.isProjecting.value)
            assertIs<ToastEvent.SongLive>(vm.toastEvent.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding to the schedule in demo mode confirms without a desktop`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val song = vm.songs.value.first()
            vm.openSongDetail(song)
            advanceUntilIdle()

            vm.addSongToSchedule()
            advanceUntilIdle()

            assertEquals(ToastEvent.SongAddedToSchedule(song.title), vm.toastEvent.value)
            assertTrue(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a demo add still nudges the schedule tab to refresh`() = runVmTest {
        // The Schedule tab watches this counter; leaving it alone would show an
        // empty running order right after a confirmation that something was added.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.openSongDetail(vm.songs.value.first())
            advanceUntilIdle()
            val before = vm.scheduleRefreshTrigger.value

            vm.addSongToSchedule()
            advanceUntilIdle()

            assertEquals(before + 1, vm.scheduleRefreshTrigger.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the display in demo mode just stops projecting`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            vm.openSongDetail(vm.songs.value.first())
            advanceUntilIdle()
            vm.toggleProjecting()
            advanceUntilIdle()

            vm.clearDisplay()
            advanceUntilIdle()

            assertFalse(vm.isProjecting.value)
            assertNull(vm.selectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Guards ───────────────────────────────────────────────────────────

    @Test
    fun `projecting with no song open stops rather than sending nothing`() = runVmTest {
        // Reachable by tapping Project on a detail sheet that has just been
        // dismissed; without the guard this would send a null song.
        val vm = demoVm()
        try {
            advanceUntilIdle()

            vm.toggleProjecting()
            advanceUntilIdle()

            assertFalse(vm.isProjecting.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a second load keeps the catalogue already on screen`() = runVmTestUnconfined {
        // Switching tabs re-runs the load; throwing the list away and refetching
        // would blank the screen on every return to the Songs tab.
        val vm = liveVm()
        try {
            val loaded = vm.songs.first { it.isNotEmpty() }

            vm.loadSongs()

            assertEquals(loaded, vm.songs.value)
            assertFalse(vm.isLoading.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a forced reload does go back to the desktop`() = runVmTestUnconfined {
        // The refresh gesture has to bypass the cache the tab switch relies on.
        val requests = MutableStateFlow(0)
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val reader = SongService(settings, ws, mockClient { path ->
            if (path.contains("/songs/")) respond(detailJson) else {
                requests.value += 1
                respond(catalogueJson)
            }
        })
        val vm = SongsViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            sender = ws,
            catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), reader),
            serviceFactory = { SongService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            val before = requests.first { it > 0 }

            vm.loadSongs(forceReload = true)

            assertTrue(requests.first { it > before } > before)
        } finally {
            tearDown(vm)
        }
    }

    // ── Settings saves ───────────────────────────────────────────────────

    /** A live ViewModel that counts how many times the catalogue is fetched. */
    private fun countingVm(requests: MutableStateFlow<Int>): SongsViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val reader = SongService(settings, ws, mockClient { path ->
            if (path.contains("/songs/")) respond(detailJson) else {
                requests.value += 1
                respond(catalogueJson)
            }
        })
        return SongsViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            sender = ws,
            catalog = SongCatalog(MutableStateFlow(AppMode.REMOTE), reader),
            serviceFactory = { SongService(it, ws, mockClient { respond("{}") }) },
        )
    }

    @Test
    fun `the same settings-save token is only acted on once`() = runVmTestUnconfined {
        // The Songs tab reloads from a LaunchedEffect keyed on this token, which
        // re-fires on every recomposition after a rotation. Without the guard the
        // catalogue would be refetched each time the screen was rebuilt.
        val requests = MutableStateFlow(0)
        val vm = countingVm(requests)
        try {
            vm.songs.first { it.isNotEmpty() }

            vm.onSettingsSaved(settingsSaveToken = 7)
            val afterFirst = requests.first { it > 1 }
            vm.onSettingsSaved(settingsSaveToken = 7)

            assertEquals(afterFirst, requests.value, "the repeat should have been skipped")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a new settings-save token reloads again`() = runVmTestUnconfined {
        val requests = MutableStateFlow(0)
        val vm = countingVm(requests)
        try {
            vm.songs.first { it.isNotEmpty() }

            vm.onSettingsSaved(settingsSaveToken = 7)
            val afterFirst = requests.first { it > 1 }
            vm.onSettingsSaved(settingsSaveToken = 8)

            assertTrue(requests.first { it > afterFirst } > afterFirst)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a settings save with no token always reloads`() = runVmTestUnconfined {
        // Token 0 is "no token given" — the Settings screen's own save path —
        // and must never be swallowed by the repeat guard.
        val requests = MutableStateFlow(0)
        val vm = countingVm(requests)
        try {
            vm.songs.first { it.isNotEmpty() }
            val before = requests.value

            vm.onSettingsSaved()

            assertTrue(requests.first { it > before } > before)
        } finally {
            tearDown(vm)
        }
    }
}
