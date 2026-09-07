package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Choosing a colour for the audience screen.
 *
 * Someone who knows the hex types it; someone who does not drags the sliders.
 * Both routes end at the same place, and the rule that keeps the screen usable
 * while they get there is that a half-typed value is *kept on screen but not
 * published*: "#2A1D" is on the way to something, not a colour to project.
 */
@OptIn(ExperimentalTestApi::class)
class ColorFieldTest {

    private val tag = "look:gradientTop"

    // ── Reading what was typed ───────────────────────────────────────────

    @Test
    fun aSixDigitHexIsAColour() {
        assertNotNull(parseHexColorOrNull("#2A1D5E"))
    }

    @Test
    fun theHashIsOptional() {
        assertNotNull(parseHexColorOrNull("2A1D5E"))
    }

    @Test
    fun aThreeDigitHexIsAColour() {
        assertNotNull(parseHexColorOrNull("#0AF"))
    }

    @Test
    fun aThreeDigitHexMeansTheSameAsItsLongForm() {
        assertEquals(parseHexColorOrNull("#00AAFF"), parseHexColorOrNull("#0AF"))
    }

    @Test
    fun lowerCaseIsRead() {
        assertEquals(parseHexColorOrNull("#2a1d5e"), parseHexColorOrNull("#2A1D5E"))
    }

    @Test
    fun surroundingSpaceIsIgnored() {
        assertEquals(parseHexColorOrNull("  #2A1D5E  "), parseHexColorOrNull("#2A1D5E"))
    }

    @Test
    fun aHalfTypedValueIsNotAColourYet() {
        assertNull(parseHexColorOrNull("#2A1D"))
    }

    @Test
    fun anEmptyFieldIsNotAColour() {
        assertNull(parseHexColorOrNull(""))
    }

    @Test
    fun justAHashIsNotAColour() {
        assertNull(parseHexColorOrNull("#"))
    }

    @Test
    fun lettersPastFAreNotAColour() {
        assertNull(parseHexColorOrNull("#GGGGGG"))
    }

    @Test
    fun aWordIsNotAColour() {
        assertNull(parseHexColorOrNull("purple"))
    }

    @Test
    fun eightDigitsAreNotAColour() {
        // Alpha is not the operator's to set here; the renderer owns it.
        assertNull(parseHexColorOrNull("#2A1D5EFF"))
    }

    @Test
    fun blackIsAColour() {
        assertEquals(Color.Black, parseHexColorOrNull("#000000"))
    }

    @Test
    fun whiteIsAColour() {
        assertEquals(Color.White, parseHexColorOrNull("#FFFFFF"))
    }

    // ── What gets stored ─────────────────────────────────────────────────

    @Test
    fun aStoredColourAlwaysCarriesItsHash() {
        assertEquals("#2A1D5E", normaliseHex("2A1D5E"))
    }

    @Test
    fun aStoredColourIsUpperCase() {
        // Every renderer sees the same shape, so two equal colours compare equal.
        assertEquals("#2A1D5E", normaliseHex("#2a1d5e"))
    }

    @Test
    fun aShortFormIsStoredInFull() {
        assertEquals("#00AAFF", normaliseHex("#0AF"))
    }

    @Test
    fun surroundingSpaceIsNotStored() {
        assertEquals("#2A1D5E", normaliseHex("  #2A1D5E  "))
    }

    // ── The field on screen ──────────────────────────────────────────────

    @Test
    fun theFieldShowsTheColourItWasGiven() = runComposeUiTest {
        showScreen { ColorField(label = "Top", value = "#2A1D5E", onValueChange = {}, tag = tag) }

        assertTrue(exists(tag))
    }

    @Test
    fun theFieldOffersASwatchToOpenThePicker() = runComposeUiTest {
        showScreen { ColorField(label = "Top", value = "#2A1D5E", onValueChange = {}, tag = tag) }

        assertTrue(exists("$tag:swatch"))
    }

    @Test
    fun typingACompleteColourPublishesIt() = runComposeUiTest {
        var published: String? = null
        showScreen {
            ColorField(label = "Top", value = "#000000", onValueChange = { published = it }, tag = tag)
        }

        type(tag, "#2A1D5E")

        assertEquals("#2A1D5E", published)
    }

    @Test
    fun typingPublishesTheStoredShape() = runComposeUiTest {
        var published: String? = null
        showScreen {
            ColorField(label = "Top", value = "#000000", onValueChange = { published = it }, tag = tag)
        }

        type(tag, "2a1d5e")

        assertEquals("#2A1D5E", published)
    }

    @Test
    fun aHalfTypedColourIsNotPublished() = runComposeUiTest {
        // Publishing it would repaint the audience screen with whatever "#2A1D"
        // happened to parse as.
        var published: String? = null
        showScreen {
            ColorField(label = "Top", value = "#000000", onValueChange = { published = it }, tag = tag)
        }

        type(tag, "#2A1D")

        assertNull(published)
    }

    @Test
    fun aHalfTypedColourStaysOnScreen() = runComposeUiTest {
        // Kept so the operator can carry on typing it.
        showScreen { ColorField(label = "Top", value = "#000000", onValueChange = {}, tag = tag) }

        type(tag, "#2A1D")

        assertTrue(isShowing("#2A1D"))
    }

