package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Editing and deleting from the Library tab.
 *
 * This is the one screen where content can be lost: the library is the only
 * copy, so a delete that skips its confirmation destroys work a church typed in
 * by hand.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryRowActionsTest {

    // ── Editing and deleting ─────────────────────────────────────────────

    @Test
    fun eachRowOffersExactlyTwoActions() = runComposeUiTest {
        // Delete and Edit. A third would mean something new became reachable
        // from the library without anyone deciding it should be.
        showLibrary(repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace"))))

        assertEquals(2, actionsOnRow("42 Amazing Grace").size)
    }

    @Test
    fun everySongGetsItsOwnPairOfActions() = runComposeUiTest {
        showLibrary(
            repositoryWith(
                songs = listOf(song("s1", "42", "Amazing Grace"), song("s2", "7", "Be Thou My Vision")),
            )
        )

        assertEquals(2, actionsOnRow("42 Amazing Grace").size)
        assertEquals(2, actionsOnRow("7 Be Thou My Vision").size)
    }

    @Test
    fun editingASongReportsWhichOne() = runComposeUiTest {
        var edited: String? = null
        showLibrary(
            repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace"))),
            onEditSong = { edited = it },
        )

        actionsOnRow("42 Amazing Grace").last().performClick()

        assertEquals("s1", edited)
    }

    @Test
    fun editingReportsTheRowThatWasTapped() = runComposeUiTest {
        // With two songs listed, the callback has to carry the one beside the
        // button rather than the first in the list.
        var edited: String? = null
        showLibrary(
            repositoryWith(
                songs = listOf(song("s1", "42", "Amazing Grace"), song("s2", "7", "Be Thou My Vision")),
            ),
            onEditSong = { edited = it },
        )

        actionsOnRow("7 Be Thou My Vision").last().performClick()

        assertEquals("s2", edited)
    }

    @Test
    fun deletingAsksBeforeItRemovesAnything() = runComposeUiTest {
        // The library is the only copy. One stray tap must not be enough.
        val repository = repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace")))
        showLibrary(repository)

        actionsOnRow("42 Amazing Grace").first().performClick()

        assertNotNull(repository.song("s1"), "the song went without a confirmation")
    }

    @Test
    fun theDeleteTapOpensAConfirmation() = runComposeUiTest {
        val repository = repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace")))
        showLibrary(repository)
        val before = clickableCount()

        actionsOnRow("42 Amazing Grace").first().performClick()

        assertTrue(clickableCount() > before, "no confirmation appeared")
    }

    @Test
    fun confirmingTheDeleteRemovesTheSong() = runComposeUiTest {
        val repository = repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace")))
        showLibrary(repository)
        actionsOnRow("42 Amazing Grace").first().performClick()

        // The dialog's confirm and dismiss are the two clickables it added.
        val all = onAllNodes(hasClickAction())
        val count = all.fetchSemanticsNodes().size
        all[count - 2].performClick()

        assertNull(repository.song("s1"))
    }

    @Test
    fun dismissingTheConfirmationKeepsTheSong() = runComposeUiTest {
        // The way out of a mistap, and the reason the dialog exists at all.
        val repository = repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace")))
        showLibrary(repository)
        actionsOnRow("42 Amazing Grace").first().performClick()

        val all = onAllNodes(hasClickAction())
        all[all.fetchSemanticsNodes().size - 1].performClick()

        assertNotNull(repository.song("s1"))
        assertTrue(isShowing("42 Amazing Grace"))
    }

    @Test
    fun aRowDoesNotProjectWhenTapped() = runComposeUiTest {
        // Browsing your own library used to put the song straight on the
        // audience screen. Going live belongs to the Songs tab.
        val sender = FakeWsSender()
        showLibrary(repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace"))), sender = sender)

        onNodeWithText("42 Amazing Grace", substring = true).performClick()

        assertTrue(sender.calls.isEmpty(), "tapping a row sent ${sender.calls}")
    }
}
