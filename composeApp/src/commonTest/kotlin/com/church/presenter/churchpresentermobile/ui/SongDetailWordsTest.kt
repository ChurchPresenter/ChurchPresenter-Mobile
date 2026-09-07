package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
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
 * The words of a song, and how the sections are labelled.
 *
 * Songs copied from a computer still carry chord markup; showing a literal
 * "[G]" mid-line is what this card used to do.
 */
@OptIn(ExperimentalTestApi::class)
class SongDetailWordsTest {
    @Test
    fun everyVerseIsListed() = runComposeUiTest {
        showSongDetail()

        assertTrue(exists(UiTags.verseCard(0)))
        assertTrue(exists(UiTags.verseCard(1)))
        assertTrue(exists(UiTags.verseCard(2)))
    }

    @Test
    fun aVerseShowsItsWords() = runComposeUiTest {
        showSongDetail()

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun repeatedSectionTypesAreNumbered() = runComposeUiTest {
        // Two verses and one chorus: the verses need telling apart, the chorus
        // does not. "Verse" twice would leave the operator counting cards.
        showSongDetail()

        assertTrue(isShowing("VERSE 1"))
        assertTrue(isShowing("VERSE 2"))
    }

    @Test
    fun aSectionTypeThatAppearsOnceIsNotNumbered() = runComposeUiTest {
        // "Chorus 1" with no Chorus 2 reads as a missing section.
        showSongDetail()

        assertTrue(isShowing("CHORUS"))
        assertFalse(isShowing("CHORUS 1"))
    }

    @Test
    fun aVerseWithNoNameIsNumberedByItsPosition() = runComposeUiTest {
        showSongDetail(
            detail = detailOf(listOf(verse(null, "first"), verse(null, "second"))),
        )

        assertTrue(isShowing("1"))
        assertTrue(isShowing("2"))
    }

    @Test
    fun chordMarkupIsNotShownAsLiteralBrackets() = runComposeUiTest {
        // Songs copied from the computer carry their markup; showing "[G]" in
        // the middle of a line is what this card used to do.
        showSongDetail(detail = detailOf(listOf(verse("Verse", "[G]Amazing [C]grace"))))

        assertTrue(isShowing("Amazing grace"))
        assertFalse(isShowing("[G]"))
    }

    @Test
    fun theChordsAreDrawnWhenTheChartIsAskedFor() = runComposeUiTest {
        showSongDetail(
            detail = detailOf(listOf(verse("Verse", "[G]Amazing [C]grace"))),
            showChords = true,
        )

        assertTrue(isShowing("G"))
        assertTrue(isShowing("Amazing"))
    }

    // ── Selecting a verse ────────────────────────────────────────────────
}
