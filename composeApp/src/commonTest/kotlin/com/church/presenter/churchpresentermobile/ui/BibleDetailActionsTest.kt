package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
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
 * The action stack on a Bible reading — cast, queue, freeze, clear.
 *
 * Freezing or clearing a screen that is already blank does nothing, and a
 * button that does nothing teaches the operator to distrust the rest.
 */
@OptIn(ExperimentalTestApi::class)
class BibleDetailActionsTest {
    @Test
    fun castingIsReported() = runComposeUiTest {
        var toggled = false
        showBibleDetail(onToggleProjecting = { toggled = true })

        click(UiTags.FAB_CAST)

        assertTrue(toggled)
    }

    @Test
    fun addingToTheScheduleIsReported() = runComposeUiTest {
        var added = false
        showBibleDetail(onAddToSchedule = { added = true })

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        assertTrue(added)
    }

    @Test
    fun holdIsOfferedOnlyWhileSomethingIsProjected() = runComposeUiTest {
        // Freezing a blank screen does nothing.
        showBibleDetail(isProjecting = false)

        assertFalse(exists(UiTags.FAB_HOLD))
    }

    @Test
    fun holdingIsReported() = runComposeUiTest {
        var held = false
        showBibleDetail(isProjecting = true, onToggleHold = { held = true })

        click(UiTags.FAB_HOLD)

        assertTrue(held)
    }

    @Test
    fun clearIsOfferedOnlyWhileSomethingIsProjected() = runComposeUiTest {
        showBibleDetail(isProjecting = false)

        assertFalse(exists(UiTags.FAB_CLEAR_DISPLAY))
    }

    @Test
    fun clearingIsReported() = runComposeUiTest {
        var cleared = false
        showBibleDetail(isProjecting = true, onClearDisplay = { cleared = true })

        click(UiTags.FAB_CLEAR_DISPLAY)

        assertTrue(cleared)
    }
}
