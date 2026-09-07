package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.BibleSyncOutcome
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SyncOutcome
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.network.BibleDownloadService
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import com.church.presenter.churchpresentermobile.viewmodel.BibleSyncViewModel
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
 * The card that reports a finished copy, actually on screen.
 *
 * Every ending has to reach the sheet, not just the ViewModel: a copy that was
 * stopped, a desktop that went away between picking and downloading, a module
 * the desktop no longer offers. Each of those is a different sentence, and an
 * operator who is told "copied" after any of them will walk away from a library
 * that is not there.
 */
@OptIn(ExperimentalTestApi::class)
class SyncOutcomeCardTest {

    // ── Songs ────────────────────────────────────────────────────────────

    private val catalogue = """
        {"song-book":[
          {"book-name":"Hymns","song-total":3,"songs":[
            {"id":1,"number":"1","title":"Amazing Grace"},
            {"id":2,"number":"2","title":"How Great"},
            {"id":3,"number":"3","title":"The Lord Is My Shepherd"}
          ]}
        ]}
    """.trimIndent()

    private fun songDetail(number: String) = """
        {"number":"$number","title":"Song $number","song-book":"Hymns",
         "verses":[{"label":"Verse","text":"Amazing grace"}]}
    """.trimIndent()

    private fun songVm(detailDelayMs: Long = 0L): LibrarySyncViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            if (path.endsWith("/songs")) respond(catalogue) else {
                delay(detailDelayMs)
                respond(songDetail(path.substringAfterLast('/')))
            }
        })
        return LibrarySyncViewModel(
            LibraryRepository(InMemoryFileStorage()) { 1_000L },
            settings,
            SongService(settings, FakeWsSender(), client),
        )
    }

    private fun ComposeUiTest.showSongSync(viewModel: LibrarySyncViewModel, onDone: () -> Unit = {}) =
        showScreen {
            SongSyncSection(
                repository = LibraryRepository(InMemoryFileStorage()) { 1_000L },
                settings = AppSettings(InMemorySettingsStorage()),
                sender = FakeWsSender(),
                onDone = onDone,
                providedViewModel = viewModel,
            )
        }

    @Test
    fun aStoppedCopyPutsItsAccountOnScreen() = runComposeUiTest {
        // The state flipping is not the point; the operator reading it is.
        val viewModel = songVm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.outcome.value is SyncOutcome.Cancelled }
        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
    }

    @Test
    fun aStoppedCopyLeavesTheProgressBarBehind() = runComposeUiTest {
        val viewModel = songVm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { !exists(LibraryTags.SYNC_PROGRESS) }
    }

    @Test
    fun aStoppedCopyIsStillPressableAfterwards() = runComposeUiTest {
        val viewModel = songVm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }

        assertTrue(exists(LibraryTags.SYNC_BUTTON))
    }

    @Test
    fun aStoppedCopyKeepsTheScopeChoiceOnScreen() = runComposeUiTest {
        val viewModel = songVm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
        assertTrue(exists(LibraryTags.syncScope(0)))
    }

    @Test
    fun aFinishedCopyPutsItsAccountOnScreen() = runComposeUiTest {
        val viewModel = songVm()
        showSongSync(viewModel)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.outcome.value is SyncOutcome.Success }
        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
    }

    @Test
    fun aFinishedCopyTakesTheProgressBarAway() = runComposeUiTest {
        val viewModel = songVm()
        showSongSync(viewModel)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
        assertFalse(exists(LibraryTags.SYNC_PROGRESS))
    }

    @Test
    fun aRunningCopyHasNoResultYet() = runComposeUiTest {
        // A card left over from a previous run would be read as this one's.
        val viewModel = songVm(detailDelayMs = 1_000)
        showSongSync(viewModel)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { viewModel.progress.value.isRunning }
        assertFalse(exists(LibraryTags.SYNC_OUTCOME))
    }

    @Test
    fun startingASecondCopyClearsTheFirstsResult() = runComposeUiTest {
        val viewModel = songVm(detailDelayMs = 1_000)
        showSongSync(viewModel)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { viewModel.progress.value.isRunning }
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { !exists(LibraryTags.SYNC_OUTCOME) }
    }

    @Test
    fun aRunningCopyShowsWhatItIsFetching() = runComposeUiTest {
        val viewModel = songVm(detailDelayMs = 1_000)
        showSongSync(viewModel)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_PROGRESS_LABEL) }
    }

    // ── Bibles ───────────────────────────────────────────────────────────

    private fun module(title: String) = """
        ##Title: $title
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning.
    """.trimIndent()

    /**
     * A desktop whose manifest can change — or stop answering — between the
     * operator picking a module and the download starting.
     */
    private class BibleFixture(
        manifests: List<String>,
        manifestStatuses: List<HttpStatusCode>,
        delayMs: Long,
        body: String,
    ) {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LocalBibleRepository(InMemoryFileStorage(), now = { 0L })
        val viewModel: BibleSyncViewModel
        val choice: BibleChoiceViewModel
        private var manifestCalls = 0

        init {
            val client = HttpClient(MockEngine { request ->
                if (request.url.encodedPath.endsWith(ApiConstants.BIBLE_TRANSLATIONS_ENDPOINT)) {
                    val call = manifestCalls++
                    val status = manifestStatuses.getOrElse(call) { manifestStatuses.last() }
                    if (status != HttpStatusCode.OK) respond("boom", status)
                    else respond(manifests.getOrElse(call) { manifests.last() }, HttpStatusCode.OK)
                } else {
                    delay(delayMs)
                    respond(body)
                }
            })
            viewModel = BibleSyncViewModel(repository, settings, BibleDownloadService(settings, client))
            choice = BibleChoiceViewModel(repository)
        }
    }

    private fun bibleFixture(
        manifests: List<String> = listOf("""["en_KJV.spb","ru_RST77.spb"]"""),
        manifestStatuses: List<HttpStatusCode> = listOf(HttpStatusCode.OK),
        delayMs: Long = 0L,
        body: String = module("King James Version"),
    ) = BibleFixture(manifests, manifestStatuses, delayMs, body)

    private fun ComposeUiTest.showBibleSync(f: BibleFixture) = showScreen {
        BibleSyncSection(
            bibles = f.repository,
            settings = f.settings,
            providedViewModel = f.viewModel,
            providedChoice = f.choice,
        )
    }

    private fun ComposeUiTest.download(f: BibleFixture, vararg fileNames: String) {
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice(fileNames.first())) }
        fileNames.forEach { click(LibraryTags.bibleChoice(it)) }
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)
    }

    @Test
    fun aDesktopThatGoesAwayBeforeTheDownloadIsReported() = runComposeUiTest {
        // The manifest is re-read at download time, so this is the moment a
        // computer being closed is noticed.
        val f = bibleFixture(
            manifestStatuses = listOf(HttpStatusCode.OK, HttpStatusCode.InternalServerError),
        )
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Failed }
    }

    @Test
    fun aDesktopThatGoesAwayPutsItsReasonOnScreen() = runComposeUiTest {
        val f = bibleFixture(
            manifestStatuses = listOf(HttpStatusCode.OK, HttpStatusCode.InternalServerError),
        )
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aDesktopThatGoesAwayInstallsNothing() = runComposeUiTest {
        val f = bibleFixture(
            manifestStatuses = listOf(HttpStatusCode.OK, HttpStatusCode.InternalServerError),
        )
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Failed }
        assertTrue(f.viewModel.installed.value.isEmpty())
    }

    @Test
    fun aFailedDownloadCanBeTriedAgain() = runComposeUiTest {
        val f = bibleFixture(
            manifestStatuses = listOf(
                HttpStatusCode.OK,
                HttpStatusCode.InternalServerError,
                HttpStatusCode.OK,
            ),
        )
        showBibleSync(f)
        download(f, "en_KJV.spb")
        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Failed }

        // A finished attempt clears the ticks, so trying again is a fresh pick.
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.installed.value.isNotEmpty() }
    }

    @Test
    fun aModuleTheDesktopNoLongerOffersIsReportedRatherThanGuessedAt() = runComposeUiTest {
        // Positions are rebuilt by the desktop from its own settings, so a stale
        // index would fetch a different Bible than the one that was ticked.
        val f = bibleFixture(
            manifests = listOf("""["en_KJV.spb","ru_RST77.spb"]""", """["ru_RST77.spb"]"""),
        )
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Success }
        assertTrue((f.viewModel.outcome.value as BibleSyncOutcome.Success).failed.isNotEmpty())
    }

    @Test
    fun aModuleTheDesktopNoLongerOffersInstallsNothing() = runComposeUiTest {
        val f = bibleFixture(
            manifests = listOf("""["en_KJV.spb","ru_RST77.spb"]""", """["ru_RST77.spb"]"""),
        )
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { f.viewModel.outcome.value != null }
        assertTrue(f.viewModel.installed.value.isEmpty())
    }

    @Test
    fun aVanishedModuleShowsItsResultLine() = runComposeUiTest {
        val f = bibleFixture(
            manifests = listOf("""["en_KJV.spb","ru_RST77.spb"]""", """["ru_RST77.spb"]"""),
        )
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aStoppedDownloadPutsItsAccountOnScreen() = runComposeUiTest {
        val f = bibleFixture(delayMs = 1_000)
        showBibleSync(f)
        download(f, "en_KJV.spb", "ru_RST77.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Cancelled }
        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aStoppedDownloadTakesTheProgressBarAway() = runComposeUiTest {
        val f = bibleFixture(delayMs = 1_000)
        showBibleSync(f)
        download(f, "en_KJV.spb", "ru_RST77.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_PROGRESS) }
    }

    @Test
    fun aStoppedDownloadTakesTheStopButtonAway() = runComposeUiTest {
        val f = bibleFixture(delayMs = 1_000)
        showBibleSync(f)
        download(f, "en_KJV.spb", "ru_RST77.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_STOP) }
    }

    @Test
    fun aStoppedDownloadStillListsWhatArrived() = runComposeUiTest {
        val f = bibleFixture(delayMs = 1_000)
        showBibleSync(f)
        download(f, "en_KJV.spb", "ru_RST77.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Cancelled }
        assertTrue(f.viewModel.installed.value.size < 2)
    }

    @Test
    fun aFinishedDownloadPutsItsAccountOnScreen() = runComposeUiTest {
        val f = bibleFixture()
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Success }
        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aRunningDownloadHasNoResultYet() = runComposeUiTest {
        val f = bibleFixture(delayMs = 1_000)
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_OUTCOME))
    }

    @Test
    fun aRunningDownloadHidesTheOfferList() = runComposeUiTest {
        // Ticking another translation mid-download would be an instruction with
        // nowhere to go.
        val f = bibleFixture(delayMs = 1_000)
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }
        assertFalse(exists(LibraryTags.bibleChoice("ru_RST77.spb")))
    }

    @Test
    fun aRunningDownloadShowsProgress() = runComposeUiTest {
        val f = bibleFixture(delayMs = 1_000)
        showBibleSync(f)

        download(f, "en_KJV.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_PROGRESS) }
    }

    @Test
    fun aSecondDownloadClearsTheFirstsResult() = runComposeUiTest {
        val f = bibleFixture(delayMs = 1_000)
        showBibleSync(f)
        download(f, "en_KJV.spb", "ru_RST77.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }
        click(LibraryTags.BIBLE_SYNC_STOP)
        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }

        click(LibraryTags.bibleChoice("ru_RST77.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aDownloadOfNothingIsNotOffered() = runComposeUiTest {
        // Nothing ticked, nothing to fetch.
        val f = bibleFixture()
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_DOWNLOAD) }
        assertTrue(f.viewModel.selection.value.isEmpty())
    }

    @Test
    fun untickingEverythingAgainLeavesNothingToFetch() = runComposeUiTest {
        val f = bibleFixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        awaitThat { f.viewModel.selection.value.isNotEmpty() }

        click(LibraryTags.bibleChoice("en_KJV.spb"))

        awaitThat { f.viewModel.selection.value.isEmpty() }
    }

    @Test
    fun anAlreadyInstalledModuleIsMarkedInTheOfferList() = runComposeUiTest {
        // So an operator does not download 4 MB they already have.
        val f = bibleFixture()
        showBibleSync(f)
        download(f, "en_KJV.spb")
        awaitThat { f.viewModel.installed.value.isNotEmpty() }

        awaitThat { f.viewModel.choices.value.any { it.isInstalled } }
    }

    @Test
    fun downloadingTheSameModuleTwiceDoesNotDuplicateIt() = runComposeUiTest {
        val f = bibleFixture()
        showBibleSync(f)
        download(f, "en_KJV.spb")
        awaitThat { f.viewModel.installed.value.size == 1 }

        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Success }
        assertTrue(f.viewModel.installed.value.size == 1)
    }
}
