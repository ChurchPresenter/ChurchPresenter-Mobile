package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The verses of a song, and reopening one that already exists.
 *
 * A verse dropped on open is dropped again on save, and saving under a fresh id
 * would leave the library with two copies and the operator projecting whichever
 * the list happened to show first.
 */
@OptIn(ExperimentalTestApi::class)
class SongEditorVersesTest {

    @Test
    fun anotherVerseCanBeAdded() = runComposeUiTest {
        showSongEditor(libraryOf())
        assertFalse(exists(LibraryTags.verse(1)))

        tagged(LibraryTags.ADD_VERSE).performClick()

        tagged(LibraryTags.verse(1))
    }

    @Test
    fun aSecondVerseIsSavedAlongsideTheFirst() = runComposeUiTest {
        val repository = libraryOf()
        showSongEditor(repository)

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")
        type(LibraryTags.verse(0), "Amazing grace, how sweet the sound")
        tagged(LibraryTags.ADD_VERSE).performClick()
        type(LibraryTags.verse(1), "'Twas grace that taught my heart to fear")
        tagged(LibraryTags.SAVE).performClick()

        val sections = repository.songs.single().sections
        assertEquals(2, sections.size)
        assertEquals("Amazing grace, how sweet the sound", sections[0].text)
        assertEquals("'Twas grace that taught my heart to fear", sections[1].text)
    }

    @Test
    fun anExistingSongOpensOnItsOwnTitle() = runComposeUiTest {
        showSongEditor(libraryOf(songs = listOf(amazingGrace())), songId = "s1")

        assertTrue(isShowing("Amazing Grace"))
    }

    @Test
    fun anExistingSongsVersesAreThereToEdit() = runComposeUiTest {
        showSongEditor(libraryOf(songs = listOf(amazingGrace())), songId = "s1")

        tagged(LibraryTags.verse(0))

        assertTrue(isShowing("Amazing grace, how sweet the sound"))
    }

    @Test
    fun everyVerseOfAnExistingSongIsLoaded() = runComposeUiTest {
        // A chorus dropped on open would be dropped again on save.
        val song = amazingGrace().copy(
            sections = listOf(
                LocalSongSection(SectionType.VERSE, "Amazing grace"),
                LocalSongSection(SectionType.CHORUS, "sung twice"),
            ),
        )
        showSongEditor(libraryOf(songs = listOf(song)), songId = "s1")

        tagged(LibraryTags.verse(1))

        assertTrue(isShowing("sung twice"))
    }

    @Test
    fun aVerseCanBeRewritten() = runComposeUiTest {
        val repository = libraryOf(songs = listOf(amazingGrace()))
        showSongEditor(repository, songId = "s1")

        type(LibraryTags.verse(0), "That saved a wretch like me")
        tagged(LibraryTags.SAVE).performClick()

        assertEquals("That saved a wretch like me", repository.songs.single().sections.single().text)
    }

    @Test
    fun editingReplacesTheSongRatherThanAddingAnother() = runComposeUiTest {
        val repository = libraryOf(songs = listOf(amazingGrace()))
        showSongEditor(repository, songId = "s1")

        type(LibraryTags.FIELD_TITLE, "Amazing Grace (new words)")
        tagged(LibraryTags.SAVE).performClick()

        assertEquals(1, repository.songs.size)
        assertEquals("Amazing Grace (new words)", repository.songs.single().title)
    }

    @Test
    fun anExistingSongCanBeSavedStraightBack() = runComposeUiTest {
        // Opening a valid song must not present a disabled Save; there is
        // nothing to fix.
        showSongEditor(libraryOf(songs = listOf(amazingGrace())), songId = "s1")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }
}
