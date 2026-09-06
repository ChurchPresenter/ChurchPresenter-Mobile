package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The song's words, and the buttons that put them on the screen.
 *
 * This is the surface an operator drives during a service: tap a verse, the
 * congregation sees it. The states it has to keep straight are all about not
 * lying — a verse marked live that is not, a Clear button offered when nothing
 * is projected, a schedule button on a build that has no schedule to add to.
 */
/**
 * Putting a verse on the audience screen.
 *
 * A verse marked live that is not, or a card that can be tapped when there is
 * nothing to project onto, both lie to the operator mid-service.
 */
@OptIn(ExperimentalTestApi::class)
class SongDetailProjectionTest {
    @Test
    fun aVerseIsNotSelectableUntilTheSongIsLive() = runComposeUiTest {
        // Tapping a verse projects it. Before the song is live there is nothing
        // to project it onto, and a tap that does nothing reads as a broken card.
        showSongDetail(isProjecting = false)

        tagged(UiTags.verseCard(0)).assertIsNotEnabled()
    }

    @Test
    fun aVerseIsSelectableWhileTheSongIsLive() = runComposeUiTest {
        showSongDetail(isProjecting = true)

        tagged(UiTags.verseCard(0)).assertIsEnabled()
    }

    @Test
    fun tappingAVerseReportsItsPosition() = runComposeUiTest {
        var picked: Int? = null
        showSongDetail(isProjecting = true, onVerseSelected = { picked = it })

        click(UiTags.verseCard(2))

        assertEquals(2, picked)
    }

    @Test
    fun theLiveVerseIsMarkedAsSelected() = runComposeUiTest {
        showSongDetail(isProjecting = true, selectedVerseIndex = 1)

        tagged(UiTags.verseCard(1)).assertIsSelected()
        tagged(UiTags.verseCard(0)).assertIsNotSelected()
    }

    @Test
    fun theLiveVerseCarriesALivePill() = runComposeUiTest {
        // The one visual cue that says which words the congregation can see.
        showSongDetail(isProjecting = true, selectedVerseIndex = 1)

        assertTrue(exists(UiTags.verseLivePill(1)))
        assertFalse(exists(UiTags.verseLivePill(0)))
    }

    @Test
    fun noVerseIsMarkedLiveWhenTheSongIsNot() = runComposeUiTest {
        // A verse left highlighted after Clear would say the congregation is
        // still looking at it.
        showSongDetail(isProjecting = false, selectedVerseIndex = 1)

        tagged(UiTags.verseCard(1)).assertIsNotSelected()
        assertFalse(exists(UiTags.verseLivePill(1)))
    }

    // ── Loading, failing, and having nothing ─────────────────────────────
}
