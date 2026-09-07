package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.AppTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The two decisions inside [MainActivity] that can actually be wrong.
 *
 * The Activity itself is framework plumbing — `registerForActivityResult`,
 * `enableEdgeToEdge`, `setContent` — and needs an emulator, which AGENT.md rules
 * out. What is worth pinning is what it *decides* when an intent arrives, and
 * both of these are reached from `onResume`: it runs again on every foreground
 * and every configuration change, with the same intent still attached.
 */
class MainActivityIntentsTest {

    // ── Deep links ───────────────────────────────────────────────────────

    @Test
    fun `an ordinary launch carries no deep link`() {
        // Opening the app from the launcher has no data string at all.
        assertNull(deepLinkToHandle(url = null, lastHandled = null))
    }

    @Test
    fun `a link for another app is left alone`() {
        assertNull(deepLinkToHandle("https://example.org/connect?host=1.2.3.4", null))
    }

    @Test
    fun `a churchpresenter link is handled`() {
        val url = "churchpresenter://connect?host=192.168.1.10&port=8765"

        assertEquals(url, deepLinkToHandle(url, lastHandled = null))
    }

    @Test
    fun `the same link is not handled twice`() {
        // The regression this guards: onResume fires again on every rotation,
        // and the scanned QR is still the current intent. Re-handling it
        // re-applies the host, port and key the operator may have since edited.
        val url = "churchpresenter://connect?host=192.168.1.10"

        assertNull(deepLinkToHandle(url, lastHandled = url))
    }

    @Test
    fun `a different link after one already handled is still handled`() {
        // Scanning a second QR code in one session must not be swallowed by the
        // dedupe meant for the first.
        val first = "churchpresenter://connect?host=192.168.1.10"
        val second = "churchpresenter://connect?host=10.0.0.5"

        assertEquals(second, deepLinkToHandle(second, lastHandled = first))
    }

    // ── Home-screen shortcuts ────────────────────────────────────────────

    @Test
    fun `the songs shortcut opens songs`() {
        assertEquals(AppTab.SONGS, shortcutTab(SHORTCUT_OPEN_SONGS))
    }

    @Test
    fun `the bible shortcut opens the bible`() {
        assertEquals(AppTab.BIBLE, shortcutTab(SHORTCUT_OPEN_BIBLE))
    }

    @Test
    fun `an ordinary launch action opens no tab of its own`() {
        // android.intent.action.MAIN arrives on every normal launch; treating it
        // as a shortcut would yank the operator to a tab they did not ask for.
        assertNull(shortcutTab("android.intent.action.MAIN"))
        assertNull(shortcutTab(null))
    }

    @Test
    fun `an unknown shortcut action opens nothing rather than guessing`() {
        assertNull(shortcutTab("com.church.presenter.churchpresentermobile.OPEN_SOMETHING"))
    }
}
