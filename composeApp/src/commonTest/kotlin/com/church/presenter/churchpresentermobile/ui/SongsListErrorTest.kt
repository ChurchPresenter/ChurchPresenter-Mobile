package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Songs tab's error banner.
 *
 * It sits above the list rather than replacing it: a blip mid-service must not
 * blank a catalogue the operator is working from.
 */
@OptIn(ExperimentalTestApi::class)
class SongsListErrorTest {

    @Test
    fun theFilterCannotBeOpenedWhenThereAreNoBooks() = runComposeUiTest {
        // An empty menu is not a feature; it just looks broken. Read from the
        // node's state rather than by pressing it: a disabled clickable still
        // publishes a click action, so invoking it would open the menu anyway.
        showSongs(availableBooks = emptyList())

        tagged(UiTags.SONGS_BOOK_FILTER).assertIsNotEnabled()
    }

    @Test
    fun theFilterCanBeOpenedWhenThereAreBooks() = runComposeUiTest {
        showSongs()

        tagged(UiTags.SONGS_BOOK_FILTER).assertIsEnabled()
    }

    // ── Searching ────────────────────────────────────────────────────────

    @Test
    fun typingInTheSearchFieldIsReported() = runComposeUiTest {
        // The screen holds no query of its own — filtering is the caller's job.
        var typed: String? = null
        showSongs(onSearchQueryChange = { typed = it })

        type(UiTags.SONGS_SEARCH, "grace")

        assertEquals("grace", typed)
    }

    @Test
    fun theSearchFieldShowsTheQueryItWasGiven() = runComposeUiTest {
        showSongs(searchQuery = "grace")

        assertTrue(isShowing("grace"))
    }

    // ── Nothing to show ──────────────────────────────────────────────────
}
