package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.BibleBook

/** Setup for the Bible tab's book list. */
internal fun book(name: String, chapters: Int = 50) =
    BibleBook(name = name, chapterTotal = chapters)

internal val pentateuch = listOf(book("Genesis"), book("Exodus", chapters = 40))

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showBooks(
    books: List<BibleBook> = pentateuch,
    searchQuery: String = "",
    isLoading: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
    onBookSelect: (BibleBook) -> Unit = {},
) = showScreen {
    BibleBooksScreen(
        books = books,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onBookSelect = onBookSelect,
        isLoading = isLoading,
    )
}
