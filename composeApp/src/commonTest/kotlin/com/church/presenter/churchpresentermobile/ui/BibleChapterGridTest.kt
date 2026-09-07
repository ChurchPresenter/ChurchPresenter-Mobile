package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.BibleBook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A Bible book once it is open — the chapter grid, then the verses.
 *
 * The verse list is the surface an operator drives during a reading, and the
 * things it must not get wrong are all about what the congregation can see: a
 * verse highlighted that is not projected, a multi-select count that disagrees
 * with the selection, a Clear button offered when the screen is already blank.
 */
/**
 * Choosing a chapter, and reading it.
 *
 * A zero-based grid would send the operator one chapter off every time, and a
 * book that does not declare a chapter count still has to be openable.
 */
@OptIn(ExperimentalTestApi::class)
class BibleChapterGridTest {
    @Test
    fun aBookWithNoChapterChosenOffersTheGrid() = runComposeUiTest {
        showBibleDetail(selectedChapter = null)

        assertTrue(exists(UiTags.BIBLE_CHAPTERS_GRID))
    }

    @Test
    fun theGridOffersEveryChapterTheBookHas() = runComposeUiTest {
        showBibleDetail(selectedChapter = null)

        assertTrue(exists(UiTags.bibleChapter(1)))
        assertTrue(exists(UiTags.bibleChapter(2)))
    }

    @Test
    fun tappingAChapterReportsIt() = runComposeUiTest {
        var picked: Int? = null
        showBibleDetail(selectedChapter = null, onChapterSelect = { picked = it })

        click(UiTags.bibleChapter(A_CHAPTER))

        assertEquals(A_CHAPTER, picked)
    }

    @Test
    fun chaptersAreNumberedFromOne() = runComposeUiTest {
        // A zero-based grid would send the operator one chapter off every time.
        showBibleDetail(selectedChapter = null)

        assertFalse(exists(UiTags.bibleChapter(0)))
        assertTrue(exists(UiTags.bibleChapter(1)))
    }

    @Test
    fun aBookThatDoesNotSayHowManyChaptersStillOffersSome() = runComposeUiTest {
        // Falls back to 150 — the longest book there is — rather than an empty
        // grid the operator cannot get past.
        showBibleDetail(book = BibleBook(name = "Psalms", chapterTotal = 0), selectedChapter = null)

        assertTrue(exists(UiTags.bibleChapter(1)))
        assertTrue(exists(UiTags.BIBLE_CHAPTERS_GRID))
    }

    @Test
    fun theGridIsGoneOnceAChapterIsOpen() = runComposeUiTest {
        showBibleDetail(selectedChapter = 1)

        assertFalse(exists(UiTags.BIBLE_CHAPTERS_GRID))
    }

    // ── The verses ───────────────────────────────────────────────────────

    private companion object {
        /** Any chapter that is not the first — the first would pass by accident. */
        const val A_CHAPTER = 3
    }
}
