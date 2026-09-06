package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What it takes to save a song, and what is kept when it is.
 *
 * Save stays unpressable until the song would actually project — a title with
 * no verses is a blank slide, verses with no title cannot be found again in a
 * list of a hundred. That gate is the screen's decision, not the ViewModel's.
 */
@OptIn(ExperimentalTestApi::class)
class SongEditorSavingTest {

    @Test
    fun aNewSongAlreadyHasAVerseToFillIn() = runComposeUiTest {
        // Opening with no verse at all would leave the operator hunting for the
        // button that adds one before they could type a word.
        showSongEditor(libraryOf())

        assertTrue(exists(LibraryTags.FIELD_TITLE))
        tagged(LibraryTags.verse(0))
    }

    @Test
    fun aBlankSongCannotBeSaved() = runComposeUiTest {
        showSongEditor(libraryOf())

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aTitleWithNoWordsCannotBeSaved() = runComposeUiTest {
        // A song with no verses projects a blank slide.
        val repository = libraryOf()
        showSongEditor(repository)

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
        assertTrue(repository.songs.isEmpty())
    }

    @Test
    fun wordsWithNoTitleCannotBeSaved() = runComposeUiTest {
        // The title is how it is found again in a list of a hundred.
        showSongEditor(libraryOf())

        type(LibraryTags.verse(0), "Amazing grace, how sweet the sound")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aTitleAndAVerseAreEnough() = runComposeUiTest {
        showSongEditor(libraryOf())

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")
        type(LibraryTags.verse(0), "Amazing grace, how sweet the sound")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun savingKeepsTheSong() = runComposeUiTest {
        val repository = libraryOf()
        showSongEditor(repository)

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")
        type(LibraryTags.verse(0), "Amazing grace, how sweet the sound")
        tagged(LibraryTags.SAVE).performClick()

        val saved = repository.songs.single()
        assertEquals("Amazing Grace", saved.title)
        assertEquals("Amazing grace, how sweet the sound", saved.sections.single().text)
    }

    @Test
    fun savingClosesTheEditor() = runComposeUiTest {
        var closed = false
        showSongEditor(libraryOf(), onClose = { closed = true })

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")
        type(LibraryTags.verse(0), "Amazing grace")
        tagged(LibraryTags.SAVE).performClick()

        assertTrue(closed)
    }

    @Test
    fun theDetailsAroundTheSongAreKeptToo() = runComposeUiTest {
        // Number and songbook are what the Songs tab lists a hymn under; losing
        // them makes it unfindable by the number in the operator's book.
        val repository = libraryOf()
        showSongEditor(repository)

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")
        type(LibraryTags.FIELD_NUMBER, "42")
        type(LibraryTags.FIELD_BOOK, "Hymns")
        type(LibraryTags.FIELD_AUTHOR, "John Newton")
        type(LibraryTags.FIELD_COPYRIGHT, "Public domain")
        type(LibraryTags.verse(0), "Amazing grace")
        tagged(LibraryTags.SAVE).performClick()

        val saved = repository.songs.single()
        assertEquals("42", saved.number)
        assertEquals("Hymns", saved.bookName)
        assertEquals("John Newton", saved.author)
        assertEquals("Public domain", saved.copyright)
    }
}
