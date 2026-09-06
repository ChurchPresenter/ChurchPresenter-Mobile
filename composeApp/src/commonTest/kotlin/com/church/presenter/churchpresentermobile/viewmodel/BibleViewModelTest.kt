package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
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
}
