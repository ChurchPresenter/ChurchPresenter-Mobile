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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the Bible half says once a download has finished.
 *
 * Three shapes, and the differences matter on a Sunday morning: everything
 * arrived, some arrived and some did not, or nothing arrived at all. The last
 * one is easy to render as if it were the first — "copied" with an empty list —
 * which would send the operator to a Bible tab that is still empty.
 */
@OptIn(ExperimentalTestApi::class)
class BibleSyncOutcomeTest {

    private val module = """
        ##Title: King James Version
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning.
    """.trimIndent()

    private class Fixture(
        manifest: String,
        moduleBody: String,
        moduleStatus: HttpStatusCode,
    ) {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LocalBibleRepository(InMemoryFileStorage(), now = { 0L })
        val viewModel: BibleSyncViewModel
        val choice: BibleChoiceViewModel

        init {
            val client = HttpClient(MockEngine { request ->
                if (request.url.encodedPath.endsWith(ApiConstants.BIBLE_TRANSLATIONS_ENDPOINT)) {
                    respond(manifest, HttpStatusCode.OK)
                } else {
                    if (moduleStatus != HttpStatusCode.OK) respond("boom", moduleStatus)
                    else respond(moduleBody)
                }
            })
            viewModel = BibleSyncViewModel(repository, settings, BibleDownloadService(settings, client))
            choice = BibleChoiceViewModel(repository)
        }
    }

    private fun fixture(
        manifest: String = """["en_KJV.spb","ru_RST77.spb"]""",
        moduleBody: String = module,
        moduleStatus: HttpStatusCode = HttpStatusCode.OK,
    ) = Fixture(manifest, moduleBody, moduleStatus)

    private fun ComposeUiTest.showBibleSync(f: Fixture) = showScreen {
        BibleSyncSection(
            bibles = f.repository,
            settings = f.settings,
            providedViewModel = f.viewModel,
            providedChoice = f.choice,
        )
    }

