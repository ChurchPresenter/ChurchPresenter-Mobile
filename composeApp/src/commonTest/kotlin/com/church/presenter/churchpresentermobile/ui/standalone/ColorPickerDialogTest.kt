package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Picking a colour by dragging rather than by knowing its hex.
 *
 * The rule the dialog exists to keep is that nothing reaches the audience
 * screen until Save: the theme rides inside every slide, so writing through on
 * each frame of a drag would repaint the wall while somebody is still choosing.
 * The other half is that it opens on the colour already set — starting from
 * black every time makes a small adjustment impossible.
 */
@OptIn(ExperimentalTestApi::class)
class ColorPickerDialogTest {

    // ── What it opens on ─────────────────────────────────────────────────

    @Test
    fun thePickerOpensOnTheColourItWasGiven() = runComposeUiTest {
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FF8800", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("#FF8800"))
    }

    @Test
    fun theHexLineIsShown() = runComposeUiTest {
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FF8800", onPick = {}, onDismiss = {})
        }

        assertTrue(exists(COLOR_PICKER_HEX))
    }

    @Test
    fun aShortFormColourOpensInFull() = runComposeUiTest {
        // #F80 and #FF8800 are the same colour; the dialog works in the long form.
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#F80", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("#FF8800"))
    }

    @Test
    fun aColourWithoutItsHashStillOpens() = runComposeUiTest {
        showScreen {
            ColorPickerDialog(title = "Text", initial = "FF8800", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("#FF8800"))
    }

    @Test
    fun anUnreadableColourOpensOnBlack() = runComposeUiTest {
        // Half a hex code is what a field looks like mid-typing; the dialog has
        // to start somewhere rather than refuse to open.
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FF", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("#000000"))
    }

    @Test
    fun anEmptyColourOpensOnBlack() = runComposeUiTest {
        showScreen {
            ColorPickerDialog(title = "Text", initial = "", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("#000000"))
    }

    @Test
    fun aWordOpensOnBlack() = runComposeUiTest {
        showScreen {
            ColorPickerDialog(title = "Text", initial = "purple", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("#000000"))
    }

    @Test
    fun blackOpensAsBlack() = runComposeUiTest {
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#000000", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("#000000"))
    }

    @Test
    fun whiteOpensAsWhite() = runComposeUiTest {
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FFFFFF", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("#FFFFFF"))
    }

    @Test
    fun aLowerCaseColourIsShownInUpperCase() = runComposeUiTest {
        // One shape on the wire, so every renderer sees the same value.
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#ff8800", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("#FF8800"))
    }

    @Test
    fun theTitleNamesWhatIsBeingPicked() = runComposeUiTest {
        // Four colour fields open the same dialog; without the title there is
        // nothing to say which.
        showScreen {
            ColorPickerDialog(title = "Gradient top", initial = "#FF8800", onPick = {}, onDismiss = {})
        }

        assertTrue(isShowing("Gradient top"))
    }

    // ── Saving and cancelling ────────────────────────────────────────────

    @Test
    fun savingHandsBackTheColour() = runComposeUiTest {
        var picked: String? = null
        showScreen {
            ColorPickerDialog(
                title = "Text",
                initial = "#FF8800",
                onPick = { picked = it },
                onDismiss = {},
            )
        }

        click(COLOR_PICKER_SAVE)

        assertEquals("#FF8800", picked)
    }

    @Test
    fun savingHandsBackAShortFormColourInFull() = runComposeUiTest {
        var picked: String? = null
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#F80", onPick = { picked = it }, onDismiss = {})
        }

        click(COLOR_PICKER_SAVE)

        assertEquals("#FF8800", picked)
    }

    @Test
    fun savingAnUnreadableColourHandsBackBlack() = runComposeUiTest {
        // Whatever it started from, what leaves is a colour.
        var picked: String? = null
        showScreen {
            ColorPickerDialog(title = "Text", initial = "nonsense", onPick = { picked = it }, onDismiss = {})
        }

        click(COLOR_PICKER_SAVE)

        assertEquals("#000000", picked)
    }

    @Test
    fun savingCloses() = runComposeUiTest {
        var dismissed = 0
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FF8800", onPick = {}, onDismiss = { dismissed++ })
        }

        click(COLOR_PICKER_SAVE)

        assertEquals(1, dismissed)
    }

    @Test
    fun cancellingHandsBackNothing() = runComposeUiTest {
        // Dragging three sliders and thinking better of it must leave the wall
        // as it was.
        var picked: String? = null
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FF8800", onPick = { picked = it }, onDismiss = {})
        }

        click(COLOR_PICKER_CANCEL)

        assertNull(picked)
    }

    @Test
    fun cancellingCloses() = runComposeUiTest {
        var dismissed = 0
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FF8800", onPick = {}, onDismiss = { dismissed++ })
        }

        click(COLOR_PICKER_CANCEL)

        assertEquals(1, dismissed)
    }

    @Test
    fun openingAloneChangesNothing() = runComposeUiTest {
        // The colour is handed back on Save and at no other moment.
        var picked: String? = null
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FF8800", onPick = { picked = it }, onDismiss = {})
        }

        assertNull(picked)
    }

    @Test
    fun bothWaysOutAreOffered() = runComposeUiTest {
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FF8800", onPick = {}, onDismiss = {})
        }

        assertTrue(exists(COLOR_PICKER_SAVE))
        assertTrue(exists(COLOR_PICKER_CANCEL))
    }

    // ── The value it produces ────────────────────────────────────────────

    @Test
    fun aChannelAtFullReadsAsFF() {
        assertEquals("#FF0000", rgbToHex(255, 0, 0))
    }

    @Test
    fun aChannelAtZeroReadsAsZero() {
        assertEquals("#000000", rgbToHex(0, 0, 0))
    }

    @Test
    fun theChannelsAreRedGreenBlueInThatOrder() {
        assertEquals("#010203", rgbToHex(1, 2, 3))
    }

    @Test
    fun everyChannelIsTwoDigits() {
        // "#123" would be a different colour entirely.
        assertEquals(7, rgbToHex(1, 2, 3).length)
    }

    @Test
    fun theHexItProducesReadsBackAsTheSameColour() {
        // The value goes out to the wire and comes back through the parser.
        val hex = rgbToHex(18, 52, 86)

        assertEquals(hex, normaliseHex(hex))
    }

    @Test
    fun theHexItProducesIsAReadableColour() {
        assertTrue(parseHexColorOrNull(rgbToHex(18, 52, 86)) != null)
    }

    @Test
    fun aColourSurvivesTheRoundTripThroughTheDialog() = runComposeUiTest {
        var picked: String? = null
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#123456", onPick = { picked = it }, onDismiss = {})
        }

        click(COLOR_PICKER_SAVE)

        assertEquals("#123456", picked)
        assertTrue(parseHexColorOrNull(picked!!) != null)
    }

    @Test
    fun aNearBlackColourIsNotRoundedToBlack() = runComposeUiTest {
        // Each channel is stored as a whole number 0–255; a rounding slip here
        // would quietly flatten dark themes.
        var picked: String? = null
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#010101", onPick = { picked = it }, onDismiss = {})
        }

        click(COLOR_PICKER_SAVE)

        assertEquals("#010101", picked)
    }

    @Test
    fun aNearWhiteColourIsNotRoundedToWhite() = runComposeUiTest {
        var picked: String? = null
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FEFEFE", onPick = { picked = it }, onDismiss = {})
        }

        click(COLOR_PICKER_SAVE)

        assertEquals("#FEFEFE", picked)
    }

    @Test
    fun aMidGreyIsNotShifted() = runComposeUiTest {
        var picked: String? = null
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#808080", onPick = { picked = it }, onDismiss = {})
        }

        click(COLOR_PICKER_SAVE)

        assertEquals("#808080", picked)
    }

    @Test
    fun anEightDigitColourIsNotReadAsOne() = runComposeUiTest {
        // An alpha channel is not something this app carries; it opens on black
        // rather than dropping a channel silently.
        showScreen {
            ColorPickerDialog(title = "Text", initial = "#FF8800FF", onPick = {}, onDismiss = {})
        }

        assertFalse(isShowing("#FF8800"))
    }
}
