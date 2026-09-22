package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.calendar.RepeatRule
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.type
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Copying a service forward: once, or every week, fortnight or month.
 *
 * The dates it would create are listed before anything is created, so nobody presses Create to
 * find out what they are getting.
 */
@OptIn(ExperimentalTestApi::class)
class CopyServiceContentTest {

    private var choice: CopyChoice? = null
    private var dismissed = 0

    private fun ComposeUiTest.show(service: PlannedService = CalendarFixtures.service) = showScreen {
        CopyServiceContent(service = service, onCopy = { choice = it }, onDismiss = { dismissed++ })
    }

    private fun ComposeUiTest.create() = click(CalendarTags.COPY_CONFIRM)

    private fun ComposeUiTest.rule(rule: RepeatRule) =
        click(CalendarTags.repeatRule(RepeatRule.entries.indexOf(rule)))

    @Test
    fun theServiceBeingCopiedIsNamed() = runComposeUiTest {
        show()

        assertTrue(isShowing(CalendarFixtures.SERVICE_NAME))
    }

    @Test
    fun itOpensOnWeeklyAndSaysWhichDatesThatMeans() = runComposeUiTest {
        show()

        // Weekly from Sunday 20 September: the 27th, then October.
        assertTrue(isShowing("September 27"))
    }

    @Test
    fun creatingHandsBackTheChoice() = runComposeUiTest {
        show()

        create()

        assertEquals(RepeatRule.WEEKLY, choice?.rule)
        assertTrue(choice?.includeRows == true)
    }

    @Test
    fun copyingOnceIsOneDate() = runComposeUiTest {
        show()

        rule(RepeatRule.ONCE)
        waitForIdle()
        create()

        assertEquals(RepeatRule.ONCE, choice?.rule)
    }

    @Test
    fun fortnightlySkipsAWeek() = runComposeUiTest {
        show()

        rule(RepeatRule.FORTNIGHTLY)
        waitForIdle()

        assertTrue(isShowing("October 4"), "two weeks after the 20th")
    }

    @Test
    fun monthlyLandsInTheNextMonth() = runComposeUiTest {
        show()

        rule(RepeatRule.MONTHLY)
        waitForIdle()

        assertTrue(isShowing("October 20"))
    }

    @Test
    fun howManyTimesIsAskedOnlyWhenItRepeats() = runComposeUiTest {
        show()
        assertTrue(exists(CalendarTags.COPY_COUNT))

        rule(RepeatRule.ONCE)
        waitForIdle()

        assertTrue(!exists(CalendarTags.COPY_COUNT), "once is once; there is no count to ask for")
    }

    @Test
    fun theCountIsCarriedThrough() = runComposeUiTest {
        show()

        type(CalendarTags.COPY_COUNT, "5")
        waitForIdle()
        create()

        assertEquals(5, choice?.count)
    }

    @Test
    fun aCountThatIsNotANumberFallsBackToOne() = runComposeUiTest {
        show()

        type(CalendarTags.COPY_COUNT, "")
        waitForIdle()
        create()

        assertEquals(1, choice?.count)
    }

    @Test
    fun theRunOfShowCanBeLeftBehind() = runComposeUiTest {
        show()

        click(CalendarTags.COPY_INCLUDE_ROWS)
        waitForIdle()
        create()

        assertFalse(choice?.includeRows == true)
    }

    @Test
    fun theAutomationIsOfferedOnlyWhenThereIsSome() = runComposeUiTest {
        val withCue = CalendarFixtures.service.copy(
            rows = CalendarFixtures.service.rows + PlanRow.Ref(id = "c", title = "Go live", kind = RowKind.CUE),
        )
        show(withCue)

        assertTrue(exists(CalendarTags.COPY_INCLUDE_CUES))
    }

    @Test
    fun closingCopiesNothing() = runComposeUiTest {
        show()

        click(CalendarTags.COPY_CANCEL)

        assertEquals(1, dismissed)
        assertNull(choice)
    }
}
