package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.Song

/** Setup for the Songs tab's list — a stateless screen handed all of its state. */
internal fun song(number: String, title: String, book: String? = "Hymns") =
    Song(number = number, title = title, bookName = book)

internal val hymns = listOf(
    song("42", "Amazing Grace"),
    song("7", "Be Thou My Vision"),
)

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showSongs(
    songs: List<Song> = hymns,
    selectedSong: Song? = null,
    isLoading: Boolean = false,
    error: String? = null,
    searchQuery: String = "",
    selectedBook: String? = null,
    availableBooks: List<String> = listOf("Hymns", "Chorus Book"),
    hasActiveFilter: Boolean = false,
    showsLocalLibrary: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onBookSelected: (String?) -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    onRefresh: () -> Unit = {},
) = showScreen {
    SongsListScreen(
        songs = songs,
        selectedSong = selectedSong,
        isLoading = isLoading,
        error = error,
        searchQuery = searchQuery,
        selectedBook = selectedBook,
        availableBooks = availableBooks,
        hasActiveFilter = hasActiveFilter,
        onSearchQueryChange = onSearchQueryChange,
        onBookSelected = onBookSelected,
        onSongClick = onSongClick,
        onRefresh = onRefresh,
        showsLocalLibrary = showsLocalLibrary,
    )
}

internal fun card(number: String, book: String? = "Hymns") = UiTags.songCard(number, book)
