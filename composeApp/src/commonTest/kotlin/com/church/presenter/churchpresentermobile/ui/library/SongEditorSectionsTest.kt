package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Rearranging a song while writing it.
 *
 * The order of the sections *is* the song — a chorus that ends up after the
 * last verse is projected in the wrong place on Sunday — so moving, splitting
 * and removing have to land on the section the operator meant. Each control
 * names its own position, which is how "the callback carried the wrong index"
 * becomes catchable rather than invisible.
 */
@OptIn(ExperimentalTestApi::class)
class SongEditorSectionsTest {

    private fun repository() = LibraryRepository(InMemoryFileStorage()) { 1_000L }

    private fun songWith(vararg texts: String) = LocalSong(
        id = "s1",
        number = "42",
        title = "Amazing Grace",
        sections = texts.map { LocalSongSection(SectionType.VERSE, it) },
    )

    private fun savedSong(repo: LibraryRepository) = repo.library.value.songs.first()

    /**
     * Brings section [index] into view.
     *
     * The sections live in a `LazyColumn`, so one below the fold is not composed
     * at all and `exists` would report its controls missing whether they are
     * offered or not.
     */
    private fun ComposeUiTest.showingVerse(index: Int) {
        tagged(LibraryTags.verse(index))
    }

    // ── Moving a section ─────────────────────────────────────────────────

    @Test
    fun theFirstSectionCannotBeMovedUp() = runComposeUiTest {
        // There is nowhere above it, and an inert control reads as broken.
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")
        showingVerse(0)

        assertFalse(exists(LibraryTags.verseUp(0)))
    }

    @Test
    fun theLastSectionCannotBeMovedDown() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")
        showingVerse(1)

