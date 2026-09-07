package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.DictionaryFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Book → Chapter → Verse filter above the dictionary list.
 *
 * It is a cascade: each dropdown only appears once the one before it has an
 * answer, and picking again higher up throws away what was chosen below. The
 * failure that matters is a stale narrower part — a chapter left over from the
 * previous book would silently restrict the search to a reference the operator
 * never asked for, and the list would just look wrong.
 */
@OptIn(ExperimentalTestApi::class)
class DictionaryReferenceFilterTest {

    private val genesis = refBook("Genesis", chapters = 50, id = 1, verseTotals = mapOf(1 to 31))
    private val exodus = refBook("Exodus", chapters = 40, id = 2)

    private fun desktopWithBooks() = FakeDesktop(books = listOf(genesis, exodus))

    // ── Whether the row is there at all ──────────────────────────────────

    @Test
    fun noReferenceFilterIsOfferedBeforeTheBooksArrive() = runComposeUiTest {
        // The desktop may have no Bible at all; an empty book dropdown is worse
        // than none.
        val vm = FakeDesktop(books = emptyList()).viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.dictEntry("H1254")) }
        assertFalse(exists(UiTags.DICT_REF_BOOK))
    }

    @Test
    fun theBookDropdownAppearsOnceTheBooksArrive() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.DICT_REF_BOOK) }
    }

    @Test
    fun noChapterDropdownIsOfferedBeforeABookIsChosen() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        assertFalse(exists(UiTags.DICT_REF_CHAPTER))
    }

    @Test
    fun noVerseDropdownIsOfferedBeforeAChapterIsChosen() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        assertFalse(exists(UiTags.DICT_REF_VERSE))
    }

    @Test
    fun noClearButtonIsOfferedWhileTheFilterIsUnused() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)

        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        assertFalse(exists(UiTags.DICT_REF_CLEAR))
    }

    // ── Choosing a book ──────────────────────────────────────────────────

    @Test
    fun theBookDropdownListsEveryBook() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)

        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        assertTrue(exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)))
    }

    @Test
    fun theBookDropdownNamesEachBook() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)

        awaitThat { isShowing("Exodus") }
    }

    @Test
    fun pickingABookSelectsIt() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))

        awaitThat { vm.refBook.value?.displayName == "Genesis" }
    }

    @Test
    fun pickingTheSecondBookSelectsTheSecondBook() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 1))

        awaitThat { vm.refBook.value?.displayName == "Exodus" }
    }

    @Test
    fun theChosenBookIsNamedOnThePill() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 1))

        awaitThat { vm.refBook.value != null }
        assertTrue(isShowing("Exodus"))
    }

    @Test
    fun choosingABookOffersItsChapters() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))

        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }
    }

    @Test
    fun choosingABookOffersAWayOutOfTheFilter() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))

        awaitThat { exists(UiTags.DICT_REF_CLEAR) }
    }

    @Test
    fun choosingABookAloneOffersNoVerseDropdown() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))

        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }
        assertFalse(exists(UiTags.DICT_REF_VERSE))
    }

    @Test
    fun theChosenBookNarrowsTheSearch() = runComposeUiTest {
        val desktop = desktopWithBooks()
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 1))

        awaitThat { desktop.searches.any { it["book"] == "2" } }
    }

    @Test
    fun aBookWithoutAnIdIsNumberedByItsPositionInTheList() = runComposeUiTest {
        // The desktop does not always send book-id; the list order is then the
        // only thing that says which book number the server means.
        val desktop = FakeDesktop(
            books = listOf(refBook("Genesis", chapters = 50), refBook("Exodus", chapters = 40)),
        )
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 1))

        awaitThat { desktop.searches.any { it["book"] == "2" } }
    }

    // ── Choosing a chapter ───────────────────────────────────────────────

    @Test
    fun theChapterDropdownListsEveryChapter() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 1))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }

        click(UiTags.DICT_REF_CHAPTER)

        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 39)) }
    }

    @Test
    fun theChapterDropdownStopsAtTheLastChapter() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 1))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }

        click(UiTags.DICT_REF_CHAPTER)

        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 39)) }
        assertFalse(exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 40)))
    }

    @Test
    fun pickingAChapterSelectsIt() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }

        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 2)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 2))

        awaitThat { vm.refChapter.value == 3 }
    }

    @Test
    fun theChosenChapterNarrowsTheSearch() = runComposeUiTest {
        val desktop = desktopWithBooks()
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }

        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))

        awaitThat { desktop.searches.any { it["chapter"] == "1" } }
    }

    @Test
    fun aChapterWhoseVerseCountIsKnownOffersItsVerses() = runComposeUiTest {
        // Genesis 1 carries verse-total in the book metadata, so no second
        // request is needed before the verse dropdown can appear.
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }

        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))

        awaitThat { exists(UiTags.DICT_REF_VERSE) }
    }

    @Test
    fun aChapterWithNoKnownVerseCountAsksTheDesktop() = runComposeUiTest {
        val desktop = FakeDesktop(books = listOf(exodus), chapterVerseCount = 22)
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }

        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))

        awaitThat { vm.refVerseCount.value == 22 }
    }

    @Test
    fun aChapterWithNoVersesAtAllOffersNoVerseDropdown() = runComposeUiTest {
        val desktop = FakeDesktop(books = listOf(exodus), chapterVerseCount = 0)
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }

        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))

        awaitThat { vm.refChapter.value == 1 }
        assertFalse(exists(UiTags.DICT_REF_VERSE))
    }

    // ── Choosing a verse ─────────────────────────────────────────────────

    @Test
    fun theVerseDropdownListsTheChaptersVerses() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }
        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))
        awaitThat { exists(UiTags.DICT_REF_VERSE) }

        click(UiTags.DICT_REF_VERSE)

        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_VERSE, 30)) }
        assertFalse(exists(UiTags.refOption(UiTags.DICT_REF_VERSE, 31)))
    }

    @Test
    fun pickingAVerseSelectsIt() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }
        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))
        awaitThat { exists(UiTags.DICT_REF_VERSE) }

        click(UiTags.DICT_REF_VERSE)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_VERSE, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_VERSE, 0))

        awaitThat { vm.refVerse.value == 1 }
    }

    @Test
    fun theChosenVerseNarrowsTheSearch() = runComposeUiTest {
        val desktop = desktopWithBooks()
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }
        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))
        awaitThat { exists(UiTags.DICT_REF_VERSE) }

        click(UiTags.DICT_REF_VERSE)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_VERSE, 4)) }
        click(UiTags.refOption(UiTags.DICT_REF_VERSE, 4))

        awaitThat { desktop.searches.any { it["verse"] == "5" } }
    }

    // ── Changing your mind ───────────────────────────────────────────────

    @Test
    fun choosingAnotherBookForgetsTheChapter() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }
        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 4)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 4))
        awaitThat { vm.refChapter.value == 5 }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 1))

        awaitThat { vm.refBook.value?.displayName == "Exodus" }
        assertNull(vm.refChapter.value)
    }

    @Test
    fun choosingAnotherBookForgetsTheVerse() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }
        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))
        awaitThat { exists(UiTags.DICT_REF_VERSE) }
        click(UiTags.DICT_REF_VERSE)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_VERSE, 2)) }
        click(UiTags.refOption(UiTags.DICT_REF_VERSE, 2))
        awaitThat { vm.refVerse.value == 3 }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 1))

        awaitThat { vm.refBook.value?.displayName == "Exodus" }
        assertNull(vm.refVerse.value)
    }

    @Test
    fun choosingAnotherBookTakesTheVerseDropdownAway() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }
        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))
        awaitThat { exists(UiTags.DICT_REF_VERSE) }

        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 1))

        awaitThat { vm.refBook.value?.displayName == "Exodus" }
        assertFalse(exists(UiTags.DICT_REF_VERSE))
    }

    @Test
    fun choosingAnotherChapterForgetsTheVerse() = runComposeUiTest {
        val desktop = FakeDesktop(
            books = listOf(refBook("Genesis", chapters = 50, id = 1, verseTotals = mapOf(1 to 31, 2 to 25))),
        )
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }
        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 0))
        awaitThat { exists(UiTags.DICT_REF_VERSE) }
        click(UiTags.DICT_REF_VERSE)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_VERSE, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_VERSE, 0))
        awaitThat { vm.refVerse.value == 1 }

        click(UiTags.DICT_REF_CHAPTER)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 1)) }
        click(UiTags.refOption(UiTags.DICT_REF_CHAPTER, 1))

        awaitThat { vm.refChapter.value == 2 }
        assertNull(vm.refVerse.value)
    }

    // ── Clearing ─────────────────────────────────────────────────────────

    @Test
    fun clearingForgetsTheBook() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CLEAR) }

        click(UiTags.DICT_REF_CLEAR)

        awaitThat { vm.refBook.value == null }
    }

    @Test
    fun clearingTakesTheChapterDropdownAway() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CHAPTER) }

        click(UiTags.DICT_REF_CLEAR)

        awaitThat { !exists(UiTags.DICT_REF_CHAPTER) }
    }

    @Test
    fun clearingTakesTheClearButtonAway() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CLEAR) }

        click(UiTags.DICT_REF_CLEAR)

        awaitThat { !exists(UiTags.DICT_REF_CLEAR) }
    }

    @Test
    fun clearingLeavesTheBookDropdownInPlace() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { exists(UiTags.DICT_REF_CLEAR) }

        click(UiTags.DICT_REF_CLEAR)

        awaitThat { vm.refBook.value == null }
        assertTrue(exists(UiTags.DICT_REF_BOOK))
    }

    @Test
    fun clearingWidensTheSearchAgain() = runComposeUiTest {
        val desktop = desktopWithBooks()
        val vm = desktop.viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { desktop.searches.any { it["book"] == "1" } }

        click(UiTags.DICT_REF_CLEAR)

        awaitThat { desktop.searches.last()["book"] == null }
    }

    @Test
    fun clearingKeepsTheTypedSearch() = runComposeUiTest {
        // The reference filter and the search box are two different narrowings;
        // dropping one must not drop the other.
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        type(UiTags.DICT_SEARCH, "light")
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { vm.refBook.value != null }

        click(UiTags.DICT_REF_CLEAR)

        awaitThat { vm.refBook.value == null }
        assertEquals("light", vm.searchQuery.value)
    }

    @Test
    fun clearingKeepsTheLanguageFilter() = runComposeUiTest {
        val vm = desktopWithBooks().viewModel()
        showDictionary(vm)
        awaitThat { exists(UiTags.DICT_REF_BOOK) }
        click(UiTags.dictFilter(1))
        click(UiTags.DICT_REF_BOOK)
        awaitThat { exists(UiTags.refOption(UiTags.DICT_REF_BOOK, 0)) }
        click(UiTags.refOption(UiTags.DICT_REF_BOOK, 0))
        awaitThat { vm.refBook.value != null }

        click(UiTags.DICT_REF_CLEAR)

        awaitThat { vm.refBook.value == null }
        assertEquals(DictionaryFilter.HEBREW, vm.filter.value)
    }
}
