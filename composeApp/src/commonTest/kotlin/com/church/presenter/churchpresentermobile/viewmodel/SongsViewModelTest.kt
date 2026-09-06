package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
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
}
