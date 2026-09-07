package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.BibleViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Bible tab as a whole — the books list and the chapter view behind one
 * switch, plus everything the tab owes the rest of the app.
 *
 * The two halves are covered on their own; what only exists here is the wiring.
 * Those are the failures an operator meets mid-service: a back gesture
 * registered when there is nothing to go back from, a toolbar left naming a
 * chapter that is no longer open, and a schedule tap that opens the book but not
 * the verse.
 *
 * Demo mode serves a canned translation, so nothing here touches a network.
 */
@OptIn(ExperimentalTestApi::class)
class BibleTabTest {

    private fun demoVm(): BibleViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return BibleViewModel(settings, ServerEventService(settings), isDemoMode = true)
    }

    private fun ComposeUiTest.showBibleTab(
        vm: BibleViewModel,
        settingsSaveToken: Int = 0,
        onNavigationChanged: (BibleBook?, Int?) -> Unit = { _, _ -> },
        onRegisterBackAction: ((() -> Unit)?) -> Unit = {},
        pendingNavBookName: String? = null,
        pendingNavChapter: Int? = null,
        pendingNavVerses: Set<Int> = emptySet(),
        onPendingNavHandled: () -> Unit = {},
        onScheduleRefresh: () -> Unit = {},
    ) = showScreen {
        BibleScreen(
            appSettings = AppSettings(InMemorySettingsStorage()),
            isDemoMode = true,
            settingsSaveToken = settingsSaveToken,
            onNavigationChanged = onNavigationChanged,
            onRegisterBackAction = onRegisterBackAction,
            pendingNavBookName = pendingNavBookName,
            pendingNavChapter = pendingNavChapter,
            pendingNavVerses = pendingNavVerses,
            onPendingNavHandled = onPendingNavHandled,
            onScheduleRefresh = onScheduleRefresh,
            providedViewModel = vm,
        )
    }

    private fun firstBookName(vm: BibleViewModel) = vm.books.value.first().displayName

    // ── What the tab opens on ────────────────────────────────────────────

    @Test
    fun theBookListIsWhatOpensFirst() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)

        awaitThat { vm.books.value.isNotEmpty() }
        assertTrue(exists(UiTags.BIBLE_SEARCH))
    }

    @Test
    fun theBooksTheTranslationHasAreListed() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)

        awaitThat { vm.books.value.isNotEmpty() }
        assertTrue(exists(UiTags.bibleBook(firstBookName(vm))))
    }

    @Test
    fun noChapterIsOpenToBeginWith() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)

        awaitThat { vm.books.value.isNotEmpty() }
        assertNull(vm.selectedBook.value)
    }

    @Test
    fun noChapterGridIsShownBeforeABookIsOpened() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)

        awaitThat { vm.books.value.isNotEmpty() }
        assertFalse(exists(UiTags.BIBLE_CHAPTERS_GRID))
    }

    @Test
    fun noErrorIsShownOnAWorkingTab() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)

        awaitThat { vm.books.value.isNotEmpty() }
        assertNull(vm.error.value)
    }

    // ── Opening a book ───────────────────────────────────────────────────

    @Test
    fun tappingABookOpensIt() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }

        click(UiTags.bibleBook(firstBookName(vm)))

        awaitThat { vm.selectedBook.value != null }
    }

    @Test
    fun openingABookShowsItsChapters() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }

        click(UiTags.bibleBook(firstBookName(vm)))

        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
    }

    @Test
    fun openingABookHidesTheBookList() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }
        val book = firstBookName(vm)

        click(UiTags.bibleBook(book))

        awaitThat { !exists(UiTags.bibleBook(book)) }
    }

    @Test
    fun openingTheSecondBookOpensTheSecondBook() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.size > 1 }
        val second = vm.books.value[1].displayName

        click(UiTags.bibleBook(second))

        awaitThat { vm.selectedBook.value?.displayName == second }
    }

    @Test
    fun openingABookTellsTheToolbar() = runComposeUiTest {
        // The tab strip shows the open book; a stale name there is how an
        // operator loses track of where they are.
        var reported: String? = null
        val vm = demoVm()
        showBibleTab(vm, onNavigationChanged = { book, _ -> reported = book?.displayName })
        awaitThat { vm.books.value.isNotEmpty() }

        click(UiTags.bibleBook(firstBookName(vm)))

        awaitThat { reported != null }
    }

    @Test
    fun theToolbarIsToldNothingIsOpenToBeginWith() = runComposeUiTest {
        var reported: String? = "stale"
        val vm = demoVm()
        showBibleTab(vm, onNavigationChanged = { book, _ -> reported = book?.displayName })

        awaitThat { reported == null }
    }

    @Test
    fun openingAChapterTellsTheToolbar() = runComposeUiTest {
        var chapter: Int? = null
        val vm = demoVm()
        showBibleTab(vm, onNavigationChanged = { _, c -> chapter = c })
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }

        click(UiTags.bibleChapter(1))

        awaitThat { chapter == 1 }
    }

    // ── Going back ───────────────────────────────────────────────────────

    @Test
    fun noBackActionIsRegisteredOnTheBookList() = runComposeUiTest {
        // There is nowhere to go back to; a registered action would swallow the
        // gesture that should leave the tab.
        var action: (() -> Unit)? = { }
        val vm = demoVm()
        showBibleTab(vm, onRegisterBackAction = { action = it })

        awaitThat { action == null }
    }

    @Test
    fun openingABookRegistersABackAction() = runComposeUiTest {
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showBibleTab(vm, onRegisterBackAction = { action = it })
        awaitThat { vm.books.value.isNotEmpty() }

        click(UiTags.bibleBook(firstBookName(vm)))

        awaitThat { action != null }
    }

    @Test
    fun theRegisteredBackActionClosesTheBook() = runComposeUiTest {
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showBibleTab(vm, onRegisterBackAction = { action = it })
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { action != null }

        action!!.invoke()

        awaitThat { vm.selectedBook.value == null }
    }

    @Test
    fun goingBackFromAChapterReturnsToTheChapterGrid() = runComposeUiTest {
        // One step at a time: chapter → book → list.
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showBibleTab(vm, onRegisterBackAction = { action = it })
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
        click(UiTags.bibleChapter(1))
        awaitThat { vm.selectedChapter.value == 1 }

        action!!.invoke()

        awaitThat { vm.selectedChapter.value == null }
        assertTrue(vm.selectedBook.value != null)
    }

    @Test
    fun goingBackTwiceReturnsToTheBookList() = runComposeUiTest {
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showBibleTab(vm, onRegisterBackAction = { action = it })
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
        click(UiTags.bibleChapter(1))
        awaitThat { vm.selectedChapter.value == 1 }

        action!!.invoke()
        awaitThat { vm.selectedChapter.value == null }
        action!!.invoke()

        awaitThat { vm.selectedBook.value == null }
    }

    @Test
    fun theBookListComesBackAfterGoingBack() = runComposeUiTest {
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showBibleTab(vm, onRegisterBackAction = { action = it })
        awaitThat { vm.books.value.isNotEmpty() }
        val book = firstBookName(vm)
        click(UiTags.bibleBook(book))
        awaitThat { action != null }

        action!!.invoke()

        awaitThat { exists(UiTags.bibleBook(book)) }
    }

    @Test
    fun closingTheBookTellsTheToolbarNothingIsOpen() = runComposeUiTest {
        var reported: String? = null
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showBibleTab(
            vm,
            onNavigationChanged = { b, _ -> reported = b?.displayName },
            onRegisterBackAction = { action = it },
        )
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { reported != null }

        action!!.invoke()

        awaitThat { reported == null }
    }

    // ── Searching the book list ──────────────────────────────────────────

    @Test
    fun typingFiltersTheBookList() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }

        type(UiTags.BIBLE_SEARCH, "gene")

        awaitThat { vm.bookSearchQuery.value == "gene" }
    }

    @Test
    fun aSearchThatMatchesNothingSaysSo() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }

        type(UiTags.BIBLE_SEARCH, "zzzzz")

        awaitThat { exists(UiTags.BIBLE_NO_MATCH) }
    }

    @Test
    fun aFruitlessSearchIsNotShownAsAnEmptyTranslation() = runComposeUiTest {
        // "No books" would send the operator to re-download a Bible that is
        // already there.
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }

        type(UiTags.BIBLE_SEARCH, "zzzzz")

        awaitThat { exists(UiTags.BIBLE_NO_MATCH) }
        assertFalse(exists(UiTags.BIBLE_NO_BOOKS))
    }

    @Test
    fun clearingTheSearchBringsTheBooksBack() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }
        val book = firstBookName(vm)
        type(UiTags.BIBLE_SEARCH, "zzzzz")
        awaitThat { exists(UiTags.BIBLE_NO_MATCH) }

        type(UiTags.BIBLE_SEARCH, "")

        awaitThat { exists(UiTags.bibleBook(book)) }
    }

    @Test
    fun theSearchIsStillThereWhileNothingMatches() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }

        type(UiTags.BIBLE_SEARCH, "zzzzz")

        awaitThat { exists(UiTags.BIBLE_NO_MATCH) }
        assertTrue(exists(UiTags.BIBLE_SEARCH))
    }

    // ── Arriving from the schedule ───────────────────────────────────────

    @Test
    fun arrivingFromTheScheduleOpensTheBook() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm, pendingNavBookName = "Genesis", pendingNavChapter = 1)

        awaitThat { vm.selectedBook.value?.displayName == "Genesis" }
    }

    @Test
    fun arrivingFromTheScheduleOpensTheChapter() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm, pendingNavBookName = "Genesis", pendingNavChapter = 1)

        awaitThat { vm.selectedChapter.value == 1 }
    }

    @Test
    fun arrivingFromTheScheduleIsReportedHandled() = runComposeUiTest {
        var handled = 0
        val vm = demoVm()
        showBibleTab(
            vm,
            pendingNavBookName = "Genesis",
            pendingNavChapter = 1,
            onPendingNavHandled = { handled++ },
        )

        awaitThat { handled == 1 }
    }

    @Test
    fun arrivingFromTheScheduleSelectsTheVerses() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(
            vm,
            pendingNavBookName = "Genesis",
            pendingNavChapter = 1,
            pendingNavVerses = setOf(1),
        )

        awaitThat { vm.selectedVerseIndices.value.isNotEmpty() }
    }

    @Test
    fun openingNormallyReportsNoPendingNavigation() = runComposeUiTest {
        var handled = 0
        val vm = demoVm()
        showBibleTab(vm, onPendingNavHandled = { handled++ })

        awaitThat { vm.books.value.isNotEmpty() }
        assertEquals(0, handled)
    }

    @Test
    fun aBookNameWithoutAChapterIsNotTreatedAsNavigation() = runComposeUiTest {
        var handled = 0
        val vm = demoVm()
        showBibleTab(vm, pendingNavBookName = "Genesis", onPendingNavHandled = { handled++ })

        awaitThat { vm.books.value.isNotEmpty() }
        assertEquals(0, handled)
    }

    @Test
    fun arrivingFromTheScheduleShowsTheChapter() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm, pendingNavBookName = "Genesis", pendingNavChapter = 1)

        awaitThat { vm.verses.value.isNotEmpty() }
        assertTrue(exists(UiTags.bibleVerse(0)))
    }

    // ── The chapter view ─────────────────────────────────────────────────

    @Test
    fun openingAChapterShowsItsVerses() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }

        click(UiTags.bibleChapter(1))

        awaitThat { exists(UiTags.bibleVerse(0)) }
    }

    @Test
    fun tappingAVerseSelectsIt() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
        click(UiTags.bibleChapter(1))
        awaitThat { exists(UiTags.bibleVerse(0)) }

        click(UiTags.bibleVerse(0))

        awaitThat { vm.selectedVerseIndices.value.contains(0) }
    }

    @Test
    fun castingAVerseProjectsIt() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
        click(UiTags.bibleChapter(1))
        awaitThat { exists(UiTags.bibleVerse(0)) }
        click(UiTags.bibleVerse(0))

        click(UiTags.FAB_CAST)

        awaitThat { vm.isProjecting.value }
    }

    @Test
    fun castingAgainStopsProjecting() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
        click(UiTags.bibleChapter(1))
        awaitThat { exists(UiTags.bibleVerse(0)) }
        click(UiTags.bibleVerse(0))
        click(UiTags.FAB_CAST)
        awaitThat { vm.isProjecting.value }
        awaitThat { exists(UiTags.FAB_CAST) }

        click(UiTags.FAB_CAST)

        awaitThat { !vm.isProjecting.value }
    }

    @Test
    fun theHoldButtonAppearsOnceSomethingIsProjecting() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
        click(UiTags.bibleChapter(1))
        awaitThat { exists(UiTags.bibleVerse(0)) }
        click(UiTags.bibleVerse(0))

        click(UiTags.FAB_CAST)

        awaitThat { exists(UiTags.FAB_HOLD) }
    }

    @Test
    fun addingAVerseToTheScheduleRefreshesTheRunningOrder() = runComposeUiTest {
        var refreshes = 0
        val vm = demoVm()
        showBibleTab(vm, onScheduleRefresh = { refreshes++ })
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
        click(UiTags.bibleChapter(1))
        awaitThat { exists(UiTags.bibleVerse(0)) }
        click(UiTags.bibleVerse(0))

        click(UiTags.FAB_ADD_TO_SCHEDULE)

        awaitThat { refreshes >= 1 }
    }

    @Test
    fun nothingRefreshesTheRunningOrderOnOpen() = runComposeUiTest {
        var refreshes = 0
        val vm = demoVm()
        showBibleTab(vm, onScheduleRefresh = { refreshes++ })

        awaitThat { vm.books.value.isNotEmpty() }
        assertEquals(0, refreshes)
    }

    @Test
    fun multiSelectLetsASecondVerseJoinTheFirst() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
        click(UiTags.bibleChapter(1))
        awaitThat { exists(UiTags.bibleVerse(1)) }
        awaitThat { exists(UiTags.FAB_SELECT) }

        click(UiTags.FAB_SELECT)
        click(UiTags.bibleVerse(0))
        click(UiTags.bibleVerse(1))

        awaitThat { vm.selectedVerseIndices.value.size == 2 }
    }

    @Test
    fun theSelectionCountIsShownWhileMultiSelecting() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm)
        awaitThat { vm.books.value.isNotEmpty() }
        click(UiTags.bibleBook(firstBookName(vm)))
        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
        click(UiTags.bibleChapter(1))
        awaitThat { exists(UiTags.bibleVerse(0)) }
        awaitThat { exists(UiTags.FAB_SELECT) }

        click(UiTags.FAB_SELECT)
        click(UiTags.bibleVerse(0))

        awaitThat { exists(UiTags.BIBLE_MULTI_SELECT_COUNT) }
    }

    // ── Settings changes ─────────────────────────────────────────────────

    @Test
    fun aSettingsSaveIsAcceptedWithoutLosingTheBooks() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm, settingsSaveToken = 1)

        awaitThat { vm.books.value.isNotEmpty() }
    }

    @Test
    fun aSettingsSaveWhileAChapterIsOpenKeepsTheTabUsable() = runComposeUiTest {
        val vm = demoVm()
        showBibleTab(vm, settingsSaveToken = 2)
        awaitThat { vm.books.value.isNotEmpty() }

        click(UiTags.bibleBook(firstBookName(vm)))

        awaitThat { exists(UiTags.BIBLE_CHAPTERS_GRID) }
    }
}
