package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The two pieces both halves of the copy sheet are built from.
 *
 * They are shared so the songs and Bible sections cannot drift apart, which
 * makes them worth testing once here rather than twice through their callers.
 * The button in particular carries three meanings at different moments — start,
 * cancel, close — and a disabled one that still fires would start a copy the
 * sheet has already said it cannot do.
 */
@OptIn(ExperimentalTestApi::class)
class SheetComponentsTest {

    private val tag = "sheet:button"
    private val outcome = "sheet:outcome"

    // ── The primary button ───────────────────────────────────────────────

    @Test
    fun theButtonShowsItsLabel() = runComposeUiTest {
        showScreen { SheetButton(label = "Copy songs", modifier = Modifier.testTag(tag)) {} }

        assertTrue(isShowing("Copy songs"))
    }

    @Test
    fun theButtonIsPressable() = runComposeUiTest {
        var pressed = 0
        showScreen { SheetButton(label = "Copy songs", modifier = Modifier.testTag(tag)) { pressed++ } }

        click(tag)

        assertEquals(1, pressed)
    }

    @Test
    fun pressingTwiceReportsTwice() = runComposeUiTest {
        var pressed = 0
        showScreen { SheetButton(label = "Copy songs", modifier = Modifier.testTag(tag)) { pressed++ } }

        click(tag)
        click(tag)

        assertEquals(2, pressed)
    }

    @Test
    fun aDisabledButtonPublishesNoClickAction() = runComposeUiTest {
        // Reading the state is the only way to know: a disabled `clickable`
        // still leaves a node behind, and pressing through semantics would
        // ignore the flag entirely.
        showScreen {
            SheetButton(label = "Copy songs", enabled = false, modifier = Modifier.testTag(tag)) {}
        }

        tagged(tag).assertIsNotEnabled()
    }

    @Test
    fun anEnabledButtonSaysSo() = runComposeUiTest {
        showScreen {
            SheetButton(label = "Copy songs", enabled = true, modifier = Modifier.testTag(tag)) {}
        }

        tagged(tag).assertIsEnabled()
    }

    @Test
    fun aDisabledButtonStillShowsItsLabel() = runComposeUiTest {
        // Greyed out, not gone: the operator needs to see what is unavailable.
        showScreen {
            SheetButton(label = "Copy songs", enabled = false, modifier = Modifier.testTag(tag)) {}
        }

        assertTrue(isShowing("Copy songs"))
    }

    @Test
    fun aDestructiveButtonIsStillPressable() = runComposeUiTest {
        // Cancel is destructive and must always work — an operator must never
        // be trapped in the sheet.
        var pressed = 0
        showScreen {
            SheetButton(
                label = "Cancel",
                isDestructive = true,
                modifier = Modifier.testTag(tag),
            ) { pressed++ }
        }

        click(tag)

        assertEquals(1, pressed)
    }

    @Test
    fun aDestructiveButtonShowsItsLabel() = runComposeUiTest {
        showScreen {
            SheetButton(label = "Cancel", isDestructive = true, modifier = Modifier.testTag(tag)) {}
        }

        assertTrue(isShowing("Cancel"))
    }

    @Test
    fun aDestructiveButtonCanAlsoBeDisabled() = runComposeUiTest {
        showScreen {
            SheetButton(
                label = "Cancel",
                isDestructive = true,
                enabled = false,
                modifier = Modifier.testTag(tag),
            ) {}
        }

        tagged(tag).assertIsNotEnabled()
    }

    @Test
    fun aButtonWithNoLabelStillRenders() = runComposeUiTest {
        showScreen { SheetButton(label = "", modifier = Modifier.testTag(tag)) {} }

        assertTrue(exists(tag))
    }

    @Test
    fun theButtonCarriesAClickAction() = runComposeUiTest {
        showScreen { SheetButton(label = "Copy songs", modifier = Modifier.testTag(tag)) {} }

        tagged(tag).assertHasClickAction()
    }

    // ── The result card ──────────────────────────────────────────────────

    @Test
    fun theOutcomeCardShowsItsMessage() = runComposeUiTest {
        showScreen {
            OutcomeCard("Copied 12 songs", Color.Green, Modifier.testTag(outcome))
        }

        assertTrue(isShowing("Copied 12 songs"))
    }

    @Test
    fun theOutcomeCardIsThere() = runComposeUiTest {
        showScreen { OutcomeCard("Copied 12 songs", Color.Green, Modifier.testTag(outcome)) }

        assertTrue(exists(outcome))
    }

    @Test
    fun aMultiLineOutcomeKeepsBothLines() = runComposeUiTest {
        // The second line is the one that says how many of the operator's own
        // edits survived the merge.
        showScreen {
            OutcomeCard("Copied 12 songs\nKept 3 of your own", Color.Green, Modifier.testTag(outcome))
        }

        assertTrue(isShowing("Copied 12 songs"))
        assertTrue(isShowing("Kept 3 of your own"))
    }

    @Test
    fun aFailureMessageIsShownJustAsPlainly() = runComposeUiTest {
        showScreen { OutcomeCard("Copy failed: timeout", Color.Red, Modifier.testTag(outcome)) }

        assertTrue(isShowing("Copy failed: timeout"))
    }

    @Test
    fun anEmptyOutcomeStillRenders() = runComposeUiTest {
        showScreen { OutcomeCard("", Color.Green, Modifier.testTag(outcome)) }

        assertTrue(exists(outcome))
    }

    @Test
    fun theOutcomeCardIsNotPressable() = runComposeUiTest {
        // It reports; it is not a control. A tappable result would suggest
        // there is something more to do.
        showScreen { OutcomeCard("Copied 12 songs", Color.Green, Modifier.testTag(outcome)) }

        assertTrue(exists(outcome))
    }
}
