package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

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
 * What the Songs tab lists, and in what order.
 *
 * The list is scanned against a printed hymnal while a service is starting, so
 * order and identity are the whole job: 7 has to come before 42, and a card has
 * to say which songbook it is from when two hymnals share a number.
 */
@OptIn(ExperimentalTestApi::class)
class SongsListContentTest {
    @Test
    fun everySongIsListed() = runComposeUiTest {
        showSongs()

        assertTrue(exists(card("42")))
        assertTrue(exists(card("7")))
    }

    @Test
    fun aSongShowsItsNumberAndTitle() = runComposeUiTest {
        showSongs()

        assertTrue(isShowing("42"))
        assertTrue(isShowing("Amazing Grace"))
    }

    @Test
    fun aSongShowsWhichBookItIsFrom() = runComposeUiTest {
        // Two hymnals can both have a number 42; without the book the operator
        // cannot tell which one they are about to project.
        showSongs(songs = listOf(song("42", "Amazing Grace", book = "Ancient & Modern")))

        assertTrue(isShowing("Ancient & Modern"))
    }

    @Test
    fun aSongWithNoBookIsStillListed() = runComposeUiTest {
        showSongs(songs = listOf(song("42", "Amazing Grace", book = null)))

        assertTrue(exists(card("42", book = null)))
        assertTrue(isShowing("Amazing Grace"))
    }

    @Test
    fun songsAreListedInNumberOrder() = runComposeUiTest {
        // The list is scanned against a printed hymnal, so 7 has to come before
        // 42 — string order would put "42" first.
        showSongs()

        val cards = listOf(card("7"), card("42")).map {
            tagged(it).fetchSemanticsNode().positionInRoot.y
        }

        assertTrue(cards[0] < cards[1], "42 was listed above 7")
    }

    @Test
    fun aSongWhoseNumberIsNotANumberIsStillListed() = runComposeUiTest {
        // Hymnals use "42a" and "10b"; sorting must not drop them.
        showSongs(songs = hymns + song("42a", "Amazing Grace (alt)"))

        assertTrue(exists(card("42a")))
    }

    @Test
    fun theCountReportsHowManyAreListed() = runComposeUiTest {
        showSongs()

        assertTrue(isShowing("2"))
        assertTrue(exists(UiTags.SONGS_COUNT))
    }

    // ── Selection ────────────────────────────────────────────────────────

    @Test
    fun anEmptyListSaysSo() = runComposeUiTest {
        // Which of the three empty states, and that it is not one of the
        // others, is SongsListEmptyStatesTest's job.
        showSongs(songs = emptyList())

        assertTrue(exists(UiTags.SONGS_EMPTY_NO_SONGS))
    }
}
