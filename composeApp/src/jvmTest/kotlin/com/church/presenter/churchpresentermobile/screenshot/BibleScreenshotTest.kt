package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.ui.BibleBooksScreen
import com.church.presenter.churchpresentermobile.ui.BibleDetailScreen
import com.church.presenter.churchpresentermobile.ui.book
import com.church.presenter.churchpresentermobile.ui.chapterOne
import com.church.presenter.churchpresentermobile.ui.genesis
import kotlin.test.Test

/**
 * The Bible tab: the book list, and a chapter once it is open.
 *
 * The detail screen is the one an operator drives a reading from, and it holds
 * more state than any other: a chapter selected or not, verses picked one at a
 * time or in a multi-select run, live, held, and already in the schedule. Each
 * of those paints something different.
 */
class BibleScreenshotTest {

    private val books = listOf(
        book("Genesis"),
        book("Exodus", chapters = 40),
        book("Leviticus", chapters = 27),
        book("Numbers", chapters = 36),
        book("Psalms", chapters = 150),
    )

    private fun booksScreen(
        books: List<BibleBook> = this.books,
        searchQuery: String = "",
        isLoading: Boolean = false,
    ): @Composable () -> Unit = {
        BibleBooksScreen(
            books = books,
            searchQuery = searchQuery,
            onSearchQueryChange = {},
            onBookSelect = {},
            isLoading = isLoading,
        )
    }

    private fun detail(
        selectedChapter: Int? = 1,
        verses: List<BibleVerse> = chapterOne,
        isLoading: Boolean = false,
        isProjecting: Boolean = false,
        isHolding: Boolean = false,
        scheduleAdded: Boolean = false,
        selectedVerseIndices: Set<Int> = emptySet(),
        projectedVerseIndex: Int? = null,
        isMultiSelectMode: Boolean = false,
    ): @Composable () -> Unit = {
        BibleDetailScreen(
            book = genesis,
            selectedChapter = selectedChapter,
            verses = verses,
            isLoading = isLoading,
            isProjecting = isProjecting,
            isHolding = isHolding,
            scheduleAdded = scheduleAdded,
            selectedVerseIndices = selectedVerseIndices,
            projectedVerseIndex = projectedVerseIndex,
            isMultiSelectMode = isMultiSelectMode,
            onToggleMultiSelect = {},
            onChapterSelect = {},
            onVerseToggleSelection = {},
            onToggleProjecting = {},
            onToggleHold = {},
            onClearDisplay = {},
            onAddToSchedule = {},
        )
    }

    // ── The book list ────────────────────────────────────────────────────

    @Test
    fun books() = screenshot("bible-books__list", content = booksScreen())

    @Test
    fun booksLoading() = screenshot(
        "bible-books__loading",
        content = booksScreen(books = emptyList(), isLoading = true),
    )

    @Test
    fun booksSearching() = screenshot(
        "bible-books__searching",
        content = booksScreen(books = listOf(book("Psalms", chapters = 150)), searchQuery = "psa"),
    )

    @Test
    fun booksNoMatches() = screenshot(
        "bible-books__no-matches",
        content = booksScreen(books = emptyList(), searchQuery = "zzz"),
    )

    // ── An open chapter ──────────────────────────────────────────────────

    @Test
    fun chapterOpen() = screenshot("bible-detail__chapter", content = detail())

    @Test
    fun noChapterChosen() = screenshot(
        // The grid of chapter numbers, before a reading is picked.
        "bible-detail__chapter-picker",
        content = detail(selectedChapter = null, verses = emptyList()),
    )

    @Test
    fun chapterLoading() = screenshot(
        "bible-detail__loading",
        content = detail(verses = emptyList(), isLoading = true),
    )

    @Test
    fun verseSelected() = screenshot(
        "bible-detail__verse-selected",
        content = detail(selectedVerseIndices = setOf(1)),
    )

    @Test
    fun verseLive() = screenshot(
        "bible-detail__projecting",
        content = detail(selectedVerseIndices = setOf(1), projectedVerseIndex = 1, isProjecting = true),
    )

    @Test
    fun held() = screenshot(
        // Live but frozen: the display keeps the verse while the operator moves
        // on, so the screen has to show both facts at once.
        "bible-detail__held",
        content = detail(
            selectedVerseIndices = setOf(2),
            projectedVerseIndex = 1,
            isProjecting = true,
            isHolding = true,
        ),
    )

    @Test
    fun multiSelectRun() = screenshot(
        "bible-detail__multi-select",
        content = detail(isMultiSelectMode = true, selectedVerseIndices = setOf(0, 1, 2)),
    )

    @Test
    fun alreadyInSchedule() = screenshot(
        "bible-detail__in-schedule",
        content = detail(selectedVerseIndices = setOf(0), scheduleAdded = true),
    )

    @Test
    fun tablet() = screenshot(
        "bible-detail__tablet",
        width = Screenshots.TABLET_WIDTH,
        content = detail(selectedVerseIndices = setOf(0)),
    )
}
