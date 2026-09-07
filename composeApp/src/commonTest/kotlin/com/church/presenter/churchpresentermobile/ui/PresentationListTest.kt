package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.WsMessageType
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The presentations list — every deck on the desktop, slide by slide.
 *
 * A tap here jumps a live deck to a particular slide, so the two things worth
 * pinning are which slide the tap named and which deck it belonged to. Two
 * decks are on screen in most of these tests for exactly that reason: a slide
 * index that reaches the desktop attached to the wrong deck moves the wrong
 * presentation, and a test with a single deck cannot tell.
 */
@OptIn(ExperimentalTestApi::class)
class PresentationListTest {

    // ── The list ─────────────────────────────────────────────────────────

    @Test
    fun everyDeckOnTheDesktopIsListed() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationHeader("sermon")) }
        assertTrue(exists(UiTags.presentationHeader("notices")))
    }

    @Test
    fun aDeckIsNamedByItsFile() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { isShowing("Sermon.pptx") }
    }

    @Test
    fun aDeckWithNoFileNameFallsBackToItsId() = runComposeUiTest {
        val vm = FakeDeckDesktop(decks = listOf(deck(id = "deck-7", fileName = null))).viewModel()
        showPresentations(vm)

        awaitThat { isShowing("deck-7") }
    }

    @Test
    fun aDeckSaysHowManySlidesItHas() = runComposeUiTest {
        val vm = FakeDeckDesktop(decks = listOf(deck(slides = 4))).viewModel()
        showPresentations(vm)

        awaitThat { isShowing("· 4") }
    }

    @Test
    fun theSlideCountIsTheDesktopsTotal() = runComposeUiTest {
        // The desktop can send thumbnails for only part of a long deck; the
        // header must still say how long the deck is.
        val vm = FakeDeckDesktop(decks = listOf(deck(slides = 2, slideTotal = 88))).viewModel()
        showPresentations(vm)

        awaitThat { isShowing("· 88") }
    }

    @Test
    fun aDeckWithNoSlidesShowsNoCount() = runComposeUiTest {
        val vm = FakeDeckDesktop(decks = listOf(deck(slides = 0, slideTotal = 0))).viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationHeader("sermon")) }
        assertFalse(isShowing("· 0"))
    }

    @Test
    fun everySlideOfADeckGetsATile() = runComposeUiTest {
        val vm = FakeDeckDesktop(decks = listOf(deck(slides = 4))).viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        assertTrue(exists(UiTags.presentationSlide("sermon", 1)))
        assertTrue(exists(UiTags.presentationSlide("sermon", 2)))
        assertTrue(exists(UiTags.presentationSlide("sermon", 3)))
    }

    @Test
    fun noTileIsInventedForASlideTheDeckDoesNotHave() = runComposeUiTest {
        val vm = FakeDeckDesktop(decks = listOf(deck(slides = 4))).viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationSlide("sermon", 3)) }
        assertFalse(exists(UiTags.presentationSlide("sermon", 4)))
    }

    @Test
    fun anOddNumberOfSlidesStillListsTheLastOne() = runComposeUiTest {
        // Slides are laid out two to a row; a lone last slide is the case a
        // chunking bug drops.
        val vm = FakeDeckDesktop(decks = listOf(deck(slides = 3))).viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationSlide("sermon", 2)) }
    }

    @Test
    fun aDeckWithASingleSlideListsIt() = runComposeUiTest {
        val vm = FakeDeckDesktop(decks = listOf(deck(slides = 1))).viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
    }

    @Test
    fun slidesAreKeptWithTheirOwnDeck() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationSlide("notices", 1)) }
        assertFalse(exists(UiTags.presentationSlide("notices", 2)))
    }

    @Test
    fun aDeckWithNoSlidesStillGetsAHeader() = runComposeUiTest {
        // A deck the desktop is still rendering has no thumbnails yet; dropping
        // the row entirely would look like the upload failed.
        val vm = FakeDeckDesktop(decks = listOf(deck(slides = 0))).viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationHeader("sermon")) }
    }

    // ── Nothing to show ──────────────────────────────────────────────────

    @Test
    fun aDesktopWithNoDecksSaysSo() = runComposeUiTest {
        val vm = FakeDeckDesktop(decks = emptyList()).viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.PRESENTATION_EMPTY) }
    }

    @Test
    fun aDesktopWithDecksShowsNoEmptyState() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationHeader("sermon")) }
        assertFalse(exists(UiTags.PRESENTATION_EMPTY))
    }

    // ── When the desktop refuses ─────────────────────────────────────────

    @Test
    fun aRefusedListRequestIsReported() = runComposeUiTest {
        val vm = FakeDeckDesktop(listStatus = HttpStatusCode.InternalServerError).viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.PRESENTATION_ERROR) }
    }

    @Test
    fun aRefusedListRequestOffersARetry() = runComposeUiTest {
        val vm = FakeDeckDesktop(listStatus = HttpStatusCode.InternalServerError).viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.PRESENTATION_RETRY) }
    }

    @Test
    fun retryingAsksTheDesktopAgain() = runComposeUiTest {
        val desktop = FakeDeckDesktop(listStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.PRESENTATION_RETRY) }
        val before = desktop.listRequests.size

        click(UiTags.PRESENTATION_RETRY)

        awaitThat { desktop.listRequests.size > before }
    }

    @Test
    fun aRecoveredRetryFillsTheList() = runComposeUiTest {
        val desktop = FakeDeckDesktop(listStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.PRESENTATION_ERROR) }

        desktop.listStatus = HttpStatusCode.OK
        click(UiTags.PRESENTATION_RETRY)

        awaitThat { exists(UiTags.presentationHeader("sermon")) }
    }

    @Test
    fun aRecoveredRetryClearsTheBanner() = runComposeUiTest {
        val desktop = FakeDeckDesktop(listStatus = HttpStatusCode.InternalServerError)
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.PRESENTATION_ERROR) }

        desktop.listStatus = HttpStatusCode.OK
        click(UiTags.PRESENTATION_RETRY)

        awaitThat { !exists(UiTags.PRESENTATION_ERROR) }
    }

    @Test
    fun aWorkingListShowsNoErrorBanner() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationHeader("sermon")) }
        assertFalse(exists(UiTags.PRESENTATION_ERROR))
    }

    @Test
    fun aWorkingListOffersNoRetry() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationHeader("sermon")) }
        assertFalse(exists(UiTags.PRESENTATION_RETRY))
    }

    // ── Tapping a slide ──────────────────────────────────────────────────

    @Test
    fun tappingASlideSelectsIt() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 2)) }

        click(UiTags.presentationSlide("sermon", 2))

        awaitThat { vm.selectedSlideIndex.value == 2 }
    }

    @Test
    fun tappingASlideSelectsItsDeck() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("notices", 1)) }

        click(UiTags.presentationSlide("notices", 1))

        awaitThat { vm.selectedPresentation.value?.id == "notices" }
    }

    @Test
    fun aSlideFromTheSecondDeckDoesNotMoveTheFirst() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("notices", 0)) }

        click(UiTags.presentationSlide("notices", 0))

        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_SLIDE).isNotEmpty() }
        assertTrue(
            desktop.payloadsOf(WsMessageType.SELECT_SLIDE).first().contains("notices"),
            "the deck the slide belongs to should reach the desktop",
        )
    }

    @Test
    fun theTappedSlideNumberReachesTheDesktop() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 3)) }

        click(UiTags.presentationSlide("sermon", 3))

        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_SLIDE).isNotEmpty() }
        assertTrue(
            desktop.payloadsOf(WsMessageType.SELECT_SLIDE).first().contains("\"index\":3"),
            "sent ${desktop.payloadsOf(WsMessageType.SELECT_SLIDE)}",
        )
    }

    @Test
    fun tappingASlideStartsProjecting() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }

        click(UiTags.presentationSlide("sermon", 0))

        awaitThat { vm.isProjecting.value }
    }

    @Test
    fun nothingIsProjectingWhenTheScreenOpens() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        assertFalse(vm.isProjecting.value)
    }

    @Test
    fun noSlideIsSelectedWhenTheScreenOpens() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        assertNull(vm.selectedSlideIndex.value)
    }

    @Test
    fun noSlideIsMarkedLiveWhenTheScreenOpens() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)

        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        tagged(UiTags.presentationSlide("sermon", 0)).assertIsNotSelected()
    }

    @Test
    fun theTappedSlideIsMarkedLive() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 1)) }

        click(UiTags.presentationSlide("sermon", 1))

        awaitThat { vm.selectedSlideIndex.value == 1 }
        tagged(UiTags.presentationSlide("sermon", 1)).assertIsSelected()
    }

    @Test
    fun onlyTheTappedSlideIsMarkedLive() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 1)) }

        click(UiTags.presentationSlide("sermon", 1))

        awaitThat { vm.selectedSlideIndex.value == 1 }
        tagged(UiTags.presentationSlide("sermon", 0)).assertIsNotSelected()
        tagged(UiTags.presentationSlide("sermon", 2)).assertIsNotSelected()
    }

    @Test
    fun theSameSlideNumberInAnotherDeckIsNotMarkedLive() = runComposeUiTest {
        // Both decks have a slide 1; only the one in the deck that is live
        // should carry the marker.
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("notices", 1)) }

        click(UiTags.presentationSlide("sermon", 1))

        awaitThat { vm.selectedSlideIndex.value == 1 }
        tagged(UiTags.presentationSlide("notices", 1)).assertIsNotSelected()
    }

    @Test
    fun theLiveMarkerFollowsTheNextTap() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { vm.selectedSlideIndex.value == 0 }

        click(UiTags.presentationSlide("sermon", 2))

        awaitThat { vm.selectedSlideIndex.value == 2 }
        tagged(UiTags.presentationSlide("sermon", 0)).assertIsNotSelected()
        tagged(UiTags.presentationSlide("sermon", 2)).assertIsSelected()
    }

    @Test
    fun theLiveMarkerMovesWithTheDeck() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("notices", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { vm.selectedPresentation.value?.id == "sermon" }

        click(UiTags.presentationSlide("notices", 0))

        awaitThat { vm.selectedPresentation.value?.id == "notices" }
        tagged(UiTags.presentationSlide("sermon", 0)).assertIsNotSelected()
        tagged(UiTags.presentationSlide("notices", 0)).assertIsSelected()
    }

    @Test
    fun reTappingTheLiveSlideSendsItAgain() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_SLIDE).size == 1 }

        click(UiTags.presentationSlide("sermon", 0))

        awaitThat { desktop.payloadsOf(WsMessageType.SELECT_SLIDE).size == 2 }
    }

    @Test
    fun aDeckWithoutAnIdSendsNothingToTheDesktop() = runComposeUiTest {
        // Nothing identifies the deck to the server, so the tap can only update
        // the phone's own state.
        val desktop = FakeDeckDesktop(decks = listOf(deck(id = null, fileName = "Orphan.pptx")))
        val vm = desktop.viewModel()
        showPresentations(vm)
        awaitThat { exists(UiTags.presentationSlide("", 0)) }

        click(UiTags.presentationSlide("", 0))

        awaitThat { vm.selectedSlideIndex.value == 0 }
        assertFalse(desktop.actions.contains(WsMessageType.SELECT_SLIDE))
    }

    // ── Reloading ────────────────────────────────────────────────────────

    @Test
    fun theListIsRequestedOnOpen() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm)

        awaitThat { desktop.listRequests.isNotEmpty() }
    }

    @Test
    fun savingSettingsReloadsTheList() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm, settingsSaveToken = 1)

        awaitThat { desktop.listRequests.size >= 2 }
    }

    @Test
    fun openingWithoutASettingsSaveRequestsTheListOnce() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm, settingsSaveToken = 0)

        awaitThat { desktop.listRequests.isNotEmpty() }
        assertEquals(1, desktop.listRequests.size)
    }

    @Test
    fun savingSettingsDropsTheOldSelection() = runComposeUiTest {
        // The deck that was live belonged to the previous computer.
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, settingsSaveToken = 0)
        awaitThat { exists(UiTags.presentationSlide("sermon", 0)) }
        click(UiTags.presentationSlide("sermon", 0))
        awaitThat { vm.isProjecting.value }

        vm.onSettingsSaved()

        awaitThat { !vm.isProjecting.value }
        assertNull(vm.selectedSlideIndex.value)
    }

    // ── Arriving from the schedule ───────────────────────────────────────

    @Test
    fun arrivingFromTheScheduleAsksForThatDeck() = runComposeUiTest {
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm, pendingNavPresentationId = "notices")

        awaitThat { desktop.listRequests.any { it.endsWith("/notices") } }
    }

    @Test
    fun arrivingFromTheScheduleIsReportedHandled() = runComposeUiTest {
        var handled = 0
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, pendingNavPresentationId = "notices", onPendingNavHandled = { handled++ })

        awaitThat { handled == 1 }
    }

    @Test
    fun arrivingFromTheScheduleShowsOnlyThatDeck() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, pendingNavPresentationId = "notices")

        awaitThat { exists(UiTags.presentationHeader("notices")) }
        awaitThat { !exists(UiTags.presentationHeader("sermon")) }
    }

    @Test
    fun arrivingFromTheScheduleSelectsThatDeck() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, pendingNavPresentationId = "notices")

        awaitThat { vm.selectedPresentation.value?.id == "notices" }
    }

    @Test
    fun arrivingFromTheScheduleProjectsNothingByItself() = runComposeUiTest {
        // Opening a deck from the running order is navigation, not a cue: the
        // congregation should not see a slide until someone taps one.
        val desktop = FakeDeckDesktop()
        val vm = desktop.viewModel()
        showPresentations(vm, pendingNavPresentationId = "notices")

        awaitThat { vm.selectedPresentation.value != null }
        assertFalse(desktop.actions.contains(WsMessageType.SELECT_SLIDE))
    }

    @Test
    fun aScheduleArrivalIsActedOnOnlyOnce() = runComposeUiTest {
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, pendingNavPresentationId = "notices")

        awaitThat { vm.selectedPresentation.value != null }
        awaitThat { vm.pendingScrollToId.value == null }
    }

    @Test
    fun aDeckTheDesktopCannotFindIsReported() = runComposeUiTest {
        val vm = FakeDeckDesktop(byIdStatus = HttpStatusCode.NotFound).viewModel()
        showPresentations(vm, pendingNavPresentationId = "gone")

        awaitThat { exists(UiTags.PRESENTATION_ERROR) }
    }

    @Test
    fun openingNormallyReportsNoPendingNavigation() = runComposeUiTest {
        var handled = 0
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, onPendingNavHandled = { handled++ })

        awaitThat { exists(UiTags.presentationHeader("sermon")) }
        assertEquals(0, handled)
    }

    @Test
    fun aBlankScheduleIdIsNotTreatedAsNavigation() = runComposeUiTest {
        var handled = 0
        val vm = FakeDeckDesktop().viewModel()
        showPresentations(vm, pendingNavPresentationId = "  ", onPendingNavHandled = { handled++ })

        awaitThat { exists(UiTags.presentationHeader("sermon")) }
        assertEquals(0, handled)
    }
}
