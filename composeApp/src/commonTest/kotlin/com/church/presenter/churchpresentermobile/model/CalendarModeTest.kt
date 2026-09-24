package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.settledMoreDestination
import com.church.presenter.churchpresentermobile.settledTab
import com.church.presenter.churchpresentermobile.tabScreenName
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Calendar mode: the phone plans services and projects nothing.
 *
 * It exists because the other two both walk through a connection the planner has no use for —
 * remote checks for a desktop and standalone opens outputs — and a volunteer planning next
 * Sunday from home met "could not connect to a server" before they met the calendar.
 */
class CalendarModeTest {

    @Test
    fun `the strip is the calendar and contact, and nothing that projects`() {
        val tabs = AppTab.forMode(AppMode.CALENDAR)

        // Contact is a tab rather than More's only tile: a launcher with one entry is a detour.
        assertEquals(listOf(AppTab.CALENDAR, AppTab.CONTACT), tabs)
        assertFalse(AppTab.PRESENT in tabs, "nothing is projected from this phone")
        assertFalse(AppTab.SONGS in tabs, "the songs tab drives a desktop; the picker reads the catalog")
        assertFalse(AppTab.MEDIA in tabs)
        assertFalse(AppTab.PRESENTATION in tabs)
    }

    @Test
    fun `the calendar tab exists only in calendar mode`() {
        assertFalse(AppTab.CALENDAR in AppTab.forMode(AppMode.REMOTE))
        assertFalse(AppTab.CALENDAR in AppTab.forMode(AppMode.STANDALONE))
        assertTrue(AppTab.CALENDAR in AppTab.forMode(AppMode.CALENDAR))
    }

    @Test
    fun `the other two modes reach the calendar through More instead`() {
        // The same screen either way -- only where it is opened from changes.
        assertTrue(MoreDestination.CALENDAR in MoreDestination.forMode(AppMode.REMOTE))
        assertTrue(MoreDestination.CALENDAR in MoreDestination.forMode(AppMode.STANDALONE))
        assertFalse(MoreDestination.CALENDAR in MoreDestination.forMode(AppMode.CALENDAR))
    }

    @Test
    fun `More keeps only what needs neither a desktop nor an output`() {
        // Contact posts to a public endpoint, so it works with nothing else attached -- and this is
        // the mode most likely to be used by someone who has never seen the church computer.
        assertEquals(listOf(MoreDestination.CONTACT), MoreDestination.forMode(AppMode.CALENDAR))
    }

    @Test
    fun `there is no desktop to talk to`() {
        AppModeHolder.resetForTest()
        assertTrue(AppModeHolder.hasDesktop, "remote is the default")

        AppModeHolder.set(AppSettings(InMemorySettingsStorage()), AppMode.CALENDAR)

        assertFalse(AppModeHolder.hasDesktop, "the screens that mirror a desktop must not load")
        AppModeHolder.resetForTest()
    }

    @Test
    fun `a tab left over from another mode settles onto the calendar`() {
        val tabs = AppTab.forMode(AppMode.CALENDAR)

        // The strip is rememberSaveable, so after a switch it can still name a tab that is gone.
        assertEquals(AppTab.CALENDAR, settledTab(AppTab.SONGS, tabs))
        assertEquals(AppTab.CALENDAR, settledTab(AppTab.PRESENT, tabs))
        assertEquals(AppTab.CALENDAR, settledTab(AppTab.MORE, tabs))
        assertEquals(AppTab.CONTACT, settledTab(AppTab.CONTACT, tabs))
    }

    @Test
    fun `a More screen left open in another mode is closed by the switch`() {
        assertEquals(null, settledMoreDestination(MoreDestination.QA, AppMode.CALENDAR))
        assertEquals(null, settledMoreDestination(MoreDestination.PICTURES, AppMode.CALENDAR))
        // The calendar itself is a tab here, not a More entry, so it does not survive either.
        assertEquals(null, settledMoreDestination(MoreDestination.CALENDAR, AppMode.CALENDAR))
        assertEquals(MoreDestination.CONTACT, settledMoreDestination(MoreDestination.CONTACT, AppMode.CALENDAR))
    }

    @Test
    fun `the calendar tab reports itself to the screen report`() {
        assertEquals("Calendar", tabScreenName(AppTab.CALENDAR))
        assertEquals("Contact", tabScreenName(AppTab.CONTACT))
    }

    @Test
    fun `every mode still has a strip with no duplicates`() {
        AppMode.entries.forEach { mode ->
            val tabs = AppTab.forMode(mode)
            assertTrue(tabs.isNotEmpty(), "$mode has no tabs")
            assertEquals(tabs.distinct(), tabs, "$mode repeats a tab")
            assertTrue(
                AppTab.MORE in tabs || AppTab.CONTACT in tabs,
                "$mode must keep a way into contact",
            )
        }
    }

    @Test
    fun `a phone remembers the mode it was put into`() {
        val storage = InMemorySettingsStorage()
        val settings = AppSettings(storage)

        settings.appMode = AppMode.CALENDAR

        // Every build, unlike standalone: this mode projects nothing, so there is no output it
        // could be missing. Standalone is the one still coerced where the platform cannot present.
        assertEquals(AppMode.CALENDAR, settings.appMode)
        assertEquals(AppMode.CALENDAR, AppSettings(storage).appMode, "and after a restart")

        settings.appMode = AppMode.STANDALONE
        val standalone = if (supportsStandalone) AppMode.STANDALONE else AppMode.REMOTE
        assertEquals(standalone, settings.appMode)
    }
}
