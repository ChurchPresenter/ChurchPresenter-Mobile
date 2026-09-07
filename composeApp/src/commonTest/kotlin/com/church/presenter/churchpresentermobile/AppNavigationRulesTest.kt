package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.util.AnalyticsScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The rules `App()` navigates by.
 *
 * The two that bite are both about a mode switch: the selected tab survives it
 * in saved state, so it can name a tab the new strip does not have, and a More
 * sub-screen opened in remote mode has no standalone equivalent to fall back
 * to. Either one leaves the operator on a screen with no way back — mid-service,
 * on a phone they are holding in one hand.
 */
class AppNavigationRulesTest {

    private val remoteTabs = AppTab.forMode(AppMode.REMOTE)
    private val standaloneTabs = AppTab.forMode(AppMode.STANDALONE)

    // ── Settling the tab after a mode switch ─────────────────────────────

    @Test
    fun `a tab the strip still has is kept`() {
        assertEquals(AppTab.SONGS, settledTab(AppTab.SONGS, standaloneTabs))
    }

    @Test
    fun `a tab the strip has lost falls back to the first`() {
        // MEDIA is remote-only; standalone opens on the live controller.
        assertEquals(AppTab.PRESENT, settledTab(AppTab.MEDIA, standaloneTabs))
    }

    @Test
    fun `a standalone-only tab falls back when going remote`() {
        assertEquals(AppTab.SONGS, settledTab(AppTab.PRESENT, remoteTabs))
    }

    @Test
    fun `the library tab falls back when going remote`() {
        assertEquals(AppTab.SONGS, settledTab(AppTab.LIBRARY, remoteTabs))
    }

    @Test
    fun `the presentation tab falls back when going standalone`() {
        assertEquals(AppTab.PRESENT, settledTab(AppTab.PRESENTATION, standaloneTabs))
    }

    @Test
    fun `a tab both modes share survives either switch`() {
        // Songs, Bible and More are the operator's place in the app; losing
        // them on a mode switch would be gratuitous.
        listOf(AppTab.SONGS, AppTab.BIBLE, AppTab.MORE).forEach { tab ->
            assertEquals(tab, settledTab(tab, remoteTabs))
            assertEquals(tab, settledTab(tab, standaloneTabs))
        }
    }

    @Test
    fun `settling is stable when nothing changed`() {
        assertEquals(AppTab.BIBLE, settledTab(settledTab(AppTab.BIBLE, remoteTabs), remoteTabs))
    }

    @Test
    fun `every remote tab settles to itself in remote`() {
        remoteTabs.forEach { assertEquals(it, settledTab(it, remoteTabs)) }
    }

    @Test
    fun `every standalone tab settles to itself in standalone`() {
        standaloneTabs.forEach { assertEquals(it, settledTab(it, standaloneTabs)) }
    }

    @Test
    fun `settling always names a tab the strip holds`() {
        AppTab.entries.forEach { tab ->
            assertTrue(settledTab(tab, remoteTabs) in remoteTabs)
            assertTrue(settledTab(tab, standaloneTabs) in standaloneTabs)
        }
    }

    // ── Settling the More destination ────────────────────────────────────

    @Test
    fun `a More screen both modes have is kept`() {
        assertEquals(
            MoreDestination.PICTURES,
            settledMoreDestination(MoreDestination.PICTURES, AppMode.STANDALONE),
        )
    }

    @Test
    fun `a desktop-backed More screen closes on going standalone`() {
        // Q&A needs the desktop's data; standalone has none to show.
        assertNull(settledMoreDestination(MoreDestination.QA, AppMode.STANDALONE))
    }

    @Test
    fun `the dictionary closes on going standalone`() {
        assertNull(settledMoreDestination(MoreDestination.DICTIONARY, AppMode.STANDALONE))
    }

    @Test
    fun `notices stay open across a switch to standalone`() {
        // A different screen with the same name, but the operator still has one.
        assertEquals(
            MoreDestination.ANNOUNCEMENTS,
            settledMoreDestination(MoreDestination.ANNOUNCEMENTS, AppMode.STANDALONE),
        )
    }

    @Test
    fun `the web screen stays open across a switch to standalone`() {
        assertEquals(
            MoreDestination.WEB,
            settledMoreDestination(MoreDestination.WEB, AppMode.STANDALONE),
        )
    }

    @Test
    fun `contact stays open in either mode`() {
        // It posts to a public endpoint, so it needs no desktop — and someone
        // hitting a problem in standalone is exactly who wants it.
        assertEquals(
            MoreDestination.CONTACT,
            settledMoreDestination(MoreDestination.CONTACT, AppMode.STANDALONE),
        )
        assertEquals(
            MoreDestination.CONTACT,
            settledMoreDestination(MoreDestination.CONTACT, AppMode.REMOTE),
        )
    }

    @Test
    fun `every remote More screen survives staying in remote`() {
        MoreDestination.forMode(AppMode.REMOTE).forEach {
            assertEquals(it, settledMoreDestination(it, AppMode.REMOTE))
        }
    }

