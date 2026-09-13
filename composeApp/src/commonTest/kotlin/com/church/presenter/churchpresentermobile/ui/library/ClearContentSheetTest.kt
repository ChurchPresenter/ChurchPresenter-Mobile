package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Emptying the phone of content.
 *
 * None of it can be undone, so the sheet has to offer only the wipes there is
 * something to wipe, ask before each, do nothing until it is told to, and
 * then actually empty the repository — which is what is asserted on, not the
 * sheet.
 */
@OptIn(ExperimentalTestApi::class)
class ClearContentSheetTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    private fun ComposeUiTest.showSheet(
        repository: LibraryRepository = libraryOf(),
        bibles: LocalBibleRepository = LocalBibleRepository(InMemoryFileStorage()),
        sender: FakeWsSender = FakeWsSender(),
        onDismiss: () -> Unit = {},
    ) = showScreen {
        ClearContentSheet(
            repository = repository,
            bibles = bibles,
            settings = settings(),
            sender = sender,
            onDismiss = onDismiss,
        )
    }

    // ── What is offered ──────────────────────────────────────────────────

    @Test
    fun anEmptyPhoneHasNothingToWipe() = runComposeUiTest {
        showSheet()

        awaitThat { exists(LibraryTags.CLEAR_NONE) }
        assertFalse(exists(LibraryTags.CLEAR_SONGS))
        assertFalse(exists(LibraryTags.CLEAR_BIBLES))
        assertFalse(exists(LibraryTags.CLEAR_ALL))
    }

    @Test
    fun songsAloneOfferTheSongWipeAndEverything() = runComposeUiTest {
        showSheet(repository = libraryOf(songs = listOf(amazingGrace())))

        awaitThat { exists(LibraryTags.CLEAR_SONGS) }
        assertTrue(exists(LibraryTags.CLEAR_ALL))
        assertFalse(exists(LibraryTags.CLEAR_BIBLES))
        assertFalse(exists(LibraryTags.CLEAR_NONE))
    }

    @Test
    fun biblesAloneOfferTheBibleWipeAndEverything() = runComposeUiTest {
        showSheet(bibles = biblesWith("KJV"))

        awaitThat { exists(LibraryTags.CLEAR_BIBLES) }
        assertTrue(exists(LibraryTags.CLEAR_ALL))
        assertFalse(exists(LibraryTags.CLEAR_SONGS))
    }

    // ── Asking first ─────────────────────────────────────────────────────

    @Test
    fun aWipeAsksBeforeDoingAnything() = runComposeUiTest {
        val repository = libraryOf(songs = listOf(amazingGrace()))
        showSheet(repository = repository)

        awaitThat { exists(LibraryTags.CLEAR_SONGS) }
        click(LibraryTags.CLEAR_SONGS)

        assertTrue(exists(LibraryTags.CLEAR_CONFIRM))
        assertEquals(1, repository.songs.size)
    }

    @Test
    fun cancellingTheQuestionWipesNothing() = runComposeUiTest {
        val repository = libraryOf(songs = listOf(amazingGrace()))
        showSheet(repository = repository)

        awaitThat { exists(LibraryTags.CLEAR_SONGS) }
        click(LibraryTags.CLEAR_SONGS)
        click(LibraryTags.CLEAR_CANCEL)

        assertFalse(exists(LibraryTags.CLEAR_CONFIRM))
        assertEquals(1, repository.songs.size)
    }

    // ── Wiping ───────────────────────────────────────────────────────────

    @Test
    fun confirmingTheSongWipeEmptiesTheSongs() = runComposeUiTest {
        val repository = libraryOf(songs = listOf(amazingGrace()))
        showSheet(repository = repository)

        awaitThat { exists(LibraryTags.CLEAR_SONGS) }
        click(LibraryTags.CLEAR_SONGS)
        click(LibraryTags.CLEAR_CONFIRM)

        awaitThat { repository.songs.isEmpty() }
        awaitThat { exists(LibraryTags.CLEAR_OUTCOME) }
    }

    @Test
    fun confirmingTheBibleWipeEmptiesTheBibles() = runComposeUiTest {
        val bibles = biblesWith("KJV", "ESV")
        showSheet(bibles = bibles)

        awaitThat { exists(LibraryTags.CLEAR_BIBLES) }
        click(LibraryTags.CLEAR_BIBLES)
        click(LibraryTags.CLEAR_CONFIRM)

        awaitThat { bibles.index.value.bibles.isEmpty() }
    }

    @Test
    fun confirmingEverythingEmptiesBoth() = runComposeUiTest {
        val repository = libraryOf(songs = listOf(amazingGrace()))
        val bibles = biblesWith("KJV")
        showSheet(repository = repository, bibles = bibles)

        awaitThat { exists(LibraryTags.CLEAR_ALL) }
        click(LibraryTags.CLEAR_ALL)
        click(LibraryTags.CLEAR_CONFIRM)

        awaitThat { repository.songs.isEmpty() && bibles.index.value.bibles.isEmpty() }
    }

    @Test
    fun aWipeClearsTheAudienceScreenToo() = runComposeUiTest {
        // Whatever was deleted must not still be up on the projector.
        val sender = FakeWsSender()
        showSheet(repository = libraryOf(songs = listOf(amazingGrace())), sender = sender)

        awaitThat { exists(LibraryTags.CLEAR_SONGS) }
        click(LibraryTags.CLEAR_SONGS)
        click(LibraryTags.CLEAR_CONFIRM)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.CLEAR, sender.lastType)
    }
}