        assertFalse(exists(LibraryTags.verseDown(1)))
    }

    @Test
    fun aMiddleSectionCanGoBothWays() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two", "Three")) }
        showSongEditor(repo, songId = "s1")
        showingVerse(1)

        assertTrue(exists(LibraryTags.verseUp(1)))
        assertTrue(exists(LibraryTags.verseDown(1)))
    }

    @Test
    fun aLoneSectionHasNowhereToGo() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("Only")) }
        showSongEditor(repo, songId = "s1")
        showingVerse(0)

        assertFalse(exists(LibraryTags.verseUp(0)))
        assertFalse(exists(LibraryTags.verseDown(0)))
    }

    @Test
    fun movingASectionUpChangesTheOrder() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseUp(1))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.first().text == "Two" }
    }

    @Test
    fun movingASectionUpKeepsTheOtherOne() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseUp(1))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.size == 2 }
        assertEquals("One", savedSong(repo).sections[1].text)
    }

    @Test
    fun movingASectionDownChangesTheOrder() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseDown(0))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.first().text == "Two" }
    }

    @Test
    fun movingTheRightSectionOfThree() = runComposeUiTest {
        // Two rows on screen and the wrong index moved is the bug this catches.
        val repo = repository().apply { upsertSong(songWith("One", "Two", "Three")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseUp(2))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.map { it.text } == listOf("One", "Three", "Two") }
    }

    @Test
    fun movingTwiceWalksASectionToTheTop() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two", "Three")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseUp(2))
        click(LibraryTags.verseUp(1))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.first().text == "Three" }
    }

    @Test
    fun movingUpAndBackDownLeavesTheSongAsItWas() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseUp(1))
        click(LibraryTags.verseDown(0))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.map { it.text } == listOf("One", "Two") }
    }

    // ── Removing a section ───────────────────────────────────────────────

    @Test
    fun everySectionOffersRemoval() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")
        showingVerse(0)

        assertTrue(exists(LibraryTags.verseRemove(0)))
        showingVerse(1)
        assertTrue(exists(LibraryTags.verseRemove(1)))
    }

    @Test
    fun removingASectionTakesItOutOfTheSong() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseRemove(1))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.size == 1 }
    }

    @Test
    fun removingASectionTakesOutTheRightOne() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseRemove(0))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.first().text == "Two" }
    }

    @Test
    fun removingTheMiddleSectionClosesTheGap() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two", "Three")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseRemove(1))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.map { it.text } == listOf("One", "Three") }
    }

    @Test
    fun removingEverySectionLeavesNothingToSave() = runComposeUiTest {
        // A song with no words is not a song, and the editor says so by refusing.
        val repo = repository().apply { upsertSong(songWith("One")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseRemove(0))

        awaitThat { !exists(LibraryTags.verse(0)) }
    }

    @Test
    fun anotherSectionCanBeAddedAfterARemoval() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One")) }
        showSongEditor(repo, songId = "s1")
        click(LibraryTags.verseRemove(0))

        click(LibraryTags.ADD_VERSE)

        awaitThat { exists(LibraryTags.verse(0)) }
    }

    // ── Splitting a section ──────────────────────────────────────────────

    @Test
    fun aSectionWithNoBlankLineCannotBeSplit() = runComposeUiTest {
        // There is nothing to split on, so the control is not offered.
        val repo = repository().apply { upsertSong(songWith("One line only")) }
        showSongEditor(repo, songId = "s1")
        showingVerse(0)

        assertFalse(exists(LibraryTags.verseSplit(0)))
    }

    @Test
    fun aSectionWithABlankLineOffersASplit() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("First half\n\nSecond half")) }
        showSongEditor(repo, songId = "s1")
        showingVerse(0)

        assertTrue(exists(LibraryTags.verseSplit(0)))
    }

    @Test
    fun splittingMakesTwoSections() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("First half\n\nSecond half")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseSplit(0))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.size == 2 }
    }

    @Test
    fun splittingKeepsBothHalves() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("First half\n\nSecond half")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseSplit(0))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.size == 2 }
        assertEquals("First half", savedSong(repo).sections[0].text.trim())
        assertEquals("Second half", savedSong(repo).sections[1].text.trim())
    }

    @Test
    fun splittingTheSecondSectionLeavesTheFirstAlone() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("Verse one", "A\n\nB")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseSplit(1))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.size == 3 }
        assertEquals("Verse one", savedSong(repo).sections.first().text)
    }

    @Test
    fun aSplitSectionCanBeSplitNoFurther() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("A\n\nB")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseSplit(0))

        awaitThat { !exists(LibraryTags.verseSplit(0)) }
    }

    @Test
    fun typingABlankLineIntoASectionOffersASplit() = runComposeUiTest {
        // The control appears as soon as the words make it possible.
        val repo = repository().apply { upsertSong(songWith("One line")) }
        showSongEditor(repo, songId = "s1")

        type(LibraryTags.verse(0), "First half\n\nSecond half")

        awaitThat { exists(LibraryTags.verseSplit(0)) }
    }

    @Test
    fun aSplitSectionKeepsItsPlaceInTheSong() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("A\n\nB", "Last")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseSplit(0))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.size == 3 }
        assertEquals("Last", savedSong(repo).sections.last().text)
    }

    // ── What kind of section it is ───────────────────────────────────────

    @Test
    fun aSectionStartsAsAVerse() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One")) }
        showSongEditor(repo, songId = "s1")
        showingVerse(0)

        assertTrue(exists(LibraryTags.verseType(0, 0)))
    }

    @Test
    fun aSectionCanBeMadeAChorus() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseType(0, 1))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.first().type == SectionType.CHORUS }
    }

    @Test
    fun changingOneSectionsKindLeavesTheOther() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseType(1, 1))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections[1].type == SectionType.CHORUS }
        assertEquals(SectionType.VERSE, savedSong(repo).sections[0].type)
    }

    @Test
    fun aSectionCanBeMadeABridge() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One")) }
        showSongEditor(repo, songId = "s1")

        click(LibraryTags.verseType(0, 2))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.first().type == SectionType.BRIDGE }
    }

    @Test
    fun aSectionCanBeChangedBackToAVerse() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One")) }
        showSongEditor(repo, songId = "s1")
        click(LibraryTags.verseType(0, 1))

        click(LibraryTags.verseType(0, 0))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.first().type == SectionType.VERSE }
    }

    @Test
    fun theKindSurvivesAMove() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One", "Two")) }
        showSongEditor(repo, songId = "s1")
        click(LibraryTags.verseType(1, 1))

        click(LibraryTags.verseUp(1))
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.first().type == SectionType.CHORUS }
    }

    @Test
    fun anExistingChorusOpensAsAChorus() = runComposeUiTest {
        val repo = repository().apply {
            upsertSong(
                LocalSong(
                    id = "s1",
                    number = "42",
                    title = "Amazing Grace",
                    sections = listOf(LocalSongSection(SectionType.CHORUS, "Praise him")),
                )
            )
        }

        showSongEditor(repo, songId = "s1")
        showingVerse(0)

        assertTrue(exists(LibraryTags.verseType(0, 1)))
    }

    @Test
    fun rewritingASectionKeepsItsKind() = runComposeUiTest {
        val repo = repository().apply { upsertSong(songWith("One")) }
        showSongEditor(repo, songId = "s1")
        click(LibraryTags.verseType(0, 1))

        type(LibraryTags.verse(0), "New words")
        click(LibraryTags.SAVE)

        awaitThat { savedSong(repo).sections.first().text == "New words" }
        assertEquals(SectionType.CHORUS, savedSong(repo).sections.first().type)
    }
}
