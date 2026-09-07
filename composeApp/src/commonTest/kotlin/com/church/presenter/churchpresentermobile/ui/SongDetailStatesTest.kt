package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Loading, failing, and having no words at all.
 *
 * A spinner and an error at once would leave the operator waiting on a request
 * that has already failed.
 */
@OptIn(ExperimentalTestApi::class)
class SongDetailStatesTest {

    @Test
    fun aSpinnerIsShownWhileTheWordsAreFetched() = runComposeUiTest {
        showSongDetail(detail = null, isLoading = true)

        assertTrue(exists(UiTags.SONG_DETAIL_LOADING))
    }

    @Test
    fun noSpinnerOnceTheWordsHaveArrived() = runComposeUiTest {
        showSongDetail()

        assertFalse(exists(UiTags.SONG_DETAIL_LOADING))
    }

    @Test
    fun aFailureIsShownAsTheDesktopWordedIt() = runComposeUiTest {
        showSongDetail(detail = null, error = "Could not load the lyrics")

        assertTrue(exists(UiTags.SONG_DETAIL_ERROR))
        assertTrue(isShowing("Could not load the lyrics"))
    }

    @Test
    fun anErrorIsShownInsteadOfASpinner() = runComposeUiTest {
        // Both at once would leave the operator waiting on a request that has
        // already failed.
        showSongDetail(detail = null, isLoading = false, error = "Could not load the lyrics")

        assertFalse(exists(UiTags.SONG_DETAIL_LOADING))
        assertTrue(exists(UiTags.SONG_DETAIL_ERROR))
    }

    @Test
    fun aSongWithNoSectionsFallsBackToItsPlainText() = runComposeUiTest {
        // Some desktops send the whole song as one blob rather than as verses.
        showSongDetail(detail = detailOf(plain = "Amazing grace"))

        assertTrue(exists(UiTags.SONG_DETAIL_PLAIN_TEXT))
        assertTrue(isShowing("Amazing grace"))
    }

    @Test
    fun aSongWithNoWordsAtAllSaysSo() = runComposeUiTest {
        showSongDetail(detail = detailOf())

        assertTrue(exists(UiTags.SONG_DETAIL_NO_LYRICS))
    }

    @Test
    fun aBlankPlainTextIsTreatedAsNoWords() = runComposeUiTest {
        showSongDetail(detail = detailOf(plain = "   "))

        assertTrue(exists(UiTags.SONG_DETAIL_NO_LYRICS))
    }

    // ── The action buttons ───────────────────────────────────────────────
}
