package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.tagged
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Standalone's live controls: the bar above the tabs, the chip it folds into,
 * and the full live view behind its thumbnail.
 *
 * These are the controls reached for without looking — next, blank, clear — so
 * what they do has to be exact. The failure that matters is not a control that
 * looks wrong but one that moves the wrong slide: "Next" that skips two, or
 * "Blank" that clears the deck instead of hiding it. Asserted on the engine and
 * on what reached the screen, not on the buttons.
 */
@OptIn(ExperimentalTestApi::class)
class LiveBarTest {

    private fun StandaloneFixture.loadThreeSlides() {
        engine.setDeck(
            SlideDeckBuilder.fromAnnouncements(
                items = listOf("First slide", "Second slide", "Third slide"),
                title = "Notices",
            )
        )
    }

    /** A bar with three slides loaded, ready to press. */
    private fun ComposeUiTest.barWithThreeSlides(f: StandaloneFixture = StandaloneFixture()): StandaloneFixture {
        showLiveBar(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.LIVE_BAR) }
        return f
    }

    /** The live view, opened from the bar's thumbnail, with three slides loaded. */
    private fun ComposeUiTest.liveViewWithThreeSlides(): StandaloneFixture {
        val f = barWithThreeSlides()
        click(StandaloneTags.LIVE_BAR_PREVIEW)
        awaitThat { exists(StandaloneTags.section(0)) }
        return f
    }

    // ── The bar ──────────────────────────────────────────────────────────

    @Test
    fun withNothingLoadedTheBarDrawsNothing() = runComposeUiTest {
        // A strip of disabled buttons over every tab would be noise.
        showLiveBar(StandaloneFixture())

        assertFalse(exists(StandaloneTags.LIVE_BAR))
    }

    @Test
    fun loadingADeckBringsTheBarUp() = runComposeUiTest {
        barWithThreeSlides()

        assertTrue(exists(StandaloneTags.LIVE_BAR))
        assertTrue(isShowing("Notices"))
    }

    @Test
    fun clearingTheDeckTakesTheBarAway() = runComposeUiTest {
        val f = barWithThreeSlides()

        f.engine.clear()

        awaitThat { !exists(StandaloneTags.LIVE_BAR) }
    }

    @Test
    fun atTheFirstSlidePreviousIsUnavailable() = runComposeUiTest {
        barWithThreeSlides()

        tagged(StandaloneTags.LIVE_BAR_PREV).assertIsNotEnabled()
        tagged(StandaloneTags.LIVE_BAR_NEXT).assertIsEnabled()
    }

    @Test
    fun atTheLastSlideNextIsUnavailable() = runComposeUiTest {
        val f = barWithThreeSlides()

        f.engine.showSlide(2)

        awaitThat { f.engine.index.value == 2 }
        tagged(StandaloneTags.LIVE_BAR_NEXT).assertIsNotEnabled()
        tagged(StandaloneTags.LIVE_BAR_PREV).assertIsEnabled()
    }

    @Test
    fun nextMovesOnOneSlide() = runComposeUiTest {
        val f = barWithThreeSlides()

        click(StandaloneTags.LIVE_BAR_NEXT)

        awaitThat { f.engine.index.value == 1 }
    }

    @Test
    fun nextSendsTheNextSlideToTheScreen() = runComposeUiTest {
        val f = barWithThreeSlides()

        click(StandaloneTags.LIVE_BAR_NEXT)

        awaitThat { f.liveText?.contains("Second slide") == true }
    }

    @Test
    fun previousGoesBackOne() = runComposeUiTest {
        val f = barWithThreeSlides()
        f.engine.showSlide(2)
        awaitThat { f.engine.index.value == 2 }

        click(StandaloneTags.LIVE_BAR_PREV)

        awaitThat { f.engine.index.value == 1 }
    }

    @Test
    fun blankingHidesTheSlideWithoutLosingIt() = runComposeUiTest {
        // The deck stays loaded: blanking is a pause, not a stop.
        val f = barWithThreeSlides()

        click(StandaloneTags.LIVE_BAR_BLANK)

        awaitThat { f.engine.isBlank.value }
        assertFalse(f.engine.deck.value.isEmpty)
    }

    @Test
    fun blankingAgainBringsTheSlideBack() = runComposeUiTest {
        val f = barWithThreeSlides()
        click(StandaloneTags.LIVE_BAR_BLANK)
        awaitThat { f.engine.isBlank.value }

        click(StandaloneTags.LIVE_BAR_BLANK)

        awaitThat { !f.engine.isBlank.value }
    }

    @Test
    fun theHandleFoldsTheBarAway() = runComposeUiTest {
        var collapsed = false
        val f = StandaloneFixture()
        showLiveBar(f, onCollapse = { collapsed = true })
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.LIVE_BAR_COLLAPSE) }

        click(StandaloneTags.LIVE_BAR_COLLAPSE)

        assertTrue(collapsed)
    }

    @Test
    fun theScreensDotOpensTheScreensSettings() = runComposeUiTest {
        var opened = false
        val f = StandaloneFixture()
        showLiveBar(f, onOpenScreens = { opened = true })
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.LIVE_BAR_STATUS) }

        click(StandaloneTags.LIVE_BAR_STATUS)

        assertTrue(opened)
    }

    // ── The chip ─────────────────────────────────────────────────────────

    @Test
    fun withNothingLoadedTheChipDrawsNothing() = runComposeUiTest {
        showLiveChip(StandaloneFixture())

        assertFalse(exists(StandaloneTags.LIVE_CHIP))
    }

    @Test
    fun theChipBringsTheBarBack() = runComposeUiTest {
        var expanded = false
        val f = StandaloneFixture()
        showLiveChip(f, onExpand = { expanded = true })
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.LIVE_CHIP) }

        click(StandaloneTags.LIVE_CHIP)

        assertTrue(expanded)
    }

    // ── The full live view ───────────────────────────────────────────────

    @Test
    fun theThumbnailOpensTheLiveView() = runComposeUiTest {
        liveViewWithThreeSlides()

        assertTrue(exists(StandaloneTags.LIVE_VIEW))
        assertTrue(exists(StandaloneTags.PREVIEW))
    }

    @Test
    fun theLiveViewListsEverySection() = runComposeUiTest {
        liveViewWithThreeSlides()

        assertTrue(exists(StandaloneTags.section(0)))
        assertTrue(exists(StandaloneTags.section(2)))
        assertFalse(exists(StandaloneTags.section(3)))
    }

    @Test
    fun tappingASectionSendsThatOneToTheScreen() = runComposeUiTest {
        // The list is the operator's map of the song; showing the wrong entry's
        // slide is the bug worth catching.
        val f = liveViewWithThreeSlides()

        click(StandaloneTags.section(1))

        awaitThat { f.engine.index.value == 1 }
        awaitThat { f.liveText?.contains("Second slide") == true }
    }

    @Test
    fun theLiveViewSteps() = runComposeUiTest {
        val f = liveViewWithThreeSlides()

        click(StandaloneTags.NEXT)

        awaitThat { f.engine.index.value == 1 }
    }

    @Test
    fun clearingEmptiesTheDeck() = runComposeUiTest {
        val f = liveViewWithThreeSlides()

        click(StandaloneTags.CLEAR)

        awaitThat { f.engine.deck.value.isEmpty }
    }

    @Test
    fun holdingOutputBackKeepsTheDeck() = runComposeUiTest {
        // "Live" is the difference between preparing the next song and showing
        // it; it must not throw away what is loaded.
        val f = liveViewWithThreeSlides()

        click(StandaloneTags.LIVE)

        awaitThat { !f.engine.isLive.value }
        assertFalse(f.engine.deck.value.isEmpty)
    }

    @Test
    fun goingLiveAgainResumesOutput() = runComposeUiTest {
        val f = liveViewWithThreeSlides()
        click(StandaloneTags.LIVE)
        awaitThat { !f.engine.isLive.value }

        click(StandaloneTags.LIVE)

        awaitThat { f.engine.isLive.value }
    }
}
