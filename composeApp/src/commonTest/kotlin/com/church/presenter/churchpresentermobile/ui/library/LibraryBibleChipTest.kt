package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The chip that says which translation the Bible tab is reading.
 *
 * Which one is read is a choice, not the order they happened to be downloaded
 * in, so the chip both names the active translation and is the place to change
 * it. It is absent until something is downloaded — an empty picker is not a
 * feature — and its menu carries a way to fetch another, because that is the
 * question an operator has when the one they want is not listed.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryBibleChipTest {

    private fun library() = libraryOf(songs = listOf(amazingGrace()))

    // ── Whether the chip is there at all ─────────────────────────────────

    @Test
    fun noChipIsShownWithNoTranslationDownloaded() = runComposeUiTest {
        showLibrary(library(), bibles = biblesWith())

        assertFalse(exists(LibraryTags.BIBLE_CHIP))
    }

    @Test
    fun aDownloadedTranslationBringsTheChip() = runComposeUiTest {
        showLibrary(library(), bibles = biblesWith("King James Version"))

        assertTrue(exists(LibraryTags.BIBLE_CHIP))
    }

    @Test
    fun theChipNamesTheTranslationInUse() = runComposeUiTest {
        showLibrary(library(), bibles = biblesWith("King James Version"))

        assertTrue(isShowing("King James Version"))
    }

    @Test
    fun theChipSitsAlongsideTheOtherTwo() = runComposeUiTest {
        showLibrary(library(), bibles = biblesWith("King James Version"))

        assertTrue(exists(LibraryTags.SYNC_CHIP))
        assertTrue(exists(LibraryTags.BIBLE_CHIP))
        assertTrue(exists(LibraryTags.SHARE_CHIP))
    }

    @Test
    fun theChipIsThereWithSeveralTranslations() = runComposeUiTest {
        showLibrary(library(), bibles = biblesWith("King James Version", "Russian Synodal"))

        assertTrue(exists(LibraryTags.BIBLE_CHIP))
    }

    @Test
    fun theChipIsThereOnAnEmptyLibrary() = runComposeUiTest {
        // A Bible is content too; the chip does not depend on there being songs.
        showLibrary(libraryOf(), bibles = biblesWith("King James Version"))

        assertTrue(exists(LibraryTags.BIBLE_CHIP))
    }

    // ── Its menu ─────────────────────────────────────────────────────────

    @Test
    fun theMenuIsClosedToBeginWith() = runComposeUiTest {
        val bibles = biblesWith("King James Version")
        showLibrary(library(), bibles = bibles)
        val id = bibles.index.value.bibles.first().id

        assertFalse(exists(LibraryTags.bibleMenuItem(id)))
    }

    @Test
    fun tappingTheChipOpensTheMenu() = runComposeUiTest {
        val bibles = biblesWith("King James Version")
        showLibrary(library(), bibles = bibles)
        val id = bibles.index.value.bibles.first().id

        click(LibraryTags.BIBLE_CHIP)

        awaitThat { exists(LibraryTags.bibleMenuItem(id)) }
    }

    @Test
    fun theMenuListsEveryTranslationOnThePhone() = runComposeUiTest {
        val bibles = biblesWith("King James Version", "Russian Synodal")
        showLibrary(library(), bibles = bibles)
        val ids = bibles.index.value.bibles.map { it.id }

        click(LibraryTags.BIBLE_CHIP)

        awaitThat { exists(LibraryTags.bibleMenuItem(ids[0])) }
        assertTrue(exists(LibraryTags.bibleMenuItem(ids[1])))
    }

    @Test
    fun theMenuNamesEachTranslation() = runComposeUiTest {
        showLibrary(library(), bibles = biblesWith("King James Version", "Russian Synodal"))

        click(LibraryTags.BIBLE_CHIP)

        awaitThat { isShowing("Russian Synodal") }
    }

    @Test
    fun theMenuOffersAWayToFetchAnother() = runComposeUiTest {
        // The question an operator has when the one they want is not listed.
        showLibrary(library(), bibles = biblesWith("King James Version"))

        click(LibraryTags.BIBLE_CHIP)

        awaitThat { exists(LibraryTags.BIBLE_MENU_COPY) }
    }

    @Test
    fun fetchingAnotherOpensTheCopySheetOnTheBibleHalf() = runComposeUiTest {
        showLibrary(library(), bibles = biblesWith("King James Version"))
        click(LibraryTags.BIBLE_CHIP)
        awaitThat { exists(LibraryTags.BIBLE_MENU_COPY) }

        click(LibraryTags.BIBLE_MENU_COPY)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun fetchingAnotherClosesTheMenu() = runComposeUiTest {
        val bibles = biblesWith("King James Version")
        showLibrary(library(), bibles = bibles)
        val id = bibles.index.value.bibles.first().id
        click(LibraryTags.BIBLE_CHIP)
        awaitThat { exists(LibraryTags.BIBLE_MENU_COPY) }

        click(LibraryTags.BIBLE_MENU_COPY)

        awaitThat { !exists(LibraryTags.bibleMenuItem(id)) }
    }

    // ── Choosing which one is read ───────────────────────────────────────

    @Test
    fun theFirstTranslationIsTheOneInUse() = runComposeUiTest {
        val bibles = biblesWith("King James Version")
        showLibrary(library(), bibles = bibles)

        awaitThat { bibles.index.value.activeId != null }
    }

    @Test
    fun choosingAnotherTranslationSwitchesToIt() = runComposeUiTest {
        val bibles = biblesWith("King James Version", "Russian Synodal")
        showLibrary(library(), bibles = bibles)
        val second = bibles.index.value.bibles[1].id
        click(LibraryTags.BIBLE_CHIP)
        awaitThat { exists(LibraryTags.bibleMenuItem(second)) }

        click(LibraryTags.bibleMenuItem(second))

        awaitThat { bibles.index.value.activeId == second }
    }

    @Test
    fun choosingAnotherTranslationRenamesTheChip() = runComposeUiTest {
        val bibles = biblesWith("King James Version", "Russian Synodal")
        showLibrary(library(), bibles = bibles)
        val second = bibles.index.value.bibles[1].id
        click(LibraryTags.BIBLE_CHIP)
        awaitThat { exists(LibraryTags.bibleMenuItem(second)) }

        click(LibraryTags.bibleMenuItem(second))

        awaitThat { bibles.index.value.activeId == second }
        assertTrue(isShowing("Russian Synodal"))
    }

    @Test
    fun choosingATranslationClosesTheMenu() = runComposeUiTest {
        val bibles = biblesWith("King James Version", "Russian Synodal")
        showLibrary(library(), bibles = bibles)
        val second = bibles.index.value.bibles[1].id
        click(LibraryTags.BIBLE_CHIP)
        awaitThat { exists(LibraryTags.bibleMenuItem(second)) }

        click(LibraryTags.bibleMenuItem(second))

        awaitThat { !exists(LibraryTags.bibleMenuItem(second)) }
    }

    @Test
    fun choosingTheOneAlreadyInUseChangesNothing() = runComposeUiTest {
        val bibles = biblesWith("King James Version", "Russian Synodal")
        showLibrary(library(), bibles = bibles)
        val first = bibles.index.value.bibles[0].id
        click(LibraryTags.BIBLE_CHIP)
        awaitThat { exists(LibraryTags.bibleMenuItem(first)) }

        click(LibraryTags.bibleMenuItem(first))

        awaitThat { bibles.index.value.activeId == first }
    }

    @Test
    fun theChoiceSurvivesASearch() = runComposeUiTest {
        // The chip belongs to the whole tab, not to the list under it.
        val bibles = biblesWith("King James Version", "Russian Synodal")
        showLibrary(library(), bibles = bibles)
        val second = bibles.index.value.bibles[1].id
        click(LibraryTags.BIBLE_CHIP)
        click(LibraryTags.bibleMenuItem(second))
        awaitThat { bibles.index.value.activeId == second }

        type(LibraryTags.SEARCH, "zzzzz")

        assertEquals(second, bibles.index.value.activeId)
    }

    @Test
    fun theChipStaysWhileTheListIsEmpty() = runComposeUiTest {
        showLibrary(library(), bibles = biblesWith("King James Version"))

        type(LibraryTags.SEARCH, "zzzzz")

        assertTrue(exists(LibraryTags.BIBLE_CHIP))
    }

    @Test
    fun theMenuCanBeOpenedAgainAfterChoosing() = runComposeUiTest {
        val bibles = biblesWith("King James Version", "Russian Synodal")
        showLibrary(library(), bibles = bibles)
        val second = bibles.index.value.bibles[1].id
        click(LibraryTags.BIBLE_CHIP)
        click(LibraryTags.bibleMenuItem(second))
        awaitThat { bibles.index.value.activeId == second }

        click(LibraryTags.BIBLE_CHIP)

        awaitThat { exists(LibraryTags.bibleMenuItem(second)) }
    }

    // ── The sync chip's own label ────────────────────────────────────────

    @Test
    fun aLibraryThatHasNeverBeenCopiedSaysSo() = runComposeUiTest {
        showLibrary(library(), settings = AppSettings(InMemorySettingsStorage()))

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun aRecentCopyIsReportedOnTheChip() = runComposeUiTest {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.librarySyncStateJson =
            """{"lastSyncEpochMs":${1_700_000_000_000L},"songCount":42}"""
        showLibrary(library(), settings = settings)

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun anUnreadableSyncStateStillOpensTheTab() = runComposeUiTest {
        // The blob is written by a previous version and can be anything; a wrong
        // chip is not worth a crash.
        val settings = AppSettings(InMemorySettingsStorage())
        settings.librarySyncStateJson = "{ truncated"
        showLibrary(library(), settings = settings)

        assertTrue(exists(LibraryTags.SYNC_CHIP))
        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun anEmptySyncStateStillOpensTheTab() = runComposeUiTest {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.librarySyncStateJson = ""
        showLibrary(library(), settings = settings)

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun theSyncChipStillOpensTheCopySheet() = runComposeUiTest {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.librarySyncStateJson =
            """{"lastSyncEpochMs":${1_700_000_000_000L},"songCount":42}"""
        showLibrary(library(), settings = settings)

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }
}
