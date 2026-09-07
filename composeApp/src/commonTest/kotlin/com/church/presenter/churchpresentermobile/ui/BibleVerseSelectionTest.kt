package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * A Bible book once it is open — the chapter grid, then the verses.
 *
 * The verse list is the surface an operator drives during a reading, and the
 * things it must not get wrong are all about what the congregation can see: a
 * verse highlighted that is not projected, a multi-select count that disagrees
 * with the selection, a Clear button offered when the screen is already blank.
 */
/**
 * Which verses are lined up, and which one the congregation can see.
 *
 * Selection is what the operator lined up; projection is what is on the screen.
 * Losing the first when the second stops would drop their work mid-reading.
 */
@OptIn(ExperimentalTestApi::class)
class BibleVerseSelectionTest {
    @Test
    fun aSelectedVerseIsMarkedAsSuch() = runComposeUiTest {
        showBibleDetail(selectedVerseIndices = setOf(1))

        tagged(UiTags.bibleVerse(1)).assertIsSelected()
        tagged(UiTags.bibleVerse(0)).assertIsNotSelected()
    }

    @Test
    fun theProjectedVerseIsMarkedWhileLive() = runComposeUiTest {
        showBibleDetail(isProjecting = true, projectedVerseIndex = 2)

        tagged(UiTags.bibleVerse(2)).assertIsSelected()
    }

    @Test
    fun theProjectedVerseIsNotMarkedWhenNothingIsLive() = runComposeUiTest {
        // A verse left highlighted after Clear would say the congregation is
        // still looking at it.
        showBibleDetail(isProjecting = false, projectedVerseIndex = 2)

        tagged(UiTags.bibleVerse(2)).assertIsNotSelected()
    }

    @Test
    fun aSelectedVerseStaysMarkedWhetherOrNotItIsLive() = runComposeUiTest {
        // Selection is what the operator lined up; projection is what is on the
        // screen. Losing the first when the second stops would drop their work.
        showBibleDetail(isProjecting = false, selectedVerseIndices = setOf(1))

        tagged(UiTags.bibleVerse(1)).assertIsSelected()
    }

    // ── Selecting a range ────────────────────────────────────────────────

    @Test
    fun noSelectionCountBeforeMultiSelectIsOn() = runComposeUiTest {
        showBibleDetail(selectedVerseIndices = setOf(0, 1))

        assertFalse(exists(UiTags.BIBLE_MULTI_SELECT_COUNT))
    }
}
