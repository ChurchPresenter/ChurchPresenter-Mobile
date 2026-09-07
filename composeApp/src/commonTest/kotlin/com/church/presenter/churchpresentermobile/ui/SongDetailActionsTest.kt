package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The action stack on a song — cast, queue, clear.
 *
 * Standalone has no desktop running order, so the schedule button is absent
 * there rather than present and broken.
 */
@OptIn(ExperimentalTestApi::class)
class SongDetailActionsTest {

    @Test
    fun theCastButtonIsAlwaysOffered() = runComposeUiTest {
        showSongDetail()

        assertTrue(exists(UiTags.FAB_CAST))
    }

    @Test
    fun castingReportsTheToggle() = runComposeUiTest {
        var toggled = false
        showSongDetail(onToggleProjecting = { toggled = true })

        click(UiTags.FAB_CAST)

        assertTrue(toggled)
    }

    @Test
    fun theScheduleButtonIsOfferedWhenThereIsASchedule() = runComposeUiTest {
        showSongDetail(onAddToSchedule = {})

        assertTrue(exists(UiTags.FAB_ADD_TO_SCHEDULE))
    }

    @Test
    fun theScheduleButtonIsHiddenWhereThereIsNoSchedule() = runComposeUiTest {
        // Standalone has no desktop running order; offering the button would
        // send the operator to a feature that cannot work.
        showSongDetail(onAddToSchedule = null)

        assertFalse(exists(UiTags.FAB_ADD_TO_SCHEDULE))
    }

    @Test
    fun addingToTheScheduleIsReported() = runComposeUiTest {
        var added = false
        showSongDetail(onAddToSchedule = { added = true })

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        assertTrue(added)
    }

    @Test
    fun clearIsOfferedOnlyWhileSomethingIsProjected() = runComposeUiTest {
        // Clearing a screen that is already blank does nothing, and a button
        // that does nothing teaches the operator to distrust the rest.
        showSongDetail(isProjecting = false, onClearDisplay = {})

        assertFalse(exists(UiTags.FAB_CLEAR_DISPLAY))
    }

    @Test
    fun clearAppearsOnceTheSongIsLive() = runComposeUiTest {
        showSongDetail(isProjecting = true, onClearDisplay = {})

        assertTrue(exists(UiTags.FAB_CLEAR_DISPLAY))
    }

    @Test
    fun clearingIsReported() = runComposeUiTest {
        var cleared = false
        showSongDetail(isProjecting = true, onClearDisplay = { cleared = true })

        click(UiTags.FAB_CLEAR_DISPLAY)

        assertTrue(cleared)
    }

    @Test
    fun clearIsHiddenWhereTheScreenCannotBeCleared() = runComposeUiTest {
        showSongDetail(isProjecting = true, onClearDisplay = null)

        assertFalse(exists(UiTags.FAB_CLEAR_DISPLAY))
    }

    @Test
    fun noSelectButtonOnASongDetail() = runComposeUiTest {
        // Multi-select belongs to the Bible, where a range is a real thing to
        // project; a song's verses go up one at a time.
        showSongDetail()

        assertFalse(exists(UiTags.FAB_SELECT))
    }
}
