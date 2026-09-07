package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.NamedTheme
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideMargin
import com.church.presenter.churchpresentermobile.model.SlideTextAlign
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.model.SlideVerticalAlign
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.ui.tagged
import com.church.presenter.churchpresentermobile.ui.type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Everything about how the audience screen looks.
 *
 * Every control here writes straight through — the theme rides inside each
 * slide, so the phone's own output, an attached screen and any browser watching
 * all follow one edit without being told separately. There is no Apply and no
 * preview to keep in step, which makes "did the change reach the theme" the
 * only thing worth asserting, and the thing that silently stops working.
 */
@OptIn(ExperimentalTestApi::class)
class LookSheetTest {

    private fun preset(name: String, theme: SlideTheme = SlideTheme()) = NamedTheme(name, theme)

    /**
     * Shows the sheet over a theme that the sheet's own edits update, the way
     * the ViewModel does — so a second edit sees the result of the first.
     */
    private fun ComposeUiTest.showLook(
        initial: SlideTheme = SlideTheme(),
        initialTextSize: SlideTextSize = SlideTextSize.MEDIUM,
        initialShowChords: Boolean = false,
        presets: List<NamedTheme> = listOf(preset("Classic"), preset("Midnight")),
        savedThemes: List<NamedTheme> = emptyList(),
        onApplyTheme: (NamedTheme) -> Unit = {},
        onSaveTheme: (String) -> Unit = {},
        onDeleteTheme: (String) -> Unit = {},
        state: LookState = LookState(initial, initialTextSize, initialShowChords),
    ): LookState {
        showScreen {
            var theme by remember { mutableStateOf(state.theme) }
            var size by remember { mutableStateOf(state.textSize) }
            var chords by remember { mutableStateOf(state.showChords) }
            LookSheetContent(
                theme = theme,
                onThemeChange = { edit -> theme = edit(theme); state.theme = theme },
                showChords = chords,
                onShowChordsChange = { chords = it; state.showChords = it },
                textSize = size,
                onTextSizeChange = { size = it; state.textSize = it },
                presets = presets,
                savedThemes = savedThemes,
                onApplyTheme = onApplyTheme,
                onSaveTheme = onSaveTheme,
                onDeleteTheme = onDeleteTheme,
            )
        }
        return state
    }

    /** What the sheet has edited so far. */
    internal class LookState(
        var theme: SlideTheme,
        var textSize: SlideTextSize,
        var showChords: Boolean,
    )

    // ── Whole looks in one tap ───────────────────────────────────────────

    @Test
    fun everyPresetIsOffered() = runComposeUiTest {
        showLook()

        assertTrue(exists(LookTags.preset("Classic")))
        assertTrue(exists(LookTags.preset("Midnight")))
    }

    @Test
    fun aPresetIsNamed() = runComposeUiTest {
        showLook()

        assertTrue(isShowing("Classic"))
    }

    @Test
    fun tappingAPresetAdoptsIt() = runComposeUiTest {
        // Setting up on a Sunday morning should not mean assembling a readable
        // screen out of six colour fields.
        var applied: String? = null
        showLook(onApplyTheme = { applied = it.name })

        click(LookTags.preset("Midnight"))

        assertEquals("Midnight", applied)
    }

    @Test
    fun tappingOnePresetDoesNotApplyAnother() = runComposeUiTest {
        var applied: String? = null
        showLook(onApplyTheme = { applied = it.name })

        click(LookTags.preset("Classic"))

        assertEquals("Classic", applied)
    }

    @Test
    fun noSavedLooksAreShownWhenThereAreNone() = runComposeUiTest {
        showLook(savedThemes = emptyList())

        assertFalse(exists(LookTags.savedTheme("Mine")))
    }

    @Test
    fun aSavedLookIsOffered() = runComposeUiTest {
        showLook(savedThemes = listOf(preset("Mine")))

        assertTrue(exists(LookTags.savedTheme("Mine")))
    }

    @Test
    fun aSavedLookCanBeAdopted() = runComposeUiTest {
        var applied: String? = null
        showLook(savedThemes = listOf(preset("Mine")), onApplyTheme = { applied = it.name })

        click(LookTags.savedTheme("Mine"))

        assertEquals("Mine", applied)
    }

    @Test
    fun aSavedLookCanBeRemoved() = runComposeUiTest {
        var deleted: String? = null
        showLook(savedThemes = listOf(preset("Mine")), onDeleteTheme = { deleted = it })

        click(LookTags.deleteTheme("Mine"))

        assertEquals("Mine", deleted)
    }

