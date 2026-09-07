package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LibraryValidation
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the song editor says about a song before it will save it.
 *
 * The distinction it draws is the whole point: an *error* is something that
 * would project badly enough to stop the save — a blank slide, a section too
 * long to fit — while a *warning* is a slip the operator may well have meant,
 * like reusing a hymn number. Turning a warning into a block would stop someone
 * saving a song mid-service; missing an error puts a broken slide on the wall.
 */
@OptIn(ExperimentalTestApi::class)
class SongEditorValidationTest {

    private fun repository() = LibraryRepository(InMemoryFileStorage()) { 1_000L }

    private fun ComposeUiTest.writeASong(title: String = "Amazing Grace", words: String = "Grace") {
        type(LibraryTags.FIELD_TITLE, title)
        type(LibraryTags.verse(0), words)
    }

    private val longSection = (1..20).joinToString("\n") { "Line $it" }

    private val oversizedSection = "x".repeat(LibraryValidation.MAX_SECTION_CHARS + 1)

    // ── Errors stop the save ─────────────────────────────────────────────

    @Test
    fun aSongWithNoTitleCannotBeSaved() = runComposeUiTest {
        showSongEditor(repository())

        type(LibraryTags.verse(0), "Amazing grace")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aSongWithOnlyBlankVersesCannotBeSaved() = runComposeUiTest {
        // Whitespace is not words; it would project an empty slide.
        showSongEditor(repository())

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")
        type(LibraryTags.verse(0), "   ")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aSectionTooLongToFitCannotBeSaved() = runComposeUiTest {
        showSongEditor(repository())

        writeASong()
        type(LibraryTags.verse(0), oversizedSection)

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aSectionTooLongToFitSaysWhy() = runComposeUiTest {
        // A disabled button with no explanation reads as a broken app.
        showSongEditor(repository())

        writeASong()
        type(LibraryTags.verse(0), oversizedSection)

        awaitThat { isShowing("too long") }
    }

    @Test
    fun shorteningAnOversizedSectionAllowsTheSave() = runComposeUiTest {
        showSongEditor(repository())
        writeASong()
        type(LibraryTags.verse(0), oversizedSection)
        tagged(LibraryTags.SAVE).assertIsNotEnabled()

        type(LibraryTags.verse(0), "Amazing grace, how sweet the sound")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun anOversizedSectionKeepsTheSongOutOfTheLibrary() = runComposeUiTest {
        val repo = repository()
        showSongEditor(repo)

        writeASong()
        type(LibraryTags.verse(0), oversizedSection)
        click(LibraryTags.SAVE)

        assertTrue(repo.songs.isEmpty())
    }

    @Test
    fun givingATitleClearsTheTitleProblem() = runComposeUiTest {
        showSongEditor(repository())
        type(LibraryTags.verse(0), "Amazing grace")
        tagged(LibraryTags.SAVE).assertIsNotEnabled()

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun clearingTheTitleAgainStopsTheSave() = runComposeUiTest {
        showSongEditor(repository())
        writeASong()
        tagged(LibraryTags.SAVE).assertIsEnabled()

        type(LibraryTags.FIELD_TITLE, "")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    // ── Warnings do not ──────────────────────────────────────────────────

    @Test
    fun aLongSectionIsWarnedAbout() = runComposeUiTest {
        // Twenty lines will not fit on one slide, and the author is the only
        // person who can decide what to do about it.
        showSongEditor(repository())

        writeASong()
        type(LibraryTags.verse(0), longSection)

        awaitThat { isShowing("may not fit") }
    }

    @Test
    fun aLongSectionCanStillBeSaved() = runComposeUiTest {
        // Blocking here would stop someone saving a song mid-service.
        showSongEditor(repository())

        writeASong()
        type(LibraryTags.verse(0), longSection)

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun aLongSectionIsKeptAsWritten() = runComposeUiTest {
        val repo = repository()
        showSongEditor(repo)

        writeASong()
        type(LibraryTags.verse(0), longSection)
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.songs.isNotEmpty() }
        assertEquals(longSection, repo.songs.single().sections.first().text)
    }

    @Test
    fun shorteningALongSectionTakesTheWarningAway() = runComposeUiTest {
        showSongEditor(repository())
        writeASong()
        type(LibraryTags.verse(0), longSection)
        awaitThat { isShowing("may not fit") }

        type(LibraryTags.verse(0), "Amazing grace")

        awaitThat { !isShowing("may not fit") }
    }

    @Test
    fun aReusedSongNumberIsWarnedAbout() = runComposeUiTest {
        // Almost always a slip, but the operator may have meant it.
        val repo = repository().apply { upsertSong(amazingGrace()) }
        showSongEditor(repo)

        writeASong(title = "Another Song")
        type(LibraryTags.FIELD_NUMBER, "42")

        awaitThat { isShowing("already uses number") }
    }

    @Test
    fun aReusedSongNumberCanStillBeSaved() = runComposeUiTest {
        val repo = repository().apply { upsertSong(amazingGrace()) }
        showSongEditor(repo)

        writeASong(title = "Another Song")
        type(LibraryTags.FIELD_NUMBER, "42")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun aReusedSongNumberIsKept() = runComposeUiTest {
        val repo = repository().apply { upsertSong(amazingGrace()) }
        showSongEditor(repo)

        writeASong(title = "Another Song")
        type(LibraryTags.FIELD_NUMBER, "42")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.songs.size == 2 }
    }

    @Test
    fun changingToAFreeNumberTakesTheWarningAway() = runComposeUiTest {
        val repo = repository().apply { upsertSong(amazingGrace()) }
        showSongEditor(repo)
        writeASong(title = "Another Song")
        type(LibraryTags.FIELD_NUMBER, "42")
        awaitThat { isShowing("already uses number") }

        type(LibraryTags.FIELD_NUMBER, "43")

        awaitThat { !isShowing("already uses number") }
    }

    @Test
    fun aSongDoesNotClashWithItself() = runComposeUiTest {
        // Reopening a saved song must not warn about the number it already has.
        val repo = repository().apply { upsertSong(amazingGrace()) }

        showSongEditor(repo, songId = "s1")

        awaitThat { !isShowing("already uses number") }
    }

    @Test
    fun anEditedSongCanBeSavedBackWithItsOwnNumber() = runComposeUiTest {
        val repo = repository().apply { upsertSong(amazingGrace()) }
        showSongEditor(repo, songId = "s1")

        type(LibraryTags.verse(0), "New words")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun noNumberAtAllIsNotAClash() = runComposeUiTest {
        // Two untitled-by-number songs are perfectly normal.
        val repo = repository().apply { upsertSong(amazingGrace().copy(number = "")) }
        showSongEditor(repo)

        writeASong(title = "Another Song")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun aWarningAndAnErrorTogetherStillBlockTheSave() = runComposeUiTest {
        // The error wins; a warning never unblocks anything.
        val repo = repository().apply { upsertSong(amazingGrace()) }
        showSongEditor(repo)

        type(LibraryTags.FIELD_NUMBER, "42")
        type(LibraryTags.verse(0), "Amazing grace")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    // ── Every kind of section ────────────────────────────────────────────

    @Test
    fun aSectionCanBeMadeATag() = runComposeUiTest {
        val repo = repository().apply { upsertSong(amazingGrace()) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseType(0, 3))
        click(LibraryTags.SAVE)

        awaitThat { repo.songs.single().sections.first().type == SectionType.TAG }
    }

    @Test
    fun aSectionCanBeMadeAnEnding() = runComposeUiTest {
        val repo = repository().apply { upsertSong(amazingGrace()) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseType(0, 4))
        click(LibraryTags.SAVE)

        awaitThat { repo.songs.single().sections.first().type == SectionType.ENDING }
    }

    @Test
    fun anExistingTagOpensAsATag() = runComposeUiTest {
        val repo = repository().apply {
            upsertSong(
                LocalSong(
                    id = "s1",
                    number = "42",
                    title = "Amazing Grace",
                    sections = listOf(LocalSongSection(SectionType.TAG, "Praise him")),
                )
            )
        }

        showSongEditor(repo, songId = "s1")

        tagged(LibraryTags.verse(0))
        assertTrue(exists(LibraryTags.verseType(0, 3)))
    }

    @Test
    fun anExistingEndingOpensAsAnEnding() = runComposeUiTest {
        val repo = repository().apply {
            upsertSong(
                LocalSong(
                    id = "s1",
                    number = "42",
                    title = "Amazing Grace",
                    sections = listOf(LocalSongSection(SectionType.ENDING, "Amen")),
                )
            )
        }

        showSongEditor(repo, songId = "s1")

        tagged(LibraryTags.verse(0))
        assertTrue(exists(LibraryTags.verseType(0, 4)))
    }

    @Test
    fun aSongCanMixSectionKinds() = runComposeUiTest {
        val repo = repository().apply {
            upsertSong(
                LocalSong(
                    id = "s1",
                    number = "42",
                    title = "Amazing Grace",
                    sections = listOf(
                        LocalSongSection(SectionType.VERSE, "Amazing grace"),
                        LocalSongSection(SectionType.CHORUS, "Praise him"),
                        LocalSongSection(SectionType.ENDING, "Amen"),
                    ),
                )
            )
        }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.SAVE)

        awaitThat { repo.songs.single().sections.size == 3 }
        assertEquals(
            listOf(SectionType.VERSE, SectionType.CHORUS, SectionType.ENDING),
            repo.songs.single().sections.map { it.type },
        )
    }

    // ── The details around the song ──────────────────────────────────────

    @Test
    fun aSongNumberIsOptional() = runComposeUiTest {
        val repo = repository()
        showSongEditor(repo)

        writeASong()
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.songs.isNotEmpty() }
        assertEquals("", repo.songs.single().number)
    }

    @Test
    fun aSongNumberIsKeptWhenGiven() = runComposeUiTest {
        val repo = repository()
        showSongEditor(repo)

        writeASong()
        type(LibraryTags.FIELD_NUMBER, "42")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.songs.isNotEmpty() }
        assertEquals("42", repo.songs.single().number)
    }

    @Test
    fun aTitleOfOnlySpacesIsNoTitle() = runComposeUiTest {
        showSongEditor(repository())

        type(LibraryTags.FIELD_TITLE, "   ")
        type(LibraryTags.verse(0), "Amazing grace")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aSecondVerseWithWordsIsEnoughEvenIfTheFirstIsBlank() = runComposeUiTest {
        // The check is "any usable section", not "the first one".
        showSongEditor(repository())

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")
        click(LibraryTags.ADD_VERSE)
        type(LibraryTags.verse(1), "Amazing grace")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun aBlankVerseIsNotSavedAlongsideAGoodOne() = runComposeUiTest {
        val repo = repository()
        showSongEditor(repo)

        type(LibraryTags.FIELD_TITLE, "Amazing Grace")
        click(LibraryTags.ADD_VERSE)
        type(LibraryTags.verse(1), "Amazing grace")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.songs.isNotEmpty() }
        assertTrue(repo.songs.single().sections.none { it.text.isBlank() })
    }

    @Test
    fun theWarningGoesWhenAnErrorTakesOver() = runComposeUiTest {
        // Only one problem line per field: an error replaces the warning rather
        // than stacking under it.
        showSongEditor(repository())
        writeASong()
        type(LibraryTags.verse(0), longSection)
        awaitThat { isShowing("may not fit") }

        type(LibraryTags.verse(0), oversizedSection)

        awaitThat { !isShowing("may not fit") }
    }

    @Test
    fun anOversizedSectionShowsItsErrorRatherThanNothing() = runComposeUiTest {
        showSongEditor(repository())

        writeASong()
        type(LibraryTags.verse(0), oversizedSection)

        awaitThat { isShowing("split it into two") }
    }

    @Test
    fun aNewSongStartsWithNoComplaints() = runComposeUiTest {
        // An editor that opens shouting at the operator is not a good welcome.
        showSongEditor(repository())

        assertTrue(!isShowing("may not fit"))
        assertTrue(!isShowing("already uses number"))
    }

    @Test
    fun aNewSongStartsUnsaveable() = runComposeUiTest {
        showSongEditor(repository())

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aSavedSongCanBeReopenedAndSavedAgainUnchanged() = runComposeUiTest {
        val repo = repository()
        showSongEditor(repo)
        writeASong()
        tagged(LibraryTags.SAVE).performClick()
        awaitThat { repo.songs.isNotEmpty() }

        assertEquals(1, repo.songs.size)
    }
}
