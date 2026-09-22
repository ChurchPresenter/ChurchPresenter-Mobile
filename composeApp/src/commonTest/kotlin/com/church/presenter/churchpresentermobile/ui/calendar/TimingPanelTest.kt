package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.RowEnd
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Starts · Runs · Repeats · At end — the four chip rows that say when a row fires by itself.
 *
 * The chips are in a fixed order, which is what a test can hold on to: their labels come from
 * `strings.xml` and render empty here, while the numbers among them (2, 3) are drawn from data.
 */
@OptIn(ExperimentalTestApi::class)
class TimingPanelTest {

    private var changed: TimingDraft? = null

    private fun ComposeUiTest.show(draft: TimingDraft = TimingDraft(), enabled: Boolean = true) = showScreen {
        TimingPanel(draft = draft, onChange = { changed = it }, serviceStart = "10:00", enabled = enabled)
    }

    private fun ComposeUiTest.chips() = onAllNodes(hasClickAction(), useUnmergedTree = true)

    /**
     * The chips in order: Cued, After prev, −15, −5, On time (5), then the four run choices (9),
     * Once, Loop, 2, 3 (13), then Hold, Next, Blank (16).
     */
    private fun ComposeUiTest.chip(index: Int) = chips()[index].performClick()

    @Test
    fun everyChoiceIsOfferedAtOnce() = runComposeUiTest {
        show()

        assertEquals(17, chips().fetchSemanticsNodes().size)
    }

    @Test
    fun cuedIsWhereARowStarts() = runComposeUiTest {
        show(TimingDraft(startOffsetMinutes = 5))

        chip(0)

        assertNull(changed?.startOffsetMinutes)
        assertEquals(false, changed?.followsPrevious)
    }

    @Test
    fun aRowCanBeSetToFollowTheOneBeforeIt() = runComposeUiTest {
        show()

        chip(1)

        assertEquals(true, changed?.followsPrevious)
        assertNull(changed?.startOffsetMinutes, "it waits for the row, not the clock")
    }

    @Test
    fun theTwoEarlyOffsetsAreBeforeTheServiceStarts() = runComposeUiTest {
        show()

        chip(2)
        assertEquals(-15, changed?.startOffsetMinutes)

        chip(3)
        assertEquals(-5, changed?.startOffsetMinutes)
    }

    @Test
    fun onTimeIsAnOffsetOfNone() = runComposeUiTest {
        show()

        chip(4)

        assertEquals(0, changed?.startOffsetMinutes)
        assertEquals(false, changed?.followsPrevious)
    }

    @Test
    fun pinningARowClearsFollowsPrevious() = runComposeUiTest {
        show(TimingDraft(followsPrevious = true))

        chip(4)

        assertEquals(false, changed?.followsPrevious)
    }

    @Test
    fun theItemsOwnLengthIsTheFirstRunChoice() = runComposeUiTest {
        show(TimingDraft(runSeconds = 300))

        chip(5)

        assertNull(changed?.runSeconds)
    }

    @Test
    fun theRunChoicesAreMinutes() = runComposeUiTest {
        show()

        chip(6)
        assertEquals(300, changed?.runSeconds, "5 minutes")

        chip(9)
        assertEquals(3600, changed?.runSeconds, "an hour")
    }

    @Test
    fun onceIsTheDefaultAndCanBeReturnedTo() = runComposeUiTest {
        show(TimingDraft(repeats = 0))

        chip(10)

        assertEquals(1, changed?.repeats)
    }

    @Test
    fun loopingIsZeroRepeats() = runComposeUiTest {
        show()

        chip(11)

        assertEquals(0, changed?.repeats, "0 means until something else goes live")
    }

    @Test
    fun aCountedRepeatIsOfferedAsANumber() = runComposeUiTest {
        show()

        chip(12)
        assertEquals(2, changed?.repeats)

        chip(13)
        assertEquals(3, changed?.repeats)
    }

    @Test
    fun theCountedRepeatsAreLabelledWithTheirNumbers() = runComposeUiTest {
        show()

        assertTrue(isShowing("2"))
        assertTrue(isShowing("3"))
    }

    @Test
    fun whatHappensAtTheEndIsOneOfThree() = runComposeUiTest {
        show()

        chip(14)
        assertEquals(RowEnd.HOLD, changed?.atEnd)

        chip(15)
        assertEquals(RowEnd.NEXT, changed?.atEnd)

        chip(16)
        assertEquals(RowEnd.BLANK, changed?.atEnd)
    }

    @Test
    fun theSummaryNamesTheClockTimeARowIsPinnedTo() = runComposeUiTest {
        show(TimingDraft(startOffsetMinutes = -5))

        assertTrue(isShowing("9:55"), "five minutes before a 10:00 service")
    }

    @Test
    fun aDisabledPanelChangesNothing() = runComposeUiTest {
        show(enabled = false)

        chip(1)

        assertNull(changed, "a section has no timing, so its panel is read-only")
    }

    @Test
    fun aPanelForAServiceWithNoStartTimeStillDraws() = runComposeUiTest {
        showScreen {
            TimingPanel(TimingDraft(startOffsetMinutes = 10), onChange = { changed = it }, serviceStart = "")
        }

        assertEquals(17, chips().fetchSemanticsNodes().size)
    }
}