    @Test
    fun aPresetCannotBeRemoved() = runComposeUiTest {
        // They are the way back to something readable; deleting one would be a
        // one-way door.
        showLook()

        assertFalse(exists(LookTags.deleteTheme("Classic")))
    }

    @Test
    fun savingALookAsksForANameFirst() = runComposeUiTest {
        showLook()

        assertFalse(exists(LookTags.THEME_NAME))
        click(LookTags.THEME_SAVE)

        assertTrue(exists(LookTags.THEME_NAME))
    }

    @Test
    fun aNamedLookIsSavedUnderThatName() = runComposeUiTest {
        var saved: String? = null
        showLook(onSaveTheme = { saved = it })
        click(LookTags.THEME_SAVE)

        type(LookTags.THEME_NAME, "Evening")
        click(LookTags.THEME_SAVE_CONFIRM)

        assertEquals("Evening", saved)
    }

    @Test
    fun savingALookClosesTheNameField() = runComposeUiTest {
        showLook()
        click(LookTags.THEME_SAVE)

        type(LookTags.THEME_NAME, "Evening")
        click(LookTags.THEME_SAVE_CONFIRM)

        assertFalse(exists(LookTags.THEME_NAME))
    }

    // ── Colours ──────────────────────────────────────────────────────────

    @Test
    fun allFourColoursAreOffered() = runComposeUiTest {
        showLook()

        assertTrue(exists(LookTags.GRADIENT_TOP))
        assertTrue(exists(LookTags.GRADIENT_BOTTOM))
        assertTrue(exists(LookTags.TEXT_COLOUR))
        assertTrue(exists(LookTags.ACCENT_COLOUR))
    }

    @Test
    fun theTopOfTheGradientCanBeChanged() = runComposeUiTest {
        val state = showLook()

        type(LookTags.GRADIENT_TOP, "#123456")

        assertEquals("#123456", state.theme.gradientTop)
    }

    @Test
    fun theBottomOfTheGradientCanBeChanged() = runComposeUiTest {
        val state = showLook()

        type(LookTags.GRADIENT_BOTTOM, "#654321")

        assertEquals("#654321", state.theme.gradientBottom)
    }

    @Test
    fun theTextColourCanBeChanged() = runComposeUiTest {
        val state = showLook()

        type(LookTags.TEXT_COLOUR, "#FFEE00")

        assertEquals("#FFEE00", state.theme.textColor)
    }

    @Test
    fun theAccentColourCanBeChanged() = runComposeUiTest {
        val state = showLook()

        type(LookTags.ACCENT_COLOUR, "#00EEFF")

        assertEquals("#00EEFF", state.theme.accentColor)
    }

    @Test
    fun changingOneColourLeavesTheOthersAlone() = runComposeUiTest {
        val before = SlideTheme()
        val state = showLook(initial = before)

        type(LookTags.TEXT_COLOUR, "#FFEE00")

        assertEquals(before.gradientTop, state.theme.gradientTop)
        assertEquals(before.accentColor, state.theme.accentColor)
    }

    @Test
    fun aHalfTypedColourIsNotAppliedToTheScreen() = runComposeUiTest {
        val before = SlideTheme()
        val state = showLook(initial = before)

        type(LookTags.TEXT_COLOUR, "#FFE")

        // Three digits is a complete colour; four is not.
        type(LookTags.GRADIENT_TOP, "#12")

        assertEquals(before.gradientTop, state.theme.gradientTop)
    }

    // ── Size, font and layout ────────────────────────────────────────────

    @Test
    fun allThreeTextSizesAreOffered() = runComposeUiTest {
        showLook()

        assertTrue(exists(LookTags.textSize(0)))
        assertTrue(exists(LookTags.textSize(1)))
        assertTrue(exists(LookTags.textSize(2)))
    }

    @Test
    fun theCurrentTextSizeIsSelected() = runComposeUiTest {
        showLook(initialTextSize = SlideTextSize.LARGE)

        tagged(LookTags.textSize(2)).assertIsSelected()
    }

    @Test
    fun pickingASmallerTextSizeAppliesIt() = runComposeUiTest {
        val state = showLook(initialTextSize = SlideTextSize.LARGE)

        click(LookTags.textSize(0))

        assertEquals(SlideTextSize.SMALL, state.textSize)
    }

    @Test
    fun pickingALargerTextSizeAppliesIt() = runComposeUiTest {
        val state = showLook(initialTextSize = SlideTextSize.SMALL)

        click(LookTags.textSize(2))

        assertEquals(SlideTextSize.LARGE, state.textSize)
    }

