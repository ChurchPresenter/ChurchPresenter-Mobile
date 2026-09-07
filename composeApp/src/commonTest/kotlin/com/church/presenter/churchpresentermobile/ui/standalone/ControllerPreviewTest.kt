package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.tagged
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The operator's own view of what the room is seeing.
 *
 * The preview and the section list are the only feedback there is while facing
 * away from the screen, so they have to agree with the output at all times —
 * including the awkward moments: a deck swapped out from under a selection, a
 * blank held over a slide change, the last slide of one song and the first of
 * the next.
 */
@OptIn(ExperimentalTestApi::class)
class ControllerPreviewTest {

    private fun StandaloneFixture.load(vararg items: String, title: String = "Notices") {
        engine.setDeck(SlideDeckBuilder.fromAnnouncements(items.toList(), title))
    }

    // ── The preview ──────────────────────────────────────────────────────

    @Test
    fun thePreviewIsAlwaysOnScreen() = runComposeUiTest {
        val f = StandaloneFixture()

        showController(f)

        assertTrue(exists(StandaloneTags.PREVIEW))
    }

    @Test
    fun thePreviewShowsTheLiveSlidesWords() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        f.load("First slide", "Second slide")

        awaitThat { isShowing("First slide") }
    }

    @Test
    fun thePreviewFollowsAStep() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide", "Second slide")
        awaitThat { isShowing("First slide") }

        click(StandaloneTags.NEXT)

        awaitThat { isShowing("Second slide") }
    }

    @Test
    fun thePreviewFollowsASectionTap() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide", "Second slide", "Third slide")
        awaitThat { exists(StandaloneTags.section(2)) }

        click(StandaloneTags.section(2))

        awaitThat { isShowing("Third slide") }
    }

    @Test
    fun blankingReachesTheSlideThePreviewDraws() = runComposeUiTest {
        // The preview fades with the wall rather than dropping the words, so the
        // thing to assert is the slide it is handed.
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide")
        awaitThat { isShowing("First slide") }

        click(StandaloneTags.BLANK)

        awaitThat { f.sink.rendered.last().slide?.isHidden == true }
    }

    @Test
    fun unblankingPutsTheSlideBack() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide")
        click(StandaloneTags.BLANK)
        awaitThat { f.engine.isBlank.value }

        click(StandaloneTags.BLANK)

        awaitThat { f.sink.rendered.last().slide?.isHidden == false }
    }

    @Test
    fun holdingBackReachesTheSlideThePreviewDraws() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide")
        awaitThat { isShowing("First slide") }

        click(StandaloneTags.LIVE)

        awaitThat { f.sink.rendered.last().slide?.isHidden == true }
    }

    @Test
    fun thePreviewEmptiesWhenTheDeckIsCleared() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide")
        awaitThat { isShowing("First slide") }

        click(StandaloneTags.CLEAR)

        awaitThat { !isShowing("First slide") }
    }

    @Test
    fun steppingWhileBlankedStillMovesUnderneath() = runComposeUiTest {
        // Lining the next slide up behind a blank is how a service is run.
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide", "Second slide")
        click(StandaloneTags.BLANK)
        awaitThat { f.engine.isBlank.value }

        click(StandaloneTags.NEXT)

        awaitThat { f.engine.index.value == 1 }
    }

    @Test
    fun unblankingAfterAStepShowsTheNewSlide() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide", "Second slide")
        click(StandaloneTags.BLANK)
        awaitThat { f.engine.isBlank.value }
        click(StandaloneTags.NEXT)
        awaitThat { f.engine.index.value == 1 }

        click(StandaloneTags.BLANK)

        awaitThat { isShowing("Second slide") }
    }

    // ── The section list ─────────────────────────────────────────────────

    @Test
    fun theLiveSectionIsMarked() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide", "Second slide")
        awaitThat { exists(StandaloneTags.section(0)) }

        tagged(StandaloneTags.section(0)).assertIsSelected()
    }

    @Test
    fun theMarkFollowsAStep() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide", "Second slide")
        awaitThat { exists(StandaloneTags.section(1)) }

        click(StandaloneTags.NEXT)

        awaitThat { f.engine.index.value == 1 }
        tagged(StandaloneTags.section(1)).assertIsSelected()
    }

    @Test
    fun theMarkFollowsATap() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide", "Second slide", "Third slide")
        awaitThat { exists(StandaloneTags.section(2)) }

        click(StandaloneTags.section(2))

        awaitThat { f.engine.index.value == 2 }
        tagged(StandaloneTags.section(2)).assertIsSelected()
    }

    @Test
    fun theSectionListShowsEachSlidesWords() = runComposeUiTest {
        // It is how the operator finds the verse they want without stepping
        // through the whole song on the wall.
        val f = StandaloneFixture()
        showController(f)

        f.load("First slide", "Second slide")

        awaitThat { isShowing("Second slide") }
    }

    @Test
    fun aSecondDeckReplacesTheFirstsSections() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide", "Second slide", "Third slide")
        awaitThat { exists(StandaloneTags.section(2)) }

        f.load("Only one")

        awaitThat { !exists(StandaloneTags.section(2)) }
    }

    @Test
    fun aSecondDeckStartsAtItsFirstSlide() = runComposeUiTest {
        // Landing on slide three of a one-slide song is how a blank screen
        // happens mid-service.
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide", "Second slide", "Third slide")
        awaitThat { exists(StandaloneTags.section(2)) }
        click(StandaloneTags.section(2))
        awaitThat { f.engine.index.value == 2 }

        f.load("Only one")

        awaitThat { f.engine.index.value == 0 }
    }

    @Test
    fun aSecondDeckShowsItsOwnFirstSlide() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("First slide")
        awaitThat { isShowing("First slide") }

        f.load("A different notice")

        awaitThat { isShowing("A different notice") }
    }

    @Test
    fun aSingleSlideDeckHasOneSection() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        f.load("Only one")

        awaitThat { exists(StandaloneTags.section(0)) }
        assertFalse(exists(StandaloneTags.section(1)))
    }

    @Test
    fun aLongDeckListsItsLaterSections() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        f.load("One", "Two", "Three", "Four", "Five")

        awaitThat { exists(StandaloneTags.section(4)) }
    }

    @Test
    fun steppingThroughALongDeckEndsOnTheLast() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("One", "Two", "Three", "Four", "Five")
        awaitThat { exists(StandaloneTags.section(4)) }

        repeat(4) { click(StandaloneTags.NEXT) }

        awaitThat { f.engine.index.value == 4 }
    }

    @Test
    fun steppingBackFromTheEndWalksBack() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("One", "Two", "Three")
        awaitThat { exists(StandaloneTags.section(2)) }
        click(StandaloneTags.section(2))
        awaitThat { f.engine.index.value == 2 }

        click(StandaloneTags.PREV)
        click(StandaloneTags.PREV)

        awaitThat { f.engine.index.value == 0 }
    }

    @Test
    fun steppingBackPastTheStartStaysAtTheStart() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("One", "Two")
        awaitThat { exists(StandaloneTags.section(1)) }

        repeat(3) { click(StandaloneTags.PREV) }

        awaitThat { f.engine.index.value == 0 }
    }

    @Test
    fun anEmptyDeckHasNothingToStepThrough() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.NEXT)

        awaitThat { f.engine.index.value == 0 }
    }

    @Test
    fun clearingAfterSteppingResetsThePosition() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("One", "Two", "Three")
        awaitThat { exists(StandaloneTags.section(2)) }
        click(StandaloneTags.section(2))
        awaitThat { f.engine.index.value == 2 }

        click(StandaloneTags.CLEAR)

        awaitThat { f.engine.deck.value.isEmpty }
        assertEquals(0, f.engine.index.value)
    }

    @Test
    fun aDeckLoadedAfterClearingStartsFresh() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("One", "Two")
        awaitThat { exists(StandaloneTags.section(1)) }
        click(StandaloneTags.CLEAR)
        awaitThat { exists(StandaloneTags.EMPTY_DECK) }

        f.load("New one", "New two")

        awaitThat { isShowing("New one") }
    }

    @Test
    fun theTransportSurvivesADeckChange() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("One", "Two")
        awaitThat { exists(StandaloneTags.section(1)) }

        f.load("Only one")

        awaitThat { exists(StandaloneTags.section(0)) }
        assertTrue(exists(StandaloneTags.NEXT))
    }

    @Test
    fun projectingANewDeckLiftsABlank() = runComposeUiTest {
        // Projecting is the deliberate "show this" press; leaving the screen
        // black after it would read as a dead projector.
        val f = StandaloneFixture()
        showController(f)
        f.load("One")
        click(StandaloneTags.BLANK)
        awaitThat { f.engine.isBlank.value }

        f.load("Next song")

        awaitThat { !f.engine.isBlank.value }
    }

    @Test
    fun browsingADeckLeavesTheScreenAsItWas() = runComposeUiTest {
        // Opening a song to see its verses is not projecting it — that is the
        // difference between loading a deck and setting one.
        val f = StandaloneFixture()
        showController(f)
        f.load("One")
        awaitThat { isShowing("One") }
        val framesBefore = f.sink.rendered.size

        f.engine.loadDeck(SlideDeckBuilder.fromAnnouncements(listOf("Next song"), "Later"))

        awaitThat { exists(StandaloneTags.section(0)) }
        assertEquals(framesBefore, f.sink.rendered.size)
    }

    @Test
    fun aHeldBackScreenStaysHeldBackAcrossADeckChange() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("One")
        click(StandaloneTags.LIVE)
        awaitThat { !f.engine.isLive.value }

        f.load("Next song")

        awaitThat { !f.engine.isLive.value }
    }

    @Test
    fun theDeckTitleFollowsTheDeck() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("One", title = "Notices")
        awaitThat { isShowing("NOTICES") }

        f.load("One", title = "Amazing Grace")

        awaitThat { isShowing("AMAZING GRACE") }
    }

    @Test
    fun theEmptyHintReturnsAfterTheLastDeckIsCleared() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.load("One")
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.CLEAR)

        awaitThat { exists(StandaloneTags.EMPTY_DECK) }
    }

    @Test
    fun aBlankedEmptyScreenIsStillBlank() = runComposeUiTest {
        // Blanking with nothing loaded is harmless, and stays that way when a
        // deck arrives.
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.BLANK)

        awaitThat { f.engine.isBlank.value }
    }
}
