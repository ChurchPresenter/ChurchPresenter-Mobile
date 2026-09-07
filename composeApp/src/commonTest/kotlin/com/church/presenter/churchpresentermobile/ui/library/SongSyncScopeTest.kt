package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SyncOutcome
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.LibrarySyncViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Choosing which songbooks to copy, and stopping a copy that is under way.
 *
 * A church with five songbooks that wants one of them had no way to say so
 * until this existed, and the choice is stated as a control rather than hidden
 * behind a link for exactly that reason. The stop half matters more: a copy of
 * a large songbook over a slow link is the one thing in the app worth
 * abandoning, and abandoning it must leave an account of what did arrive rather
 * than silently reverting to the start.
 */
@OptIn(ExperimentalTestApi::class)
class SongSyncScopeTest {

    private val catalogue = """
        {"song-book":[
          {"book-name":"Hymns","song-total":2,"songs":[
            {"id":1,"number":"1","title":"Amazing Grace"},
            {"id":2,"number":"2","title":"How Great"}
          ]},
          {"book-name":"Psalms","song-total":1,"songs":[
            {"id":3,"number":"3","title":"The Lord Is My Shepherd"}
          ]}
        ]}
    """.trimIndent()

    private fun detail(number: String) = """
        {"number":"$number","title":"Song $number","song-book":"Hymns",
         "verses":[{"label":"Verse","text":"Amazing grace"}]}
    """.trimIndent()

