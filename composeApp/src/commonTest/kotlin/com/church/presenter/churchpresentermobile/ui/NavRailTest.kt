package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The tablet's side rail.
 *
 * It is the only way between tabs once the bottom strip is gone, so what is
 * asserted here is what would strand an operator: a tab missing from the rail is
 * a tab they cannot reach, and a tap reporting the wrong tab sends them
 * somewhere else mid-service.
 *
 * Tabs are named by [UiTags.tab] rather than by their labels — those come from
 * compose-resources, which renders empty in the wasmJs runtime. It is the same
 * tag the bottom strip carries, which is the point of the two sharing
 * `tabSpecs`.
 */
@OptIn(ExperimentalTestApi::class)
class NavRailTest {

    @Test
    fun everyRemoteTabIsOnTheRail() = runComposeUiTest {
        showRail(tabs = AppTab.forMode(AppMode.REMOTE))

        AppTab.forMode(AppMode.REMOTE).forEach { tab ->
            assertTrue(exists(UiTags.tab(tab)), "$tab is missing from the rail")
        }
    }

    @Test
    fun standaloneGetsItsOwnTabsAndNotTheDesktopOnes() = runComposeUiTest {
        showRail(selected = AppTab.PRESENT, tabs = AppTab.forMode(AppMode.STANDALONE))

        assertTrue(exists(UiTags.tab(AppTab.PRESENT)))
        assertTrue(exists(UiTags.tab(AppTab.LIBRARY)))
        // Media casting and the desktop's decks mean nothing with no desktop.
        assertFalse(exists(UiTags.tab(AppTab.MEDIA)), "Media has no place in standalone")
        assertFalse(exists(UiTags.tab(AppTab.PRESENTATION)), "decks have no place in standalone")
    }

    @Test
    fun theRailCarriesTheBrandMark() = runComposeUiTest {
        // The bottom strip has nowhere to put it. On a tablet the app is one
        // window among several and has to say whose window it is.
        showRail()

        assertTrue(exists(UiTags.NAV_RAIL_BRAND))
    }

    @Test
    fun tappingATabReportsThatTab() = runComposeUiTest {
        var picked: AppTab? = null
        showRail(onTabSelected = { picked = it })

        click(UiTags.tab(AppTab.BIBLE))

        assertEquals(AppTab.BIBLE, picked)
    }

    @Test
    fun tappingReportsTheTabThatWasTapped() = runComposeUiTest {
        // Two taps, two different answers. A rail reporting a fixed tab, or its
        // own index, passes a single-tap test and fails every operator.
        var picked: AppTab? = null
        showRail(onTabSelected = { picked = it })

        click(UiTags.tab(AppTab.MEDIA))
        assertEquals(AppTab.MEDIA, picked)

        click(UiTags.tab(AppTab.MORE))
        assertEquals(AppTab.MORE, picked)
    }

    @Test
    fun theSelectedTabIsStillTappable() = runComposeUiTest {
        // Re-tapping the active More tab is how the operator backs out of a More
        // sub-screen, so the current tab must not be inert.
        var picked: AppTab? = null
        showRail(selected = AppTab.MORE, onTabSelected = { picked = it })

        click(UiTags.tab(AppTab.MORE))

        assertEquals(AppTab.MORE, picked)
    }

    @Test
    fun nothingIsReportedUntilSomethingIsTapped() = runComposeUiTest {
        var picked: AppTab? = null
        showRail(onTabSelected = { picked = it })

        assertNull(picked, "the rail reported a tab nobody tapped")
    }

    @Test
    fun theRailKeepsTheOrderTheStripWasGiven() = runComposeUiTest {
        // `tabSpecs` is in declaration order; the rail has to follow
        // AppTab.forMode instead, or standalone's Present ends up mid-list.
        val reversed = AppTab.forMode(AppMode.REMOTE).reversed()
        showRail(tabs = reversed)

        reversed.forEach { assertTrue(exists(UiTags.tab(it))) }
    }
}