    @Test
    fun bothFontsAreOffered() = runComposeUiTest {
        showLook()

        assertTrue(exists(LookTags.font(0)))
        assertTrue(exists(LookTags.font(1)))
    }

    @Test
    fun theCurrentFontIsSelected() = runComposeUiTest {
        showLook(initial = SlideTheme(font = SlideFont.SANS))

        tagged(LookTags.font(1)).assertIsSelected()
    }

    @Test
    fun pickingTheOtherFontAppliesIt() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(font = SlideFont.SERIF))

        click(LookTags.font(1))

        assertEquals(SlideFont.SANS, state.theme.font)
    }

    @Test
    fun goingBackToSerifAppliesIt() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(font = SlideFont.SANS))

        click(LookTags.font(0))

        assertEquals(SlideFont.SERIF, state.theme.font)
    }

    @Test
    fun allThreeAlignmentsAreOffered() = runComposeUiTest {
        showLook()

        assertTrue(exists(LookTags.align(0)))
        assertTrue(exists(LookTags.align(1)))
        assertTrue(exists(LookTags.align(2)))
    }

    @Test
    fun centreIsTheAlignmentToBeginWith() = runComposeUiTest {
        showLook()

        tagged(LookTags.align(1)).assertIsSelected()
    }

    @Test
    fun aligningLeftAppliesIt() = runComposeUiTest {
        val state = showLook()

        click(LookTags.align(0))

        assertEquals(SlideTextAlign.LEFT, state.theme.textAlign)
    }

    @Test
    fun aligningRightAppliesIt() = runComposeUiTest {
        val state = showLook()

        click(LookTags.align(2))

        assertEquals(SlideTextAlign.RIGHT, state.theme.textAlign)
    }

    @Test
    fun theChosenAlignmentBecomesTheSelectedOne() = runComposeUiTest {
        showLook()

        click(LookTags.align(0))

        tagged(LookTags.align(0)).assertIsSelected()
        tagged(LookTags.align(1)).assertIsNotSelected()
    }

    @Test
    fun allThreeMarginsAreOffered() = runComposeUiTest {
        showLook()

        assertTrue(exists(LookTags.margin(0)))
        assertTrue(exists(LookTags.margin(2)))
    }

    @Test
    fun aThinMarginCanBeChosen() = runComposeUiTest {
        val state = showLook()

        click(LookTags.margin(0))

        assertEquals(SlideMargin.THIN, state.theme.margin)
    }

    @Test
    fun aThickMarginCanBeChosen() = runComposeUiTest {
        val state = showLook()

        click(LookTags.margin(2))

        assertEquals(SlideMargin.THICK, state.theme.margin)
    }

    @Test
    fun theMiddleMarginIsTheOneToBeginWith() = runComposeUiTest {
        // What every output used before this was a setting, so nothing moves
        // for anyone who does not go looking.
        showLook()

        tagged(LookTags.margin(1)).assertIsSelected()
    }

    @Test
    fun wordsCanBeMovedToTheTop() = runComposeUiTest {
        val state = showLook()

        click(LookTags.verticalAlign(0))

        assertEquals(SlideVerticalAlign.TOP, state.theme.verticalAlign)
    }

    @Test
    fun wordsCanBeMovedToTheBottom() = runComposeUiTest {
        val state = showLook()

        click(LookTags.verticalAlign(2))

        assertEquals(SlideVerticalAlign.BOTTOM, state.theme.verticalAlign)
    }

    @Test
    fun theMiddleIsWhereWordsSitToBeginWith() = runComposeUiTest {
        showLook()

        tagged(LookTags.verticalAlign(1)).assertIsSelected()
    }

    // ── The church's name in the corner ──────────────────────────────────

    @Test
    fun theCornerLineCanBeSet() = runComposeUiTest {
        val state = showLook()

        type(LookTags.BRAND_LINE, "St Mary's")

        assertEquals("St Mary's", state.theme.brandLine)
    }

    @Test
    fun anEmptyCornerLineMeansNoCornerLine() = runComposeUiTest {
        // Rather than an empty one taking up space on the screen.
        val state = showLook(initial = SlideTheme(brandLine = "St Mary's"))

        type(LookTags.BRAND_LINE, "")

        assertNull(state.theme.brandLine)
    }

    @Test
    fun aCornerLineOfSpacesMeansNoCornerLine() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(brandLine = "St Mary's"))

        type(LookTags.BRAND_LINE, "   ")

        assertNull(state.theme.brandLine)
    }

    // ── The switches ─────────────────────────────────────────────────────

    @Test
    fun everySwitchIsOffered() = runComposeUiTest {
        showLook()

        assertTrue(exists(LookTags.SHOW_SONG_REFERENCE))
        assertTrue(exists(LookTags.SHOW_BIBLE_REFERENCE))
        assertTrue(exists(LookTags.SHOW_OTHER_REFERENCE))
        assertTrue(exists(LookTags.SHOW_CLOCK))
        assertTrue(exists(LookTags.AUTO_FIT))
        assertTrue(exists(LookTags.IGNORE_BREAKS))
        assertTrue(exists(LookTags.SHOW_CHORDS))
    }

    @Test
    fun theSongHeadingSwitchShowsItsState() = runComposeUiTest {
        showLook(initial = SlideTheme(showSongReference = true))

        tagged(LookTags.SHOW_SONG_REFERENCE).assertIsOn()
    }

    @Test
    fun turningTheSongHeadingOffAppliesIt() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(showSongReference = true))

        click(LookTags.SHOW_SONG_REFERENCE)

        assertFalse(state.theme.showSongReference)
    }

    @Test
    fun turningTheSongHeadingOffLeavesTheBibleOneAlone() = runComposeUiTest {
        // The whole reason they are two switches.
        val state = showLook(initial = SlideTheme(showSongReference = true, showBibleReference = true))

        click(LookTags.SHOW_SONG_REFERENCE)

        assertTrue(state.theme.showBibleReference)
    }

    @Test
    fun turningTheBibleHeadingOffAppliesIt() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(showBibleReference = true))

        click(LookTags.SHOW_BIBLE_REFERENCE)

        assertFalse(state.theme.showBibleReference)
    }

    @Test
    fun turningTheOtherHeadingOffAppliesIt() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(showOtherReference = true))

        click(LookTags.SHOW_OTHER_REFERENCE)

        assertFalse(state.theme.showOtherReference)
    }

    @Test
    fun theClockCanBeTurnedOff() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(showClock = true))

        click(LookTags.SHOW_CLOCK)

        assertFalse(state.theme.showClock)
    }

    @Test
    fun autoFitCanBeTurnedOn() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(autoFitText = false))

        click(LookTags.AUTO_FIT)

        assertTrue(state.theme.autoFitText)
    }

    @Test
    fun autoFitShowsItsStateWhenOff() = runComposeUiTest {
        showLook(initial = SlideTheme(autoFitText = false))

        tagged(LookTags.AUTO_FIT).assertIsOff()
    }

    @Test
    fun ignoringLineBreaksCanBeTurnedOn() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(ignoreLineBreaks = false))

        click(LookTags.IGNORE_BREAKS)

        assertTrue(state.theme.ignoreLineBreaks)
    }

    @Test
    fun chordsCanBeTurnedOn() = runComposeUiTest {
        // Not part of the theme: it is the operator's view, not the room's.
        val state = showLook(initialShowChords = false)

        click(LookTags.SHOW_CHORDS)

        assertTrue(state.showChords)
    }

    @Test
    fun chordsCanBeTurnedOffAgain() = runComposeUiTest {
        val state = showLook(initialShowChords = true)

        click(LookTags.SHOW_CHORDS)

        assertFalse(state.showChords)
    }

    @Test
    fun theChordSwitchShowsItsState() = runComposeUiTest {
        showLook(initialShowChords = true)

        tagged(LookTags.SHOW_CHORDS).assertIsOn()
    }

    @Test
    fun oneSwitchDoesNotMoveAnother() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(showClock = true, autoFitText = false))

        click(LookTags.AUTO_FIT)

        assertTrue(state.theme.showClock)
    }

    // ── Starting over ────────────────────────────────────────────────────

    @Test
    fun theLookCanBeResetToTheDefault() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(textColor = "#FF0000", showClock = false))

        click(LookTags.RESET)

        assertEquals(SlideTheme(), state.theme)
    }

    @Test
    fun resettingPutsTheColoursBack() = runComposeUiTest {
        val state = showLook(initial = SlideTheme(gradientTop = "#123456"))

        click(LookTags.RESET)

        assertEquals(SlideTheme().gradientTop, state.theme.gradientTop)
    }

    @Test
    fun resettingDoesNotTouchTheChordSwitch() = runComposeUiTest {
        // Chords are the operator's own view, not part of the look.
        val state = showLook(initialShowChords = true)

        click(LookTags.RESET)

        assertTrue(state.showChords)
    }

    @Test
    fun resettingDoesNotTouchTheTextSize() = runComposeUiTest {
        val state = showLook(initialTextSize = SlideTextSize.LARGE)

        click(LookTags.RESET)

        assertEquals(SlideTextSize.LARGE, state.textSize)
    }
}
