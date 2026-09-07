package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.network.BibleCatalog
import com.church.presenter.churchpresentermobile.network.BibleReader
import com.church.presenter.churchpresentermobile.network.BibleService
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests [verseRangeString] plus BibleViewModel multi-select and book-search (demo mode). */
class BibleViewModelTest {

    private fun demoVm(): BibleViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return BibleViewModel(settings, ServerEventService(settings), isDemoMode = true)
    }

    // ── verseRangeString (pure) ──────────────────────────────────────────────

    @Test
    fun verseRangeStringHandlesAllCases() {
        assertNull(verseRangeString(emptyList()))
        assertNull(verseRangeString(listOf(5)))
        assertEquals("3-5", verseRangeString(listOf(3, 4, 5)))
        assertEquals("1-2", verseRangeString(listOf(1, 2)))
        assertEquals("3,5,9", verseRangeString(listOf(3, 5, 9)))
        assertEquals("7,9", verseRangeString(listOf(7, 9)))
    }

    // ── Multi-select machine ─────────────────────────────────────────────────

    @Test
    fun singleSelectReplacesAndDeselects() = runVmTest {
        val vm = demoVm()
        try {
            vm.toggleVerseSelection(2)
            assertEquals(setOf(2), vm.selectedVerseIndices.value)
            vm.toggleVerseSelection(2) // tapping the only selected verse clears it
            assertEquals(emptySet(), vm.selectedVerseIndices.value)
            vm.toggleVerseSelection(2)
            vm.toggleVerseSelection(5) // single-select replaces
            assertEquals(setOf(5), vm.selectedVerseIndices.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun multiSelectAccumulatesAndTogglesOut() = runVmTest {
        val vm = demoVm()
        try {
            vm.toggleMultiSelectMode()
            assertTrue(vm.isMultiSelectMode.value)
            vm.toggleVerseSelection(2)
            vm.toggleVerseSelection(5)
            assertEquals(setOf(2, 5), vm.selectedVerseIndices.value)
            vm.toggleVerseSelection(2)
            assertEquals(setOf(5), vm.selectedVerseIndices.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun leavingMultiSelectClearsSelection() = runVmTest {
        val vm = demoVm()
        try {
            vm.toggleMultiSelectMode()
            vm.toggleVerseSelection(1)
            vm.toggleMultiSelectMode()
            assertFalse(vm.isMultiSelectMode.value)
            assertEquals(emptySet(), vm.selectedVerseIndices.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Book search filter (demo data) ───────────────────────────────────────

    @Test
    fun bookSearchFiltersByDisplayName() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val all = vm.books.value
            assertTrue(all.isNotEmpty())
            val name = all.first().displayName
            val fragment = name.take(3)
            vm.setBookSearchQuery(fragment)
            advanceUntilIdle()
            assertTrue(vm.books.value.all { it.displayName.contains(fragment, ignoreCase = true) })
            assertTrue(vm.books.value.any { it.displayName == name })
            vm.setBookSearchQuery("")
            advanceUntilIdle()
            assertEquals(all.size, vm.books.value.size)
        } finally {
            tearDown(vm)
        }
    }

    // ── Chapter selection (demo mode) ────────────────────────────────────

    private fun bookAndChapterVm(chapter: Int = 1): BibleViewModel {
        val vm = demoVm()
        vm.selectBook(DemoData.books.first())
        vm.selectChapter(chapter)
        return vm
    }

    @Test
    fun selectingAChapterLoadsItsVerses() = runVmTest {
        val vm = demoVm()
        try {
            val book = DemoData.books.first()

            vm.selectBook(book)
            vm.selectChapter(1)
            advanceUntilIdle()

            assertEquals(1, vm.selectedChapter.value)
            assertEquals(DemoData.getVerses(book.displayName, 1), vm.verses.value)
            assertTrue(vm.verses.value.isNotEmpty())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun selectingAChapterWithNoBookChosenDoesNothing() = runVmTest {
        val vm = demoVm()
        try {
            vm.selectChapter(3)
            advanceUntilIdle()

            assertNull(vm.selectedChapter.value)
            assertTrue(vm.verses.value.isEmpty())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun movingToAnotherChapterClearsEverythingFromTheLastOne() = runVmTest {
        // Verse indices belong to the chapter they were picked in; carrying a
        // selection across would project the wrong verses under a new heading.
        val vm = demoVm()
        try {
            vm.selectBook(DemoData.books.first())
            vm.selectChapter(1)
            advanceUntilIdle()
            vm.toggleMultiSelectMode()
            vm.toggleVerseSelection(0)
            vm.toggleVerseSelection(1)

            vm.selectChapter(2)
            advanceUntilIdle()

            assertEquals(emptySet(), vm.selectedVerseIndices.value)
            assertFalse(vm.isMultiSelectMode.value)
            assertFalse(vm.isProjecting.value)
            assertNull(vm.projectedVerseIndex.value)
            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Projecting (demo mode) ───────────────────────────────────────────

    @Test
    fun projectingWithNoChapterOpenDoesNothing() = runVmTest {
        val vm = demoVm()
        try {
            vm.toggleProjecting()
            advanceUntilIdle()

            assertFalse(vm.isProjecting.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun projectingTurnsOnAndOffAgain() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            vm.toggleVerseSelection(0)

            vm.toggleProjecting()
            advanceUntilIdle()
            assertTrue(vm.isProjecting.value)

            vm.toggleProjecting()
            advanceUntilIdle()
            assertFalse(vm.isProjecting.value)
            assertNull(vm.projectedVerseIndex.value, "clearing must forget which verse was live")
        } finally {
            tearDown(vm)
        }
    }

    // ── Settings changes ─────────────────────────────────────────────────

    @Test
    fun savingSettingsClearsTheOpenBookAndChapter() = runVmTest {
        // A different server has a different catalogue; keeping the open chapter
        // would leave the screen showing something the new desktop may not have.
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()

            vm.onSettingsSaved(settingsSaveToken = 1)
            advanceUntilIdle()

            assertNull(vm.selectedBook.value)
            assertNull(vm.selectedChapter.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun theSameSaveTokenIsOnlyActedOnOnce() = runVmTest {
        // The token guards against a recomposition replaying the same save.
        val vm = demoVm()
        try {
            vm.onSettingsSaved(settingsSaveToken = 7)
            advanceUntilIdle()
            vm.selectBook(DemoData.books.first())
            vm.selectChapter(1)
            advanceUntilIdle()

            vm.onSettingsSaved(settingsSaveToken = 7)
            advanceUntilIdle()

            assertEquals(1, vm.selectedChapter.value, "a repeated token must not reset the screen")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun anUntokenedSaveIsAlwaysActedOn() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()

            vm.onSettingsSaved()
            advanceUntilIdle()

            assertNull(vm.selectedChapter.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Navigating back ──────────────────────────────────────────────────

    @Test
    fun `back from a chapter returns to the chapter list and drops its state`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            vm.toggleMultiSelectMode()
            vm.toggleVerseSelection(0)

            vm.navigateBack()

            assertNull(vm.selectedChapter.value)
            assertTrue(vm.verses.value.isEmpty())
            assertEquals(emptySet(), vm.selectedVerseIndices.value)
            assertFalse(vm.isMultiSelectMode.value)
            assertFalse(vm.isProjecting.value)
            assertFalse(vm.scheduleAdded.value)
            // The book stays open — one step back, not two.
            assertNotNull(vm.selectedBook.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `back again from a book returns to the book list`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()

            vm.navigateBack()
            vm.navigateBack()

            assertNull(vm.selectedChapter.value)
            assertNull(vm.selectedBook.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `back at the book list does nothing`() = runVmTest {
        val vm = demoVm()
        try {
            vm.navigateBack()

            assertNull(vm.selectedBook.value)
            assertNull(vm.selectedChapter.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Navigating in from the schedule drawer ───────────────────────────

    @Test
    fun `navigating to a book and chapter opens it`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val book = DemoData.books.first()

            vm.navigateToBookAndChapter(book.displayName, 1)
            advanceUntilIdle()

            assertEquals(book.displayName, vm.selectedBook.value?.displayName)
            assertEquals(1, vm.selectedChapter.value)
            assertTrue(vm.verses.value.isNotEmpty())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the book name is matched whatever case it arrives in`() = runVmTest {
        // It comes from a schedule row's display text, not from our own list.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val book = DemoData.books.first()

            vm.navigateToBookAndChapter(book.displayName.uppercase(), 1)
            advanceUntilIdle()

            assertEquals(book.displayName, vm.selectedBook.value?.displayName)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `requested verses arrive already selected`() = runVmTest {
        // Tapping "John 3:16-17" in the schedule should land on those verses,
        // not just on the chapter containing them.
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val book = DemoData.books.first()
            val wanted = DemoData.getVerses(book.displayName, 1).take(2).map { it.number }.toSet()

            vm.navigateToBookAndChapter(book.displayName, 1, wanted)
            advanceUntilIdle()

            assertEquals(setOf(0, 1), vm.selectedVerseIndices.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `an unknown book leaves the screen where it was`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()

            vm.navigateToBookAndChapter("Book Of Nowhere", 1)
            advanceUntilIdle()

            assertNull(vm.selectedBook.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Adding to the schedule (demo mode) ───────────────────────────────

    @Test
    fun `adding with no verses selected asks for one rather than sending nothing`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()

            vm.addToSchedule()

            assertFalse(vm.scheduleAdded.value)
            assertIs<ToastEvent.FailedToAddBibleSchedule>(vm.toastEvent.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding one verse names it as a single reference`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            val book = DemoData.books.first()
            val first = DemoData.getVerses(book.displayName, 1).first().number
            vm.toggleVerseSelection(0)

            vm.addToSchedule()

            assertTrue(vm.scheduleAdded.value)
            val toast = vm.toastEvent.value
            assertIs<ToastEvent.BibleAddedToSchedule>(toast)
            assertEquals("${book.displayName} 1:$first", toast.reference)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding a run of verses names it as a range`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            vm.toggleMultiSelectMode()
            vm.toggleVerseSelection(0)
            vm.toggleVerseSelection(1)
            vm.toggleVerseSelection(2)

            vm.addToSchedule()

            val toast = vm.toastEvent.value
            assertIs<ToastEvent.BibleAddedToSchedule>(toast)
            assertTrue(toast.reference.contains("-"), "expected a range, got ${toast.reference}")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding asks the schedule drawer to reload`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            val before = vm.scheduleRefreshTrigger.value
            vm.toggleVerseSelection(0)

            vm.addToSchedule()

            assertEquals(before + 1, vm.scheduleRefreshTrigger.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding with no chapter open does nothing at all`() = runVmTest {
        val vm = demoVm()
        try {
            vm.addToSchedule()

            assertFalse(vm.scheduleAdded.value)
            assertNull(vm.toastEvent.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── The desktop clearing its screen ──────────────────────────────────

    @Test
    fun `a desktop clear resets what this screen thinks is live`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            advanceUntilIdle()

            vm.onDisplayCleared()

            assertFalse(vm.isProjecting.value)
            assertNull(vm.projectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Hold ─────────────────────────────────────────────────────────────

    @Test
    fun `hold toggles on and off`() = runVmTest {
        // Held, the desktop freezes on the current verse so the operator can read
        // ahead without the congregation following along.
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            assertFalse(vm.isHolding.value)

            vm.toggleHold()
            assertTrue(vm.isHolding.value)

            vm.toggleHold()
            assertFalse(vm.isHolding.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Clearing the display ─────────────────────────────────────────────

    @Test
    fun `clearing the display stops projecting and releases the hold`() = runVmTest {
        // A held display that survived a clear would freeze the next verse too.
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            vm.toggleHold()
            advanceUntilIdle()

            vm.clearDisplay()
            advanceUntilIdle()

            assertFalse(vm.isProjecting.value)
            assertNull(vm.projectedVerseIndex.value)
            assertFalse(vm.isHolding.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing when nothing is live is harmless`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()

            vm.clearDisplay()
            advanceUntilIdle()

            assertFalse(vm.isProjecting.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Moving between verses while live ─────────────────────────────────

    @Test
    fun `a verse is only selectable while projecting`() = runVmTest {
        // Tapping a verse on a screen that is not live must not put it up.
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()

            vm.selectVerse(1)
            advanceUntilIdle()

            assertNull(vm.projectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `selecting a verse while live projects it and adds it to the selection`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            advanceUntilIdle()

            vm.selectVerse(2)
            advanceUntilIdle()

            assertEquals(2, vm.projectedVerseIndex.value)
            assertTrue(2 in vm.selectedVerseIndices.value, "the tapped verse should join the selection")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `an out-of-range verse index is ignored rather than projecting nothing`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            advanceUntilIdle()
            val before = vm.projectedVerseIndex.value

            vm.selectVerse(999)
            advanceUntilIdle()

            assertEquals(before, vm.projectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `moving between verses keeps the latest live`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            advanceUntilIdle()

            vm.selectVerse(1)
            vm.selectVerse(2)
            advanceUntilIdle()

            assertEquals(2, vm.projectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── The live paths ───────────────────────────────────────────────────
    //
    // Everything above runs in demo mode, which short-circuits before any
    // request. These drive the real ones: reading comes from an injected
    // catalog, the projection actions from an injected service.

    private fun liveVm(ws: FakeWsSender = FakeWsSender()): BibleViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val reader = object : BibleReader {
            override suspend fun getBooks(): Result<List<BibleBook>> =
                Result.success(listOf(BibleBook(name = "Genesis", chapterTotal = 50, bookId = 1)))

            override suspend fun getChapter(bookNumber: Int, chapter: Int): Result<List<BibleVerse>> =
                Result.success(
                    listOf(
                        BibleVerse(verse = 1, text = "In the beginning"),
                        BibleVerse(verse = 2, text = "And the earth was without form"),
                        BibleVerse(verse = 3, text = "And God said, Let there be light"),
                    ),
                )
        }
        val mode = MutableStateFlow(AppMode.REMOTE)
        return BibleViewModel(
            appSettings = settings,
            eventService = ws,
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, reader, LocalBibleRepository(InMemoryFileStorage()) { 1L }),
            serviceFactory = { BibleService(it, ws, mockClient { respond("{}") }) },
        )
    }

    private suspend fun BibleViewModel.openFirstChapter() {
        val book = books.first { it.isNotEmpty() }.first()
        selectBook(book)
        selectChapter(1)
        verses.first { it.isNotEmpty() }
    }

    @Test
    fun `the book list loads from the desktop`() = runVmTestUnconfined {
        val vm = liveVm()
        try {
            val books = vm.books.first { it.isNotEmpty() }

            assertEquals(listOf("Genesis"), books.map { it.displayName })
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `opening a chapter loads its verses`() = runVmTestUnconfined {
        val vm = liveVm()
        try {
            vm.openFirstChapter()

            assertEquals(3, vm.verses.value.size)
            assertEquals(1, vm.selectedChapter.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `projecting sends the selected verse to the desktop`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)

            vm.toggleProjecting()
            vm.isProjecting.first { it }

            assertEquals(WsMessageType.SELECT_BIBLE_VERSE, ws.lastType)
            assertTrue(ws.lastPayload.contains("Genesis"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `moving to another verse while live sends that one`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            vm.isProjecting.first { it }

            vm.selectVerse(2)
            vm.projectedVerseIndex.first { it == 2 }

            assertTrue(ws.lastPayload.contains("Let there be light"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `turning projection off clears the desktop`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            vm.isProjecting.first { it }

            vm.toggleProjecting()
            vm.isProjecting.first { !it }

            assertEquals(WsMessageType.CLEAR, ws.lastType)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `hold is sent to the desktop with the flag`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()

            vm.toggleHold()
            vm.isHolding.first { it }

            assertEquals(WsMessageType.BIBLE_HOLD, ws.lastType)
            assertTrue(ws.lastPayload.contains("true"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the display tells the desktop and releases the hold`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            vm.toggleHold()
            vm.isHolding.first { it }

            vm.clearDisplay()
            vm.isProjecting.first { !it }

            assertEquals(WsMessageType.CLEAR, ws.lastType)
            assertFalse(vm.isHolding.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding a range to the schedule sends it and confirms`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleMultiSelectMode()
            vm.toggleVerseSelection(0)
            vm.toggleVerseSelection(1)

            vm.addToSchedule()
            vm.scheduleAdded.first { it }

            assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
            assertTrue(ws.lastPayload.contains("\"verseRange\":\"1-2\""), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a refused add is reported rather than confirmed`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("denied"))
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)

            vm.addToSchedule()
            val toast = vm.toastEvent.first { it is ToastEvent.FailedToAddBibleSchedule }

            assertIs<ToastEvent.FailedToAddBibleSchedule>(toast)
            assertFalse(vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `saving settings rebuilds the service and clears the open chapter`() = runVmTestUnconfined {
        var built = 0
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val mode = MutableStateFlow(AppMode.REMOTE)
        val reader = object : BibleReader {
            override suspend fun getBooks(): Result<List<BibleBook>> =
                Result.success(listOf(BibleBook(name = "Genesis", chapterTotal = 50, bookId = 1)))
            override suspend fun getChapter(bookNumber: Int, chapter: Int): Result<List<BibleVerse>> =
                Result.success(listOf(BibleVerse(verse = 1, text = "In the beginning")))
        }
        val vm = BibleViewModel(
            appSettings = settings,
            eventService = ws,
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, reader, LocalBibleRepository(InMemoryFileStorage()) { 1L }),
            serviceFactory = { built++; BibleService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            vm.openFirstChapter()
            assertEquals(1, built)

            vm.onSettingsSaved(settingsSaveToken = 1)

            assertEquals(2, built, "a new server needs a new client")
            assertNull(vm.selectedChapter.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Branches the happy path does not reach ───────────────────────────

    @Test
    fun `back is offered only once a book is open`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            assertFalse(vm.canNavigateBack, "the book list is the top of this tab")

            vm.selectBook(DemoData.books.first())

            assertTrue(vm.canNavigateBack)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `back stops being offered once the book is closed`() = runVmTest {
        val vm = bookAndChapterVm()
        try {
            advanceUntilIdle()

            vm.navigateBack()
            vm.navigateBack()

            assertFalse(vm.canNavigateBack)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a queued navigation waits for the book list and then runs`() = runVmTestUnconfined {
        // The schedule drawer can ask before the books have arrived; the request
        // is held rather than dropped, which is what makes tapping a bible row
        // from a cold start land on the passage.
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val gate = MutableStateFlow(false)
        val reader = object : BibleReader {
            override suspend fun getBooks(): Result<List<BibleBook>> {
                gate.first { it }
                return Result.success(listOf(BibleBook(name = "Genesis", chapterTotal = 50, bookId = 1)))
            }
            override suspend fun getChapter(bookNumber: Int, chapter: Int) =
                Result.success(listOf(BibleVerse(verse = 1, text = "In the beginning")))
        }
        val mode = MutableStateFlow(AppMode.REMOTE)
        val vm = BibleViewModel(
            appSettings = settings,
            eventService = ws,
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, reader, LocalBibleRepository(InMemoryFileStorage()) { 1L }),
            serviceFactory = { BibleService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            // Asked while the book list is still in flight.
            vm.navigateToBookAndChapter("Genesis", 1)
            assertNull(vm.selectedBook.value, "nothing to match against yet")

            gate.value = true
            vm.verses.first { it.isNotEmpty() }

            assertEquals("Genesis", vm.selectedBook.value?.displayName)
            assertEquals(1, vm.selectedChapter.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a book is matched on its raw name when the display name does not fit`() = runVmTestUnconfined {
        // Schedule rows carry the desktop's own spelling, which need not match the
        // display name this app builds.
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val reader = object : BibleReader {
            override suspend fun getBooks() =
                Result.success(listOf(BibleBook(name = "Song of Solomon", bookName = "Canticles", bookId = 22)))
            override suspend fun getChapter(bookNumber: Int, chapter: Int) =
                Result.success(listOf(BibleVerse(verse = 1, text = "The song of songs")))
        }
        val mode = MutableStateFlow(AppMode.REMOTE)
        val vm = BibleViewModel(
            appSettings = settings,
            eventService = ws,
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, reader, LocalBibleRepository(InMemoryFileStorage()) { 1L }),
            serviceFactory = { BibleService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            vm.books.first { it.isNotEmpty() }

            vm.navigateToBookAndChapter("Canticles", 1)
            vm.verses.first { it.isNotEmpty() }

            assertNotNull(vm.selectedBook.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `searching filters the book list`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val all = vm.books.value.size

            vm.setBookSearchQuery(DemoData.books.first().displayName)
            advanceUntilIdle()

            assertTrue(vm.books.value.size <= all)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the search restores every book`() = runVmTest {
        val vm = demoVm()
        try {
            advanceUntilIdle()
            val all = vm.books.value.size
            vm.setBookSearchQuery("zzzz")
            advanceUntilIdle()

            vm.setBookSearchQuery("")
            advanceUntilIdle()

            assertEquals(all, vm.books.value.size)
        } finally {
            tearDown(vm)
        }
    }

    // ── How a refusal is worded ──────────────────────────────────────────
    //
    // Same mapping as the songs tab, on the bible tab's own actions.

    private suspend fun BibleViewModel.toastAfterFailedAdd(ws: FakeWsSender, error: Throwable): ToastEvent? {
        openFirstChapter()
        toggleVerseSelection(0)
        ws.failWith(error)
        addToSchedule()
        return toastEvent.first { it != null && it !is ToastEvent.BibleAddedToSchedule }
    }

    @Test
    fun `an operator pressing deny is reported as denied`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            assertIs<ToastEvent.RequestDenied>(vm.toastAfterFailedAdd(ws, ApiException(403, "denied")))
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a blocked session is its own message`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            assertIs<ToastEvent.SessionBlocked>(vm.toastAfterFailedAdd(ws, ApiException(403, "blocked")))
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a refusal with no reason shows the status code`() = runVmTestUnconfined {
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
            val toast = vm.toastAfterFailedAdd(ws, ApiException(503, "No bible loaded"))

            assertIs<ToastEvent.RequestRejectedWithReason>(toast)
            assertEquals("No bible loaded", toast.reason)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failure that is not the desktop answering falls back`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            assertIs<ToastEvent.FailedToAddBibleSchedule>(
                vm.toastAfterFailedAdd(ws, IllegalStateException("Connection refused")),
            )
        } finally {
            tearDown(vm)
        }
    }

    // ── When an action fails mid-service ─────────────────────────────────

    @Test
    fun `a failed clear still stops this screen showing as live`() = runVmTestUnconfined {
        // The desktop may or may not have cleared; what matters is that the phone
        // does not keep claiming a verse is up when the request errored.
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            vm.isProjecting.first { it }

            ws.failWith(IllegalStateException("socket closed"))
            vm.clearDisplay()

            assertFalse(vm.isProjecting.value)
            assertNull(vm.projectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed hold leaves the flag where the operator put it`() = runVmTestUnconfined {
        // The toggle is optimistic; the request failing does not un-press it.
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            ws.failWith(IllegalStateException("socket closed"))

            vm.toggleHold()

            assertTrue(vm.isHolding.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed verse move is reported`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)
            vm.toggleProjecting()
            vm.isProjecting.first { it }
            ws.failWith(IllegalStateException("socket closed"))

            vm.selectVerse(2)

            // The screen still tracks where the operator moved to.
            assertEquals(2, vm.projectedVerseIndex.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a chapter that will not load is reported and leaves no verses`() = runVmTestUnconfined {
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val reader = object : BibleReader {
            override suspend fun getBooks() =
                Result.success(listOf(BibleBook(name = "Genesis", chapterTotal = 50, bookId = 1)))
            override suspend fun getChapter(bookNumber: Int, chapter: Int): Result<List<BibleVerse>> =
                Result.failure(Exception("Connect timeout has expired"))
        }
        val mode = MutableStateFlow(AppMode.REMOTE)
        val vm = BibleViewModel(
            appSettings = settings,
            eventService = ws,
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, reader, LocalBibleRepository(InMemoryFileStorage()) { 1L }),
            serviceFactory = { BibleService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            val book = vm.books.first { it.isNotEmpty() }.first()
            vm.selectBook(book)

            vm.selectChapter(1)
            vm.isLoading.first { !it }

            assertTrue(vm.verses.value.isEmpty())
            assertNotNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a book list that will not load is reported`() = runVmTestUnconfined {
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val reader = object : BibleReader {
            override suspend fun getBooks(): Result<List<BibleBook>> =
                Result.failure(Exception("Connect timeout has expired"))
            override suspend fun getChapter(bookNumber: Int, chapter: Int) =
                Result.success(emptyList<BibleVerse>())
        }
        val mode = MutableStateFlow(AppMode.REMOTE)
        val vm = BibleViewModel(
            appSettings = settings,
            eventService = ws,
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, reader, LocalBibleRepository(InMemoryFileStorage()) { 1L }),
            serviceFactory = { BibleService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            val error = vm.error.first { it != null }

            assertNotNull(error)
            assertTrue(vm.books.value.isEmpty())
            assertFalse(vm.isLoading.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `refreshing after a failure asks again`() = runVmTestUnconfined {
        var attempt = 0
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val asked = MutableStateFlow(0)
        val reader = object : BibleReader {
            override suspend fun getBooks(): Result<List<BibleBook>> {
                asked.value = ++attempt
                return if (attempt == 1) Result.failure(Exception("Connect timeout has expired"))
                else Result.success(listOf(BibleBook(name = "Genesis", chapterTotal = 50, bookId = 1)))
            }
            override suspend fun getChapter(bookNumber: Int, chapter: Int) =
                Result.success(emptyList<BibleVerse>())
        }
        val mode = MutableStateFlow(AppMode.REMOTE)
        val vm = BibleViewModel(
            appSettings = settings,
            eventService = ws,
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, reader, LocalBibleRepository(InMemoryFileStorage()) { 1L }),
            serviceFactory = { BibleService(it, ws, mockClient { respond("{}") }) },
        )
        try {
            vm.error.first { it != null }

            vm.refresh()
            val books = vm.books.first { it.isNotEmpty() }

            assertEquals(listOf("Genesis"), books.map { it.displayName })
            assertNull(vm.error.value, "the banner outlived the thing it described")
        } finally {
            tearDown(vm)
        }
    }

    // ── Guards and housekeeping ──────────────────────────────────────────

    @Test
    fun `a consumed toast is cleared so it shows once`() = runVmTestUnconfined {
        // A snackbar that is not cleared re-shows on the next recomposition, so
        // the operator sees the same message again every time the verse list
        // scrolls.
        val vm = liveVm()
        try {
            vm.openFirstChapter()
            vm.toggleProjecting()
            assertNotNull(vm.toastEvent.first { it != null })

            vm.toastShown()

            assertNull(vm.toastEvent.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `projecting with no verse selected asks for one`() = runVmTestUnconfined {
        // Opening a chapter clears the selection, so this is simply pressing
        // Project before tapping a verse. Sending nothing would blank the
        // audience screen instead.
        val vm = liveVm()
        try {
            vm.openFirstChapter()

            vm.toggleProjecting()

            val toast = assertIs<ToastEvent.FailedToProjectBible>(vm.toastEvent.first { it != null })
            assertEquals("Select at least one verse first", toast.reason)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `nothing is sent to the desktop when no verse is selected`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            val before = ws.calls.size

            vm.toggleProjecting()

            assertEquals(before, ws.calls.size)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a second load keeps the book list already on screen`() = runVmTestUnconfined {
        // Switching tabs re-runs the load; refetching would blank the list every
        // time the operator came back to the Bible tab.
        val vm = liveVm()
        try {
            val loaded = vm.books.first { it.isNotEmpty() }

            vm.loadBooks()

            assertEquals(loaded, vm.books.value)
            assertFalse(vm.isLoading.value)
        } finally {
            tearDown(vm)
        }
    }
}
