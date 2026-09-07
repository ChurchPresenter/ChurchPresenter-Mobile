package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ContentOrigin
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What the copy sheet says once a copy has finished.
 *
 * The result line is the only account the operator gets of a job that touched
 * their library, and the three things it can say are not interchangeable: how
 * many songs arrived, how many failed, and — the one the merge rules exist for
 * — how many of their own edits were kept rather than overwritten. A copy that
 * quietly reports plain success while having dropped songs is the failure this
 * covers.
 */
@OptIn(ExperimentalTestApi::class)
class SongSyncOutcomeTest {

    private val catalogue = """
        {"song-book":[
          {"book-name":"Hymns","song-total":2,"songs":[
            {"id":1,"number":"1","title":"Amazing Grace"},
            {"id":2,"number":"2","title":"How Great"}
          ]}
        ]}
    """.trimIndent()

    /** The desktop answers for the song that was asked for, as a real one does. */
    private fun detail(number: String) = """
        {"number":"$number","title":"${if (number == "1") "Amazing Grace" else "How Great"}",
         "song-book":"Hymns",
         "verses":[{"label":"Verse","text":"Amazing grace, how sweet the sound"}]}
    """.trimIndent()

    /**
     * A desktop whose catalogue always answers, and whose per-song detail can be
     * made to fail — which is how a copy ends up half-done.
     */
    private fun fixture(
        detailStatus: HttpStatusCode = HttpStatusCode.OK,
        catalogueStatus: HttpStatusCode = HttpStatusCode.OK,
        repository: LibraryRepository = LibraryRepository(InMemoryFileStorage()) { 1_000L },
    ): Pair<LibrarySyncViewModel, LibraryRepository> {
        val settings = AppSettings(InMemorySettingsStorage())
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/songs") ->
                    if (catalogueStatus != HttpStatusCode.OK) respond("boom", catalogueStatus)
                    else respond(catalogue)
                else ->
                    if (detailStatus != HttpStatusCode.OK) respond("boom", detailStatus)
                    else respond(detail(path.substringAfterLast('/')))
            }
        })
        val service = SongService(settings, FakeWsSender(), client)
        return LibrarySyncViewModel(repository, settings, service) to repository
    }

    private fun ComposeUiTest.showSongSync(
        vm: LibrarySyncViewModel,
        onDone: () -> Unit = {},
    ) = showScreen {
        SongSyncSection(
            repository = LibraryRepository(InMemoryFileStorage()) { 1_000L },
            settings = AppSettings(InMemorySettingsStorage()),
            sender = FakeWsSender(),
            onDone = onDone,
            providedViewModel = vm,
        )
    }

    // ── A copy that worked ───────────────────────────────────────────────

    @Test
    fun aFinishedCopyReportsSuccess() = runComposeUiTest {
        val (vm, _) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
    }

    @Test
    fun aFinishedCopyShowsItsResultLine() = runComposeUiTest {
        val (vm, _) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
    }

    @Test
    fun aFinishedCopyCountsTheSongsThatArrived() = runComposeUiTest {
        val (vm, _) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
        assertEquals(2, (vm.outcome.value as SyncOutcome.Success).songCount)
    }

    @Test
    fun aFinishedCopyPutsTheSongsInTheLibrary() = runComposeUiTest {
        val (vm, repo) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { repo.library.value.songs.size == 2 }
    }

    @Test
    fun aCleanCopyReportsNoFailures() = runComposeUiTest {
        val (vm, _) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
        assertEquals(0, (vm.outcome.value as SyncOutcome.Success).failedCount)
    }

    @Test
    fun aFinishedCopyTurnsTheButtonIntoTheWayOut() = runComposeUiTest {
        var done = 0
        val (vm, _) = fixture()
        showSongSync(vm, onDone = { done++ })
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { done == 1 }
    }

    @Test
    fun aFinishedCopyRemembersWhenItHappened() = runComposeUiTest {
        // The sync chip reads this back to say "synced 3 days ago".
        val (vm, _) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
        assertTrue(vm.state.value.hasEverSynced)
    }

    // ── A copy that half worked ──────────────────────────────────────────

    @Test
    fun aCopyWhoseSongsFailedSaysHowMany() = runComposeUiTest {
        // Reporting plain success while having dropped songs is the failure
        // this line exists to prevent.
        val (vm, _) = fixture(detailStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
        assertTrue((vm.outcome.value as SyncOutcome.Success).failedCount > 0)
    }

    @Test
    fun aCopyWhoseSongsFailedStillReportsAResult() = runComposeUiTest {
        val (vm, _) = fixture(detailStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
    }

    @Test
    fun aCopyWhoseSongsFailedCopiesNoneOfThem() = runComposeUiTest {
        val (vm, repo) = fixture(detailStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
        assertTrue(repo.library.value.songs.isEmpty())
    }

    @Test
    fun aHalfDoneCopyStillOffersTheWayOut() = runComposeUiTest {
        var done = 0
        val (vm, _) = fixture(detailStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm, onDone = { done++ })
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { done == 1 }
    }

    // ── A copy that could not start ──────────────────────────────────────

    @Test
    fun aRefusedCatalogueIsReportedAsAFailure() = runComposeUiTest {
        val (vm, _) = fixture(catalogueStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Failed }
    }

    @Test
    fun aRefusedCatalogueSaysWhy() = runComposeUiTest {
        val (vm, _) = fixture(catalogueStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Failed }
        assertTrue((vm.outcome.value as SyncOutcome.Failed).message.isNotBlank())
    }

    @Test
    fun aRefusedCatalogueCopiesNothing() = runComposeUiTest {
        val (vm, repo) = fixture(catalogueStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Failed }
        assertTrue(repo.library.value.songs.isEmpty())
    }

    @Test
    fun aFailureDoesNotCountAsHavingSynced() = runComposeUiTest {
        // Otherwise the chip would say "synced just now" after a copy that
        // never happened.
        val (vm, _) = fixture(catalogueStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Failed }
        assertFalse(vm.state.value.hasEverSynced)
    }

    @Test
    fun aFailureLeavesTheButtonAsAStart() = runComposeUiTest {
        // Not "Done": the copy did not happen, and trying again is the sensible
        // next act.
        var done = 0
        val (vm, _) = fixture(catalogueStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm, onDone = { done++ })
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { vm.outcome.value is SyncOutcome.Failed }

        click(LibraryTags.SYNC_BUTTON)

        assertEquals(0, done)
    }

    @Test
    fun aFailedCopyCanBeRetried() = runComposeUiTest {
        val (vm, _) = fixture(catalogueStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { vm.outcome.value is SyncOutcome.Failed }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Failed }
    }

    // ── The operator's own edits ─────────────────────────────────────────

    @Test
    fun aCopyKeepsAnEditedSongRatherThanReplacingIt() = runComposeUiTest {
        // The whole reason the merge rules exist.
        val repo = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        repo.upsertSong(
            amazingGrace().copy(
                id = "own",
                number = "1",
                bookName = "Hymns",
                title = "My own words",
                origin = ContentOrigin.LOCAL_OVERRIDE,
            )
        )
        val (vm, _) = fixture(repository = repo)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
        assertTrue(repo.library.value.songs.any { it.title == "My own words" })
    }

    @Test
    fun aCopyReportsHowManyEditsItKept() = runComposeUiTest {
        val repo = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        repo.upsertSong(
            amazingGrace().copy(
                id = "own",
                number = "1",
                bookName = "Hymns",
                title = "My own words",
                origin = ContentOrigin.LOCAL_OVERRIDE,
            )
        )
        val (vm, _) = fixture(repository = repo)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
        assertTrue((vm.outcome.value as SyncOutcome.Success).keptLocal > 0)
    }

    @Test
    fun aCopyStillBringsTheSongsThatWereNotEdited() = runComposeUiTest {
        val repo = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        repo.upsertSong(
            amazingGrace().copy(
                id = "own",
                number = "1",
                bookName = "Hymns",
                title = "My own words",
                origin = ContentOrigin.LOCAL_OVERRIDE,
            )
        )
        val (vm, _) = fixture(repository = repo)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
        assertTrue(repo.library.value.songs.size >= 2)
    }

    @Test
    fun aPlainCopyReportsNoKeptEdits() = runComposeUiTest {
        val (vm, _) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Success }
        assertEquals(0, (vm.outcome.value as SyncOutcome.Success).keptLocal)
    }

    @Test
    fun anOutcomeIsAlwaysOneOfTheThreeShapes() = runComposeUiTest {
        val (vm, _) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value != null }
        assertIs<SyncOutcome>(vm.outcome.value)
    }

    // ── While it runs ────────────────────────────────────────────────────

    @Test
    fun aFinishedCopyLeavesNoProgressBar() = runComposeUiTest {
        val (vm, _) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
        assertFalse(exists(LibraryTags.SYNC_PROGRESS))
    }

    @Test
    fun aFinishedCopyTakesTheScopeChoiceAway() = runComposeUiTest {
        val (vm, _) = fixture()
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
        assertFalse(exists(LibraryTags.syncScope(0)))
    }

    @Test
    fun aFailedCopyKeepsTheScopeChoice() = runComposeUiTest {
        // It has not finished; the operator may want to narrow it and try again.
        val (vm, _) = fixture(catalogueStatus = HttpStatusCode.InternalServerError)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value is SyncOutcome.Failed }
        assertTrue(exists(LibraryTags.syncScope(0)))
    }
}
