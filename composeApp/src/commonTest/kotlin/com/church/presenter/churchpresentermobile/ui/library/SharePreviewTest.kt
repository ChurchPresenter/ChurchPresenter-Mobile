package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
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
import kotlin.test.assertTrue

/**
 * The preview an import always shows before it changes anything.
 *
 * Its job is to make the operator's own copies safe: it says how much is new,
 * how much overlaps, and refuses to proceed on the overlap without an answer.
 * The cases here are the awkward ones — a file with nothing in it, a file that
 * is all overlap, and backing out halfway.
 */
@OptIn(ExperimentalTestApi::class)
class SharePreviewTest {

    private fun repository() = LibraryRepository(InMemoryFileStorage()) { 1_000L }

    private fun notice(id: String, title: String) =
        LocalAnnouncement(id = id, title = title, body = "Coffee in the hall")

    private fun ComposeUiTest.showShare(vm: LibraryShareViewModel) = showScreen {
        ShareSheetContent(viewModel = vm, onMessage = {})
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

    // ── A file with nothing in it ────────────────────────────────────────

    @Test
    fun anEmptyLibraryFileIsPreviewedRatherThanImported() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        vm.onFilePicked(exported(), "empty-library.cpset")

        awaitThat { vm.uiState.value is ShareUiState.Previewing || vm.uiState.value is ShareUiState.Error }
    }

    @Test
    fun anEmptyLibraryFileAddsNothing() = runComposeUiTest {
        val repo = repository()
        val vm = LibraryShareViewModel(repo)
        showShare(vm)

        vm.onFilePicked(exported(), "empty-library.cpset")

        awaitThat { vm.uiState.value !is ShareUiState.Idle }
        assertTrue(repo.library.value.songs.isEmpty())
    }

    // ── A file that is all new ───────────────────────────────────────────

    @Test
    fun aFileOfNewSongsIsCounted() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        vm.onFilePicked(exported(songTitles = listOf("A", "B", "C")), "library.cpset")

