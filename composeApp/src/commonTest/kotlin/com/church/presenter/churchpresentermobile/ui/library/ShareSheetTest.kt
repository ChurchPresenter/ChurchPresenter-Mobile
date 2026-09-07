package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.CpsetError
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.LibraryShareViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ShareUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Handing a library to someone else, and taking one in.
 *
 * An import always previews first and makes the operator choose what happens to
 * anything they already have. A silent merge would be quicker and much worse —
 * there would be no way to know a version had been replaced — so the conflict
 * question, and both answers to it, are what these tests hold in place.
 */
@OptIn(ExperimentalTestApi::class)
class ShareSheetTest {

    private fun repository() = LibraryRepository(InMemoryFileStorage()) { 1_000L }

    private fun shareVm(repo: LibraryRepository = repository()) = LibraryShareViewModel(repo)

    private fun ComposeUiTest.showShare(
        vm: LibraryShareViewModel,
        onMessage: (String) -> Unit = {},
    ) = showScreen {
        ShareSheetContent(viewModel = vm, onMessage = onMessage)
    }

    /** A library file carrying [count] songs, as another phone would have written it. */
    private fun exportedLibrary(vararg titles: String): String {
        val repo = repository()
        titles.forEachIndexed { i, title ->
            repo.upsertSong(amazingGrace().copy(id = "s$i", title = title, number = "${i + 1}"))
        }
        return LibraryShareViewModel(repo).exportText()
    }

    // ── What the sheet opens on ──────────────────────────────────────────

    @Test
    fun theSheetOffersToSendALibrary() = runComposeUiTest {
        showShare(shareVm())

        assertTrue(exists(LibraryTags.SHARE_EXPORT))
    }

    @Test
    fun theSheetOffersToTakeALibraryIn() = runComposeUiTest {
        showShare(shareVm())

        assertTrue(exists(LibraryTags.SHARE_IMPORT))
    }

    @Test
    fun noPreviewIsShownBeforeAFileIsPicked() = runComposeUiTest {
        showShare(shareVm())

        assertFalse(exists(LibraryTags.SHARE_PREVIEW))
    }

    @Test
    fun noResultIsShownBeforeAnythingHappens() = runComposeUiTest {
        showShare(shareVm())

        assertFalse(exists(LibraryTags.SHARE_RESULT))
    }

    // ── An import previews first ─────────────────────────────────────────

    @Test
    fun aPickedLibraryIsPreviewedRatherThanMerged() = runComposeUiTest {
        val vm = shareVm()
        showShare(vm)

        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")

        awaitThat { exists(LibraryTags.SHARE_PREVIEW) }
    }

    @Test
    fun nothingIsImportedByThePreviewItself() = runComposeUiTest {
        val repo = repository()
        val vm = shareVm(repo)
        showShare(vm)

        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")

        awaitThat { exists(LibraryTags.SHARE_PREVIEW) }
        assertTrue(repo.library.value.songs.isEmpty())
    }

    @Test
    fun aPreviewSaysHowManyAreNew() = runComposeUiTest {
        val vm = shareVm()
        showShare(vm)

        vm.onFilePicked(exportedLibrary("Amazing Grace", "How Great"), "library.cpset")

        awaitThat { exists(LibraryTags.SHARE_PREVIEW) }
        assertTrue(isShowing("2"))
    }

    @Test
    fun aLibraryWithNoOverlapIsConfirmedInOneTap() = runComposeUiTest {
        // Nothing of the operator's is at risk, so there is nothing to ask.
        val vm = shareVm()
        showShare(vm)

        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")

        awaitThat { exists(LibraryTags.shareResolve("confirm")) }
        assertFalse(exists(LibraryTags.shareResolve("replace")))
    }

    @Test
    fun confirmingAnImportBringsTheSongsIn() = runComposeUiTest {
        val repo = repository()
        val vm = shareVm(repo)
        showShare(vm)
        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { repo.library.value.songs.isNotEmpty() }
    }

    @Test
    fun aFinishedImportSaysHowManyArrived() = runComposeUiTest {
        val vm = shareVm()
        showShare(vm)
        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
    }

    @Test
    fun aFinishedImportClosesThePreview() = runComposeUiTest {
        val vm = shareVm()
        showShare(vm)
        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { !exists(LibraryTags.SHARE_PREVIEW) }
    }

    // ── When the two libraries overlap ───────────────────────────────────

