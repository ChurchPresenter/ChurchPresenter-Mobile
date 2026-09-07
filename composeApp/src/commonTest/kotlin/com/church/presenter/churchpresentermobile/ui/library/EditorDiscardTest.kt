package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Backing out of an editor with unsaved work.
 *
 * The Library is the only copy of anything typed into these screens, so leaving
 * one has to ask before it throws the text away — and only when there is
 * something to lose, or the prompt becomes noise the operator learns to dismiss.
 */
@OptIn(ExperimentalTestApi::class)
class EditorDiscardTest {

    @Test
    fun cancellingAnUntouchedSongJustCloses() = runComposeUiTest {
        var closed = false
        showSongEditor(libraryOf(), onClose = { closed = true })

        tagged(LibraryTags.CANCEL).performClick()

        assertTrue(closed)
        assertFalse(exists(LibraryTags.DISCARD), "asked about work that did not exist")
    }

    @Test
    fun cancellingAHalfWrittenSongAsksFirst() = runComposeUiTest {
        // The verses exist nowhere else yet.
        var closed = false
        showSongEditor(libraryOf(), onClose = { closed = true })
        type(LibraryTags.verse(0), "Amazing grace, how sweet")

        tagged(LibraryTags.CANCEL).performClick()

        assertFalse(closed, "the editor closed without asking")
        assertTrue(exists(LibraryTags.DISCARD))
    }

    @Test
    fun cancellingAnEditedSongAsksToo() = runComposeUiTest {
        var closed = false
        showSongEditor(libraryOf(songs = listOf(amazingGrace())), songId = "s1", onClose = { closed = true })
        type(LibraryTags.FIELD_TITLE, "Something else")

        tagged(LibraryTags.CANCEL).performClick()

        assertFalse(closed)
        assertTrue(exists(LibraryTags.DISCARD))
    }

    @Test
    fun discardingASongLeavesTheLibraryAsItWas() = runComposeUiTest {
        val repository = libraryOf()
        showSongEditor(repository)
        type(LibraryTags.verse(0), "Half a verse")
        tagged(LibraryTags.CANCEL).performClick()

        tagged(LibraryTags.DISCARD).performClick()

        assertTrue(repository.songs.isEmpty(), "a discarded song was saved anyway")
    }

    @Test
    fun keepingEditingReturnsToTheSong() = runComposeUiTest {
        // The way out of a mistap on Cancel.
        var closed = false
        showSongEditor(libraryOf(), onClose = { closed = true })
        type(LibraryTags.verse(0), "Half a verse")
        tagged(LibraryTags.CANCEL).performClick()

        tagged(LibraryTags.KEEP_EDITING).performClick()

        assertFalse(closed)
        assertFalse(exists(LibraryTags.DISCARD), "the prompt stayed up")
        assertTrue(isShowing("Half a verse"), "the words were lost anyway")
    }

    @Test
    fun cancellingAnUntouchedNoticeJustCloses() = runComposeUiTest {
        var closed = false
        showNoticeEditor(libraryOf(), onClose = { closed = true })

        tagged(LibraryTags.CANCEL).performClick()

        assertTrue(closed)
    }

    @Test
    fun cancellingAHalfWrittenNoticeAsksFirst() = runComposeUiTest {
        var closed = false
        showNoticeEditor(libraryOf(), onClose = { closed = true })
        type(LibraryTags.FIELD_BODY, "Half a thought")

        tagged(LibraryTags.CANCEL).performClick()

        assertFalse(closed, "the editor closed without asking")
        assertTrue(exists(LibraryTags.DISCARD))
    }

    @Test
    fun discardingANoticeLeavesNothingBehind() = runComposeUiTest {
        val repository = libraryOf()
        showNoticeEditor(repository)
        type(LibraryTags.FIELD_BODY, "Half a thought")
        tagged(LibraryTags.CANCEL).performClick()

        tagged(LibraryTags.DISCARD).performClick()

        assertTrue(repository.announcements.isEmpty(), "a discarded notice was saved anyway")
    }
}
