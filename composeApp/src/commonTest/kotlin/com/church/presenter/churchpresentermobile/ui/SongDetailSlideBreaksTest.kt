package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A verse split across slides with a manual break (`[---]`).
 *
 * The desktop shows such a verse as two slides, and the index a card sends — and
 * the one the desktop reports back as live — counts those slides. A sheet that
 * listed the verse once sent every later card one short: tapping verse 2 put the
 * second half of verse 1 on the screen.
 */
@OptIn(ExperimentalTestApi::class)
class SongDetailSlideBreaksTest {

    private val split = detailOf(
        listOf(
            verse("Verse", "Amazing grace, how sweet the sound\n[---]\nI once was lost, but now am found"),
            verse("Verse", "'Twas grace that taught my heart to fear"),
            verse("Chorus", "How sweet the sound"),
        ),
    )

    @Test
    fun aSplitVerseIsListedAsOneCardPerSlide() = runComposeUiTest {
        showSongDetail(detail = split)

        (0..3).forEach { assertTrue(exists(UiTags.verseCard(it)), "card $it") }
        assertFalse(exists(UiTags.verseCard(4)))
    }

    @Test
    fun bothHalvesCarryTheVerseLabelAndTheirPart() = runComposeUiTest {
        showSongDetail(detail = split)

        assertTrue(isShowing("VERSE 1 (1/2)"))
        assertTrue(isShowing("VERSE 1 (2/2)"))
    }

    @Test
    fun theVerseAfterASplitKeepsItsOwnNumber() = runComposeUiTest {
        // Numbered by verse, not by slide: the second verse is still "Verse 2".
        showSongDetail(detail = split)

        assertTrue(isShowing("VERSE 2"))
    }

    @Test
    fun theBreakMarkerIsNotShownAsWords() = runComposeUiTest {
        showSongDetail(detail = split)

        assertFalse(isShowing("[---]"))
        assertTrue(isShowing("I once was lost, but now am found"))
    }

    @Test
    fun tappingTheNextVerseSendsTheDesktopsSlideIndex() = runComposeUiTest {
        // The reported bug: verse 2 is the desktop's third slide, not its second.
        var picked: Int? = null
        showSongDetail(detail = split, isProjecting = true, onVerseSelected = { picked = it })

        click(UiTags.verseCard(2))

        assertEquals(2, picked)
    }

    @Test
    fun tappingTheSecondHalfSendsItsOwnSlide() = runComposeUiTest {
        var picked: Int? = null
        showSongDetail(detail = split, isProjecting = true, onVerseSelected = { picked = it })

        click(UiTags.verseCard(1))

        assertEquals(1, picked)
    }

    @Test
    fun theSlideTheDesktopReportsLiveIsTheOneMarked() = runComposeUiTest {
        // The desktop reports the live slide by the same index: slide 1 is the
        // second half of verse 1, not verse 2.
        showSongDetail(detail = split, isProjecting = true, selectedVerseIndex = 1)

        tagged(UiTags.verseCard(1)).assertIsSelected()
        tagged(UiTags.verseCard(2)).assertIsNotSelected()
    }
}