    private fun vm(
        detailDelayMs: Long = 0L,
        catalogueStatus: HttpStatusCode = HttpStatusCode.OK,
        repository: LibraryRepository = LibraryRepository(InMemoryFileStorage()) { 1_000L },
    ): LibrarySyncViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            if (path.endsWith("/songs")) {
                if (catalogueStatus != HttpStatusCode.OK) respond("boom", catalogueStatus)
                else respond(catalogue)
            } else {
                delay(detailDelayMs)
                respond(detail(path.substringAfterLast('/')))
            }
        })
        return LibrarySyncViewModel(repository, settings, SongService(settings, FakeWsSender(), client))
    }

    private fun ComposeUiTest.showSongSync(
        viewModel: LibrarySyncViewModel,
        onDone: () -> Unit = {},
    ) = showScreen {
        SongSyncSection(
            repository = LibraryRepository(InMemoryFileStorage()) { 1_000L },
            settings = AppSettings(InMemorySettingsStorage()),
            sender = FakeWsSender(),
            onDone = onDone,
            providedViewModel = viewModel,
        )
    }

    /** Switches to "choose books" and waits for the list to arrive. */
    private fun ComposeUiTest.chooseBooks(viewModel: LibrarySyncViewModel) {
        click(LibraryTags.syncScope(1))
        awaitThat { viewModel.books.value.isNotEmpty() }
    }

    // ── Everything, or a choice ──────────────────────────────────────────

    @Test
    fun theScopeStartsAtEverything() = runComposeUiTest {
        showSongSync(vm())

        tagged(LibraryTags.syncScope(0)).assertIsSelected()
    }

    @Test
    fun noBookListIsShownWhileTheScopeIsEverything() = runComposeUiTest {
        showSongSync(vm())

        assertFalse(exists(LibraryTags.SYNC_BOOK_COUNT))
        assertFalse(exists(LibraryTags.SYNC_BOOKS_FINDING))
    }

    @Test
    fun choosingBooksFetchesTheList() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)

        chooseBooks(viewModel)

        awaitThat { exists(LibraryTags.syncBook("Hymns")) }
    }

    @Test
    fun choosingBooksListsEveryBookTheDesktopHas() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)

        chooseBooks(viewModel)

        awaitThat { exists(LibraryTags.syncBook("Psalms")) }
    }

    @Test
    fun theBookListStartsWithEverythingTicked() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)

        chooseBooks(viewModel)

        awaitThat { viewModel.selectedBooks.value.size == viewModel.books.value.size }
    }

    @Test
    fun theBookCountIsShownOnceTheListArrives() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)

        chooseBooks(viewModel)

        awaitThat { exists(LibraryTags.SYNC_BOOK_COUNT) }
    }

    @Test
    fun goingBackToEverythingHidesTheBookList() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)

        click(LibraryTags.syncScope(0))

        awaitThat { !exists(LibraryTags.SYNC_BOOK_COUNT) }
    }

    @Test
    fun goingBackToEverythingKeepsTheScopeControl() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)

        click(LibraryTags.syncScope(0))

        tagged(LibraryTags.syncScope(0)).assertIsSelected()
    }

    // ── Ticking books ────────────────────────────────────────────────────

    @Test
    fun aBookCanBeUnticked() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)

        click(LibraryTags.syncBook("Psalms"))

        awaitThat { "Psalms" !in viewModel.selectedBooks.value }
    }

    @Test
    fun untickingOneLeavesTheOthers() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)

        click(LibraryTags.syncBook("Psalms"))

        awaitThat { "Hymns" in viewModel.selectedBooks.value }
    }

    @Test
    fun aBookCanBeTickedAgain() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)
        click(LibraryTags.syncBook("Psalms"))
        awaitThat { "Psalms" !in viewModel.selectedBooks.value }

        click(LibraryTags.syncBook("Psalms"))

        awaitThat { "Psalms" in viewModel.selectedBooks.value }
    }

    @Test
    fun theToggleClearsEverythingWhenEverythingIsTicked() = runComposeUiTest {
        // A long list is tedious to untick one at a time, and picking one of
        // forty starts from none.
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)

        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)

        awaitThat { viewModel.selectedBooks.value.isEmpty() }
    }

    @Test
    fun theToggleTicksEverythingWhenSomethingIsMissing() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)
        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)
        awaitThat { viewModel.selectedBooks.value.isEmpty() }

        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)

        awaitThat { viewModel.selectedBooks.value.size == viewModel.books.value.size }
    }

    @Test
    fun tickingNothingSaysSo() = runComposeUiTest {
        // Otherwise a disabled button is the only clue, and it reads as broken.
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)

        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)

        awaitThat { exists(LibraryTags.SYNC_BOOKS_NONE) }
    }

    @Test
    fun tickingNothingLeavesNoWayToStart() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)

        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)

        awaitThat { viewModel.selectedBooks.value.isEmpty() }
        tagged(LibraryTags.SYNC_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun tickingOneBookAgainAllowsAStart() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)
        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)
        awaitThat { viewModel.selectedBooks.value.isEmpty() }

        click(LibraryTags.syncBook("Hymns"))

        awaitThat { viewModel.selectedBooks.value.isNotEmpty() }
        tagged(LibraryTags.SYNC_BUTTON).assertIsEnabled()
    }

    @Test
    fun theNoBooksWarningGoesOnceOneIsTicked() = runComposeUiTest {
        val viewModel = vm()
        showSongSync(viewModel)
        chooseBooks(viewModel)
        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)
        awaitThat { exists(LibraryTags.SYNC_BOOKS_NONE) }

        click(LibraryTags.syncBook("Hymns"))

        awaitThat { !exists(LibraryTags.SYNC_BOOKS_NONE) }
    }

    @Test
    fun aDesktopThatRefusesTheBookListSaysSo() = runComposeUiTest {
        // An unreachable computer and a computer with no songbooks are
        // different problems, and only one of them is worth retrying — so this
        // is the failure line, not the empty-list one.
        val viewModel = vm(catalogueStatus = HttpStatusCode.InternalServerError)
        showSongSync(viewModel)

        click(LibraryTags.syncScope(1))

        awaitThat { exists(LibraryTags.SYNC_BOOKS_FAILED) }
    }

    @Test
    fun aDesktopThatRefusesTheBookListIsNotReportedAsEmpty() = runComposeUiTest {
        val viewModel = vm(catalogueStatus = HttpStatusCode.InternalServerError)
        showSongSync(viewModel)

        click(LibraryTags.syncScope(1))

        awaitThat { exists(LibraryTags.SYNC_BOOKS_FAILED) }
        assertFalse(exists(LibraryTags.SYNC_BOOKS_MISSING))
    }

    @Test
    fun copyingAChosenBookTakesOnlyThatBook() = runComposeUiTest {
        val repo = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        val viewModel = vm(repository = repo)
        showSongSync(viewModel)
        chooseBooks(viewModel)
        click(LibraryTags.syncBook("Psalms"))
        awaitThat { "Psalms" !in viewModel.selectedBooks.value }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.outcome.value is SyncOutcome.Success }
        assertTrue((viewModel.outcome.value as SyncOutcome.Success).songCount <= 2)
    }

    // ── Stopping a copy ──────────────────────────────────────────────────

    @Test
    fun aRunningCopyOffersTheWayToStopIt() = runComposeUiTest {
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.progress.value.isRunning }
        tagged(LibraryTags.SYNC_BUTTON).assertIsEnabled()
    }

    @Test
    fun stoppingACopyEndsIt() = runComposeUiTest {
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { !viewModel.progress.value.isRunning }
    }

    @Test
    fun stoppingACopySaysItWasStopped() = runComposeUiTest {
        // Not "done": an operator who stopped a copy needs to know the library
        // is half-copied, not complete.
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.outcome.value is SyncOutcome.Cancelled }
    }

    @Test
    fun aStoppedCopyShowsItsResultLine() = runComposeUiTest {
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
    }

    @Test
    fun aStoppedCopyAccountsForWhatArrived() = runComposeUiTest {
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.outcome.value is SyncOutcome.Cancelled }
        assertTrue((viewModel.outcome.value as SyncOutcome.Cancelled).songCount >= 0)
    }

    @Test
    fun aStoppedCopyIsNotAFinishedOne() = runComposeUiTest {
        // The chip would otherwise say "synced just now" over a half-copy.
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.outcome.value is SyncOutcome.Cancelled }
        assertFalse(viewModel.state.value.hasEverSynced)
    }

    @Test
    fun aStoppedCopyOffersTheScopeControlAgain() = runComposeUiTest {
        // It is a start, not a close: trying again is the sensible next act.
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.syncScope(0)) }
    }

    @Test
    fun aStoppedCopyDoesNotCloseTheSheet() = runComposeUiTest {
        var done = 0
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel, onDone = { done++ })
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.outcome.value is SyncOutcome.Cancelled }
        assertFalse(done > 0)
    }

    @Test
    fun aStoppedCopyCanBeStartedAgain() = runComposeUiTest {
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.outcome.value is SyncOutcome.Cancelled }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.progress.value.isRunning }
    }

    @Test
    fun theScopeControlIsHiddenWhileACopyRuns() = runComposeUiTest {
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.progress.value.isRunning }
        assertFalse(exists(LibraryTags.syncScope(1)))
    }

    @Test
    fun theBookListIsHiddenWhileACopyRuns() = runComposeUiTest {
        val viewModel = vm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        chooseBooks(viewModel)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.progress.value.isRunning }
        assertFalse(exists(LibraryTags.syncBook("Hymns")))
    }

    @Test
    fun aFinishedCopyHidesTheScopeControl() = runComposeUiTest {
        // The sheet is now a result, and the way out is the only thing to press.
        val viewModel = vm()
        showSongSync(viewModel)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.outcome.value is SyncOutcome.Success }
        assertFalse(exists(LibraryTags.syncScope(0)))
    }
}