        awaitThat { vm.uiState.value is ShareUiState.Previewing }
        assertEquals(3, (vm.uiState.value as ShareUiState.Previewing).preview.newCount)
    }

    @Test
    fun aFileOfNewSongsHasNoOverlap() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        vm.onFilePicked(exported(songTitles = listOf("A", "B")), "library.cpset")

        awaitThat { vm.uiState.value is ShareUiState.Previewing }
        assertEquals(0, (vm.uiState.value as ShareUiState.Previewing).preview.conflictCount)
    }

    @Test
    fun aFileOfNewNoticesIsCountedToo() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        vm.onFilePicked(exported(noticeTitles = listOf("Welcome", "Offering")), "library.cpset")

        awaitThat { vm.uiState.value is ShareUiState.Previewing }
        assertTrue((vm.uiState.value as ShareUiState.Previewing).preview.newCount >= 2)
    }

    @Test
    fun songsAndNoticesAreCountedTogether() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        vm.onFilePicked(
            exported(songTitles = listOf("A"), noticeTitles = listOf("Welcome")),
            "library.cpset",
        )

        awaitThat { vm.uiState.value is ShareUiState.Previewing }
        assertEquals(2, (vm.uiState.value as ShareUiState.Previewing).preview.newCount)
    }

    @Test
    fun confirmingBringsInBothKinds() = runComposeUiTest {
        val repo = repository()
        val vm = LibraryShareViewModel(repo)
        showShare(vm)
        vm.onFilePicked(
            exported(songTitles = listOf("A"), noticeTitles = listOf("Welcome")),
            "library.cpset",
        )
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { repo.library.value.songs.isNotEmpty() }
        assertTrue(repo.library.value.announcements.isNotEmpty())
    }

    @Test
    fun aFinishedImportSaysHowManyArrived() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)
        vm.onFilePicked(exported(songTitles = listOf("A", "B")), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { vm.uiState.value is ShareUiState.Imported }
        assertEquals(2, (vm.uiState.value as ShareUiState.Imported).count)
    }

    // ── A file that overlaps ─────────────────────────────────────────────

    @Test
    fun anOverlappingNoticeIsCounted() = runComposeUiTest {
        val repo = repository()
        repo.upsertAnnouncement(notice("n0", "Welcome"))
        val vm = LibraryShareViewModel(repo)
        showShare(vm)

        vm.onFilePicked(exported(noticeTitles = listOf("Welcome")), "library.cpset")

        awaitThat { vm.uiState.value is ShareUiState.Previewing }
        assertTrue((vm.uiState.value as ShareUiState.Previewing).preview.conflictCount > 0)
    }

    @Test
    fun aFileThatIsAllOverlapStillAsksTheQuestion() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", number = "1", title = "Mine"))
        val vm = LibraryShareViewModel(repo)
        showShare(vm)

        vm.onFilePicked(exported(songTitles = listOf("Theirs")), "library.cpset")

        awaitThat { exists(LibraryTags.shareResolve("keepMine")) }
        assertTrue(exists(LibraryTags.shareResolve("replace")))
    }

    @Test
    fun keepingMineImportsTheNewOnesAnyway() = runComposeUiTest {
        // Only the overlap is in question; anything new still comes in.
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", number = "1", title = "Mine"))
        val vm = LibraryShareViewModel(repo)
        showShare(vm)
        vm.onFilePicked(exported(songTitles = listOf("Theirs", "Brand new")), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("keepMine")) }

        click(LibraryTags.shareResolve("keepMine"))

        awaitThat { vm.uiState.value is ShareUiState.Imported }
        assertTrue(repo.library.value.songs.any { it.title == "Brand new" })
    }

    @Test
    fun keepingMineLeavesTheOverlapAlone() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", number = "1", title = "Mine"))
        val vm = LibraryShareViewModel(repo)
        showShare(vm)
        vm.onFilePicked(exported(songTitles = listOf("Theirs")), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("keepMine")) }

        click(LibraryTags.shareResolve("keepMine"))

        awaitThat { vm.uiState.value is ShareUiState.Imported }
        assertTrue(repo.library.value.songs.any { it.title == "Mine" })
    }

    @Test
    fun replacingTakesTheirVersionOfTheOverlap() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", number = "1", title = "Mine"))
        val vm = LibraryShareViewModel(repo)
        showShare(vm)
        vm.onFilePicked(exported(songTitles = listOf("Theirs")), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("replace")) }

        click(LibraryTags.shareResolve("replace"))

        awaitThat { vm.uiState.value is ShareUiState.Imported }
        assertTrue(repo.library.value.songs.any { it.title == "Theirs" })
    }

    @Test
    fun replacingLeavesOneCopyNotTwo() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace().copy(id = "s0", number = "1", title = "Mine"))
        val vm = LibraryShareViewModel(repo)
        showShare(vm)
        vm.onFilePicked(exported(songTitles = listOf("Theirs")), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("replace")) }

        click(LibraryTags.shareResolve("replace"))

        awaitThat { vm.uiState.value is ShareUiState.Imported }
        assertEquals(1, repo.library.value.songs.size)
    }

    @Test
    fun anOverlappingNoticeCanBeKept() = runComposeUiTest {
        val repo = repository()
        repo.upsertAnnouncement(notice("n0", "Welcome"))
        val vm = LibraryShareViewModel(repo)
        showShare(vm)
        vm.onFilePicked(exported(noticeTitles = listOf("Welcome")), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("keepMine")) }

        click(LibraryTags.shareResolve("keepMine"))

        awaitThat { vm.uiState.value is ShareUiState.Imported }
        assertEquals(1, repo.library.value.announcements.size)
    }

    // ── Backing out ──────────────────────────────────────────────────────

    @Test
    fun aPreviewCanBeAbandoned() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)
        vm.onFilePicked(exported(songTitles = listOf("A")), "library.cpset")
        awaitThat { exists(LibraryTags.SHARE_PREVIEW) }

        vm.dismiss()

        awaitThat { vm.uiState.value is ShareUiState.Idle }
    }

    @Test
    fun abandoningAPreviewImportsNothing() = runComposeUiTest {
        val repo = repository()
        val vm = LibraryShareViewModel(repo)
        showShare(vm)
        vm.onFilePicked(exported(songTitles = listOf("A")), "library.cpset")
        awaitThat { exists(LibraryTags.SHARE_PREVIEW) }

        vm.dismiss()

        awaitThat { vm.uiState.value is ShareUiState.Idle }
        assertTrue(repo.library.value.songs.isEmpty())
    }

    @Test
    fun abandoningAPreviewBringsBackTheTwoDirections() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)
        vm.onFilePicked(exported(songTitles = listOf("A")), "library.cpset")
        awaitThat { exists(LibraryTags.SHARE_PREVIEW) }

        vm.dismiss()

        awaitThat { exists(LibraryTags.SHARE_EXPORT) }
    }

    @Test
    fun aSecondFileCanBePreviewedAfterAnImport() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)
        vm.onFilePicked(exported(songTitles = listOf("A")), "library.cpset")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }
        click(LibraryTags.shareResolve("confirm"))
        awaitThat { vm.uiState.value is ShareUiState.Imported }

        vm.onFilePicked(exported(songTitles = listOf("B")), "another.cpset")

        awaitThat { vm.uiState.value is ShareUiState.Previewing }
    }

    // ── What an export carries ───────────────────────────────────────────

    @Test
    fun anExportCarriesTheSongs() = runComposeUiTest {
        val repo = repository()
        repo.upsertSong(amazingGrace())
        val vm = LibraryShareViewModel(repo)
        showShare(vm)

        assertTrue(vm.exportText().contains("Amazing Grace"))
    }

    @Test
    fun anExportCarriesTheNotices() = runComposeUiTest {
        val repo = repository()
        repo.upsertAnnouncement(notice("n1", "Welcome"))
        val vm = LibraryShareViewModel(repo)
        showShare(vm)

        assertTrue(vm.exportText().contains("Welcome"))
    }

    @Test
    fun anExportOfAnEmptyLibraryIsStillAFile() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        assertTrue(vm.exportText().isNotBlank())
    }

    @Test
    fun anExportIsNamedSomethingRecognisable() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        assertTrue(vm.exportFileName().endsWith(".cpset"))
    }

    @Test
    fun anExportedFileCanBeReadBackIn() = runComposeUiTest {
        // The round trip is the whole feature: what one phone writes another has
        // to be able to open.
        val repo = repository()
        repo.upsertSong(amazingGrace())
        val exportedText = LibraryShareViewModel(repo).exportText()
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        vm.onFilePicked(exportedText, "library.cpset")

        awaitThat { vm.uiState.value is ShareUiState.Previewing }
    }

    @Test
    fun aPlainTextFileIsTreatedAsASingleSong() = runComposeUiTest {
        // Songs arrive from other apps as ChordPro or plain text, not as a whole
        // library.
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        vm.onFilePicked("Amazing grace, how sweet the sound", "song.txt")

        awaitThat { vm.uiState.value is ShareUiState.Previewing }
    }

    @Test
    fun aPlainTextSongCountsAsOneNewItem() = runComposeUiTest {
        val vm = LibraryShareViewModel(repository())
        showShare(vm)

        vm.onFilePicked("Amazing grace, how sweet the sound", "song.txt")

        awaitThat { vm.uiState.value is ShareUiState.Previewing }
        assertEquals(1, (vm.uiState.value as ShareUiState.Previewing).preview.newCount)
    }

    @Test
    fun aPlainTextSongCanBeImported() = runComposeUiTest {
        val repo = repository()
        val vm = LibraryShareViewModel(repo)
        showShare(vm)
        vm.onFilePicked("Amazing grace, how sweet the sound", "song.txt")
        awaitThat { exists(LibraryTags.shareResolve("confirm")) }

        click(LibraryTags.shareResolve("confirm"))

        awaitThat { repo.library.value.songs.isNotEmpty() }
    }
}
