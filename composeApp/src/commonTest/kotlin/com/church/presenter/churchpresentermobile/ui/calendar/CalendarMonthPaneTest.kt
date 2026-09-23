package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The month a service is planned in: the grid, the day under it, and the legend beside them.
 *
 * What a test can read here is the data — day numbers, service names, times — because a label from
 * `strings.xml` renders empty in this runtime. That is enough: every one of these is about which
 * day is which and what happens when it is tapped.
 */
@OptIn(ExperimentalTestApi::class)
class CalendarMonthPaneTest {

    private val september = com.church.presenter.churchpresentermobile.calendar.YearMonthRef(2026, 9)

    private fun clickables(test: androidx.compose.ui.test.ComposeUiTest) =
        test.onAllNodes(hasClickAction(), useUnmergedTree = true)

    // ── The grid ─────────────────────────────────────────────────────────

    @Test
    fun everyDayOfTheMonthIsDrawn() = runComposeUiTest {
        showScreen {
            MonthGrid(september, CalendarFixtures.SUNDAY, CalendarFixtures.TODAY, CalendarFixtures.month, {}, {}, {})
        }

        // September has 30 days; the grid also carries the trailing days of August and October.
        assertTrue(isShowing("1"))
        assertTrue(isShowing("20"))
        assertTrue(isShowing("30"))
    }

    @Test
    fun tappingADayReportsIt() = runComposeUiTest {
        var picked: LocalDate? = null
        showScreen {
            MonthGrid(september, CalendarFixtures.SUNDAY, CalendarFixtures.TODAY, emptyList(), { picked = it }, {}, {})
        }

        click(CalendarTags.day("2026-09-23"))

        assertEquals(LocalDate(2026, 9, 23), picked)
    }

    @Test
    fun theMonthCanBeStepped() = runComposeUiTest {
        var back = 0
        var forward = 0
        showScreen {
            MonthGrid(
                september,
                CalendarFixtures.SUNDAY,
                CalendarFixtures.TODAY,
                emptyList(),
                {},
                { back++ },
                { forward++ },
            )
        }

        click(CalendarTags.MONTH_PREVIOUS)
        click(CalendarTags.MONTH_NEXT)

        assertEquals(1, back)
        assertEquals(1, forward)
    }

    @Test
    fun aMonthWithNoServicesStillDrawsItsDays() = runComposeUiTest {
        showScreen {
            MonthGrid(september, CalendarFixtures.SUNDAY, CalendarFixtures.TODAY, emptyList(), {}, {}, {})
        }

        assertTrue(isShowing("15"))
    }

    // ── The legend ───────────────────────────────────────────────────────

    @Test
    fun theLegendCountsEachKindInTheMonth() = runComposeUiTest {
        showScreen { ServiceTypesLegend(CalendarFixtures.month) }

        // One of each kind in the fixture month.
        assertEquals(3, onAllNodes(hasText("1"), useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun theLegendCountsZeroForAKindNobodyPlanned() = runComposeUiTest {
        showScreen { ServiceTypesLegend(listOf(CalendarFixtures.service)) }

        assertTrue(isShowing("0"), "the kinds nobody used are still listed, at zero")
    }

    // ── The day ──────────────────────────────────────────────────────────

    @Test
    fun theDayListsEachServiceOnIt() = runComposeUiTest {
        showScreen {
            DayServices(CalendarFixtures.SUNDAY, CalendarFixtures.month, null, {}, {}, null)
        }

        assertTrue(isShowing(CalendarFixtures.SERVICE_NAME))
        assertTrue(isShowing("Midweek Prayer"))
    }

    @Test
    fun aServiceShowsWhenItStarts() = runComposeUiTest {
        showScreen { DayServices(CalendarFixtures.SUNDAY, listOf(CalendarFixtures.service), null, {}, {}, null) }

        assertTrue(isShowing("10:00"))
    }

    @Test
    fun openingAServiceReportsWhichOne() = runComposeUiTest {
        var opened: String? = null
        showScreen {
            DayServices(CalendarFixtures.SUNDAY, CalendarFixtures.month, null, { opened = it.id }, {}, null)
        }

        click(CalendarTags.serviceCard("s1"))

        assertEquals("s1", opened)
    }

    @Test
    fun anEmptyDayOffersBothWaysToFillIt() = runComposeUiTest {
        var added = 0
        var copied = 0
        showScreen {
            DayServices(CalendarFixtures.SUNDAY, emptyList(), null, {}, { added++ }, { copied++ })
        }

        click(CalendarTags.DAY_ADD)
        click(CalendarTags.DAY_COPY_LAST)
        assertEquals(1, added)
        assertEquals(1, copied)
    }

    @Test
    fun anEmptyDayWithNothingToCopyOffersOnlyAdd() = runComposeUiTest {
        showScreen { DayServices(CalendarFixtures.SUNDAY, emptyList(), null, {}, {}, null) }

        assertEquals(1, clickables(this).fetchSemanticsNodes().size)
    }

    @Test
    fun anEmptyDaySaysWhichDayItIs() = runComposeUiTest {
        showScreen { DayServices(CalendarFixtures.SUNDAY, emptyList(), null, {}, {}, null) }

        assertTrue(isShowing("Sep 20"), "the date this app formatted, not a resource")
    }

    // ── A service card ───────────────────────────────────────────────────

    @Test
    fun aCardNamesItsServiceAndTime() = runComposeUiTest {
        showScreen { ServiceCard(CalendarFixtures.service, selected = false, onClick = {}) }

        assertTrue(isShowing(CalendarFixtures.SERVICE_NAME))
        assertTrue(isShowing("10:00"))
    }

    @Test
    fun aSelectedCardIsStillTappable() = runComposeUiTest {
        var taps = 0
        showScreen {
            ServiceCard(
                CalendarFixtures.service,
                selected = true,
                onClick = { taps++ },
                modifier = androidx.compose.ui.Modifier,
            )
        }

        clickables(this)[0].performClick()

        assertEquals(1, taps)
    }

    @Test
    fun aCardWithNoRowsSaysSoRatherThanNothing() = runComposeUiTest {
        showScreen { ServiceCard(CalendarFixtures.empty, selected = false, onClick = {}) }

        assertTrue(isShowing(CalendarFixtures.SERVICE_NAME))
        assertFalse(isShowing(CalendarFixtures.FIRST_SONG), "a card is not the run of show")
    }

    @Test
    fun theAddBarReachesItsCallback() = runComposeUiTest {
        var added = 0
        showScreen { AddServiceBar(onAdd = { added++ }) }

        click(CalendarTags.DAY_ADD)

        assertEquals(1, added)
    }
}
