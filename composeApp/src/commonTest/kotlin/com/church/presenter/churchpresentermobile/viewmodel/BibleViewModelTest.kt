package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
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
        vm.toggleVerseSelection(2)
        assertEquals(setOf(2), vm.selectedVerseIndices.value)
        vm.toggleVerseSelection(2) // tapping the only selected verse clears it
        assertEquals(emptySet(), vm.selectedVerseIndices.value)
        vm.toggleVerseSelection(2)
        vm.toggleVerseSelection(5) // single-select replaces
        assertEquals(setOf(5), vm.selectedVerseIndices.value)
    }

    @Test
    fun multiSelectAccumulatesAndTogglesOut() = runVmTest {
        val vm = demoVm()
        vm.toggleMultiSelectMode()
        assertTrue(vm.isMultiSelectMode.value)
        vm.toggleVerseSelection(2)
        vm.toggleVerseSelection(5)
        assertEquals(setOf(2, 5), vm.selectedVerseIndices.value)
        vm.toggleVerseSelection(2)
        assertEquals(setOf(5), vm.selectedVerseIndices.value)
    }

    @Test
    fun leavingMultiSelectClearsSelection() = runVmTest {
        val vm = demoVm()
        vm.toggleMultiSelectMode()
        vm.toggleVerseSelection(1)
        vm.toggleMultiSelectMode()
        assertFalse(vm.isMultiSelectMode.value)
        assertEquals(emptySet(), vm.selectedVerseIndices.value)
    }

    // ── Book search filter (demo data) ───────────────────────────────────────

    @Test
    fun bookSearchFiltersByDisplayName() = runVmTest {
        val vm = demoVm()
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
    }
}
