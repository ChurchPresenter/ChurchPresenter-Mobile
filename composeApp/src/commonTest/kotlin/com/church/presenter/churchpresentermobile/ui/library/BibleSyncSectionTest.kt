package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.runComposeUiTest
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
 * Copying a Bible from the computer onto the phone.
 *
 * Unlike songs, nothing is asked of the computer until the operator asks for
 * it: opening the sheet to copy songs must not fire a Bible request at an
 * address that may be wrong. After that the sheet has four faces — nothing
 * found yet, a pick list, a download in flight, and a result — and each has to
 * offer the one action that makes sense for it.
 */
@OptIn(ExperimentalTestApi::class)
class BibleSyncSectionTest {

    private val module = """
        ##Title: King James Version
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning.
    """.trimIndent()

    private class Fixture(
        manifest: String = """["en_KJV.spb","ru_RST77.spb"]""",
        manifestStatus: HttpStatusCode = HttpStatusCode.OK,
        moduleBody: String = "",
    ) {
        val storage = InMemoryFileStorage()
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LocalBibleRepository(storage, now = { 0L })
        val viewModel: BibleSyncViewModel
        val choice: BibleChoiceViewModel

        init {
            val client = HttpClient(MockEngine { request ->
                if (request.url.encodedPath.endsWith(ApiConstants.BIBLE_TRANSLATIONS_ENDPOINT)) {
                    respond(manifest, manifestStatus)
                } else {
                    respond(moduleBody, HttpStatusCode.OK)
                }
            })
            viewModel = BibleSyncViewModel(repository, settings, BibleDownloadService(settings, client))
            choice = BibleChoiceViewModel(repository)
        }
    }

    private fun ComposeUiTest.showBibleSync(f: Fixture) = showScreen {
        BibleSyncSection(
            bibles = f.repository,
            settings = f.settings,
            providedViewModel = f.viewModel,
            providedChoice = f.choice,
        )
    }

    private fun fixture(
        manifest: String = """["en_KJV.spb","ru_RST77.spb"]""",
        manifestStatus: HttpStatusCode = HttpStatusCode.OK,
    ) = Fixture(manifest, manifestStatus, module)

    // ── Nothing is asked until the operator asks ─────────────────────────

    @Test
    fun theSheetOffersToLookRatherThanLookingByItself() = runComposeUiTest {
        // Opening the sheet must not fire a request at an address that may be wrong.
        val f = fixture()
        showBibleSync(f)

        assertTrue(exists(LibraryTags.BIBLE_SYNC_FIND))
    }

