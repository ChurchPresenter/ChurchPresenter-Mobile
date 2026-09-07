package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Choosing which songbooks come across.
 *
 * The picker existed but was read as a footnote, so every copy took the whole
 * catalogue; these cover the numbers the sheet now shows and the scope it sends.
 */
class LibrarySyncScopeTest {

    private val catalogue = """
        {
          "song-book": [
            {
              "book-name": "Hymns",
              "song-total": 2,
              "songs": [
                { "id": 1, "number": "1", "title": "Amazing Grace" },
                { "id": 2, "number": "2", "title": "Be Thou My Vision" }
              ]
            },
            {
              "book-name": "Choruses",
              "song-total": 1,
              "songs": [ { "id": 3, "number": "1", "title": "Shine Jesus Shine" } ]
            }
          ]
        }
    """.trimIndent()

    private fun vm(
        body: String = catalogue,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): LibrarySyncViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val client = mockClient {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return LibrarySyncViewModel(
            repository = LibraryRepository(InMemoryFileStorage()) { 1_000L },
            settings = settings,
            songService = SongService(settings, FakeWsSender(), client),
        )
    }

    @Test
    fun theBookListArrivesWithEveryBookTicked() = runVmTestUnconfined {
        // Anyone who ignores the list and presses Copy must get what they got before.
        val viewModel = vm()

        viewModel.loadBooks()
        val books = viewModel.books.first { it.isNotEmpty() }

        assertEquals(listOf("Choruses", "Hymns"), books)
        assertEquals(books.toSet(), viewModel.selectedBooks.value)
    }

    @Test
    fun eachBookSaysHowManySongsItHolds() = runVmTestUnconfined {
        // The size of the choice is the thing an operator is actually deciding on.
        val viewModel = vm()

        viewModel.loadBooks()
        val counts = viewModel.bookCounts.first { it.isNotEmpty() }

        assertEquals(2, counts["Hymns"])
        assertEquals(1, counts["Choruses"])
    }

    @Test
    fun anUnreachableComputerIsSaidSoRatherThanReportedAsNoSongbooks() = runVmTestUnconfined {
        val viewModel = vm(body = "nope", status = HttpStatusCode.ServiceUnavailable)

        viewModel.loadBooks()
        viewModel.booksError.first { it != null }

        assertTrue(viewModel.books.value.isEmpty())
    }

    @Test
    fun askingAgainClearsTheLastFailure() = runVmTestUnconfined {
        val viewModel = vm()

        viewModel.loadBooks()
        viewModel.books.first { it.isNotEmpty() }

        assertNull(viewModel.booksError.value)
    }

    @Test
    fun untickingEveryBookIsNotACopy() = runVmTestUnconfined {
        // Copying nothing and reporting success reads as the feature being broken.
        val viewModel = vm()
        viewModel.setChooseBooks(true)
        viewModel.books.first { it.isNotEmpty() }

        viewModel.clearBooks()

        assertFalse(viewModel.canSync)
    }

    @Test
    fun switchingBackToAllCopiesEverythingAgain() = runVmTestUnconfined {
        // The ticked set survives the switch, so "all" must not be read off it.
        val viewModel = vm()
        viewModel.setChooseBooks(true)
        viewModel.books.first { it.isNotEmpty() }
        viewModel.clearBooks()

        viewModel.setChooseBooks(false)

        assertTrue(viewModel.canSync)
    }

    @Test
    fun refreshingDropsAStaleResult() = runVmTestUnconfined {
        // The view model is keyed to the Library tab, not the sheet, so an old
        // "copied 240 songs" greeted the next open — including after a wipe.
        val viewModel = vm()
        viewModel.sync()
        viewModel.outcome.first { it != null }

        viewModel.refreshState()

        assertNull(viewModel.outcome.value)
    }
}
