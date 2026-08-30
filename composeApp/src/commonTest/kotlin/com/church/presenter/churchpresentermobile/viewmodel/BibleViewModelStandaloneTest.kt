package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.network.BibleCatalog
import com.church.presenter.churchpresentermobile.network.BibleReader
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Standalone has no Bible: the text lives on a desktop and none is bundled on
 * the device. The tab must therefore say so quietly rather than firing a request
 * at a machine that isn't there and rendering the timeout as an error.
 */
class BibleViewModelStandaloneTest {

    private fun vm(mode: AppMode) = BibleViewModel(
        appSettings = AppSettings(InMemorySettingsStorage()),
        eventService = FakeWsSender(),
        isDemoMode = false,
        presenter = null,
        mode = MutableStateFlow(mode),
    )

    @Test
    fun standaloneLoadsNothingAndReportsNoError() = runVmTest {
        val vm = vm(AppMode.STANDALONE)
        advanceUntilIdle()

        assertTrue(vm.books.value.isEmpty())
        assertNull(vm.error.value, "an absent computer is not an error the operator can fix")
        assertFalse(vm.isLoading.value, "nothing is loading, so nothing may spin")
        assertTrue(vm.hasNoLocalBibles.value)
    }

    @Test
    fun refreshInStandaloneStaysQuiet() = runVmTest {
        val vm = vm(AppMode.STANDALONE)
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.error.value)
        assertTrue(vm.books.value.isEmpty())
    }

    @Test
    fun remoteModeStillReportsThatBiblesExist() = runVmTest {
        val vm = vm(AppMode.REMOTE)
        advanceUntilIdle()

        assertFalse(vm.hasNoLocalBibles.value)
    }

    // ── The tab that stayed empty ────────────────────────────────────────────

    private fun module(title: String) = """
        ##Title: $title
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning God created the heavens and the earth.
        B001C001V002 1 1 2 Now the earth was formless and void.
    """.trimIndent()

    /** Fails only after [delayMs], the way a real connect timeout does. */
    private class SlowlyTimingOutReader(private val delayMs: Long = 10_000) : BibleReader {
        override suspend fun getBooks(): Result<List<BibleBook>> {
            delay(delayMs)
            return Result.failure(Exception("Connect timeout has expired [url=http://192.168.1.100:8765]"))
        }

        override suspend fun getChapter(bookNumber: Int, chapter: Int): Result<List<BibleVerse>> {
            delay(delayMs)
            return Result.failure(Exception("Connect timeout has expired"))
        }
    }

    @Test
    fun installingATranslationFillsTheTabWithoutASwitchAway() = runVmTest {
        // Reported from a real phone: the download said "installed 1, failed 0",
        // and the tab still read "No Bible books available" until the operator
        // switched tabs and came back. Nothing re-ran the load.
        val bibles = LocalBibleRepository(InMemoryFileStorage()) { 1_700_000_000_000 }
        val mode = MutableStateFlow(AppMode.STANDALONE)
        val settings = AppSettings(InMemorySettingsStorage())
        val vm = BibleViewModel(
            appSettings = settings,
            eventService = FakeWsSender(),
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, SlowlyTimingOutReader(), bibles),
        )
        advanceUntilIdle()
        assertTrue(vm.books.value.isEmpty(), "nothing is installed yet")

        bibles.install("en_KJV.spb", module("King James Version"))
        advanceUntilIdle()

        assertEquals(listOf("Genesis"), vm.books.value.map { it.name })
        assertNull(vm.error.value)
    }

    @Test
    fun theStaleServerErrorGoesWhenATranslationArrives() = runVmTest {
        // The exact journey that was reported. On a first launch the tab is
        // built before the mode picker is answered, so it asks the default
        // desktop address; with nothing downloaded yet there is no local copy to
        // fall back on, and the timeout becomes a banner. Downloading a
        // translation is the moment that banner stops being true.
        val bibles = LocalBibleRepository(InMemoryFileStorage()) { 1_700_000_000_000 }
        val mode = MutableStateFlow(AppMode.REMOTE)
        val vm = BibleViewModel(
            appSettings = AppSettings(InMemorySettingsStorage()),
            eventService = FakeWsSender(),
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, SlowlyTimingOutReader(), bibles),
        )
        advanceUntilIdle()
        assertNotNull(vm.error.value, "nothing installed and no computer: the banner is honest here")

        mode.value = AppMode.STANDALONE
        bibles.install("en_KJV.spb", module("King James Version"))
        advanceUntilIdle()

        assertEquals(listOf("Genesis"), vm.books.value.map { it.name })
        assertNull(vm.error.value, "the banner outlived the thing it described")
        assertFalse(vm.isLoading.value)
    }
}
