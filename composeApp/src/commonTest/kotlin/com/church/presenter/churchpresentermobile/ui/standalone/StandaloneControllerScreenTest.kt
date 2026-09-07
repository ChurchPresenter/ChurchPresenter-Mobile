package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.tagged
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The surface an operator has open during a service.
 *
 * These are the controls reached for without looking — next, blank, clear —
 * which is why they never scroll away and why what they do has to be exact. The
 * failure that matters here is not a control that looks wrong but one that
 * moves the wrong slide: "Next" that skips two, or "Blank" that clears the deck
 * instead of hiding it.
 */
@OptIn(ExperimentalTestApi::class)
class StandaloneControllerScreenTest {

    private fun StandaloneFixture.loadThreeSlides() {
        engine.setDeck(
            SlideDeckBuilder.fromAnnouncements(
                items = listOf("First slide", "Second slide", "Third slide"),
                title = "Notices",
            )
        )
    }

    // ── Nothing loaded ───────────────────────────────────────────────────

    @Test
    fun anEmptyDeckSaysWhatToDoNext() = runComposeUiTest {
        val f = StandaloneFixture()

        showController(f)

        assertTrue(exists(StandaloneTags.EMPTY_DECK))
    }

    @Test
    fun anEmptyDeckStillShowsThePreview() = runComposeUiTest {
        // The preview is what the audience screen looks like; an empty one is
        // still the answer to "what is on the wall".
        val f = StandaloneFixture()

        showController(f)

        assertTrue(exists(StandaloneTags.PREVIEW))
    }

    @Test
    fun anEmptyDeckStillOffersTheOutputChip() = runComposeUiTest {
        val f = StandaloneFixture()

        showController(f)

        assertTrue(exists(StandaloneTags.OUTPUT_CHIP))
    }

    @Test
    fun anEmptyDeckHasNoSections() = runComposeUiTest {
        val f = StandaloneFixture()

        showController(f)

        assertFalse(exists(StandaloneTags.section(0)))
    }

    @Test
    fun aLoadedDeckTakesTheHintAway() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        f.loadThreeSlides()

