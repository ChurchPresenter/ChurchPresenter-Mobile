package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Songs tab's list — what an operator scans while a service is starting.
 *
 * Everything here is state the screen is handed, so the failures it guards are
 * about *which* of several states it shows: a list that keeps a stale error
 * banner, an empty result that reads as "no songs" when a filter is on, or a
 * card whose tap reports the wrong song. Every one of those puts the wrong
 * words on the screen behind the operator.
 */
/**
 * Opening a song, and which one the screen says is open.
 *
 * Two hymnals can share a number, so a song is identified by its number and
 * its book together — comparing only the number would light up both.
 */
@OptIn(ExperimentalTestApi::class)
class SongsListSelectionTest {
    @Test
    fun tappingASongReportsIt() = runComposeUiTest {
        var tapped: Song? = null
        showSongs(onSongClick = { tapped = it })

        click(card("42"))

        assertEquals("Amazing Grace", tapped?.title)
    }

    @Test
    fun tappingReportsTheSongThatWasTapped() = runComposeUiTest {
        // With a list on screen the callback has to carry the row under the
        // finger, not the first in the list.
        var tapped: Song? = null
        showSongs(onSongClick = { tapped = it })

        click(card("7"))

        assertEquals("Be Thou My Vision", tapped?.title)
    }

    @Test
    fun theSelectedSongIsMarkedAsSuch() = runComposeUiTest {
        showSongs(selectedSong = hymns[0])

        tagged(card("42")).assertIsSelected()
        tagged(card("7")).assertIsNotSelected()
    }

    @Test
    fun nothingIsSelectedBeforeASongIsOpened() = runComposeUiTest {
        showSongs()

        tagged(card("42")).assertIsNotSelected()
    }

    @Test
    fun aSongIsSelectedByItsNumberAndBookTogether() = runComposeUiTest {
        // Two hymnals sharing a number would both light up if only the number
        // were compared.
        val other = song("42", "A different 42", book = "Chorus Book")
        showSongs(songs = listOf(hymns[0], other), selectedSong = other)

        tagged(card("42", book = "Chorus Book")).assertIsSelected()
        tagged(card("42", book = "Hymns")).assertIsNotSelected()
    }

    // ── The error banner ─────────────────────────────────────────────────
}
