package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppTabTest {

    /**
     * Phase 0's whole claim is that remote mode is untouched. If this list ever
     * changes, existing users' tab strip changed with it.
     */
    @Test
    fun `remote keeps exactly the tabs it has always had`() {
        assertEquals(
            listOf(AppTab.SONGS, AppTab.BIBLE, AppTab.MEDIA, AppTab.PRESENTATION, AppTab.MORE),
            AppTab.forMode(AppMode.REMOTE),
        )
    }

    @Test
    fun `standalone is bible, songs, library, announcements and more`() {
        // The live controls ride along every tab rather than having one, and the
        // running order is the schedule drawer, as in remote.
        assertEquals(
            listOf(AppTab.BIBLE, AppTab.SONGS, AppTab.LIBRARY, AppTab.ANNOUNCEMENTS, AppTab.MORE),
            AppTab.forMode(AppMode.STANDALONE),
        )
    }

    @Test
    fun `standalone drops the tabs that need a desktop`() {
        val tabs = AppTab.forMode(AppMode.STANDALONE)
        assertFalse(AppTab.MEDIA in tabs, "media casting is a desktop transport")
        assertFalse(AppTab.PRESENTATION in tabs, "presentation decks live on the desktop")
    }

    @Test
    fun `the announcements tab exists only in standalone`() {
        // Remote's composer is a More screen that drives the desktop.
        assertFalse(AppTab.ANNOUNCEMENTS in AppTab.forMode(AppMode.REMOTE))
        assertTrue(AppTab.ANNOUNCEMENTS in AppTab.forMode(AppMode.STANDALONE))
    }

    /** The library is on-device content, which only standalone mode can present. */
    @Test
    fun `the library tab exists only in standalone`() {
        assertFalse(AppTab.LIBRARY in AppTab.forMode(AppMode.REMOTE))
        assertTrue(AppTab.LIBRARY in AppTab.forMode(AppMode.STANDALONE))
    }

    @Test
    fun `every mode offers a non-empty strip with no duplicates`() {
        AppMode.entries.forEach { mode ->
            val tabs = AppTab.forMode(mode)
            assertTrue(tabs.isNotEmpty(), "$mode has no tabs")
            assertEquals(tabs.distinct(), tabs, "$mode has a duplicate tab")
        }
    }

    @Test
    fun `songs and bible are reachable in both presenting modes`() {
        // Calendar mode presents nothing, so it has neither tab.
        listOf(AppMode.REMOTE, AppMode.STANDALONE).forEach { mode ->
            val tabs = AppTab.forMode(mode)
            assertTrue(AppTab.SONGS in tabs)
            assertTrue(AppTab.BIBLE in tabs)
        }
    }

    // ── More destinations ────────────────────────────────────────────────

    @Test
    fun `remote keeps every More destination but the report`() {
        // The CCLI report is about what this device projected, which in remote mode is nothing.
        // …and but the canvas, which is tablet-only.
        assertEquals(
            MoreDestination.entries - MoreDestination.REPORT - MoreDestination.CANVAS,
            MoreDestination.forMode(AppMode.REMOTE),
        )
    }

    @Test
    fun `standalone keeps only the More destinations it can serve itself`() {
        // Photos are local — picked on this device and projected by it. Q&A, the
        // dictionary and Web need the desktop, so they stay out, and Announcements
        // here is the remote composer: standalone has a tab of its own.
        //
        // REPORT is the CCLI report of what this device put on a screen, which
        // only standalone has: in remote mode the desktop projects and reports.
        //
        // CALENDAR plans against this device's own calendar.json; the desktop
        // only enters through the relay, which standalone can enroll with too.
        assertEquals(
            listOf(
                MoreDestination.CALENDAR,
                MoreDestination.PICTURES,
                MoreDestination.REPORT,
                MoreDestination.CONTACT,
            ),
            MoreDestination.forMode(AppMode.STANDALONE),
        )
    }

    @Test
    fun `contact is reachable in both modes`() {
        // It posts to a public endpoint on the internet, so it needs no desktop —
        // and a user stuck in standalone is exactly the one with something to report.
        assertTrue(MoreDestination.CONTACT in MoreDestination.forMode(AppMode.STANDALONE))
        assertTrue(MoreDestination.CONTACT in MoreDestination.forMode(AppMode.REMOTE))
    }

    @Test
    fun `the More tab exists in both modes`() {
        assertTrue(AppTab.MORE in AppTab.forMode(AppMode.STANDALONE))
        assertTrue(AppTab.MORE in AppTab.forMode(AppMode.REMOTE))
    }
}