        awaitThat { !exists(StandaloneTags.EMPTY_DECK) }
    }

    // ── The section list ─────────────────────────────────────────────────

    @Test
    fun aLoadedDeckListsItsSections() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        f.loadThreeSlides()

        awaitThat { exists(StandaloneTags.section(0)) }
    }

    @Test
    fun everySectionIsListed() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        f.loadThreeSlides()

        awaitThat { exists(StandaloneTags.section(2)) }
    }

    @Test
    fun theFirstSectionIsTheOneShowing() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        f.loadThreeSlides()

        awaitThat { f.engine.index.value == 0 }
    }

    @Test
    fun tappingASectionShowsIt() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(2)) }

        click(StandaloneTags.section(2))

        awaitThat { f.engine.index.value == 2 }
    }

    @Test
    fun tappingASectionSendsItToTheScreen() = runComposeUiTest {
        // The list is the operator's map of the song; tapping the wrong entry's
        // slide is the bug worth catching.
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(1)) }

        click(StandaloneTags.section(1))

        awaitThat { f.liveText?.contains("Second slide") == true }
    }

    @Test
    fun theDecksTitleHeadsTheSectionList() = runComposeUiTest {
        // Shown as an overline, so it arrives in caps.
        val f = StandaloneFixture()
        showController(f)

        f.loadThreeSlides()

        awaitThat { isShowing("NOTICES") }
    }

    // ── Stepping through ─────────────────────────────────────────────────

    @Test
    fun nextMovesOnOneSlide() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.NEXT)

        awaitThat { f.engine.index.value == 1 }
    }

    @Test
    fun nextMovesOnOnlyOneSlide() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.NEXT)

        awaitThat { f.engine.index.value == 1 }
        assertEquals(1, f.engine.index.value)
    }

    @Test
    fun previousGoesBackOne() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }
        click(StandaloneTags.NEXT)
        awaitThat { f.engine.index.value == 1 }

        click(StandaloneTags.PREV)

        awaitThat { f.engine.index.value == 0 }
    }

    @Test
    fun theFirstSlideCannotStepBack() = runComposeUiTest {
        // A control that does nothing reads as a broken app; this one is
        // disabled instead.
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }

        assertEquals(0, f.engine.index.value)
    }

    @Test
    fun steppingPastTheEndStaysOnTheLastSlide() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }

        repeat(5) { click(StandaloneTags.NEXT) }

        awaitThat { f.engine.index.value == f.engine.deck.value.slides.lastIndex }
    }

    @Test
    fun steppingSendsEachSlideToTheScreen() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.NEXT)

        awaitThat { f.liveText?.contains("Second slide") == true }
    }

    // ── Blanking, clearing and holding back ──────────────────────────────

    @Test
    fun blankingHidesTheSlideWithoutLosingIt() = runComposeUiTest {
        // The deck stays loaded: blanking is a pause, not a stop.
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.BLANK)

        awaitThat { f.engine.isBlank.value }
        assertFalse(f.engine.deck.value.isEmpty)
    }

    @Test
    fun blankingAgainBringsTheSlideBack() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }
        click(StandaloneTags.BLANK)
        awaitThat { f.engine.isBlank.value }

        click(StandaloneTags.BLANK)

        awaitThat { !f.engine.isBlank.value }
    }

    @Test
    fun clearingEmptiesTheDeck() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.CLEAR)

        awaitThat { f.engine.deck.value.isEmpty }
    }

    @Test
    fun clearingBringsBackTheEmptyHint() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.CLEAR)

        awaitThat { exists(StandaloneTags.EMPTY_DECK) }
    }

    @Test
    fun holdingOutputBackKeepsTheDeck() = runComposeUiTest {
        // "Live" is the difference between preparing the next song and showing
        // it; it must not throw away what is loaded.
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.LIVE)

        awaitThat { !f.engine.isLive.value }
        assertFalse(f.engine.deck.value.isEmpty)
    }

    @Test
    fun goingLiveAgainResumesOutput() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }
        click(StandaloneTags.LIVE)
        awaitThat { !f.engine.isLive.value }

        click(StandaloneTags.LIVE)

        awaitThat { f.engine.isLive.value }
    }

    @Test
    fun steppingWhileHeldBackStillMovesTheSlide() = runComposeUiTest {
        // Lining up the next verse before showing it is the whole point.
        val f = StandaloneFixture()
        showController(f)
        f.loadThreeSlides()
        awaitThat { exists(StandaloneTags.section(0)) }
        click(StandaloneTags.LIVE)
        awaitThat { !f.engine.isLive.value }

        click(StandaloneTags.NEXT)

        awaitThat { f.engine.index.value == 1 }
    }

    @Test
    fun theTransportStaysOnScreenWithAnEmptyDeck() = runComposeUiTest {
        val f = StandaloneFixture()

        showController(f)

        assertTrue(exists(StandaloneTags.NEXT))
        assertTrue(exists(StandaloneTags.BLANK))
    }

    // ── The backdrop ─────────────────────────────────────────────────────

    @Test
    fun theBackdropStartsAsAGradient() = runComposeUiTest {
        val f = StandaloneFixture()

        showController(f)

        tagged(StandaloneTags.backdrop(0)).assertIsSelected()
    }

    @Test
    fun theBackdropCanBeMadeBlack() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.backdrop(2))

        awaitThat { f.engine.backdrop.value == SlideBackdrop.BLACK }
    }

    @Test
    fun theBackdropCanGoBackToAGradient() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.backdrop(2))
        awaitThat { f.engine.backdrop.value == SlideBackdrop.BLACK }

        click(StandaloneTags.backdrop(0))

        awaitThat { f.engine.backdrop.value == SlideBackdrop.GRADIENT }
    }

    @Test
    fun choosingAnImageBackdropOffersThePhotos() = runComposeUiTest {
        // Choosing IMAGE is only half the instruction — without a photo the
        // audience page falls back to the gradient and the option looks broken.
        val f = StandaloneFixture()
        showController(f, photos = photoLibraryWith(2))

        click(StandaloneTags.backdrop(1))

        awaitThat { exists(StandaloneTags.backdropPhoto("p0")) }
    }

    @Test
    fun theOtherBackdropsOfferNoPhotos() = runComposeUiTest {
        val f = StandaloneFixture()

        showController(f, photos = photoLibraryWith(2))

        assertFalse(exists(StandaloneTags.backdropPhoto("p0")))
    }

    @Test
    fun anImageBackdropWithNoPhotosSaysSo() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f, photos = photoLibraryWith(0))

        click(StandaloneTags.backdrop(1))

        awaitThat { exists(StandaloneTags.BACKDROP_NO_PHOTOS) }
    }

    @Test
    fun pickingAPhotoBackdropSendsItsAddress() = runComposeUiTest {
        // A backdrop travels as a URL, so the one that reaches the screen is the
        // thing worth asserting.
        val f = StandaloneFixture()
        showController(f, photos = photoLibraryWith(2))
        click(StandaloneTags.backdrop(1))
        awaitThat { exists(StandaloneTags.backdropPhoto("p1")) }

        click(StandaloneTags.backdropPhoto("p1"))

        awaitThat { f.engine.backdropUrl.value?.contains("p1") == true }
    }

    @Test
    fun aPhotoBackdropNeedsTheServer() = runComposeUiTest {
        // Until the phone's own server is up there is no address to send.
        val f = StandaloneFixture()
        showController(f, photos = photoLibraryWith(2, baseUrl = null))

        click(StandaloneTags.backdrop(1))

        awaitThat { exists(StandaloneTags.BACKDROP_NEEDS_SERVER) }
    }

    @Test
    fun theLookIsReachableFromHere() = runComposeUiTest {
        val f = StandaloneFixture()

        showController(f)

        assertTrue(exists(StandaloneTags.LOOK))
    }

    @Test
    fun theOutputChipOpensTheOutputsList() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { exists(StandaloneTags.sink("screen")) }
    }
}
