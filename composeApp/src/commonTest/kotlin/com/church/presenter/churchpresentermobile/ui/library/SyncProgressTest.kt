package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
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
 * What the copy sheet shows while a copy is actually running.
 *
 * A long copy with no sign of movement reads as a hung app, and the first
 * moments are the worst: before the catalogue answers there is no total, so
 * "Copying 0 of 0…" at 0% would be the least reassuring thing the sheet could
 * say. The button changes meaning too — while a copy runs it is the way to stop
 * it, and it must be pressable whatever the ticks say.
 */
@OptIn(ExperimentalTestApi::class)
class SyncProgressTest {

    private val catalogue = """
        {"song-book":[
          {"book-name":"Hymns","song-total":2,"songs":[
            {"id":1,"number":"1","title":"Amazing Grace"},
            {"id":2,"number":"2","title":"How Great"}
          ]}
        ]}
    """.trimIndent()

    private val songDetail = """
        {"number":"1","title":"Amazing Grace","song-book":"Hymns",
         "verses":[{"label":"Verse","text":"Amazing grace"}]}
    """.trimIndent()

    /** A desktop that answers slowly, so the sheet can be seen mid-copy. */
    private fun slowSongVm(delayMs: Long): LibrarySyncViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        val client = HttpClient(MockEngine { request ->
            delay(delayMs)
            if (request.url.encodedPath.endsWith("/songs")) respond(catalogue) else respond(songDetail)
        })
        return LibrarySyncViewModel(
            LibraryRepository(InMemoryFileStorage()) { 1_000L },
            settings,
            SongService(settings, FakeWsSender(), client),
        )
    }

    private fun ComposeUiTest.showSongSync(vm: LibrarySyncViewModel, onDone: () -> Unit = {}) =
        showScreen {
            SongSyncSection(
                repository = LibraryRepository(InMemoryFileStorage()) { 1_000L },
                settings = AppSettings(InMemorySettingsStorage()),
                sender = FakeWsSender(),
                onDone = onDone,
                providedViewModel = vm,
            )
        }

    private class BibleFixture(delayMs: Long) {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LocalBibleRepository(InMemoryFileStorage(), now = { 0L })
        val viewModel: BibleSyncViewModel
        val choice = BibleChoiceViewModel(repository)

        init {
            val body = """
                ##Title: King James Version
                1 Genesis 50
                -----
                B001C001V001 1 1 1 In the beginning.
            """.trimIndent()
            val client = HttpClient(MockEngine { request ->
                if (request.url.encodedPath.endsWith(ApiConstants.BIBLE_TRANSLATIONS_ENDPOINT)) {
                    respond("""["en_KJV.spb"]""", HttpStatusCode.OK)
                } else {
                    delay(delayMs)
                    respond(body, HttpStatusCode.OK)
                }
            })
            viewModel = BibleSyncViewModel(repository, settings, BibleDownloadService(settings, client))
        }
    }

    private fun ComposeUiTest.showBibleSync(f: BibleFixture) = showScreen {
        BibleSyncSection(
            bibles = f.repository,
            settings = f.settings,
            providedViewModel = f.viewModel,
            providedChoice = f.choice,
        )
    }

    private fun ComposeUiTest.startBibleDownload(f: BibleFixture) {
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)
    }

    // ── While songs are copying ──────────────────────────────────────────

    @Test
    fun aRunningCopyShowsProgress() = runComposeUiTest {
        val vm = slowSongVm(1_500)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_PROGRESS) }
    }

    @Test
    fun aRunningCopySaysWhereItHasGot() = runComposeUiTest {
        val vm = slowSongVm(1_500)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_PROGRESS_LABEL) }
    }

    @Test
    fun aRunningCopyPutsTheScopeChoiceAway() = runComposeUiTest {
        // Nothing left to choose once it has started.
        val vm = slowSongVm(1_500)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { !exists(LibraryTags.syncScope(0)) }
    }

    @Test
    fun aRunningCopyKeepsItsButtonPressable() = runComposeUiTest {
        // It is the way to stop; an operator must never be trapped in the sheet.
        val vm = slowSongVm(1_500)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.progress.value.isRunning }
        tagged(LibraryTags.SYNC_BUTTON).assertIsEnabled()
    }

    @Test
    fun aRunningCopyShowsNoResultYet() = runComposeUiTest {
        val vm = slowSongVm(1_500)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.progress.value.isRunning }
        assertFalse(exists(LibraryTags.SYNC_OUTCOME))
    }

    @Test
    fun stoppingACopyEndsIt() = runComposeUiTest {
        val vm = slowSongVm(1_500)
        showSongSync(vm)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { vm.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { !vm.progress.value.isRunning }
    }

    @Test
    fun stoppingACopyReportsWhatItManaged() = runComposeUiTest {
        val vm = slowSongVm(1_500)
        showSongSync(vm)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { vm.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { vm.outcome.value != null }
    }

    @Test
    fun stoppingACopyLeavesAResultLine() = runComposeUiTest {
        val vm = slowSongVm(1_500)
        showSongSync(vm)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { vm.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
    }

    @Test
    fun stoppingACopyTakesTheProgressBarAway() = runComposeUiTest {
        val vm = slowSongVm(1_500)
        showSongSync(vm)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { vm.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { !exists(LibraryTags.SYNC_PROGRESS) }
    }

    @Test
    fun aStoppedCopyBringsTheScopeChoiceBack() = runComposeUiTest {
        // It did not finish, so what to take is a live question again.
        val vm = slowSongVm(1_500)
        showSongSync(vm)
        click(LibraryTags.SYNC_BUTTON)
        awaitThat { vm.progress.value.isRunning }

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.syncScope(0)) }
    }

    @Test
    fun aFinishedCopyLeavesNoProgressLabel() = runComposeUiTest {
        val vm = slowSongVm(0)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_OUTCOME) }
        assertFalse(exists(LibraryTags.SYNC_PROGRESS_LABEL))
    }

    @Test
    fun nothingIsRunningBeforeTheButtonIsPressed() = runComposeUiTest {
        val vm = slowSongVm(1_500)
        showSongSync(vm)

        assertFalse(vm.progress.value.isRunning)
        assertFalse(exists(LibraryTags.SYNC_PROGRESS))
    }

    @Test
    fun aRunningCopyStillOffersItsButton() = runComposeUiTest {
        val vm = slowSongVm(1_500)
        showSongSync(vm)

        click(LibraryTags.SYNC_BUTTON)

        awaitThat { exists(LibraryTags.SYNC_PROGRESS) }
        assertTrue(exists(LibraryTags.SYNC_BUTTON))
    }

    // ── While a Bible is downloading ─────────────────────────────────────

    @Test
    fun aRunningDownloadShowsProgress() = runComposeUiTest {
        val f = BibleFixture(1_500)
        showBibleSync(f)

        startBibleDownload(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_PROGRESS) }
    }

    @Test
    fun aRunningDownloadOffersAStop() = runComposeUiTest {
        val f = BibleFixture(1_500)
        showBibleSync(f)

        startBibleDownload(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }
    }

    @Test
    fun aRunningDownloadPutsThePickListAway() = runComposeUiTest {
        val f = BibleFixture(1_500)
        showBibleSync(f)

        startBibleDownload(f)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_DOWNLOAD) }
    }

    @Test
    fun stoppingADownloadEndsIt() = runComposeUiTest {
        val f = BibleFixture(1_500)
        showBibleSync(f)
        startBibleDownload(f)
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { !f.viewModel.progress.value.isRunning }
    }

    @Test
    fun stoppingADownloadReportsWhatItManaged() = runComposeUiTest {
        val f = BibleFixture(1_500)
        showBibleSync(f)
        startBibleDownload(f)
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { f.viewModel.outcome.value != null }
    }

    @Test
    fun stoppingADownloadBringsThePickListBack() = runComposeUiTest {
        val f = BibleFixture(1_500)
        showBibleSync(f)
        startBibleDownload(f)
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_DOWNLOAD) }
    }

    @Test
    fun aStoppedDownloadTakesTheProgressBarAway() = runComposeUiTest {
        val f = BibleFixture(1_500)
        showBibleSync(f)
        startBibleDownload(f)
        awaitThat { exists(LibraryTags.BIBLE_SYNC_PROGRESS) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_PROGRESS) }
    }

    @Test
    fun nothingDownloadsBeforeTheButtonIsPressed() = runComposeUiTest {
        val f = BibleFixture(1_500)
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        assertFalse(f.viewModel.progress.value.isRunning)
        assertFalse(exists(LibraryTags.BIBLE_SYNC_PROGRESS))
    }

    @Test
    fun aStoppedDownloadCanBeStartedAgain() = runComposeUiTest {
        val f = BibleFixture(300)
        showBibleSync(f)
        startBibleDownload(f)
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }
        click(LibraryTags.BIBLE_SYNC_STOP)
        awaitThat { exists(LibraryTags.BIBLE_SYNC_DOWNLOAD) }

        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.progress.value.isRunning || f.viewModel.outcome.value != null }
    }

    @Test
    fun aRunningDownloadShowsNoResultYet() = runComposeUiTest {
        val f = BibleFixture(1_500)
        showBibleSync(f)

        startBibleDownload(f)

        awaitThat { f.viewModel.progress.value.isRunning }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_OUTCOME))
    }

    @Test
    fun aFinishedDownloadTakesTheStopAway() = runComposeUiTest {
        val f = BibleFixture(0)
        showBibleSync(f)

        startBibleDownload(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_STOP))
    }
}
