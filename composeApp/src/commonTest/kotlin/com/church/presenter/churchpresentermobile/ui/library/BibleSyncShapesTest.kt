package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.BibleSyncOutcome
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.network.BibleDownloadService
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import com.church.presenter.churchpresentermobile.viewmodel.BibleSyncViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Bible half's awkward endings, and taking a translation off the phone.
 *
 * "Copied" over an empty list is the worst of these — it sends an operator to a
 * Bible tab that is still empty on a Sunday morning — so a download where
 * nothing arrived, one where some arrived, one that never started and one that
 * was stopped all have to read differently. Removal is the other side of the
 * same care: a 4 MB module is slow to fetch again, so it asks first.
 */
@OptIn(ExperimentalTestApi::class)
class BibleSyncShapesTest {

    private fun module(title: String) = """
        ##Title: $title
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning.
    """.trimIndent()

    /**
     * A desktop offering two modules, either of which can be made to refuse.
     *
     * [failIndexes] names positions in the manifest, which is how the download
     * endpoint addresses them.
     */
    private class Fixture(
        manifest: String,
        bodyFor: (Int) -> String?,
        manifestStatus: HttpStatusCode,
        delayMs: Long,
    ) {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LocalBibleRepository(InMemoryFileStorage(), now = { 0L })
        val viewModel: BibleSyncViewModel
        val choice: BibleChoiceViewModel

        init {
            val client = HttpClient(MockEngine { request ->
                val path = request.url.encodedPath
                if (path.endsWith(ApiConstants.BIBLE_TRANSLATIONS_ENDPOINT)) {
                    if (manifestStatus != HttpStatusCode.OK) respond("boom", manifestStatus)
                    else respond(manifest, HttpStatusCode.OK)
                } else {
                    delay(delayMs)
                    val index = path.substringAfterLast('/').toIntOrNull() ?: 0
                    bodyFor(index)?.let { respond(it) }
                        ?: respond("boom", HttpStatusCode.InternalServerError)
                }
            })
            viewModel = BibleSyncViewModel(repository, settings, BibleDownloadService(settings, client))
            choice = BibleChoiceViewModel(repository)
        }
    }

    private fun fixture(
        manifest: String = """["en_KJV.spb","ru_RST77.spb"]""",
        bodyFor: (Int) -> String? = { module(if (it == 0) "King James Version" else "Synodal") },
        manifestStatus: HttpStatusCode = HttpStatusCode.OK,
        delayMs: Long = 0L,
    ) = Fixture(manifest, bodyFor, manifestStatus, delayMs)

    private fun ComposeUiTest.showBibleSync(f: Fixture) = showScreen {
        BibleSyncSection(
            bibles = f.repository,
            settings = f.settings,
            providedViewModel = f.viewModel,
            providedChoice = f.choice,
        )
    }

