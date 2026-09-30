package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.tearDown
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
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
import kotlin.test.assertTrue

/**
 * What a projected verse tells the desktop about where it came from: the phone's own translation
 * and text when a Bible is downloaded onto it, and the canonical book number either way.
 */
class BibleViewModelVerseSourceTest {

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

    /**
     * Remote mode with a Bible downloaded onto the phone: the reader is still the desktop, but
     * the text comes from the phone's own translation — named in Russian, numbered canonically.
     */
    private fun localVm(ws: FakeWsSender): BibleViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val bibles = LocalBibleRepository(InMemoryFileStorage()) { 1L }
        bibles.install(
            "RUS_RB.spb",
            "##Title:\tRussian Bible\n##Abbreviation:\tRB\n1\tБытие\t50\n-----\n" +
                "B001C001V001\t1\t1\t1\tВ начале сотворил Бог небо и землю.\n",
        )
        val desktop = object : BibleReader {
            override suspend fun getBooks(): Result<List<BibleBook>> = Result.success(emptyList())
            override suspend fun getChapter(bookNumber: Int, chapter: Int): Result<List<BibleVerse>> =
                Result.success(emptyList())
        }
        val mode = MutableStateFlow(AppMode.REMOTE)
        return BibleViewModel(
            appSettings = settings,
            eventService = ws,
            isDemoMode = false,
            presenter = null,
            mode = mode,
            catalog = BibleCatalog(mode, desktop, bibles),
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
    fun `a verse from the phone's own Bible is sent with its text translation and book number`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = localVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)

            vm.toggleProjecting()
            vm.isProjecting.first { it }

            val payload = ws.lastPayload
            assertTrue(payload.contains("\"useClientText\":true"), payload)
            assertTrue(payload.contains("\"bibleName\":\"Russian Bible\""), payload)
            assertTrue(payload.contains("\"bibleAbbreviation\":\"RB\""), payload)
            assertTrue(payload.contains("\"bookId\":1"), payload)
            assertTrue(payload.contains("В начале сотворил Бог"), payload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a verse from the desktop's Bible asks for no client text`() = runVmTestUnconfined {
        // An older desktop ignores the new keys; a current one must still resolve its own text.
        val ws = FakeWsSender()
        val vm = liveVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)

            vm.toggleProjecting()
            vm.isProjecting.first { it }

            assertFalse(ws.lastPayload.contains("useClientText"), ws.lastPayload)
            assertFalse(ws.lastPayload.contains("bibleName"), ws.lastPayload)
            assertTrue(ws.lastPayload.contains("\"bookId\":1"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a verse added to the schedule from the phone's Bible carries its book number`() = runVmTestUnconfined {
        // The desktop's schedule looks the book up by number, so a Russian name still opens.
        val ws = FakeWsSender()
        val vm = localVm(ws)
        try {
            vm.openFirstChapter()
            vm.toggleVerseSelection(0)

            vm.addToSchedule()
            vm.scheduleAdded.first { it }

            assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
            assertTrue(ws.lastPayload.contains("\"bookId\":1"), ws.lastPayload)
            assertTrue(ws.lastPayload.contains("\"bookName\":\"Бытие\""), ws.lastPayload)
            assertTrue(ws.lastPayload.contains("\"useClientText\":true"), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }
}
