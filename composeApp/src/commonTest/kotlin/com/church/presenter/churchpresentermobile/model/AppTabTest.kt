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
    fun `standalone leads with the live controller`() {
        assertEquals(AppTab.PRESENT, AppTab.forMode(AppMode.STANDALONE).first())
    }

    @Test
    fun `standalone drops the tabs that need a desktop`() {
        val tabs = AppTab.forMode(AppMode.STANDALONE)
        assertFalse(AppTab.MEDIA in tabs, "media casting is a desktop transport")
        assertFalse(AppTab.PRESENTATION in tabs, "presentation decks live on the desktop")
    }

    @Test
    fun `the present tab exists only in standalone`() {
        assertFalse(AppTab.PRESENT in AppTab.forMode(AppMode.REMOTE))
        assertTrue(AppTab.PRESENT in AppTab.forMode(AppMode.STANDALONE))
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
    fun `songs and bible are reachable in both modes`() {
        AppMode.entries.forEach { mode ->
            val tabs = AppTab.forMode(mode)
            assertTrue(AppTab.SONGS in tabs)
            assertTrue(AppTab.BIBLE in tabs)
        }
    }

    // ── More destinations ────────────────────────────────────────────────

    @Test
    fun `remote keeps every More destination`() {
        assertEquals(MoreDestination.entries.toList(), MoreDestination.forMode(AppMode.REMOTE))
    }

    @Test
    fun `standalone keeps only the More destination it can serve itself`() {
        // Photos is local — picked from this device, projected by it. The rest
        // need the desktop's data, and the announcements composer writes to the
        // desktop's schedule and screen.
        assertEquals(listOf(MoreDestination.PICTURES), MoreDestination.forMode(AppMode.STANDALONE))
    }

    @Test
    fun `the More tab exists in both modes`() {
        assertTrue(AppTab.MORE in AppTab.forMode(AppMode.STANDALONE))
        assertTrue(AppTab.MORE in AppTab.forMode(AppMode.REMOTE))
    }
}
