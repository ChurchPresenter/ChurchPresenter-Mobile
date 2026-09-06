package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * What the Library tab lists, and what searching does to it.
 *
 * A search that hides a song the operator can no longer find costs work that
 * exists nowhere else — the library never leaves the device unless someone
 * exports it.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryListingTest {

    // ── What is listed ───────────────────────────────────────────────────

    @Test
    fun everySongInTheLibraryIsListed() = runComposeUiTest {
        showLibrary(
            repositoryWith(
                songs = listOf(
                    song("s1", "42", "Amazing Grace"),
                    song("s2", "7", "Be Thou My Vision"),
                )
            )
        )

        assertTrue(isShowing("42 Amazing Grace"))
        assertTrue(isShowing("7 Be Thou My Vision"))
    }

    @Test
    fun aNoticeIsListedAlongsideTheSongs() = runComposeUiTest {
        showLibrary(
            repositoryWith(
                songs = listOf(song("s1", "42", "Amazing Grace")),
                announcements = listOf(LocalAnnouncement(id = "a1", title = "Bring a dish")),
            )
        )

        assertTrue(isShowing("42 Amazing Grace"))
        assertTrue(isShowing("Bring a dish"))
    }

    @Test
    fun anEmptyLibraryListsNothing() = runComposeUiTest {
        showLibrary(repositoryWith())

        assertTrue(!isShowing("Amazing Grace"))
    }

    // ── Searching ────────────────────────────────────────────────────────

    @Test
    fun searchingNarrowsTheListToWhatMatches() = runComposeUiTest {
        showLibrary(
            repositoryWith(
                songs = listOf(
                    song("s1", "42", "Amazing Grace"),
                    song("s2", "7", "Be Thou My Vision"),
                )
            )
        )

        search("Amazing")

        assertTrue(isShowing("42 Amazing Grace"))
        assertTrue(!isShowing("Be Thou My Vision"))
    }

    @Test
    fun searchingByNumberFindsTheSong() = runComposeUiTest {
        // How an operator with a hymnal in front of them looks a song up.
        showLibrary(
            repositoryWith(
                songs = listOf(
                    song("s1", "42", "Amazing Grace"),
                    song("s2", "7", "Be Thou My Vision"),
                )
            )
        )

        search("42")

        assertTrue(isShowing("42 Amazing Grace"))
        assertTrue(!isShowing("Be Thou My Vision"))
    }

    @Test
    fun clearingTheSearchBringsEverythingBack() = runComposeUiTest {
        // The failure that costs work: a stale filter leaving a song the
        // operator then believes they never wrote.
        showLibrary(
            repositoryWith(
                songs = listOf(
                    song("s1", "42", "Amazing Grace"),
                    song("s2", "7", "Be Thou My Vision"),
                )
            )
        )
        search("Amazing")

        search("")

        assertTrue(isShowing("42 Amazing Grace"))
        assertTrue(isShowing("7 Be Thou My Vision"))
    }

    @Test
    fun aSearchThatMatchesNothingListsNothing() = runComposeUiTest {
        showLibrary(repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace"))))

        search("zzzzz")

        assertTrue(!isShowing("42 Amazing Grace"))
    }

    @Test
    fun searchIgnoresCase() = runComposeUiTest {
        showLibrary(repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace"))))

        search("amazing")

        assertTrue(isShowing("42 Amazing Grace"))
    }

    // ── Typing into the search field ─────────────────────────────────────

    @Test
    fun theSearchFieldAcceptsTyping() = runComposeUiTest {
        showLibrary(repositoryWith(songs = listOf(song("s1", "42", "Amazing Grace"))))

        onNode(hasSetTextAction()).performTextInput("Ama")

        assertTrue(isShowing("42 Amazing Grace"))
    }
}
