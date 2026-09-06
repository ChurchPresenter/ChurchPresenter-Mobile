package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
