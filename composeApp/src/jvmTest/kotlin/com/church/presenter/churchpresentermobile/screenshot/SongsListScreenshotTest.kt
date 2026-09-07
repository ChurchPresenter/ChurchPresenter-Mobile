package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.ui.SongsListScreen
import kotlin.test.Test

/**
 * [SongsListScreen] in each state the operator can walk into.
 *
 * This is the first screen most people open and the one they are looking at when
 * the service starts, so every one of its states is worth a picture: the wait
 * before the desktop answers, the answer never arriving, a library with nothing
 * in it, a filter that matched nothing, and the ordinary full list.
 *
 * The screen takes plain data and callbacks — no ViewModel — so each state here
 * is just a different set of arguments.
 */
class SongsListScreenshotTest {

    private val songs = listOf(
        Song(id = 1, number = "1", title = "Amazing Grace", author = "John Newton", bookName = "Hymns"),
        Song(id = 2, number = "23", title = "Be Thou My Vision", author = "Dallán Forgaill", bookName = "Hymns"),
        Song(id = 3, number = "104", title = "How Great Thou Art", author = "Carl Boberg", bookName = "Hymns"),
        Song(id = 4, number = "7", title = "In Christ Alone", author = "Keith Getty", bookName = "Modern"),
        Song(id = 5, number = "88", title = "Great Is Thy Faithfulness", bookName = "Hymns"),
    )

    private fun songsList(
        songs: List<Song> = this.songs,
        selectedSong: Song? = null,
        isLoading: Boolean = false,
        error: String? = null,
        searchQuery: String = "",
        selectedBook: String? = null,
        hasActiveFilter: Boolean = false,
        showsLocalLibrary: Boolean = false,
    ): @Composable () -> Unit = {
        SongsListScreen(
            songs = songs,
            selectedSong = selectedSong,
            isLoading = isLoading,
            error = error,
            searchQuery = searchQuery,
            selectedBook = selectedBook,
            availableBooks = listOf("Hymns", "Modern"),
            hasActiveFilter = hasActiveFilter,
            onSearchQueryChange = {},
            onBookSelected = {},
            onSongClick = {},
            onRefresh = {},
            showsLocalLibrary = showsLocalLibrary,
        )
    }

    @Test
    fun populated() = screenshot("songs-list__populated", content = songsList())

    @Test
    fun loading() = screenshot("songs-list__loading", content = songsList(songs = emptyList(), isLoading = true))

    @Test
    fun error() = screenshot(
        "songs-list__error",
        content = songsList(
            songs = emptyList(),
            error = "Could not reach your computer. Check that both devices are on the same Wi-Fi.",
        ),
    )

    @Test
    fun emptyLibrary() = screenshot("songs-list__empty", content = songsList(songs = emptyList()))

    @Test
    fun searching() = screenshot(
        "songs-list__searching",
        content = songsList(songs = songs.take(1), searchQuery = "amazing", hasActiveFilter = true),
    )

    @Test
    fun filterMatchedNothing() = screenshot(
        "songs-list__no-matches",
        content = songsList(songs = emptyList(), searchQuery = "zzz", hasActiveFilter = true),
    )

    @Test
    fun bookFilterApplied() = screenshot(
        "songs-list__book-filter",
        content = songsList(
            songs = songs.filter { it.bookName == "Modern" },
            selectedBook = "Modern",
            hasActiveFilter = true,
        ),
    )

    @Test
    fun withSelection() = screenshot("songs-list__selected", content = songsList(selectedSong = songs[1]))

    @Test
    fun localLibrary() = screenshot(
        "songs-list__local-library",
        // Standalone mode: the list is this device's own library, which the
        // screen says so the operator knows nothing is coming from a desktop.
        content = songsList(showsLocalLibrary = true),
    )

    @Test
    fun onATabletWidth() = screenshot(
        "songs-list__tablet",
        width = Screenshots.TABLET_WIDTH,
        content = songsList(),
    )
}
