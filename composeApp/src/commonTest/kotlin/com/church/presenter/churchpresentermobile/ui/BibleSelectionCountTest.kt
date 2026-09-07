package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * How many verses are lined up, and how that reaches the operator.
 *
 * The cast badge is the only place they see the size of what they are about to
 * project.
 */
@OptIn(ExperimentalTestApi::class)
class BibleSelectionCountTest {

    @Test
    fun theSelectionCountAppearsInMultiSelect() = runComposeUiTest {
        showBibleDetail(isMultiSelectMode = true, selectedVerseIndices = setOf(0, 1))

        assertTrue(exists(UiTags.BIBLE_MULTI_SELECT_COUNT))
        assertTrue(isShowing("2"))
    }

    @Test
    fun noCountIsShownWhenNothingIsSelectedYet() = runComposeUiTest {
        // "0 selected" is noise; the bar appears when there is something in it.
        showBibleDetail(isMultiSelectMode = true, selectedVerseIndices = emptySet())

        assertFalse(exists(UiTags.BIBLE_MULTI_SELECT_COUNT))
    }

    @Test
    fun theSelectButtonReportsTheToggle() = runComposeUiTest {
        var toggled = false
        showBibleDetail(onToggleMultiSelect = { toggled = true })

        click(UiTags.FAB_SELECT)

        assertTrue(toggled)
    }

    @Test
    fun theCastButtonCarriesHowManyVersesAreSelected() = runComposeUiTest {
        // The badge is the only place the operator sees the size of what they
        // are about to project.
        showBibleDetail(selectedVerseIndices = setOf(0, 1, 2))

        assertTrue(exists(UiTags.FAB_CAST_BADGE))
        assertTrue(isShowing("3"))
    }

    @Test
    fun noBadgeWhenNothingIsSelected() = runComposeUiTest {
        showBibleDetail(selectedVerseIndices = emptySet())

        assertFalse(exists(UiTags.FAB_CAST_BADGE))
    }

    // ── The action buttons ───────────────────────────────────────────────
}
