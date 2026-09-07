package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.ConflictResolution
import com.church.presenter.churchpresentermobile.model.CpsetError
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.LibraryShareViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ShareUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What the share sheet says when an import ends — well or badly.
 *
 * A file that cannot be read has four distinct reasons, and they are not
 * interchangeable advice: "this is not one of our files" sends someone looking
 * for the right file, while "this needs a newer app" sends them to the store.
 * Collapsing them into one "import failed" is the failure these cover, along
 * with the opposite mistake — an import that reports a count it did not
 * actually write.
 */
@OptIn(ExperimentalTestApi::class)
class ShareImportOutcomesTest {

    private fun repository() = LibraryRepository(InMemoryFileStorage()) { 1_000L }

    private fun notice(id: String, title: String) =
        LocalAnnouncement(id = id, title = title, body = "Coffee in the hall")

    private fun ComposeUiTest.showShare(
        viewModel: LibraryShareViewModel,
        onMessage: (String) -> Unit = {},
    ) = showScreen {
        ShareSheetContent(viewModel = viewModel, onMessage = onMessage)
    }

    /** A library file as another phone would have written it. */
    private fun exported(
        songTitles: List<String> = emptyList(),
        noticeTitles: List<String> = emptyList(),
    ): String {
        val repo = repository()
        songTitles.forEachIndexed { i, title ->
            repo.upsertSong(amazingGrace().copy(id = "s$i", title = title, number = "${i + 1}"))
        }
        noticeTitles.forEachIndexed { i, title -> repo.upsertAnnouncement(notice("n$i", title)) }
        return LibraryShareViewModel(repo).exportText()
    }

    // ── A file that cannot be read ───────────────────────────────────────

    @Test
    fun aFileClaimingToBeOursButNotJsonIsRefused() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("""{ "format" : truncated""", "broken.cpset")

