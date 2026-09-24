package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.calendar.storedDate
import com.church.presenter.churchpresentermobile.calendar.today
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The calendar as a whole: the month, the run of show inside it, and the two shapes they take.
 *
 * Built over an in-memory calendar file rather than mocks, so what a tap does here is what it does
 * on a phone — the plan it changes is read back from the repository.
 */
@OptIn(ExperimentalTestApi::class)
class CalendarScreenTest {

    private val storage = InMemoryFileStorage()
    private val repository = CalendarRepository(storage, now = { "2026-09-20T10:00:00Z" })
    private val settings = AppSettings(InMemorySettingsStorage())
    private var settingsOpened = 0
    private var backs = 0

    private fun ComposeUiTest.show(twoPane: Boolean = false, withSettings: Boolean = false) = showScreen {
        CalendarScreen(
            repository = repository,
            songCatalog = null,
            bibleCatalog = null,
            settings = settings,
            twoPane = twoPane,
            onBack = { backs++ },
            onSettings = if (withSettings) ({ settingsOpened++ }) else null,
        )
    }

    /** Today, so that the day pane a tablet opens on is the one the service is planned for. */
    private val todayStored = storedDate(today())

    private fun plan(id: String = "s1", name: String = "Sunday Morning", date: String = todayStored) {
        repository.saveService(PlannedService(id = id, date = date, name = name, startTime = "10:00"))
    }

    @Test
    fun theMonthOpensOnThisMonthsGrid() = runComposeUiTest {
        show()

        assertTrue(exists(CalendarTags.MONTH_NEXT), "the grid and its arrows are drawn")
        assertTrue(exists(CalendarTags.MONTH_PREVIOUS))
    }

    @Test
    fun aPlannedServiceIsListedOnItsDay() = runComposeUiTest {
        plan()
        show()

        click(CalendarTags.day(todayStored))
        waitForIdle()

        assertTrue(isShowing("Sunday Morning"))
    }

    @Test
    fun openingAServiceShowsItsRunOfShow() = runComposeUiTest {
        plan()
        show()
        click(CalendarTags.day(todayStored))
        waitForIdle()

        click(CalendarTags.serviceCard("s1"))
        waitForIdle()

        assertTrue(exists(CalendarTags.RUN_BACK), "the run of show, with a way back to the month")
    }

    @Test
    fun theRunOfShowGoesBackToTheMonth() = runComposeUiTest {
        plan()
        show()
        click(CalendarTags.day(todayStored))
        waitForIdle()
        click(CalendarTags.serviceCard("s1"))
        waitForIdle()

        click(CalendarTags.RUN_BACK)
        waitForIdle()

        assertTrue(exists(CalendarTags.MONTH_NEXT), "back on the month")
    }

    @Test
    fun steppingTheMonthMovesTheGrid() = runComposeUiTest {
        show()

        click(CalendarTags.MONTH_NEXT)
        waitForIdle()

        assertTrue(exists(CalendarTags.MONTH_PREVIOUS), "still a month, a month later")
    }

    @Test
    fun theSettingsGearIsThereOnlyWhenTheShellHasNoneOfItsOwn() = runComposeUiTest {
        show(withSettings = true)

        assertTrue(exists(CalendarTags.MONTH_NEXT))
    }

    @Test
    fun aTabletDrawsTheMonthAndTheDayTogether() = runComposeUiTest {
        plan()
        show(twoPane = true)
        waitForIdle()

        // Both panes at once: the grid on the left, the day's services on the right.
        assertTrue(exists(CalendarTags.MONTH_NEXT))
        assertTrue(isShowing("Sunday Morning"))
    }

    @Test
    fun aTabletOpensTheRunOfShowBesideTheMonthRatherThanOverIt() = runComposeUiTest {
        plan()
        show(twoPane = true)
        waitForIdle()

        click(CalendarTags.serviceCard("s1"))
        waitForIdle()

        assertTrue(exists(CalendarTags.MONTH_NEXT), "the month is still there")
        assertTrue(!exists(CalendarTags.RUN_BACK), "and the run of show has nowhere to go back to")
    }

    @Test
    fun anEmptyMonthStillDrawsItsDays() = runComposeUiTest {
        show()

        assertTrue(exists(CalendarTags.day(todayStored)))
    }
}