    @Test
    fun `the launcher grid stays the launcher grid`() {
        assertNull(settledMoreDestination(null, AppMode.REMOTE))
        assertNull(settledMoreDestination(null, AppMode.STANDALONE))
    }

    @Test
    fun `settling always names a screen the mode offers`() {
        MoreDestination.entries.forEach { destination ->
            val settled = settledMoreDestination(destination, AppMode.STANDALONE)
            assertTrue(settled == null || settled in MoreDestination.forMode(AppMode.STANDALONE))
        }
    }

    // ── Pager pages and tabs ─────────────────────────────────────────────

    @Test
    fun `a tab's page is its place in the strip`() {
        assertEquals(0, pageForTab(AppTab.SONGS, remoteTabs))
        assertEquals(1, pageForTab(AppTab.BIBLE, remoteTabs))
    }

    @Test
    fun `the same tab has a different page in each mode`() {
        // Songs is first in remote and second in standalone; a pager left on
        // the old index shows a different tab than the strip highlights.
        assertEquals(0, pageForTab(AppTab.SONGS, remoteTabs))
        assertEquals(1, pageForTab(AppTab.SONGS, standaloneTabs))
    }

    @Test
    fun `a tab the strip has lost falls back to the first page`() {
        assertEquals(0, pageForTab(AppTab.MEDIA, standaloneTabs))
    }

    @Test
    fun `a page names the tab at that index`() {
        assertEquals(AppTab.BIBLE, tabForPage(1, remoteTabs))
    }

    @Test
    fun `a page past the end falls back to the first tab`() {
        assertEquals(remoteTabs.first(), tabForPage(99, remoteTabs))
    }

    @Test
    fun `a negative page falls back to the first tab`() {
        assertEquals(remoteTabs.first(), tabForPage(-1, remoteTabs))
    }

    @Test
    fun `page and tab are two ways of saying the same thing`() {
        remoteTabs.forEach { tab ->
            assertEquals(tab, tabForPage(pageForTab(tab, remoteTabs), remoteTabs))
        }
    }

    @Test
    fun `page and tab agree in standalone too`() {
        standaloneTabs.forEach { tab ->
            assertEquals(tab, tabForPage(pageForTab(tab, standaloneTabs), standaloneTabs))
        }
    }

    @Test
    fun `every page of the strip names a tab`() {
        standaloneTabs.indices.forEach { page ->
            assertTrue(tabForPage(page, standaloneTabs) in standaloneTabs)
        }
    }

    // ── What the reports are told ────────────────────────────────────────

    @Test
    fun `the songs tab reports the songs screen`() {
        assertEquals(AnalyticsScreen.SONGS, tabScreenName(AppTab.SONGS))
    }

    @Test
    fun `the bible tab reports the book list`() {
        // The tab itself is always the top of the Bible; depth is reported
        // separately as the operator goes in.
        assertEquals(AnalyticsScreen.BIBLE_BOOKS, tabScreenName(AppTab.BIBLE))
    }

    @Test
    fun `the live controller reports the standalone screen`() {
        assertEquals(AnalyticsScreen.STANDALONE, tabScreenName(AppTab.PRESENT))
    }

    @Test
    fun `the library tab reports the library screen`() {
        assertEquals(AnalyticsScreen.LIBRARY, tabScreenName(AppTab.LIBRARY))
    }

    @Test
    fun `the media tab reports the media screen`() {
        assertEquals(AnalyticsScreen.MEDIA, tabScreenName(AppTab.MEDIA))
    }

    @Test
    fun `the presentation tab reports the presentations screen`() {
        assertEquals(AnalyticsScreen.PRESENTATIONS, tabScreenName(AppTab.PRESENTATION))
    }

    @Test
    fun `the more tab reports the more screen`() {
        assertEquals(AnalyticsScreen.MORE, tabScreenName(AppTab.MORE))
    }

