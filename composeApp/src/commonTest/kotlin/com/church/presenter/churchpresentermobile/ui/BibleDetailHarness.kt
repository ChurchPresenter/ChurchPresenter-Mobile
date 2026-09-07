package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse

/** Setup for a Bible book once it is open. */
internal val genesis = BibleBook(name = "Genesis", chapterTotal = 50)

internal val chapterOne = listOf(
    BibleVerse(verse = 1, text = "In the beginning God created the heaven and the earth"),
    BibleVerse(verse = 2, text = "And the earth was without form, and void"),
    BibleVerse(verse = 3, text = "And God said, Let there be light"),
)

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showBibleDetail(
    book: BibleBook = genesis,
    selectedChapter: Int? = 1,
    verses: List<BibleVerse> = chapterOne,
    isLoading: Boolean = false,
    isProjecting: Boolean = false,
    isHolding: Boolean = false,
    scheduleAdded: Boolean = false,
    selectedVerseIndices: Set<Int> = emptySet(),
    projectedVerseIndex: Int? = null,
    isMultiSelectMode: Boolean = false,
    onToggleMultiSelect: () -> Unit = {},
    onChapterSelect: (Int) -> Unit = {},
    onVerseToggleSelection: (Int) -> Unit = {},
    onToggleProjecting: () -> Unit = {},
    onToggleHold: () -> Unit = {},
    onClearDisplay: () -> Unit = {},
    onAddToSchedule: () -> Unit = {},
) = showScreen {
    BibleDetailScreen(
        book = book,
        selectedChapter = selectedChapter,
        verses = verses,
        isLoading = isLoading,
        isProjecting = isProjecting,
        isHolding = isHolding,
        scheduleAdded = scheduleAdded,
        selectedVerseIndices = selectedVerseIndices,
        projectedVerseIndex = projectedVerseIndex,
        isMultiSelectMode = isMultiSelectMode,
        onToggleMultiSelect = onToggleMultiSelect,
        onChapterSelect = onChapterSelect,
        onVerseToggleSelection = onVerseToggleSelection,
        onToggleProjecting = onToggleProjecting,
        onToggleHold = onToggleHold,
        onClearDisplay = onClearDisplay,
        onAddToSchedule = onAddToSchedule,
    )
}