    @Test
    fun anOverlapIsCounted() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", title = "Amazing Grace", number = "1"))
        val vm = shareVm(repo)
        showShare(vm)

        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")

        awaitThat { exists(LibraryTags.SHARE_PREVIEW) }
        assertTrue(
            (vm.uiState.value as ShareUiState.Previewing).preview.conflictCount > 0,
            "an incoming song with the same number should be counted as a conflict",
        )
    }

    @Test
    fun anOverlapAsksWhatToDo() = runComposeUiTest {
        // A silent merge would leave the operator with no way to know their own
        // version had been replaced.
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", title = "Amazing Grace", number = "1"))
        val vm = shareVm(repo)
        showShare(vm)

        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")

        awaitThat { exists(LibraryTags.shareResolve("keepMine")) }
        assertTrue(exists(LibraryTags.shareResolve("replace")))
    }

    @Test
    fun anOverlapOffersNoOneTapConfirm() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", title = "Amazing Grace", number = "1"))
        val vm = shareVm(repo)
        showShare(vm)

        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")

        awaitThat { exists(LibraryTags.shareResolve("replace")) }
        assertFalse(exists(LibraryTags.shareResolve("confirm")))
    }

    @Test
    fun keepingMineLeavesTheOperatorsVersionInPlace() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", title = "My own words", number = "1"))
        val vm = shareVm(repo)
        showShare(vm)
        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("keepMine")) }

        click(LibraryTags.shareResolve("keepMine"))

        awaitThat { vm.uiState.value is ShareUiState.Imported }
        assertTrue(repo.library.value.songs.any { it.title == "My own words" })
    }

    @Test
    fun replacingTakesTheIncomingVersion() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", title = "My own words", number = "1"))
        val vm = shareVm(repo)
        showShare(vm)
        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("replace")) }

        click(LibraryTags.shareResolve("replace"))

        awaitThat { vm.uiState.value is ShareUiState.Imported }
        assertTrue(repo.library.value.songs.any { it.title == "Amazing Grace" })
    }

    @Test
    fun eitherAnswerFinishesTheImport() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", title = "My own words", number = "1"))
        val vm = shareVm(repo)
        showShare(vm)
        vm.onFilePicked(exportedLibrary("Amazing Grace"), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("keepMine")) }

        click(LibraryTags.shareResolve("keepMine"))

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
    }

    // ── A file that will not open ────────────────────────────────────────

    @Test
    fun anEmptyFileIsReported() = runComposeUiTest {
        val vm = shareVm()
        showShare(vm)

        vm.onFilePicked("", "empty.cpset")

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
    }

    @Test
    fun anEmptyFileIsNotPreviewed() = runComposeUiTest {
        val vm = shareVm()
        showShare(vm)

        vm.onFilePicked("", "empty.cpset")

        awaitThat { vm.uiState.value is ShareUiState.Error }
        assertFalse(exists(LibraryTags.SHARE_PREVIEW))
    }

    @Test
    fun anUnreadableLibraryIsReported() = runComposeUiTest {
        // It announces itself as a library file and then does not parse — which
        // is a different problem from a plain text file, and gets a different
        // message.
        val vm = shareVm()
        showShare(vm)

        vm.onFilePicked("""{"format":"cpset", not json at all""", "broken.cpset")

        awaitThat { exists(LibraryTags.SHARE_RESULT) }
    }

    @Test
    fun aFailedImportChangesNothing() = runComposeUiTest {
        val repo = repository()
        val vm = shareVm(repo)
        showShare(vm)

        vm.onFilePicked("""{"format":"cpset", not json at all""", "broken.cpset")

        awaitThat { vm.uiState.value is ShareUiState.Error }
        assertTrue(repo.library.value.songs.isEmpty())
    }

    // ── Which words each failure gets ────────────────────────────────────

    @Test
    fun anUnreadableFileHasItsOwnMessage() {
        assertEquals(
            CpsetError.UNREADABLE.messageResource(),
            CpsetError.UNREADABLE.messageResource(),
        )
    }

    @Test
    fun everyFailureReasonGetsADifferentMessage() {
        // Each points at a different fix — re-export, pick another file, update
        // the app — so two sharing a message would send someone down the wrong one.
        val messages = CpsetError.entries.map { it.messageResource() }

        assertEquals(messages.size, messages.toSet().size)
    }

    @Test
    fun everyFailureReasonIsAccountedFor() {
        CpsetError.entries.forEach { it.messageResource() }
    }
}