        awaitThat { viewModel.uiState.value is ShareUiState.Error }
    }

    @Test
    fun anUnreadableFileIsNamedAsUnreadable() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("""{ "format" : truncated""", "broken.cpset")

        awaitThat { viewModel.uiState.value is ShareUiState.Error }
        assertEquals(CpsetError.UNREADABLE, (viewModel.uiState.value as ShareUiState.Error).error)
    }

    @Test
    fun anUnreadableFileShowsAResultLine() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("""{ "format" : truncated""", "broken.cpset")

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
    }

    @Test
    fun someoneElsesJsonIsNamedAsTheWrongFormat() = runComposeUiTest {
        // Not "corrupt": the file is fine, it is simply not ours.
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("""{"format":"some-other-app","version":1}""", "other.cpset")

        awaitThat { viewModel.uiState.value is ShareUiState.Error }
        assertEquals(CpsetError.WRONG_FORMAT, (viewModel.uiState.value as ShareUiState.Error).error)
    }

    @Test
    fun someoneElsesJsonShowsAResultLine() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("""{"format":"some-other-app","version":1}""", "other.cpset")

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
    }

    @Test
    fun aFileFromANewerAppIsNamedAsTooNew() = runComposeUiTest {
        // Partially reading it would silently drop fields the sender thought
        // mattered, which is worse than saying so.
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("""{"format":"churchpresenter.set","version":9999}""", "new.cpset")

        awaitThat { viewModel.uiState.value is ShareUiState.Error }
        assertEquals(CpsetError.TOO_NEW, (viewModel.uiState.value as ShareUiState.Error).error)
    }

    @Test
    fun aFileFromANewerAppImportsNothing() = runComposeUiTest {
        val repo = repository()
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)

        viewModel.onFilePicked("""{"format":"churchpresenter.set","version":9999}""", "new.cpset")

        awaitThat { viewModel.uiState.value is ShareUiState.Error }
        assertTrue(repo.library.value.songs.isEmpty())
    }

    @Test
    fun anEmptyTextFileIsNamedAsEmpty() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("   ", "blank.txt")

        awaitThat { viewModel.uiState.value is ShareUiState.Error }
        assertEquals(CpsetError.EMPTY, (viewModel.uiState.value as ShareUiState.Error).error)
    }

    @Test
    fun anEmptyTextFileShowsAResultLine() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("", "blank.txt")

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
    }

    @Test
    fun aRefusedFileLeavesNoPreview() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("""{"format":"some-other-app"}""", "other.cpset")

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
        assertFalse(exists(LibraryTags.SHARE_PREVIEW))
    }

    @Test
    fun aRefusedFileLeavesNoImportButton() = runComposeUiTest {
        // The sheet is showing its answer; the way in is not also on screen.
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("""{"format":"some-other-app"}""", "other.cpset")

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
        assertFalse(exists(LibraryTags.SHARE_IMPORT))
    }

    @Test
    fun aRefusedFileCanBeFollowedByAGoodOne() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)
        viewModel.onFilePicked("""{"format":"some-other-app"}""", "other.cpset")
        awaitThat { viewModel.uiState.value is ShareUiState.Error }

        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace")), "good.cpset")

        awaitThat { viewModel.uiState.value is ShareUiState.Previewing }
    }

    // ── A file that read fine ────────────────────────────────────────────

    @Test
    fun aFinishedImportSaysHowMuchArrived() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)
        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace", "How Great")), "in.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { viewModel.uiState.value is ShareUiState.Imported }
        assertEquals(2, (viewModel.uiState.value as ShareUiState.Imported).count)
    }

    @Test
    fun aFinishedImportShowsAResultLine() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)
        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace")), "in.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
    }

    @Test
    fun aFinishedImportWritesWhatItCounted() = runComposeUiTest {
        // The count and the library have to agree; a report of two over one
        // written song is the failure worth catching.
        val repo = repository()
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)
        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace", "How Great")), "in.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { repo.library.value.songs.size == 2 }
    }

    @Test
    fun aFinishedImportBringsNoticesToo() = runComposeUiTest {
        val repo = repository()
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)
        viewModel.onFilePicked(exported(noticeTitles = listOf("Welcome")), "in.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { repo.library.value.announcements.size == 1 }
    }

    @Test
    fun aFinishedImportClearsThePreview() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)
        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace")), "in.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { !exists(LibraryTags.SHARE_PREVIEW) }
    }

    @Test
    fun aFinishedImportDoesNotOfferAnotherOneOverItsResult() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)
        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace")), "in.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
        assertFalse(exists(LibraryTags.SHARE_EXPORT))
    }

    @Test
    fun anImportOfNothingNewReportsNothing() = runComposeUiTest {
        // Everything in the file is already here and the operator kept theirs.
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(number = "1"))
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)
        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace")), "in.cpset")
        awaitThat { exists(LibraryTags.shareResolve("keepMine")) }

        click(LibraryTags.shareResolve("keepMine"))

        awaitThat { viewModel.uiState.value is ShareUiState.Imported }
        assertEquals(0, (viewModel.uiState.value as ShareUiState.Imported).count)
    }

    @Test
    fun replacingCountsTheOverlapAsImported() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(number = "1"))
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)
        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace")), "in.cpset")
        awaitThat { exists(LibraryTags.shareResolve("replace")) }

        click(LibraryTags.shareResolve("replace"))

        awaitThat { viewModel.uiState.value is ShareUiState.Imported }
        assertEquals(1, (viewModel.uiState.value as ShareUiState.Imported).count)
    }

    @Test
    fun keepingMineLeavesTheOperatorsVersionInPlace() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(number = "1", title = "Amazing Grace"))
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)
        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace")), "in.cpset")
        awaitThat { exists(LibraryTags.shareResolve("keepMine")) }

        click(LibraryTags.shareResolve("keepMine"))

        awaitThat { viewModel.uiState.value is ShareUiState.Imported }
        assertEquals(1, repo.library.value.songs.size)
    }

    // ── Backing out ──────────────────────────────────────────────────────

    @Test
    fun abandoningAPreviewReturnsToTheStart() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)
        viewModel.onFilePicked(exported(), "empty.cpset")
        awaitThat { viewModel.uiState.value !is ShareUiState.Idle }

        viewModel.dismiss()

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
    }

    @Test
    fun abandoningAPreviewImportsNothing() = runComposeUiTest {
        val repo = repository()
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)
        viewModel.onFilePicked(exported(songTitles = listOf("Amazing Grace")), "in.cpset")
        awaitThat { viewModel.uiState.value is ShareUiState.Previewing }

        viewModel.dismiss()

        awaitThat { exists(LibraryTags.SHARE_IMPORT) }
        assertTrue(repo.library.value.songs.isEmpty())
    }

    @Test
    fun dismissingAResultReturnsToTheStart() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)
        viewModel.onFilePicked("""{"format":"some-other-app"}""", "other.cpset")
        awaitThat { exists(LibraryTags.SHARE_RESULT) }

        viewModel.dismiss()

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
    }

    @Test
    fun confirmingWithNoPreviewChangesNothing() = runComposeUiTest {
        // The sheet cannot show the buttons in this state; the guard is what
        // keeps a stale tap from writing an empty library over a full one.
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(number = "1"))
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)

        viewModel.confirmImport(ConflictResolution.REPLACE)

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
        assertEquals(1, repo.library.value.songs.size)
    }

    // ── Exporting ────────────────────────────────────────────────────────

    @Test
    fun theStartOfTheSheetOffersBothDirections() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())

        showShare(viewModel)

        assertTrue(exists(LibraryTags.SHARE_EXPORT))
        assertTrue(exists(LibraryTags.SHARE_IMPORT))
    }

    @Test
    fun anExportNamesAFileTheOperatorCanRecognise() = runComposeUiTest {
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        assertTrue(viewModel.exportFileName().endsWith(".cpset"))
    }

    @Test
    fun anExportCarriesTheLibraryItWasMadeFrom() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(number = "1"))
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)

        assertTrue(viewModel.exportText().contains("Amazing Grace"))
    }

    @Test
    fun anExportOfNothingIsStillOurFormat() = runComposeUiTest {
        // An empty library exports a valid, empty document rather than nothing.
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        assertTrue(viewModel.looksLikeCpset(viewModel.exportText()))
    }

    @Test
    fun anExportedFileReadsBackAsAPreview() = runComposeUiTest {
        // The round trip is the point of the feature.
        val source = repository()
        source.upsertSong(amazingGrace())
        val text = LibraryShareViewModel(source).exportText()
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked(text, "round-trip.cpset")

        awaitThat { viewModel.uiState.value is ShareUiState.Previewing }
    }

    @Test
    fun anExportedFileReadsBackWithItsSongs() = runComposeUiTest {
        val source = repository()
        source.upsertSong(amazingGrace())
        val text = LibraryShareViewModel(source).exportText()
        val target = repository()
        val viewModel = LibraryShareViewModel(target)
        showShare(viewModel)
        viewModel.onFilePicked(text, "round-trip.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { target.library.value.songs.size == 1 }
    }

    @Test
    fun aPlainSongFileIsTreatedAsOneSong() = runComposeUiTest {
        // Songs arrive from other apps as ChordPro or plain text, not as ours.
        val viewModel = LibraryShareViewModel(repository())
        showShare(viewModel)

        viewModel.onFilePicked("Amazing grace, how sweet the sound", "amazing-grace.txt")

        awaitThat { viewModel.uiState.value is ShareUiState.Previewing }
        assertIs<ShareUiState.Previewing>(viewModel.uiState.value)
    }

    @Test
    fun aPlainSongFileTakesItsTitleFromItsName() = runComposeUiTest {
        val repo = repository()
        val viewModel = LibraryShareViewModel(repo)
        showShare(viewModel)
        viewModel.onFilePicked("Amazing grace, how sweet the sound", "Amazing Grace.txt")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { repo.library.value.songs.any { it.title == "Amazing Grace" } }
    }
}