    /** Finds the offers, ticks the named ones, and starts the download. */
    private fun ComposeUiTest.download(vararg fileNames: String) {
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice(fileNames.first())) }
        fileNames.forEach { click(LibraryTags.bibleChoice(it)) }
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)
    }

    // ── Nothing arrived ──────────────────────────────────────────────────

    @Test
    fun aDownloadWhereNothingArrivedIsNotReportedAsDone() = runComposeUiTest {
        val f = fixture(bodyFor = { null })
        showBibleSync(f)

        download("en_KJV.spb")

        awaitThat { f.viewModel.outcome.value != null }
        val outcome = f.viewModel.outcome.value
        assertTrue(outcome !is BibleSyncOutcome.Success || outcome.installed.isEmpty())
    }

    @Test
    fun aDownloadWhereNothingArrivedStillShowsAResultLine() = runComposeUiTest {
        val f = fixture(bodyFor = { null })
        showBibleSync(f)

        download("en_KJV.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aDownloadWhereNothingArrivedInstallsNothing() = runComposeUiTest {
        val f = fixture(bodyFor = { null })
        showBibleSync(f)

        download("en_KJV.spb")

        awaitThat { f.viewModel.outcome.value != null }
        assertTrue(f.viewModel.installed.value.isEmpty())
    }

    @Test
    fun aDownloadWhereNothingArrivedLeavesNoInstalledHeading() = runComposeUiTest {
        val f = fixture(bodyFor = { null })
        showBibleSync(f)

        download("en_KJV.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_INSTALLED))
    }

    @Test
    fun aModuleWithNoVersesCountsAsNothingArriving() = runComposeUiTest {
        // A file that parsed to nothing is not a Bible, however well it downloaded.
        val f = fixture(bodyFor = { "##Title: Empty\n-----\n" })
        showBibleSync(f)

        download("en_KJV.spb")

        awaitThat { f.viewModel.outcome.value != null }
        assertTrue(f.viewModel.installed.value.isEmpty())
    }

    // ── Some arrived, some did not ───────────────────────────────────────

    @Test
    fun aPartialDownloadKeepsWhatArrived() = runComposeUiTest {
        val f = fixture(bodyFor = { if (it == 0) module("King James Version") else null })
        showBibleSync(f)

        download("en_KJV.spb", "ru_RST77.spb")

        awaitThat { f.viewModel.installed.value.size == 1 }
    }

    @Test
    fun aPartialDownloadNamesWhatArrived() = runComposeUiTest {
        val f = fixture(bodyFor = { if (it == 0) module("King James Version") else null })
        showBibleSync(f)

        download("en_KJV.spb", "ru_RST77.spb")

        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        assertEquals("King James Version", f.viewModel.installed.value.first().title)
    }

    @Test
    fun aPartialDownloadReportsTheFailureToo() = runComposeUiTest {
        // Reporting plain success over a missing translation is the failure this
        // line exists to prevent.
        val f = fixture(bodyFor = { if (it == 0) module("King James Version") else null })
        showBibleSync(f)

        download("en_KJV.spb", "ru_RST77.spb")

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Success }
        assertTrue((f.viewModel.outcome.value as BibleSyncOutcome.Success).failed.isNotEmpty())
    }

    @Test
    fun aPartialDownloadShowsItsResultLine() = runComposeUiTest {
        val f = fixture(bodyFor = { if (it == 0) module("King James Version") else null })
        showBibleSync(f)

        download("en_KJV.spb", "ru_RST77.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aPartialDownloadStillListsWhatIsOnThePhone() = runComposeUiTest {
        val f = fixture(bodyFor = { if (it == 0) module("King James Version") else null })
        showBibleSync(f)

        download("en_KJV.spb", "ru_RST77.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_INSTALLED) }
    }

    // ── It never started ─────────────────────────────────────────────────

    @Test
    fun aDesktopThatWillNotListSaysWhy() = runComposeUiTest {
        val f = fixture(manifestStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_LOAD_ERROR) }
    }

    @Test
    fun aDesktopThatWillNotListOffersToTryAgain() = runComposeUiTest {
        val f = fixture(manifestStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_LOAD_ERROR) }
        assertTrue(exists(LibraryTags.BIBLE_SYNC_FIND))
    }

    @Test
    fun aDesktopThatWillNotListInstallsNothing() = runComposeUiTest {
        val f = fixture(manifestStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { f.viewModel.loadError.value != null }
        assertTrue(f.viewModel.installed.value.isEmpty())
    }

    @Test
    fun aSecondAttemptAfterAListingFailureIsAllowed() = runComposeUiTest {
        val f = fixture(manifestStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { f.viewModel.loadError.value != null }

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { f.viewModel.loadError.value != null }
    }

    // ── It was stopped ───────────────────────────────────────────────────

    @Test
    fun aRunningDownloadOffersTheWayToStopIt() = runComposeUiTest {
        val f = fixture(delayMs = 1_000)
        showBibleSync(f)

        download("en_KJV.spb")

        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }
    }

    @Test
    fun stoppingADownloadEndsIt() = runComposeUiTest {
        val f = fixture(delayMs = 1_000)
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { !f.viewModel.progress.value.isRunning }
    }

    @Test
    fun aStoppedDownloadSaysItWasStopped() = runComposeUiTest {
        // Two modules, stopped during the first: the module already on the wire
        // cannot be abandoned, so what stopping means is that the rest is not
        // fetched — and the sheet has to say so rather than report "copied".
        val f = fixture(delayMs = 1_000)
        showBibleSync(f)
        download("en_KJV.spb", "ru_RST77.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Cancelled }
    }

    @Test
    fun aStoppedDownloadShowsItsResultLine() = runComposeUiTest {
        val f = fixture(delayMs = 1_000)
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aStoppedDownloadSkipsTheRest() = runComposeUiTest {
        val f = fixture(delayMs = 1_000)
        showBibleSync(f)
        download("en_KJV.spb", "ru_RST77.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Cancelled }
        assertTrue(f.viewModel.installed.value.size < 2)
    }

    @Test
    fun aStoppedDownloadPutsTheOffersBack() = runComposeUiTest {
        val f = fixture(delayMs = 1_000)
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }

        click(LibraryTags.BIBLE_SYNC_STOP)

        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
    }

    @Test
    fun aStoppedDownloadCanBeStartedAgain() = runComposeUiTest {
        val f = fixture(delayMs = 1_000)
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { exists(LibraryTags.BIBLE_SYNC_STOP) }
        click(LibraryTags.BIBLE_SYNC_STOP)
        awaitThat { exists(LibraryTags.BIBLE_SYNC_DOWNLOAD) }

        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.progress.value.isRunning || f.viewModel.outcome.value != null }
    }

    // ── Taking one off the phone ─────────────────────────────────────────

    @Test
    fun removingATranslationAsksFirst() = runComposeUiTest {
        // A 4 MB module is slow to fetch again over church Wi-Fi.
        val f = fixture()
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { f.viewModel.installed.value.isNotEmpty() }

        click(LibraryTags.bibleRemove(f.viewModel.installed.value.first().id))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }
    }

    @Test
    fun askingIsNotRemoving() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { f.viewModel.installed.value.isNotEmpty() }

        click(LibraryTags.bibleRemove(f.viewModel.installed.value.first().id))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }
        assertTrue(f.viewModel.installed.value.isNotEmpty())
    }

    @Test
    fun confirmingRemovesTheTranslation() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        click(LibraryTags.bibleRemove(f.viewModel.installed.value.first().id))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }

        click(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM)

        awaitThat { f.viewModel.installed.value.isEmpty() }
    }

    @Test
    fun confirmingClosesTheQuestion() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        click(LibraryTags.bibleRemove(f.viewModel.installed.value.first().id))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }

        click(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }
    }

    @Test
    fun removingTheLastTranslationLeavesNoInstalledHeading() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        click(LibraryTags.bibleRemove(f.viewModel.installed.value.first().id))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }

        click(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_INSTALLED) }
    }

    @Test
    fun backingOutOfARemovalKeepsTheTranslation() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        click(LibraryTags.bibleRemove(f.viewModel.installed.value.first().id))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_DISMISS) }

        click(LibraryTags.BIBLE_SYNC_REMOVE_DISMISS)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }
        assertTrue(f.viewModel.installed.value.isNotEmpty())
    }

    @Test
    fun backingOutTwiceIsStillHarmless() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        download("en_KJV.spb")
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        val id = f.viewModel.installed.value.first().id
        click(LibraryTags.bibleRemove(id))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_DISMISS) }
        click(LibraryTags.BIBLE_SYNC_REMOVE_DISMISS)
        awaitThat { !exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }

        click(LibraryTags.bibleRemove(id))
        click(LibraryTags.BIBLE_SYNC_REMOVE_DISMISS)

        awaitThat { f.viewModel.installed.value.isNotEmpty() }
    }

    @Test
    fun aSecondTranslationBringsTheChoiceHint() = runComposeUiTest {
        // With one there is nothing to choose between; with two there is.
        val f = fixture()
        showBibleSync(f)

        download("en_KJV.spb", "ru_RST77.spb")

        awaitThat { f.viewModel.installed.value.size == 2 }
        assertTrue(exists(LibraryTags.BIBLE_SYNC_CHOOSE_HINT))
    }

    @Test
    fun oneTranslationNeedsNoChoiceHint() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        download("en_KJV.spb")

        awaitThat { f.viewModel.installed.value.size == 1 }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_CHOOSE_HINT))
    }

    @Test
    fun pickingAnInstalledTranslationMakesItTheActiveOne() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        download("en_KJV.spb", "ru_RST77.spb")
        awaitThat { f.viewModel.installed.value.size == 2 }
        val second = f.viewModel.installed.value[1].id

        click(LibraryTags.bibleInstalled(second))

        awaitThat { f.choice.activeId.value == second }
    }

    @Test
    fun removingTheActiveTranslationLeavesTheOther() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        download("en_KJV.spb", "ru_RST77.spb")
        awaitThat { f.viewModel.installed.value.size == 2 }
        val first = f.viewModel.installed.value[0].id
        click(LibraryTags.bibleInstalled(first))
        awaitThat { f.choice.activeId.value == first }

        click(LibraryTags.bibleRemove(first))
        click(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM)

        awaitThat { f.viewModel.installed.value.size == 1 }
    }
}
