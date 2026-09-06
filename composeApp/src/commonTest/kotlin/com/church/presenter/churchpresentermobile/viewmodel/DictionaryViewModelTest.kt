package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.BibleService
import com.church.presenter.churchpresentermobile.network.DictionaryFilter
import com.church.presenter.churchpresentermobile.network.DictionaryService
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import com.church.presenter.churchpresentermobile.testutil.tearDown
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the Strong's dictionary screen.
 *
 * Every path here is a request — the dictionary lives on the desktop and there is
 * no demo or local fallback — so the whole class is driven through mocked
 * services rather than through a mode flag.
 */
class DictionaryViewModelTest {

    private val entriesJson = """
        [{"number":"H1254","word":"bara","definition":"to create"},
         {"number":"G26","word":"agape","definition":"love"}]
    """.trimIndent()

    private val booksJson = """{"books":[{"name":"Genesis","chapter-total":50}]}"""

    private fun vm(
        ws: FakeWsSender = FakeWsSender(),
        dictionary: MockRequestHandleScope.(path: String) -> HttpResponseData = { respond(entriesJson) },
        bible: MockRequestHandleScope.(path: String) -> HttpResponseData = { respond(booksJson) },
    ): DictionaryViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return DictionaryViewModel(
            settings,
            ws,
            serviceFactory = { DictionaryService(it, ws, mockClient(dictionary)) },
            bibleServiceFactory = { BibleService(it, ws, mockClient(bible)) },
        )
    }

    // ── Search ───────────────────────────────────────────────────────────

    @Test
    fun `the screen loads entries on open`() = runVmTestUnconfined {
        val vm = vm()
        try {
            val entries = vm.entries.first { it.isNotEmpty() }

            assertEquals(2, entries.size)
            assertEquals("H1254", entries.first().number)
            assertNull(vm.error.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed search is reported and stops the spinner`() = runVmTestUnconfined {
        val vm = vm(dictionary = { respond("boom", HttpStatusCode.InternalServerError) })
        try {
            val error = vm.error.first { it != null }

            assertNotNull(error)
            vm.isLoading.first { !it }
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `typing sets the query`() = runVmTestUnconfined {
        val vm = vm()
        try {
            vm.entries.first { it.isNotEmpty() }

            vm.setSearchQuery("love")

            assertEquals("love", vm.searchQuery.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `changing the filter re-runs the search`() = runVmTestUnconfined {
        // A StateFlow rather than a list: `first { }` only resumes on a new
        // emission, and appending to a plain list produces none.
        val asked = MutableStateFlow(0)
        val vm = vm(dictionary = { asked.value += 1; respond(entriesJson) })
        try {
            vm.entries.first { it.isNotEmpty() }
            val before = asked.value

            vm.setFilter(DictionaryFilter.HEBREW)
            asked.first { it > before }

            assertEquals(DictionaryFilter.HEBREW, vm.filter.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `setting the same filter again does not re-ask`() = runVmTestUnconfined {
        val vm = vm()
        try {
            vm.entries.first { it.isNotEmpty() }

            vm.setFilter(DictionaryFilter.ALL)

            assertEquals(DictionaryFilter.ALL, vm.filter.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── The Book → Chapter → Verse filter ────────────────────────────────

    @Test
    fun `the book list is loaded for the reference filter`() = runVmTestUnconfined {
        val vm = vm()
        try {
            val books = vm.books.first { it.isNotEmpty() }

            assertEquals("Genesis", books.first().displayName)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed book list leaves the filter unavailable but search still works`() = runVmTestUnconfined {
        // Silent by design: the filter is an extra, not the feature.
        val vm = vm(bible = { respond("no", HttpStatusCode.InternalServerError) })
        try {
            val entries = vm.entries.first { it.isNotEmpty() }

            assertTrue(vm.books.value.isEmpty())
            assertEquals(2, entries.size)
            assertNull(vm.error.value, "a missing filter is not a search error")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `choosing a book resets any chapter and verse below it`() = runVmTestUnconfined {
        val vm = vm()
        try {
            val books = vm.books.first { it.isNotEmpty() }
            vm.setRefBook(books.first())
            vm.setRefChapter(1)
            vm.setRefVerse(3)

            vm.setRefBook(books.first())

            assertNull(vm.refChapter.value)
            assertNull(vm.refVerse.value)
            assertEquals(0, vm.refVerseCount.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `choosing a chapter resets the verse below it`() = runVmTestUnconfined {
        val vm = vm()
        try {
            val books = vm.books.first { it.isNotEmpty() }
            vm.setRefBook(books.first())
            vm.setRefChapter(1)
            vm.setRefVerse(3)

            vm.setRefChapter(2)

            assertEquals(2, vm.refChapter.value)
            assertNull(vm.refVerse.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the reference empties the whole cascade`() = runVmTestUnconfined {
        val vm = vm()
        try {
            val books = vm.books.first { it.isNotEmpty() }
            vm.setRefBook(books.first())
            vm.setRefChapter(1)
            vm.setRefVerse(2)

            vm.clearReference()

            assertNull(vm.refBook.value)
            assertNull(vm.refChapter.value)
            assertNull(vm.refVerse.value)
            assertEquals(0, vm.refVerseCount.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Opening an entry ─────────────────────────────────────────────────

    @Test
    fun `selecting an entry opens it and loads where it appears`() = runVmTestUnconfined {
        val vm = vm(dictionary = { path ->
            if (path.contains("verses")) respond(
                """{"number":"H1254","total":1,"verses":[{"bookName":"Genesis","chapter":1,"verse":1,""" +
                    """"reference":"Genesis 1:1","text":"In the beginning"}]}""",
            )
            else respond(entriesJson)
        })
        try {
            val entries = vm.entries.first { it.isNotEmpty() }

            vm.selectEntry(entries.first())
            val appears = vm.appearsIn.first { it != null }

            assertEquals("H1254", vm.selectedEntry.value?.number)
            assertEquals(1, appears?.total)
            assertEquals("Genesis 1:1", appears?.verses?.first()?.reference)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `closing an entry clears it and its verses`() = runVmTestUnconfined {
        val vm = vm()
        try {
            val entries = vm.entries.first { it.isNotEmpty() }
            vm.selectEntry(entries.first())

            vm.clearSelection()

            assertNull(vm.selectedEntry.value)
            assertNull(vm.appearsIn.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a tapped Strongs link opens that entry`() = runVmTestUnconfined {
        // Definitions carry H####/G#### cross-references the reader can follow.
        val vm = vm(dictionary = { path ->
            if (path.contains("H430")) respond("""{"number":"H430","word":"elohim","definition":"God"}""")
            else respond(entriesJson)
        })
        try {
            vm.entries.first { it.isNotEmpty() }

            vm.selectByNumber("H430")
            val opened = vm.selectedEntry.first { it != null }

            assertEquals("H430", opened?.number)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a link to an entry the desktop does not have is reported`() = runVmTestUnconfined {
        val vm = vm(dictionary = { path ->
            if (path.contains("H999")) respond("gone", HttpStatusCode.NotFound)
            else respond(entriesJson)
        })
        try {
            vm.entries.first { it.isNotEmpty() }

            vm.selectByNumber("H999")
            val error = vm.actionError.first { it != null }

            assertTrue(error!!.contains("H999"), error)
        } finally {
            tearDown(vm)
        }
    }

    // ── Projecting and scheduling ────────────────────────────────────────

    @Test
    fun `projecting the open entry sends it and records it live`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = vm(ws)
        try {
            val entries = vm.entries.first { it.isNotEmpty() }
            vm.selectEntry(entries.first())

            vm.projectSelected()
            val live = vm.projectedNumber.first { it != null }

            assertEquals("H1254", live)
            assertEquals(WsMessageType.PROJECT, ws.lastType)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `projecting with nothing open does nothing`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = vm(ws)
        try {
            vm.entries.first { it.isNotEmpty() }

            vm.projectSelected()

            assertNull(vm.projectedNumber.value)
            assertTrue(ws.calls.isEmpty())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a refused projection is reported`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("denied"))
        val vm = vm(ws)
        try {
            val entries = vm.entries.first { it.isNotEmpty() }
            vm.selectEntry(entries.first())

            vm.projectSelected()
            val error = vm.actionError.first { it != null }

            assertNotNull(error)
            assertNull(vm.projectedNumber.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding the open entry to the schedule confirms`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = vm(ws)
        try {
            val entries = vm.entries.first { it.isNotEmpty() }
            vm.selectEntry(entries.first())

            vm.addSelectedToSchedule()
            vm.scheduleAdded.first { it }

            assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `opening another entry clears the previous add confirmation`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = vm(ws)
        try {
            val entries = vm.entries.first { it.isNotEmpty() }
            vm.selectEntry(entries.first())
            vm.addSelectedToSchedule()
            vm.scheduleAdded.first { it }

            vm.selectEntry(entries.last())

            assertEquals(false, vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a dismissed action error goes away`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("denied"))
        val vm = vm(ws)
        try {
            val entries = vm.entries.first { it.isNotEmpty() }
            vm.selectEntry(entries.first())
            vm.projectSelected()
            vm.actionError.first { it != null }

            vm.clearActionError()

            assertNull(vm.actionError.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `saving settings re-runs the search against the new server`() = runVmTestUnconfined {
        val asked = MutableStateFlow(0)
        val vm = vm(dictionary = { asked.value += 1; respond(entriesJson) })
        try {
            vm.entries.first { it.isNotEmpty() }
            val before = asked.value

            vm.onSettingsSaved()

            asked.first { it > before }
        } finally {
            tearDown(vm)
        }
    }

    // ── How many verses a chapter has ────────────────────────────────────
    //
    // The verse dropdown needs a count. It comes from the book metadata when the
    // desktop supplied one, and otherwise from fetching the chapter — the extra
    // request exists so the dropdown is populated either way.

    /**
     * A ViewModel whose bible endpoint answers both roles.
     *
     * `getBooks` and `getChapter` are both `GET /api/bible`; only the query
     * distinguishes them, which the shared [mockClient] helper does not expose.
     * Hence a raw MockEngine here.
     */
    private fun vmWithChapters(
        verseTotal: Int? = null,
        chapterVerses: Int = 3,
        chapterFails: Boolean = false,
    ): DictionaryViewModel {
        val chapters = if (verseTotal != null) {
            ""","chapters":[{"chapter":1,"verse-total":$verseTotal}]"""
        } else {
            ""
        }
        val books = """{"books":[{"name":"Genesis","chapter-total":50$chapters}]}"""
        val verses = """{"verses":[${(1..chapterVerses).joinToString(",") { """{"verse":$it,"text":"v$it"}""" }}]}"""
        val settings = AppSettings(InMemorySettingsStorage())
        val ws = FakeWsSender()
        val bibleClient = HttpClient(MockEngine { request ->
            val isChapter = request.url.parameters["chapter"] != null
            when {
                isChapter && chapterFails -> respond("no", HttpStatusCode.InternalServerError)
                isChapter -> respond(verses, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond(books, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })
        return DictionaryViewModel(
            settings,
            ws,
            serviceFactory = { DictionaryService(it, ws, mockClient { respond(entriesJson) }) },
            bibleServiceFactory = { BibleService(it, ws, bibleClient) },
        )
    }

    @Test
    fun `a verse count already in the book metadata is used without asking`() = runVmTestUnconfined {
        val vm = vmWithChapters(verseTotal = 31)
        try {
            val books = vm.books.first { it.isNotEmpty() }
            vm.setRefBook(books.first())

            vm.setRefChapter(1)
            val count = vm.refVerseCount.first { it > 0 }

            assertEquals(31, count)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `with no metadata the chapter is fetched to count its verses`() = runVmTestUnconfined {
        val vm = vmWithChapters(chapterVerses = 4)
        try {
            val books = vm.books.first { it.isNotEmpty() }
            vm.setRefBook(books.first())

            vm.setRefChapter(1)
            val count = vm.refVerseCount.first { it > 0 }

            assertEquals(4, count)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a zero verse total is not trusted and the chapter is fetched instead`() = runVmTestUnconfined {
        // Some desktops report 0 rather than omitting the field; taking it at face
        // value leaves the dropdown empty.
        val vm = vmWithChapters(verseTotal = 0, chapterVerses = 5)
        try {
            val books = vm.books.first { it.isNotEmpty() }
            vm.setRefBook(books.first())

            vm.setRefChapter(1)
            val count = vm.refVerseCount.first { it > 0 }

            assertEquals(5, count)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed chapter fetch leaves the count at zero rather than erroring`() = runVmTestUnconfined {
        // The dropdown stays unpopulated; search still works, which is the point.
        val vm = vmWithChapters(chapterFails = true)
        try {
            val books = vm.books.first { it.isNotEmpty() }
            vm.setRefBook(books.first())

            vm.setRefChapter(1)

            assertEquals(0, vm.refVerseCount.value)
            assertNull(vm.error.value, "a missing count is not a search failure")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the chapter clears the count too`() = runVmTestUnconfined {
        val vm = vmWithChapters(verseTotal = 31)
        try {
            val books = vm.books.first { it.isNotEmpty() }
            vm.setRefBook(books.first())
            vm.setRefChapter(1)
            vm.refVerseCount.first { it > 0 }

            vm.setRefChapter(null)

            assertEquals(0, vm.refVerseCount.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a chapter chosen with no book selected does nothing`() = runVmTestUnconfined {
        val vm = vmWithChapters(verseTotal = 31)
        try {
            vm.entries.first { it.isNotEmpty() }

            vm.setRefChapter(1)

            assertEquals(0, vm.refVerseCount.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed add to schedule is reported`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = vm(ws)
        try {
            val entries = vm.entries.first { it.isNotEmpty() }
            vm.selectEntry(entries.first())
            ws.failWith(IllegalStateException("denied"))

            vm.addSelectedToSchedule()
            val error = vm.actionError.first { it != null }

            assertEquals("denied", error)
            assertEquals(false, vm.scheduleAdded.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding with nothing open does nothing`() = runVmTestUnconfined {
        val ws = FakeWsSender()
        val vm = vm(ws)
        try {
            vm.entries.first { it.isNotEmpty() }

            vm.addSelectedToSchedule()

            assertEquals(false, vm.scheduleAdded.value)
            assertTrue(ws.calls.isEmpty())
        } finally {
            tearDown(vm)
        }
    }
}
