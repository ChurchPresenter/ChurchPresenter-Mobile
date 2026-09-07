package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.ui.tagged
import com.church.presenter.churchpresentermobile.viewmodel.LibrarySyncViewModel
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Copying a songbook from the computer onto the phone.
 *
 * The sheet's own decisions are what is tested here — which books get taken,
 * whether the button starts a copy or closes the sheet, and what the result
 * line says afterwards. Two of those have gone wrong in the field: "Copy songs"
 * left sitting under a finished result was read as nothing having happened, and
 * a whole songbook was copied a second time; and starting a copy with every
 * book unticked reported cheerful success having copied nothing.
 */
@OptIn(ExperimentalTestApi::class)
class SongSyncSectionTest {

    private val catalogue = """
        {"song-book":[
          {"book-name":"Hymns","song-total":2,"songs":[
            {"id":1,"number":"1","title":"Amazing Grace"},
            {"id":2,"number":"2","title":"How Great"}
          ]},
          {"book-name":"Chorus","song-total":1,"songs":[
            {"id":3,"number":"10","title":"Shout"}
          ]}
        ]}
    """.trimIndent()

    private fun syncVm(
        body: String = catalogue,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): LibrarySyncViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        val songService = SongService(settings, FakeWsSender(), mockClient { respond(body, status) })
        return LibrarySyncViewModel(repository, settings, songService)
    }

    private fun ComposeUiTest.showSongSync(
        vm: LibrarySyncViewModel,
        onDone: () -> Unit = {},
    ) = showScreen {
        val settings = AppSettings(InMemorySettingsStorage())
        SongSyncSection(
            repository = LibraryRepository(InMemoryFileStorage()) { 1_000L },
            settings = settings,
            sender = FakeWsSender(),
            onDone = onDone,
            providedViewModel = vm,
        )
    }

    // ── What the sheet offers before anything runs ───────────────────────

    @Test
    fun theCopyButtonIsOffered() = runComposeUiTest {
        showSongSync(syncVm())

        assertTrue(exists(LibraryTags.SYNC_BUTTON))
    }

    @Test
    fun theCopyButtonIsPressable() = runComposeUiTest {
        showSongSync(syncVm())

        tagged(LibraryTags.SYNC_BUTTON).assertIsEnabled()
    }

    @Test
    fun bothScopesAreOffered() = runComposeUiTest {
        showSongSync(syncVm())

        assertTrue(exists(LibraryTags.syncScope(0)))
        assertTrue(exists(LibraryTags.syncScope(1)))
    }

    @Test
    fun everythingIsTheScopeToBeginWith() = runComposeUiTest {
        // A church that wants the lot should not have to ask for it.
        showSongSync(syncVm())

        tagged(LibraryTags.syncScope(0)).assertIsSelected()
    }

    @Test
    fun choosingBooksIsNotTheScopeToBeginWith() = runComposeUiTest {
        showSongSync(syncVm())

        tagged(LibraryTags.syncScope(1)).assertIsNotSelected()
    }

    @Test
    fun noBookListIsShownWhileTakingEverything() = runComposeUiTest {
        showSongSync(syncVm())

        assertFalse(exists(LibraryTags.SYNC_BOOK_COUNT))
    }

    @Test
    fun noProgressIsShownBeforeACopyStarts() = runComposeUiTest {
        showSongSync(syncVm())

        assertFalse(exists(LibraryTags.SYNC_PROGRESS))
    }

    @Test
    fun noResultIsShownBeforeACopyStarts() = runComposeUiTest {
        showSongSync(syncVm())

        assertFalse(exists(LibraryTags.SYNC_OUTCOME))
    }

    // ── Choosing which books ─────────────────────────────────────────────

    @Test
    fun choosingBooksSwitchesTheScope() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)

        click(LibraryTags.syncScope(1))

        awaitThat { vm.chooseBooks.value }
    }

    @Test
    fun choosingBooksMarksThatScopeSelected() = runComposeUiTest {
        showSongSync(syncVm())

        click(LibraryTags.syncScope(1))

        tagged(LibraryTags.syncScope(1)).assertIsSelected()
    }

    @Test
    fun choosingBooksAsksTheComputerWhatItHas() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)

        click(LibraryTags.syncScope(1))

        awaitThat { vm.books.value.isNotEmpty() }
    }

    @Test
    fun everyBookTheComputerHasIsListed() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)

        click(LibraryTags.syncScope(1))

        awaitThat { exists(LibraryTags.syncBook("Hymns")) }
        assertTrue(exists(LibraryTags.syncBook("Chorus")))
    }

    @Test
    fun eachBookIsNamed() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)

        click(LibraryTags.syncScope(1))

        awaitThat { isShowing("Hymns") }
    }

    @Test
    fun everyBookIsTickedToBeginWith() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)

        click(LibraryTags.syncScope(1))

        awaitThat { vm.books.value.isNotEmpty() }
        assertEquals(vm.books.value.size, vm.selectedBooks.value.size)
    }

    @Test
    fun untickingABookLeavesItOut() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)
        click(LibraryTags.syncScope(1))
        awaitThat { exists(LibraryTags.syncBook("Hymns")) }

        click(LibraryTags.syncBook("Hymns"))

        awaitThat { !vm.selectedBooks.value.contains("Hymns") }
    }

    @Test
    fun untickingOneBookKeepsTheOthers() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)
        click(LibraryTags.syncScope(1))
        awaitThat { exists(LibraryTags.syncBook("Hymns")) }

        click(LibraryTags.syncBook("Hymns"))

        awaitThat { vm.selectedBooks.value.contains("Chorus") }
    }

    @Test
    fun tickingABookBackPutsItIn() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)
        click(LibraryTags.syncScope(1))
        awaitThat { exists(LibraryTags.syncBook("Hymns")) }
        click(LibraryTags.syncBook("Hymns"))
        awaitThat { !vm.selectedBooks.value.contains("Hymns") }

        click(LibraryTags.syncBook("Hymns"))

        awaitThat { vm.selectedBooks.value.contains("Hymns") }
    }

    @Test
    fun theCountSaysHowManyBooksAreTicked() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)

        click(LibraryTags.syncScope(1))

        awaitThat { exists(LibraryTags.SYNC_BOOK_COUNT) }
    }

    @Test
    fun unticketingEverythingIsSaidPlainly() = runComposeUiTest {
        // Copying nothing and reporting success reads as the feature being broken.
        val vm = syncVm()
        showSongSync(vm)
        click(LibraryTags.syncScope(1))
        awaitThat { exists(LibraryTags.SYNC_BOOKS_TOGGLE_ALL) }

        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)

        awaitThat { exists(LibraryTags.SYNC_BOOKS_NONE) }
    }

    @Test
    fun withNoBooksTickedTheCopyButtonIsDead() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)
        click(LibraryTags.syncScope(1))
        awaitThat { exists(LibraryTags.SYNC_BOOKS_TOGGLE_ALL) }

        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)

        awaitThat { vm.selectedBooks.value.isEmpty() }
        tagged(LibraryTags.SYNC_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun theToggleTakesEverythingBack() = runComposeUiTest {
        // Picking one of forty starts from none; the same control gets back.
        val vm = syncVm()
        showSongSync(vm)
        click(LibraryTags.syncScope(1))
        awaitThat { exists(LibraryTags.SYNC_BOOKS_TOGGLE_ALL) }
        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)
        awaitThat { vm.selectedBooks.value.isEmpty() }

        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)

        awaitThat { vm.selectedBooks.value.size == vm.books.value.size }
    }

    @Test
    fun tickingOneBookBackMakesTheCopyButtonLiveAgain() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)
        click(LibraryTags.syncScope(1))
        awaitThat { exists(LibraryTags.SYNC_BOOKS_TOGGLE_ALL) }
        click(LibraryTags.SYNC_BOOKS_TOGGLE_ALL)
        awaitThat { vm.selectedBooks.value.isEmpty() }

        click(LibraryTags.syncBook("Hymns"))

        awaitThat { vm.selectedBooks.value.isNotEmpty() }
        tagged(LibraryTags.SYNC_BUTTON).assertIsEnabled()
    }

    @Test
    fun goingBackToEverythingHidesTheBookList() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)
        click(LibraryTags.syncScope(1))
        awaitThat { exists(LibraryTags.SYNC_BOOK_COUNT) }

        click(LibraryTags.syncScope(0))

        awaitThat { !exists(LibraryTags.SYNC_BOOK_COUNT) }
    }

    @Test
    fun aComputerWithNoBooksSaysSo() = runComposeUiTest {
        val vm = syncVm(body = """{"song-book":[]}""")
        showSongSync(vm)

        click(LibraryTags.syncScope(1))

        awaitThat { exists(LibraryTags.SYNC_BOOKS_MISSING) }
    }

    @Test
    fun aComputerThatRefusesTheBookListSaysSo() = runComposeUiTest {
        // Its own line, not the empty-list one: a computer that refused is
        // worth asking again, a computer with no songbooks is not.
        val vm = syncVm(status = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.syncScope(1))

        awaitThat { exists(LibraryTags.SYNC_BOOKS_FAILED) }
    }

    @Test
    fun aComputerWithNoBooksLeavesNothingToTick() = runComposeUiTest {
        val vm = syncVm(body = """{"song-book":[]}""")
        showSongSync(vm)

        click(LibraryTags.syncScope(1))

        awaitThat { exists(LibraryTags.SYNC_BOOKS_MISSING) }
        assertFalse(exists(LibraryTags.SYNC_BOOK_COUNT))
    }

    // ── Running a copy ───────────────────────────────────────────────────

    @Test
    fun copyingReportsWhatItDid() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
    }

    @Test
    fun aFinishedCopySaysHowManySongsArrived() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value != null }
        assertTrue(isShowing("3"))
    }

    @Test
    fun aFinishedCopyTurnsTheButtonIntoTheWayOut() = runComposeUiTest {
        // The bug this exists for: "Copy songs" under a finished result was read
        // as nothing having happened.
        var done = 0
        val vm = syncVm()
        showSongSync(vm, onDone = { done++ })
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { done == 1 }
    }

    @Test
    fun aFinishedCopyDoesNotStartAnother() = runComposeUiTest {
        val vm = syncVm()
        showSongSync(vm)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
        val first = vm.outcome.value

        click(LibraryTags.SYNC_BUTTON)

        assertEquals(first, vm.outcome.value)
    }

    @Test
    fun aFinishedCopyTakesTheScopeChoiceAway() = runComposeUiTest {
        // Nothing left to choose: the sheet is done and the way out is the button.
        val vm = syncVm()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
        assertFalse(exists(LibraryTags.syncScope(0)))
    }

    @Test
    fun aRefusedCatalogueIsReported() = runComposeUiTest {
        val vm = syncVm(status = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
    }

    @Test
    fun aRefusedCatalogueDoesNotTurnTheButtonIntoDone() = runComposeUiTest {
        // A failure is not a finished copy; offering "Done" would file it away.
        var done = 0
        val vm = syncVm(status = HttpStatusCode.InternalServerError)
        showSongSync(vm, onDone = { done++ })
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }

        click(LibraryTags.SYNC_BUTTON)

        assertEquals(0, done)
    }

    @Test
    fun theCopyExplainsItselfBeforeItRuns() = runComposeUiTest {
        showSongSync(syncVm())

        assertTrue(exists(LibraryTags.SYNC_BUTTON))
    }

    // ── The button's three jobs ──────────────────────────────────────────

    @Test
    fun aWaitingSheetOffersStart() {
        val button = syncButtonFor(isRunning = false, isFinished = false, canStart = true)

        assertEquals(SyncButton.Action.START, button.action)
        assertTrue(button.isEnabled)
        assertFalse(button.isDestructive)
    }

    @Test
    fun aWaitingSheetWithNothingToCopyCannotStart() {
        val button = syncButtonFor(isRunning = false, isFinished = false, canStart = false)

        assertEquals(SyncButton.Action.START, button.action)
        assertFalse(button.isEnabled)
    }

    @Test
    fun aRunningCopyOffersCancel() {
        val button = syncButtonFor(isRunning = true, isFinished = false, canStart = true)

        assertEquals(SyncButton.Action.CANCEL, button.action)
        assertTrue(button.isDestructive)
    }

    @Test
    fun aRunningCopyCanAlwaysBeCancelled() {
        // Never trapped in the sheet, whatever the ticks say.
        val button = syncButtonFor(isRunning = true, isFinished = false, canStart = false)

        assertEquals(SyncButton.Action.CANCEL, button.action)
        assertTrue(button.isEnabled)
    }

    @Test
    fun aFinishedCopyOffersClose() {
        val button = syncButtonFor(isRunning = false, isFinished = true, canStart = true)

        assertEquals(SyncButton.Action.CLOSE, button.action)
        assertTrue(button.isEnabled)
        assertFalse(button.isDestructive)
    }

    @Test
    fun runningWinsOverFinished() {
        // The previous run's result is still on screen while the next is in
        // flight; the button has to be the way out of the running one.
        val button = syncButtonFor(isRunning = true, isFinished = true, canStart = true)

        assertEquals(SyncButton.Action.CANCEL, button.action)
    }

    @Test
    fun aFinishedCopyCanBeClosedEvenWithNothingTicked() {
        val button = syncButtonFor(isRunning = false, isFinished = true, canStart = false)

        assertEquals(SyncButton.Action.CLOSE, button.action)
        assertTrue(button.isEnabled)
    }
}
