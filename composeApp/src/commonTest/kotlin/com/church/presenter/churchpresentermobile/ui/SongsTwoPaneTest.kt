package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Songs tab's tablet arrangement.
 *
 * The thing the phone layout could never get wrong, and this one can: both halves
 * are on screen at once. So what is asserted is that they *both* are — a split
 * that quietly dropped one would look like the phone layout and pass any test
 * that only checked the visible half.
 */
@OptIn(ExperimentalTestApi::class)
class SongsTwoPaneTest {

    private val songs = listOf(
        song("1", "Amazing Grace"),
        song("23", "Be Thou My Vision"),
        song("104", "How Great Thou Art"),
    )

    @Test
    fun bothPanesAreOnScreenAtOnce() = runComposeUiTest {
        showTwoPane(showDetail = true)

        // The list, by a row only it draws...
        assertTrue(exists(card("23")), "the list pane is missing")
        // ...and the detail, by a verse only it draws.
        assertTrue(exists(UiTags.verseCard(0)), "the detail pane is missing")
    }

    @Test
    fun theListStaysWhileASongIsOpen() = runComposeUiTest {
        // On a phone opening a song replaced the list. Here it must not.
        showTwoPane(showDetail = true)

        songs.forEach { assertTrue(exists(card(it.number)), "${it.title} fell out of the list") }
    }

    @Test
    fun nothingOpenShowsTheHintRatherThanAnEmptyHalf() = runComposeUiTest {
        showTwoPane(showDetail = false)

        assertTrue(exists(card("1")), "the list should still be there")
        assertFalse(exists(UiTags.verseCard(0)), "no song is open, so there are no verses")
    }

    @Test
    fun onlyTheOpenSongIsMarkedInTheList() = runComposeUiTest {
        // The bug this whole arrangement exposed: selection was compared on
        // number + songbook, so a run of songs sharing those all lit up.
        var clicked: Song? = null
        showTwoPane(showDetail = true, selectedSong = songs[1], onSongClick = { clicked = it })

        click(card("104"))

        assertEquals(songs[2].identity, clicked?.identity)
    }

    @Test
    fun tappingARowReportsThatRow() = runComposeUiTest {
        var clicked: Song? = null
        showTwoPane(showDetail = true, onSongClick = { clicked = it })

        click(card("1"))
        assertEquals("Amazing Grace", clicked?.title)

        click(card("23"))
        assertEquals("Be Thou My Vision", clicked?.title)
    }

    @Test
    fun theSchedulesAndSettingsButtonsReachTheirCallbacks() = runComposeUiTest {
        // The list pane carries the tab's own header, because the shell draws
        // none for a split tab. If these are not wired the operator loses the
        // schedule drawer and settings entirely on a tablet.
        var menu = 0
        var settings = 0
        showTwoPane(showDetail = true, onMenu = { menu++ }, onSettings = { settings++ })

        click(UiTags.HEADER_MENU)
        click(UiTags.HEADER_SETTINGS)

        assertEquals(1, menu)
        assertEquals(1, settings)
    }

    private fun ComposeUiTest.showTwoPane(
        showDetail: Boolean,
        selectedSong: Song? = null,
        onSongClick: (Song) -> Unit = {},
        onMenu: () -> Unit = {},
        onSettings: () -> Unit = {},
    ) = showScreen {
        // A fixed frame, because both panes fill what they are given and the
        // test surface would otherwise leave the detail half at zero width.
        Box(Modifier.size(width = 1200.dp, height = 800.dp)) {
            SongsTwoPane(
                showDetail = showDetail,
                detailTitle = "Amazing Grace".takeIf { showDetail },
                detailSubtitle = "Hymns",
                onMenu = onMenu,
                onSettings = onSettings,
                listPane = {
                    SongsListScreen(
                        songs = songs,
                        selectedSong = selectedSong,
                        isLoading = false,
                        error = null,
                        searchQuery = "",
                        selectedBook = null,
                        availableBooks = listOf("Hymns"),
                        hasActiveFilter = false,
                        onSearchQueryChange = {},
                        onBookSelected = {},
                        onSongClick = onSongClick,
                        onRefresh = {},
                        showsLocalLibrary = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                detailPane = { detailPane() },
            )
        }
    }

    private val detailPane: @Composable () -> Unit = {
        SongDetailScreen(
            detail = com.church.presenter.churchpresentermobile.model.SongDetail(
                title = "Amazing Grace",
                verses = listOf(
                    com.church.presenter.churchpresentermobile.model.SongVerse(
                        type = "verse",
                        text = "Amazing grace! how sweet the sound",
                    ),
                ),
            ),
            isLoading = false,
            error = null,
            selectedVerseIndex = null,
            isProjecting = false,
            scheduleAdded = false,
            onVerseSelected = {},
            onToggleProjecting = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