    /** Finds the offered translations, ticks the first, and downloads. */
    private fun ComposeUiTest.downloadFirst(f: Fixture) {
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)
    }

    // ── Everything arrived ───────────────────────────────────────────────

    @Test
    fun aFinishedDownloadReportsSuccess() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Success }
    }

    @Test
    fun aFinishedDownloadShowsAResultLine() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aFinishedDownloadNamesWhatArrived() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Success }
        val outcome = f.viewModel.outcome.value as BibleSyncOutcome.Success
        assertTrue(outcome.installed.isNotEmpty())
    }

    @Test
    fun aCleanDownloadNamesNoFailures() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Success }
        assertTrue((f.viewModel.outcome.value as BibleSyncOutcome.Success).failed.isEmpty())
    }

    @Test
    fun aFinishedDownloadPutsTheTranslationOnThePhone() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.viewModel.installed.value.isNotEmpty() }
    }

    @Test
    fun aFinishedDownloadListsItAsInstalled() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_INSTALLED) }
    }

    @Test
    fun theInstalledTranslationIsNamed() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { isShowing("King James Version") }
    }

    @Test
    fun aFinishedDownloadBecomesTheOneInUse() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.choice.activeId.value != null }
    }

    @Test
    fun aFinishedDownloadLeavesNoProgressBar() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_PROGRESS))
    }

    @Test
    fun aFinishedDownloadOffersTheListAgain() = runComposeUiTest {
        // A second translation is a normal next act.
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { exists(LibraryTags.bibleChoice("ru_RST77.spb")) }
    }

    // ── Nothing arrived ──────────────────────────────────────────────────

    @Test
    fun aRefusedModuleIsNotReportedAsCopied() = runComposeUiTest {
        // "Copied" with an empty list would send the operator to a Bible tab
        // that is still empty.
        val f = fixture(moduleStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.viewModel.outcome.value != null }
        val outcome = f.viewModel.outcome.value
        val installedNames = when (outcome) {
            is BibleSyncOutcome.Success -> outcome.installed
            is BibleSyncOutcome.Cancelled -> outcome.installed
            else -> emptyList()
        }
        assertTrue(installedNames.isEmpty())
    }

    @Test
    fun aRefusedModuleStillReportsSomething() = runComposeUiTest {
        val f = fixture(moduleStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aRefusedModuleInstallsNothing() = runComposeUiTest {
        val f = fixture(moduleStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.viewModel.outcome.value != null }
        assertTrue(f.viewModel.installed.value.isEmpty())
    }

    @Test
    fun aRefusedModuleLeavesNoInstalledSection() = runComposeUiTest {
        val f = fixture(moduleStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.viewModel.outcome.value != null }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_INSTALLED))
    }

    @Test
    fun aModuleWithNoVersesIsNotInstalled() = runComposeUiTest {
        // A file that parses to nothing is not a Bible, however well it downloaded.
        val f = fixture(moduleBody = "##Title: Empty\n")
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.viewModel.outcome.value != null }
        assertTrue(f.viewModel.installed.value.isEmpty())
    }

    @Test
    fun aModuleWithNoVersesStillReportsAnOutcome() = runComposeUiTest {
        val f = fixture(moduleBody = "##Title: Empty\n")
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aFailedDownloadCanBeTriedAgain() = runComposeUiTest {
        val f = fixture(moduleStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)
        downloadFirst(f)
        awaitThat { f.viewModel.outcome.value != null }

        assertTrue(exists(LibraryTags.BIBLE_SYNC_DOWNLOAD))
    }

    // ── Two at once ──────────────────────────────────────────────────────

    @Test
    fun bothTickedTranslationsAreDownloaded() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("ru_RST77.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.bibleChoice("ru_RST77.spb"))

        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Success }
    }

    @Test
    fun theResultNamesEverythingThatArrived() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("ru_RST77.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.bibleChoice("ru_RST77.spb"))

        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.outcome.value is BibleSyncOutcome.Success }
        assertTrue((f.viewModel.outcome.value as BibleSyncOutcome.Success).installed.isNotEmpty())
    }

    @Test
    fun aSecondTranslationBringsTheChoosingHint() = runComposeUiTest {
        // With two on the phone, which is read becomes a question.
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("ru_RST77.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.bibleChoice("ru_RST77.spb"))

        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.installed.value.size == 2 }
        assertTrue(exists(LibraryTags.BIBLE_SYNC_CHOOSE_HINT))
    }

    @Test
    fun eachInstalledTranslationCanBeRemoved() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        downloadFirst(f)
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        val id = f.viewModel.installed.value.first().id

        assertTrue(exists(LibraryTags.bibleRemove(id)))
    }

    @Test
    fun removingTheLastTranslationTakesTheSectionAway() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        downloadFirst(f)
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        val id = f.viewModel.installed.value.first().id
        click(LibraryTags.bibleRemove(id))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }

        click(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_INSTALLED) }
    }

    @Test
    fun aRemovedTranslationCanBeDownloadedAgain() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        downloadFirst(f)
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        val id = f.viewModel.installed.value.first().id
        click(LibraryTags.bibleRemove(id))
        click(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM)
        awaitThat { f.viewModel.installed.value.isEmpty() }

        assertTrue(exists(LibraryTags.bibleChoice("en_KJV.spb")))
    }

    @Test
    fun aDesktopOfferingNothingIsNotAFailure() = runComposeUiTest {
        // It answered; it simply has no modules to give.
        val f = fixture(manifest = "[]")
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_OUTCOME))
    }

    @Test
    fun aDownloadedTranslationSaysHowManyVersesItHas() = runComposeUiTest {
        // The number is how an operator tells a full Bible from a New Testament.
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        assertTrue(f.viewModel.installed.value.first().verseCount > 0)
    }

    @Test
    fun aDownloadedTranslationStaysListedWhileMoreCanBePicked() = runComposeUiTest {
        // The offer list is not cleared by a download: an operator who wants two
        // translations should not have to go looking again.
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_INSTALLED) }
        assertTrue(exists(LibraryTags.bibleChoice("ru_RST77.spb")))
    }

    @Test
    fun findingIsNotOfferedTwice() = runComposeUiTest {
        // Once the offers are on screen, the button that fetched them has done
        // its job and goes.
        val f = fixture()
        showBibleSync(f)

        downloadFirst(f)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_INSTALLED) }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_FIND))
    }
}