    @Test
    fun clearingTheFieldPublishesNothing() = runComposeUiTest {
        var published: String? = null
        showScreen {
            ColorField(label = "Top", value = "#2A1D5E", onValueChange = { published = it }, tag = tag)
        }

        type(tag, "")

        assertNull(published)
    }

    @Test
    fun aThreeDigitColourIsPublishedInFull() = runComposeUiTest {
        var published: String? = null
        showScreen {
            ColorField(label = "Top", value = "#000000", onValueChange = { published = it }, tag = tag)
        }

        type(tag, "#0AF")

        assertEquals("#00AAFF", published)
    }

    // ── The picker behind the swatch ─────────────────────────────────────

    @Test
    fun noPickerIsOpenToBeginWith() = runComposeUiTest {
        showScreen { ColorField(label = "Top", value = "#2A1D5E", onValueChange = {}, tag = tag) }

        assertFalse(exists(COLOR_PICKER_SAVE))
    }

    @Test
    fun theSwatchOpensThePicker() = runComposeUiTest {
        showScreen { ColorField(label = "Top", value = "#2A1D5E", onValueChange = {}, tag = tag) }

        click("$tag:swatch")

        assertTrue(exists(COLOR_PICKER_SAVE))
    }

    @Test
    fun thePickerShowsTheColourItWouldSave() = runComposeUiTest {
        showScreen { ColorField(label = "Top", value = "#2A1D5E", onValueChange = {}, tag = tag) }

        click("$tag:swatch")

        assertTrue(exists(COLOR_PICKER_HEX))
    }

    @Test
    fun thePickerOpensOnTheColourAlreadySet() = runComposeUiTest {
        showScreen { ColorField(label = "Top", value = "#2A1D5E", onValueChange = {}, tag = tag) }

        click("$tag:swatch")

        assertTrue(isShowing("#2A1D5E"))
    }

    @Test
    fun cancellingThePickerPublishesNothing() = runComposeUiTest {
        var published: String? = null
        showScreen {
            ColorField(label = "Top", value = "#2A1D5E", onValueChange = { published = it }, tag = tag)
        }
        click("$tag:swatch")

        click(COLOR_PICKER_CANCEL)

        assertNull(published)
    }

    @Test
    fun cancellingClosesThePicker() = runComposeUiTest {
        showScreen { ColorField(label = "Top", value = "#2A1D5E", onValueChange = {}, tag = tag) }
        click("$tag:swatch")

        click(COLOR_PICKER_CANCEL)

        assertFalse(exists(COLOR_PICKER_SAVE))
    }

    @Test
    fun savingPublishesTheChosenColour() = runComposeUiTest {
        // Only on save: dragging a slider must not repaint the audience screen
        // on every frame.
        var published: String? = null
        showScreen {
            ColorField(label = "Top", value = "#2A1D5E", onValueChange = { published = it }, tag = tag)
        }
        click("$tag:swatch")

        click(COLOR_PICKER_SAVE)

        assertEquals("#2A1D5E", published)
    }

    @Test
    fun savingClosesThePicker() = runComposeUiTest {
        showScreen { ColorField(label = "Top", value = "#2A1D5E", onValueChange = {}, tag = tag) }
        click("$tag:swatch")

        click(COLOR_PICKER_SAVE)

        assertFalse(exists(COLOR_PICKER_SAVE))
    }

    @Test
    fun aFieldWithAnUnreadableValueStillOpensThePicker() = runComposeUiTest {
        // Black is the fallback; the operator is not stuck with a broken field.
        showScreen { ColorField(label = "Top", value = "nonsense", onValueChange = {}, tag = tag) }

        click("$tag:swatch")

        assertTrue(exists(COLOR_PICKER_SAVE))
    }

    // ── Channels to hex ──────────────────────────────────────────────────

    @Test
    fun blackIsAllChannelsAtZero() {
        assertEquals("#000000", rgbToHex(0, 0, 0))
    }

    @Test
    fun whiteIsAllChannelsFull() {
        assertEquals("#FFFFFF", rgbToHex(255, 255, 255))
    }

    @Test
    fun eachChannelIsTwoDigits() {
        assertEquals("#0A0B0C", rgbToHex(10, 11, 12))
    }

    @Test
    fun theChannelsKeepTheirOrder() {
        assertEquals("#FF0000", rgbToHex(255, 0, 0))
        assertEquals("#00FF00", rgbToHex(0, 255, 0))
        assertEquals("#0000FF", rgbToHex(0, 0, 255))
    }

    @Test
    fun aChannelPastFullIsClamped() {
        assertEquals("#FFFFFF", rgbToHex(300, 300, 300))
    }

    @Test
    fun aNegativeChannelIsClamped() {
        assertEquals("#000000", rgbToHex(-5, -5, -5))
    }

    @Test
    fun whatTheHexSaysIsWhatTheFieldWouldRead() {
        // The two halves have to agree, or the picker's own output would come
        // back as a half-typed value.
        val hex = rgbToHex(42, 29, 94)
        assertNotNull(parseHexColorOrNull(hex))
        assertEquals(hex, normaliseHex(hex))
    }
}
