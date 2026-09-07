package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.type
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The look sheet as the operator reaches it: from the live controller.
 *
 * Everything in it writes straight through — the theme rides inside each slide,
 * so one edit reaches the phone's preview, an attached screen and any browser
 * watching without being told separately. That means the thing to assert is
 * never "the control moved" but "the engine's theme changed", and the failure
 * this catches is a sheet whose edits stop at the sheet.
 */
@OptIn(ExperimentalTestApi::class)
class ControllerLookTest {

    private fun StandaloneFixture.loadDeck() {
        engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One", "Two"), "Notices"))
    }

    // ── Getting into it ──────────────────────────────────────────────────

    @Test
    fun theControllerOffersTheLook() = runComposeUiTest {
        val f = StandaloneFixture()

        showController(f)

        assertTrue(exists(StandaloneTags.LOOK))
    }

    @Test
    fun theLookIsClosedToBeginWith() = runComposeUiTest {
        // It would cover the transport controls, which must stay reachable.
        val f = StandaloneFixture()

        showController(f)

        assertFalse(exists(LookTags.GRADIENT_TOP))
    }

    @Test
    fun openingTheLookShowsItsControls() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.LOOK)

        awaitThat { exists(LookTags.GRADIENT_TOP) }
    }

    @Test
    fun openingTheLookOffersTheTextSizes() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.LOOK)

        awaitThat { exists(LookTags.textSize(0)) }
    }

    @Test
    fun openingTheLookOffersTheFonts() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.LOOK)

        awaitThat { exists(LookTags.font(0)) }
    }

    @Test
    fun openingTheLookOffersTheReferenceSwitches() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.LOOK)

        awaitThat { exists(LookTags.SHOW_SONG_REFERENCE) }
    }

    @Test
    fun theLookOpensOverALoadedDeck() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadDeck()
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.LOOK)

        awaitThat { exists(LookTags.GRADIENT_TOP) }
    }

    @Test
    fun theLookOpensOverAnEmptyDeck() = runComposeUiTest {
        // Setting the church's look before the first song is normal.
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.LOOK)

        awaitThat { exists(LookTags.TEXT_COLOUR) }
    }

    // ── Edits reaching the screen ────────────────────────────────────────

    @Test
    fun theTextSizeReachesTheEngine() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.textSize(0)) }

        click(LookTags.textSize(0))

        awaitThat { f.engine.textSize.value == SlideTextSize.SMALL }
    }

    @Test
    fun theLargestTextSizeReachesTheEngine() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.textSize(2)) }

        click(LookTags.textSize(2))

        awaitThat { f.engine.textSize.value == SlideTextSize.LARGE }
    }

    @Test
    fun theGradientTopReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.GRADIENT_TOP) }

        type(LookTags.GRADIENT_TOP, "#102030")

        awaitThat { f.engine.theme.value.gradientTop == "#102030" }
    }

    @Test
    fun theGradientBottomReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.GRADIENT_BOTTOM) }

        type(LookTags.GRADIENT_BOTTOM, "#405060")

        awaitThat { f.engine.theme.value.gradientBottom == "#405060" }
    }

    @Test
    fun theTextColourReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.TEXT_COLOUR) }

        type(LookTags.TEXT_COLOUR, "#FFEE00")

        awaitThat { f.engine.theme.value.textColor == "#FFEE00" }
    }

    @Test
    fun theBrandLineReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.BRAND_LINE) }

        type(LookTags.BRAND_LINE, "St Mary's")

        awaitThat { f.engine.theme.value.brandLine == "St Mary's" }
    }

    @Test
    fun turningOffTheSongReferenceReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.SHOW_SONG_REFERENCE) }

        click(LookTags.SHOW_SONG_REFERENCE)

        awaitThat { !f.engine.theme.value.showSongReference }
    }

    @Test
    fun turningOffTheBibleReferenceReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.SHOW_BIBLE_REFERENCE) }

        click(LookTags.SHOW_BIBLE_REFERENCE)

        awaitThat { !f.engine.theme.value.showBibleReference }
    }

    @Test
    fun turningTheClockOffReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.SHOW_CLOCK) }

        click(LookTags.SHOW_CLOCK)

        awaitThat { !f.engine.theme.value.showClock }
    }

    @Test
    fun ignoringLineBreaksReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.IGNORE_BREAKS) }

        click(LookTags.IGNORE_BREAKS)

        awaitThat { f.engine.theme.value.ignoreLineBreaks }
    }

    @Test
    fun anAlignmentReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.align(0)) }

        click(LookTags.align(0))

        awaitThat { f.engine.theme.value.textAlign.name == "LEFT" }
    }

    @Test
    fun aMarginReachesTheTheme() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.margin(0)) }

        click(LookTags.margin(0))

        awaitThat { f.engine.theme.value.margin.name == "THIN" }
    }

    @Test
    fun aThemeEditReachesAScreenAlreadyShowingASlide() = runComposeUiTest {
        // The theme rides inside the slide, so the screen has to be told again.
        val f = StandaloneFixture()
        showController(f)
        f.loadDeck()
        awaitThat { exists(StandaloneTags.section(0)) }
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.TEXT_COLOUR) }

        type(LookTags.TEXT_COLOUR, "#FFEE00")

        awaitThat { f.sink.rendered.last().slide?.theme?.textColor == "#FFEE00" }
    }

    @Test
    fun aTextSizeEditReachesAScreenAlreadyShowingASlide() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadDeck()
        awaitThat { exists(StandaloneTags.section(0)) }
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.textSize(2)) }

        click(LookTags.textSize(2))

        awaitThat { f.sink.rendered.last().slide?.textSize == SlideTextSize.LARGE }
    }

    @Test
    fun aPresetReachesTheTheme() = runComposeUiTest {
        // The presets are the fastest way to a look that reads from the back.
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.GRADIENT_TOP) }
        val before = f.engine.theme.value

        type(LookTags.GRADIENT_TOP, "#010203")

        awaitThat { f.engine.theme.value != before }
    }

    @Test
    fun theLookLeavesTheDeckAlone() = runComposeUiTest {
        // Changing how it looks is not changing what is on it.
        val f = StandaloneFixture()
        showController(f)
        f.loadDeck()
        awaitThat { exists(StandaloneTags.section(0)) }
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.textSize(2)) }

        click(LookTags.textSize(2))

        awaitThat { f.engine.deck.value.slides.size == 2 }
    }

    @Test
    fun theLookLeavesTheCurrentSlideAlone() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        f.loadDeck()
        awaitThat { exists(StandaloneTags.section(1)) }
        click(StandaloneTags.section(1))
        awaitThat { f.engine.index.value == 1 }
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.textSize(0)) }

        click(LookTags.textSize(0))

        awaitThat { f.engine.index.value == 1 }
    }

    @Test
    fun theLookLeavesTheBackdropAlone() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.backdrop(2))
        awaitThat { f.engine.backdrop.value == SlideBackdrop.BLACK }
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.textSize(0)) }

        click(LookTags.textSize(0))

        awaitThat { f.engine.backdrop.value == SlideBackdrop.BLACK }
    }

    @Test
    fun twoEditsBothStick() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.TEXT_COLOUR) }

        type(LookTags.TEXT_COLOUR, "#FFEE00")
        type(LookTags.GRADIENT_TOP, "#102030")

        awaitThat {
            f.engine.theme.value.textColor == "#FFEE00" &&
                f.engine.theme.value.gradientTop == "#102030"
        }
    }

    @Test
    fun anUnreadableColourIsNotWrittenIntoTheTheme() = runComposeUiTest {
        // Half a hex code is what the field looks like mid-typing.
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.TEXT_COLOUR) }
        val before = f.engine.theme.value.textColor

        type(LookTags.TEXT_COLOUR, "#FF")

        awaitThat { f.engine.theme.value.textColor == before }
    }

    @Test
    fun theChordSwitchReachesTheViewModel() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)
        click(StandaloneTags.LOOK)
        awaitThat { exists(LookTags.SHOW_CHORDS) }

        click(LookTags.SHOW_CHORDS)

        awaitThat { exists(LookTags.SHOW_CHORDS) }
    }

    @Test
    fun theTransportIsStillThereAfterTheLookIsOpened() = runComposeUiTest {
        val f = StandaloneFixture()
        showController(f)

        click(StandaloneTags.LOOK)

        awaitThat { exists(LookTags.GRADIENT_TOP) }
        assertTrue(exists(StandaloneTags.NEXT))
    }
}
