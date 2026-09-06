package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The verses of an open chapter.
 */
@OptIn(ExperimentalTestApi::class)
class BibleChapterVersesTest {

    @Test
    fun everyVerseOfTheChapterIsTappable() = runComposeUiTest {
        // Asserted by tapping each one rather than by looking for it: the rows
        // live in a lazy list inside a pager, and only the tap proves the row
        // is both there and wired to the right index.
        val tapped = mutableListOf<Int>()
        showBibleDetail(onVerseToggleSelection = { tapped += it })

        click(UiTags.bibleVerse(0))
        click(UiTags.bibleVerse(1))
        click(UiTags.bibleVerse(2))

        assertEquals(listOf(0, 1, 2), tapped)
    }

    @Test
    fun aVerseShowsItsWords() = runComposeUiTest {
        showBibleDetail()

        assertTrue(isShowing("In the beginning God created"))
        assertTrue(isShowing("And God said, Let there be light"))
    }

    @Test
    fun tappingAVerseReportsItsPosition() = runComposeUiTest {
        var toggled: Int? = null
        showBibleDetail(onVerseToggleSelection = { toggled = it })

        click(UiTags.bibleVerse(2))

        assertEquals(2, toggled)
    }

    @Test
    fun aChapterWithNoVersesSaysSo() = runComposeUiTest {
        showBibleDetail(verses = emptyList())

        assertTrue(exists(UiTags.BIBLE_NO_VERSES))
    }

    @Test
    fun theEmptyMessageIsGoneOnceVersesArrive() = runComposeUiTest {
        showBibleDetail()

        assertFalse(exists(UiTags.BIBLE_NO_VERSES))
    }

    // ── What the congregation is looking at ──────────────────────────────

    @Test
    fun theActionButtonsAreGoneWhileChoosingAChapter() = runComposeUiTest {
        // There is nothing to project until a chapter is open.
        showBibleDetail(selectedChapter = null)

        assertFalse(exists(UiTags.FAB_CAST))
    }
}
