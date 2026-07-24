package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
