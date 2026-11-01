package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SongVerse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A song's notes, its section cues and the order it is played in, as the detail sheet shows them. */
@OptIn(ExperimentalTestApi::class)
class SongDetailArrangementTest {

    private val arranged = SongDetail(
        sections = listOf(
            SongVerse(key = "[Verse 1]", label = "Verse 1", text = "Amazing grace, how sweet the sound"),
            SongVerse(key = "{Chorus}", label = "Chorus", text = "Praise the Lord"),
            SongVerse(key = "[Verse 2]", label = "Verse 2", text = "'Twas grace that taught my heart"),
        ),
        order = listOf("[Verse 1]", "{Chorus}", "{Chorus}"),
        playOrder = listOf("[Verse 1]", "{Chorus}", "{Chorus}", "[Verse 2]"),
        notes = "Capo 2 for the band. Piano intro, 4 bars.",
        cues = mapOf("[Verse 1]" to "Piano only"),
    )

    @Test
    fun theSongsNotesAreShownAboveItsVerses() = runComposeUiTest {
        showSongDetail(detail = arranged)

        assertTrue(exists(UiTags.SONG_DETAIL_NOTES))
        assertTrue(isShowing("SONG"))
        assertTrue(isShowing("Capo 2 for the band. Piano intro, 4 bars."))
    }

    @Test
    fun tappingTheNotesOpensThem() = runComposeUiTest {
        showSongDetail(detail = arranged)

        click(UiTags.SONG_DETAIL_NOTES)

        assertTrue(isShowing("Capo 2 for the band. Piano intro, 4 bars."))
    }

    @Test
    fun aSongWithoutNotesShowsNoNotesCard() = runComposeUiTest {
        showSongDetail(detail = arranged.copy(notes = "  "))

        assertFalse(exists(UiTags.SONG_DETAIL_NOTES))
    }

    @Test
    fun aSectionsCueIsShownOnItsCardOnly() = runComposeUiTest {
        showSongDetail(detail = arranged)

        assertTrue(exists(UiTags.verseCue(0)))
        assertTrue(isShowing("Piano only"))
        assertFalse(exists(UiTags.verseCue(1)))
    }

    @Test
    fun theCardsFollowThePlayOrderAndAPlayedAgainChorusKeepsItsLabel() = runComposeUiTest {
        showSongDetail(detail = arranged)

        (0..3).forEach { assertTrue(exists(UiTags.verseCard(it)), "card $it") }
        assertFalse(exists(UiTags.verseCard(4)))
        assertFalse(isShowing("CHORUS 2"))
        assertTrue(isShowing("VERSE 2"))
    }

    @Test
    fun tappingThePlayedAgainChorusSendsItsPlace() = runComposeUiTest {
        var picked: Int? = null
        showSongDetail(detail = arranged, isProjecting = true, onVerseSelected = { picked = it })

        click(UiTags.verseCard(2))

        assertEquals(2, picked)
    }
}