    @Test
    fun `every tab reports a name of its own`() {
        // Two tabs sharing a name merges their figures into one row nobody can
        // read apart again.
        val names = AppTab.entries.map { tabScreenName(it) }

        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `no tab reports a blank name`() {
        AppTab.entries.forEach { assertTrue(tabScreenName(it).isNotBlank()) }
    }

    @Test
    fun `the photos screen reports itself`() {
        assertEquals(AnalyticsScreen.PICTURES, moreScreenName(MoreDestination.PICTURES))
    }

    @Test
    fun `the QA screen reports itself`() {
        assertEquals(AnalyticsScreen.QA_ADMIN, moreScreenName(MoreDestination.QA))
    }

    @Test
    fun `the dictionary reports itself`() {
        assertEquals(AnalyticsScreen.DICTIONARY, moreScreenName(MoreDestination.DICTIONARY))
    }

    @Test
    fun `the announcements screen reports itself`() {
        assertEquals(AnalyticsScreen.ANNOUNCEMENTS, moreScreenName(MoreDestination.ANNOUNCEMENTS))
    }

    @Test
    fun `the web screen reports itself`() {
        assertEquals(AnalyticsScreen.WEB, moreScreenName(MoreDestination.WEB))
    }

    @Test
    fun `the contact form reports nothing`() {
        // It is a form posting to a public endpoint, not a screen of the app's
        // own content.
        assertNull(moreScreenName(MoreDestination.CONTACT))
    }

    @Test
    fun `the launcher grid reports nothing of its own`() {
        // The More tab has already been reported by then.
        assertNull(moreScreenName(null))
    }

    @Test
    fun `no two More screens report the same name`() {
        val names = MoreDestination.entries.mapNotNull { moreScreenName(it) }

        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `the top of the bible reports the book list`() {
        assertEquals(AnalyticsScreen.BIBLE_BOOKS, bibleScreenName(hasBook = false, hasChapter = false))
    }

    @Test
    fun `a chosen book reports the chapter list`() {
        assertEquals(AnalyticsScreen.BIBLE_CHAPTERS, bibleScreenName(hasBook = true, hasChapter = false))
    }

    @Test
    fun `a chosen chapter reports the verses`() {
        assertEquals(AnalyticsScreen.BIBLE_VERSES, bibleScreenName(hasBook = true, hasChapter = true))
    }

    @Test
    fun `a chapter without a book still reports the verses`() {
        // Not a state the UI can reach, but the deeper answer is the safer one:
        // a chapter is on screen either way.
        assertEquals(AnalyticsScreen.BIBLE_VERSES, bibleScreenName(hasBook = false, hasChapter = true))
    }

    @Test
    fun `the three bible depths are three different names`() {
        val names = setOf(
            bibleScreenName(hasBook = false, hasChapter = false),
            bibleScreenName(hasBook = true, hasChapter = false),
            bibleScreenName(hasBook = true, hasChapter = true),
        )

        assertEquals(3, names.size)
    }

    // ── Demo mode ────────────────────────────────────────────────────────

    @Test
    fun `a debug build never starts in demo mode`() {
        // A developer working against a live desktop must not have it swapped
        // for canned content by a flag flipped in a console.
        assertFalse(startsInDemoMode(isDebug = true, remoteFlag = true))
    }

    @Test
    fun `a debug build with the flag off is not in demo mode either`() {
        assertFalse(startsInDemoMode(isDebug = true, remoteFlag = false))
    }

    @Test
    fun `a release build follows the flag`() {
        assertTrue(startsInDemoMode(isDebug = false, remoteFlag = true))
    }

    @Test
    fun `a release build with the flag off is not in demo mode`() {
        assertFalse(startsInDemoMode(isDebug = false, remoteFlag = false))
    }

    // ── What the live map is told ────────────────────────────────────────

    @Test
    fun `an opted-in device sends its id`() {
        assertEquals("device-1234", pingDeviceId(telemetryEnabled = true, deviceId = "device-1234"))
    }

    @Test
    fun `an opted-out device sends no id`() {
        // The ping still goes — it is anonymous and city-level — but nothing
        // ties two launches to one device.
        assertEquals("", pingDeviceId(telemetryEnabled = false, deviceId = "device-1234"))
    }

    @Test
    fun `an opted-out device sends no id even when one exists`() {
        assertTrue(pingDeviceId(telemetryEnabled = false, deviceId = "device-1234").isEmpty())
    }

    @Test
    fun `an opted-in device with no id sends nothing`() {
        assertEquals("", pingDeviceId(telemetryEnabled = true, deviceId = ""))
    }

    // ── Small decisions in the chrome ────────────────────────────────────

    @Test
    fun `a scanned link opens settings`() {
        // So the operator can see what the scan changed.
        assertTrue(deepLinkOpensSettings(showingConnectSetup = false))
    }

    @Test
    fun `a scan during connect setup does not open settings over it`() {
        // The setup screen handled the scan and is showing its own confirmation.
        assertFalse(deepLinkOpensSettings(showingConnectSetup = true))
    }

    @Test
    fun `re-tapping the More tab returns to its launcher`() {
        assertTrue(tapReturnsToMoreLauncher(tab = AppTab.MORE, selected = AppTab.MORE))
    }

    @Test
    fun `arriving at More from another tab keeps the launcher`() {
        // Nothing to return from; the grid is what opens anyway.
        assertFalse(tapReturnsToMoreLauncher(tab = AppTab.MORE, selected = AppTab.SONGS))
    }

    @Test
    fun `re-tapping another tab does not touch the More screen`() {
        // An operator on a More sub-screen who taps Songs and comes back should
        // find it as they left it.
        assertFalse(tapReturnsToMoreLauncher(tab = AppTab.SONGS, selected = AppTab.SONGS))
    }

    @Test
    fun `tapping away from More does not touch its screen`() {
        assertFalse(tapReturnsToMoreLauncher(tab = AppTab.BIBLE, selected = AppTab.MORE))
    }
}
