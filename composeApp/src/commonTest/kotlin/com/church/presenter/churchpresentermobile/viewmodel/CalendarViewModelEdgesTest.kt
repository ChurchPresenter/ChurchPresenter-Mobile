package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.calendar.CANONICAL_BOOKS
import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.calendar.PickerBook
import com.church.presenter.churchpresentermobile.calendar.RepeatRule
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.network.BibleCatalog
import com.church.presenter.churchpresentermobile.network.BibleReader
import com.church.presenter.churchpresentermobile.network.SongCatalog
import com.church.presenter.churchpresentermobile.network.SongReader
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The planner's edges: what the picker reads from the song and Bible catalogs, and what every
 * action does when the service or row it names is not there, or there is no desktop to ask.
 */
class CalendarViewModelEdgesTest {

    private val repository = CalendarRepository(InMemoryFileStorage(), now = { "2026-09-20T10:00:00Z" })
    private var counter = 0
    private val sunday = LocalDate(2026, 9, 27)

    private class Bible(
        private val books: Result<List<BibleBook>>,
        private val verses: Map<Pair<Int, Int>, List<BibleVerse>> = emptyMap(),
    ) : BibleReader {
        override suspend fun getBooks() = books
        override suspend fun getChapter(bookNumber: Int, chapter: Int): Result<List<BibleVerse>> =
            verses[bookNumber to chapter]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("no such chapter"))
    }

    private class Songs(private val songs: List<Song>) : SongReader {
        override suspend fun getSongs() = Result.success(songs)
        override suspend fun getSongDetail(number: String, bookName: String?, songId: Int, title: String?) =
            Result.failure<SongDetail>(IllegalStateException("not needed here"))
    }

    private val remote = MutableStateFlow(AppMode.REMOTE)

    private fun viewModel(bible: BibleReader? = null, songs: SongReader? = null) = CalendarViewModel(
        repository,
        songCatalog = songs?.let { SongCatalog(remote, it) },
        bibleCatalog = bible?.let { BibleCatalog(remote, it) },
        newId = { "id-${++counter}" },
    )

    @Test
    fun thePickerOffersTheDesktopsSongsAndBooksSkippingBooksWithNoChapters() = runVmTest {
        val bible = Bible(
            Result.success(
                listOf(
                    BibleBook(name = "Genesis", bookId = 1, chapterTotal = 50),
                    BibleBook(name = "Lost book", chapterTotal = 0),
                    BibleBook(name = "Exodus", chapterTotal = 40),
                ),
            ),
        )
        val songs = Songs(listOf(Song(number = "1", title = "Amazing Grace")))
        val vm = viewModel(bible, songs)
        try {
            testScheduler.advanceUntilIdle()
            assertEquals(listOf("Amazing Grace"), vm.songs.value.map { it.title })
            assertFalse(vm.songsLoading.value)
            // A book with no chapters is not something to pick; one with no id takes its place in the list.
            assertEquals(listOf(PickerBook(1, "Genesis", 50), PickerBook(3, "Exodus", 40)), vm.books.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun withNoBibleToReadThePickerKeepsTheCanonicalBooks() = runVmTest {
        val unreachable = viewModel(Bible(Result.failure(IllegalStateException("offline"))))
        val empty = viewModel(Bible(Result.success(listOf(BibleBook(name = "Nothing", chapterTotal = 0)))))
        try {
            testScheduler.advanceUntilIdle()
            assertEquals(CANONICAL_BOOKS, unreachable.books.value)
            assertEquals(CANONICAL_BOOKS, empty.books.value)
        } finally {
            tearDown(unreachable, empty)
        }
    }

    @Test
    fun aChapterPreviewCountsItsVersesAndKeepsTheirFirstWords() = runVmTest {
        val john = PickerBook(43, "John", 21)
        val bible = Bible(
            Result.success(emptyList()),
            mapOf(
                (43 to 3) to listOf(
                    BibleVerse(verse = 16, text = "For God so loved the world"),
                    BibleVerse(verse = 17, content = "For God sent not his Son"),
                ),
                (43 to 4) to emptyList(),
            ),
        )
        val vm = viewModel(bible)
        val none = viewModel()
        try {
            val preview = assertNotNull(vm.chapterPreview(john, 3))
            assertEquals(17, preview.verseCount)
            assertEquals("For God sent not his Son", preview.firstWords[17])
            assertNull(vm.chapterPreview(john, 4), "a chapter with no verses has nothing to preview")
            assertNull(vm.chapterPreview(john, 99), "nor does one the Bible cannot read")
            assertNull(none.chapterPreview(john, 3), "nor does a planner with no Bible at all")
        } finally {
            tearDown(vm, none)
        }
    }

    @Test
    fun actionsOnAServiceThatIsNotThereDoNothing() = runVmTest {
        val vm = viewModel()
        try {
            vm.rows.add("missing", PlanRow.Song("r1", "Opening"), 60, RowTiming.DEFAULT)
            vm.rows.remove("missing", "r1")
            vm.rows.move("missing", 0, 1)
            vm.services.setArmed("missing", true)
            vm.services.saveAsTemplate("missing", "Standard")
            vm.services.copyLastInto(sunday)
            assertTrue(vm.document.value.services.isEmpty())
            assertTrue(vm.document.value.templates.isEmpty())
            assertNull(vm.openServiceId.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun rowsAreUpdatedInPlaceAndANewRowIdIsNeverRepeated() = runVmTest {
        val vm = viewModel()
        try {
            vm.select(sunday)
            val id = vm.services.add("Sunday", "10:00", "sunday", null)
            vm.rows.add(id, PlanRow.Song("r1", "Opening"), 60, RowTiming.DEFAULT)
            vm.rows.update(id, PlanRow.Song("r1", "Opening hymn"), 90, RowTiming.DEFAULT)
            assertEquals("Opening hymn", repository.service(id)!!.rows.single().title)
            assertTrue(vm.rows.newId() != vm.rows.newId())

            vm.services.update(repository.service(id)!!.copy(name = "Sunday morning"))
            assertEquals("Sunday morning", repository.service(id)!!.name)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun copyingAServiceAlreadyInASeriesKeepsThatSeries() = runVmTest {
        val vm = viewModel()
        try {
            vm.select(sunday)
            val id = vm.services.add("Sunday", "10:00", "sunday", null)
            vm.services.copy(id, RepeatRule.WEEKLY, 1, includeRows = false, includeCues = false)
            val series = repository.service(id)!!.seriesId
            // The copy is now the latest in the series; copying it again adds to the same one.
            val latest = vm.document.value.services.first { it.id != id }
            assertEquals(1, vm.services.copy(latest.id, RepeatRule.ONCE, 1, includeRows = false, includeCues = false))
            assertTrue(vm.document.value.services.all { it.seriesId == series })
            assertEquals(3, vm.document.value.services.size)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun deletingAServiceThatIsNotOpenLeavesTheOpenOneOpen() = runVmTest {
        val vm = viewModel()
        try {
            vm.select(sunday)
            val first = vm.services.add("First", "09:00", "sunday", null)
            val second = vm.services.add("Second", "11:00", "sunday", null)
            assertEquals(second, vm.openServiceId.value)
            vm.services.delete(first)
            assertEquals(second, vm.openServiceId.value)
            vm.closeService()
            assertNull(vm.openServiceId.value)
            vm.openService(second)
            assertEquals(second, vm.openServiceId.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun withNoDesktopAndNoRelayPairingAsksNothingAndSyncsNothing() = runVmTest {
        val vm = viewModel()
        try {
            vm.pairing.start()
            assertEquals(EnrollFlow.Idle, vm.enrollment.value, "there is no desktop to ask")
            vm.pairing.syncNow()
            vm.pairing.leave()
            assertEquals(EnrollFlow.Idle, vm.enrollment.value)
        } finally {
            tearDown(vm)
        }
    }
}