    @Test
    fun nothingIsAskedOfTheComputerOnOpen() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        assertTrue(f.viewModel.choices.value.isEmpty())
    }

    @Test
    fun noPickListIsShownBeforeLooking() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        assertFalse(exists(LibraryTags.bibleChoice("en_KJV.spb")))
    }

    @Test
    fun noDownloadButtonIsShownBeforeLooking() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        assertFalse(exists(LibraryTags.BIBLE_SYNC_DOWNLOAD))
    }

    @Test
    fun askingFetchesWhatTheComputerHas() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { f.viewModel.choices.value.isNotEmpty() }
    }

    @Test
    fun everyTranslationOfferedIsListed() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        assertTrue(exists(LibraryTags.bibleChoice("ru_RST77.spb")))
    }

    @Test
    fun theListReplacesTheLookButton() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun aComputerThatOffersNothingKeepsTheLookButton() = runComposeUiTest {
        // Nothing to pick from; the only sensible action is to try again.
        val f = fixture(manifest = "[]")
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun aComputerThatRefusesIsReported() = runComposeUiTest {
        val f = fixture(manifestStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_LOAD_ERROR) }
    }

    @Test
    fun aRefusedLookCanBeTriedAgain() = runComposeUiTest {
        val f = fixture(manifestStatus = HttpStatusCode.InternalServerError)
        showBibleSync(f)

        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_LOAD_ERROR) }
        assertTrue(exists(LibraryTags.BIBLE_SYNC_FIND))
    }

    // ── Picking translations ─────────────────────────────────────────────

    @Test
    fun nothingIsTickedToBeginWith() = runComposeUiTest {
        // A multi-megabyte download is not something to start by accident.
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)

        awaitThat { f.viewModel.choices.value.isNotEmpty() }
        assertTrue(f.viewModel.selection.value.isEmpty())
    }

    @Test
    fun withNothingTickedTheDownloadIsDead() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.BIBLE_SYNC_DOWNLOAD) }

        tagged(LibraryTags.BIBLE_SYNC_DOWNLOAD).assertIsNotEnabled()
    }

    @Test
    fun tickingATranslationSelectsIt() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }

        click(LibraryTags.bibleChoice("en_KJV.spb"))

        awaitThat { f.viewModel.selection.value.contains("en_KJV.spb") }
    }

    @Test
    fun tickingATranslationMakesTheDownloadLive() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }

        click(LibraryTags.bibleChoice("en_KJV.spb"))

        awaitThat { f.viewModel.selection.value.isNotEmpty() }
        tagged(LibraryTags.BIBLE_SYNC_DOWNLOAD).assertIsEnabled()
    }

    @Test
    fun untickingItAgainDisablesTheDownload() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        awaitThat { f.viewModel.selection.value.isNotEmpty() }

        click(LibraryTags.bibleChoice("en_KJV.spb"))

        awaitThat { f.viewModel.selection.value.isEmpty() }
        tagged(LibraryTags.BIBLE_SYNC_DOWNLOAD).assertIsNotEnabled()
    }

    @Test
    fun twoTranslationsCanBeTakenAtOnce() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("ru_RST77.spb")) }

        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.bibleChoice("ru_RST77.spb"))

        awaitThat { f.viewModel.selection.value.size == 2 }
    }

    @Test
    fun tickingOneLeavesTheOtherAlone() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("ru_RST77.spb")) }

        click(LibraryTags.bibleChoice("en_KJV.spb"))

        awaitThat { f.viewModel.selection.value.contains("en_KJV.spb") }
        assertFalse(f.viewModel.selection.value.contains("ru_RST77.spb"))
    }

    // ── Downloading ──────────────────────────────────────────────────────

    @Test
    fun downloadingReportsWhatArrived() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))

        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_OUTCOME) }
    }

    @Test
    fun aDownloadedTranslationIsListedAsInstalled() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))

        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        awaitThat { exists(LibraryTags.BIBLE_SYNC_INSTALLED) }
    }

    @Test
    fun aDownloadedTranslationCanBeRemoved() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        val id = f.viewModel.installed.value.first().id

        assertTrue(exists(LibraryTags.bibleRemove(id)))
    }

    @Test
    fun removingAsksFirst() = runComposeUiTest {
        // A translation is a long download to lose to a mis-tap.
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        val id = f.viewModel.installed.value.first().id

        click(LibraryTags.bibleRemove(id))

        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }
    }

    @Test
    fun backingOutOfARemovalKeepsTheTranslation() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        val id = f.viewModel.installed.value.first().id
        click(LibraryTags.bibleRemove(id))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_DISMISS) }

        click(LibraryTags.BIBLE_SYNC_REMOVE_DISMISS)

        awaitThat { !exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }
        assertTrue(f.viewModel.installed.value.isNotEmpty())
    }

    @Test
    fun confirmingARemovalTakesItOff() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)
        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        val id = f.viewModel.installed.value.first().id
        click(LibraryTags.bibleRemove(id))
        awaitThat { exists(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM) }

        click(LibraryTags.BIBLE_SYNC_REMOVE_CONFIRM)

        awaitThat { f.viewModel.installed.value.isEmpty() }
    }

    @Test
    fun noInstalledSectionIsShownWithNothingDownloaded() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)

        assertFalse(exists(LibraryTags.BIBLE_SYNC_INSTALLED))
    }

    @Test
    fun aSingleTranslationNeedsNoChoosingHint() = runComposeUiTest {
        // With one there is nothing to choose between.
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        assertFalse(exists(LibraryTags.BIBLE_SYNC_CHOOSE_HINT))
    }

    @Test
    fun theDownloadedTranslationBecomesTheOneInUse() = runComposeUiTest {
        val f = fixture()
        showBibleSync(f)
        click(LibraryTags.BIBLE_SYNC_FIND)
        awaitThat { exists(LibraryTags.bibleChoice("en_KJV.spb")) }
        click(LibraryTags.bibleChoice("en_KJV.spb"))
        click(LibraryTags.BIBLE_SYNC_DOWNLOAD)

        awaitThat { f.viewModel.installed.value.isNotEmpty() }
        awaitThat { f.choice.activeId.value != null }
    }
}
